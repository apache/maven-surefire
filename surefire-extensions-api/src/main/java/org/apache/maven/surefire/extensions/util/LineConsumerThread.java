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

import org.apache.maven.surefire.shared.utils.cli.StreamConsumer;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 *
 */
public final class LineConsumerThread extends Thread implements Closeable
{
    private final Charset encoding;
    private final ReadableByteChannel channel;
    private final StreamConsumer consumer;
    private final CountdownCloseable countdownCloseable;
    private volatile boolean disabled;

    public LineConsumerThread( @Nonnull String threadName,
                               @Nonnull ReadableByteChannel channel, @Nonnull StreamConsumer consumer,
                               @Nonnull CountdownCloseable countdownCloseable )
    {
        this( threadName, channel, consumer, countdownCloseable, Charset.defaultCharset() );
    }

    public LineConsumerThread( @Nonnull String threadName,
                               @Nonnull ReadableByteChannel channel, @Nonnull StreamConsumer consumer,
                               @Nonnull CountdownCloseable countdownCloseable, @Nonnull Charset encoding )
    {
        setName( threadName );
        setDaemon( true );
        this.channel = channel;
        this.consumer = consumer;
        this.countdownCloseable = countdownCloseable;
        this.encoding = encoding;
    }

    @Override
    public void run()
    {
        try ( Scanner stream = new Scanner( channel, encoding.name() );
              CountdownCloseable c = countdownCloseable; )
        {
            boolean isError = false;
            while ( stream.hasNextLine() )
            {
                try
                {
                    String line = stream.nextLine();
                    isError |= stream.ioException() != null;
                    if ( !isError && !disabled )
                    {
                        consumer.consumeLine( line );
                    }
                }
                catch ( IllegalStateException e )
                {
                    isError = true;
                }
            }
        }
        catch ( IllegalStateException | IOException e )
        {
            // not needed
        }
    }

    public void disable()
    {
        disabled = true;
    }

    @Override
    public void close() throws IOException
    {
        channel.close();
    }
}
