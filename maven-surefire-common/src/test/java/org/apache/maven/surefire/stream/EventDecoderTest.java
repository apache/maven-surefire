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
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.report.SafeThrowable;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.stream.AbstractStreamDecoder.Memento;
import org.apache.maven.surefire.api.stream.AbstractStreamDecoder.Segment;
import org.apache.maven.surefire.api.stream.SegmentType;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;

import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
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
import static org.apache.maven.surefire.api.stream.SegmentType.DATA_INTEGER;
import static org.apache.maven.surefire.api.stream.SegmentType.DATA_STRING;
import static org.apache.maven.surefire.api.stream.SegmentType.END_OF_FRAME;
import static org.apache.maven.surefire.api.stream.SegmentType.RUN_MODE;
import static org.apache.maven.surefire.api.stream.SegmentType.STRING_ENCODING;
import static org.apache.maven.surefire.api.stream.SegmentType.TEST_RUN_ID;
import static org.apache.maven.surefire.stream.EventDecoder.newReportEntry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 *
 */
public class EventDecoderTest
{
    @Test
    public void shouldMapEventTypes() throws Exception
    {
        Map<Segment, ForkedProcessEventType> eventTypes = invokeMethod( EventDecoder.class, "segmentsToEvents" );
        assertThat( eventTypes )
            .hasSize( ForkedProcessEventType.values().length );
    }

