package org.apache.maven.surefire.booter.spi;

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

import org.apache.maven.surefire.api.booter.Command;
import org.apache.maven.surefire.api.booter.MasterProcessChannelDecoder;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.api.stream.AbstractStreamDecoder.Memento;
import org.apache.maven.surefire.api.stream.MalformedChannelException;
import org.apache.maven.surefire.booter.stream.CommandDecoder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * magic number : opcode [: opcode specific data]*
 * <br>
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M5
 */
public class CommandChannelDecoder implements MasterProcessChannelDecoder
{
    private final CommandDecoder decoder;
    private Memento memento;

    public CommandChannelDecoder( @Nonnull ReadableByteChannel channel,
                                  @Nonnull ForkNodeArguments arguments )
    {
        decoder = new CommandDecoder( channel, arguments );
    }

    @Override
    @Nonnull
    @SuppressWarnings( "checkstyle:innerassignment" )
    public Command decode() throws IOException
    {
        if ( memento == null )
        {
            // do not create memento in constructor because the constructor is called in another thread
            // memento is the thread confinement object
            memento = decoder.new Memento();
        }

        do
        {
            try
            {
                Command command = decoder.decode( memento );
                if ( command != null )
                {
                    return command;
                }
            }
            catch ( MalformedChannelException e )
            {
                // a bad stream, already logged the stream down to a dump file or console, and continue till OEF
            }
        }
        while ( true );
    }

    @Override
    public void close() throws IOException
    {
        decoder.close();
    }
}
