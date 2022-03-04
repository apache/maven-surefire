package org.apache.maven.surefire.stream;

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

import org.apache.maven.surefire.api.booter.MasterProcessCommand;
import org.apache.maven.surefire.api.stream.AbstractStreamEncoder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import static org.apache.maven.surefire.api.booter.Constants.DEFAULT_STREAM_ENCODING;
import static org.apache.maven.surefire.api.booter.Constants.DEFAULT_STREAM_ENCODING_BYTES;
import static org.apache.maven.surefire.api.booter.Constants.MAGIC_NUMBER_FOR_COMMANDS_BYTES;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.BYE_ACK;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.NOOP;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.RUN_CLASS;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.SHUTDOWN;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.SKIP_SINCE_NEXT_TEST;
import static org.apache.maven.surefire.api.booter.MasterProcessCommand.TEST_SET_FINISHED;

/**
 *
 */
public class CommandEncoder extends AbstractStreamEncoder<MasterProcessCommand> implements AutoCloseable
{
    private final WritableByteChannel out;

    public CommandEncoder( WritableByteChannel out )
    {
        super( out );
        this.out = out;
    }

    public void sendRunClass( String testClassName ) throws IOException
    {
        CharsetEncoder encoder = newCharsetEncoder();
        int bufferMaxLength = estimateBufferLength( RUN_CLASS.getOpcodeLength(), null, encoder, 0, 0, testClassName );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );
        encode( encoder, result, RUN_CLASS, testClassName );
        write( result, true );
    }

    public void sendTestSetFinished() throws IOException
    {
        int bufferMaxLength = estimateBufferLength( TEST_SET_FINISHED.getOpcodeLength(), null, null, 0, 0 );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );
        encodeHeader( result, TEST_SET_FINISHED );
        write( result, true );
    }

    public void sendSkipSinceNextTest() throws IOException
    {
        int bufferMaxLength = estimateBufferLength( SKIP_SINCE_NEXT_TEST.getOpcodeLength(), null, null, 0, 0 );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );
        encodeHeader( result, SKIP_SINCE_NEXT_TEST );
        write( result, true );
    }

    public void sendShutdown( String shutdownData ) throws IOException
    {
        CharsetEncoder encoder = newCharsetEncoder();
        int bufferMaxLength = estimateBufferLength( SHUTDOWN.getOpcodeLength(), null, encoder, 0, 0, shutdownData );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );
        encode( encoder, result, SHUTDOWN, shutdownData );
        write( result, true );
    }

    public void sendNoop() throws IOException
    {
        int bufferMaxLength = estimateBufferLength( NOOP.getOpcodeLength(), null, null, 0, 0 );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );
        encodeHeader( result, NOOP );
        write( result, true );
    }

    public void sendByeAck() throws IOException
    {
        int bufferMaxLength = estimateBufferLength( BYE_ACK.getOpcodeLength(), null, null, 0, 0 );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );
        encodeHeader( result, BYE_ACK );
        write( result, true );
    }

    @Nonnull
    @Override
    protected final byte[] getEncodedMagicNumber()
    {
        return MAGIC_NUMBER_FOR_COMMANDS_BYTES;
    }

    @Nonnull
    @Override
    protected final byte[] enumToByteArray( MasterProcessCommand masterProcessCommand )
    {
        return masterProcessCommand.getOpcodeBinary();
    }

    @Nonnull
    @Override
    protected final byte[] getEncodedCharsetName()
    {
        return DEFAULT_STREAM_ENCODING_BYTES;
    }

    @Nonnull
    @Override
    protected final Charset getCharset()
    {
        return DEFAULT_STREAM_ENCODING;
    }

    @Nonnull
    @Override
    protected final CharsetEncoder newCharsetEncoder()
    {
        return getCharset().newEncoder();
    }

    @Override
    public void close() throws IOException
    {
        out.close();
    }
}
