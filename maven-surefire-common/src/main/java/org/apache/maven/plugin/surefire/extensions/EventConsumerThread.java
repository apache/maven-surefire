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

import org.apache.maven.plugin.surefire.booterclient.output.DeserializedStacktraceWriter;
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
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.extensions.CloseableDaemonThread;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.extensions.ForkNodeArguments;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.nio.charset.CodingErrorAction.REPLACE;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.apache.maven.plugin.surefire.extensions.EventConsumerThread.StreamReadStatus.OVERFLOW;
import static org.apache.maven.plugin.surefire.extensions.EventConsumerThread.StreamReadStatus.UNDERFLOW;
import static org.apache.maven.surefire.api.booter.Constants.DEFAULT_STREAM_ENCODING;
import static org.apache.maven.surefire.api.booter.Constants.MAGIC_NUMBER_BYTES;
import static org.apache.maven.surefire.api.report.CategorizedReportEntry.reportEntry;

/**
 *
 */
public class EventConsumerThread extends CloseableDaemonThread
{
    private static final String[] JVM_ERROR_PATTERNS =
        {
            "could not create the java virtual machine",
            "error occurred during initialization", // of VM, of boot layer
            "error:", // general errors
            "could not reserve enough space", "could not allocate", "unable to allocate", // memory errors
            "java.lang.module.findexception" // JPMS errors
        };

    private static final String PRINTABLE_JVM_NATIVE_STREAM = "Listening for transport dt_socket at address:";

    private static final SegmentType[] EVENT_WITHOUT_DATA = new SegmentType[] {
        SegmentType.END_OF_FRAME
    };

    private static final SegmentType[] EVENT_WITH_ERROR_TRACE = new SegmentType[] {
        SegmentType.STRING_ENCODING,
        SegmentType.DATA_STRING,
        SegmentType.DATA_STRING,
        SegmentType.DATA_STRING,
        SegmentType.END_OF_FRAME
    };

    private static final SegmentType[] EVENT_WITH_ONE_STRING = new SegmentType[] {
        SegmentType.STRING_ENCODING,
        SegmentType.DATA_STRING,
        SegmentType.END_OF_FRAME
    };

    private static final SegmentType[] EVENT_WITH_RUNMODE_AND_ONE_STRING = new SegmentType[] {
        SegmentType.RUN_MODE,
        SegmentType.STRING_ENCODING,
        SegmentType.DATA_STRING,
        SegmentType.END_OF_FRAME
    };

    private static final SegmentType[] EVENT_WITH_RUNMODE_AND_TWO_STRINGS = new SegmentType[] {
        SegmentType.RUN_MODE,
        SegmentType.STRING_ENCODING,
        SegmentType.DATA_STRING,
        SegmentType.DATA_STRING,
        SegmentType.END_OF_FRAME
    };

    private static final SegmentType[] EVENT_TEST_CONTROL = new SegmentType[] {
        SegmentType.RUN_MODE,
        SegmentType.STRING_ENCODING,
        SegmentType.DATA_STRING,
        SegmentType.DATA_STRING,
        SegmentType.DATA_STRING,
        SegmentType.DATA_STRING,
        SegmentType.DATA_STRING,
        SegmentType.DATA_STRING,
        SegmentType.DATA_INT,
        SegmentType.DATA_STRING,
        SegmentType.DATA_STRING,
        SegmentType.DATA_STRING,
        SegmentType.END_OF_FRAME
    };

    private static final int BUFFER_SIZE = 1024;
    private static final byte[] DEFAULT_STREAM_ENCODING_BYTES = DEFAULT_STREAM_ENCODING.name().getBytes( US_ASCII );
    private static final int DELIMITER_LENGTH = 1;
    private static final int BYTE_LENGTH = 1;
    private static final int INT_LENGTH = 4;
    private static final int NO_POSITION = -1;

    private final ReadableByteChannel channel;
    private final EventHandler<Event> eventHandler;
    private final CountdownCloseable countdownCloseable;
    private final ForkNodeArguments arguments;
    private volatile boolean disabled;

