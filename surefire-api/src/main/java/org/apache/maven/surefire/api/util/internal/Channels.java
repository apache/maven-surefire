package org.apache.maven.surefire.api.util.internal;

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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

/**
 * Converts {@link OutputStream}, {@link java.io.PrintStream}, {@link InputStream} to the Java {@link
 * java.nio.channels.Channel}.
 * <br>
 * We do not use the Java's utility class {@link java.nio.channels.Channels} because the utility closes the stream as
 * soon as the particular Thread is interrupted. If the frameworks (Zookeeper, Netty) interrupts the thread, the
 * communication channels become closed and the JVM hangs. Therefore we developed internal utility which is safe for the
 * Surefire.
 *
 * @since 3.0.0-M5
 */
public final class Channels
{
    private static final int BUFFER_SIZE = 64 * 1024;

    private Channels()
    {
        throw new IllegalStateException( "no instantiable constructor" );
    }

    public static WritableByteChannel newChannel( @Nonnull OutputStream out )
    {
        return newChannel( out, 0 );
    }

    public static WritableBufferedByteChannel newBufferedChannel( @Nonnull OutputStream out )
    {
        return newChannel( out, BUFFER_SIZE );
    }

    public static ReadableByteChannel newChannel( @Nonnull final InputStream is )
    {
        return newChannel( is, 0 );
    }

    public static ReadableByteChannel newBufferedChannel( @Nonnull final InputStream is )
    {
        return newChannel( is, BUFFER_SIZE );
    }

    public static OutputStream newOutputStream( final AsynchronousByteChannel channel )
    {
        return new OutputStream()
        {
            @Override
            public synchronized void write( byte[] b, int off, int len ) throws IOException
            {
                if ( off < 0 || off > b.length || len < 0 || off + len > b.length || off + len < 0 )
                {
                    throw new IndexOutOfBoundsException(
                        "b.length = " + b.length + ", off = " + off + ", len = " + len );
                }
                else if ( len > 0 )
                {
                    ByteBuffer bb = ByteBuffer.wrap( b, off, len );
                    while ( bb.hasRemaining() )
                    {
                        try
                        {
                            channel.write( bb ).get();
                        }
                        catch ( ExecutionException e )
                        {
                            Throwable t = e.getCause();
                            throw t instanceof IOException
                                ? (IOException) t
                                : new IOException( ( t == null ? e : t ).getLocalizedMessage(), t );
                        }
                        catch ( Exception e )
                        {
                            throw new IOException( e.getLocalizedMessage(), e );
                        }
                    }
                }
            }

            @Override
            public void write( int b ) throws IOException
            {
                write( new byte[] {(byte) b} );
            }

            @Override
            public synchronized void close() throws IOException
            {
                if ( channel.isOpen() )
                {
                    try
                    {
                        channel.close();
                    }
                    catch ( ClosedChannelException e )
                    {
                        // closed channel anyway
                    }
                }
            }
        };
    }

    public static InputStream newInputStream( final AsynchronousByteChannel channel )
    {
        return new InputStream()
        {
            @Override
            public synchronized int read( byte[] b, int off, int len ) throws IOException
            {
                if ( off < 0 || off > b.length || len < 0 || off + len > b.length || off + len < 0 )
                {
                    throw new IndexOutOfBoundsException(
                        "b.length = " + b.length + ", off = " + off + ", len = " + len );
                }
                else if ( len == 0 )
                {
                    return 0;
                }
                ByteBuffer bb = ByteBuffer.wrap( b, off, len );
                try
                {
                    return channel.read( bb ).get();
                }
                catch ( ExecutionException e )
                {
                    Throwable t = e.getCause();
                    throw t instanceof IOException
                        ? (IOException) t
                        : new IOException( ( t == null ? e : t ).getLocalizedMessage(), t );
                }
                catch ( Exception e )
                {
                    throw new IOException( e.getLocalizedMessage(), e );
                }
            }

            @Override
            public int read() throws IOException
            {
                int count;
                byte[] b = new byte[1];
                do
                {
                    count = read( b, 0, 1 );
                }
                while ( count == 0 );

                return count == -1 ? -1 : b[0];
            }

            @Override
            public synchronized void close() throws IOException
            {
                if ( channel.isOpen() )
                {
                    try
                    {
                        channel.close();
                    }
                    catch ( ClosedChannelException e )
                    {
                        // closed channel anyway
                    }
                }
            }
        };
    }

    private static ReadableByteChannel newChannel( @Nonnull InputStream is, @Nonnegative int bufferSize )
    {
        requireNonNull( is, "the stream should not be null" );
        final InputStream bis = bufferSize == 0 ? is : new BufferedInputStream( is, bufferSize );

        return new AbstractNoninterruptibleReadableChannel()
        {
            @Override
            protected int readImpl( ByteBuffer src ) throws IOException
            {
                int count = bis.read( src.array(), src.arrayOffset() + ( (Buffer) src ).position(), src.remaining() );
                if ( count > 0 )
                {
                    ( (Buffer) src ).position( count + ( (Buffer) src ).position() );
                }
                return count;
            }

            @Override
            protected void closeImpl() throws IOException
            {
                bis.close();
            }
        };
    }

    private static WritableBufferedByteChannel newChannel( @Nonnull OutputStream out,
                                                           @Nonnegative final int bufferSize )
    {
        requireNonNull( out, "the stream should not be null" );
        final OutputStream bos = bufferSize == 0 ? out : new BufferedOutputStream( out, bufferSize );

        return new AbstractNoninterruptibleWritableChannel()
        {
            private final AtomicLong bytesCounter = new AtomicLong();

            @Override
            public long countBufferOverflows()
            {
                return bufferSize == 0 ? 0 : max( bytesCounter.get() - 1, 0 ) / bufferSize;
            }

            @Override
            protected void writeImpl( ByteBuffer src ) throws IOException
            {
                int count = src.remaining();
                bos.write( src.array(), src.arrayOffset() + ( (Buffer) src ).position(), count );
                bytesCounter.getAndAdd( count );
            }

            @Override
            protected void closeImpl() throws IOException
            {
                bos.close();
            }

            @Override
            protected void flushImpl() throws IOException
            {
                bos.flush();
            }
        };
    }
}
