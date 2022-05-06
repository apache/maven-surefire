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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.nio.Buffer;
import java.nio.ByteBuffer;

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
    /** Output file lazily initialized on first write. */
    private File file;
    private RandomAccessFile storage;
    private boolean closed;
    private SoftReference<byte[]> largeCache;
    private ByteBuffer cache;
    private boolean isDirty;

    Utf8RecodingDeferredFileOutputStream( String channel )
    {
        this.channel = requireNonNull( channel );
    }

    /**
     * Writes the specified output to the stream unless {@code closed}.
     *
     * @param output output to write
     * @param newLine whether to append a newline sequence after output
     * @throws IOException if temporary file cannot be created/opened or write operation fails
     */
    public synchronized void write( String output, boolean newLine )
        throws IOException
    {
        if ( closed )
        {
            return;
        }

        if ( storage == null )
        {
            file = File.createTempFile( channel, "deferred" );
            file.deleteOnExit();
            storage = new RandomAccessFile( file, "rw" );
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
            if ( storage != null )
            {
                sync();
                return storage.length();
            }
            else if ( file != null )
            {
                return file.length();
            }
            return 0;
        }
        catch ( IOException e )
        {
            return 0;
        }
    }

    /**
     * Writes contents of the temporary file to an output stream.<br>
     * This operation is always permitted (even if this stream is {@code closed}.
     *
     * @param out output stream
     * @throws IOException if reading from temp file or writing to output stream fails
     */
    @SuppressWarnings( "checkstyle:innerassignment" )
    public synchronized void writeTo( OutputStream out )
        throws IOException
    {
        try ( RandomAccessFile f = new RandomAccessFile( file, "r" ) )
        {
            byte[] buffer = new byte[CACHE_SIZE];
            for ( int readCount; ( readCount = f.read( buffer ) ) != -1; )
            {
                out.write ( buffer, 0, readCount );
            }
        }
    }

    /**
     * Synchronizes the cache to file and closes the file.<br>
     * This stream is marked as {@code closed} and cannot be written to further.
     */
    public synchronized void free()
    {
        closed = true;
        if ( file == null )
        {
            return;
        }

        // file was written at least once (file != null)
        try
        {
            if ( cache != null )
            {
                sync(); // synchronize cache to file
                ( (Buffer) cache ).clear();
                cache = null;
            }
            if ( storage != null )
            {
                storage.close();
                storage = null;
            }
        }
        catch ( IOException e )
        {
            storage = null;
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

    @Override
    public String toString()
    {
        return getClass().getSimpleName()
                + String.format( "[file=%s, closed=%s]", file, closed );
    }

}
