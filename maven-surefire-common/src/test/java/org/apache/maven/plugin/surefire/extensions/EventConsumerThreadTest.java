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

import org.apache.maven.plugin.surefire.extensions.EventConsumerThread.Memento;
import org.apache.maven.plugin.surefire.extensions.EventConsumerThread.Segment;
import org.apache.maven.plugin.surefire.extensions.EventConsumerThread.SegmentType;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.booter.ForkedProcessEventType;
import org.apache.maven.surefire.api.event.ConsoleDebugEvent;
import org.apache.maven.surefire.api.event.ConsoleErrorEvent;
import org.apache.maven.surefire.api.event.ConsoleInfoEvent;
import org.apache.maven.surefire.api.event.ConsoleWarningEvent;
import org.apache.maven.surefire.api.event.ControlByeEvent;
import org.apache.maven.surefire.api.event.ControlNextTestEvent;
import org.apache.maven.surefire.api.event.ControlStopOnNextTestEvent;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.event.JvmExitErrorEvent;
import org.apache.maven.surefire.api.event.StandardStreamErrEvent;
import org.apache.maven.surefire.api.event.StandardStreamErrWithNewLineEvent;
import org.apache.maven.surefire.api.event.StandardStreamOutEvent;
import org.apache.maven.surefire.api.event.StandardStreamOutWithNewLineEvent;
import org.apache.maven.surefire.api.event.SystemPropertyEvent;
import org.apache.maven.surefire.api.event.TestAssumptionFailureEvent;
import org.apache.maven.surefire.api.event.TestErrorEvent;
import org.apache.maven.surefire.api.event.TestFailedEvent;
import org.apache.maven.surefire.api.event.TestSkippedEvent;
import org.apache.maven.surefire.api.event.TestStartingEvent;
import org.apache.maven.surefire.api.event.TestSucceededEvent;
import org.apache.maven.surefire.api.event.TestsetCompletedEvent;
import org.apache.maven.surefire.api.event.TestsetStartingEvent;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.extensions.ForkNodeArguments;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.fest.assertions.Condition;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.min;
import static java.nio.charset.CodingErrorAction.REPLACE;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.maven.plugin.surefire.extensions.EventConsumerThread.SegmentType.DATA_INT;
import static org.apache.maven.plugin.surefire.extensions.EventConsumerThread.SegmentType.DATA_STRING;
import static org.apache.maven.plugin.surefire.extensions.EventConsumerThread.SegmentType.END_OF_FRAME;
import static org.apache.maven.plugin.surefire.extensions.EventConsumerThread.SegmentType.RUN_MODE;
import static org.apache.maven.plugin.surefire.extensions.EventConsumerThread.SegmentType.STRING_ENCODING;
import static org.apache.maven.plugin.surefire.extensions.EventConsumerThread.mapEventTypes;
import static org.apache.maven.plugin.surefire.extensions.EventConsumerThread.mapRunModes;
import static org.apache.maven.plugin.surefire.extensions.EventConsumerThread.nextSegmentType;
import static org.apache.maven.plugin.surefire.extensions.EventConsumerThread.toEvent;
import static org.apache.maven.surefire.api.booter.Constants.DEFAULT_STREAM_ENCODING;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_BYE;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_DEBUG;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_ERROR;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_INFO;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_WARNING;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_JVM_EXIT_ERROR;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_NEXT_TEST;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STDERR;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STDERR_NEW_LINE;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STDOUT;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STDOUT_NEW_LINE;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_STOP_ON_NEXT_TEST;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_SYSPROPS;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TESTSET_COMPLETED;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TESTSET_STARTING;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_ASSUMPTIONFAILURE;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_ERROR;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_FAILED;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_SKIPPED;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_STARTING;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_SUCCEEDED;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.report.RunMode.RERUN_TEST_AFTER_FAILURE;
import static org.fest.assertions.Assertions.assertThat;
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
public class EventConsumerThreadTest
{
    private static final CountdownCloseable COUNTDOWN_CLOSEABLE = new CountdownCloseable( new MockCloseable(), 0 );

    private static final String PATTERN1 =
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

    private static final String PATTERN2 = "€ab©c";

    private static final byte[] PATTERN2_BYTES =
        new byte[]{(byte) -30, (byte) -126, (byte) -84, 'a', 'b', (byte) 0xc2, (byte) 0xa9, 'c'};

    @Test
    public void shouldDecodeHappyCase() throws Exception
    {
        CharsetDecoder decoder = UTF_8.newDecoder()
            .onMalformedInput( REPLACE )
            .onUnmappableCharacter( REPLACE );
        ByteBuffer input = ByteBuffer.allocate( 1024 );
        input.put( PATTERN2_BYTES )
            .flip();
        int bytesToDecode = PATTERN2_BYTES.length;
        CharBuffer output = CharBuffer.allocate( 1024 );
        int readBytes =
            invokeMethod( EventConsumerThread.class, "decodeString", decoder, input, output, bytesToDecode, true, 0 );

        assertThat( readBytes )
            .isEqualTo( bytesToDecode );

        assertThat( output.flip().toString() )
            .isEqualTo( PATTERN2 );
    }

