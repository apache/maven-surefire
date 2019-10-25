package org.apache.maven.plugin.surefire.booterclient.output;

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

import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.AbstractCommandReader;
import org.apache.maven.shared.utils.cli.CommandLineCallable;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.shared.utils.cli.StreamConsumer;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
final class PipeProcessExecutor
    implements ExecutableCommandline<String>
{
    @Override
    @Nonnull
    public CommandLineCallable executeCommandLineAsCallable( @Nonnull Commandline cli,
                                                             @Nonnull AbstractCommandReader commands,
                                                             @Nonnull EventHandler<String> events,
                                                             StreamConsumer stdOut,
                                                             StreamConsumer stdErr,
                                                             @Nonnull Runnable runAfterProcessTermination )
            throws CommandLineException
    {
        return CommandLineUtils.executeCommandLineAsCallable( cli, new CommandReaderAdapter( commands ),
                new EventHandlerAdapter( events ), stdErr, 0, runAfterProcessTermination, ISO_8859_1 );
    }

    private static class EventHandlerAdapter implements StreamConsumer
    {
        private final EventHandler<String> events;

        private EventHandlerAdapter( EventHandler<String> events )
        {
            this.events = events;
        }

        @Override
        public void consumeLine( String line )
        {
            events.handleEvent( line );
        }
    }

    private static class CommandReaderAdapter extends InputStream
    {
        private final AbstractCommandReader commands;

        private byte[] currentBuffer;
        private int currentPos;
        private volatile boolean closed;

        CommandReaderAdapter( AbstractCommandReader commands )
        {
            this.commands = commands;
        }

        @Override
        public int read() throws IOException
        {
            if ( commands.isClosed() )
            {
                close();
            }

            if ( closed )
            {
                return -1;
            }

            if ( currentBuffer == null )
            {
                currentBuffer = commands.readNextCommand();
                if ( currentBuffer == null )
                {
                    return -1;
                }
            }

            @SuppressWarnings( "checkstyle:magicnumber" )
            int b =  currentBuffer[currentPos++] & 0xff;
            if ( currentPos == currentBuffer.length )
            {
                currentBuffer = null;
                currentPos = 0;
            }
            return b;
        }

        @Override
        public void close()
        {
            closed = true;
        }
    }
}
