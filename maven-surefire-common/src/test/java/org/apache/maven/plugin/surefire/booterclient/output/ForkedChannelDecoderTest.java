package org.apache.maven.plugin.surefire.booterclient.output;

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

import org.apache.maven.plugin.surefire.log.api.ConsoleLoggerUtils;
import org.apache.maven.surefire.booter.ForkedChannelEncoder;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunMode;
import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.util.internal.ObjectUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.maven.plugin.surefire.booterclient.output.ForkedChannelDecoder.toReportEntry;
import static org.apache.maven.surefire.report.RunMode.NORMAL_RUN;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ForkedChannelDecoder}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
@RunWith( Enclosed.class )
public class ForkedChannelDecoderTest
{
    public static class DecoderOperationsTest
    {
        @Rule
        public final ExpectedException rule = none();

        @Test
        public void shouldBeFailSafe()
        {
            assertThat( ForkedChannelDecoder.decode( null, UTF_8 ) ).isNull();
            assertThat( ForkedChannelDecoder.decode( "-", UTF_8 ) ).isNull();
            assertThat( ForkedChannelDecoder.decodeToInteger( null ) ).isNull();
            assertThat( ForkedChannelDecoder.decodeToInteger( "-" ) ).isNull();
        }

        @Test
        public void shouldHaveSystemProperty() throws IOException
        {
            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.sendSystemProperties( ObjectUtils.systemProps() );

            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setSystemPropertiesListener( new PropertyEventAssertionListener() );
            LineNumberReader reader = out.newReader( UTF_8 );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            for ( String line; ( line = reader.readLine() ) != null; )
            {
                decoder.handleEvent( line, errorHandler );
            }
            verifyZeroInteractions( errorHandler );
            assertThat( reader.getLineNumber() ).isPositive();
        }

        @Test
        public void shouldRecognizeEmptyStream4ReportEntry()
        {
            ReportEntry reportEntry = toReportEntry( null, null, null, "", "", null, null, "",
                    "", "", null );
            assertThat( reportEntry ).isNull();

            reportEntry = toReportEntry( UTF_8, "", "", "", "", "", "", "-", "", "", "" );
            assertThat( reportEntry ).isNotNull();
            assertThat( reportEntry.getStackTraceWriter() ).isNull();
            assertThat( reportEntry.getSourceName() ).isEmpty();
            assertThat( reportEntry.getSourceText() ).isEmpty();
            assertThat( reportEntry.getName() ).isEmpty();
            assertThat( reportEntry.getNameText() ).isEmpty();
            assertThat( reportEntry.getGroup() ).isEmpty();
            assertThat( reportEntry.getNameWithGroup() ).isEmpty();
            assertThat( reportEntry.getMessage() ).isEmpty();
            assertThat( reportEntry.getElapsed() ).isNull();

            rule.expect( NumberFormatException.class );
            toReportEntry( UTF_8, "", "", "", "", "", "", "", "", "", "" );
            fail();
        }