    @Test
    public void shouldMapRunModes() throws Exception
    {
        Map<Segment, RunMode> map = invokeMethod( EventDecoder.class, "segmentsToRunModes" );

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
    public void shouldMapEventTypeToSegmentType()
    {
        byte[] stream = {};
        Channel channel = new Channel( stream, 1 );
        EventDecoder decoder = new EventDecoder( channel, new MockForkNodeArguments() );

        SegmentType[] segmentTypes = decoder.nextSegmentType( BOOTERCODE_BYE );
        assertThat( segmentTypes )
            .hasSize( 1 )
            .containsOnly( END_OF_FRAME );

        segmentTypes = decoder.nextSegmentType( BOOTERCODE_STOP_ON_NEXT_TEST );
        assertThat( segmentTypes )
            .hasSize( 1 )
            .containsOnly( END_OF_FRAME );

        segmentTypes = decoder.nextSegmentType( BOOTERCODE_NEXT_TEST );
        assertThat( segmentTypes )
            .hasSize( 1 )
            .containsOnly( END_OF_FRAME );

        segmentTypes = decoder.nextSegmentType( BOOTERCODE_CONSOLE_ERROR );
        assertThat( segmentTypes )
            .hasSize( 5 )
            .isEqualTo( new SegmentType[] { STRING_ENCODING, DATA_STRING, DATA_STRING, DATA_STRING, END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( ForkedProcessEventType.BOOTERCODE_JVM_EXIT_ERROR );
        assertThat( segmentTypes )
            .hasSize( 5 )
            .isEqualTo( new SegmentType[] { STRING_ENCODING, DATA_STRING, DATA_STRING, DATA_STRING, END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( BOOTERCODE_CONSOLE_INFO );
        assertThat( segmentTypes )
            .hasSize( 3 )
            .isEqualTo( new SegmentType[] { STRING_ENCODING, DATA_STRING, END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( ForkedProcessEventType.BOOTERCODE_CONSOLE_DEBUG );
        assertThat( segmentTypes )
            .hasSize( 3 )
            .isEqualTo( new SegmentType[] { STRING_ENCODING, DATA_STRING, END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( ForkedProcessEventType.BOOTERCODE_CONSOLE_WARNING );
        assertThat( segmentTypes )
            .hasSize( 3 )
            .isEqualTo( new SegmentType[] { STRING_ENCODING, DATA_STRING, END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( BOOTERCODE_STDOUT );
        assertThat( segmentTypes )
            .hasSize( 5 )
            .isEqualTo( new SegmentType[] { RUN_MODE, TEST_RUN_ID, STRING_ENCODING, DATA_STRING, END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( ForkedProcessEventType.BOOTERCODE_STDOUT_NEW_LINE );
        assertThat( segmentTypes )
            .hasSize( 5 )
            .isEqualTo( new SegmentType[] { RUN_MODE, TEST_RUN_ID, STRING_ENCODING, DATA_STRING, END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( ForkedProcessEventType.BOOTERCODE_STDERR );
        assertThat( segmentTypes )
            .hasSize( 5 )
            .isEqualTo( new SegmentType[] { RUN_MODE, TEST_RUN_ID, STRING_ENCODING, DATA_STRING, END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( ForkedProcessEventType.BOOTERCODE_STDERR_NEW_LINE );
        assertThat( segmentTypes )
            .hasSize( 5 )
            .isEqualTo( new SegmentType[] { RUN_MODE, TEST_RUN_ID, STRING_ENCODING, DATA_STRING, END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( BOOTERCODE_SYSPROPS );
        assertThat( segmentTypes )
            .hasSize( 6 )
            .isEqualTo( new SegmentType[] { RUN_MODE, TEST_RUN_ID, STRING_ENCODING, DATA_STRING, DATA_STRING,
                END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( BOOTERCODE_TESTSET_STARTING );
        assertThat( segmentTypes )
            .hasSize( 14 )
            .isEqualTo( new SegmentType[] {
                RUN_MODE, TEST_RUN_ID, STRING_ENCODING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_INTEGER, DATA_STRING, DATA_STRING, DATA_STRING, END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( ForkedProcessEventType.BOOTERCODE_TESTSET_COMPLETED );
        assertThat( segmentTypes )
            .hasSize( 14 )
            .isEqualTo( new SegmentType[] { RUN_MODE, TEST_RUN_ID, STRING_ENCODING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_INTEGER, DATA_STRING, DATA_STRING, DATA_STRING,
                END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( ForkedProcessEventType.BOOTERCODE_TEST_STARTING );
        assertThat( segmentTypes )
            .hasSize( 14 )
            .isEqualTo( new SegmentType[] { RUN_MODE, TEST_RUN_ID, STRING_ENCODING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_INTEGER, DATA_STRING, DATA_STRING, DATA_STRING,
                END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( BOOTERCODE_TEST_SUCCEEDED );
        assertThat( segmentTypes )
            .hasSize( 14 )
            .isEqualTo( new SegmentType[] { RUN_MODE, TEST_RUN_ID, STRING_ENCODING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_INTEGER, DATA_STRING, DATA_STRING, DATA_STRING,
                END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( BOOTERCODE_TEST_FAILED );
        assertThat( segmentTypes )
            .hasSize( 14 )
            .isEqualTo( new SegmentType[] { RUN_MODE, TEST_RUN_ID, STRING_ENCODING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_INTEGER, DATA_STRING, DATA_STRING, DATA_STRING,
                END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( ForkedProcessEventType.BOOTERCODE_TEST_SKIPPED );
        assertThat( segmentTypes )
            .hasSize( 14 )
            .isEqualTo( new SegmentType[] { RUN_MODE, TEST_RUN_ID, STRING_ENCODING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_INTEGER, DATA_STRING, DATA_STRING, DATA_STRING,
                END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( ForkedProcessEventType.BOOTERCODE_TEST_ERROR );
        assertThat( segmentTypes )
            .hasSize( 14 )
            .isEqualTo( new SegmentType[] { RUN_MODE, TEST_RUN_ID, STRING_ENCODING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_INTEGER, DATA_STRING, DATA_STRING, DATA_STRING,
                END_OF_FRAME } );

        segmentTypes = decoder.nextSegmentType( ForkedProcessEventType.BOOTERCODE_TEST_ASSUMPTIONFAILURE );
        assertThat( segmentTypes )
            .hasSize( 14 )
            .isEqualTo( new SegmentType[] { RUN_MODE, TEST_RUN_ID, STRING_ENCODING, DATA_STRING, DATA_STRING,
                DATA_STRING, DATA_STRING, DATA_STRING, DATA_STRING, DATA_INTEGER, DATA_STRING, DATA_STRING, DATA_STRING,
                END_OF_FRAME } );
    }

    @Test
    public void shouldCreateEvent() throws Exception
    {
        byte[] stream = {};
        Channel channel = new Channel( stream, 1 );
        EventDecoder decoder = new EventDecoder( channel, new MockForkNodeArguments() );

        Event event = decoder.toMessage( BOOTERCODE_BYE, decoder.new Memento() );
        assertThat( event )
            .isInstanceOf( ControlByeEvent.class );

        event = decoder.toMessage( BOOTERCODE_STOP_ON_NEXT_TEST, decoder.new Memento() );
        assertThat( event )
            .isInstanceOf( ControlStopOnNextTestEvent.class );

        event = decoder.toMessage( BOOTERCODE_NEXT_TEST, decoder.new Memento() );
        assertThat( event )
            .isInstanceOf( ControlNextTestEvent.class );

        Memento memento = decoder.new Memento();
        memento.getData().addAll( asList( "1", "2", "3" ) );
        event = decoder.toMessage( BOOTERCODE_CONSOLE_ERROR, memento );
        assertThat( event )
            .isInstanceOf( ConsoleErrorEvent.class );
        ConsoleErrorEvent consoleErrorEvent = (ConsoleErrorEvent) event;
        assertThat( consoleErrorEvent.getStackTraceWriter().getThrowable().getLocalizedMessage() )
            .isEqualTo( "1" );
        assertThat( consoleErrorEvent.getStackTraceWriter().smartTrimmedStackTrace() )
            .isEqualTo( "2" );
        assertThat( consoleErrorEvent.getStackTraceWriter().writeTraceToString() )
            .isEqualTo( "3" );

        memento = decoder.new Memento();
        memento.getData().addAll( asList( null, null, null ) );
        event = decoder.toMessage( BOOTERCODE_CONSOLE_ERROR, memento );
        assertThat( event )
            .isInstanceOf( ConsoleErrorEvent.class );
        consoleErrorEvent = (ConsoleErrorEvent) event;
        assertThat( consoleErrorEvent.getStackTraceWriter() )
            .isNull();

        memento = decoder.new Memento();
        memento.getData().addAll( asList( "1", "2", "3" ) );
        event = decoder.toMessage( BOOTERCODE_JVM_EXIT_ERROR, memento );
        assertThat( event )
            .isInstanceOf( JvmExitErrorEvent.class );
        JvmExitErrorEvent jvmExitErrorEvent = (JvmExitErrorEvent) event;
        assertThat( jvmExitErrorEvent.getStackTraceWriter().getThrowable().getLocalizedMessage() )
            .isEqualTo( "1" );
        assertThat( jvmExitErrorEvent.getStackTraceWriter().smartTrimmedStackTrace() )
            .isEqualTo( "2" );
        assertThat( jvmExitErrorEvent.getStackTraceWriter().writeTraceToString() )
            .isEqualTo( "3" );

        memento = decoder.new Memento();
        memento.getData().addAll( asList( null, null, null ) );
        event = decoder.toMessage( BOOTERCODE_JVM_EXIT_ERROR, memento );
        assertThat( event )
            .isInstanceOf( JvmExitErrorEvent.class );
        jvmExitErrorEvent = (JvmExitErrorEvent) event;
        assertThat( jvmExitErrorEvent.getStackTraceWriter() )
            .isNull();

        memento = decoder.new Memento();
        memento.getData().addAll( singletonList( "m" ) );
        event = decoder.toMessage( BOOTERCODE_CONSOLE_INFO, memento );
        assertThat( event ).isInstanceOf( ConsoleInfoEvent.class );
        assertThat( ( (ConsoleInfoEvent) event ).getMessage() ).isEqualTo( "m" );

        memento = decoder.new Memento();
        memento.getData().addAll( singletonList( "" ) );
        event = decoder.toMessage( BOOTERCODE_CONSOLE_WARNING, memento );
        assertThat( event ).isInstanceOf( ConsoleWarningEvent.class );
        assertThat( ( (ConsoleWarningEvent) event ).getMessage() ).isEmpty();

        memento = decoder.new Memento();
        memento.getData().addAll( singletonList( null ) );
        event = decoder.toMessage( BOOTERCODE_CONSOLE_DEBUG, memento );
        assertThat( event ).isInstanceOf( ConsoleDebugEvent.class );
        assertThat( ( (ConsoleDebugEvent) event ).getMessage() ).isNull();

        memento = decoder.new Memento();
        memento.getData().addAll( asList( NORMAL_RUN, 1L, "m" ) );
        event = decoder.toMessage( BOOTERCODE_STDOUT, memento );
        assertThat( event ).isInstanceOf( StandardStreamOutEvent.class );
        assertThat( ( (StandardStreamOutEvent) event ).getMessage() ).isEqualTo( "m" );
        assertThat( ( (StandardStreamOutEvent) event ).getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (StandardStreamOutEvent) event ).getTestRunId() ).isEqualTo( 1L );

        memento = decoder.new Memento();
        memento.getData().addAll( asList( RERUN_TEST_AFTER_FAILURE, 1L, null ) );
        event = decoder.toMessage( BOOTERCODE_STDOUT_NEW_LINE, memento );
        assertThat( event ).isInstanceOf( StandardStreamOutWithNewLineEvent.class );
        assertThat( ( (StandardStreamOutWithNewLineEvent) event ).getMessage() ).isNull();
        assertThat( ( (StandardStreamOutWithNewLineEvent) event ).getRunMode() ).isEqualTo( RERUN_TEST_AFTER_FAILURE );
        assertThat( ( (StandardStreamOutWithNewLineEvent) event ).getTestRunId() ).isEqualTo( 1L );

        memento = decoder.new Memento();
        memento.getData().addAll( asList( RERUN_TEST_AFTER_FAILURE, 1L, null ) );
        event = decoder.toMessage( BOOTERCODE_STDERR, memento );
        assertThat( event ).isInstanceOf( StandardStreamErrEvent.class );
        assertThat( ( (StandardStreamErrEvent) event ).getMessage() ).isNull();
        assertThat( ( (StandardStreamErrEvent) event ).getRunMode() ).isEqualTo( RERUN_TEST_AFTER_FAILURE );
        assertThat( ( (StandardStreamErrEvent) event ).getTestRunId() ).isEqualTo( 1L );

        memento = decoder.new Memento();
        memento.getData().addAll( asList( NORMAL_RUN, 1L, "abc" ) );
        event = decoder.toMessage( BOOTERCODE_STDERR_NEW_LINE, memento );
        assertThat( event ).isInstanceOf( StandardStreamErrWithNewLineEvent.class );
        assertThat( ( (StandardStreamErrWithNewLineEvent) event ).getMessage() ).isEqualTo( "abc" );
        assertThat( ( (StandardStreamErrWithNewLineEvent) event ).getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (StandardStreamErrWithNewLineEvent) event ).getTestRunId() ).isEqualTo( 1L );

        memento = decoder.new Memento();
        memento.getData().addAll( asList( NORMAL_RUN, 1L, "key", "value" ) );
        event = decoder.toMessage( BOOTERCODE_SYSPROPS, memento );
        assertThat( event ).isInstanceOf( SystemPropertyEvent.class );
        assertThat( ( (SystemPropertyEvent) event ).getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (SystemPropertyEvent) event ).getTestRunId() ).isEqualTo( 1L );
        assertThat( ( (SystemPropertyEvent) event ).getKey() ).isEqualTo( "key" );
        assertThat( ( (SystemPropertyEvent) event ).getValue() ).isEqualTo( "value" );

        memento = decoder.new Memento();
        memento.getData().addAll( asList( NORMAL_RUN , 1L, "source", "sourceText", "name", "nameText", "group",
            "message", 5, "traceMessage", "smartTrimmedStackTrace", "stackTrace" ) );
        event = decoder.toMessage( BOOTERCODE_TESTSET_STARTING, memento );
        assertThat( event ).isInstanceOf( TestsetStartingEvent.class );
        assertThat( ( (TestsetStartingEvent) event ).getReportEntry().getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (TestsetStartingEvent) event ).getReportEntry().getTestRunId() ).isEqualTo( 1L );
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

        memento = decoder.new Memento();
        memento.getData().addAll( asList( NORMAL_RUN, 1L, "source", "sourceText", "name", "nameText", "group", null, 5,
            "traceMessage", "smartTrimmedStackTrace", "stackTrace" ) );
        event = decoder.toMessage( BOOTERCODE_TESTSET_COMPLETED, memento );
        assertThat( event ).isInstanceOf( TestsetCompletedEvent.class );
        assertThat( ( (TestsetCompletedEvent) event ).getReportEntry().getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (TestsetCompletedEvent) event ).getReportEntry().getTestRunId() ).isEqualTo( 1L );
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

        memento = decoder.new Memento();
        memento.getData().addAll( asList( NORMAL_RUN, 1L, "source", "sourceText", "name", "nameText", "group",
            "message", 5, null, "smartTrimmedStackTrace", "stackTrace" ) );
        event = decoder.toMessage( BOOTERCODE_TEST_STARTING, memento );
        assertThat( event ).isInstanceOf( TestStartingEvent.class );
        assertThat( ( (TestStartingEvent) event ).getReportEntry().getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (TestStartingEvent) event ).getReportEntry().getTestRunId() ).isEqualTo( 1L );
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

        memento = decoder.new Memento();
        memento.getData().addAll( asList( NORMAL_RUN, 1L, "source", "sourceText", "name", "nameText", "group",
            "message", 5, null, null, null ) );
        event = decoder.toMessage( BOOTERCODE_TEST_SUCCEEDED, memento );
        assertThat( event ).isInstanceOf( TestSucceededEvent.class );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getTestRunId() ).isEqualTo( 1L );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getSourceName() ).isEqualTo( "source" );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getSourceText() ).isEqualTo( "sourceText" );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getName() ).isEqualTo( "name" );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getNameText() ).isEqualTo( "nameText" );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getGroup() ).isEqualTo( "group" );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getMessage() ).isEqualTo( "message" );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getElapsed() ).isEqualTo( 5 );
        assertThat( ( (TestSucceededEvent) event ).getReportEntry().getStackTraceWriter() ).isNull();

        memento = decoder.new Memento();
        memento.getData().addAll( asList( RERUN_TEST_AFTER_FAILURE, 1L, "source", null, "name", null, "group", null, 5,
            "traceMessage", "smartTrimmedStackTrace", "stackTrace" ) );
        event = decoder.toMessage( BOOTERCODE_TEST_FAILED, memento );
        assertThat( event ).isInstanceOf( TestFailedEvent.class );
        assertThat( ( (TestFailedEvent) event ).getReportEntry().getRunMode() ).isEqualTo( RERUN_TEST_AFTER_FAILURE );
        assertThat( ( (TestFailedEvent) event ).getReportEntry().getTestRunId() ).isEqualTo( 1L );
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

        memento = decoder.new Memento();
        memento.getData().addAll( asList( NORMAL_RUN, 1L, "source", null, "name", null, null, null, 5, null, null,
            "stackTrace" ) );
        event = decoder.toMessage( BOOTERCODE_TEST_SKIPPED, memento );
        assertThat( event ).isInstanceOf( TestSkippedEvent.class );
        assertThat( ( (TestSkippedEvent) event ).getReportEntry().getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (TestSkippedEvent) event ).getReportEntry().getTestRunId() ).isEqualTo( 1L );
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

        memento = decoder.new Memento();
        memento.getData().addAll( asList( NORMAL_RUN, 1L, "source", null, "name", "nameText", null, null, 0, null, null,
            "stackTrace" ) );
        event = decoder.toMessage( BOOTERCODE_TEST_ERROR, memento );
        assertThat( event ).isInstanceOf( TestErrorEvent.class );
        assertThat( ( (TestErrorEvent) event ).getReportEntry().getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (TestErrorEvent) event ).getReportEntry().getTestRunId() ).isEqualTo( 1L );
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

        memento = decoder.new Memento();
        memento.getData().addAll( asList( NORMAL_RUN, 1L, "source", null, "name", null, "group", null, 5, null, null,
            "stackTrace" ) );
        event = decoder.toMessage( BOOTERCODE_TEST_ASSUMPTIONFAILURE, memento );
        assertThat( event ).isInstanceOf( TestAssumptionFailureEvent.class );
        assertThat( ( (TestAssumptionFailureEvent) event ).getReportEntry().getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( ( (TestAssumptionFailureEvent) event ).getReportEntry().getTestRunId() ).isEqualTo( 1L );
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

    @Test
    public void shouldRecognizeEmptyStream4ReportEntry()
    {
        ReportEntry reportEntry = newReportEntry( NORMAL_RUN, 1L, "", "", "", "", "", "", null, "", "", "" );
        assertThat( reportEntry ).isNotNull();
        assertThat( reportEntry.getRunMode() ).isEqualTo( NORMAL_RUN );
        assertThat( reportEntry.getTestRunId() ).isEqualTo( 1L );
        assertThat( reportEntry.getStackTraceWriter() ).isNotNull();
        assertThat( reportEntry.getStackTraceWriter().smartTrimmedStackTrace() ).isEmpty();
        assertThat( reportEntry.getStackTraceWriter().writeTraceToString() ).isEmpty();
        assertThat( reportEntry.getStackTraceWriter().writeTrimmedTraceToString() ).isEmpty();
        assertThat( reportEntry.getSourceName() ).isEmpty();
        assertThat( reportEntry.getSourceText() ).isEmpty();
        assertThat( reportEntry.getName() ).isEmpty();
        assertThat( reportEntry.getNameText() ).isEmpty();
        assertThat( reportEntry.getGroup() ).isEmpty();
        assertThat( reportEntry.getNameWithGroup() ).isEmpty();
        assertThat( reportEntry.getMessage() ).isEmpty();
        assertThat( reportEntry.getElapsed() ).isNull();
    }

    @Test
    @SuppressWarnings( "checkstyle:magicnumber" )
    public void testCreatingReportEntry()
    {
        final String exceptionMessage = "msg";
        final String smartStackTrace = "MyTest:86 >> Error";
        final String stackTrace = "Exception: msg\ntrace line 1\ntrace line 2";
        final String trimmedStackTrace = "trace line 1\ntrace line 2";

        SafeThrowable safeThrowable = new SafeThrowable( exceptionMessage );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( 102 );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameText() ).thenReturn( "my display name" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getSourceText() ).thenReturn( "test class display name" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        ReportEntry decodedReportEntry = newReportEntry( NORMAL_RUN, 1L, reportEntry.getSourceName(),
            reportEntry.getSourceText(), reportEntry.getName(), reportEntry.getNameText(), reportEntry.getGroup(),
            reportEntry.getMessage(), null, null, null, null );

        assertThat( decodedReportEntry ).isNotNull();
        assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
        assertThat( decodedReportEntry.getSourceText() ).isEqualTo( reportEntry.getSourceText() );
        assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
        assertThat( decodedReportEntry.getNameText() ).isEqualTo( reportEntry.getNameText() );
        assertThat( decodedReportEntry.getGroup() ).isEqualTo( reportEntry.getGroup() );
        assertThat( decodedReportEntry.getMessage() ).isEqualTo( reportEntry.getMessage() );
        assertThat( decodedReportEntry.getStackTraceWriter() ).isNull();

        decodedReportEntry = newReportEntry( NORMAL_RUN, 1L, reportEntry.getSourceName(),
            reportEntry.getSourceText(), reportEntry.getName(), reportEntry.getNameText(), reportEntry.getGroup(),
            reportEntry.getMessage(), null, exceptionMessage, smartStackTrace, null );

        assertThat( decodedReportEntry ).isNotNull();
        assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
        assertThat( decodedReportEntry.getSourceText() ).isEqualTo( reportEntry.getSourceText() );
        assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
        assertThat( decodedReportEntry.getNameText() ).isEqualTo( reportEntry.getNameText() );
        assertThat( decodedReportEntry.getGroup() ).isEqualTo( reportEntry.getGroup() );
        assertThat( decodedReportEntry.getMessage() ).isEqualTo( reportEntry.getMessage() );
        assertThat( decodedReportEntry.getElapsed() ).isNull();
        assertThat( decodedReportEntry.getStackTraceWriter() ).isNotNull();
        assertThat( decodedReportEntry.getStackTraceWriter().getThrowable().getMessage() )
            .isEqualTo( exceptionMessage );
        assertThat( decodedReportEntry.getStackTraceWriter().smartTrimmedStackTrace() )
            .isEqualTo( smartStackTrace );
        assertThat( decodedReportEntry.getStackTraceWriter().writeTraceToString() )
            .isNull();

        decodedReportEntry = newReportEntry( NORMAL_RUN, 1L, reportEntry.getSourceName(),
            reportEntry.getSourceText(), reportEntry.getName(), reportEntry.getNameText(), reportEntry.getGroup(),
            reportEntry.getMessage(), 1003, exceptionMessage, smartStackTrace, null );

        assertThat( decodedReportEntry ).isNotNull();
        assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
        assertThat( decodedReportEntry.getSourceText() ).isEqualTo( reportEntry.getSourceText() );
        assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
        assertThat( decodedReportEntry.getNameText() ).isEqualTo( reportEntry.getNameText() );
        assertThat( decodedReportEntry.getGroup() ).isEqualTo( reportEntry.getGroup() );
        assertThat( decodedReportEntry.getMessage() ).isEqualTo( reportEntry.getMessage() );
        assertThat( decodedReportEntry.getElapsed() ).isEqualTo( 1003 );
        assertThat( decodedReportEntry.getStackTraceWriter() ).isNotNull();
        assertThat( decodedReportEntry.getStackTraceWriter().getThrowable().getMessage() )
            .isEqualTo( exceptionMessage );
        assertThat( decodedReportEntry.getStackTraceWriter().smartTrimmedStackTrace() )
            .isEqualTo( smartStackTrace );
        assertThat( decodedReportEntry.getStackTraceWriter().writeTraceToString() )
            .isNull();

        decodedReportEntry = newReportEntry( NORMAL_RUN, 1L, reportEntry.getSourceName(),
            reportEntry.getSourceText(), reportEntry.getName(), reportEntry.getNameText(), reportEntry.getGroup(),
            reportEntry.getMessage(), 1003, exceptionMessage, smartStackTrace, stackTrace );

        assertThat( decodedReportEntry ).isNotNull();
        assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
        assertThat( decodedReportEntry.getSourceText() ).isEqualTo( reportEntry.getSourceText() );
        assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
        assertThat( decodedReportEntry.getNameText() ).isEqualTo( reportEntry.getNameText() );
        assertThat( decodedReportEntry.getGroup() ).isEqualTo( reportEntry.getGroup() );
        assertThat( decodedReportEntry.getMessage() ).isEqualTo( reportEntry.getMessage() );
        assertThat( decodedReportEntry.getElapsed() ).isEqualTo( 1003 );
        assertThat( decodedReportEntry.getStackTraceWriter() ).isNotNull();
        assertThat( decodedReportEntry.getStackTraceWriter().getThrowable().getMessage() ).isNotNull();
        assertThat( decodedReportEntry.getStackTraceWriter().getThrowable().getMessage() )
            .isEqualTo( exceptionMessage );
        assertThat( decodedReportEntry.getStackTraceWriter().smartTrimmedStackTrace() )
            .isEqualTo( smartStackTrace );
        assertThat( decodedReportEntry.getStackTraceWriter().writeTraceToString() ).isEqualTo( stackTrace );
        assertThat( decodedReportEntry.getStackTraceWriter().writeTrimmedTraceToString() ).isEqualTo( stackTrace );

        decodedReportEntry = newReportEntry( NORMAL_RUN, 1L, reportEntry.getSourceName(),
            reportEntry.getSourceText(), reportEntry.getName(), reportEntry.getNameText(), reportEntry.getGroup(),
            reportEntry.getMessage(), 1003, exceptionMessage, smartStackTrace, trimmedStackTrace );

        assertThat( decodedReportEntry ).isNotNull();
        assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
        assertThat( decodedReportEntry.getSourceText() ).isEqualTo( reportEntry.getSourceText() );
        assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
        assertThat( decodedReportEntry.getNameText() ).isEqualTo( reportEntry.getNameText() );
        assertThat( decodedReportEntry.getGroup() ).isEqualTo( reportEntry.getGroup() );
        assertThat( decodedReportEntry.getMessage() ).isEqualTo( reportEntry.getMessage() );
        assertThat( decodedReportEntry.getElapsed() ).isEqualTo( 1003 );
        assertThat( decodedReportEntry.getStackTraceWriter() ).isNotNull();
        assertThat( decodedReportEntry.getStackTraceWriter().getThrowable().getMessage() ).isNotNull();
        assertThat( decodedReportEntry.getStackTraceWriter().getThrowable().getMessage() )
            .isEqualTo( exceptionMessage );
        assertThat( decodedReportEntry.getStackTraceWriter().smartTrimmedStackTrace() )
            .isEqualTo( smartStackTrace );
        assertThat( decodedReportEntry.getStackTraceWriter().writeTraceToString() ).isEqualTo( trimmedStackTrace );
        assertThat( decodedReportEntry.getStackTraceWriter().writeTrimmedTraceToString() )
            .isEqualTo( trimmedStackTrace );
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

}
