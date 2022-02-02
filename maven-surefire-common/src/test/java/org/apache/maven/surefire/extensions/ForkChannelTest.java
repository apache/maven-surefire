package org.apache.maven.surefire.extensions;

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

import org.apache.maven.plugin.surefire.booterclient.MockReporter;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.TestLessInputStream;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.TestLessInputStream.TestLessInputStreamBuilder;
import org.apache.maven.plugin.surefire.extensions.SurefireForkNodeFactory;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.event.ControlByeEvent;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class ForkChannelTest
{
    private static final long TESTCASE_TIMEOUT = 30_000L;

    private final AtomicBoolean hasError = new AtomicBoolean();

    @Test( timeout = TESTCASE_TIMEOUT )
    public void shouldRequestReplyMessagesViaTCP() throws Exception
    {
        final MockReporter reporter = new MockReporter();
        final String sessionId = UUID.randomUUID().toString();
        ForkNodeArguments forkNodeArguments = new ForkNodeArguments()
        {
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

            @Nonnull
            @Override
            public String getSessionId()
            {
                return sessionId;
            }

            @Override
            public int getForkChannelId()
            {
                return 1;
            }

            @Override
            @Nonnull
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

            @Override
            @Nonnull
            public ConsoleLogger getConsoleLogger()
            {
                return reporter;
            }

            @Nonnull
            @Override
            public Object getConsoleLock()
            {
                return reporter;
            }
        };

        ForkNodeFactory factory = new SurefireForkNodeFactory();
        try ( ForkChannel channel = factory.createForkChannel( forkNodeArguments ) )
        {
            assertThat( channel.getArguments().getForkChannelId() )
                .isEqualTo( 1 );

            assertThat( channel.getCountdownCloseablePermits() )
                .isEqualTo( 3 );

            String localHost = InetAddress.getLoopbackAddress().getHostAddress();
            assertThat( channel.getForkNodeConnectionString() )
                .startsWith( "tcp://" + localHost + ":" )
                .isNotEqualTo( "tcp://" + localHost + ":" )
                .endsWith( "?sessionId=" + sessionId );

            URI uri = new URI( channel.getForkNodeConnectionString() );

            assertThat( uri.getPort() )
                .isPositive();

            final TestLessInputStreamBuilder builder = new TestLessInputStreamBuilder();
            TestLessInputStream commandReader = builder.build();
            final CountDownLatch isCloseableCalled = new CountDownLatch( 1 );
            Closeable closeable = new Closeable()
            {
                @Override
                public void close()
                {
                    isCloseableCalled.countDown();
                }
            };
            CountdownCloseable cc = new CountdownCloseable( closeable, 2 );
            Consumer consumer = new Consumer();

            Client client = new Client( uri.getPort(), sessionId );
            client.start();

            channel.tryConnectToClient();
            channel.bindCommandReader( commandReader, null );
            ReadableByteChannel stdOut = mock( ReadableByteChannel.class );
            when( stdOut.read( any( ByteBuffer.class ) ) ).thenReturn( -1 );
            channel.bindEventHandler( consumer, cc, stdOut );

            commandReader.noop();

            client.join( TESTCASE_TIMEOUT );

            assertThat( hasError.get() )
                .isFalse();

            assertThat( isCloseableCalled.await( TESTCASE_TIMEOUT, MILLISECONDS ) )
                .isTrue();

            assertThat( reporter.getEvents() )
                .describedAs( "The decoder captured the list of stream errors: " + reporter.getData().toString() )
                .isEmpty();

            assertThat( consumer.lines )
                .hasSize( 1 );

            assertThat( consumer.lines.element() )
                .isInstanceOf( ControlByeEvent.class );
        }
    }

    private static class Consumer implements EventHandler<Event>
    {
        final Queue<Event> lines = new ConcurrentLinkedQueue<>();

        @Override
        public void handleEvent( @Nonnull Event s )
        {
            lines.add( s );
        }
    }

    private final class Client extends Thread
    {
        private final int port;
        private final String sessionId;

        private Client( int port, String sessionId )
        {
            this.port = port;
            this.sessionId = sessionId;
        }

        @Override
        public void run()
        {
            try ( Socket socket = new Socket( InetAddress.getLoopbackAddress().getHostAddress(), port ) )
            {
                socket.getOutputStream().write( sessionId.getBytes( US_ASCII ) );
                byte[] data = new byte[128];
                int readLength = socket.getInputStream().read( data );
                String token = new String( data, 0, readLength, US_ASCII );
                assertThat( token ).isEqualTo( ":maven-surefire-command:\u0004:noop:" );
                socket.getOutputStream().write( ":maven-surefire-event:\u0003:bye:".getBytes( US_ASCII ) );
            }
            catch ( IOException e )
            {
                hasError.set( true );
                e.printStackTrace();
                throw new IllegalStateException( e );
            }
            catch ( RuntimeException e )
            {
                hasError.set( true );
                e.printStackTrace();
                throw e;
            }
        }
    }
}
