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

import javax.annotation.Nonnull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.maven.plugin.surefire.booterclient.output.ThreadedStreamConsumer;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.surefire.api.booter.Command;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.api.report.OutputReportEntry;
import org.apache.maven.surefire.api.report.TestOutputReceiver;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.booter.spi.EventChannelEncoder;
import org.apache.maven.surefire.booter.spi.SurefireMasterProcessChannelProcessorFactory;
import org.apache.maven.surefire.extensions.CommandReader;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.report.TestOutputReportEntry.stdOutln;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

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
        server.tryConnectToClient();

        final String connection = server.getForkNodeConnectionString();

        final SurefireMasterProcessChannelProcessorFactory factory = new SurefireMasterProcessChannelProcessorFactory();
        factory.connect( connection );
        final EventChannelEncoder encoder = (EventChannelEncoder) factory.createEncoder( arguments );

        final CountDownLatch awaitHandlerFinished = new CountDownLatch( 2 );

        final AtomicLong readTime = new AtomicLong();

        final int totalCalls = 400_000; // 400_000; // 1_000_000; // 10_000_000;

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

        EventHandler<Event> queue = new ThreadedStreamConsumer( h );

        System.gc();

        SECONDS.sleep( 5L );

        server.bindEventHandler( queue, new CountdownCloseable( new DummyCloseable(), 1 ), new DummyReadableChannel() );
        server.bindCommandReader( new DummyCommandReader(), null );

        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                TestOutputReceiver<OutputReportEntry> target = new TestOutputReceiver()
                {
                    @Override
                    public void writeTestOutput( OutputReportEntry reportEntry )
                    {
                        encoder.testOutput( stdOutln( reportEntry.getLog() ) );
                    }
                };

                //PrintStream out = System.out;
                //PrintStream err = System.err;

                //ConsoleOutputCapture.startCapture( target );

                try
                {
                    long t1 = System.currentTimeMillis();
                    for ( int i = 0; i < totalCalls; i++ )
                    {
                        //System.out.println( LONG_STRING );
                        encoder.testOutput( new TestOutputReportEntry( stdOutln( LONG_STRING ), NORMAL_RUN, 1L ) );
                    }
                    long t2 = System.currentTimeMillis();
                    long spent = t2 - t1;
                    //System.setOut( out );
                    //System.setErr( err );
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

        assertThat( awaitHandlerFinished.await( 30L, SECONDS ) )
            .isTrue();

        factory.close();
        server.close();
        //queue.close();

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
        ForkNodeArguments forkNodeArguments =
            new Arguments( UUID.randomUUID().toString(), 1, new NullConsoleLogger() );

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

            assertThat( task.get() )
                .isEqualTo( "client connected" );
        }
    }

    @Test( timeout = 10_000L )
    public void shouldNotVerifyClient() throws Exception
    {
        ForkNodeArguments forkNodeArguments =
            new Arguments( UUID.randomUUID().toString(), 1, new NullConsoleLogger() );

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
                + forkNodeArguments.getSessionId() + "'." );

            server.tryConnectToClient();
            server.bindCommandReader( new DummyCommandReader(), new DummyWritableByteChannel() );

            server.bindEventHandler( new DummyEventHandler(),
                new CountdownCloseable( new DummyCloseable(), 1 ), new DummyReadableChannel() );

            fail( task.get() );
        }
    }

    private static class DummyEventHandler<Event> implements EventHandler<Event>
    {
        @Override
        public void handleEvent( @Nonnull Event event )
        {
        }
    }

    private static class DummyReadableChannel implements ReadableByteChannel
    {
        private volatile Thread t;

        @Override
        public int read( ByteBuffer dst ) throws IOException
        {
            try
            {
                t = Thread.currentThread();
                HOURS.sleep( 1L );
                return 0;
            }
            catch ( InterruptedException e )
            {
                throw new IOException( e.getLocalizedMessage(), e );
            }
        }

        @Override
        public boolean isOpen()
        {
            return true;
        }

        @Override
        public void close()
        {
            if ( t != null )
            {
                t.interrupt();
            }
        }
    }

    private static class DummyWritableByteChannel implements WritableByteChannel
    {
        private volatile Thread t;

        @Override
        public int write( ByteBuffer src ) throws IOException
        {
            try
            {
                t = Thread.currentThread();
                HOURS.sleep( 1L );
                return 0;
            }
            catch ( InterruptedException e )
            {
                throw new IOException( e.getLocalizedMessage(), e );
            }
        }

        @Override
        public boolean isOpen()
        {
            return true;
        }

        @Override
        public void close() throws IOException
        {
            if ( t != null )
            {
                t.interrupt();
            }
        }
    }

    private static class DummyCommandReader implements CommandReader
    {
        private volatile Thread t;

        @Override
        public Command readNextCommand() throws IOException
        {
            try
            {
                t = Thread.currentThread();
                HOURS.sleep( 1L );
                return null;
            }
            catch ( InterruptedException e )
            {
                throw new IOException( e.getLocalizedMessage(), e );
            }
        }

        @Override
        public void close()
        {
            if ( t != null )
            {
                t.interrupt();
            }
        }

        @Override
        public boolean isClosed()
        {
            return false;
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

        @Nonnull
        @Override
        public Object getConsoleLock()
        {
            return logger;
        }

        @Override
        public File getEventStreamBinaryFile()
        {
            return null;
        }

        @Override
        public File getCommandStreamBinaryFile()
        {
            return null;
        }
    }
}
