package org.apache.maven.plugin.surefire.report;

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

import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.maven.surefire.api.util.internal.StringUtils.NL;

/**
 * A deferred file output stream decorator that recodes the bytes written into the stream from the VM default encoding
 * to UTF-8.
 *
 * @author Andreas Gudian
 */
final class Utf8RecodingDeferredFileOutputStream
{
    private static final byte[] NL_BYTES = NL.getBytes( UTF_8 );
    public static final int CACHE_SIZE = 64 * 1024;

    private final String channel;
    private Path file;
    private RandomAccessFile storage;
    private boolean closed;
    private SoftReference<byte[]> largeCache;
    private ByteBuffer cache;
    private boolean isDirty;

    Utf8RecodingDeferredFileOutputStream( String channel )
    {
        this.channel = requireNonNull( channel );
    }

    public synchronized void write( String output, boolean newLine )
        throws IOException
    {
        if ( closed )
        {
            return;
        }

        if ( storage == null )
        {
            file = Files.createTempFile( channel, "deferred" );
            storage = new RandomAccessFile( file.toFile(), "rw" );
        }

        if ( output == null )
        {
            output = "null";
        }

        if ( cache == null )
        {
            cache = ByteBuffer.allocate( CACHE_SIZE );
        }

        isDirty = true;

        byte[] decodedString = output.getBytes( UTF_8 );
        int newLineLength = newLine ? NL_BYTES.length : 0;
        if ( cache.remaining() >= decodedString.length + newLineLength )
        {
            cache.put( decodedString );
            if ( newLine )
            {
                cache.put( NL_BYTES );
            }
        }
        else
        {
            ( (Buffer) cache ).flip();
            int minLength = cache.remaining() + decodedString.length + NL_BYTES.length;
            byte[] buffer = getLargeCache( minLength );
            int bufferLength = 0;
            System.arraycopy( cache.array(), cache.arrayOffset() + ( (Buffer) cache ).position(),
                buffer, bufferLength, cache.remaining() );
            bufferLength += cache.remaining();
            ( (Buffer) cache ).clear();

            System.arraycopy( decodedString, 0, buffer, bufferLength, decodedString.length );
            bufferLength += decodedString.length;

            if ( newLine )
            {
                System.arraycopy( NL_BYTES, 0, buffer, bufferLength, NL_BYTES.length );
                bufferLength += NL_BYTES.length;
            }

            storage.write( buffer, 0, bufferLength );
        }
    }

    public synchronized long getByteCount()
    {
        try
        {
            long length = 0;
            if ( storage != null )
            {
                sync();
                length = storage.length();
            }
            return length;
        }
        catch ( IOException e )
        {
            return 0;
        }
    }

    @SuppressWarnings( "checkstyle:innerassignment" )
    public synchronized void writeTo( OutputStream out )
        throws IOException
    {
        if ( storage != null )
        {
            sync();
            storage.seek( 0L );
            byte[] buffer = new byte[CACHE_SIZE];
            for ( int readCount; ( readCount = storage.read( buffer ) ) != -1; )
            {
                out.write ( buffer, 0, readCount );
            }
        }
    }

    public synchronized void free()
    {
        if ( !closed )
        {
            closed = true;
            if ( cache != null )
            {
                try
                {
                    sync();
                    storage.close();
                    Files.delete( file );
                }
                catch ( IOException e )
                {
                    file.toFile()
                        .deleteOnExit();
                }
            }
        }
    }

    private void sync() throws IOException
    {
        if ( !isDirty )
        {
            return;
        }

        isDirty = false;

        ( (Buffer) cache ).flip();
        byte[] array = cache.array();
        int offset = cache.arrayOffset() + ( (Buffer) cache ).position();
        int length = cache.remaining();
        ( (Buffer) cache ).clear();
        storage.write( array, offset, length );
        // the data that you wrote with the mode "rw" may still only be kept in memory and may be read back
        // storage.getFD().sync();
    }

    @SuppressWarnings( "checkstyle:innerassignment" )
    private byte[] getLargeCache( int minLength )
    {
        byte[] buffer;
        if ( largeCache == null || ( buffer = largeCache.get() ) == null || buffer.length < minLength )
        {
            buffer = new byte[minLength];
            largeCache = new SoftReference<>( buffer );
        }
        return buffer;
    }
}
