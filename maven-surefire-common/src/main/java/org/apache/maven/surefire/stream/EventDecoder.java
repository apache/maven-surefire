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

import org.apache.maven.plugin.surefire.booterclient.output.DeserializedStacktraceWriter;
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
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.api.stream.AbstractStreamDecoder;
import org.apache.maven.surefire.api.stream.SegmentType;

import javax.annotation.Nonnull;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static org.apache.maven.surefire.api.booter.Constants.MAGIC_NUMBER_FOR_EVENTS_BYTES;
import static org.apache.maven.surefire.api.report.CategorizedReportEntry.reportEntry;
import static org.apache.maven.surefire.api.stream.SegmentType.DATA_INTEGER;
import static org.apache.maven.surefire.api.stream.SegmentType.DATA_STRING;
import static org.apache.maven.surefire.api.stream.SegmentType.END_OF_FRAME;
import static org.apache.maven.surefire.api.stream.SegmentType.RUN_MODE;
import static org.apache.maven.surefire.api.stream.SegmentType.STRING_ENCODING;

/**
 *
 */
public class EventDecoder extends AbstractStreamDecoder<Event, ForkedProcessEventType, SegmentType>
{
    private static final int DEBUG_SINK_BUFFER_SIZE = 64 * 1024;
    // due to have fast and thread-safe Map
    private static final Map<Segment, ForkedProcessEventType> EVENT_TYPES = segmentsToEvents();
    private static final Map<Segment, RunMode> RUN_MODES = segmentsToRunModes();

    private static final SegmentType[] EVENT_WITHOUT_DATA = new SegmentType[] {
        END_OF_FRAME
    };

    private static final SegmentType[] EVENT_WITH_ERROR_TRACE = new SegmentType[] {
        STRING_ENCODING,
        DATA_STRING,
        DATA_STRING,
        DATA_STRING,
        END_OF_FRAME
    };

    private static final SegmentType[] EVENT_WITH_ONE_STRING = new SegmentType[] {
        STRING_ENCODING,
        DATA_STRING,
        END_OF_FRAME
    };

    private static final SegmentType[] EVENT_WITH_RUNMODE_AND_ONE_STRING = new SegmentType[] {
        RUN_MODE,
        STRING_ENCODING,
        DATA_STRING,
        END_OF_FRAME
    };

    private static final SegmentType[] EVENT_WITH_RUNMODE_AND_TWO_STRINGS = new SegmentType[] {
        RUN_MODE,
        STRING_ENCODING,
        DATA_STRING,
        DATA_STRING,
        END_OF_FRAME
    };

    private static final SegmentType[] EVENT_TEST_CONTROL = new SegmentType[] {
        RUN_MODE,
        STRING_ENCODING,
        DATA_STRING,
        DATA_STRING,
        DATA_STRING,
        DATA_STRING,
        DATA_STRING,
        DATA_STRING,
        DATA_INTEGER,
        DATA_STRING,
        DATA_STRING,
        DATA_STRING,
        END_OF_FRAME
    };

    private static final int NO_POSITION = -1;

    private final OutputStream debugSink;

    public EventDecoder( @Nonnull ReadableByteChannel channel,
                         @Nonnull ForkNodeArguments arguments )
    {
        super( channel, arguments, EVENT_TYPES );
        debugSink = newDebugSink( arguments );
    }

