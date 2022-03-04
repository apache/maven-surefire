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

import org.apache.maven.plugin.surefire.booterclient.output.ThreadedStreamConsumer.QueueSynchronizer;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.event.StandardStreamOutWithNewLineEvent;
import org.apache.maven.surefire.extensions.EventHandler;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class ThreadedStreamConsumerTest
{
    @Test
    public void testQueueSynchronizer() throws Exception
    {
        final CountDownLatch countDown = new CountDownLatch( 5_000_000 );
        final QueueSynchronizer<Integer> sync = new QueueSynchronizer<>(  8 * 1024, null );

        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                while ( true )
                {
                    try
                    {
                        sync.awaitNext();
                        countDown.countDown();
                    }
                    catch ( InterruptedException e )
                    {
                        throw new IllegalStateException( e );
                    }
                }
            }
        };
        t.setDaemon( true );
        t.start();

        SECONDS.sleep( 1 );
        System.gc();
        SECONDS.sleep( 2 );

        long t1 = System.currentTimeMillis();

        for ( int i = 0; i < 5_000_000; i++ )
        {
            sync.pushNext( i );
        }

        assertThat( countDown.await( 3L, SECONDS ) )
            .isTrue();

        long t2 = System.currentTimeMillis();
        System.out.println( ( t2 - t1 ) + " millis in testQueueSynchronizer()" );
    }

    @Test
    public void testThreadedStreamConsumer() throws Exception
    {
        final CountDownLatch countDown = new CountDownLatch( 5_000_000 );
        EventHandler<Event> handler = new EventHandler<Event>()
        {
            @Override
            public void handleEvent( @Nonnull Event event )
            {
                countDown.countDown();
            }
        };

        ThreadedStreamConsumer streamConsumer = new ThreadedStreamConsumer( handler );

        SECONDS.sleep( 1 );
        System.gc();
        SECONDS.sleep( 2 );

        long t1 = System.currentTimeMillis();

        Event event = new StandardStreamOutWithNewLineEvent( NORMAL_RUN, 1L, "" );
        for ( int i = 0; i < 5_000_000; i++ )
        {
            streamConsumer.handleEvent( event );
        }

        assertThat( countDown.await( 3L, SECONDS ) )
            .isTrue();

        long t2 = System.currentTimeMillis();
        System.out.println( ( t2 - t1 ) + " millis in testThreadedStreamConsumer()" );

        streamConsumer.close();
    }

    @Test
    public void testBasicStatus() throws Exception
    {
        final QueueSynchronizer<String> sync = new QueueSynchronizer<>( 2, null );
        sync.pushNext( "1" );
        sync.pushNext( "2" );
        String s1 = sync.awaitNext();
        String s2 = sync.awaitNext();
        assertThat( s1 ).isEqualTo( "1" );
        assertThat( s2 ).isEqualTo( "2" );
        FutureTask<Void> future = new FutureTask<>( new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                sync.awaitNext();
                return null;
            }
        } );
        Thread t = new Thread( future );
        t.setDaemon( true );
        t.start();
        SECONDS.sleep( 3L );
        assertThat( t.getState() )
            .isEqualTo( Thread.State.WAITING );
    }
}
