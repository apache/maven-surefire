package org.apache.maven.surefire.booter;

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

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.util.internal.ObjectUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.copyOfRange;
import static org.apache.maven.surefire.shared.codec.binary.Base64.encodeBase64String;
import static org.apache.maven.surefire.booter.ForkedChannelEncoder.encode;
import static org.apache.maven.surefire.booter.ForkedChannelEncoder.encodeHeader;
import static org.apache.maven.surefire.booter.ForkedChannelEncoder.encodeMessage;
import static org.apache.maven.surefire.booter.ForkedChannelEncoder.encodeOpcode;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_SYSPROPS;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.MAGIC_NUMBER;
import static org.apache.maven.surefire.report.RunMode.NORMAL_RUN;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ForkedChannelEncoder}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public class ForkedChannelEncoderTest
{
    private static final int ELAPSED_TIME = 102;

    @Test
    public void shouldBeFailSafe()
    {
        assertThat( ForkedChannelEncoder.toBase64( null ) ).isEqualTo( "-" );
        assertThat( ForkedChannelEncoder.toBase64( "" ) ).isEqualTo( "" );
    }

    @Test
    public void shouldHaveSystemProperty()
    {
        StringBuilder actualEncoded = encode( BOOTERCODE_SYSPROPS, NORMAL_RUN, "arg1", "arg2" );
        String expected = MAGIC_NUMBER + BOOTERCODE_SYSPROPS.getOpcode() + ":normal-run" + ":UTF-8:YXJnMQ==:YXJnMg==";

        assertThat( actualEncoded.toString() )
                .isEqualTo( expected );
    }

    @Test
    public void safeThrowableShouldBeEncoded()
    {
        final String exceptionMessage = "msg";
        final String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        final String smartStackTrace = "MyTest:86 >> Error";
        final String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        final String stackTrace = "trace line 1\ntrace line 2";
        final String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        final String trimmedStackTrace = "trace line 1\ntrace line 2";
        final String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        StringBuilder encoded = new StringBuilder();
        encode( encoded, stackTraceWriter, false );
        assertThat( encoded.toString() )
                .isEqualTo( ":" + encodedExceptionMsg + ":" + encodedSmartStackTrace + ":" + encodedStackTrace );

        encoded = new StringBuilder();
        encode( encoded, stackTraceWriter, true );
        assertThat( encoded.toString() )
                .isEqualTo( ":" + encodedExceptionMsg + ":" + encodedSmartStackTrace + ":" + encodedTrimmedStackTrace );
    }

    @Test
    public void emptySafeThrowable()
    {
        SafeThrowable safeThrowable = new SafeThrowable( new Exception( "" ) );

        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( "" );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( "" );

        StringBuilder encoded = new StringBuilder();
        encode( encoded, stackTraceWriter, false );

        assertThat( encoded.toString() )
                .isEqualTo( ":::" );
    }

    @Test
    public void nullSafeThrowable()
    {
        SafeThrowable safeThrowable = new SafeThrowable( new Exception() );

        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );

        StringBuilder encoded = new StringBuilder();
        encode( encoded, stackTraceWriter, false );

        assertThat( encoded.toString() )
                .isEqualTo( ":-:-:-" );
    }

    @Test
    public void reportEntry() throws IOException
    {
        final String exceptionMessage = "msg";
        final String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        final String smartStackTrace = "MyTest:86 >> Error";
        final String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        final String stackTrace = "trace line 1\ntrace line 2";
        final String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        final String trimmedStackTrace = "trace line 1\ntrace line 2";
        final String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );


        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        StringBuilder encode = encode( "X", "normal-run", reportEntry, false );
        assertThat( encode.toString() )
                .isEqualTo( ":maven:surefire:std:out:X:normal-run:UTF-8:"
                                + encodedSourceName
                                + ":"
                                + "-"
                                + ":"
                                + encodedName
                                + ":"
                                + "-"
                                + ":"
                                + encodedGroup
                                + ":"
                                + encodedMessage
                                + ":"
                                + ELAPSED_TIME
                                + ":"

                                + encodedExceptionMsg
                                + ":"
                                + encodedSmartStackTrace
                                + ":"
                                + encodedStackTrace
                );

        encode = encode( "X", "normal-run", reportEntry, true );
        assertThat( encode.toString() )
                .isEqualTo( ":maven:surefire:std:out:X:normal-run:UTF-8:"
                                + encodedSourceName
                                + ":"
                                + "-"
                                + ":"
                                + encodedName
                                + ":"
                                + "-"
                                + ":"
                                + encodedGroup
                                + ":"
                                + encodedMessage
                                + ":"
                                + ELAPSED_TIME
                                + ":"

                                + encodedExceptionMsg
                                + ":"
                                + encodedSmartStackTrace
                                + ":"
                                + encodedTrimmedStackTrace
                );

        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.testSetStarting( reportEntry, true );
        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( ":maven:surefire:std:out:testset-starting:normal-run:UTF-8:"
                                    + encodedSourceName
                                    + ":"
                                    + "-"
                                    + ":"
                                    + encodedName
                                    + ":"
                                    + "-"
                                    + ":"
                                    + encodedGroup
                                    + ":"
                                    + encodedMessage
                                    + ":"
                                    + ELAPSED_TIME
                                    + ":"

                                    + encodedExceptionMsg
                                    + ":"
                                    + encodedSmartStackTrace
                                    + ":"
                                    + encodedTrimmedStackTrace
                );
        assertThat( printedLines.readLine() ).isNull();

        out = Stream.newStream();
        forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.testSetStarting( reportEntry, false );
        printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( ":maven:surefire:std:out:testset-starting:normal-run:UTF-8:"
                                    + encodedSourceName
                                    + ":"
                                    + "-"
                                    + ":"
                                    + encodedName
                                    + ":"
                                    + "-"
                                    + ":"
                                    + encodedGroup
                                    + ":"
                                    + encodedMessage
                                    + ":"
                                    + ELAPSED_TIME
                                    + ":"

                                    + encodedExceptionMsg
                                    + ":"
                                    + encodedSmartStackTrace
                                    + ":"
                                    + encodedStackTrace
                );
        assertThat( printedLines.readLine() ).isNull();
    }

    @Test
    public void testSetCompleted() throws IOException
    {
        String exceptionMessage = "msg";
        String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        String smartStackTrace = "MyTest:86 >> Error";
        String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        String stackTrace = "trace line 1\ntrace line 2";
        String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        String trimmedStackTrace = "trace line 1\ntrace line 2";
        String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.testSetCompleted( reportEntry, false );
        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( ":maven:surefire:std:out:testset-completed:normal-run:UTF-8:"
                        + encodedSourceName
                        + ":"
                        + "-"
                        + ":"
                        + encodedName
                        + ":"
                        + "-"
                        + ":"
                        + encodedGroup
                        + ":"
                        + encodedMessage
                        + ":"
                        + ELAPSED_TIME
                        + ":"

                        + encodedExceptionMsg
                        + ":"
                        + encodedSmartStackTrace
                        + ":"
                        + encodedStackTrace
                );
        assertThat( printedLines.readLine() ).isNull();
    }

    @Test
    public void testStarting() throws IOException
    {
        String exceptionMessage = "msg";
        String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        String smartStackTrace = "MyTest:86 >> Error";
        String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        String stackTrace = "trace line 1\ntrace line 2";
        String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        String trimmedStackTrace = "trace line 1\ntrace line 2";
        String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.testStarting( reportEntry, true );
        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( ":maven:surefire:std:out:test-starting:normal-run:UTF-8:"
                        + encodedSourceName
                        + ":"
                        + "-"
                        + ":"
                        + encodedName
                        + ":"
                        + "-"
                        + ":"
                        + encodedGroup
                        + ":"
                        + encodedMessage
                        + ":"
                        + ELAPSED_TIME
                        + ":"

                        + encodedExceptionMsg
                        + ":"
                        + encodedSmartStackTrace
                        + ":"
                        + encodedTrimmedStackTrace
                );
        assertThat( printedLines.readLine() ).isNull();
    }

    @Test
    public void testSuccess() throws IOException
    {
        String exceptionMessage = "msg";
        String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        String smartStackTrace = "MyTest:86 >> Error";
        String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        String stackTrace = "trace line 1\ntrace line 2";
        String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        String trimmedStackTrace = "trace line 1\ntrace line 2";
        String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.testSucceeded( reportEntry, true );
        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( ":maven:surefire:std:out:test-succeeded:normal-run:UTF-8:"
                        + encodedSourceName
                        + ":"
                        + "-"
                        + ":"
                        + encodedName
                        + ":"
                        + "-"
                        + ":"
                        + encodedGroup
                        + ":"
                        + encodedMessage
                        + ":"
                        + ELAPSED_TIME
                        + ":"

                        + encodedExceptionMsg
                        + ":"
                        + encodedSmartStackTrace
                        + ":"
                        + encodedTrimmedStackTrace
                );
        assertThat( printedLines.readLine() ).isNull();
    }

    @Test
    public void testFailed() throws IOException
    {
        String exceptionMessage = "msg";
        String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        String smartStackTrace = "MyTest:86 >> Error";
        String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        String stackTrace = "trace line 1\ntrace line 2";
        String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        String trimmedStackTrace = "trace line 1\ntrace line 2";
        String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.testFailed( reportEntry, false );
        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( ":maven:surefire:std:out:test-failed:normal-run:UTF-8:"
                        + encodedSourceName
                        + ":"
                        + "-"
                        + ":"
                        + encodedName
                        + ":"
                        + "-"
                        + ":"
                        + encodedGroup
                        + ":"
                        + encodedMessage
                        + ":"
                        + ELAPSED_TIME
                        + ":"

                        + encodedExceptionMsg
                        + ":"
                        + encodedSmartStackTrace
                        + ":"
                        + encodedStackTrace
                );
        assertThat( printedLines.readLine() ).isNull();
    }

    @Test
    public void testSkipped() throws IOException
    {
        String encodedExceptionMsg = "-";

        String smartStackTrace = "MyTest:86 >> Error";
        String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        String stackTrace = "trace line 1\ntrace line 2";
        String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        String trimmedStackTrace = "trace line 1\ntrace line 2";
        String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception() );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.testSkipped( reportEntry, false );
        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( ":maven:surefire:std:out:test-skipped:normal-run:UTF-8:"
                        + encodedSourceName
                        + ":"
                        + "-"
                        + ":"
                        + encodedName
                        + ":"
                        + "-"
                        + ":"
                        + encodedGroup
                        + ":"
                        + encodedMessage
                        + ":"
                        + ELAPSED_TIME
                        + ":"

                        + encodedExceptionMsg
                        + ":"
                        + encodedSmartStackTrace
                        + ":"
                        + encodedStackTrace
                );
        assertThat( printedLines.readLine() ).isNull();
    }

    @Test
    public void testError() throws IOException
    {
        String encodedExceptionMsg = "-";

        String encodedSmartStackTrace = "-";

        String stackTrace = "trace line 1\ntrace line 2";
        String encodedStackTrace = encodeBase64String( toArray( UTF_8.encode( stackTrace ) ) );

        String trimmedStackTrace = "trace line 1\ntrace line 2";
        String encodedTrimmedStackTrace = encodeBase64String( toArray( UTF_8.encode( trimmedStackTrace ) ) );

        SafeThrowable safeThrowable = new SafeThrowable( new Exception() );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( null );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( trimmedStackTrace );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( stackTrace );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.testError( reportEntry, false );
        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( ":maven:surefire:std:out:test-error:normal-run:UTF-8:"
                        + encodedSourceName
                        + ":"
                        + "-"
                        + ":"
                        + encodedName
                        + ":"
                        + "-"
                        + ":"
                        + encodedGroup
                        + ":"
                        + encodedMessage
                        + ":"
                        + ELAPSED_TIME
                        + ":"

                        + encodedExceptionMsg
                        + ":"
                        + encodedSmartStackTrace
                        + ":"
                        + encodedStackTrace
                );
        assertThat( printedLines.readLine() ).isNull();
    }

    @Test
    public void testAssumptionFailure() throws IOException
    {
        String exceptionMessage = "msg";
        String encodedExceptionMsg = encodeBase64String( toArray( UTF_8.encode( exceptionMessage ) ) );

        String smartStackTrace = "MyTest:86 >> Error";
        String encodedSmartStackTrace = encodeBase64String( toArray( UTF_8.encode( smartStackTrace ) ) );

        String encodedStackTrace = "-";

        SafeThrowable safeThrowable = new SafeThrowable( new Exception( exceptionMessage ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( safeThrowable );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( smartStackTrace );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( null );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( null );

        ReportEntry reportEntry = mock( ReportEntry.class );
        when( reportEntry.getElapsed() ).thenReturn( null );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        String encodedSourceName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getSourceName() ) ) );
        String encodedName = encodeBase64String( toArray( UTF_8.encode( reportEntry.getName() ) ) );
        String encodedGroup = encodeBase64String( toArray( UTF_8.encode( reportEntry.getGroup() ) ) );
        String encodedMessage = encodeBase64String( toArray( UTF_8.encode( reportEntry.getMessage() ) ) );

        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.testAssumptionFailure( reportEntry, false );
        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( ":maven:surefire:std:out:test-assumption-failure:normal-run:UTF-8:"
                        + encodedSourceName
                        + ":"
                        + "-"
                        + ":"
                        + encodedName
                        + ":"
                        + "-"
                        + ":"
                        + encodedGroup
                        + ":"
                        + encodedMessage
                        + ":"
                        + "-"
                        + ":"

                        + encodedExceptionMsg
                        + ":"
                        + encodedSmartStackTrace
                        + ":"
                        + encodedStackTrace
                );
        assertThat( printedLines.readLine() ).isNull();
    }

    @Test
    public void testBye() throws IOException
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.bye();
        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( ":maven:surefire:std:out:bye" );
        assertThat( printedLines.readLine() ).isNull();
    }

    @Test
    public void testStopOnNextTest() throws IOException
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.stopOnNextTest();
        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( ":maven:surefire:std:out:stop-on-next-test" );
        assertThat( printedLines.readLine() ).isNull();
    }

    @Test
    public void testAcquireNextTest() throws IOException
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.acquireNextTest();
        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( ":maven:surefire:std:out:next-test" );
        assertThat( printedLines.readLine() ).isNull();
    }

    @Test
    public void testSendOpcode()
    {
        StringBuilder encoded = encodeOpcode( "some-opcode", "normal-run" );
        assertThat( encoded.toString() )
                .isEqualTo( ":maven:surefire:std:out:some-opcode:normal-run" );

        encoded = encodeHeader( "some-opcode", "normal-run" );
        assertThat( encoded.toString() )
                .isEqualTo( ":maven:surefire:std:out:some-opcode:normal-run:UTF-8" );

        encoded = encodeMessage( "some-opcode", "normal-run", "msg" );
        assertThat( encoded.toString() )
                .isEqualTo( ":maven:surefire:std:out:some-opcode:normal-run:UTF-8:msg" );

        Stream out = Stream.newStream();
        ForkedChannelEncoder encoder = new ForkedChannelEncoder( out );
        encoded = encoder.print( "some-opcode", "msg" );
        assertThat( encoded.toString() )
                .isEqualTo( ":maven:surefire:std:out:some-opcode:UTF-8:bXNn" );

        encoded = encoder.print( "some-opcode", new String[] { null } );
        assertThat( encoded.toString() )
                .isEqualTo( ":maven:surefire:std:out:some-opcode:UTF-8:-" );
    }

    @Test
    public void testConsoleInfo()
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.consoleInfoLog( "msg" );

        String encoded = new String( out.toByteArray(), UTF_8 );

        String expected = ":maven:surefire:std:out:console-info-log:UTF-8:"
                                  + encodeBase64String( toArray( UTF_8.encode( "msg" ) ) )
                                  + "\n";

        assertThat( encoded )
                .isEqualTo( expected );
    }

    @Test
    public void testConsoleError()
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.consoleErrorLog( "msg" );

        String encoded = new String( out.toByteArray(), UTF_8 );

        String expected = ":maven:surefire:std:out:console-error-log:UTF-8:"
                + encodeBase64String( toArray( UTF_8.encode( "msg" ) ) )
                + "\n";

        assertThat( encoded )
                .isEqualTo( expected );
    }

    @Test
    public void testConsoleErrorLog1() throws IOException
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.consoleErrorLog( new Exception( "msg" ) );
        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .startsWith( ":maven:surefire:std:out:console-error-log:UTF-8:bXNn:-:" );
        assertThat( printedLines.readLine() ).isNull();
    }

    @Test
    public void testConsoleErrorLog2() throws IOException
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.consoleErrorLog( "msg2", new Exception( "msg" ) );
        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .startsWith( ":maven:surefire:std:out:console-error-log:UTF-8:bXNnMg==:-:" );
        assertThat( printedLines.readLine() ).isNull();
    }

    @Test
    public void testConsoleErrorLog3() throws IOException
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( new SafeThrowable( "1" ) );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( "2" );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( "3" );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( "4" );

        forkedChannelEncoder.consoleErrorLog( stackTraceWriter, true );
        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .startsWith( ":maven:surefire:std:out:console-error-log:UTF-8:MQ==:Mg==:NA==" );
        assertThat( printedLines.readLine() ).isNull();
    }

    @Test
    public void testConsoleDebug()
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.consoleDebugLog( "msg" );

        String encoded = new String( out.toByteArray(), UTF_8 );

        String expected = ":maven:surefire:std:out:console-debug-log:UTF-8:"
                                  + encodeBase64String( toArray( UTF_8.encode( "msg" ) ) )
                                  + "\n";

        assertThat( encoded )
                .isEqualTo( expected );
    }

    @Test
    public void testConsoleWarning()
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.consoleWarningLog( "msg" );

        String encoded = new String( out.toByteArray(), UTF_8 );

        String expected = ":maven:surefire:std:out:console-warning-log:UTF-8:"
                                  + encodeBase64String( toArray( UTF_8.encode( "msg" ) ) )
                                  + "\n";

        assertThat( encoded )
                .isEqualTo( expected );
    }

    @Test
    public void testStdOutStream() throws IOException
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.stdOut( "msg", false );

        String expected = ":maven:surefire:std:out:std-out-stream:normal-run:UTF-8:bXNn";

        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( expected );
        assertThat( printedLines.readLine() )
                .isNull();
    }

    @Test
    public void testStdOutStreamLn() throws IOException
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.stdOut( "msg", true );

        String expected = ":maven:surefire:std:out:std-out-stream-new-line:normal-run:UTF-8:bXNn";

        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( expected );
        assertThat( printedLines.readLine() )
                .isNull();
    }

    @Test
    public void testStdErrStream() throws IOException
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.stdErr( "msg", false );

        String expected = ":maven:surefire:std:out:std-err-stream:normal-run:UTF-8:bXNn";

        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( expected );
        assertThat( printedLines.readLine() )
                .isNull();
    }

    @Test
    public void testStdErrStreamLn() throws IOException
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        forkedChannelEncoder.stdErr( "msg", true );

        String expected = ":maven:surefire:std:out:std-err-stream-new-line:normal-run:UTF-8:bXNn";

        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .isEqualTo( expected );
        assertThat( printedLines.readLine() )
                .isNull();
    }

    @Test
    @SuppressWarnings( "checkstyle:innerassignment" )
    public void shouldCountSameNumberOfSystemProperties() throws IOException
    {
        Stream out = Stream.newStream();
        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );

        Map<String, String> sysProps = ObjectUtils.systemProps();
        int expectedSize = sysProps.size();
        forkedChannelEncoder.sendSystemProperties( sysProps );

        LineNumberReader printedLines = out.newReader( UTF_8 );

        int size = 0;
        for ( String line; ( line = printedLines.readLine() ) != null; size++ )
        {
            assertThat( line )
                    .startsWith( ":maven:surefire:std:out:sys-prop:normal-run:UTF-8:" );
        }

        assertThat( size )
                .isEqualTo( expectedSize );
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

        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .startsWith( ":maven:surefire:std:out:jvm-exit-error:UTF-8:MQ==:Mg==:Mw==" );
    }

    @Test
    public void shouldHandleExitWithTrimmedTrace() throws IOException
    {
        Stream out = Stream.newStream();

        ForkedChannelEncoder forkedChannelEncoder = new ForkedChannelEncoder( out );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( new SafeThrowable( "1" ) );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( "2" );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( "3" );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( "4" );
        forkedChannelEncoder.sendExitEvent( stackTraceWriter, true );

        LineNumberReader printedLines = out.newReader( UTF_8 );
        assertThat( printedLines.readLine() )
                .startsWith( ":maven:surefire:std:out:jvm-exit-error:UTF-8:MQ==:Mg==:NA==" );
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
        return copyOfRange( buffer.array(), buffer.arrayOffset(), buffer.arrayOffset() + buffer.remaining() );
    }

}
