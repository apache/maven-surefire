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
import org.apache.maven.surefire.api.booter.MasterProcessCommand;
import org.apache.maven.surefire.extensions.CloseableDaemonThread;
import org.apache.maven.surefire.extensions.CommandReader;
import org.apache.maven.surefire.api.util.internal.ImmutableMap;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.BYE_ACK;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.MAGIC_NUMBER;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.NOOP;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.RUN_CLASS;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.SHUTDOWN;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.SKIP_SINCE_NEXT_TEST;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.TEST_SET_FINISHED;

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
    private static final Map<MasterProcessCommand, String> COMMAND_OPCODES = opcodesToStrings();

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
        try ( WritableByteChannel c = channel )
        {
            for ( Command cmd; ( cmd = commandReader.readNextCommand() ) != null; )
            {
                if ( !disabled )
                {
                    MasterProcessCommand cmdType = cmd.getCommandType();
                    byte[] data = cmdType.hasDataType() ? encode( cmdType, cmd.getData() ) : encode( cmdType );
                    c.write( ByteBuffer.wrap( data ) );
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

    /**
     * Public method for testing purposes.
     *
     * @param cmdType command type
     * @param data data to encode
     * @return command with data encoded to bytes
     */
    public static byte[] encode( MasterProcessCommand cmdType, String data )
    {
        if ( !cmdType.hasDataType() )
        {
            throw new IllegalArgumentException( "cannot use data without data type" );
        }

        if ( cmdType.getDataType() != String.class )
        {
            throw new IllegalArgumentException( "Data type can be only " + String.class );
        }

        return encode( COMMAND_OPCODES.get( cmdType ), data )
            .toString()
            .getBytes( US_ASCII );
    }

    /**
     * Public method for testing purposes.
     *
     * @param cmdType command type
     * @return command without data encoded to bytes
     */
    public static byte[] encode( MasterProcessCommand cmdType )
    {
        if ( cmdType.getDataType() != Void.class )
        {
            throw new IllegalArgumentException( "Data type can be only " + cmdType.getDataType() );
        }

        return encode( COMMAND_OPCODES.get( cmdType ), null )
            .toString()
            .getBytes( US_ASCII );
    }

    /**
     * Encodes opcode and data.
     *
     * @param operation opcode
     * @param data   data
     * @return encoded command
     */
    private static StringBuilder encode( String operation, String data )
    {
        StringBuilder s = new StringBuilder( 128 )
            .append( ':' )
            .append( MAGIC_NUMBER )
            .append( ':' )
            .append( operation );

        if ( data != null )
        {
            s.append( ':' )
                .append( data );
        }

        return s.append( ':' );
    }

    private static Map<MasterProcessCommand, String> opcodesToStrings()
    {
        Map<MasterProcessCommand, String> opcodes = new HashMap<>();
        opcodes.put( RUN_CLASS, "run-testclass" );
        opcodes.put( TEST_SET_FINISHED, "testset-finished" );
        opcodes.put( SKIP_SINCE_NEXT_TEST, "skip-since-next-test" );
        opcodes.put( SHUTDOWN, "shutdown" );
        opcodes.put( NOOP, "noop" );
        opcodes.put( BYE_ACK, "bye-ack" );
        return new ImmutableMap<>( opcodes );
    }
}