    public EventConsumerThread( @Nonnull String threadName,
                                @Nonnull ReadableByteChannel channel,
                                @Nonnull EventHandler<Event> eventHandler,
                                @Nonnull CountdownCloseable countdownCloseable,
                                @Nonnull ForkNodeArguments arguments )
    {
        super( threadName );
        this.channel = channel;
        this.eventHandler = eventHandler;
        this.countdownCloseable = countdownCloseable;
        this.arguments = arguments;
    }

    @Override
    public void run()
    {
        try ( ReadableByteChannel stream = channel;
              CountdownCloseable c = countdownCloseable; )
        {
            decode();
        }
        catch ( IOException e )
        {
            // not needed
        }
    }

    @Override
    public void disable()
    {
        disabled = true;
    }

    @Override
    public void close() throws IOException
    {
        channel.close();
    }

    @SuppressWarnings( "checkstyle:innerassignment" )
    private void decode() throws IOException
    {
        Map<Segment, ForkedProcessEventType> eventTypes = mapEventTypes();
        Map<Segment, RunMode> runModes = mapRunModes();
        Memento memento = new Memento();
        memento.bb.limit( 0 );

        do
        {
            try
            {
                ForkedProcessEventType eventType = readEventType( eventTypes, memento );
                if ( eventType == null )
                {
                    throw new MalformedFrameException( memento.line.positionByteBuffer, memento.bb.position() );
                }
                RunMode runMode = null;
                for ( SegmentType segmentType : nextSegmentType( eventType ) )
                {
                    if ( segmentType == null )
                    {
                        break;
                    }

                    switch ( segmentType )
                    {
                        case RUN_MODE:
                            runMode = runModes.get( readSegment( memento ) );
                            break;
                        case STRING_ENCODING:
                            memento.setCharset( readCharset( memento ) );
                            break;
                        case DATA_STRING:
                            memento.data.add( readString( memento ) );
                            break;
                        case DATA_INT:
                            memento.data.add( readInteger( memento ) );
                            break;
                        case END_OF_FRAME:
                            memento.line.positionByteBuffer = memento.bb.position();
                            if ( !disabled )
                            {
                                eventHandler.handleEvent( toEvent( eventType, runMode, memento.data ) );
                            }
                            break;
                        default:
                            memento.line.positionByteBuffer = NO_POSITION;
                            arguments.dumpStreamText( "Unknown enum ("
                                + ForkedProcessEventType.class.getSimpleName()
                                + ") "
                                + segmentType );
                    }
                }
            }
            catch ( MalformedFrameException e )
            {
                if ( e.hasValidPositions() )
                {
                    int length = e.readTo - e.readFrom;
                    memento.line.write( memento.bb, e.readFrom, length );
                }
            }
            catch ( RuntimeException e )
            {
                arguments.dumpStreamException( e );
            }
            catch ( IOException e )
            {
                printRemainingStream( memento );
                throw e;
            }
            finally
            {
                memento.reset();
            }
        }
        while ( true );
    }

    protected ForkedProcessEventType readEventType( Map<Segment, ForkedProcessEventType> eventTypes, Memento memento )
        throws IOException, MalformedFrameException
    {
        int readCount = DELIMITER_LENGTH + MAGIC_NUMBER_BYTES.length + DELIMITER_LENGTH
            + BYTE_LENGTH + DELIMITER_LENGTH;
        read( memento, readCount );
        checkHeader( memento );
        return eventTypes.get( readSegment( memento ) );
    }

    protected String readString( Memento memento ) throws IOException, MalformedFrameException
    {
        memento.cb.clear();
        int readCount = readInt( memento );
        read( memento, readCount + DELIMITER_LENGTH );

        final String string;
        if ( readCount == 0 )
        {
            string = "";
        }
        else if ( readCount == 1 )
        {
            read( memento, 1 );
            byte oneChar = memento.bb.get();
            string = oneChar == 0 ? null : String.valueOf( (char) oneChar );
        }
        else
        {
            string = readString( memento, readCount );
        }

        checkDelimiter( memento );
        return string;
    }

    @Nonnull
    @SuppressWarnings( "checkstyle:magicnumber" )
    protected Segment readSegment( Memento memento ) throws IOException, MalformedFrameException
    {
        int readCount = readByte( memento ) & 0xff;
        read( memento, readCount + DELIMITER_LENGTH );
        ByteBuffer bb = memento.bb;
        Segment segment = new Segment( bb.array(), bb.arrayOffset() + bb.position(), readCount );
        bb.position( bb.position() + readCount );
        checkDelimiter( memento );
        return segment;
    }

