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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.nio.charset.CodingErrorAction.REPLACE;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Arrays.copyOf;
import static org.apache.maven.surefire.api.booter.Constants.DEFAULT_STREAM_ENCODING;
import static org.apache.maven.surefire.api.stream.AbstractStreamDecoder.StreamReadStatus.OVERFLOW;
import static org.apache.maven.surefire.api.stream.AbstractStreamDecoder.StreamReadStatus.UNDERFLOW;
import static org.apache.maven.surefire.shared.lang3.StringUtils.isBlank;

/**
 * @param <M> message object
 * @param <MT> enum describing the meaning of the message
 * @param <ST> enum for segment type
 */
public abstract class AbstractStreamDecoder<M, MT extends Enum<MT>, ST extends Enum<ST>> implements AutoCloseable
{
    public static final int BUFFER_SIZE = 1024;

    private static final String PRINTABLE_JVM_NATIVE_STREAM = "Listening for transport dt_socket at address:";

    private static final String[] JVM_ERROR_PATTERNS = {
        "could not create the java virtual machine", "error occurred during initialization", // of VM, of boot layer
        "error:", // general errors
        "could not reserve enough space", "could not allocate", "unable to allocate", // memory errors
        "java.lang.module.findexception" // JPMS errors
    };

    private static final byte[] DEFAULT_STREAM_ENCODING_BYTES = DEFAULT_STREAM_ENCODING.name().getBytes( US_ASCII );

    private static final int NO_POSITION = -1;
    private static final int DELIMITER_LENGTH = 1;
    private static final int BYTE_LENGTH = 1;
    private static final int INT_LENGTH = 4;
    private static final int LONG_LENGTH = 8;

    private final ReadableByteChannel channel;
    private final ForkNodeArguments arguments;
    private final Map<Segment, MT> messageTypes;
    private final ConsoleLogger logger;

    protected AbstractStreamDecoder( @Nonnull ReadableByteChannel channel,
                                     @Nonnull ForkNodeArguments arguments,
                                     @Nonnull Map<Segment, MT> messageTypes )
    {
        this.channel = channel;
        this.arguments = arguments;
        this.messageTypes = messageTypes;
        logger = arguments.getConsoleLogger();
    }

    public abstract M decode( @Nonnull Memento memento ) throws MalformedChannelException, IOException;

    @Nonnull
    protected abstract byte[] getEncodedMagicNumber();

    @Nonnull
    protected abstract ST[] nextSegmentType( @Nonnull MT messageType );

    @Nonnull
    protected abstract M toMessage( @Nonnull MT messageType, @Nonnull Memento memento )
        throws MalformedFrameException;

    @Nonnull
    protected final ForkNodeArguments getArguments()
    {
        return arguments;
    }

    protected void debugStream( byte[] array, int position, int remaining )
    {
    }

    protected MT readMessageType( @Nonnull Memento memento ) throws IOException, MalformedFrameException
    {
        byte[] header = getEncodedMagicNumber();
        int readCount = DELIMITER_LENGTH + header.length + DELIMITER_LENGTH + BYTE_LENGTH + DELIMITER_LENGTH;
        read( memento, readCount );
        checkHeader( memento );
        return messageTypes.get( readSegment( memento ) );
    }

    @Nonnull
    @SuppressWarnings( "checkstyle:magicnumber" )
    protected Segment readSegment( @Nonnull Memento memento ) throws IOException, MalformedFrameException
    {
        int readCount = readByte( memento ) & 0xff;
        read( memento, readCount + DELIMITER_LENGTH );
        ByteBuffer bb = memento.getByteBuffer();
        Segment segment = new Segment( bb.array(), bb.arrayOffset() + ( (Buffer) bb ).position(), readCount );
        ( (Buffer) bb ).position( ( (Buffer) bb ).position() + readCount );
        checkDelimiter( memento );
        return segment;
    }