    @Test
    public void shouldDecodeShifted() throws Exception
    {
        CharsetDecoder decoder = UTF_8.newDecoder()
            .onMalformedInput( REPLACE )
            .onUnmappableCharacter( REPLACE );
        ByteBuffer input = ByteBuffer.allocate( 1024 );
        input.put( PATTERN1.getBytes( UTF_8 ) )
            .put( 90, (byte) 'A' )
            .put( 91, (byte) 'B' )
            .put( 92, (byte) 'C' )
            .position( 90 );
        CharBuffer output = CharBuffer.allocate( 1024 );
        int readBytes =
            invokeMethod( EventConsumerThread.class, "decodeString", decoder, input, output, 2, true, 0 );

        assertThat( readBytes )
            .isEqualTo( 2 );

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
        invokeMethod( EventConsumerThread.class, "decodeString", decoder, input, output, bytesToDecode, true, 0 );
    }

    @Test
    public void shouldReadInt() throws Exception
    {
        Channel channel = new Channel( new byte[] {0x01, 0x02, 0x03, 0x04, ':'}, 1 );

        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
        assertThat( thread.readInt( memento ) )
            .isEqualTo( new BigInteger( new byte[] {0x01, 0x02, 0x03, 0x04} ).intValue() );
    }

    @Test
    public void shouldReadInteger() throws Exception
    {
        Channel channel = new Channel( new byte[] {(byte) 0xff, 0x01, 0x02, 0x03, 0x04, ':'}, 1 );

        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
        assertThat( thread.readInteger( memento ) )
            .isEqualTo( new BigInteger( new byte[] {0x01, 0x02, 0x03, 0x04} ).intValue() );
    }

    @Test
    public void shouldReadNullInteger() throws Exception
    {
        Channel channel = new Channel( new byte[] {(byte) 0x00, ':'}, 1 );

        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
        assertThat( thread.readInteger( memento ) )
            .isNull();
    }

    @Test( expected = EOFException.class )
    public void shouldNotReadString() throws Exception
    {
        Channel channel = new Channel( PATTERN1.getBytes(), PATTERN1.length() );
        channel.read( ByteBuffer.allocate( 100 ) );

        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
        thread.readString( memento, 10 );
    }

    @Test
    public void shouldReadString() throws Exception
    {
        Channel channel = new Channel( PATTERN1.getBytes(), PATTERN1.length() );

        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
        String s = thread.readString( memento, 10 );
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

        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        // whatever position will be compacted to 0
        memento.bb.position( 974 ).limit( 974 );
        assertThat( thread.readString( memento, PATTERN1.length() + 3 ) )
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

        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.limit( 0 );
        assertThat( thread.readString( memento, PATTERN1.length() ) )
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

        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        // whatever position will be compacted to 0
        memento.bb.position( 974 ).limit( 974 );

        StringBuilder expected = new StringBuilder( "789" );
        for ( int i = 0; i < 11; i++ )
        {
            expected.append( PATTERN1 );
        }
        expected.setLength( 1100 );
        assertThat( thread.readString( memento, 1100 ) )
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
            System.arraycopy( encodedSymbol, 0, input, encodedSymbol.length * i, encodedSymbol.length );
        }

