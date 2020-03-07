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

import org.apache.maven.surefire.shared.io.output.DeferredFileOutputStream;

import java.io.IOException;
import java.io.OutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.surefire.util.internal.StringUtils.NL;

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
        deferredFileOutputStream = new DeferredFileOutputStream( 1000000, channel, "deferred", null );
    }

    public synchronized void write( String output, boolean newLine )
        throws IOException
    {
        if ( closed )
        {
            return;
        }

        if ( output == null )
        {
            output = "null";
        }

        deferredFileOutputStream.write( output.getBytes( UTF_8 ) );
        if ( newLine )
        {
            deferredFileOutputStream.write( NL.getBytes( UTF_8 ) );
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
