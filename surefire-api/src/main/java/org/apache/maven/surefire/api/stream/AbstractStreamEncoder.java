package org.apache.maven.surefire.api.stream;

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

import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.util.internal.WritableBufferedByteChannel;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

import static java.lang.Math.ceil;
import static java.nio.CharBuffer.wrap;

/**
 * The base class of stream encoder.
 * The type of message is expressed by opcode where the opcode object is described by the generic type {@link E}.
 * @param <E> type of the message
 */
public abstract class AbstractStreamEncoder<E extends Enum<E>>
{
    private static final byte BOOLEAN_NON_NULL_OBJECT = (byte) 0xff;
    private static final byte BOOLEAN_NULL_OBJECT = (byte) 0;
    private static final byte[] INT_BINARY = new byte[] {0, 0, 0, 0};

    private final WritableByteChannel out;

    public AbstractStreamEncoder( WritableByteChannel out )
    {
        this.out = out;
    }

    @Nonnull
    protected abstract byte[] getEncodedMagicNumber();

    @Nonnull
    protected abstract byte[] enumToByteArray( E e );

    @Nonnull
    protected abstract byte[] getEncodedCharsetName();

    @Nonnull
    protected abstract Charset getCharset();

    @Nonnull
    protected abstract CharsetEncoder newCharsetEncoder();

    protected void write( ByteBuffer frame, boolean sendImmediately )
        throws IOException
    {
        if ( !sendImmediately && out instanceof WritableBufferedByteChannel )
        {
            ( (WritableBufferedByteChannel) out ).writeBuffered( frame );
        }
        else
        {
            out.write( frame );
        }
    }

    public void encodeHeader( ByteBuffer result, E operation, RunMode runMode )
    {
        result.put( (byte) ':' );
        result.put( getEncodedMagicNumber() );
        result.put( (byte) ':' );
        byte[] opcode = enumToByteArray( operation );
        result.put( (byte) opcode.length );
        result.put( (byte) ':' );
        result.put( opcode );
        result.put( (byte) ':' );

        if ( runMode != null )
        {
            byte[] runmode = runMode.getRunmodeBinary();
            result.put( (byte) runmode.length );
            result.put( (byte) ':' );
            result.put( runmode );
            result.put( (byte) ':' );
        }
    }

    public void encodeCharset( ByteBuffer result )
    {
        byte[] charsetNameBinary = getEncodedCharsetName();
        result.put( (byte) charsetNameBinary.length );
        result.put( (byte) ':' );
        result.put( charsetNameBinary );
        result.put( (byte) ':' );
    }

    public void encodeString( CharsetEncoder encoder, ByteBuffer result, String string )
    {
        String nonNullString = nonNull( string );

        int counterPosition = result.position();

        result.put( INT_BINARY ).put( (byte) ':' );

        int msgStart = result.position();
        encoder.encode( wrap( nonNullString ), result, true );
        int msgEnd = result.position();
        int encodedMsgSize = msgEnd - msgStart;
        result.putInt( counterPosition, encodedMsgSize );

        result.position( msgEnd );

        result.put( (byte) ':' );
    }

    public void encodeInteger( ByteBuffer result, Integer i )
    {
        if ( i == null )
        {
            result.put( BOOLEAN_NULL_OBJECT );
        }
        else
        {
            result.put( BOOLEAN_NON_NULL_OBJECT ).putInt( i );
        }
        result.put( (byte) ':' );
    }

    public void encode( CharsetEncoder encoder, ByteBuffer result, E operation, RunMode runMode, String... messages )
    {
        encodeHeader( result, operation, runMode );
        encodeCharset( result );
        for ( String message : messages )
        {
            encodeString( encoder, result, message );
        }
    }

    public int estimateBufferLength( int opcodeLength, RunMode runMode, CharsetEncoder encoder,
                                     int integersCounter, String... strings )
    {
        assert !( encoder == null && strings.length != 0 );

        // one delimiter character ':' + <string> + one delimiter character ':' +
        // one byte + one delimiter character ':' + <string> + one delimiter character ':'
        int lengthOfMetadata = 1 + getEncodedMagicNumber().length + 1 + 1 + 1 + opcodeLength + 1;

        if ( runMode != null )
        {
            // one byte of length + one delimiter character ':' + <string> + one delimiter character ':'
            lengthOfMetadata += 1 + 1 + runMode.geRunmode().length() + 1;
        }

        if ( encoder != null )
        {
            // one byte of length + one delimiter character ':' + <string> + one delimiter character ':'
            lengthOfMetadata += 1 + 1 + encoder.charset().name().length() + 1;
        }

        // one byte (0x00 if NULL) + 4 bytes for integer + one delimiter character ':'
        int lengthOfData = ( 1 + 1 + 4 ) * integersCounter;

        for ( String string : strings )
        {
            String s = nonNull( string );
            // 4 bytes of string length + one delimiter character ':' + <string> + one delimiter character ':'
            lengthOfData += 4 + 1 + (int) ceil( encoder.maxBytesPerChar() * s.length() ) + 1;
        }

        return lengthOfMetadata + lengthOfData;
    }

    private static String nonNull( String msg )
    {
        return msg == null ? "\u0000" : msg;
    }
}
