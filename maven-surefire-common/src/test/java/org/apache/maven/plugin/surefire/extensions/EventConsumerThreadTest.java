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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.copyOfRange;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class EventConsumerThreadTest
{
    @SuppressWarnings( "checkstyle:magicnumber" )
    @Test( timeout = 60_000L )
    public void performanceTest() throws Exception
    {
        final long[] staredAt = {0};
        final long[] finishedAt = {0};
        final AtomicInteger calls = new AtomicInteger();
        final int totalCalls = 1_000_000; // 400_000; // 1_000_000; // 10_000_000;

        EventHandler<Event> handler = new EventHandler<Event>()
        {
            @Override
            public void handleEvent( @Nonnull Event event )
            {
                if ( staredAt[0] == 0 )
                {
                    staredAt[0] = System.currentTimeMillis();
                }

                if ( calls.incrementAndGet() == totalCalls )
                {
                    finishedAt[0] = System.currentTimeMillis();
                }
            }
        };

        final ByteBuffer event = ByteBuffer.allocate( 192 );
        event.put( ":maven-surefire-event:".getBytes( UTF_8 ) );
        event.put( (byte) 14 );
        event.put( ":std-out-stream:".getBytes( UTF_8 ) );
        event.put( (byte) 10 );
        event.put( ":normal-run:".getBytes( UTF_8 ) );
        event.put( "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001:".getBytes( UTF_8 ) );
        event.put( (byte) 5 );
        event.put( ":UTF-8:".getBytes( UTF_8 ) );
        event.putInt( 100 );
        event.put(
            ":0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789:"
                .getBytes( UTF_8 ) );

        ( (Buffer) event ).flip();
        byte[] frame = copyOfRange( event.array(), event.arrayOffset(), event.arrayOffset() + event.remaining() );
        ReadableByteChannel channel = new Channel( frame, 100 )
        {
            private int countRounds;

            @Override
            public synchronized int read( ByteBuffer dst )
            {
                if ( countRounds == totalCalls )
                {
                    return -1;
                }

                if ( remaining() == 0 )
                {
                    countRounds++;
                    i = 0;
                }

                return super.read( dst );
            }
        };

        EventConsumerThread thread = new EventConsumerThread( "t", channel, handler,
            new CountdownCloseable( new MockCloseable(), 1 ), new MockForkNodeArguments() );

        SECONDS.sleep( 2 );
        System.gc();
        SECONDS.sleep( 5 );

        System.out.println( "Starting the event thread..." );

        thread.start();
        thread.join();

        long execTime = finishedAt[0] - staredAt[0];
        System.out.println( execTime );

        // 0.6 seconds while using the encoder/decoder for 10 million messages
        assertThat( execTime )
            .describedAs( "The performance test should assert 0.75s of read time. "
                + "The limit 3.65s guarantees that the read time does not exceed this limit on overloaded CPU." )
            .isPositive()
            .isLessThanOrEqualTo( 3_650L );
    }

    private static class Channel implements ReadableByteChannel
    {
        private final byte[] bytes;
        private final int chunkSize;
        protected int i;

        Channel( byte[] bytes, int chunkSize )
        {
            this.bytes = bytes;
            this.chunkSize = chunkSize;
        }

        @Override
        public int read( ByteBuffer dst )
        {
            if ( remaining() == 0 )
            {
                return -1;
            }
            else if ( dst.hasRemaining() )
            {
                int length = min( min( chunkSize, remaining() ), dst.remaining() ) ;
                dst.put( bytes, i, length );
                i += length;
                return length;
            }
            else
            {
                return 0;
            }
        }

        protected final int remaining()
        {
            return bytes.length - i;
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

    private static class MockCloseable implements Closeable
    {
        @Override
        public void close()
        {
        }
    }

    private static class MockForkNodeArguments implements ForkNodeArguments
    {
        @Nonnull
        @Override
        public String getSessionId()
        {
            return null;
        }

        @Override
        public int getForkChannelId()
        {
            return 0;
        }

        @Nonnull
        @Override
        public File dumpStreamText( @Nonnull String text )
        {
            return null;
        }

        @Nonnull
        @Override
        public File dumpStreamException( @Nonnull Throwable t )
        {
            return null;
        }

        @Override
        public void logWarningAtEnd( @Nonnull String text )
        {
        }

        @Nonnull
        @Override
        public ConsoleLogger getConsoleLogger()
        {
            return null;
        }

        @Nonnull
        @Override
        public Object getConsoleLock()
        {
            return new Object();
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
