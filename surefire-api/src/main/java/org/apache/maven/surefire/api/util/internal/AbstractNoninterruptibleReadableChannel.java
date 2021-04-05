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
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.ReadableByteChannel;

/**
 * The channel used for reads which cannot be implicitly closed after the operational Thread
 * is {@link Thread#isInterrupted() interrupted}.
 *
 * @since 3.0.0-M5
 */
abstract class AbstractNoninterruptibleReadableChannel implements ReadableByteChannel
{
    private volatile boolean open = true;

    protected abstract int readImpl( ByteBuffer src ) throws IOException;
    protected abstract void closeImpl() throws IOException;

    @Override
    public final int read( ByteBuffer src ) throws IOException
    {
        if ( !isOpen() )
        {
            throw new ClosedChannelException();
        }

        if ( !src.hasArray() || src.isReadOnly() )
        {
            throw new NonReadableChannelException();
        }

        return src.hasRemaining() ? readImpl( src ) : 0;
    }

    @Override
    public final boolean isOpen()
    {
        return open;
    }

    @Override
    public final void close() throws IOException
    {
        try
        {
            closeImpl();
        }
        finally
        {
            open = false;
        }
    }
}
