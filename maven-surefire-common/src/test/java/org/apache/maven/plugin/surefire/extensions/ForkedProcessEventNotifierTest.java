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
import org.apache.maven.plugin.surefire.booterclient.output.ForkedProcessEventListener;
import org.apache.maven.plugin.surefire.booterclient.output.ForkedProcessEventNotifier;
import org.apache.maven.plugin.surefire.booterclient.output.ForkedProcessExitErrorListener;
import org.apache.maven.plugin.surefire.booterclient.output.ForkedProcessPropertyEventListener;
import org.apache.maven.plugin.surefire.booterclient.output.ForkedProcessReportEventListener;
import org.apache.maven.plugin.surefire.booterclient.output.ForkedProcessStackTraceEventListener;
import org.apache.maven.plugin.surefire.booterclient.output.ForkedProcessStandardOutErrEventListener;
import org.apache.maven.plugin.surefire.booterclient.output.ForkedProcessStringEventListener;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.ConsoleLoggerUtils;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.report.SafeThrowable;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.api.util.internal.WritableBufferedByteChannel;
import org.apache.maven.surefire.booter.spi.EventChannelEncoder;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.report.TestOutputReportEntry.stdErr;
import static org.apache.maven.surefire.api.report.TestOutputReportEntry.stdOut;
import static org.apache.maven.surefire.api.report.TestOutputReportEntry.stdOutln;
import static org.apache.maven.surefire.api.util.internal.Channels.newBufferedChannel;
import static org.apache.maven.surefire.api.util.internal.Channels.newChannel;
import static org.apache.maven.surefire.api.util.internal.ObjectUtils.systemProps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.rules.ExpectedException.none;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Test for {@link ForkedProcessEventNotifier}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
@RunWith( Enclosed.class )
public class ForkedProcessEventNotifierTest
{
    /**
     *
     */
    @RunWith( PowerMockRunner.class )
    @PowerMockIgnore( { "org.jacoco.agent.rt.*", "com.vladium.emma.rt.*" } )
    public static class DecoderOperationsTest
    {
        @Rule
        public final ExpectedException rule = none();

        @Test
        public void shouldSendByeEvent() throws Exception
        {
            Stream out = Stream.newStream();
            EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );
            encoder.bye();
            String read = new String( out.toByteArray(), UTF_8 );

            assertThat( read )
                    .isEqualTo( ":maven-surefire-event:\u0003:bye:" );

            LineNumberReader lines = out.newReader( UTF_8 );

            final String cmd = lines.readLine();
            assertThat( cmd )
                    .isNotNull();

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            EventAssertionListener listener = new EventAssertionListener();
            notifier.setByeListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void shouldSendStopOnNextTestEvent() throws Exception
        {
            Stream out = Stream.newStream();
            EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );
            encoder.stopOnNextTest();
            String read = new String( out.toByteArray(), UTF_8 );

            assertThat( read )
                    .isEqualTo( ":maven-surefire-event:\u0011:stop-on-next-test:" );

            LineNumberReader lines = out.newReader( UTF_8 );

            final String cmd = lines.readLine();
            assertThat( cmd )
                .isNotNull();

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            EventAssertionListener listener = new EventAssertionListener();
            notifier.setStopOnNextTestListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.dumpStreamText )
                .isEmpty();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void shouldCorrectlyDecodeStackTracesWithEmptyStringTraceMessages() throws Exception
        {
            String exceptionMessage = "";
            String smartStackTrace = "JUnit5Test.failWithEmptyString:16";
            String exceptionStackTrace = "org.opentest4j.AssertionFailedError: \n"
                    + "\tat JUnit5Test.failWithEmptyString(JUnit5Test.java:16)\n";

            StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
            SafeThrowable safeThrowable = new SafeThrowable( exceptionMessage );
            when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
            when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
            when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( exceptionStackTrace );
            when( stackTraceWriter.writeTraceToString() ).thenReturn( exceptionStackTrace );

            ReportEntry reportEntry = mock( ReportEntry.class );
            when( reportEntry.getRunMode() ).thenReturn( NORMAL_RUN );
            when( reportEntry.getTestRunId() ).thenReturn( 1L );
            when( reportEntry.getElapsed() ).thenReturn( 7 );
            when( reportEntry.getGroup() ).thenReturn( null );
            when( reportEntry.getMessage() ).thenReturn( null );
            when( reportEntry.getName() ).thenReturn( "failWithEmptyString" );
            when( reportEntry.getNameWithGroup() ).thenReturn( "JUnit5Test" );
            when( reportEntry.getSourceName() ).thenReturn( "JUnit5Test" );
            when( reportEntry.getSourceText() ).thenReturn( null );
            when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

            final Stream out = Stream.newStream();
            EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );
            encoder.testFailed( reportEntry, true );

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            final ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            ReportEventAssertionListener listener = new ReportEventAssertionListener( reportEntry, true );
            notifier.setTestFailedListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void shouldSendNextTestEvent() throws Exception
        {
            final Stream out = Stream.newStream();
            EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );
            encoder.acquireNextTest();
            String read = new String( out.toByteArray(), UTF_8 );

