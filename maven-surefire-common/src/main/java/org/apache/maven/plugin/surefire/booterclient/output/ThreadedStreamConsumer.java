package org.apache.maven.plugin.surefire.booterclient.output;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.annotation.Nonnull;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.extensions.EventHandler;

import static java.lang.Thread.currentThread;
import static org.apache.maven.surefire.api.util.internal.DaemonThreadFactory.newDaemonThread;

/**
 * Knows how to reconstruct *all* the state transmitted over Channel by the forked process.
 * <br>
 * After applying the performance improvements with {@link QueueSynchronizer} the throughput becomes
 * 6.33 mega messages per second
 * (158 nano seconds per message, 5 million messages within 0.79 seconds - see the test ThreadedStreamConsumerTest)
 * on CPU i5 Dual Core 2.6 GHz and Oracle JDK 11.
 *
 * @author Kristian Rosenvold
 */
public final class ThreadedStreamConsumer
    implements EventHandler<Event>, Closeable
{
    private static final int QUEUE_MAX_ITEMS = 10_000;
    private static final Event END_ITEM = new FinalEvent();

    private final QueueSynchronizer<Event> synchronizer = new QueueSynchronizer<>( QUEUE_MAX_ITEMS, END_ITEM );
    private final AtomicBoolean stop = new AtomicBoolean();
    private final AtomicBoolean isAlive = new AtomicBoolean( true );
    private final Thread consumer;
    private final Pumper pumper;

    final class Pumper
        implements Runnable
    {
        private final EventHandler<Event> target;

        private final MultipleFailureException errors = new MultipleFailureException();

        Pumper( EventHandler<Event> target )
        {
            this.target = target;
        }

        /**
         * Calls {@link ForkClient#handleEvent(Event)} which may throw any {@link RuntimeException}.<br>
         * Even if {@link ForkClient} is not fault-tolerant, this method MUST be fault-tolerant and thus the
         * try-catch block must be inside of the loop which prevents from loosing events from {@link EventHandler}.
         * <br>
         * If {@link org.apache.maven.plugin.surefire.report.ConsoleOutputFileReporter#writeTestOutput} throws
         * {@link java.io.IOException} and then {@code target.consumeLine()} throws any RuntimeException, this method
         * MUST NOT skip reading the events from the forked JVM; otherwise we could simply lost events
         * e.g. acquire-next-test which means that {@link ForkClient} could hang on waiting for old test to complete
         * and therefore the plugin could be permanently in progress.
         */
        @Override
        public void run()
        {
            while ( !stop.get() || !synchronizer.isEmptyQueue() )
            {
                try
                {
                    Event item = synchronizer.awaitNext();

                    if ( shouldStopQueueing( item ) )
                    {
                        break;
                    }

                    target.handleEvent( item );
                }
                catch ( Throwable t )
                {
                    // ensure the stack trace to be at the instance of the exception
                    t.getStackTrace();
                    errors.addException( t );
                }
            }

            isAlive.set( false );
        }

        boolean hasErrors()
        {
            return errors.hasNestedExceptions();
        }

        void throwErrors() throws IOException
        {
            throw errors;
        }
    }

    public ThreadedStreamConsumer( EventHandler<Event> target )
    {
        pumper = new Pumper( target );
        Thread consumer = newDaemonThread( pumper, "ThreadedStreamConsumer" );
        consumer.setUncaughtExceptionHandler( ( t, e ) -> isAlive.set( false ) );
        consumer.start();
        this.consumer = consumer;
    }

    @Override
    public void handleEvent( @Nonnull Event event )
    {
        // Do NOT call Thread.isAlive() - slow.
        // It makes worse performance from 790 millis to 1250 millis for 5 million messages.
        if ( !stop.get() && isAlive.get() )
        {
            synchronizer.pushNext( event );
        }
    }

    @Override
    public void close()
        throws IOException
    {
        isAlive.compareAndSet( true, consumer.isAlive() );
        if ( stop.compareAndSet( false, true ) && isAlive.get() )
        {
            if ( currentThread().isInterrupted() )
            {
                synchronizer.markStopped();
                consumer.interrupt();
            }
            else
            {
                synchronizer.markStopped();

                try
                {
                    consumer.join();
                }
                catch ( InterruptedException e )
                {
                    // we should not set interrupted=true in this Thread
                    // if consumer's Thread was interrupted which is indicated by InterruptedException
                }

                synchronizer.clearQueue();
            }
        }

        if ( pumper.hasErrors() )
        {
            pumper.throwErrors();
        }
    }

    /**
     * Compared item with {@link #END_ITEM} by identity.
     *
     * @param item    element from <code>items</code>
     * @return {@code true} if tail of the queue
     */
    private static boolean shouldStopQueueing( Event item )
    {
        return item == END_ITEM;
    }

    /**
     *
     */
    private static class FinalEvent extends Event
    {
        FinalEvent()
        {
            super( null );
        }

        @Override
        public boolean isControlCategory()
        {
            return false;
        }

        @Override
        public boolean isConsoleCategory()
        {
            return false;
        }

        @Override
        public boolean isConsoleErrorCategory()
        {
            return false;
        }

        @Override
        public boolean isStandardStreamCategory()
        {
            return false;
        }

        @Override
        public boolean isSysPropCategory()
        {
            return false;
        }

        @Override
        public boolean isTestCategory()
        {
            return false;
        }

        @Override
        public boolean isJvmExitError()
        {
            return false;
        }
    }

    /**
     * This synchronization helper mostly avoids the locks.
     * If the queue size has reached zero or {@code maxQueueSize} then the threads are locked (parked/unparked).
     * The thread instance T1 is reader (see the class "Pumper") and T2 is the writer (see the method "handleEvent").
     *
     * @param <T> element type in the queue
     */
    static class QueueSynchronizer<T>
    {
        private final SyncT1 t1 = new SyncT1();
        private final SyncT2 t2 = new SyncT2();
        private final ConcurrentLinkedDeque<T> queue = new ConcurrentLinkedDeque<>();
        private final AtomicInteger queueSize = new AtomicInteger();
        private final int maxQueueSize;
        private final T stopItemMarker;

        QueueSynchronizer( int maxQueueSize, T stopItemMarker )
        {
            this.maxQueueSize = maxQueueSize;
            this.stopItemMarker = stopItemMarker;
        }

        private class SyncT1 extends AbstractQueuedSynchronizer
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected int tryAcquireShared( int arg )
            {
                return queueSize.get() == 0 ? -1 : 1;
            }

            @Override
            protected boolean tryReleaseShared( int arg )
            {
                return true;
            }

            void waitIfZero() throws InterruptedException
            {
                acquireSharedInterruptibly( 1 );
            }

            void release()
            {
                releaseShared( 0 );
            }
        }

        private class SyncT2 extends AbstractQueuedSynchronizer
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected int tryAcquireShared( int arg )
            {
                return queueSize.get() < maxQueueSize ? 1 : -1;
            }

            @Override
            protected boolean tryReleaseShared( int arg )
            {
                return true;
            }

            void awaitMax()
            {
                acquireShared( 1 );
            }

            void tryRelease()
            {
                if ( queueSize.get() == 0 )
                {
                    releaseShared( 0 );
                }
            }
        }

        void markStopped()
        {
            addNext( stopItemMarker );
        }

        void pushNext( T t )
        {
            t2.awaitMax();
            addNext( t );
        }

        T awaitNext() throws InterruptedException
        {
            t2.tryRelease();
            t1.waitIfZero();
            queueSize.decrementAndGet();
            return queue.pollFirst();
        }

        boolean isEmptyQueue()
        {
            return queue.isEmpty();
        }

        void clearQueue()
        {
            queue.clear();
        }

        private void addNext( T t )
        {
            queue.addLast( t );
            if ( queueSize.getAndIncrement() == 0 )
            {
                t1.release();
            }
        }
    }
}
