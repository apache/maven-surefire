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

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;

/**
 * The channel used for writes which cannot be implicitly closed after the operational Thread
 * is {@link Thread#isInterrupted() interrupted}.
 *
 * @since 3.0.0-M5
 */
abstract class AbstractNoninterruptibleWritableChannel implements WritableBufferedByteChannel
{
    private volatile boolean open = true;

    protected abstract void writeImpl( ByteBuffer src ) throws IOException;
    protected abstract void closeImpl() throws IOException;
    protected abstract void flushImpl() throws IOException;

    @Override
    public final int write( ByteBuffer src ) throws IOException
    {
        return write( src, true );
    }

    @Override
    public final void writeBuffered( ByteBuffer src ) throws IOException
    {
        write( src, false );
    }

    int write( ByteBuffer src, boolean flush ) throws IOException
    {
        if ( !isOpen() )
        {
            throw new ClosedChannelException();
        }

        if ( !src.hasArray() || src.isReadOnly() )
        {
            throw new NonWritableChannelException();
        }

        if ( src.remaining() != src.capacity() )
        {
            ( (Buffer) src ).flip();
        }

        int countWrittenBytes = src.remaining();
        writeImpl( src );
        ( (Buffer) src ).position( ( (Buffer) src ).limit() );
        if ( flush )
        {
            flushImpl();
        }
        return countWrittenBytes;
    }

    @Override
    public final boolean isOpen()
    {
        return open;
    }

    @Override
    public final void close() throws IOException
    {
        open = false;
        closeImpl();
    }
}