        Channel channel = new Channel( input, 2 );
        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );
        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
        String decodedOutput = thread.readString( memento, input.length );

        assertThat( decodedOutput )
            .isEqualTo( new String( input, 0, input.length, UTF_8 ) );
    }

    @Test
    public void shouldDecode100Bytes()
    {
        CharsetDecoder decoder = DEFAULT_STREAM_ENCODING.newDecoder()
            .onMalformedInput( REPLACE )
            .onUnmappableCharacter( REPLACE );
        // empty stream: CharsetDecoder + ByteBuffer.allocate( 0 ) makes 11.5 nanos
        // empty stream: CharsetDecoder + ByteBuffer.allocate( 0 ) + toString() makes 16.1 nanos
        ByteBuffer buffer = ByteBuffer.wrap( PATTERN1.getBytes( UTF_8 ) );
        CharBuffer chars = CharBuffer.allocate( 100 );
        /* uncomment this section for a proper measurement of the exec time
        TimeUnit.SECONDS.sleep( 2 );
        System.gc();
        TimeUnit.SECONDS.sleep( 5 );
        */
        String s = null;
        long l1 = System.currentTimeMillis();
        for ( int i = 0; i < 10_000_000; i++ )
        {
            decoder.reset()
                .decode( buffer, chars, true ); // CharsetDecoder 71 nanos
            s = chars.flip().toString(); // CharsetDecoder + toString = 91 nanos
            buffer.clear();
            chars.clear();
        }
        long l2 = System.currentTimeMillis();
        System.out.println( "decoded 100 bytes within " + ( l2 - l1 ) + " millis (10 million cycles)" );
        assertThat( s )
            .isEqualTo( PATTERN1 );
    }

    @Test( timeout = 60_000L )
    public void performanceTest() throws Exception
    {
        final long[] staredAt = {0};
        final long[] finishedAt = {0};
        final AtomicInteger calls = new AtomicInteger();
        final int totalCalls = 1_000_000; // 400_000; // 1_000_000; // 10_000_000;

        EventHandler<Event> handler = new EventHandler<Event>()
        {
            @Override
            public void handleEvent( @Nonnull Event event )
            {
                if ( staredAt[0] == 0 )
                {
                    staredAt[0] = System.currentTimeMillis();
                }

                if ( calls.incrementAndGet() == totalCalls )
                {
                    finishedAt[0] = System.currentTimeMillis();
                }
            }
        };

        final ByteBuffer event = ByteBuffer.allocate( 192 );
        event.put( ":maven-surefire-event:".getBytes( UTF_8 ) );
        event.put( (byte) 14 );
        event.put( ":std-out-stream:".getBytes( UTF_8 ) );
        event.put( (byte) 10 );
        event.put( ":normal-run:".getBytes( UTF_8 ) );
        event.put( (byte) 5 );
        event.put( ":UTF-8:".getBytes( UTF_8 ) );
        event.putInt( 100 );
        event.put(
            ":0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789:"
                .getBytes( UTF_8 ) );

        event.flip();
        byte[] frame = copyOfRange( event.array(), event.arrayOffset(), event.arrayOffset() + event.remaining() );
        ReadableByteChannel channel = new Channel( frame, 1024 )
        {
            private int countRounds;

            @Override
            public int read( ByteBuffer dst )
            {
                int length = super.read( dst );
                if ( length == -1 && countRounds < totalCalls )
                {
                    i = 0;
                    length = super.read( dst );
                    countRounds++;
                }
                return length;
            }
        };

        EventConsumerThread thread = new EventConsumerThread( "t", channel, handler,
            new CountdownCloseable( new MockCloseable(), 1 ), new MockForkNodeArguments() );

        TimeUnit.SECONDS.sleep( 2 );
        System.gc();
        TimeUnit.SECONDS.sleep( 5 );

        System.out.println( "Staring the event thread..." );

        thread.start();
        thread.join();

        long execTime = finishedAt[0] - staredAt[0];
        System.out.println( execTime );

        // 0.6 seconds while using the encoder/decoder
        assertThat( execTime )
            .describedAs( "The performance test should assert 1.0s of read time. "
                + "The limit 3.6s guarantees that the read time does not exceed this limit on overloaded CPU." )
            .isPositive()
            .isLessThanOrEqualTo( 3_600L );
    }

    @Test
    public void shouldReadEventType() throws Exception
    {
        Map<Segment, ForkedProcessEventType> eventTypes = mapEventTypes();
        assertThat( eventTypes )
            .hasSize( ForkedProcessEventType.values().length );

        byte[] stream = ":maven-surefire-event:\u000E:std-out-stream:".getBytes( UTF_8 );
        Channel channel = new Channel( stream, 1 );
        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
        memento.setCharset( UTF_8 );

        ForkedProcessEventType eventType = thread.readEventType( eventTypes, memento );
        assertThat( eventType )
            .isEqualTo( BOOTERCODE_STDOUT );
    }

    @Test( expected = EOFException.class )
    public void shouldEventTypeReachedEndOfStream() throws Exception
    {
        Map<Segment, ForkedProcessEventType> eventTypes = mapEventTypes();
        assertThat( eventTypes )
            .hasSize( ForkedProcessEventType.values().length );

        byte[] stream = ":maven-surefire-event:\u000E:xxx".getBytes( UTF_8 );
        Channel channel = new Channel( stream, 1 );
        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
        memento.setCharset( UTF_8 );
        thread.readEventType( eventTypes, memento );
    }

    @Test( expected = EventConsumerThread.MalformedFrameException.class )
    public void shouldEventTypeReachedMalformedHeader() throws Exception
    {
        Map<Segment, ForkedProcessEventType> eventTypes = mapEventTypes();
        assertThat( eventTypes )
            .hasSize( ForkedProcessEventType.values().length );

        byte[] stream = ":xxxxx-xxxxxxxx-xxxxx:\u000E:xxx".getBytes( UTF_8 );
        Channel channel = new Channel( stream, 1 );
        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
        memento.setCharset( UTF_8 );
        thread.readEventType( eventTypes, memento );
    }

    @Test
    public void shouldMapSegmentToEventType()
    {
        Map<Segment, RunMode> map = mapRunModes();

        assertThat( map )
            .hasSize( 2 );

        byte[] stream = "normal-run".getBytes( US_ASCII );
        Segment segment = new Segment( stream, 0, stream.length );
        assertThat( map.get( segment ) )
            .isEqualTo( NORMAL_RUN );

        stream = "rerun-test-after-failure".getBytes( US_ASCII );
        segment = new Segment( stream, 0, stream.length );
        assertThat( map.get( segment ) )
            .isEqualTo( RERUN_TEST_AFTER_FAILURE );
    }

    @Test
    public void shouldReadEmptyString() throws Exception
    {
        byte[] stream = "\u0000\u0000\u0000\u0000::".getBytes( UTF_8 );
        Channel channel = new Channel( stream, 1 );
        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
        memento.setCharset( UTF_8 );

        assertThat( thread.readString( memento ) )
            .isEmpty();
    }

    @Test
    public void shouldReadNullString() throws Exception
    {
        byte[] stream = "\u0000\u0000\u0000\u0001:\u0000:".getBytes( UTF_8 );
        Channel channel = new Channel( stream, 1 );
        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
        memento.setCharset( UTF_8 );

        assertThat( thread.readString( memento ) )
            .isNull();
    }

    @Test
    public void shouldReadSingleCharString() throws Exception
    {
        byte[] stream = "\u0000\u0000\u0000\u0001:A:".getBytes( UTF_8 );
        Channel channel = new Channel( stream, 1 );
        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
        memento.setCharset( UTF_8 );

        assertThat( thread.readString( memento ) )
            .isEqualTo( "A" );
    }

    @Test
    public void shouldReadThreeCharactersString() throws Exception
    {
        byte[] stream = "\u0000\u0000\u0000\u0003:ABC:".getBytes( UTF_8 );
        Channel channel = new Channel( stream, 1 );
        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
        memento.setCharset( UTF_8 );

        assertThat( thread.readString( memento ) )
            .isEqualTo( "ABC" );
    }

    @Test
    public void shouldReadDefaultCharset() throws Exception
    {
        byte[] stream = "\u0005:UTF-8:".getBytes( US_ASCII );
        Channel channel = new Channel( stream, 1 );
        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
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
        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
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
        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );
        Memento memento = thread.new Memento();

        memento.setCharset( ISO_8859_1 );
        assertThat( memento.getDecoder().charset() ).isEqualTo( ISO_8859_1 );

        memento.setCharset( UTF_8 );
        assertThat( memento.getDecoder().charset() ).isEqualTo( UTF_8 );

        memento.reset();
        assertThat( memento.getDecoder() ).isNotNull();
        assertThat( memento.getDecoder().charset() ).isEqualTo( UTF_8 );
    }

    @Test( expected = EventConsumerThread.MalformedFrameException.class )
    public void malformedCharset() throws Exception
    {
        byte[] stream = ( (char) 8 + ":ISO_8859:" ).getBytes( US_ASCII );
        Channel channel = new Channel( stream, 1 );
        EventConsumerThread thread = new EventConsumerThread( "t", channel,
            new MockEventHandler<Event>(), COUNTDOWN_CLOSEABLE, new MockForkNodeArguments() );

        Memento memento = thread.new Memento();
        memento.bb.position( 0 ).limit( 0 );
        memento.setCharset( UTF_8 );

        thread.readCharset( memento );
    }

    @Test
    public void shouldMapEventTypeToSegmentType()
    {
        SegmentType[] segmentTypes = nextSegmentType( BOOTERCODE_BYE );
        assertThat( segmentTypes )
            .hasSize( 1 )
            .containsOnly( END_OF_FRAME );

        segmentTypes = nextSegmentType( BOOTERCODE_STOP_ON_NEXT_TEST );
        assertThat( segmentTypes )
            .hasSize( 1 )
            .containsOnly( END_OF_FRAME );

        segmentTypes = nextSegmentType( BOOTERCODE_NEXT_TEST );
        assertThat( segmentTypes )
            .hasSize( 1 )
            .containsOnly( END_OF_FRAME );

        segmentTypes = nextSegmentType( BOOTERCODE_CONSOLE_ERROR );
        assertThat( segmentTypes )
            .hasSize( 5 )
            .satisfies( new InOrder( STRING_ENCODING, DATA_STRING, DATA_STRING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( ForkedProcessEventType.BOOTERCODE_JVM_EXIT_ERROR );
        assertThat( segmentTypes )
            .hasSize( 5 )
            .satisfies( new InOrder( STRING_ENCODING, DATA_STRING, DATA_STRING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( BOOTERCODE_CONSOLE_INFO );
        assertThat( segmentTypes )
            .hasSize( 3 )
            .satisfies( new InOrder( STRING_ENCODING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( ForkedProcessEventType.BOOTERCODE_CONSOLE_DEBUG );
        assertThat( segmentTypes )
            .hasSize( 3 )
            .satisfies( new InOrder( STRING_ENCODING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( ForkedProcessEventType.BOOTERCODE_CONSOLE_WARNING );
        assertThat( segmentTypes )
            .hasSize( 3 )
            .satisfies( new InOrder( STRING_ENCODING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( BOOTERCODE_STDOUT );
        assertThat( segmentTypes )
            .hasSize( 4 )
            .satisfies( new InOrder( RUN_MODE, STRING_ENCODING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( ForkedProcessEventType.BOOTERCODE_STDOUT_NEW_LINE );
        assertThat( segmentTypes )
            .hasSize( 4 )
            .satisfies( new InOrder( RUN_MODE, STRING_ENCODING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( ForkedProcessEventType.BOOTERCODE_STDERR );
        assertThat( segmentTypes )
            .hasSize( 4 )
            .satisfies( new InOrder( RUN_MODE, STRING_ENCODING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( ForkedProcessEventType.BOOTERCODE_STDERR_NEW_LINE );
        assertThat( segmentTypes )
            .hasSize( 4 )
            .satisfies( new InOrder( RUN_MODE, STRING_ENCODING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( BOOTERCODE_SYSPROPS );
        assertThat( segmentTypes )
            .hasSize( 5 )
            .satisfies( new InOrder( RUN_MODE, STRING_ENCODING, DATA_STRING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( BOOTERCODE_TESTSET_STARTING );
        assertThat( segmentTypes )
            .hasSize( 13 )
            .satisfies( new InOrder( RUN_MODE, STRING_ENCODING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_INT, DATA_STRING, DATA_STRING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( ForkedProcessEventType.BOOTERCODE_TESTSET_COMPLETED );
        assertThat( segmentTypes )
            .hasSize( 13 )
            .satisfies( new InOrder( RUN_MODE, STRING_ENCODING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_INT, DATA_STRING, DATA_STRING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( ForkedProcessEventType.BOOTERCODE_TEST_STARTING );
        assertThat( segmentTypes )
            .hasSize( 13 )
            .satisfies( new InOrder( RUN_MODE, STRING_ENCODING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_INT, DATA_STRING, DATA_STRING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( BOOTERCODE_TEST_SUCCEEDED );
        assertThat( segmentTypes )
            .hasSize( 13 )
            .satisfies( new InOrder( RUN_MODE, STRING_ENCODING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_INT, DATA_STRING, DATA_STRING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( BOOTERCODE_TEST_FAILED );
        assertThat( segmentTypes )
            .hasSize( 13 )
            .satisfies( new InOrder( RUN_MODE, STRING_ENCODING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_INT, DATA_STRING, DATA_STRING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( ForkedProcessEventType.BOOTERCODE_TEST_SKIPPED );
        assertThat( segmentTypes )
            .hasSize( 13 )
            .satisfies( new InOrder( RUN_MODE, STRING_ENCODING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_INT, DATA_STRING, DATA_STRING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( ForkedProcessEventType.BOOTERCODE_TEST_ERROR );
        assertThat( segmentTypes )
            .hasSize( 13 )
            .satisfies( new InOrder( RUN_MODE, STRING_ENCODING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_INT, DATA_STRING, DATA_STRING, DATA_STRING, END_OF_FRAME ) );

        segmentTypes = nextSegmentType( ForkedProcessEventType.BOOTERCODE_TEST_ASSUMPTIONFAILURE );
        assertThat( segmentTypes )
            .hasSize( 13 )
            .satisfies( new InOrder( RUN_MODE, STRING_ENCODING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_INT, DATA_STRING, DATA_STRING, DATA_STRING, END_OF_FRAME ) );
    }

    @Test
    public void shouldCreateEvent()
    {
        Event event = toEvent( BOOTERCODE_BYE, NORMAL_RUN, emptyList() );
        assertThat( event )
            .isInstanceOf( ControlByeEvent.class );

        event = toEvent( BOOTERCODE_STOP_ON_NEXT_TEST, NORMAL_RUN, emptyList() );
        assertThat( event )
            .isInstanceOf( ControlStopOnNextTestEvent.class );

        event = toEvent( BOOTERCODE_NEXT_TEST, NORMAL_RUN, emptyList() );
        assertThat( event )
            .isInstanceOf( ControlNextTestEvent.class );

        List data = asList( "1", "2", "3" );
        event = toEvent( BOOTERCODE_CONSOLE_ERROR, NORMAL_RUN, data );
        assertThat( event )
            .isInstanceOf( ConsoleErrorEvent.class );
        ConsoleErrorEvent consoleErrorEvent = (ConsoleErrorEvent) event;
        assertThat( consoleErrorEvent.getStackTraceWriter().getThrowable().getLocalizedMessage() )
            .isEqualTo( "1" );
        assertThat( consoleErrorEvent.getStackTraceWriter().smartTrimmedStackTrace() )
            .isEqualTo( "2" );
        assertThat( consoleErrorEvent.getStackTraceWriter().writeTraceToString() )
            .isEqualTo( "3" );

        data = asList( null, null, null );
        event = toEvent( BOOTERCODE_CONSOLE_ERROR, NORMAL_RUN, data );
        assertThat( event )
            .isInstanceOf( ConsoleErrorEvent.class );
        consoleErrorEvent = (ConsoleErrorEvent) event;
        assertThat( consoleErrorEvent.getStackTraceWriter() )
            .isNull();

        data = asList( "1", "2", "3" );
        event = toEvent( BOOTERCODE_JVM_EXIT_ERROR, NORMAL_RUN, data );
        assertThat( event )
            .isInstanceOf( JvmExitErrorEvent.class );
        JvmExitErrorEvent jvmExitErrorEvent = (JvmExitErrorEvent) event;
        assertThat( jvmExitErrorEvent.getStackTraceWriter().getThrowable().getLocalizedMessage() )
            .isEqualTo( "1" );
        assertThat( jvmExitErrorEvent.getStackTraceWriter().smartTrimmedStackTrace() )
            .isEqualTo( "2" );
        assertThat( jvmExitErrorEvent.getStackTraceWriter().writeTraceToString() )
            .isEqualTo( "3" );

        data = asList( null, null, null );
        event = toEvent( BOOTERCODE_JVM_EXIT_ERROR, NORMAL_RUN, data );
        assertThat( event )
            .isInstanceOf( JvmExitErrorEvent.class );
        jvmExitErrorEvent = (JvmExitErrorEvent) event;
        assertThat( jvmExitErrorEvent.getStackTraceWriter() )
            .isNull();

        data = singletonList( "m" );
        event = toEvent( BOOTERCODE_CONSOLE_INFO, NORMAL_RUN, data );
        assertThat( event ).isInstanceOf( ConsoleInfoEvent.class );
        assertThat( ( (ConsoleInfoEvent) event ).getMessage() ).isEqualTo( "m" );

        data = singletonList( "" );
        event = toEvent( BOOTERCODE_CONSOLE_WARNING, NORMAL_RUN, data );
        assertThat( event ).isInstanceOf( ConsoleWarningEvent.class );
        assertThat( ( (ConsoleWarningEvent) event ).getMessage() ).isEmpty();

        data = singletonList( null );
        event = toEvent( BOOTERCODE_CONSOLE_DEBUG, NORMAL_RUN, data );
        assertThat( event ).isInstanceOf( ConsoleDebugEvent.class );
        assertThat( ( (ConsoleDebugEvent) event ).getMessage() ).isNull();

        data = singletonList( "m" );
        event = toEvent( BOOTERCODE_STDOUT, NORMAL_RUN, data );
        assertThat( event ).isInstanceOf( StandardStreamOutEvent.class );
        assertThat( ( (StandardStreamOutEvent) event ).getMessage() ).isEqualTo( "m" );
        assertThat( ( (StandardStreamOutEvent) event ).getRunMode() ).isEqualTo( NORMAL_RUN );

        data = singletonList( null );
        event = toEvent( BOOTERCODE_STDOUT_NEW_LINE, RERUN_TEST_AFTER_FAILURE, data );
        assertThat( event ).isInstanceOf( StandardStreamOutWithNewLineEvent.class );
        assertThat( ( (StandardStreamOutWithNewLineEvent) event ).getMessage() ).isNull();
        assertThat( ( (StandardStreamOutWithNewLineEvent) event ).getRunMode() ).isEqualTo( RERUN_TEST_AFTER_FAILURE );

        data = singletonList( null );
        event = toEvent( BOOTERCODE_STDERR, RERUN_TEST_AFTER_FAILURE, data );
        assertThat( event ).isInstanceOf( StandardStreamErrEvent.class );
        assertThat( ( (StandardStreamErrEvent) event ).getMessage() ).isNull();
        assertThat( ( (StandardStreamErrEvent) event ).getRunMode() ).isEqualTo( RERUN_TEST_AFTER_FAILURE );

        data = singletonList( "abc" );
        event = toEvent( BOOTERCODE_STDERR_NEW_LINE, NORMAL_RUN, data );
        assertThat( event ).isInstanceOf( StandardStreamErrWithNewLineEvent.class );
        assertThat( ( (StandardStreamErrWithNewLineEvent) event ).getMessage() ).isEqualTo( "abc" );
        assertThat( ( (StandardStreamErrWithNewLineEvent) event ).getRunMode() ).isEqualTo( NORMAL_RUN );

        data = asList( "key", "value" );
        event = toEvent( BOOTERCODE_SYSPROPS, NORMAL_RUN, data );
        assertThat( event ).isInstanceOf( SystemPropertyEvent.class );
        assertThat( ( (SystemPropertyEvent) event ).getKey() ).isEqualTo( "key" );
        assertThat( ( (SystemPropertyEvent) event ).getValue() ).isEqualTo( "value" );
        assertThat( ( (SystemPropertyEvent) event ).getRunMode() ).isEqualTo( NORMAL_RUN );

        data = asList( "source", "sourceText", "name", "nameText", "group", "message", 5,
            "traceMessage", "smartTrimmedStackTrace", "stackTrace" );
        event = toEvent( BOOTERCODE_TESTSET_STARTING, NORMAL_RUN, data );
        assertThat( event ).isInstanceOf( TestsetStartingEvent.class );
        assertThat( ( (TestsetStartingEvent) event ).getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (TestsetStartingEvent) event ).getReportEntry().getSourceName() ).isEqualTo( "source" );
        assertThat( ( (TestsetStartingEvent) event ).getReportEntry().getSourceText() ).isEqualTo( "sourceText" );
        assertThat( ( (TestsetStartingEvent) event ).getReportEntry().getName() ).isEqualTo( "name" );
        assertThat( ( (TestsetStartingEvent) event ).getReportEntry().getNameText() ).isEqualTo( "nameText" );
        assertThat( ( (TestsetStartingEvent) event ).getReportEntry().getGroup() ).isEqualTo( "group" );
        assertThat( ( (TestsetStartingEvent) event ).getReportEntry().getMessage() ).isEqualTo( "message" );
        assertThat( ( (TestsetStartingEvent) event ).getReportEntry().getElapsed() ).isEqualTo( 5 );
        assertThat( ( (TestsetStartingEvent) event )
            .getReportEntry().getStackTraceWriter().getThrowable().getLocalizedMessage() )
            .isEqualTo( "traceMessage" );
        assertThat( ( (TestsetStartingEvent) event ).getReportEntry().getStackTraceWriter().smartTrimmedStackTrace() )
            .isEqualTo( "smartTrimmedStackTrace" );
        assertThat( ( (TestsetStartingEvent) event ).getReportEntry().getStackTraceWriter().writeTraceToString() )
            .isEqualTo( "stackTrace" );

        data = asList( "source", "sourceText", "name", "nameText", "group", null, 5,
            "traceMessage", "smartTrimmedStackTrace", "stackTrace" );
        event = toEvent( BOOTERCODE_TESTSET_COMPLETED, NORMAL_RUN, data );
        assertThat( event ).isInstanceOf( TestsetCompletedEvent.class );
        assertThat( ( (TestsetCompletedEvent) event ).getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (TestsetCompletedEvent) event ).getReportEntry().getSourceName() ).isEqualTo( "source" );
        assertThat( ( (TestsetCompletedEvent) event ).getReportEntry().getSourceText() ).isEqualTo( "sourceText" );
        assertThat( ( (TestsetCompletedEvent) event ).getReportEntry().getName() ).isEqualTo( "name" );
        assertThat( ( (TestsetCompletedEvent) event ).getReportEntry().getNameText() ).isEqualTo( "nameText" );
        assertThat( ( (TestsetCompletedEvent) event ).getReportEntry().getGroup() ).isEqualTo( "group" );
        assertThat( ( (TestsetCompletedEvent) event ).getReportEntry().getMessage() ).isNull();
        assertThat( ( (TestsetCompletedEvent) event ).getReportEntry().getElapsed() ).isEqualTo( 5 );
        assertThat( ( (TestsetCompletedEvent) event )
            .getReportEntry().getStackTraceWriter().getThrowable().getLocalizedMessage() )
            .isEqualTo( "traceMessage" );
        assertThat( ( (TestsetCompletedEvent) event ).getReportEntry().getStackTraceWriter().smartTrimmedStackTrace() )
            .isEqualTo( "smartTrimmedStackTrace" );
        assertThat( ( (TestsetCompletedEvent) event ).getReportEntry().getStackTraceWriter().writeTraceToString() )
            .isEqualTo( "stackTrace" );

        data = asList( "source", "sourceText", "name", "nameText", "group", "message", 5,
            null, "smartTrimmedStackTrace", "stackTrace" );
        event = toEvent( BOOTERCODE_TEST_STARTING, NORMAL_RUN, data );
        assertThat( event ).isInstanceOf( TestStartingEvent.class );
        assertThat( ( (TestStartingEvent) event ).getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (TestStartingEvent) event ).getReportEntry().getSourceName() ).isEqualTo( "source" );
        assertThat( ( (TestStartingEvent) event ).getReportEntry().getSourceText() ).isEqualTo( "sourceText" );
        assertThat( ( (TestStartingEvent) event ).getReportEntry().getName() ).isEqualTo( "name" );
        assertThat( ( (TestStartingEvent) event ).getReportEntry().getNameText() ).isEqualTo( "nameText" );
        assertThat( ( (TestStartingEvent) event ).getReportEntry().getGroup() ).isEqualTo( "group" );
        assertThat( ( (TestStartingEvent) event ).getReportEntry().getMessage() ).isEqualTo( "message" );
        assertThat( ( (TestStartingEvent) event ).getReportEntry().getElapsed() ).isEqualTo( 5 );
        assertThat( ( (TestStartingEvent) event )
            .getReportEntry().getStackTraceWriter().getThrowable().getLocalizedMessage() )
            .isNull();
        assertThat( ( (TestStartingEvent) event ).getReportEntry().getStackTraceWriter().smartTrimmedStackTrace() )
            .isEqualTo( "smartTrimmedStackTrace" );
        assertThat( ( (TestStartingEvent) event ).getReportEntry().getStackTraceWriter().writeTraceToString() )
            .isEqualTo( "stackTrace" );

        data = asList( "source", "sourceText", "name", "nameText", "group", "message", 5, null, null, null );
        event = toEvent( BOOTERCODE_TEST_SUCCEEDED, NORMAL_RUN, data );
        assertThat( event ).isInstanceOf( TestSucceededEvent.class );
        assertThat( ( (TestSucceededEvent) event ).getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getSourceName() ).isEqualTo( "source" );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getSourceText() ).isEqualTo( "sourceText" );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getName() ).isEqualTo( "name" );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getNameText() ).isEqualTo( "nameText" );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getGroup() ).isEqualTo( "group" );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getMessage() ).isEqualTo( "message" );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getElapsed() ).isEqualTo( 5 );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getStackTraceWriter() ).isNull();

        data = asList( "source", null, "name", null, "group", null, 5,
            "traceMessage", "smartTrimmedStackTrace", "stackTrace" );
        event = toEvent( BOOTERCODE_TEST_FAILED, RERUN_TEST_AFTER_FAILURE, data );
        assertThat( event ).isInstanceOf( TestFailedEvent.class );
        assertThat( ( (TestFailedEvent) event ).getRunMode() ).isEqualTo( RERUN_TEST_AFTER_FAILURE );
        assertThat( ( (TestFailedEvent) event ).getReportEntry().getSourceName() ).isEqualTo( "source" );
        assertThat( ( (TestFailedEvent) event ).getReportEntry().getSourceText() ).isNull();
        assertThat( ( (TestFailedEvent) event ).getReportEntry().getName() ).isEqualTo( "name" );
        assertThat( ( (TestFailedEvent) event ).getReportEntry().getNameText() ).isNull();
        assertThat( ( (TestFailedEvent) event ).getReportEntry().getGroup() ).isEqualTo( "group" );
        assertThat( ( (TestFailedEvent) event ).getReportEntry().getMessage() ).isNull();
        assertThat( ( (TestFailedEvent) event ).getReportEntry().getElapsed() ).isEqualTo( 5 );
        assertThat( ( (TestFailedEvent) event )
            .getReportEntry().getStackTraceWriter().getThrowable().getLocalizedMessage() )
            .isEqualTo( "traceMessage" );
        assertThat( ( (TestFailedEvent) event ).getReportEntry().getStackTraceWriter().smartTrimmedStackTrace() )
            .isEqualTo( "smartTrimmedStackTrace" );
        assertThat( ( (TestFailedEvent) event ).getReportEntry().getStackTraceWriter().writeTraceToString() )
            .isEqualTo( "stackTrace" );

        data = asList( "source", null, "name", null, null, null, 5, null, null, "stackTrace" );
        event = toEvent( BOOTERCODE_TEST_SKIPPED, NORMAL_RUN, data );
        assertThat( event ).isInstanceOf( TestSkippedEvent.class );
        assertThat( ( (TestSkippedEvent) event ).getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (TestSkippedEvent) event ).getReportEntry().getSourceName() ).isEqualTo( "source" );
        assertThat( ( (TestSkippedEvent) event ).getReportEntry().getSourceText() ).isNull();
        assertThat( ( (TestSkippedEvent) event ).getReportEntry().getName() ).isEqualTo( "name" );
        assertThat( ( (TestSkippedEvent) event ).getReportEntry().getNameText() ).isNull();
        assertThat( ( (TestSkippedEvent) event ).getReportEntry().getGroup() ).isNull();
        assertThat( ( (TestSkippedEvent) event ).getReportEntry().getMessage() ).isNull();
        assertThat( ( (TestSkippedEvent) event ).getReportEntry().getElapsed() ).isEqualTo( 5 );
        assertThat( ( (TestSkippedEvent) event )
            .getReportEntry().getStackTraceWriter().getThrowable().getLocalizedMessage() )
            .isNull();
        assertThat( ( (TestSkippedEvent) event )
            .getReportEntry().getStackTraceWriter().smartTrimmedStackTrace() )
            .isNull();
        assertThat( ( (TestSkippedEvent) event )
            .getReportEntry().getStackTraceWriter().writeTraceToString() )
            .isEqualTo( "stackTrace" );

        data = asList( "source", null, "name", "nameText", null, null, 0, null, null, "stackTrace" );
        event = toEvent( BOOTERCODE_TEST_ERROR, NORMAL_RUN, data );
        assertThat( event ).isInstanceOf( TestErrorEvent.class );
        assertThat( ( (TestErrorEvent) event ).getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (TestErrorEvent) event ).getReportEntry().getSourceName() ).isEqualTo( "source" );
        assertThat( ( (TestErrorEvent) event ).getReportEntry().getSourceText() ).isNull();
        assertThat( ( (TestErrorEvent) event ).getReportEntry().getName() ).isEqualTo( "name" );
        assertThat( ( (TestErrorEvent) event ).getReportEntry().getNameText() ).isEqualTo( "nameText" );
        assertThat( ( (TestErrorEvent) event ).getReportEntry().getGroup() ).isNull();
        assertThat( ( (TestErrorEvent) event ).getReportEntry().getMessage() ).isNull();
        assertThat( ( (TestErrorEvent) event ).getReportEntry().getElapsed() ).isZero();
        assertThat( ( (TestErrorEvent) event )
            .getReportEntry().getStackTraceWriter().getThrowable().getLocalizedMessage() )
            .isNull();
        assertThat( ( (TestErrorEvent) event )
            .getReportEntry().getStackTraceWriter().smartTrimmedStackTrace() )
            .isNull();
        assertThat( ( (TestErrorEvent) event )
            .getReportEntry().getStackTraceWriter().writeTraceToString() )
            .isEqualTo( "stackTrace" );

        data = asList( "source", null, "name", null, "group", null, 5, null, null, "stackTrace" );
        event = toEvent( BOOTERCODE_TEST_ASSUMPTIONFAILURE, NORMAL_RUN, data );
        assertThat( event ).isInstanceOf( TestAssumptionFailureEvent.class );
        assertThat( ( (TestAssumptionFailureEvent) event ).getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (TestAssumptionFailureEvent) event ).getReportEntry().getSourceName() ).isEqualTo( "source" );
        assertThat( ( (TestAssumptionFailureEvent) event ).getReportEntry().getSourceText() ).isNull();
        assertThat( ( (TestAssumptionFailureEvent) event ).getReportEntry().getName() ).isEqualTo( "name" );
        assertThat( ( (TestAssumptionFailureEvent) event ).getReportEntry().getNameText() ).isNull();
        assertThat( ( (TestAssumptionFailureEvent) event ).getReportEntry().getGroup() ).isEqualTo( "group" );
        assertThat( ( (TestAssumptionFailureEvent) event ).getReportEntry().getMessage() ).isNull();
        assertThat( ( (TestAssumptionFailureEvent) event ).getReportEntry().getElapsed() ).isEqualTo( 5 );
        assertThat( ( (TestAssumptionFailureEvent) event )
            .getReportEntry().getStackTraceWriter().getThrowable().getLocalizedMessage() )
            .isNull();
        assertThat( ( (TestAssumptionFailureEvent) event )
            .getReportEntry().getStackTraceWriter().smartTrimmedStackTrace() )
            .isNull();
        assertThat( ( (TestAssumptionFailureEvent) event )
            .getReportEntry().getStackTraceWriter().writeTraceToString() )
            .isEqualTo( "stackTrace" );
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

    private static class MockCloseable implements Closeable
    {
        @Override
        public void close()
        {
        }
    }

    private static class MockEventHandler<T> implements EventHandler<T>
    {
        @Override
        public void handleEvent( @Nonnull T event )
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
    }

    private static class InOrder extends Condition<Object[]>
    {
        private final SegmentType[] expected;

        InOrder( SegmentType... expected )
        {
            this.expected = expected;
        }

        @Override
        public boolean matches( Object[] values )
        {
            if ( values == null && expected == null )
            {
                return true;
            }
            else if ( values != null && expected != null && values.length == expected.length )
            {
                boolean matches = true;
                for ( int i = 0; i < values.length; i++ )
                {

                    assertThat( values[i] ).isInstanceOf( SegmentType.class );
                    matches &= values[i] == expected[i];
                }
                return matches;
            }
            return false;
        }
    }
}
