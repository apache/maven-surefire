package org.apache.maven.surefire.extensions.util;

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
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import static org.apache.maven.surefire.api.util.internal.Channels.newBufferedChannel;
import static org.apache.maven.surefire.api.util.internal.Channels.newChannel;

/**
 *
 */
public final class CommandlineStreams implements Closeable
{
    private final ReadableByteChannel stdOutChannel;
    private final ReadableByteChannel stdErrChannel;
    private final WritableByteChannel stdInChannel;
    private volatile boolean closed;

    public CommandlineStreams( @Nonnull Process process )
    {
        InputStream stdOutStream = process.getInputStream();
        stdOutChannel = newBufferedChannel( stdOutStream );

        InputStream stdErrStream = process.getErrorStream();
        stdErrChannel = newBufferedChannel( stdErrStream );

        stdInChannel = newChannel( process.getOutputStream() );
    }

    public ReadableByteChannel getStdOutChannel()
    {
        return stdOutChannel;
    }

    public ReadableByteChannel getStdErrChannel()
    {
        return stdErrChannel;
    }

    public WritableByteChannel getStdInChannel()
    {
        return stdInChannel;
    }

    @Override
    public void close() throws IOException
    {
        if ( closed )
        {
            return;
        }

        try ( Channel c1 = stdOutChannel;
              Channel c2 = stdErrChannel;
              Channel c3 = stdInChannel )
        {
            closed = true;
        }
        catch ( ClosedChannelException e )
        {
            // already closed externally
        }
    }
}