    @Nonnull
    @SuppressWarnings( "checkstyle:magicnumber" )
    protected Charset readCharset( @Nonnull Memento memento ) throws IOException, MalformedFrameException
    {
        int length = readByte( memento ) & 0xff;
        read( memento, length + DELIMITER_LENGTH );
        ByteBuffer bb = memento.getByteBuffer();
        byte[] array = bb.array();
        int offset = bb.arrayOffset() + ( (Buffer) bb ).position();
        ( (Buffer) bb ).position( ( (Buffer) bb ).position() + length );
        boolean isDefaultEncoding = false;
        if ( length == DEFAULT_STREAM_ENCODING_BYTES.length )
        {
            isDefaultEncoding = true;
            for ( int i = 0; i < length; i++ )
            {
                isDefaultEncoding &= DEFAULT_STREAM_ENCODING_BYTES[i] == array[offset + i];
            }
        }

        try
        {
            Charset charset =
                isDefaultEncoding
                    ? DEFAULT_STREAM_ENCODING
                    : Charset.forName( new String( array, offset, length, US_ASCII ) );

            checkDelimiter( memento );
            return charset;
        }
        catch ( IllegalArgumentException e )
        {
            throw new MalformedFrameException( memento.getLine().getPositionByteBuffer(), ( (Buffer) bb ).position() );
        }
    }

    protected String readString( @Nonnull Memento memento ) throws IOException, MalformedFrameException
    {
        ( (Buffer) memento.getCharBuffer() ).clear();
        int readCount = readInt( memento );
        if ( readCount < 0 )
        {
            throw new MalformedFrameException( memento.getLine().getPositionByteBuffer(),
                ( (Buffer) memento.getByteBuffer() ).position() );
        }
        read( memento, readCount + DELIMITER_LENGTH );

        final String string;
        if ( readCount == 0 )
        {
            string = "";
        }
        else if ( readCount == 1 )
        {
            read( memento, 1 );
            byte oneChar = memento.getByteBuffer().get();
            string = oneChar == 0 ? null : String.valueOf( (char) oneChar );
        }
        else
        {
            string = readString( memento, readCount );
        }
        read( memento, 1 );
        checkDelimiter( memento );
        return string;
    }

    protected Integer readInteger( @Nonnull Memento memento ) throws IOException, MalformedFrameException
    {
        read( memento, BYTE_LENGTH );
        boolean isNullObject = memento.getByteBuffer().get() == 0;
        if ( isNullObject )
        {
            read( memento, DELIMITER_LENGTH );
            checkDelimiter( memento );
            return null;
        }
        return readInt( memento );
    }

    protected byte readByte( @Nonnull Memento memento ) throws IOException, MalformedFrameException
    {
        read( memento, BYTE_LENGTH + DELIMITER_LENGTH );
        byte b = memento.getByteBuffer().get();
        checkDelimiter( memento );
        return b;
    }

    protected int readInt( @Nonnull Memento memento ) throws IOException, MalformedFrameException
    {
        read( memento, INT_LENGTH + DELIMITER_LENGTH );
        int i = memento.getByteBuffer().getInt();
        checkDelimiter( memento );
        return i;
    }

    protected Long readLong( @Nonnull Memento memento ) throws IOException, MalformedFrameException
    {
        read( memento, BYTE_LENGTH );
        boolean isNullObject = memento.getByteBuffer().get() == 0;
        if ( isNullObject )
        {
            read( memento, DELIMITER_LENGTH );
            checkDelimiter( memento );
            return null;
        }
        return readLongPrivate( memento );
    }