        @Test
        public void testCreatingReportEntry()
        {
            final String exceptionMessage = "msg";
            final String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

            final String smartStackTrace = "MyTest:86 >> Error";
            final String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

            final String stackTrace = "Exception: msg\ntrace line 1\ntrace line 2";
            final String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

            final String trimmedStackTrace = "trace line 1\ntrace line 2";
            final String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

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

            String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
            String encodedSourceText = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceText() ) ) );
            String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
            String encodedText = encodeBase64String( toArray( UTF_8.encode( reportEntry.getNameText() ) ) );
            String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
            String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

            ReportEntry decodedReportEntry = toReportEntry( UTF_8, encodedSourceName, encodedSourceText,
                    encodedName, encodedText, encodedGroup, encodedMessage, "-", null, null, null );

            assertThat( decodedReportEntry ).isNotNull();
            assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
            assertThat( decodedReportEntry.getSourceText() ).isEqualTo( reportEntry.getSourceText() );
            assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
            assertThat( decodedReportEntry.getNameText() ).isEqualTo(reportEntry.getNameText());
            assertThat( decodedReportEntry.getGroup() ).isEqualTo( reportEntry.getGroup() );
            assertThat( decodedReportEntry.getMessage() ).isEqualTo( reportEntry.getMessage() );
            assertThat( decodedReportEntry.getStackTraceWriter() ).isNull();

            decodedReportEntry = toReportEntry( UTF_8, encodedSourceName, encodedSourceText, encodedName, encodedText,
                    encodedGroup, encodedMessage, "-", encodedExceptionMsg, encodedSmartStackTrace, null );

            assertThat( decodedReportEntry ).isNotNull();
            assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
            assertThat( decodedReportEntry.getSourceText() ).isEqualTo( reportEntry.getSourceText() );
            assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
            assertThat( decodedReportEntry.getNameText() ).isEqualTo(reportEntry.getNameText());
            assertThat( decodedReportEntry.getGroup() ).isEqualTo( reportEntry.getGroup() );
            assertThat( decodedReportEntry.getMessage() ).isEqualTo( reportEntry.getMessage() );
            assertThat( decodedReportEntry.getElapsed() ).isNull();
            assertThat( decodedReportEntry.getStackTraceWriter() ).isNull();

            decodedReportEntry = toReportEntry( UTF_8, encodedSourceName, encodedSourceText, encodedName, encodedText,
                    encodedGroup, encodedMessage, "1003", encodedExceptionMsg, encodedSmartStackTrace, null );

            assertThat( decodedReportEntry ).isNotNull();
            assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
            assertThat( decodedReportEntry.getSourceText() ).isEqualTo( reportEntry.getSourceText() );
            assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
            assertThat( decodedReportEntry.getNameText() ).isEqualTo(reportEntry.getNameText());
            assertThat( decodedReportEntry.getGroup() ).isEqualTo( reportEntry.getGroup() );
            assertThat( decodedReportEntry.getMessage() ).isEqualTo( reportEntry.getMessage() );
            assertThat( decodedReportEntry.getElapsed() ).isEqualTo( 1003 );
            assertThat( decodedReportEntry.getStackTraceWriter() ).isNull();

            decodedReportEntry = toReportEntry( UTF_8, encodedSourceName, encodedSourceText, encodedName, encodedText,
                    encodedGroup, encodedMessage, "1003", encodedExceptionMsg, encodedSmartStackTrace,
                    encodedStackTrace );

            assertThat( decodedReportEntry ).isNotNull();
            assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
            assertThat( decodedReportEntry.getSourceText() ).isEqualTo( reportEntry.getSourceText() );
            assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
            assertThat( decodedReportEntry.getNameText() ).isEqualTo(reportEntry.getNameText());
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

            decodedReportEntry = toReportEntry( UTF_8, encodedSourceName, encodedSourceText, encodedName, encodedText,
                    encodedGroup, encodedMessage, "1003", encodedExceptionMsg, encodedSmartStackTrace,
                    encodedTrimmedStackTrace );

            assertThat( decodedReportEntry ).isNotNull();
            assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
            assertThat( decodedReportEntry.getSourceText() ).isEqualTo( reportEntry.getSourceText() );
            assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
            assertThat( decodedReportEntry.getNameText() ).isEqualTo(reportEntry.getNameText());
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

        @Test
        public void shouldSendByeEvent() throws IOException
        {
            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.bye();
            String read = new String( out.toByteArray(), UTF_8 );
            assertThat( read )
                    .isEqualTo( ":maven:surefire:std:out:bye\n" );
            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setByeListener( new EventAssertionListener() );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void shouldSendStopOnNextTestEvent() throws IOException
        {

            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.stopOnNextTest();
            String read = new String( out.toByteArray(), UTF_8 );
            assertThat( read )
                    .isEqualTo( ":maven:surefire:std:out:stop-on-next-test\n" );
            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setStopOnNextTestListener( new EventAssertionListener() );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void shouldSendNextTestEvent() throws IOException
        {

            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.acquireNextTest();
            String read = new String( out.toByteArray(), UTF_8 );
            assertThat( read )
                    .isEqualTo( ":maven:surefire:std:out:next-test\n" );
            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setAcquireNextTestListener( new EventAssertionListener() );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testConsole() throws IOException
        {
            Stream out = Stream.newStream();

            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.consoleInfoLog( "msg" );

            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setConsoleInfoListener( new StringEventAssertionListener( "msg" ) );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testError() throws IOException
        {
            Stream out = Stream.newStream();

            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.consoleErrorLog( "msg" );

            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setConsoleErrorListener( new StackTraceEventListener( "msg", null, null ) );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testErrorWithException() throws IOException
        {
            Throwable t = new Throwable( "msg" );

            Stream out = Stream.newStream();

            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.consoleErrorLog( t );

            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            String stackTrace = ConsoleLoggerUtils.toString( t );
            decoder.setConsoleErrorListener( new StackTraceEventListener( "msg", null, stackTrace ) );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testErrorWithStackTraceWriter() throws IOException
        {
            Stream out = Stream.newStream();

            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            StackTraceWriter stackTraceWriter = new DeserializedStacktraceWriter( "1", "2", "3" );
            forkedChannelEncoder.consoleErrorLog( stackTraceWriter, false );

            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setConsoleErrorListener( new StackTraceEventListener( "1", "2", "3" ) );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testDebug() throws IOException
        {
            Stream out = Stream.newStream();

            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.consoleDebugLog( "msg" );

            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setConsoleDebugListener( new StringEventAssertionListener( "msg" ) );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testWarning() throws IOException
        {
            Stream out = Stream.newStream();

            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.consoleWarningLog( "msg" );

            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setConsoleWarningListener( new StringEventAssertionListener( "msg" ) );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testStdOutStream() throws IOException
        {
            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.stdOut( "msg", false );

            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setStdOutListener( new StandardOutErrEventAssertionListener( NORMAL_RUN, "msg", false ) );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testStdOutStreamPrint() throws IOException
        {
            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.stdOut( "", false );

            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setStdOutListener( new StandardOutErrEventAssertionListener( NORMAL_RUN, "", false ) );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testStdOutStreamPrintWithNull() throws IOException
        {
            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.stdOut( null, false );

            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setStdOutListener( new StandardOutErrEventAssertionListener( NORMAL_RUN, null, false ) );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testStdOutStreamPrintln() throws IOException
        {
            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.stdOut( "", true );

            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setStdOutListener( new StandardOutErrEventAssertionListener( NORMAL_RUN, "", true ) );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testStdOutStreamPrintlnWithNull() throws IOException
        {
            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.stdOut( null, true );

            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setStdOutListener( new StandardOutErrEventAssertionListener( NORMAL_RUN, null, true ) );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testStdErrStream() throws IOException
        {
            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.stdErr( "msg", false );

            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setStdErrListener( new StandardOutErrEventAssertionListener( NORMAL_RUN, "msg", false ) );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void shouldCountSameNumberOfSystemProperties() throws IOException
        {
            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            forkedChannelEncoder.sendSystemProperties( ObjectUtils.systemProps() );

            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setSystemPropertiesListener( new PropertyEventAssertionListener() );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
        }

        @Test
        public void shouldHandleErrorAfterNullLine()
        {
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setSystemPropertiesListener( new PropertyEventAssertionListener() );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( null, errorHandler );
            verify( errorHandler, times( 1 ) )
                    .handledError( nullable( String.class ), nullable( Throwable.class ) );
        }

        @Test
        public void shouldHandleErrorAfterUnknownOperation()
        {
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setSystemPropertiesListener( new PropertyEventAssertionListener() );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( ":maven:surefire:std:out:abnormal-run:-", errorHandler );
            verify( errorHandler, times( 1 ) )
                    .handledError( eq( ":maven:surefire:std:out:abnormal-run:-" ), nullable( Throwable.class ) );
        }

        @Test
        public void shouldHandleExit() throws IOException
        {
            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
            StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
            when( stackTraceWriter.getThrowable() ).thenReturn( new SafeThrowable( "1" ) );
            when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( "2" );
            when( stackTraceWriter.writeTraceToString() ).thenReturn( "3" );
            when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( "4" );
            forkedChannelEncoder.sendExitEvent( stackTraceWriter, false );

            LineNumberReader lines = out.newReader( UTF_8 );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setExitErrorEventListener( new ForkedProcessExitErrorListener()
            {
                @Override
                public void handle( String exceptionMessage, String smartTrimmedStackTrace, String stackTrace )
                {
                    assertThat( exceptionMessage ).isEqualTo( "1" );
                    assertThat( smartTrimmedStackTrace ).isEqualTo( "2" );
                    assertThat( stackTrace ).isEqualTo( "3" );
                }
            } );
            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( lines.readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
        }
    }

    @RunWith( Theories.class )
    public static class ReportEntryTest
    {
        @DataPoints( value = "operation" )
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
        public static String[] reportedMessage = { null, "skipped test" };

        @DataPoints( value = "elapsed" )
        public static Integer[] elapsed = { null, 102 };

        @DataPoints( value = "trim" )
        public static boolean[] trim = { false, true };

        @DataPoints( value = "msg" )
        public static boolean[] msg = { false, true };

        @DataPoints( value = "smart" )
        public static boolean[] smart = { false, true };

        @DataPoints( value = "trace" )
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

            ReportEntry reportEntry = mock( ReportEntry.class );
            when( reportEntry.getElapsed() ).thenReturn( elapsed );
            when( reportEntry.getGroup() ).thenReturn( "this group" );
            when( reportEntry.getMessage() ).thenReturn( reportedMessage );
            when( reportEntry.getName() ).thenReturn( "my test" );
            when( reportEntry.getName() ).thenReturn( "display name of test" );
            when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
            when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
            when( reportEntry.getSourceText() ).thenReturn("test class display name");
            when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

            Stream out = Stream.newStream();

            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

            ForkedChannelEncoder.class.getMethod( operation[0], ReportEntry.class, boolean.class )
                    .invoke( forkedChannelEncoder, reportEntry, trim );

            ForkedChannelDecoder decoder = new ForkedChannelDecoder();

            ForkedChannelDecoder.class.getMethod( operation[1], ForkedProcessReportEventListener.class )
                    .invoke( decoder, new ReportEventAssertionListener( reportEntry ) );

            AssertionErrorHandler errorHandler = mock( AssertionErrorHandler.class );
            decoder.handleEvent( out.newReader( UTF_8 ).readLine(), errorHandler );
            verifyZeroInteractions( errorHandler );
        }
    }

    private static class AssertionErrorHandler implements ForkedChannelDecoderErrorHandler
    {
        public void handledError( String line, Throwable e )
        {
            if ( e != null )
            {
                e.printStackTrace();
            }
            fail( line + ( e == null ? "" : "\n" + e.getLocalizedMessage() ) );
        }
    }

    private static class PropertyEventAssertionListener implements ForkedProcessPropertyEventListener
    {
        private final Map sysProps = System.getProperties();

        public void handle( RunMode runMode, String key, String value )
        {
            assertThat( runMode ).isEqualTo( NORMAL_RUN );
            assertTrue( sysProps.containsKey( key ) );
            assertThat( sysProps.get( key ) ).isEqualTo( value );
        }
    }

    private static class EventAssertionListener implements ForkedProcessEventListener
    {
        public void handle()
        {
        }
    }

    private static class StringEventAssertionListener implements ForkedProcessStringEventListener
    {
        private final String msg;

        StringEventAssertionListener( String msg )
        {
            this.msg = msg;
        }

        public void handle( String msg )
        {
            assertThat( msg )
                    .isEqualTo( this.msg );
        }
    }

    private static class StackTraceEventListener implements ForkedProcessStackTraceEventListener
    {
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
        public void handle( String msg, String smartStackTrace, String stackTrace )
        {
            assertThat( msg )
                    .isEqualTo( this.msg );

            assertThat( smartStackTrace )
                    .isEqualTo( this.smartStackTrace );

            assertThat( stackTrace )
                    .isEqualTo( this.stackTrace );
        }
    }

    private static class StandardOutErrEventAssertionListener implements ForkedProcessStandardOutErrEventListener
    {
        private final RunMode runMode;
        private final String output;
        private final boolean newLine;

        StandardOutErrEventAssertionListener( RunMode runMode, String output, boolean newLine )
        {
            this.runMode = runMode;
            this.output = output;
            this.newLine = newLine;
        }

        public void handle( RunMode runMode, String output, boolean newLine )
        {
            assertThat( runMode )
                    .isEqualTo( this.runMode );

            assertThat( output )
                    .isEqualTo( this.output );

            assertThat( newLine )
                    .isEqualTo( this.newLine );
        }
    }

    private static class ReportEventAssertionListener implements ForkedProcessReportEventListener
    {
        private final ReportEntry reportEntry;

        ReportEventAssertionListener( ReportEntry reportEntry )
        {
            this.reportEntry = reportEntry;
        }

        public void handle( RunMode runMode, ReportEntry reportEntry )
        {
            assertThat( reportEntry.getSourceName() ).isEqualTo( this.reportEntry.getSourceName() );
            assertThat( reportEntry.getSourceText() ).isEqualTo( this.reportEntry.getSourceText() );
            assertThat( reportEntry.getName() ).isEqualTo( this.reportEntry.getName() );
            assertThat( reportEntry.getNameText() ).isEqualTo( this.reportEntry.getNameText() );
            assertThat( reportEntry.getGroup() ).isEqualTo( this.reportEntry.getGroup() );
            assertThat( reportEntry.getMessage() ).isEqualTo( this.reportEntry.getMessage() );
            assertThat( reportEntry.getElapsed() ).isEqualTo( this.reportEntry.getElapsed() );
            if ( reportEntry.getStackTraceWriter() == null )
            {
                assertThat( this.reportEntry.getStackTraceWriter() ).isNull();
            }
            else
            {
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

    private static byte[] toArray( ByteBuffer buffer )
    {
        return Arrays.copyOfRange( buffer.array(), buffer.arrayOffset(), buffer.arrayOffset() + buffer.remaining() );
    }
}