    @Nonnull
    @SuppressWarnings( "checkstyle:magicnumber" )
    protected Charset readCharset( Memento memento ) throws IOException, MalformedFrameException
    {
        int length = readByte( memento ) & 0xff;
        read( memento, length + DELIMITER_LENGTH );
        ByteBuffer bb = memento.bb;
        byte[] array = bb.array();
        int offset = bb.arrayOffset() + bb.position();
        bb.position( bb.position() + length );
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
            throw new MalformedFrameException( memento.line.positionByteBuffer, bb.position() );
        }
    }

    private static void checkHeader( Memento memento ) throws MalformedFrameException
    {
        ByteBuffer bb = memento.bb;

        checkDelimiter( memento );

        int shift = 0;
        try
        {
            for ( int start = bb.arrayOffset() + bb.position(), length = MAGIC_NUMBER_BYTES.length;
                  shift < length; shift++ )
            {
                if ( bb.array()[shift + start] != MAGIC_NUMBER_BYTES[shift] )
                {
                    throw new MalformedFrameException( memento.line.positionByteBuffer, bb.position() + shift );
                }
            }
        }
        finally
        {
            bb.position( bb.position() + shift );
        }

        checkDelimiter( memento );
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    private static void checkDelimiter( Memento memento ) throws MalformedFrameException
    {
        ByteBuffer bb = memento.bb;
        if ( ( 0xff & bb.get() ) != ':' )
        {
            throw new MalformedFrameException( memento.line.positionByteBuffer, bb.position() );
        }
    }

    static SegmentType[] nextSegmentType( ForkedProcessEventType eventType )
    {
        switch ( eventType )
        {
            case BOOTERCODE_BYE:
            case BOOTERCODE_STOP_ON_NEXT_TEST:
            case BOOTERCODE_NEXT_TEST:
                return EVENT_WITHOUT_DATA;
            case BOOTERCODE_CONSOLE_ERROR:
            case BOOTERCODE_JVM_EXIT_ERROR:
                return EVENT_WITH_ERROR_TRACE;
            case BOOTERCODE_CONSOLE_INFO:
            case BOOTERCODE_CONSOLE_DEBUG:
            case BOOTERCODE_CONSOLE_WARNING:
                return EVENT_WITH_ONE_STRING;
            case BOOTERCODE_STDOUT:
            case BOOTERCODE_STDOUT_NEW_LINE:
            case BOOTERCODE_STDERR:
            case BOOTERCODE_STDERR_NEW_LINE:
                return EVENT_WITH_RUNMODE_AND_ONE_STRING;
            case BOOTERCODE_SYSPROPS:
                return EVENT_WITH_RUNMODE_AND_TWO_STRINGS;
            case BOOTERCODE_TESTSET_STARTING:
            case BOOTERCODE_TESTSET_COMPLETED:
            case BOOTERCODE_TEST_STARTING:
            case BOOTERCODE_TEST_SUCCEEDED:
            case BOOTERCODE_TEST_FAILED:
            case BOOTERCODE_TEST_SKIPPED:
            case BOOTERCODE_TEST_ERROR:
            case BOOTERCODE_TEST_ASSUMPTIONFAILURE:
                return EVENT_TEST_CONTROL;
            default:
                throw new IllegalArgumentException( "Unknown enum " + eventType );
        }
    }

    protected StreamReadStatus read( Memento memento, int recommendedCount ) throws IOException
    {
        ByteBuffer buffer = memento.bb;
        if ( buffer.remaining() >= recommendedCount && buffer.position() != 0 )
        {
            return OVERFLOW;
        }
        else
        {
            if ( buffer.position() != 0 && recommendedCount > buffer.capacity() - buffer.position() )
            {
                buffer.compact().flip();
                memento.line.positionByteBuffer = 0;
            }
            int mark = buffer.position();
            buffer.position( buffer.limit() );
            buffer.limit( buffer.capacity() );
            boolean isEnd = false;
            while ( !isEnd && buffer.position() - mark < recommendedCount && buffer.position() != buffer.limit() )
            {
                isEnd = channel.read( buffer ) == -1;
            }

            buffer.limit( buffer.position() );
            buffer.position( mark );
            int readBytes = buffer.remaining();

            if ( isEnd && readBytes < recommendedCount )
            {
                throw new EOFException();
            }
            else
            {
                return readBytes >= recommendedCount ? OVERFLOW : UNDERFLOW;
            }
        }
    }

    static Event toEvent( ForkedProcessEventType eventType, RunMode runMode, List<Object> args )
    {
        switch ( eventType )
        {
            case BOOTERCODE_BYE:
                return new ControlByeEvent();
            case BOOTERCODE_STOP_ON_NEXT_TEST:
                return new ControlStopOnNextTestEvent();
            case BOOTERCODE_NEXT_TEST:
                return new ControlNextTestEvent();
            case BOOTERCODE_CONSOLE_ERROR:
                return new ConsoleErrorEvent( toStackTraceWriter( args ) );
            case BOOTERCODE_JVM_EXIT_ERROR:
                return new JvmExitErrorEvent( toStackTraceWriter( args ) );
            case BOOTERCODE_CONSOLE_INFO:
                return new ConsoleInfoEvent( (String) args.get( 0 ) );
            case BOOTERCODE_CONSOLE_DEBUG:
                return new ConsoleDebugEvent( (String) args.get( 0 ) );
            case BOOTERCODE_CONSOLE_WARNING:
                return new ConsoleWarningEvent( (String) args.get( 0 ) );
            case BOOTERCODE_STDOUT:
                return new StandardStreamOutEvent( runMode, (String) args.get( 0 ) );
            case BOOTERCODE_STDOUT_NEW_LINE:
                return new StandardStreamOutWithNewLineEvent( runMode, (String) args.get( 0 ) );
            case BOOTERCODE_STDERR:
                return new StandardStreamErrEvent( runMode, (String) args.get( 0 ) );
            case BOOTERCODE_STDERR_NEW_LINE:
                return new StandardStreamErrWithNewLineEvent( runMode, (String) args.get( 0 ) );
            case BOOTERCODE_SYSPROPS:
                String key = (String) args.get( 0 );
                String value = (String) args.get( 1 );
                return new SystemPropertyEvent( runMode, key, value );
            case BOOTERCODE_TESTSET_STARTING:
                return new TestsetStartingEvent( runMode, toReportEntry( args ) );
            case BOOTERCODE_TESTSET_COMPLETED:
                return new TestsetCompletedEvent( runMode, toReportEntry( args ) );
            case BOOTERCODE_TEST_STARTING:
                return new TestStartingEvent( runMode, toReportEntry( args ) );
            case BOOTERCODE_TEST_SUCCEEDED:
                return new TestSucceededEvent( runMode, toReportEntry( args ) );
            case BOOTERCODE_TEST_FAILED:
                return new TestFailedEvent( runMode, toReportEntry( args ) );
            case BOOTERCODE_TEST_SKIPPED:
                return new TestSkippedEvent( runMode, toReportEntry( args ) );
            case BOOTERCODE_TEST_ERROR:
                return new TestErrorEvent( runMode, toReportEntry( args ) );
            case BOOTERCODE_TEST_ASSUMPTIONFAILURE:
                return new TestAssumptionFailureEvent( runMode, toReportEntry( args ) );
            default:
                throw new IllegalArgumentException( "Missing a branch for the event type " + eventType );
        }
    }

    private static void printCorruptedStream( Memento memento )
    {
        ByteBuffer bb = memento.bb;
        if ( bb.hasRemaining() )
        {
            int bytesToWrite = bb.remaining();
            memento.line.write( bb, bb.position(), bytesToWrite );
            bb.position( bb.position() + bytesToWrite );
        }
    }

    /**
     * Print the last string which has not been finished by a new line character.
     *
     * @param memento current memento object
     */
    private static void printRemainingStream( Memento memento )
    {
        printCorruptedStream( memento );
        memento.line.printExistingLine();
        memento.line.count = 0;
    }

    @Nonnull
    private static TestSetReportEntry toReportEntry( List<Object> args )
    {
        // ReportEntry:
        String source = (String) args.get( 0 );
        String sourceText = (String) args.get( 1 );
        String name = (String) args.get( 2 );
        String nameText = (String) args.get( 3 );
        String group = (String) args.get( 4 );
        String message = (String) args.get( 5 );
        Integer timeElapsed = (Integer) args.get( 6 );
        // StackTraceWriter:
        String traceMessage = (String) args.get( 7 );
        String smartTrimmedStackTrace = (String) args.get( 8 );
        String stackTrace = (String) args.get( 9 );
        return newReportEntry( source, sourceText, name, nameText, group, message, timeElapsed,
            traceMessage, smartTrimmedStackTrace, stackTrace );
    }

    private static StackTraceWriter toStackTraceWriter( List<Object> args )
    {
        String traceMessage = (String) args.get( 0 );
        String smartTrimmedStackTrace = (String) args.get( 1 );
        String stackTrace = (String) args.get( 2 );
        return toTrace( traceMessage, smartTrimmedStackTrace, stackTrace );
    }

    private static StackTraceWriter toTrace( String traceMessage, String smartTrimmedStackTrace, String stackTrace )
    {
        boolean exists = traceMessage != null || stackTrace != null || smartTrimmedStackTrace != null;
        return exists ? new DeserializedStacktraceWriter( traceMessage, smartTrimmedStackTrace, stackTrace ) : null;
    }

    static TestSetReportEntry newReportEntry( // ReportEntry:
                                              String source, String sourceText, String name,
                                              String nameText, String group, String message,
                                              Integer timeElapsed,
                                              // StackTraceWriter:
                                              String traceMessage,
                                              String smartTrimmedStackTrace, String stackTrace )
        throws NumberFormatException
    {
        StackTraceWriter stackTraceWriter = toTrace( traceMessage, smartTrimmedStackTrace, stackTrace );
        return reportEntry( source, sourceText, name, nameText, group, stackTraceWriter, timeElapsed, message,
            Collections.<String, String>emptyMap() );
    }

    private static boolean isJvmError( String line )
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

    private static int decodeString( @Nonnull CharsetDecoder decoder, @Nonnull ByteBuffer input,
                                     @Nonnull CharBuffer output, @Nonnegative int bytesToDecode,
                                     boolean endOfInput, @Nonnegative int errorStreamFrom )
        throws MalformedFrameException
    {
        int limit = input.limit();
        input.limit( input.position() + bytesToDecode );

        CoderResult result = decoder.decode( input, output, endOfInput );
        if ( result.isError() || result.isMalformed() )
        {
            throw new MalformedFrameException( errorStreamFrom, input.position() );
        }
        
        int decodedBytes = bytesToDecode - input.remaining();
        input.limit( limit );
        return decodedBytes;
    }

    String readString( @Nonnull final Memento memento, @Nonnegative final int totalBytes )
        throws IOException, MalformedFrameException
    {
        memento.getDecoder().reset();
        final CharBuffer output = memento.cb;
        output.clear();
        final ByteBuffer input = memento.bb;
        final List<String> strings = new ArrayList<>();
        int countDecodedBytes = 0;
        for ( boolean endOfInput = false; !endOfInput; )
        {
            final int bytesToRead = totalBytes - countDecodedBytes;
            read( memento, bytesToRead - input.remaining() );
            int bytesToDecode = min( input.remaining(), bytesToRead );
            final boolean isLastChunk = bytesToDecode == bytesToRead;
            endOfInput = countDecodedBytes + bytesToDecode >= totalBytes;
            do
            {
                boolean endOfChunk = output.remaining() >= bytesToRead;
                boolean endOfOutput = isLastChunk && endOfChunk;
                int readInputBytes = decodeString( memento.getDecoder(), input, output, bytesToDecode, endOfOutput,
                    memento.line.positionByteBuffer );
                bytesToDecode -= readInputBytes;
                countDecodedBytes += readInputBytes;
            }
            while ( isLastChunk && bytesToDecode > 0 && output.hasRemaining() );

            if ( isLastChunk || !output.hasRemaining() )
            {
                strings.add( output.flip().toString() );
                output.clear();
            }
        }

        memento.getDecoder().reset();
        output.clear();

        return toString( strings );
    }

    protected byte readByte( Memento memento ) throws IOException, MalformedFrameException
    {
        read( memento, BYTE_LENGTH + DELIMITER_LENGTH );
        byte b = memento.bb.get();
        checkDelimiter( memento );
        return b;
    }

    protected int readInt( Memento memento ) throws IOException, MalformedFrameException
    {
        read( memento, INT_LENGTH + DELIMITER_LENGTH );
        int i = memento.bb.getInt();
        checkDelimiter( memento );
        return i;
    }

    protected Integer readInteger( Memento memento ) throws IOException, MalformedFrameException
    {
        read( memento, BYTE_LENGTH );
        boolean isNullObject = memento.bb.get() == 0;
        if ( isNullObject )
        {
            read( memento, DELIMITER_LENGTH );
            checkDelimiter( memento );
            return null;
        }
        return readInt( memento );
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

    static Map<Segment, ForkedProcessEventType> mapEventTypes()
    {
        Map<Segment, ForkedProcessEventType> map = new HashMap<>();
        for ( ForkedProcessEventType e : ForkedProcessEventType.values() )
        {
            byte[] array = e.getOpcode().getBytes( US_ASCII );
            map.put( new Segment( array, 0, array.length ), e );
        }
        return map;
    }

    static Map<Segment, RunMode> mapRunModes()
    {
        Map<Segment, RunMode> map = new HashMap<>();
        for ( RunMode e : RunMode.values() )
        {
            byte[] array = e.geRunmode().getBytes( US_ASCII );
            map.put( new Segment( array, 0, array.length ), e );
        }
        return map;
    }

    enum StreamReadStatus
    {
        UNDERFLOW,
        OVERFLOW,
        EOF
    }

    enum SegmentType
    {
        RUN_MODE,
        STRING_ENCODING,
        DATA_STRING,
        DATA_INT,
        END_OF_FRAME
    }

    /**
     * This class avoids locking which gains the performance of this decoder.
     */
    private class BufferedStream
    {
        private byte[] buffer;
        private int count;
        private int positionByteBuffer;
        private boolean isNewLine;

        BufferedStream( int capacity )
        {
            this.buffer = new byte[capacity];
        }

        void write( ByteBuffer bb, int position, int length )
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

        private boolean isEmpty()
        {
            return count == 0;
        }

        @Override
        public String toString()
        {
            return new String( buffer, 0, count, DEFAULT_STREAM_ENCODING );
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
                buffer = Arrays.copyOf( buffer, max( newCapacity, exactCapacity ) );
            }
        }

        void printExistingLine()
        {
            if ( isEmpty() )
            {
                return;
            }
            ConsoleLogger logger = arguments.getConsoleLogger();
            String s = toString();
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

                String msg = "Corrupted STDOUT by directly writing to native stream in forked JVM "
                    + arguments.getForkChannelId() + ".";
                File dumpFile = arguments.dumpStreamText( msg + " Stream '" + s + "'." );
                arguments.logWarningAtEnd( msg + " See FAQ web page and the dump file " + dumpFile.getAbsolutePath() );
            }
        }
    }

    class Memento
    {
        private final CharsetDecoder defaultDecoder;
        private CharsetDecoder currentDecoder;
        final BufferedStream line = new BufferedStream( 32 );
        final List<Object> data = new ArrayList<>();
        final CharBuffer cb = CharBuffer.allocate( BUFFER_SIZE );
        final ByteBuffer bb = ByteBuffer.allocate( BUFFER_SIZE );

        Memento()
        {
            defaultDecoder = DEFAULT_STREAM_ENCODING.newDecoder()
                .onMalformedInput( REPLACE )
                .onUnmappableCharacter( REPLACE );
        }

        void reset()
        {
            currentDecoder = null;
            data.clear();
        }

        CharsetDecoder getDecoder()
        {
            return currentDecoder == null ? defaultDecoder : currentDecoder;
        }

        void setCharset( Charset charset )
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
    }

    static class Segment
    {
        private final byte[] array;
        private final int fromIndex;
        private final int length;
        private final int hashCode;

        Segment( byte[] array, int fromIndex, int length )
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

    /**
     *
     */
    static class MalformedFrameException extends Exception
    {
        private final int readFrom;
        private final int readTo;

        MalformedFrameException( int readFrom, int readTo )
        {
            this.readFrom = readFrom;
            this.readTo = readTo;
        }

        boolean hasValidPositions()
        {
            return readFrom != NO_POSITION && readTo != NO_POSITION && readTo - readFrom > 0;
        }
    }
}
