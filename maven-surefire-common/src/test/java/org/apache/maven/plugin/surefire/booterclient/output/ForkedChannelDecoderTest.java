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

import org.apache.maven.surefire.booter.ForkedChannelEncoder;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunMode;
import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.StackTraceWriter;
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

import static java.nio.charset.Charset.defaultCharset;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static org.apache.maven.plugin.surefire.booterclient.output.ForkedChannelDecoder.toReportEntry;
import static org.apache.maven.surefire.report.RunMode.NORMAL_RUN;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
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
            Charset encoding = Charset.defaultCharset();
            assertThat( ForkedChannelDecoder.decode( null, encoding ) ).isNull();
            assertThat( ForkedChannelDecoder.decode( "-", encoding ) ).isNull();
            assertThat( ForkedChannelDecoder.decodeToInteger( null ) ).isNull();
            assertThat( ForkedChannelDecoder.decodeToInteger( "-" ) ).isNull();
            assertThat( ForkedChannelDecoder.decodeToBytes( null ) ).isNull();
            assertThat( ForkedChannelDecoder.decodeToBytes( "-" ) ).isNull();
        }

        @Test
        public void shouldHaveSystemProperty() throws IOException
        {
            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( defaultCharset(), out );
            forkedChannelEncoder.sendSystemProperties();

            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setSystemPropertiesListener( new PropertyEventAssertionListener( NORMAL_RUN ) );
            LineNumberReader reader = out.newReader( defaultCharset() );
            for ( String line; ( line = reader.readLine() ) != null; )
            {
                decoder.handleEvent( line, new AssertionErrorHandler() );
            }
            assertThat( reader.getLineNumber() ).isPositive();
        }

        @Test
        public void shouldRecognizeEmptyStream4ReportEntry()
        {
            ReportEntry reportEntry = toReportEntry( null, null, "", null, null, "", "", "", null );
            assertThat( reportEntry ).isNull();

            reportEntry = toReportEntry( defaultCharset(), "", "", "", "", "-", "", "", "" );
            assertThat( reportEntry ).isNotNull();
            assertThat( reportEntry.getStackTraceWriter() ).isNull();
            assertThat( reportEntry.getSourceName() ).isEmpty();
            assertThat( reportEntry.getName() ).isEmpty();
            assertThat( reportEntry.getGroup() ).isEmpty();
            assertThat( reportEntry.getNameWithGroup() ).isEmpty();
            assertThat( reportEntry.getMessage() ).isEmpty();
            assertThat( reportEntry.getElapsed() ).isNull();

            rule.expect( NumberFormatException.class );
            toReportEntry( defaultCharset(), "", "", "", "", "", "", "", "" );
            fail();
        }

        @Test
        public void testCreatingReportEntry()
        {
            final Charset utf8 = Charset.forName( "UTF-8" );

            final String exceptionMessage = "msg";
            final String encodedExceptionMsg = printBase64Binary( toArray( utf8.encode( exceptionMessage ) ) );

            final String smartStackTrace = "MyTest:86 >> Error";
            final String encodedSmartStackTrace = printBase64Binary( toArray( utf8.encode( smartStackTrace ) ) );

            final String stackTrace = "Exception: msg\ntrace line 1\ntrace line 2";
            final String encodedStackTrace = printBase64Binary( toArray( utf8.encode( stackTrace ) ) );

            final String trimmedStackTrace = "trace line 1\ntrace line 2";
            final String encodedTrimmedStackTrace = printBase64Binary( toArray( utf8.encode( trimmedStackTrace ) ) );

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
            when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
            when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
            when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

            String encodedSourceName = printBase64Binary( toArray( utf8.encode( reportEntry.getSourceName() ) ) );
            String encodedName = printBase64Binary( toArray( utf8.encode( reportEntry.getName() ) ) );
            String encodedGroup = printBase64Binary( toArray( utf8.encode( reportEntry.getGroup() ) ) );
            String encodedMessage = printBase64Binary( toArray( utf8.encode( reportEntry.getMessage() ) ) );

            ReportEntry decodedReportEntry = toReportEntry( utf8, encodedSourceName, encodedName, encodedGroup,
                                                                  encodedMessage, "-", null, null, null
            );

            assertThat( decodedReportEntry ).isNotNull();
            assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
            assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
            assertThat( decodedReportEntry.getGroup() ).isEqualTo( reportEntry.getGroup() );
            assertThat( decodedReportEntry.getMessage() ).isEqualTo( reportEntry.getMessage() );
            assertThat( decodedReportEntry.getStackTraceWriter() ).isNull();

            decodedReportEntry = toReportEntry( utf8, encodedSourceName, encodedName, encodedGroup, encodedMessage, "-",
                                                      encodedExceptionMsg, encodedSmartStackTrace, null
            );

            assertThat( decodedReportEntry ).isNotNull();
            assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
            assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
            assertThat( decodedReportEntry.getGroup() ).isEqualTo( reportEntry.getGroup() );
            assertThat( decodedReportEntry.getMessage() ).isEqualTo( reportEntry.getMessage() );
            assertThat( decodedReportEntry.getElapsed() ).isNull();
            assertThat( decodedReportEntry.getStackTraceWriter() ).isNull();

            decodedReportEntry = toReportEntry( utf8, encodedSourceName, encodedName, encodedGroup, encodedMessage,
                                                      "1003", encodedExceptionMsg, encodedSmartStackTrace, null
            );

            assertThat( decodedReportEntry ).isNotNull();
            assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
            assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
            assertThat( decodedReportEntry.getGroup() ).isEqualTo( reportEntry.getGroup() );
            assertThat( decodedReportEntry.getMessage() ).isEqualTo( reportEntry.getMessage() );
            assertThat( decodedReportEntry.getElapsed() ).isEqualTo( 1003 );
            assertThat( decodedReportEntry.getStackTraceWriter() ).isNull();

            decodedReportEntry = toReportEntry( utf8, encodedSourceName, encodedName, encodedGroup, encodedMessage,
                                                      "1003", encodedExceptionMsg, encodedSmartStackTrace,
                                                      encodedStackTrace
            );

            assertThat( decodedReportEntry ).isNotNull();
            assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
            assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
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

            decodedReportEntry = toReportEntry( utf8, encodedSourceName, encodedName, encodedGroup, encodedMessage,
                                                      "1003", encodedExceptionMsg, encodedSmartStackTrace,
                                                      encodedTrimmedStackTrace
            );

            assertThat( decodedReportEntry ).isNotNull();
            assertThat( decodedReportEntry.getSourceName() ).isEqualTo( reportEntry.getSourceName() );
            assertThat( decodedReportEntry.getName() ).isEqualTo( reportEntry.getName() );
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
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( defaultCharset(), out );
            forkedChannelEncoder.bye();
            String read = new String( out.toByteArray(), defaultCharset() );
            assertThat( read )
                    .isEqualTo( ":maven:surefire:std:out:bye:normal-run\n" );
            LineNumberReader lines = out.newReader( defaultCharset() );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setByeListener( new EventAssertionListener( NORMAL_RUN ) );
            decoder.handleEvent( lines.readLine(), new AssertionErrorHandler() );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void shouldSendStopOnNextTestEvent() throws IOException
        {

            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( defaultCharset(), out );
            forkedChannelEncoder.stopOnNextTest();
            String read = new String( out.toByteArray(), defaultCharset() );
            assertThat( read )
                    .isEqualTo( ":maven:surefire:std:out:stopOnNextTest:normal-run\n" );
            LineNumberReader lines = out.newReader( defaultCharset() );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setStopOnNextTestListener( new EventAssertionListener( NORMAL_RUN ) );
            decoder.handleEvent( lines.readLine(), new AssertionErrorHandler() );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void shouldSendNextTestEvent() throws IOException
        {

            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( defaultCharset(), out );
            forkedChannelEncoder.acquireNextTest();
            String read = new String( out.toByteArray(), defaultCharset() );
            assertThat( read )
                    .isEqualTo( ":maven:surefire:std:out:nextTest:normal-run\n" );
            LineNumberReader lines = out.newReader( defaultCharset() );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setAcquireNextTestListener( new EventAssertionListener( NORMAL_RUN ) );
            decoder.handleEvent( lines.readLine(), new AssertionErrorHandler() );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testConsole() throws IOException
        {
            Stream out = Stream.newStream();

            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( defaultCharset(), out );
            forkedChannelEncoder.console( "msg" );

            LineNumberReader lines = out.newReader( defaultCharset() );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setConsoleInfoListener( new StringEventAssertionListener( "msg" ) );
            decoder.handleEvent( lines.readLine(), new AssertionErrorHandler() );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testError() throws IOException
        {
            Stream out = Stream.newStream();

            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( defaultCharset(), out );
            forkedChannelEncoder.error( "msg" );

            LineNumberReader lines = out.newReader( defaultCharset() );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setConsoleErrorListener( new StackTraceEventListener( "msg", "stack trace" ) );
            decoder.handleEvent( lines.readLine(), new AssertionErrorHandler() );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testDebug() throws IOException
        {
            Stream out = Stream.newStream();

            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( defaultCharset(), out );
            forkedChannelEncoder.debug( "msg" );

            LineNumberReader lines = out.newReader( defaultCharset() );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setConsoleDebugListener( new StringEventAssertionListener( "msg" ) );
            decoder.handleEvent( lines.readLine(), new AssertionErrorHandler() );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testWarning() throws IOException
        {
            Stream out = Stream.newStream();

            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( defaultCharset(), out );
            forkedChannelEncoder.warning( "msg" );

            LineNumberReader lines = out.newReader( defaultCharset() );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setConsoleWarningListener( new StringEventAssertionListener( "msg" ) );
            decoder.handleEvent( lines.readLine(), new AssertionErrorHandler() );
            assertThat( lines.readLine() )
                    .isNull();
        }

        @Test
        public void testStdOutStream() throws IOException
        {
            Charset streamEncoding = Charset.forName( "UTF-8" );
            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( streamEncoding, out );

            final Charset encoding = defaultCharset();
            byte[] msgArray = toArray( encoding.encode( "msg" ) );
            assertThat( encoding.decode( ByteBuffer.wrap( msgArray ) ).toString() ).isEqualTo( "msg" );
            forkedChannelEncoder.stdOut( msgArray, 0, msgArray.length );

            LineNumberReader printedLines = out.newReader( defaultCharset() );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setStdOutListener( new BinaryEventAssertionListener( NORMAL_RUN, encoding,
                                                                               "msg".getBytes( encoding )
                    )
            );
            decoder.handleEvent( printedLines.readLine(), new AssertionErrorHandler() );
            assertThat( printedLines.readLine() )
                    .isNull();
        }

        @Test
        public void testStdErrStream() throws IOException
        {
            Charset streamEncoding = Charset.forName( "ISO-8859-1" );
            Stream out = Stream.newStream();
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( streamEncoding, out );

            final Charset encoding = defaultCharset();
            byte[] msgArray = toArray( encoding.encode( "msg" ) );
            assertThat( encoding.decode( ByteBuffer.wrap( msgArray ) ).toString() ).isEqualTo( "msg" );
            forkedChannelEncoder.stdErr( msgArray, 0, msgArray.length );

            LineNumberReader printedLines = out.newReader( defaultCharset() );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setStdErrListener( new BinaryEventAssertionListener( NORMAL_RUN, encoding,
                                                                               "msg".getBytes( encoding )
                    )
            );
            decoder.handleEvent( printedLines.readLine(), new AssertionErrorHandler() );
            assertThat( printedLines.readLine() )
                    .isNull();
        }

        @Test
        public void shouldCountSameNumberOfSystemProperties() throws IOException
        {
            Stream out = Stream.newStream();

            Charset streamEncoding = Charset.forName( "ISO-8859-1" );
            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( streamEncoding, out );
            forkedChannelEncoder.sendSystemProperties();

            LineNumberReader printedLines = out.newReader( streamEncoding );
            ForkedChannelDecoder decoder = new ForkedChannelDecoder();
            decoder.setSystemPropertiesListener( new PropertyEventAssertionListener( NORMAL_RUN ) );
            decoder.handleEvent( printedLines.readLine(), new AssertionErrorHandler() );
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
            when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
            when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
            when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

            Stream out = Stream.newStream();

            ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( defaultCharset(), out );

            ForkedChannelEncoder.class.getMethod( operation[0], ReportEntry.class, boolean.class )
                    .invoke( forkedChannelEncoder, reportEntry, trim );

            ForkedChannelDecoder forkedChannelDecoder = new ForkedChannelDecoder();

            ForkedChannelDecoder.class.getMethod( operation[1], ForkedProcessReportEventListener.class )
                    .invoke( forkedChannelDecoder, new ReportEventAssertionListener( reportEntry ) );

            forkedChannelDecoder.handleEvent( out.newReader( defaultCharset() ).readLine(),
                                                    new AssertionErrorHandler()
            );
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
        private final RunMode runMode;

        PropertyEventAssertionListener( RunMode runMode )
        {
            this.runMode = runMode;
        }

        public void handle( String key, String value )
        {
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
        private final String stackTrace;

        StackTraceEventListener( String msg, String stackTrace )
        {
            this.msg = msg;
            this.stackTrace = stackTrace;
        }

        @Override
        public void handle( String msg, String stackTrace )
        {
            assertThat( msg )
                    .isEqualTo( this.msg );

            assertThat( stackTrace )
                    .isEqualTo( this.stackTrace );
        }
    }

    private static class BinaryEventAssertionListener implements ForkedProcessBinaryEventListener
    {
        private final RunMode runMode;
        private final Charset encoding;
        private final byte[] binary;

        BinaryEventAssertionListener( RunMode runMode, Charset encoding, byte[] binary )
        {
            this.runMode = runMode;
            this.encoding = encoding;
            this.binary = binary;
        }

        public void handle( RunMode runMode, Charset encoding, byte[] binary )
        {
            assertThat( runMode )
                    .isEqualTo( this.runMode );

            assertThat( encoding )
                    .isEqualTo( this.encoding );

            assertThat( binary )
                    .isEqualTo( this.binary );
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
            assertThat( reportEntry.getName() ).isEqualTo( this.reportEntry.getName() );
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

        public Stream( ByteArrayOutputStream out )
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
