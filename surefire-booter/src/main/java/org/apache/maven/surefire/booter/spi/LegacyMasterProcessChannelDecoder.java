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
import org.apache.maven.surefire.api.booter.DumpErrorSingleton;
import org.apache.maven.surefire.api.booter.MasterProcessCommand;
import org.apache.maven.surefire.api.booter.MasterProcessChannelDecoder;
import org.apache.maven.surefire.api.util.internal.ImmutableMap;

import javax.annotation.Nonnull;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.maven.surefire.api.booter.MasterProcessCommand.BYE_ACK;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.MAGIC_NUMBER;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.NOOP;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.RUN_CLASS;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.SHUTDOWN;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.SKIP_SINCE_NEXT_TEST;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.TEST_SET_FINISHED;

/**
 * magic number : opcode [: opcode specific data]*
 * <br>
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M5
 */
public class LegacyMasterProcessChannelDecoder implements MasterProcessChannelDecoder
{
    private static final Map<String, MasterProcessCommand> COMMAND_OPCODES = stringsToOpcodes();

    private final ReadableByteChannel channel;

    public LegacyMasterProcessChannelDecoder( @Nonnull ReadableByteChannel channel )
    {
        this.channel = channel;
    }

    @Override
    @Nonnull
    @SuppressWarnings( "checkstyle:innerassignment" )
    public Command decode() throws IOException
    {
        List<String> tokens = new ArrayList<>( 3 );
        StringBuilder token = new StringBuilder( MAGIC_NUMBER.length() );
        ByteBuffer buffer = ByteBuffer.allocate( 1 );

        start:
        do
        {
            boolean endOfStream;
            tokens.clear();
            token.setLength( 0 );
            FrameCompletion completion = null;
            for ( boolean frameStarted = false; !( endOfStream = channel.read( buffer ) == -1 ); completion = null )
            {
                buffer.flip();
                char c = (char) buffer.get();
                buffer.clear();

                if ( !frameStarted )
                {
                    if ( c == ':' )
                    {
                        frameStarted = true;
                        token.setLength( 0 );
                        tokens.clear();
                    }
                }
                else
                {
                    if ( c == ':' )
                    {
                        tokens.add( token.toString() );
                        token.setLength( 0 );
                        completion = frameCompleteness( tokens );
                        if ( completion == FrameCompletion.COMPLETE )
                        {
                            break;
                        }
                        else if ( completion == FrameCompletion.MALFORMED )
                        {
                            DumpErrorSingleton.getSingleton()
                                .dumpStreamText( "Malformed frame with tokens " + tokens );
                            continue start;
                        }
                    }
                    else
                    {
                        token.append( c );
                    }
                }
            }

            if ( completion == FrameCompletion.COMPLETE )
            {
                MasterProcessCommand cmd = COMMAND_OPCODES.get( tokens.get( 1 ) );
                if ( tokens.size() == 2 )
                {
                    return new Command( cmd );
                }
                else if ( tokens.size() == 3 )
                {
                    return new Command( cmd, tokens.get( 2 ) );
                }
            }

            if ( endOfStream )
            {
                throw new EOFException();
            }
        }
        while ( true );
    }

    private static FrameCompletion frameCompleteness( List<String> tokens )
    {
        if ( !tokens.isEmpty() && !MAGIC_NUMBER.equals( tokens.get( 0 ) ) )
        {
            return FrameCompletion.MALFORMED;
        }

        if ( tokens.size() >= 2 )
        {
            String opcode = tokens.get( 1 );
            MasterProcessCommand cmd = COMMAND_OPCODES.get( opcode );
            if ( cmd == null )
            {
                return FrameCompletion.MALFORMED;
            }
            else if ( cmd.hasDataType() == ( tokens.size() == 3 ) )
            {
                return FrameCompletion.COMPLETE;
            }
        }
        return FrameCompletion.NOT_COMPLETE;
    }

    @Override
    public void close()
    {
    }

    /**
     * Determines whether the frame is complete or malformed.
     */
    private enum FrameCompletion
    {
        NOT_COMPLETE,
        COMPLETE,
        MALFORMED
    }

    private static Map<String, MasterProcessCommand> stringsToOpcodes()
    {
        Map<String, MasterProcessCommand> opcodes = new HashMap<>();
        opcodes.put( "run-testclass", RUN_CLASS );
        opcodes.put( "testset-finished", TEST_SET_FINISHED );
        opcodes.put( "skip-since-next-test", SKIP_SINCE_NEXT_TEST );
        opcodes.put( "shutdown", SHUTDOWN );
        opcodes.put( "noop", NOOP );
        opcodes.put( "bye-ack", BYE_ACK );
        return new ImmutableMap<>( opcodes );
    }
}
