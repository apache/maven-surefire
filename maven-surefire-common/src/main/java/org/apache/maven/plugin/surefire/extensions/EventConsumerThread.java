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

import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.api.stream.AbstractStreamDecoder.Memento;
import org.apache.maven.surefire.extensions.CloseableDaemonThread;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.apache.maven.surefire.stream.EventDecoder;

import javax.annotation.Nonnull;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;

/**
 *
 */
public class EventConsumerThread extends CloseableDaemonThread
{
    private final ReadableByteChannel channel;
    private final EventHandler<Event> eventHandler;
    private final CountdownCloseable countdownCloseable;
    private final EventDecoder decoder;
    private final ForkNodeArguments arguments;
    private volatile boolean disabled;

    public EventConsumerThread( @Nonnull String threadName,
                                @Nonnull ReadableByteChannel channel,
                                @Nonnull EventHandler<Event> eventHandler,
                                @Nonnull CountdownCloseable countdownCloseable,
                                @Nonnull ForkNodeArguments arguments )
    {
        super( threadName );
        decoder = new EventDecoder( channel, arguments );
        this.channel = channel;
        this.eventHandler = eventHandler;
        this.countdownCloseable = countdownCloseable;
        this.arguments = arguments;
    }

    @Override
    public void run()
    {
        try ( ReadableByteChannel stream = channel;
              CountdownCloseable c = countdownCloseable;
              EventDecoder eventDecoder = decoder )
        {
            Memento memento = eventDecoder.new Memento();
            do
            {
                Event event = eventDecoder.decode( memento );
                if ( event != null && !disabled )
                {
                    eventHandler.handleEvent( event );
                }
            }
            while ( true );
        }
        catch ( EOFException | ClosedChannelException e )
        {
            //
        }
        catch ( IOException e )
        {
            if ( e instanceof InterruptedIOException || e.getCause() instanceof InterruptedException )
            {
                Thread.currentThread().interrupt();
            }
            else
            {
                arguments.dumpStreamException( e );
            }
        }
    }

    @Override
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
