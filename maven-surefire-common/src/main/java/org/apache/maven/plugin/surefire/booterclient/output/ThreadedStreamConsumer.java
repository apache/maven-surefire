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

import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.util.internal.DaemonThreadFactory;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.shared.utils.cli.StreamConsumer;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Knows how to reconstruct *all* the state transmitted over stdout by the forked process.
 *
 * @author Kristian Rosenvold
 */
public final class ThreadedStreamConsumer
        implements EventHandler<Event>, Closeable
{
    private static final int QUEUE_MAX_ITEMS = 10_000;
    private static final Event END_ITEM = new FinalEvent();

    private final ConcurrentLinkedDeque<Event> queue = new ConcurrentLinkedDeque<>();

    private final ReentrantLock queueLock = new ReentrantLock();

    private final Condition queueCondition = queueLock.newCondition();

    private final AtomicInteger queueSize = new AtomicInteger();

    private final AtomicBoolean stop = new AtomicBoolean();

    private final Thread thread;

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
         * try-catch block must be inside of the loop which prevents from loosing events from {@link StreamConsumer}.
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
            try
            {
                queueLock.lock();
                while ( !stop.get() || !queue.isEmpty() )
                {
                    try
                    {
                        Event item = queue.pollFirst();

                        if ( item == null )
                        {
                            queueCondition.await( 1L, SECONDS );
                            continue;
                        }
                        else
                        {
                            queueSize.decrementAndGet();
                        }

                        if ( shouldStopQueueing( item ) )
                        {
                            break;
                        }

                        target.handleEvent( item );
                    }
                    catch ( Throwable t )
                    {
                        errors.addException( t );
                    }
                }
            }
            finally
            {
                queueLock.unlock();
            }
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
        thread = DaemonThreadFactory.newDaemonThread( pumper, "ThreadedStreamConsumer" );
        thread.start();
    }

    @Override
    public void handleEvent( @Nonnull Event event )
    {
        if ( stop.get() )
        {
            return;
        }
        else if ( !thread.isAlive() )
        {
            queue.clear();
            return;
        }

        int count = queueSize.get();
        // System.out.println( "count = " + count );
        if ( count == 0 || count >= QUEUE_MAX_ITEMS )
        {
            try
            {
                queueLock.lock();
                updateAndNotifyReader( event );
            }
            finally
            {
                queueLock.unlock();
            }
        }
        else
        {
            queueSize.incrementAndGet();
            queue.addLast( event );
        }
    }

    private void updateAndNotifyReader( @Nonnull Event event )
    {
        queueSize.incrementAndGet();
        queue.addLast( event );
        queueCondition.signal();
    }

    @Override
    public void close()
            throws IOException
    {
        if ( stop.compareAndSet( false, true ) )
        {
            queue.addLast( END_ITEM );
            try
            {
                queueLock.lock();
                queueCondition.signal();
            }
            finally
            {
                queueLock.unlock();
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
    private boolean shouldStopQueueing( Event item )
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
}