            assertThat( read )
                    .isEqualTo( ":maven-surefire-event:\u0009:next-test:" );

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            EventAssertionListener listener = new EventAssertionListener();
            notifier.setAcquireNextTestListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void testConsole() throws Exception
        {
            final Stream out = Stream.newStream();
            EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );
            encoder.consoleInfoLog( "msg" );

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            StringEventAssertionListener listener = new StringEventAssertionListener( "msg" );
            notifier.setConsoleInfoListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void testError() throws Exception
        {
            final Stream out = Stream.newStream();
            EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );
            encoder.consoleErrorLog( "msg" );

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            StackTraceEventListener listener = new StackTraceEventListener( "msg", null, null );
            notifier.setConsoleErrorListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void testErrorWithException() throws Exception
        {
            final Stream out = Stream.newStream();
            EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );
            Throwable throwable = new Throwable( "msg" );
            encoder.consoleErrorLog( throwable );

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            String stackTrace = ConsoleLoggerUtils.toString( throwable );
            StackTraceEventListener listener = new StackTraceEventListener( "msg", null, stackTrace );
            notifier.setConsoleErrorListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 1 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void testErrorWithStackTraceWriter() throws Exception
        {
            final Stream out = Stream.newStream();

            EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );
            StackTraceWriter stackTraceWriter = new DeserializedStacktraceWriter( "1", "2", "3" );
            encoder.consoleErrorLog( stackTraceWriter, false );

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            StackTraceEventListener listener = new StackTraceEventListener( "1", "2", "3" );
            notifier.setConsoleErrorListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void testDebug() throws Exception
        {
            final Stream out = Stream.newStream();

            EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );
            encoder.consoleDebugLog( "msg" );

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            StringEventAssertionListener listener = new StringEventAssertionListener( "msg" );
            notifier.setConsoleDebugListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();

            assertThat( arguments.isCalled() )
                .isFalse();

            assertThat( listener.called.get() )
                .isTrue();

