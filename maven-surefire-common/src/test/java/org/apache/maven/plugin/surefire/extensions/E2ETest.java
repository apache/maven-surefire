package org.apache.maven.plugin.surefire.extensions;

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

import org.apache.maven.plugin.surefire.booterclient.output.ThreadedStreamConsumer;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.surefire.api.booter.MasterProcessChannelEncoder;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.report.ConsoleOutputCapture;
import org.apache.maven.surefire.api.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.booter.spi.SurefireMasterProcessChannelProcessorFactory;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.extensions.ForkNodeArguments;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Simulates the End To End use case where Maven process and Surefire process communicate using the TCP/IP protocol.
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class E2ETest
{
    private static final String LONG_STRING =
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

    @Rule
    public final ExpectedException e = ExpectedException.none();

    @Test
    public void endToEndTest() throws Exception
    {
        ForkNodeArguments arguments = new Arguments( UUID.randomUUID().toString(), 1, new NullConsoleLogger() );
        final SurefireForkChannel server = new SurefireForkChannel( arguments );

        final String connection = server.getForkNodeConnectionString();

        final SurefireMasterProcessChannelProcessorFactory factory = new SurefireMasterProcessChannelProcessorFactory();
        factory.connect( connection );
        final MasterProcessChannelEncoder encoder = factory.createEncoder();

        System.gc();

        TimeUnit.SECONDS.sleep( 3L );

        final CountDownLatch awaitHandlerFinished = new CountDownLatch( 2 );

        final int totalCalls = 400_000; // 400_000; // 1_000_000; // 10_000_000;

        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                ConsoleOutputReceiver target = new ConsoleOutputReceiver()
                {
                    @Override
                    public void writeTestOutput( String output, boolean newLine, boolean stdout )
                    {
                        encoder.stdOut( output, true );
                    }
                };

                PrintStream out = System.out;
                PrintStream err = System.err;

                ConsoleOutputCapture.startCapture( target );

                try
                {
                    long t1 = System.currentTimeMillis();
                    for ( int i = 0; i < totalCalls; i++ )
                    {
                        System.out.println( LONG_STRING );
                        //encoder.stdOut( LONG_STRING, true );
                    }
                    long t2 = System.currentTimeMillis();
                    long spent = t2 - t1;
                    System.setOut( out );
                    System.setErr( err );
                    System.out.println( spent + "ms on write" );
                    awaitHandlerFinished.countDown();
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }
        };
        t.setDaemon( true );
        t.start();

        server.connectToClient();

        final AtomicLong readTime = new AtomicLong();

        EventHandler<Event> h = new EventHandler<Event>()
        {
            private final AtomicInteger counter = new AtomicInteger();
            private volatile long t1;

            @Override
            public void handleEvent( @Nonnull Event event )
            {
                try
                {
                    if ( counter.getAndIncrement() == 0 )
                    {
                        t1 = System.currentTimeMillis();
                    }

                    long t2 = System.currentTimeMillis();
                    long spent = t2 - t1;

                    if ( counter.get() % 500_000 == 0 )
                    {
                        System.out.println( spent + "ms: " + counter.get() );
                    }

                    if ( counter.get() == totalCalls - 64 * 1024 )
                    {
                        readTime.set( spent );
                        System.out.println( spent + "ms on read" );
                        awaitHandlerFinished.countDown();
                    }
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }
        };

        ThreadedStreamConsumer queue = new ThreadedStreamConsumer( h );

        server.bindEventHandler( queue, new CountdownCloseable( new DummyCloseable(), 1 ), new DummyReadableChannel() )
            .start();

        assertThat( awaitHandlerFinished.await( 30L, TimeUnit.SECONDS ) )
            .isTrue();

        factory.close();
        server.close();
        queue.close();

        // 1.0 seconds while using the encoder/decoder
        assertThat( readTime.get() )
            .describedAs( "The performance test should assert 1.0s of read time. "
                + "The limit 6s guarantees that the read time does not exceed this limit on overloaded CPU." )
            .isPositive()
            .isLessThanOrEqualTo( 6_000L );
    }

    @Test( timeout = 10_000L )
    public void shouldVerifyClient() throws Exception
    {
        ForkNodeArguments forkNodeArguments = mock( ForkNodeArguments.class );
        when( forkNodeArguments.getSessionId() ).thenReturn( UUID.randomUUID().toString() );

        try ( SurefireForkChannel server = new SurefireForkChannel( forkNodeArguments );
              SurefireMasterProcessChannelProcessorFactory client = new SurefireMasterProcessChannelProcessorFactory() )
        {
            FutureTask<String> task = new FutureTask<>( new Callable<String>()
            {
                @Override
                public String call() throws Exception
                {
                    client.connect( server.getForkNodeConnectionString() );
                    return "client connected";
                }
            } );

            Thread t = new Thread( task );
            t.setDaemon( true );
            t.start();

            server.connectToClient();

            assertThat( task.get() )
                .isEqualTo( "client connected" );
        }
    }

    @Test( timeout = 10_000L )
    public void shouldNotVerifyClient() throws Exception
    {
        ForkNodeArguments forkNodeArguments = mock( ForkNodeArguments.class );
        String serverSessionId = UUID.randomUUID().toString();
        when( forkNodeArguments.getSessionId() ).thenReturn( serverSessionId );

        try ( SurefireForkChannel server = new SurefireForkChannel( forkNodeArguments );
              SurefireMasterProcessChannelProcessorFactory client = new SurefireMasterProcessChannelProcessorFactory() )
        {
            FutureTask<String> task = new FutureTask<>( new Callable<String>()
            {
                @Override
                public String call() throws Exception
                {
                    URI connectionUri = new URI( server.getForkNodeConnectionString() );
                    client.connect( "tcp://" + connectionUri.getHost() + ":" + connectionUri.getPort()
                        + "?sessionId=6ba7b812-9dad-11d1-80b4-00c04fd430c8" );
                    return "client connected";
                }
            } );

            Thread t = new Thread( task );
            t.setDaemon( true );
            t.start();

            e.expect( InvalidSessionIdException.class );
            e.expectMessage( "The actual sessionId '6ba7b812-9dad-11d1-80b4-00c04fd430c8' does not match '"
                + serverSessionId + "'." );

            server.connectToClient();

            fail( task.get() );
        }
    }

    private static class DummyReadableChannel implements ReadableByteChannel
    {
        @Override
        public int read( ByteBuffer dst )
        {
            return 0;
        }

        @Override
        public boolean isOpen()
        {
            return false;
        }

        @Override
        public void close()
        {
        }
    }

    private static class DummyCloseable implements Closeable
    {
        @Override
        public void close()
        {
        }
    }

    private static class Arguments implements ForkNodeArguments
    {
        private final String sessionId;
        private final int id;
        private final ConsoleLogger logger;

        private Arguments( String sessionId, int id, ConsoleLogger logger )
        {
            this.sessionId = sessionId;
            this.id = id;
            this.logger = logger;
        }

        @Nonnull
        @Override
        public String getSessionId()
        {
            return sessionId;
        }

        @Override
        public int getForkChannelId()
        {
            return id;
        }

        @Nonnull
        @Override
        public File dumpStreamText( @Nonnull String text )
        {
            return new File( "" );
        }

        @Nonnull
        @Override
        public File dumpStreamException( @Nonnull Throwable t )
        {
            return new File( "" );
        }

        @Override
        public void logWarningAtEnd( @Nonnull String text )
        {
        }

        @Nonnull
        @Override
        public ConsoleLogger getConsoleLogger()
        {
            return logger;
        }
    }
}
