package org.apache.maven.surefire.booter.stream;

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
import org.apache.maven.surefire.api.booter.MasterProcessCommand;
import org.apache.maven.surefire.api.booter.Shutdown;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.api.stream.AbstractStreamDecoder;
import org.apache.maven.surefire.api.stream.MalformedChannelException;
import org.apache.maven.surefire.api.stream.SegmentType;

import javax.annotation.Nonnull;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.FutureTask;

import static org.apache.maven.surefire.api.booter.Command.BYE_ACK;
import static org.apache.maven.surefire.api.booter.Command.NOOP;
import static org.apache.maven.surefire.api.booter.Command.SKIP_SINCE_NEXT_TEST;
import static org.apache.maven.surefire.api.booter.Command.TEST_SET_FINISHED;
import static org.apache.maven.surefire.api.booter.Command.toRunClass;
import static org.apache.maven.surefire.api.booter.Command.toShutdown;
import static org.apache.maven.surefire.api.booter.Constants.MAGIC_NUMBER_FOR_COMMANDS_BYTES;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.COMMAND_TYPES;
import static org.apache.maven.surefire.api.stream.SegmentType.DATA_STRING;
import static org.apache.maven.surefire.api.stream.SegmentType.END_OF_FRAME;
import static org.apache.maven.surefire.api.stream.SegmentType.STRING_ENCODING;
import static org.apache.maven.surefire.shared.utils.cli.ShutdownHookUtils.addShutDownHook;

/**
 *
 */
public class CommandDecoder extends AbstractStreamDecoder<Command, MasterProcessCommand, SegmentType>
{
    private static final int DEBUG_SINK_BUFFER_SIZE = 64 * 1024;
    private static final int NO_POSITION = -1;

    private static final SegmentType[] COMMAND_WITHOUT_DATA = new SegmentType[] {
        END_OF_FRAME
    };

    private static final SegmentType[] COMMAND_WITH_ONE_STRING = new SegmentType[] {
        STRING_ENCODING,
        DATA_STRING,
        END_OF_FRAME
    };

    private final ForkNodeArguments arguments;
    private final OutputStream debugSink;

    public CommandDecoder( @Nonnull ReadableByteChannel channel,
                           @Nonnull ForkNodeArguments arguments )
    {
        super( channel, arguments, COMMAND_TYPES );
        this.arguments = arguments;
        debugSink = newDebugSink();
    }

    @Override
    public Command decode( @Nonnull Memento memento ) throws IOException, MalformedChannelException
    {
        try
        {
            MasterProcessCommand commandType = readMessageType( memento );
            if ( commandType == null )
            {
                throw new MalformedFrameException( memento.getLine().getPositionByteBuffer(),
                    memento.getByteBuffer().position() );
            }

            for ( SegmentType segmentType : nextSegmentType( commandType ) )
            {
                switch ( segmentType )
                {
                    case STRING_ENCODING:
                        memento.setCharset( readCharset( memento ) );
                        break;
                    case DATA_STRING:
                        memento.getData().add( readString( memento ) );
                        break;
                    case DATA_INTEGER:
                        memento.getData().add( readInteger( memento ) );
                        break;
                    case END_OF_FRAME:
                        memento.getLine().setPositionByteBuffer( memento.getByteBuffer().position() );
                        memento.getLine().clear();
                        return toMessage( commandType, memento );
                    default:
                        memento.getLine().setPositionByteBuffer( NO_POSITION );
                        arguments.dumpStreamText( "Unknown enum ("
                            + SegmentType.class.getSimpleName()
                            + ") "
                            + segmentType );
                }
            }
        }
        catch ( MalformedFrameException e )
        {
            if ( e.hasValidPositions() )
            {
                int length = e.readTo() - e.readFrom();
                memento.getLine().write( memento.getByteBuffer(), e.readFrom(), length );
            }
            return null;
        }
        catch ( RuntimeException e )
        {
            getArguments().dumpStreamException( e );
            return null;
        }
        catch ( IOException e )
        {
            if ( !( e.getCause() instanceof InterruptedException ) )
            {
                printRemainingStream( memento );
            }
            throw e;
        }
        finally
        {
            memento.reset();
        }

        throw new MalformedChannelException();
    }

    @Nonnull
    @Override
    protected final byte[] getEncodedMagicNumber()
    {
        return MAGIC_NUMBER_FOR_COMMANDS_BYTES;
    }

    @Nonnull
    @Override
    protected SegmentType[] nextSegmentType( @Nonnull MasterProcessCommand commandType )
    {
        switch ( commandType )
        {
            case NOOP:
            case BYE_ACK:
            case SKIP_SINCE_NEXT_TEST:
            case TEST_SET_FINISHED:
                return COMMAND_WITHOUT_DATA;
            case RUN_CLASS:
            case SHUTDOWN:
                return COMMAND_WITH_ONE_STRING;
            default:
                throw new IllegalArgumentException( "Unknown enum " + commandType );
        }
    }

    @Nonnull
    @Override
    protected Command toMessage( @Nonnull MasterProcessCommand commandType, @Nonnull Memento memento )
        throws MalformedFrameException
    {
        switch ( commandType )
        {
            case NOOP:
                checkArguments( memento, 0 );
                return NOOP;
            case BYE_ACK:
                checkArguments( memento, 0 );
                return BYE_ACK;
            case SKIP_SINCE_NEXT_TEST:
                checkArguments( memento, 0 );
                return SKIP_SINCE_NEXT_TEST;
            case TEST_SET_FINISHED:
                checkArguments( memento, 0 );
                return TEST_SET_FINISHED;
            case RUN_CLASS:
                checkArguments( memento, 1 );
                return toRunClass( (String) memento.getData().get( 0 ) );
            case SHUTDOWN:
                checkArguments( memento, 1 );
                return toShutdown( Shutdown.parameterOf( (String) memento.getData().get( 0 ) ) );
            default:
                throw new IllegalArgumentException( "Missing a branch for the event type " + commandType );
        }
    }

    @Override
    protected void debugStream( byte[] array, int position, int remaining )
    {
        if ( debugSink == null )
        {
            return;
        }

        try
        {
            debugSink.write( array, position, remaining );
        }
        catch ( IOException e )
        {
            // logger file was deleted
            // System.out is already used by the stream in this decoder
        }
    }

    private OutputStream newDebugSink()
    {
        final File sink = arguments.getCommandStreamBinaryFile();
        if ( sink == null )
        {
            return null;
        }

        try
        {
            OutputStream fos = new FileOutputStream( sink, true );
            final OutputStream os = new BufferedOutputStream( fos, DEBUG_SINK_BUFFER_SIZE );
            addShutDownHook( new Thread( new FutureTask<>( () ->
            {
                os.close();
                return null;
            } ) ) );
            return os;
        }
        catch ( FileNotFoundException e )
        {
            return null;
        }
    }

    @Override
    public void close() throws IOException
    {
        if ( debugSink != null )
        {
            debugSink.close();
        }
    }
}