            assertThat( listener.msg )
                .isEqualTo( "msg" );
        }

        @Test
        public void testWarning() throws Exception
        {
            final Stream out = Stream.newStream();

            EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );
            encoder.consoleWarningLog( "msg" );

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            StringEventAssertionListener listener = new StringEventAssertionListener( "msg" );
            notifier.setConsoleWarningListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void testStdOutStream() throws Exception
        {
            final Stream out = Stream.newStream();
            WritableBufferedByteChannel wChannel = newBufferedChannel( out );
            EventChannelEncoder encoder = new EventChannelEncoder( wChannel );
            encoder.testOutput( new TestOutputReportEntry( stdOut( "msg" ), NORMAL_RUN, 1L ) );
            wChannel.close();

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            StandardOutErrEventAssertionListener listener =
                new StandardOutErrEventAssertionListener( NORMAL_RUN, 1L, "msg", false );
            notifier.setStdOutListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void testStdOutStreamPrint() throws Exception
        {
            final Stream out = Stream.newStream();
            WritableBufferedByteChannel wChannel = newBufferedChannel( out );
            EventChannelEncoder encoder = new EventChannelEncoder( wChannel );
            encoder.testOutput( new TestOutputReportEntry( stdOut( "" ), NORMAL_RUN, 1L ) );
            wChannel.close();

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            StandardOutErrEventAssertionListener listener =
                new StandardOutErrEventAssertionListener( NORMAL_RUN, 1L, "", false );
            notifier.setStdOutListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void testStdOutStreamPrintWithNull() throws Exception
        {
            final Stream out = Stream.newStream();
            WritableBufferedByteChannel wChannel = newBufferedChannel( out );
            EventChannelEncoder encoder = new EventChannelEncoder( wChannel );
            encoder.testOutput( new TestOutputReportEntry( stdOut( null ), NORMAL_RUN, 1L ) );
            wChannel.close();

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            StandardOutErrEventAssertionListener listener =
                new StandardOutErrEventAssertionListener( NORMAL_RUN, 1L, null, false );
            notifier.setStdOutListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void testStdOutStreamPrintln() throws Exception
        {
            final Stream out = Stream.newStream();
            WritableBufferedByteChannel wChannel = newBufferedChannel( out );
            EventChannelEncoder encoder = new EventChannelEncoder( wChannel );
            encoder.testOutput( new TestOutputReportEntry( stdOutln( "" ), NORMAL_RUN, 1L ) );
            wChannel.close();

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            StandardOutErrEventAssertionListener listener =
                new StandardOutErrEventAssertionListener( NORMAL_RUN, 1L, "", true );
            notifier.setStdOutListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void testStdOutStreamPrintlnWithNull() throws Exception
        {
            final Stream out = Stream.newStream();
            WritableBufferedByteChannel wChannel = newBufferedChannel( out );
            EventChannelEncoder encoder = new EventChannelEncoder( wChannel );
            encoder.testOutput( new TestOutputReportEntry( stdOutln( null ), NORMAL_RUN, 1L ) );
            wChannel.close();

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            StandardOutErrEventAssertionListener listener =
                new StandardOutErrEventAssertionListener( NORMAL_RUN, 1L, null, true );
            notifier.setStdOutListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void testStdErrStream() throws Exception
        {
            final Stream out = Stream.newStream();
            WritableBufferedByteChannel wChannel = newBufferedChannel( out );
            EventChannelEncoder encoder = new EventChannelEncoder( wChannel );
            encoder.testOutput( new TestOutputReportEntry( stdErr( "msg" ), NORMAL_RUN, 1L ) );
            wChannel.close();

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            StandardOutErrEventAssertionListener listener =
                new StandardOutErrEventAssertionListener( NORMAL_RUN, 1L, "msg", false );
            notifier.setStdErrListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }

        @Test
        public void shouldHandleErrorAfterNullLine()
        {
            ForkedProcessEventNotifier decoder = new ForkedProcessEventNotifier();
            decoder.setSystemPropertiesListener( new PropertyEventAssertionListener() );
            rule.expect( NullPointerException.class );
            decoder.notifyEvent( null );
        }

        @Test
        public void shouldHandleErrorAfterUnknownOperation() throws Exception
        {
            String cmd = ":maven-surefire-event:\u000c:abnormal-run:-:\n";

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( cmd.getBytes() ) );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 1 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( true, true, true, true );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                countdown.awaitClosed();
            }

            assertThat( arguments.isCalled() )
                .isTrue();

            assertThat( logger.isCalled() )
                .isTrue();

            assertThat( logger.debug )
                .hasSize( 1 );

            assertThat( logger.debug.peek() )
                .contains( ":maven-surefire-event:\u000c:abnormal-run:-:" );

            String dump = "Corrupted channel by directly writing to native stream in forked JVM 0.";
            assertThat( arguments.dumpStreamText )
                .hasSize( 1 )
                .contains( format( dump + " Stream '%s'.", ":maven-surefire-event:\u000c:abnormal-run:-:" ) );

            dump += " See FAQ web page and the dump file ";
            assertThat( arguments.logWarningAtEnd )
                .hasSize( 1 );
            assertThat( arguments.logWarningAtEnd.peek() )
                .startsWith( dump );
        }

        @Test
        public void shouldHandleExit() throws Exception
        {
            final Stream out = Stream.newStream();
            EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

            StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
            when( stackTraceWriter.getThrowable() ).thenReturn( new SafeThrowable( "1" ) );
            when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( "2" );
            when( stackTraceWriter.writeTraceToString() ).thenReturn( "3" );
            when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( "4" );
            encoder.sendExitError( stackTraceWriter, false );

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();
            ProcessExitErrorListener listener = new ProcessExitErrorListener();
            notifier.setExitErrorEventListener( listener );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();
            assertThat( arguments.isCalled() )
                .isFalse();
            assertThat( listener.called.get() )
                .isTrue();
        }
    }

    /**
     *
     */
    @RunWith( Theories.class )
    public static class ReportEntryTest
    {
        @DataPoints( value = "operation" )
        @SuppressWarnings( "checkstyle:visibilitymodifier" )
        public static String[][] operations = { { "testSetStarting", "setTestSetStartingListener" },
                                                { "testSetCompleted", "setTestSetCompletedListener" },
                                                { "testStarting", "setTestStartingListener" },
                                                { "testSucceeded", "setTestSucceededListener" },
                                                { "testFailed", "setTestFailedListener" },
                                                { "testSkipped", "setTestSkippedListener" },
                                                { "testError", "setTestErrorListener" },
                                                { "testAssumptionFailure", "setTestAssumptionFailureListener" }
        };

        @DataPoints( value = "reportedMessage" )
        @SuppressWarnings( "checkstyle:visibilitymodifier" )
        public static String[] reportedMessage = { null, "skipped test" };

        @DataPoints( value = "elapsed" )
        @SuppressWarnings( { "checkstyle:visibilitymodifier", "checkstyle:magicnumber" } )
        public static Integer[] elapsed = { null, 102 };

        @DataPoints( value = "trim" )
        @SuppressWarnings( "checkstyle:visibilitymodifier" )
        public static boolean[] trim = { false, true };

        @DataPoints( value = "msg" )
        @SuppressWarnings( "checkstyle:visibilitymodifier" )
        public static boolean[] msg = { false, true };

        @DataPoints( value = "smart" )
        @SuppressWarnings( "checkstyle:visibilitymodifier" )
        public static boolean[] smart = { false, true };

        @DataPoints( value = "trace" )
        @SuppressWarnings( "checkstyle:visibilitymodifier" )
        public static boolean[] trace = { false, true };

        @Theory
        public void testReportEntryOperations( @FromDataPoints( "operation" ) String[] operation,
                                               @FromDataPoints( "reportedMessage" ) String reportedMessage,
                                               @FromDataPoints( "elapsed" ) Integer elapsed,
                                               @FromDataPoints( "trim" ) boolean trim,
                                               @FromDataPoints( "msg" ) boolean msg,
                                               @FromDataPoints( "smart" ) boolean smart,
                                               @FromDataPoints( "trace" ) boolean trace )
                throws Exception
        {
            String exceptionMessage = msg ? "msg" : null;
            String smartStackTrace = smart ? "MyTest:86 >> Error" : null;
            String exceptionStackTrace =
                    trace ? ( trim ? "trace line 1\ntrace line 2" : "Exception: msg\ntrace line 1\ntrace line 2" )
                            : null;

            StackTraceWriter stackTraceWriter = null;
            if ( exceptionStackTrace != null )
            {
                SafeThrowable safeThrowable = new SafeThrowable( exceptionMessage );
                stackTraceWriter = mock( StackTraceWriter.class );
                when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
                when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
                when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( exceptionStackTrace );
                when( stackTraceWriter.writeTraceToString() ).thenReturn( exceptionStackTrace );
            }

            TestSetReportEntry reportEntry = mock( TestSetReportEntry.class );
            when( reportEntry.getRunMode() ).thenReturn( NORMAL_RUN );
            when( reportEntry.getTestRunId() ).thenReturn( 1L );
            when( reportEntry.getElapsed() ).thenReturn( elapsed );
            when( reportEntry.getGroup() ).thenReturn( "this group" );
            when( reportEntry.getMessage() ).thenReturn( reportedMessage );
            when( reportEntry.getName() ).thenReturn( "my test" );
            when( reportEntry.getName() ).thenReturn( "display name of test" );
            when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
            when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
            when( reportEntry.getSourceText() ).thenReturn( "test class display name" );
            when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );
            when( reportEntry.getSystemProperties() ).thenReturn( systemProps() );

            final Stream out = Stream.newStream();

            EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

            Class<?> reportType = operation[0].startsWith( "testSet" ) ? TestSetReportEntry.class : ReportEntry.class;
            EventChannelEncoder.class.getMethod( operation[0], reportType, boolean.class )
                    .invoke( encoder, reportEntry, trim );

            ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();

            ForkedProcessEventNotifier.class.getMethod( operation[1], ForkedProcessReportEventListener.class )
                    .invoke( notifier, new ReportEventAssertionListener( reportEntry, stackTraceWriter != null ) );

            ReadableByteChannel channel = newChannel( new ByteArrayInputStream( out.toByteArray() ) );

            EH eventHandler = new EH();
            CountdownCloseable countdown = new CountdownCloseable( mock( Closeable.class ), 0 );
            ConsoleLoggerMock logger = new ConsoleLoggerMock( false, false, false, false );
            ForkNodeArgumentsMock arguments = new ForkNodeArgumentsMock( logger, new File( "" ) );
            try ( EventConsumerThread t = new EventConsumerThread( "t", channel, eventHandler, countdown, arguments ) )
            {
                t.start();
                notifier.notifyEvent( eventHandler.pullEvent() );
            }

            assertThat( logger.error ).isEmpty();
            assertThat( logger.warning ).isEmpty();
            assertThat( logger.info ).isEmpty();
            assertThat( logger.debug ).isEmpty();

            assertThat( logger.isCalled() )
                .isFalse();

            assertThat( arguments.isCalled() )
                .isFalse();
        }
    }

    private static class ProcessExitErrorListener implements ForkedProcessExitErrorListener
    {
        final AtomicBoolean called = new AtomicBoolean();

        @Override
        public void handle( StackTraceWriter stackTrace )
        {
            called.set( true );
            assertThat( stackTrace.getThrowable().getMessage() ).isEqualTo( "1" );
            assertThat( stackTrace.smartTrimmedStackTrace() ).isEqualTo( "2" );
            assertThat( stackTrace.writeTraceToString() ).isEqualTo( "3" );
        }
    }

    private static class PropertyEventAssertionListener implements ForkedProcessPropertyEventListener
    {
        final AtomicBoolean called = new AtomicBoolean();
        private final Map<?, ?> sysProps = System.getProperties();
        private final AtomicInteger counter = new AtomicInteger();

        public void handle( String key, String value, RunMode runMode, Long testRunId )
        {
            called.set( true );
            counter.incrementAndGet();
            assertThat( runMode ).isEqualTo( NORMAL_RUN );
            assertThat( testRunId ).isEqualTo( 1L );
            assertTrue( sysProps.containsKey( key ) );
            assertThat( sysProps.get( key ) ).isEqualTo( value );
        }
    }

    private static class EventAssertionListener implements ForkedProcessEventListener
    {
        final AtomicBoolean called = new AtomicBoolean();

        public void handle()
        {
            called.set( true );
        }
    }

    private static class StringEventAssertionListener implements ForkedProcessStringEventListener
    {
        final AtomicBoolean called = new AtomicBoolean();
        private final String msg;

        StringEventAssertionListener( String msg )
        {
            this.msg = msg;
        }

        public void handle( String msg )
        {
            called.set( true );
            assertThat( msg )
                    .isEqualTo( this.msg );
        }
    }

    private static class StackTraceEventListener implements ForkedProcessStackTraceEventListener
    {
        final AtomicBoolean called = new AtomicBoolean();
        private final String msg;
        private final String smartStackTrace;
        private final String stackTrace;

        StackTraceEventListener( String msg, String smartStackTrace, String stackTrace )
        {
            this.msg = msg;
            this.smartStackTrace = smartStackTrace;
            this.stackTrace = stackTrace;
        }

        @Override
        public void handle( @Nonnull StackTraceWriter stackTrace )
        {
            called.set( true );

            assertThat( stackTrace.getThrowable().getMessage() )
                    .isEqualTo( msg );

            assertThat( stackTrace.smartTrimmedStackTrace() )
                    .isEqualTo( smartStackTrace );

            assertThat( stackTrace.writeTraceToString() )
                    .isEqualTo( this.stackTrace );
        }
    }

    private static class StandardOutErrEventAssertionListener implements ForkedProcessStandardOutErrEventListener
    {
        final AtomicBoolean called = new AtomicBoolean();
        private final RunMode runMode;
        private final long testRunId;
        private final String output;
        private final boolean newLine;

        StandardOutErrEventAssertionListener( RunMode runMode, long testRunId, String output, boolean newLine )
        {
            this.runMode = runMode;
            this.testRunId = testRunId;
            this.output = output;
            this.newLine = newLine;
        }

        public void handle( String output, boolean newLine, RunMode runMode, Long testRunId )
        {
            called.set( true );

            assertThat( runMode )
                    .isEqualTo( this.runMode );

            assertThat( testRunId )
                .isEqualTo( this.testRunId );

            assertThat( output )
                    .isEqualTo( this.output );

            assertThat( newLine )
                    .isEqualTo( this.newLine );
        }
    }

    private static class ReportEventAssertionListener implements ForkedProcessReportEventListener<ReportEntry>
    {
        final AtomicBoolean called = new AtomicBoolean();
        private final ReportEntry reportEntry;
        private final boolean hasStackTrace;

        ReportEventAssertionListener( ReportEntry reportEntry, boolean hasStackTrace )
        {
            this.reportEntry = reportEntry;
            this.hasStackTrace = hasStackTrace;
        }

        public void handle( ReportEntry reportEntry )
        {
            called.set( true );
            assertThat( reportEntry.getSourceName() ).isEqualTo( this.reportEntry.getSourceName() );
            assertThat( reportEntry.getSourceText() ).isEqualTo( this.reportEntry.getSourceText() );
            assertThat( reportEntry.getName() ).isEqualTo( this.reportEntry.getName() );
            assertThat( reportEntry.getNameText() ).isEqualTo( this.reportEntry.getNameText() );
            assertThat( reportEntry.getGroup() ).isEqualTo( this.reportEntry.getGroup() );
            assertThat( reportEntry.getMessage() ).isEqualTo( this.reportEntry.getMessage() );
            assertThat( reportEntry.getElapsed() ).isEqualTo( this.reportEntry.getElapsed() );
            if ( reportEntry.getStackTraceWriter() == null )
            {
                assertThat( hasStackTrace ).isFalse();
                assertThat( this.reportEntry.getStackTraceWriter() ).isNull();
            }
            else
            {
                assertThat( hasStackTrace ).isTrue();
                assertThat( this.reportEntry.getStackTraceWriter() ).isNotNull();

                assertThat( reportEntry.getStackTraceWriter().getThrowable().getMessage() )
                        .isEqualTo( this.reportEntry.getStackTraceWriter().getThrowable().getMessage() );

                assertThat( reportEntry.getStackTraceWriter().getThrowable().getLocalizedMessage() )
                        .isEqualTo( this.reportEntry.getStackTraceWriter().getThrowable().getLocalizedMessage() );

                assertThat( reportEntry.getStackTraceWriter().smartTrimmedStackTrace() )
                        .isEqualTo( this.reportEntry.getStackTraceWriter().smartTrimmedStackTrace() );
            }
        }
    }

    private static class Stream extends PrintStream
    {
        private final ByteArrayOutputStream out;

        Stream( ByteArrayOutputStream out )
        {
            super( out, true );
            this.out = out;
        }

        byte[] toByteArray()
        {
            return out.toByteArray();
        }

        LineNumberReader newReader( Charset streamCharset )
        {
            return new LineNumberReader( new StringReader( new String( toByteArray(), streamCharset ) ) );
        }

        static Stream newStream()
        {
            return new Stream( new ByteArrayOutputStream() );
        }
    }

    private static class EH implements EventHandler<Event>
    {
        private final BlockingQueue<Event> cache = new LinkedTransferQueue<>();

        Event pullEvent() throws InterruptedException
        {
            return cache.poll( 1, TimeUnit.MINUTES );
        }

        @Override
        public void handleEvent( @Nonnull Event event )
        {
            cache.add( event );
        }
    }

    /**
     * Threadsafe impl. Mockito and Powermock are not thread-safe.
     */
    private static class ForkNodeArgumentsMock implements ForkNodeArguments
    {
        private final ConcurrentLinkedQueue<String> dumpStreamText = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<String> logWarningAtEnd = new ConcurrentLinkedQueue<>();
        private final ConsoleLogger logger;
        private final File dumpStreamTextFile;

        ForkNodeArgumentsMock( ConsoleLogger logger, File dumpStreamTextFile )
        {
            this.logger = logger;
            this.dumpStreamTextFile = dumpStreamTextFile;
        }

        @Nonnull
        @Override
        public String getSessionId()
        {
            throw new UnsupportedOperationException();
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
            dumpStreamText.add( text );
            return dumpStreamTextFile;
        }

        @Nonnull
        @Override
        public File dumpStreamException( @Nonnull Throwable t )
        {
            return dumpStreamTextFile;
        }

        @Override
        public void logWarningAtEnd( @Nonnull String text )
        {
            logWarningAtEnd.add( text );
        }

        @Nonnull
        @Override
        public ConsoleLogger getConsoleLogger()
        {
            return logger;
        }

        @Nonnull
        @Override
        public Object getConsoleLock()
        {
            return logger;
        }

        boolean isCalled()
        {
            return !dumpStreamText.isEmpty() || !logWarningAtEnd.isEmpty();
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

    /**
     * Threadsafe impl. Mockito and Powermock are not thread-safe.
     */
    private static class ConsoleLoggerMock implements ConsoleLogger
    {
        final ConcurrentLinkedQueue<String> debug = new ConcurrentLinkedQueue<>();
        final ConcurrentLinkedQueue<String> info = new ConcurrentLinkedQueue<>();
        final ConcurrentLinkedQueue<String> warning = new ConcurrentLinkedQueue<>();
        final ConcurrentLinkedQueue<String> error = new ConcurrentLinkedQueue<>();
        final boolean isDebug;
        final boolean isInfo;
        final boolean isWarning;
        final boolean isError;
        private volatile boolean called;
        private volatile boolean isDebugEnabledCalled;
        private volatile boolean isInfoEnabledCalled;

        ConsoleLoggerMock( boolean isDebug, boolean isInfo, boolean isWarning, boolean isError )
        {
            this.isDebug = isDebug;
            this.isInfo = isInfo;
            this.isWarning = isWarning;
            this.isError = isError;
        }

        @Override
        public synchronized boolean isDebugEnabled()
        {
            isDebugEnabledCalled = true;
            called = true;
            return isDebug;
        }

        @Override
        public synchronized void debug( String message )
        {
            debug.add( message );
            called = true;
        }

        @Override
        public synchronized boolean isInfoEnabled()
        {
            isInfoEnabledCalled = true;
            called = true;
            return isInfo;
        }

        @Override
        public synchronized void info( String message )
        {
            info.add( message );
            called = true;
        }

        @Override
        public synchronized boolean isWarnEnabled()
        {
            called = true;
            return isWarning;
        }

        @Override
        public synchronized void warning( String message )
        {
            warning.add( message );
            called = true;
        }

        @Override
        public synchronized boolean isErrorEnabled()
        {
            called = true;
            return isError;
        }

        @Override
        public synchronized void error( String message )
        {
            error.add( message );
            called = true;
        }

        @Override
        public synchronized void error( String message, Throwable t )
        {
            called = true;
        }

        @Override
        public synchronized void error( Throwable t )
        {
            called = true;
        }

        synchronized boolean isCalled()
        {
            return called;
        }
    }
}