    @Override
    public Event decode( @Nonnull Memento memento ) throws IOException
    {
        try
        {
            ForkedProcessEventType eventType = readMessageType( memento );
            if ( eventType == null )
            {
                throw new MalformedFrameException( memento.getLine().getPositionByteBuffer(),
                    memento.getByteBuffer().position() );
            }
            RunMode runMode = null;
            for ( SegmentType segmentType : nextSegmentType( eventType ) )
            {
                switch ( segmentType )
                {
                    case RUN_MODE:
                        runMode = RUN_MODES.get( readSegment( memento ) );
                        break;
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
                        return toMessage( eventType, runMode, memento );
                    default:
                        memento.getLine().setPositionByteBuffer( NO_POSITION );
                        getArguments()
                            .dumpStreamText( "Unknown enum ("
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

        throw new IOException( "unreachable statement" );
    }

    @Nonnull
    @Override
    protected final byte[] getEncodedMagicNumber()
    {
        return MAGIC_NUMBER_FOR_EVENTS_BYTES;
    }

    @Override
    @Nonnull
    protected final SegmentType[] nextSegmentType( @Nonnull ForkedProcessEventType eventType )
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

    @Override
    @Nonnull
    protected final Event toMessage( @Nonnull ForkedProcessEventType eventType,
                                     RunMode runMode,
                                     @Nonnull Memento memento )
        throws MalformedFrameException
    {
        switch ( eventType )
        {
            case BOOTERCODE_BYE:
                checkArguments( memento, 0 );
                return new ControlByeEvent();
            case BOOTERCODE_STOP_ON_NEXT_TEST:
                checkArguments( memento, 0 );
                return new ControlStopOnNextTestEvent();
            case BOOTERCODE_NEXT_TEST:
                checkArguments( memento, 0 );
                return new ControlNextTestEvent();
            case BOOTERCODE_CONSOLE_ERROR:
                checkArguments( memento, 3 );
                return new ConsoleErrorEvent( toStackTraceWriter( memento.getData() ) );
            case BOOTERCODE_JVM_EXIT_ERROR:
                checkArguments( memento, 3 );
                return new JvmExitErrorEvent( toStackTraceWriter( memento.getData() ) );
            case BOOTERCODE_CONSOLE_INFO:
                checkArguments( memento, 1 );
                return new ConsoleInfoEvent( (String) memento.getData().get( 0 ) );
            case BOOTERCODE_CONSOLE_DEBUG:
                checkArguments( memento, 1 );
                return new ConsoleDebugEvent( (String) memento.getData().get( 0 ) );
            case BOOTERCODE_CONSOLE_WARNING:
                checkArguments( memento, 1 );
                return new ConsoleWarningEvent( (String) memento.getData().get( 0 ) );
            case BOOTERCODE_STDOUT:
                checkArguments( runMode, memento, 1 );
                return new StandardStreamOutEvent( runMode, (String) memento.getData().get( 0 ) );
            case BOOTERCODE_STDOUT_NEW_LINE:
                checkArguments( runMode, memento, 1 );
                return new StandardStreamOutWithNewLineEvent( runMode, (String) memento.getData().get( 0 ) );
            case BOOTERCODE_STDERR:
                checkArguments( runMode, memento, 1 );
                return new StandardStreamErrEvent( runMode, (String) memento.getData().get( 0 ) );
            case BOOTERCODE_STDERR_NEW_LINE:
                checkArguments( runMode, memento, 1 );
                return new StandardStreamErrWithNewLineEvent( runMode, (String) memento.getData().get( 0 ) );
            case BOOTERCODE_SYSPROPS:
                checkArguments( runMode, memento, 2 );
                String key = (String) memento.getData().get( 0 );
                String value = (String) memento.getData().get( 1 );
                return new SystemPropertyEvent( runMode, key, value );
            case BOOTERCODE_TESTSET_STARTING:
                checkArguments( runMode, memento, 10 );
                return new TestsetStartingEvent( runMode, toReportEntry( memento.getData() ) );
            case BOOTERCODE_TESTSET_COMPLETED:
                checkArguments( runMode, memento, 10 );
                return new TestsetCompletedEvent( runMode, toReportEntry( memento.getData() ) );
            case BOOTERCODE_TEST_STARTING:
                checkArguments( runMode, memento, 10 );
                return new TestStartingEvent( runMode, toReportEntry( memento.getData() ) );
            case BOOTERCODE_TEST_SUCCEEDED:
                checkArguments( runMode, memento, 10 );
                return new TestSucceededEvent( runMode, toReportEntry( memento.getData() ) );
            case BOOTERCODE_TEST_FAILED:
                checkArguments( runMode, memento, 10 );
                return new TestFailedEvent( runMode, toReportEntry( memento.getData() ) );
            case BOOTERCODE_TEST_SKIPPED:
                checkArguments( runMode, memento, 10 );
                return new TestSkippedEvent( runMode, toReportEntry( memento.getData() ) );
            case BOOTERCODE_TEST_ERROR:
                checkArguments( runMode, memento, 10 );
                return new TestErrorEvent( runMode, toReportEntry( memento.getData() ) );
            case BOOTERCODE_TEST_ASSUMPTIONFAILURE:
                checkArguments( runMode, memento, 10 );
                return new TestAssumptionFailureEvent( runMode, toReportEntry( memento.getData() ) );
            default:
                throw new IllegalArgumentException( "Missing a branch for the event type " + eventType );
        }
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

    private static Map<Segment, ForkedProcessEventType> segmentsToEvents()
    {
        Map<Segment, ForkedProcessEventType> events = new HashMap<>();
        for ( ForkedProcessEventType event : ForkedProcessEventType.values() )
        {
            byte[] array = event.getOpcodeBinary();
            events.put( new Segment( array, 0, array.length ), event );
        }
        return events;
    }

    private static Map<Segment, RunMode> segmentsToRunModes()
    {
        Map<Segment, RunMode> runModes = new HashMap<>();
        for ( RunMode runMode : RunMode.values() )
        {
            byte[] array = runMode.getRunmodeBinary();
            runModes.put( new Segment( array, 0, array.length ), runMode );
        }
        return runModes;
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
            debugSink.flush();
        }
        catch ( IOException e )
        {
            // logger file was deleted
            // System.out is already used by the stream in this decoder
        }
    }

    private OutputStream newDebugSink( ForkNodeArguments arguments )
    {
        final File sink = arguments.getEventStreamBinaryFile();
        if ( sink == null )
        {
            return null;
        }

        try
        {
            OutputStream fos = new FileOutputStream( sink, true );
            final OutputStream os = new BufferedOutputStream( fos, DEBUG_SINK_BUFFER_SIZE );
            Runtime.getRuntime().addShutdownHook( new Thread( new FutureTask<>( new Callable<Void>()
            {
                @Override
                public Void call() throws Exception
                {
                    os.close();
                    return null;
                }
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
        // do NOT close the channel, it's std/out.
        if ( debugSink != null )
        {
            debugSink.close();
        }
    }
}
