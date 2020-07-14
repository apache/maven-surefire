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
import org.apache.maven.surefire.api.event.StandardStreamOutWithNewLineEvent;
import org.apache.maven.surefire.extensions.EventHandler;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.fest.assertions.Assertions.assertThat;

/**
 *
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class ThreadedStreamConsumerTest
{
    @Test
    @Ignore
    public void test() throws Exception
    {
        final CountDownLatch countDown = new CountDownLatch( 100_000 );
        EventHandler<Event> handler = new EventHandler<Event>()
        {
            private final AtomicInteger i = new AtomicInteger();

            @Override
            public void handleEvent( @Nonnull Event event )
            {
                //System.out.println(i.get());
                countDown.countDown();
                try
                {
                    if ( i.incrementAndGet() % 11_000 == 0 )
                    {
                        TimeUnit.MILLISECONDS.sleep( 10L );
                    }
                }
                catch ( InterruptedException e )
                {
                    throw new IllegalStateException( e );
                }
            }
        };

        ThreadedStreamConsumer streamConsumer = new ThreadedStreamConsumer( handler );

        for ( int i = 0; i < 100_000; i++ )
        {
            streamConsumer.handleEvent( new StandardStreamOutWithNewLineEvent( NORMAL_RUN, "" ) );
        }

        assertThat( countDown.await( 10L, TimeUnit.MINUTES ) ).isTrue();

        streamConsumer.close();
    }

    @Test
    public void test3() throws Exception
    {
        QueueSynchronizer<String> sync = new QueueSynchronizer<>( 2 );
        sync.pushNext( "1" );
        sync.pushNext( "2" );
        //sync.pushNext( "3" );
        String s1 = sync.awaitNext();
        String s2 = sync.awaitNext();
        //String s3 = sync.awaitNext();
    }

    static class QueueSynchronizer<T>
    {
        private final AtomicInteger queueSize = new AtomicInteger();

        QueueSynchronizer( int max )
        {
            this.max = max;
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
                return queueSize.get() < max ? 1 : -1;
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

        private final SyncT1 t1 = new SyncT1();
        private final SyncT2 t2 = new SyncT2();
        private final ConcurrentLinkedDeque<T> queue = new ConcurrentLinkedDeque<>();
        private final int max;

        void pushNext( T t )
        {
            queue.addLast( t );
            int previousCount = queueSize.get();
            t2.awaitMax();
            queueSize.incrementAndGet();
            if ( previousCount == 0 )
            {
                t1.release();
            }
        }

        T awaitNext() throws InterruptedException
        {
            t2.tryRelease();
            t1.waitIfZero();
            queueSize.decrementAndGet();
            return queue.pollFirst();
        }
    }
}
