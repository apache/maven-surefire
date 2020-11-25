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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.booter.Command;
import org.apache.maven.surefire.extensions.CloseableDaemonThread;
import org.apache.maven.surefire.extensions.CommandReader;
import org.apache.maven.surefire.stream.CommandEncoder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.WritableByteChannel;

/**
 * Commands which are sent from plugin to the forked jvm.
 * <br>
 *     <br>
 * magic number : opcode [: opcode specific data]*
 * <br>
 *     or data encoded with Base64
 * <br>
 * magic number : opcode [: Base64(opcode specific data)]*
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M5
 */
public class StreamFeeder extends CloseableDaemonThread
{
    private final WritableByteChannel channel;
    private final CommandReader commandReader;
    private final ConsoleLogger logger;

    private volatile boolean disabled;
    private volatile Throwable exception;

    public StreamFeeder( @Nonnull String threadName, @Nonnull WritableByteChannel channel,
                         @Nonnull CommandReader commandReader, @Nonnull ConsoleLogger logger )
    {
        super( threadName );
        this.channel = channel;
        this.commandReader = commandReader;
        this.logger = logger;
    }

    @Override
    @SuppressWarnings( "checkstyle:innerassignment" )
    public void run()
    {
        try ( CommandEncoder encoder = new CommandEncoder( channel ) )
        {
            for ( Command cmd; ( cmd = commandReader.readNextCommand() ) != null; )
            {
                if ( !disabled )
                {
                    switch ( cmd.getCommandType() )
                    {
                        case RUN_CLASS:
                            encoder.sendRunClass( cmd.getData() );
                            break;
                        case TEST_SET_FINISHED:
                            encoder.sendTestSetFinished();
                            break;
                        case SKIP_SINCE_NEXT_TEST:
                            encoder.sendSkipSinceNextTest();
                            break;
                        case SHUTDOWN:
                            encoder.sendShutdown( cmd.getData() );
                            break;
                        case NOOP:
                            encoder.sendNoop();
                            break;
                        case BYE_ACK:
                            encoder.sendByeAck();
                            break;
                        default:
                            logger.error( "Unknown enum " + cmd.getCommandType().name() );
                    }
                }
            }
        }
        catch ( ClosedChannelException e )
        {
            // closed externally
        }
        catch ( IOException | NonWritableChannelException e )
        {
            exception = e.getCause() == null ? e : e.getCause();
        }
        catch ( IllegalArgumentException e )
        {
            logger.error( e.getLocalizedMessage() );
        }
    }

    public void disable()
    {
        disabled = true;
    }

    public Throwable getException()
    {
        return exception;
    }

    @Override
    public void close() throws IOException
    {
        channel.close();
    }
}
