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

import org.apache.commons.io.output.DeferredFileOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

import static org.apache.maven.surefire.util.internal.StringUtils.UTF_8;

/**
 * A deferred file output stream decorator that recodes the bytes written into the stream from the VM default encoding
 * to UTF-8.
 *
 * @author Andreas Gudian
 */
class Utf8RecodingDeferredFileOutputStream
{
    private DeferredFileOutputStream deferredFileOutputStream;

    private boolean closed = false;

    @SuppressWarnings( "checkstyle:magicnumber" )
    Utf8RecodingDeferredFileOutputStream( String channel )
    {
        this.deferredFileOutputStream = new DeferredFileOutputStream( 1000000, channel, "deferred", null );
    }

    public synchronized void write( byte[] buf, int off, int len )
        throws IOException
    {
        if ( closed )
        {
            return;
        }

        if ( !Charset.defaultCharset().equals( UTF_8 ) )
        {
            CharBuffer decodedFromDefaultCharset = Charset.defaultCharset().decode( ByteBuffer.wrap( buf, off, len ) );
            ByteBuffer utf8Encoded = UTF_8.encode( decodedFromDefaultCharset );

            if ( utf8Encoded.hasArray() )
            {
                byte[] convertedBytes = utf8Encoded.array();

                deferredFileOutputStream.write( convertedBytes, utf8Encoded.position(), utf8Encoded.remaining() );
            }
            else
            {
                byte[] convertedBytes = new byte[utf8Encoded.remaining()];
                utf8Encoded.get( convertedBytes, 0, utf8Encoded.remaining() );

                deferredFileOutputStream.write( convertedBytes, 0, convertedBytes.length );
            }
        }
        else
        {
            deferredFileOutputStream.write( buf, off, len );
        }
    }

    public long getByteCount()
    {
        return deferredFileOutputStream.getByteCount();
    }

    public synchronized void close()
        throws IOException
    {
        closed = true;
        deferredFileOutputStream.close();
    }

    public synchronized void writeTo( OutputStream out )
        throws IOException
    {
        if ( closed )
        {
            deferredFileOutputStream.writeTo( out );
        }
    }

    public synchronized void free()
    {
        if ( null != deferredFileOutputStream && null != deferredFileOutputStream.getFile() )
        {
            try
            {
                closed = true;
                deferredFileOutputStream.close();
                if ( !deferredFileOutputStream.getFile().delete() )
                {
                    deferredFileOutputStream.getFile().deleteOnExit();
                }
            }
            catch ( IOException ioe )
            {
                deferredFileOutputStream.getFile().deleteOnExit();

            }
        }
    }
}
