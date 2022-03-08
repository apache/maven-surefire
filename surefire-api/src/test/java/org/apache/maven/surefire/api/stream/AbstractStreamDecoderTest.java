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

import javax.annotation.Nonnull;

import java.io.EOFException;
import java.io.File;
import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.booter.Constants;
import org.apache.maven.surefire.api.booter.ForkedProcessEventType;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.api.stream.AbstractStreamDecoder.MalformedFrameException;
import org.apache.maven.surefire.api.stream.AbstractStreamDecoder.Memento;
import org.apache.maven.surefire.api.stream.AbstractStreamDecoder.Segment;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.lang.Math.min;
import static java.lang.System.arraycopy;
import static java.nio.charset.CodingErrorAction.REPLACE;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.apache.maven.surefire.api.booter.Constants.DEFAULT_STREAM_ENCODING;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STDOUT;
import static org.apache.maven.surefire.api.stream.SegmentType.END_OF_FRAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 * The performance of "get( Integer )" is 13.5 nano seconds on i5/2.6GHz:
 * <pre>
 *     {@code
 *     TreeMap<Integer, ForkedProcessEventType> map = new TreeMap<>();
 *     map.get( hash );
 *     }
 * </pre>
 *
 * <br> The performance of getting event type by Segment is 33.7 nano seconds:
 * <pre>
 *     {@code
 *     Map<Segment, ForkedProcessEventType> map = new HashMap<>();
 *     byte[] array = ForkedProcessEventType.BOOTERCODE_STDOUT.getOpcode().getBytes( UTF_8 );
 *     map.get( new Segment( array, 0, array.length ) );
 *     }
 * </pre>
 *
 * <br> The performance of decoder:
 * <pre>
 *     {@code
 *     CharsetDecoder decoder = STREAM_ENCODING.newDecoder()
 *             .onMalformedInput( REPLACE )
 *             .onUnmappableCharacter( REPLACE );
 *     ByteBuffer buffer = ByteBuffer.wrap( ForkedProcessEventType.BOOTERCODE_STDOUT.getOpcode().getBytes( UTF_8 ) );
 *     CharBuffer chars = CharBuffer.allocate( 100 );
 *     decoder.reset().decode( buffer, chars, true );
 *
 *     String s = chars.flip().toString(); // 37 nanos = CharsetDecoder + toString
 *
 *     buffer.clear();
 *     chars.clear();
 *
 *     ForkedProcessEventType.byOpcode( s ); // 65 nanos = CharsetDecoder + toString + byOpcode
 *     }
 * </pre>
 *
 * <br> The performance of decoding 100 bytes via CharacterDecoder - 71 nano seconds:
 * <pre>
 *     {@code
 *     decoder.reset()
 *         .decode( buffer, chars, true ); // CharsetDecoder 71 nanos
 *     chars.flip().toString(); // CharsetDecoder + toString = 91 nanos
 *     }
 * </pre>
 *
 * <br> The performance of a pure string creation (instead of decoder) - 31.5 nano seconds:
 * <pre>
 *     {@code
 *     byte[] b = {};
 *     new String( b, UTF_8 );
 *     }
 * </pre>
 *
 * <br> The performance of CharsetDecoder with empty ByteBuffer:
 * <pre>
 *     {@code
 *     CharsetDecoder + ByteBuffer.allocate( 0 ) makes 11.5 nanos
 *     CharsetDecoder + ByteBuffer.allocate( 0 ) + toString() makes 16.1 nanos
 *     }
 * </pre>
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class AbstractStreamDecoderTest
{
    private static final Map<Segment, ForkedProcessEventType> EVENTS = new HashMap<>();

    private static final String PATTERN1 =
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

    private static final String PATTERN2 = "€ab©c";

    private static final byte[] PATTERN2_BYTES =
        new byte[] {(byte) -30, (byte) -126, (byte) -84, 'a', 'b', (byte) 0xc2, (byte) 0xa9, 'c'};

    @BeforeClass
    public static void setup()
    {
        for ( ForkedProcessEventType event : ForkedProcessEventType.values() )
        {
            byte[] array = event.getOpcodeBinary();
            EVENTS.put( new Segment( array, 0, array.length ), event );
        }
    }

    @Test
    public void shouldDecodeHappyCase() throws Exception
    {
        CharsetDecoder decoder = UTF_8.newDecoder().onMalformedInput( REPLACE ).onUnmappableCharacter( REPLACE );
        ByteBuffer input = ByteBuffer.allocate( 1024 );
        ( (Buffer) input.put( PATTERN2_BYTES ) ).flip();
        int bytesToDecode = PATTERN2_BYTES.length;
        Buffer output = CharBuffer.allocate( 1024 );
        int readBytes = invokeMethod( AbstractStreamDecoder.class, "decodeString", decoder, input, output,
            bytesToDecode, true, 0 );

        assertThat( readBytes )
            .isEqualTo( bytesToDecode );

        assertThat( output.flip().toString() )
            .isEqualTo( PATTERN2 );
    }

    @Test
    public void shouldDecodeShifted() throws Exception
    {
        CharsetDecoder decoder = UTF_8.newDecoder().onMalformedInput( REPLACE ).onUnmappableCharacter( REPLACE );
        ByteBuffer input = ByteBuffer.allocate( 1024 );
        ( (Buffer) input.put( PATTERN1.getBytes( UTF_8 ) )
            .put( 90, (byte) 'A' )
            .put( 91, (byte) 'B' )
            .put( 92, (byte) 'C' ) )
            .position( 90 );
        Buffer output = CharBuffer.allocate( 1024 );
        int readBytes =
            invokeMethod( AbstractStreamDecoder.class, "decodeString", decoder, input, output, 2, true, 0 );

        assertThat( readBytes ).isEqualTo( 2 );

        assertThat( output.flip().toString() )
            .isEqualTo( "AB" );
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotDecode() throws Exception
    {
        CharsetDecoder decoder = UTF_8.newDecoder();
        ByteBuffer input = ByteBuffer.allocate( 100 );
        int bytesToDecode = 101;
        CharBuffer output = CharBuffer.allocate( 1000 );
        invokeMethod( AbstractStreamDecoder.class, "decodeString", decoder, input, output, bytesToDecode, true, 0 );
    }

    @Test
    public void shouldReadInt() throws Exception
    {
        Channel channel = new Channel( new byte[] {0x01, 0x02, 0x03, 0x04, ':'}, 1 );

        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();

        assertThat( thread.readInt( memento ) )
            .isEqualTo( new BigInteger( new byte[] {0x01, 0x02, 0x03, 0x04} ).intValue() );
    }

    @Test
    public void shouldReadInteger() throws Exception
    {
        Channel channel = new Channel( new byte[] {(byte) 0xff, 0x01, 0x02, 0x03, 0x04, ':'}, 1 );

        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        assertThat( thread.readInteger( memento ) )
            .isEqualTo( new BigInteger( new byte[] {0x01, 0x02, 0x03, 0x04} ).intValue() );
    }

    @Test
    public void shouldReadNullInteger() throws Exception
    {
        Channel channel = new Channel( new byte[] {(byte) 0x00, ':'}, 1 );

        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        assertThat( thread.readInteger( memento ) )
            .isNull();
    }

    @Test( expected = EOFException.class )
    public void shouldNotReadString() throws Exception
    {
        Channel channel = new Channel( PATTERN1.getBytes(), PATTERN1.length() );
        channel.read( ByteBuffer.allocate( 100 ) );

        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        invokeMethod( thread, "readString", memento, 10 );
    }

    @Test
    public void shouldReadString() throws Exception
    {
        Channel channel = new Channel( PATTERN1.getBytes(), PATTERN1.length() );

        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        String s = invokeMethod( thread, "readString", memento, 10 );
        assertThat( s )
            .isEqualTo( "0123456789" );
    }

    @Test
    public void shouldReadStringShiftedBuffer() throws Exception
    {
        StringBuilder s = new StringBuilder( 1100 );
        for ( int i = 0; i < 11; i++ )
        {
            s.append( PATTERN1 );
        }

        Channel channel = new Channel( s.toString().getBytes( UTF_8 ), s.length() );

        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        // whatever position will be compacted to 0
        ( (Buffer) ( (Buffer) memento.getByteBuffer() ).limit( 974 ) ).position( 974 );
        assertThat( (String) invokeMethod( thread, "readString", memento, PATTERN1.length() + 3 ) )
            .isEqualTo( PATTERN1 + "012" );
    }

    @Test
    public void shouldReadStringShiftedInput() throws Exception
    {
        StringBuilder s = new StringBuilder( 1100 );
        for ( int i = 0; i < 11; i++ )
        {
            s.append( PATTERN1 );
        }

        Channel channel = new Channel( s.toString().getBytes( UTF_8 ), s.length() );
        channel.read( ByteBuffer.allocate( 997 ) );

        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        assertThat( (String) invokeMethod( thread, "readString", memento, PATTERN1.length() ) )
            .isEqualTo( "789" + PATTERN1.substring( 0, 97 ) );
    }

    @Test
    public void shouldReadMultipleStringsAndShiftedInput() throws Exception
    {
        StringBuilder s = new StringBuilder( 5000 );

        for ( int i = 0; i < 50; i++ )
        {
            s.append( PATTERN1 );
        }

        Channel channel = new Channel( s.toString().getBytes( UTF_8 ), s.length() );
        channel.read( ByteBuffer.allocate( 1997 ) );

        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        // whatever position will be compacted to 0
        ( (Buffer) memento.getByteBuffer() ).limit( 974 ).position( 974 );

        StringBuilder expected = new StringBuilder( "789" );
        for ( int i = 0; i < 11; i++ )
        {
            expected.append( PATTERN1 );
        }
        expected.setLength( 1100 );
        assertThat( (String) invokeMethod( thread, "readString", memento, 1100 ) )
            .isEqualTo( expected.toString() );
    }

    @Test
    public void shouldDecode3BytesEncodedSymbol() throws Exception
    {
        byte[] encodedSymbol = new byte[] {(byte) -30, (byte) -126, (byte) -84};
        int countSymbols = 1024;
        byte[] input = new byte[encodedSymbol.length * countSymbols];
        for ( int i = 0; i < countSymbols; i++ )
        {
            arraycopy( encodedSymbol, 0, input, encodedSymbol.length * i, encodedSymbol.length );
        }

        Channel channel = new Channel( input, 64 * 1024 );
        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );
        Memento memento = thread.new Memento();
        String decodedOutput = invokeMethod( thread, "readString", memento, input.length );

        assertThat( decodedOutput )
            .isEqualTo( new String( input, 0, input.length, UTF_8 ) );
    }

    @Test
    public void shouldDecode100Bytes() throws Exception
    {
        CharsetDecoder decoder = DEFAULT_STREAM_ENCODING.newDecoder()
            .onMalformedInput( REPLACE )
            .onUnmappableCharacter( REPLACE );
        // empty stream: CharsetDecoder + ByteBuffer.allocate( 0 ) makes 11.5 nanos
        // empty stream: CharsetDecoder + ByteBuffer.allocate( 0 ) + toString() makes 16.1 nanos
        ByteBuffer buffer = ByteBuffer.wrap( PATTERN1.getBytes( UTF_8 ) );
        CharBuffer chars = CharBuffer.allocate( 100 );
        // uncomment this section for a proper measurement of the exec time
        TimeUnit.SECONDS.sleep( 2 );
        System.gc();
        TimeUnit.SECONDS.sleep( 5 );
        String s = null;
        long l1 = System.currentTimeMillis();
        for ( int i = 0; i < 10_000_000; i++ )
        {
            decoder.reset()
                .decode( buffer, chars, true ); // CharsetDecoder 71 nanos
            s = ( (Buffer) chars ).flip().toString(); // CharsetDecoder + toString = 91 nanos
            ( (Buffer) buffer ).clear();
            ( (Buffer) chars ).clear();
        }
        long l2 = System.currentTimeMillis();
        System.out.println( "decoded 100 bytes within " + ( l2 - l1 ) + " millis (10 million cycles)" );
        assertThat( s )
            .isEqualTo( PATTERN1 );
    }

    @Test
    public void shouldReadEventType() throws Exception
    {
        byte[] array = BOOTERCODE_STDOUT.getOpcodeBinary();
        Map<Segment, ForkedProcessEventType> messageType =
            singletonMap( new Segment( array, 0, array.length ), BOOTERCODE_STDOUT );

        byte[] stream = ":maven-surefire-event:\u000E:std-out-stream:".getBytes( UTF_8 );
        Channel channel = new Channel( stream, 1 );
        Mock thread = new Mock( channel, new MockForkNodeArguments(), messageType );

        Memento memento = thread.new Memento();
        memento.setCharset( UTF_8 );

        ForkedProcessEventType eventType = thread.readMessageType( memento );
        assertThat( eventType )
            .isEqualTo( BOOTERCODE_STDOUT );
    }

    @Test( expected = EOFException.class )
    public void shouldEventTypeReachedEndOfStream() throws Exception
    {
        byte[] stream = ":maven-surefire-event:\u000E:xxx".getBytes( UTF_8 );
        Channel channel = new Channel( stream, 1 );
        Mock thread = new Mock( channel, new MockForkNodeArguments(), EVENTS );

        Memento memento = thread.new Memento();
        memento.setCharset( UTF_8 );
        thread.readMessageType( memento );
    }

    @Test( expected = MalformedFrameException.class )
    public void shouldEventTypeReachedMalformedHeader() throws Exception
    {
        byte[] stream = ":xxxxx-xxxxxxxx-xxxxx:\u000E:xxx".getBytes( UTF_8 );
        Channel channel = new Channel( stream, 1 );
        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        memento.setCharset( UTF_8 );
        thread.readMessageType( memento );
    }

    @Test
    public void shouldReadEmptyString() throws Exception
    {
        byte[] stream = "\u0000\u0000\u0000\u0000::".getBytes( UTF_8 );
        Channel channel = new Channel( stream, 1 );
        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        memento.setCharset( UTF_8 );

        assertThat( thread.readString( memento ) )
            .isEmpty();
    }

    @Test
    public void shouldReadNullString() throws Exception
    {
        byte[] stream = "\u0000\u0000\u0000\u0001:\u0000:".getBytes( UTF_8 );
        Channel channel = new Channel( stream, 1 );
        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        memento.setCharset( UTF_8 );

        assertThat( thread.readString( memento ) )
            .isNull();
    }

    @Test
    public void shouldReadSingleCharString() throws Exception
    {
        byte[] stream = "\u0000\u0000\u0000\u0001:A:".getBytes( UTF_8 );
        Channel channel = new Channel( stream, 1 );
        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        memento.setCharset( UTF_8 );

        assertThat( thread.readString( memento ) )
            .isEqualTo( "A" );
    }

    @Test
    public void shouldReadThreeCharactersString() throws Exception
    {
        byte[] stream = "\u0000\u0000\u0000\u0003:ABC:".getBytes( UTF_8 );
        Channel channel = new Channel( stream, 1 );
        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        memento.setCharset( UTF_8 );

        assertThat( thread.readString( memento ) )
            .isEqualTo( "ABC" );
    }

    @Test
    public void shouldReadDefaultCharset() throws Exception
    {
        byte[] stream = "\u0005:UTF-8:".getBytes( US_ASCII );
        Channel channel = new Channel( stream, 1 );
        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        memento.setCharset( UTF_8 );

        assertThat( thread.readCharset( memento ) )
            .isNotNull()
            .isEqualTo( UTF_8 );
    }

    @Test
    public void shouldReadNonDefaultCharset() throws Exception
    {
        byte[] stream = ( (char) 10 + ":ISO_8859_1:" ).getBytes( US_ASCII );
        Channel channel = new Channel( stream, 1 );
        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        memento.setCharset( UTF_8 );

        assertThat( thread.readCharset( memento ) )
            .isNotNull()
            .isEqualTo( ISO_8859_1 );
    }

    @Test
    public void shouldSetNonDefaultCharset()
    {
        byte[] stream = {};
        Channel channel = new Channel( stream, 1 );
        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );
        Memento memento = thread.new Memento();

        memento.setCharset( ISO_8859_1 );
        assertThat( memento.getDecoder().charset() ).isEqualTo( ISO_8859_1 );

        memento.setCharset( UTF_8 );
        assertThat( memento.getDecoder().charset() ).isEqualTo( UTF_8 );

        memento.reset();
        assertThat( memento.getDecoder() ).isNotNull();
        assertThat( memento.getDecoder().charset() ).isEqualTo( UTF_8 );
    }

    @Test( expected = MalformedFrameException.class )
    public void malformedCharset() throws Exception
    {
        byte[] stream = ( (char) 8 + ":ISO_8859:" ).getBytes( US_ASCII );
        Channel channel = new Channel( stream, 1 );
        Mock thread = new Mock( channel, new MockForkNodeArguments(),
            Collections.<Segment, ForkedProcessEventType>emptyMap() );

        Memento memento = thread.new Memento();
        memento.setCharset( UTF_8 );

        thread.readCharset( memento );
    }

    private static class Channel implements ReadableByteChannel
    {
        private final byte[] bytes;
        private final int chunkSize;
        protected int i;

        Channel( byte[] bytes, int chunkSize )
        {
            this.bytes = bytes;
            this.chunkSize = chunkSize;
        }

        @Override
        public int read( ByteBuffer dst )
        {
            if ( i == bytes.length )
            {
                return -1;
            }
            else if ( dst.hasRemaining() )
            {
                int length = min( min( chunkSize, bytes.length - i ), dst.remaining() ) ;
                dst.put( bytes, i, length );
                i += length;
                return length;
            }
            else
            {
                return 0;
            }
        }

        @Override
        public boolean isOpen()
        {
            return false;
        }

        @Override
        public void close()
        {
        }
    }

    private static class MockForkNodeArguments implements ForkNodeArguments
    {
        @Nonnull
        @Override
        public String getSessionId()
        {
            return null;
        }

        @Override
        public int getForkChannelId()
        {
            return 0;
        }

        @Nonnull
        @Override
        public File dumpStreamText( @Nonnull String text )
        {
            return null;
        }

        @Nonnull
        @Override
        public File dumpStreamException( @Nonnull Throwable t )
        {
            return null;
        }

        @Override
        public void logWarningAtEnd( @Nonnull String text )
        {
        }

        @Nonnull
        @Override
        public ConsoleLogger getConsoleLogger()
        {
            return null;
        }

        @Nonnull
        @Override
        public Object getConsoleLock()
        {
            return new Object();
        }

        @Override
        public File getEventStreamBinaryFile()
        {
            return null;
        }

        @Override
        public File getCommandStreamBinaryFile()
        {
            return null;
        }
    }

    private static class Mock extends AbstractStreamDecoder<Event, ForkedProcessEventType, SegmentType>
    {
        protected Mock( @Nonnull ReadableByteChannel channel, @Nonnull ForkNodeArguments arguments,
                        @Nonnull Map<Segment, ForkedProcessEventType> messageTypes )
        {
            super( channel, arguments, messageTypes );
        }

        @Override
        public Event decode( @Nonnull Memento memento ) throws MalformedChannelException
        {
            throw new MalformedChannelException();
        }

        @Nonnull
        @Override
        protected byte[] getEncodedMagicNumber()
        {
            return Constants.MAGIC_NUMBER_FOR_EVENTS_BYTES;
        }

        @Nonnull
        @Override
        protected SegmentType[] nextSegmentType( @Nonnull ForkedProcessEventType messageType )
        {
            return new SegmentType[] {END_OF_FRAME};
        }

        @Nonnull
        @Override
        protected Event toMessage( @Nonnull ForkedProcessEventType messageType, @Nonnull Memento memento )
        {
            return null;
        }

        @Override
        public void close() throws Exception
        {
        }
    }
}