    protected long readLongPrivate( @Nonnull Memento memento ) throws IOException, MalformedFrameException
    {
        read( memento, LONG_LENGTH + DELIMITER_LENGTH );
        long num = memento.getByteBuffer().getLong();
        checkDelimiter( memento );
        return num;
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    protected final void checkDelimiter( Memento memento ) throws MalformedFrameException
    {
        ByteBuffer bb = memento.bb;
        if ( ( 0xff & bb.get() ) != ':' )
        {
            throw new MalformedFrameException( memento.getLine().getPositionByteBuffer(), ( (Buffer) bb ).position() );
        }
    }

    protected final void checkHeader( Memento memento ) throws MalformedFrameException
    {
        ByteBuffer bb = memento.bb;

        checkDelimiter( memento );

        int shift = 0;
        try
        {
            byte[] header = getEncodedMagicNumber();
            byte[] bbArray = bb.array();
            for ( int start = bb.arrayOffset() + ( (Buffer) bb ).position(), length = header.length;
                  shift < length; shift++ )
            {
                if ( bbArray[shift + start] != header[shift] )
                {
                    throw new MalformedFrameException( memento.getLine().getPositionByteBuffer(),
                        ( (Buffer) bb ).position() + shift );
                }
            }
        }
        finally
        {
            ( (Buffer) bb ).position( ( (Buffer) bb ).position() + shift );
        }

        checkDelimiter( memento );
    }

    protected void checkArguments( Memento memento, int expectedDataElements )
        throws MalformedFrameException
    {
        if ( memento.getData().size() != expectedDataElements )
        {
            throw new MalformedFrameException( memento.getLine().getPositionByteBuffer(),
                ( (Buffer) memento.getByteBuffer() ).position() );
        }
    }

    private String readString( @Nonnull final Memento memento, @Nonnegative final int totalBytes )
        throws IOException, MalformedFrameException
    {
        memento.getDecoder().reset();
        final CharBuffer output = memento.getCharBuffer();
        ( (Buffer) output ).clear();
        final ByteBuffer input = memento.getByteBuffer();
        final List<String> strings = new ArrayList<>();
        int countDecodedBytes = 0;
        for ( boolean endOfInput = false; !endOfInput; )
        {
            final int bytesToRead = totalBytes - countDecodedBytes;
            read( memento, bytesToRead );
            int bytesToDecode = min( input.remaining(), bytesToRead );
            final boolean isLastChunk = bytesToDecode == bytesToRead;
            endOfInput = countDecodedBytes + bytesToDecode >= totalBytes;
            do
            {
                boolean endOfChunk = output.remaining() >= bytesToRead;
                boolean endOfOutput = isLastChunk && endOfChunk;
                int readInputBytes = decodeString( memento.getDecoder(), input, output, bytesToDecode, endOfOutput,
                    memento.getLine().getPositionByteBuffer() );
                bytesToDecode -= readInputBytes;
                countDecodedBytes += readInputBytes;
            }
            while ( isLastChunk && bytesToDecode > 0 && output.hasRemaining() );

            if ( isLastChunk || !output.hasRemaining() )
            {
                strings.add( ( (Buffer) output ).flip().toString() );
                ( (Buffer) output ).clear();
            }
        }

        memento.getDecoder().reset();
        ( (Buffer) output ).clear();

        return toString( strings );
    }

    private static int decodeString( @Nonnull CharsetDecoder decoder, @Nonnull ByteBuffer input,
                                     @Nonnull CharBuffer output, @Nonnegative int bytesToDecode,
                                     boolean endOfInput, @Nonnegative int errorStreamFrom )
        throws MalformedFrameException
    {
        int limit = ( (Buffer) input ).limit();
        ( (Buffer) input ).limit( ( (Buffer) input ).position() + bytesToDecode );

        CoderResult result = decoder.decode( input, output, endOfInput );
        if ( result.isError() || result.isMalformed() )
        {
            throw new MalformedFrameException( errorStreamFrom, ( (Buffer) input ).position() );
        }

        int decodedBytes = bytesToDecode - input.remaining();
        ( (Buffer) input ).limit( limit );
        return decodedBytes;
    }

    private static String toString( List<String> strings )
    {
        if ( strings.size() == 1 )
        {
            return strings.get( 0 );
        }
        StringBuilder concatenated = new StringBuilder( strings.size() * BUFFER_SIZE );
        for ( String s : strings )
        {
            concatenated.append( s );
        }
        return concatenated.toString();
    }

    private void printCorruptedStream( Memento memento )
    {
        ByteBuffer bb = memento.getByteBuffer();
        if ( bb.hasRemaining() )
        {
            int bytesToWrite = bb.remaining();
            memento.getLine().write( bb, ( (Buffer) bb ).position(), bytesToWrite );
            ( (Buffer) bb ).position( ( (Buffer) bb ).position() + bytesToWrite );
        }
    }

    /**
     * Print the last string which has not been finished by a new line character.
     *
     * @param memento current memento object
     */
    protected final void printRemainingStream( Memento memento )
    {
        printCorruptedStream( memento );
        memento.getLine().printExistingLine();
        memento.getLine().clear();
    }

    /**
     *
     */
    public static final class Segment
    {
        private final byte[] array;
        private final int fromIndex;
        private final int length;
        private final int hashCode;

        public Segment( byte[] array, int fromIndex, int length )
        {
            this.array = array;
            this.fromIndex = fromIndex;
            this.length = length;

            int hashCode = 0;
            int i = fromIndex;
            for ( int loops = length >> 1; loops-- != 0; )
            {
                hashCode = 31 * hashCode + array[i++];
                hashCode = 31 * hashCode + array[i++];
            }
            this.hashCode = i == fromIndex + length ? hashCode : 31 * hashCode + array[i];
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( !( obj instanceof Segment ) )
            {
                return false;
            }

            Segment that = (Segment) obj;
            if ( that.length != length )
            {
                return false;
            }

            for ( int i = 0; i < length; i++ )
            {
                if ( that.array[that.fromIndex + i] != array[fromIndex + i] )
                {
                    return false;
                }
            }
            return true;
        }
    }

    protected @Nonnull StreamReadStatus read( @Nonnull Memento memento, int recommendedCount ) throws IOException
    {
        ByteBuffer buffer = memento.getByteBuffer();
        if ( buffer.remaining() >= recommendedCount && ( (Buffer) buffer ).limit() != 0 )
        {
            return OVERFLOW;
        }
        else
        {
            if ( ( (Buffer) buffer ).position() != 0
                && recommendedCount > buffer.capacity() - ( (Buffer) buffer ).position() )
            {
                ( (Buffer) buffer.compact() ).flip();
                memento.getLine().setPositionByteBuffer( 0 );
            }
            int mark = ( (Buffer) buffer ).position();
            ( (Buffer) buffer ).position( ( (Buffer) buffer ).limit() );
            ( (Buffer) buffer ).limit( min( ( (Buffer) buffer ).position() + recommendedCount, buffer.capacity() ) );
            return read( buffer, mark, recommendedCount );
        }
    }

    private StreamReadStatus read( ByteBuffer buffer, int oldPosition, int recommendedCount )
        throws IOException
    {
        StreamReadStatus readStatus = null;
        boolean isEnd = false;
        try
        {
            while ( !isEnd && ( (Buffer) buffer ).position() - oldPosition < recommendedCount
                && ( (Buffer) buffer ).position() < ( (Buffer) buffer ).limit() )
            {
                isEnd = channel.read( buffer ) == -1;
            }
        }
        finally
        {
            ( (Buffer) buffer ).limit( ( (Buffer) buffer ).position() );
            ( (Buffer) buffer ).position( oldPosition );
            int readBytes = buffer.remaining();
            boolean readComplete = readBytes >= recommendedCount;
            if ( !isEnd || readComplete )
            {
                debugStream( buffer.array(),
                    buffer.arrayOffset() + ( (Buffer) buffer ).position(), buffer.remaining() );
                readStatus = readComplete ? OVERFLOW : UNDERFLOW;
            }
        }

        if ( readStatus == null )
        {
            throw new EOFException();
        }
        else
        {
            return readStatus;
        }
    }

    /**
     *
     */
    public final class Memento
    {
        private CharsetDecoder currentDecoder;
        private final CharsetDecoder defaultDecoder;
        private final BufferedStream line = new BufferedStream( 32 );
        private final List<Object> data = new ArrayList<>();
        private final CharBuffer cb = CharBuffer.allocate( BUFFER_SIZE );
        private final ByteBuffer bb = ByteBuffer.allocate( BUFFER_SIZE );

        public Memento()
        {
            defaultDecoder = DEFAULT_STREAM_ENCODING.newDecoder()
                .onMalformedInput( REPLACE )
                .onUnmappableCharacter( REPLACE );
            ( (Buffer) bb ).limit( 0 );
        }

        public void reset()
        {
            currentDecoder = null;
            data.clear();
        }

        public CharsetDecoder getDecoder()
        {
            return currentDecoder == null ? defaultDecoder : currentDecoder;
        }

        public void setCharset( Charset charset )
        {
            if ( charset.name().equals( defaultDecoder.charset().name() ) )
            {
                currentDecoder = defaultDecoder;
            }
            else
            {
                currentDecoder = charset.newDecoder()
                    .onMalformedInput( REPLACE )
                    .onUnmappableCharacter( REPLACE );
            }
        }

        public BufferedStream getLine()
        {
            return line;
        }

        public List<Object> getData()
        {
            return data;
        }

        public <T> T ofDataAt( int indexOfData )
        {
            //noinspection unchecked
            return (T) data.get( indexOfData );
        }

        public CharBuffer getCharBuffer()
        {
            return cb;
        }

        public ByteBuffer getByteBuffer()
        {
            return bb;
        }
    }

    /**
     * This class avoids locking which gains the performance of this decoder.
     */
    public final class BufferedStream
    {
        private byte[] buffer;
        private int count;
        private int positionByteBuffer;
        private boolean isNewLine;

        BufferedStream( int capacity )
        {
            this.buffer = new byte[capacity];
        }

        public int getPositionByteBuffer()
        {
            return positionByteBuffer;
        }

        public void setPositionByteBuffer( int positionByteBuffer )
        {
            this.positionByteBuffer = positionByteBuffer;
        }

        public void write( ByteBuffer bb, int position, int length )
        {
            ensureCapacity( length );
            byte[] array = bb.array();
            int pos = bb.arrayOffset() + position;
            while ( length-- > 0 )
            {
                positionByteBuffer++;
                byte b = array[pos++];
                if ( b == '\r' || b == '\n' )
                {
                    if ( !isNewLine )
                    {
                        printExistingLine();
                        count = 0;
                    }
                    isNewLine = true;
                }
                else
                {
                    buffer[count++] = b;
                    isNewLine = false;
                }
            }
        }

        public void clear()
        {
            count = 0;
        }

        @Override
        public String toString()
        {
            return new String( buffer, 0, count, DEFAULT_STREAM_ENCODING );
        }

        private boolean isEmpty()
        {
            return count == 0;
        }

        private void ensureCapacity( int addCapacity )
        {
            int oldCapacity = buffer.length;
            int exactCapacity = count + addCapacity;
            if ( exactCapacity < 0 )
            {
                throw new OutOfMemoryError();
            }

            if ( oldCapacity < exactCapacity )
            {
                int newCapacity = oldCapacity << 1;
                buffer = copyOf( buffer, max( newCapacity, exactCapacity ) );
            }
        }

        void printExistingLine()
        {
            if ( isEmpty() )
            {
                return;
            }

            String s = toString();
            if ( isBlank( s ) )
            {
                return;
            }

            if ( s.contains( PRINTABLE_JVM_NATIVE_STREAM ) )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( s );
                }
                else if ( logger.isInfoEnabled() )
                {
                    logger.info( s );
                }
                else
                {
                    // In case of debugging forked JVM, see PRINTABLE_JVM_NATIVE_STREAM.
                    System.out.println( s );
                }
            }
            else
            {
                if ( isJvmError( s ) )
                {
                    logger.error( s );
                }
                else if ( logger.isDebugEnabled() )
                {
                    logger.debug( s );
                }

                String msg = "Corrupted channel by directly writing to native stream in forked JVM "
                    + arguments.getForkChannelId() + ".";
                File dumpFile = arguments.dumpStreamText( msg + " Stream '" + s + "'." );
                String dumpPath = dumpFile.getAbsolutePath();
                arguments.logWarningAtEnd( msg + " See FAQ web page and the dump file " + dumpPath );
            }
        }

        private boolean isJvmError( String line )
        {
            String lineLower = line.toLowerCase();
            for ( String errorPattern : JVM_ERROR_PATTERNS )
            {
                if ( lineLower.contains( errorPattern ) )
                {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     *
     */
    public static final class MalformedFrameException extends Exception
    {
        private final int readFrom;
        private final int readTo;

        public MalformedFrameException( int readFrom, int readTo )
        {
            this.readFrom = readFrom;
            this.readTo = readTo;
        }

        public int readFrom()
        {
            return readFrom;
        }

        public int readTo()
        {
            return readTo;
        }

        public boolean hasValidPositions()
        {
            return readFrom != NO_POSITION && readTo != NO_POSITION && readTo - readFrom > 0;
        }
    }

    /**
     * Underflow - could not completely read out al bytes in one call.
     * <br>
     * Overflow - read all bytes or more
     * <br>
     * EOF - end of stream
     */
    public enum StreamReadStatus
    {
        UNDERFLOW,
        OVERFLOW,
        EOF
    }
}
