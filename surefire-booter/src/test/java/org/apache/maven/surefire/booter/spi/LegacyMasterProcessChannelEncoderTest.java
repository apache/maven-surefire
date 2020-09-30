package org.apache.maven.surefire.booter.spi;

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
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.SafeThrowable;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.util.internal.ObjectUtils;
import org.apache.maven.surefire.api.util.internal.WritableBufferedByteChannel;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Map;
import java.util.Map.Entry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.copyOfRange;
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
import static org.apache.maven.surefire.api.util.internal.Channels.newBufferedChannel;
import static org.apache.maven.surefire.booter.spi.LegacyMasterProcessChannelEncoder.encode;
import static org.apache.maven.surefire.booter.spi.LegacyMasterProcessChannelEncoder.encodeHeader;
import static org.apache.maven.surefire.booter.spi.LegacyMasterProcessChannelEncoder.encodeMessage;
import static org.apache.maven.surefire.booter.spi.LegacyMasterProcessChannelEncoder.encodeOpcode;
import static org.apache.maven.surefire.booter.spi.LegacyMasterProcessChannelEncoder.estimateBufferLength;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link LegacyMasterProcessChannelEncoder}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
@SuppressWarnings( { "checkstyle:linelength", "checkstyle:magicnumber" } )
public class LegacyMasterProcessChannelEncoderTest
{
    private static final int ELAPSED_TIME = 102;
    private static final byte[] ELAPSED_TIME_HEXA = new byte[] {0, 0, 0, 0x66};

    @Test
    public void shouldComputeStreamPreemptiveLength()
    {
        CharsetEncoder encoder = UTF_8.newEncoder();

        // :maven-surefire-event:8:sys-prop:10:normal-run:5:UTF-8:0001:kkk:0001:vvv:
        assertThat( estimateBufferLength( BOOTERCODE_SYSPROPS, NORMAL_RUN, encoder, 0, "k", "v" ) )
            .isEqualTo( 72 );

        // :maven-surefire-event:16:testset-starting:10:normal-run:5:UTF-8:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:X0001:0001:sss:0001:sss:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_TESTSET_STARTING, NORMAL_RUN, encoder, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 149 );

        // :maven-surefire-event:17:testset-completed:10:normal-run:5:UTF-8:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:X0001:0001:sss:0001:sss:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_TESTSET_COMPLETED, NORMAL_RUN, encoder, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 150 );

        // :maven-surefire-event:13:test-starting:10:normal-run:5:UTF-8:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:X0001:0001:sss:0001:sss:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_TEST_STARTING, NORMAL_RUN, encoder, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 146 );

        // :maven-surefire-event:14:test-succeeded:10:normal-run:5:UTF-8:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:X0001:0001:sss:0001:sss:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_TEST_SUCCEEDED, NORMAL_RUN, encoder, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 147 );

        // :maven-surefire-event:11:test-failed:10:normal-run:5:UTF-8:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:X0001:0001:sss:0001:sss:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_TEST_FAILED, NORMAL_RUN, encoder, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 144 );

        // :maven-surefire-event:12:test-skipped:10:normal-run:5:UTF-8:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:X0001:0001:sss:0001:sss:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_TEST_SKIPPED, NORMAL_RUN, encoder, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 145 );

        // :maven-surefire-event:10:test-error:10:normal-run:5:UTF-8:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:X0001:0001:sss:0001:sss:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_TEST_ERROR, NORMAL_RUN, encoder, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 143 );

        // :maven-surefire-event:23:test-assumption-failure:10:normal-run:5:UTF-8:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:0001:sss:X0001:0001:sss:0001:sss:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_TEST_ASSUMPTIONFAILURE, NORMAL_RUN, encoder, 1, "s", "s", "s", "s", "s", "s", "s", "s", "s" ) )
            .isEqualTo( 156 );

        // :maven-surefire-event:14:std-out-stream:10:normal-run:5:UTF-8:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_STDOUT, NORMAL_RUN, encoder, 0, "s" ) )
            .isEqualTo( 69 );

        // :maven-surefire-event:23:std-out-stream-new-line:10:normal-run:5:UTF-8:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_STDOUT_NEW_LINE, NORMAL_RUN, encoder, 0, "s" ) )
            .isEqualTo( 78 );

        // :maven-surefire-event:14:std-err-stream:10:normal-run:5:UTF-8:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_STDERR, NORMAL_RUN, encoder, 0, "s" ) )
            .isEqualTo( 69 );

        // :maven-surefire-event:23:std-err-stream-new-line:10:normal-run:5:UTF-8:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_STDERR_NEW_LINE, NORMAL_RUN, encoder, 0, "s" ) )
            .isEqualTo( 78 );

        // :maven-surefire-event:16:console-info-log:5:UTF-8:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_CONSOLE_INFO, null, encoder, 0, "s" ) )
            .isEqualTo( 58 );

        // :maven-surefire-event:17:console-debug-log:5:UTF-8:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_CONSOLE_DEBUG, null, encoder, 0, "s" ) )
            .isEqualTo( 59 );

        // :maven-surefire-event:19:console-warning-log:5:UTF-8:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_CONSOLE_WARNING, null, encoder, 0, "s" ) )
            .isEqualTo( 61 );

        // :maven-surefire-event:17:console-error-log:5:UTF-8:0001:sss:
        assertThat( estimateBufferLength( BOOTERCODE_CONSOLE_ERROR, null, encoder, 0, "s" ) )
            .isEqualTo( 59 );

        // :maven-surefire-event:3:bye:
        assertThat( estimateBufferLength( BOOTERCODE_BYE, null, null, 0 ) )
            .isEqualTo( 28 );

        // :maven-surefire-event:17:stop-on-next-test:
        assertThat( estimateBufferLength( BOOTERCODE_STOP_ON_NEXT_TEST, null, null, 0 ) )
            .isEqualTo( 42 );

        // :maven-surefire-event:9:next-test:
        assertThat( estimateBufferLength( BOOTERCODE_NEXT_TEST, null, null, 0 ) )
            .isEqualTo( 34 );

        // :maven-surefire-event:14:jvm-exit-error:
        assertThat( estimateBufferLength( BOOTERCODE_JVM_EXIT_ERROR, null, null, 0 ) )
            .isEqualTo( 39 );
    }

    @Test
    public void reportEntry() throws IOException
    {
        final String exceptionMessage = "msg";
        final String smartStackTrace = "MyTest:86 >> Error";
        final String stackTrace = "trace line 1\ntrace line 2";
        final String trimmedStackTrace = "trace line 1\ntrace line 2";

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

        ByteBuffer encoded = encode( BOOTERCODE_TEST_ERROR, NORMAL_RUN, reportEntry, false );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":test-error:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 5 );
        expectedFrame.write( ":UTF-8:".getBytes( UTF_8 ) );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getSourceName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 7} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getGroup().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 12} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getMessage().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( 0xff );
        expectedFrame.write( ELAPSED_TIME_HEXA );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 3} );
        expectedFrame.write( ':' );
        expectedFrame.write( exceptionMessage.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 18} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getStackTraceWriter().smartTrimmedStackTrace().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 25} );
        expectedFrame.write( ':' );
        expectedFrame.write( stackTrace.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        encoded.flip();

        assertThat( toArray( encoded ) )
            .isEqualTo( expectedFrame.toByteArray() );

        encoded = encode( BOOTERCODE_TEST_ERROR, NORMAL_RUN, reportEntry, true );
        expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":test-error:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 5 );
        expectedFrame.write( ":UTF-8:".getBytes( UTF_8 ) );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getSourceName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 7} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getGroup().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 12} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getMessage().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( 0xff );
        expectedFrame.write( ELAPSED_TIME_HEXA );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 3} );
        expectedFrame.write( ':' );
        expectedFrame.write( exceptionMessage.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 18} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getStackTraceWriter().smartTrimmedStackTrace().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 25} );
        expectedFrame.write( ':' );
        expectedFrame.write( trimmedStackTrace.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        encoded.flip();

        assertThat( toArray( encoded ) )
            .isEqualTo( expectedFrame.toByteArray() );

        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.testSetStarting( reportEntry, true );
        expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 16 );
        expectedFrame.write( ":testset-starting:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 5 );
        expectedFrame.write( ":UTF-8:".getBytes( UTF_8 ) );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getSourceName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 7} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getGroup().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 12} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getMessage().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( 0xff );
        expectedFrame.write( ELAPSED_TIME_HEXA );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 3} );
        expectedFrame.write( ':' );
        expectedFrame.write( exceptionMessage.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 18} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getStackTraceWriter().smartTrimmedStackTrace().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 25} );
        expectedFrame.write( ':' );
        expectedFrame.write( trimmedStackTrace.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        assertThat( out.toByteArray() )
            .isEqualTo( expectedFrame.toByteArray() );

        out = Stream.newStream();
        encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.testSetStarting( reportEntry, false );
        expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 16 );
        expectedFrame.write( ":testset-starting:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 5 );
        expectedFrame.write( ":UTF-8:".getBytes( UTF_8 ) );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getSourceName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 7} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getGroup().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 12} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getMessage().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( 0xff );
        expectedFrame.write( ELAPSED_TIME_HEXA );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 3} );
        expectedFrame.write( ':' );
        expectedFrame.write( exceptionMessage.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 18} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getStackTraceWriter().smartTrimmedStackTrace().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 25} );
        expectedFrame.write( ':' );
        expectedFrame.write( stackTrace.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        assertThat( out.toByteArray() )
            .isEqualTo( expectedFrame.toByteArray() );
    }

    @Test
    public void testSetCompleted() throws IOException
    {
        String exceptionMessage = "msg";
        String smartStackTrace = "MyTest:86 >> Error";
        String stackTrace = "trace line 1\ntrace line 2";
        String trimmedStackTrace = "trace line 1\ntrace line 2";

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

        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.testSetCompleted( reportEntry, false );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 17 );
        expectedFrame.write( ":testset-completed:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 5 );
        expectedFrame.write( ":UTF-8:".getBytes( UTF_8 ) );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getSourceName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 7} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getGroup().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 12} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getMessage().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( 0xff );
        expectedFrame.write( ELAPSED_TIME_HEXA );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 3} );
        expectedFrame.write( ':' );
        expectedFrame.write( exceptionMessage.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 18} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getStackTraceWriter().smartTrimmedStackTrace().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 25} );
        expectedFrame.write( ':' );
        expectedFrame.write( stackTrace.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        assertThat( out.toByteArray() )
            .isEqualTo( expectedFrame.toByteArray() );
    }

    @Test
    public void testStarting() throws IOException
    {
        String exceptionMessage = "msg";
        String smartStackTrace = "MyTest:86 >> Error";
        String stackTrace = "trace line 1\ntrace line 2";
        String trimmedStackTrace = "trace line 1\ntrace line 2";

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

        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.testStarting( reportEntry, true );

        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 13 );
        expectedFrame.write( ":test-starting:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 5 );
        expectedFrame.write( ":UTF-8:".getBytes( UTF_8 ) );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getSourceName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 7} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getGroup().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 12} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getMessage().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( 0xff );
        expectedFrame.write( ELAPSED_TIME_HEXA );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 3} );
        expectedFrame.write( ':' );
        expectedFrame.write( exceptionMessage.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 18} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getStackTraceWriter().smartTrimmedStackTrace().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 25} );
        expectedFrame.write( ':' );
        expectedFrame.write( stackTrace.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        assertThat( out.toByteArray() )
            .isEqualTo( expectedFrame.toByteArray() );
    }

    @Test
    public void testSuccess() throws IOException
    {
        String exceptionMessage = "msg";
        String smartStackTrace = "MyTest:86 >> Error";
        String stackTrace = "trace line 1\ntrace line 2";
        String trimmedStackTrace = "trace line 1\ntrace line 2";

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

        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.testSucceeded( reportEntry, true );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 14 );
        expectedFrame.write( ":test-succeeded:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 5 );
        expectedFrame.write( ":UTF-8:".getBytes( UTF_8 ) );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getSourceName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 7} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getGroup().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 12} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getMessage().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( 0xff );
        expectedFrame.write( ELAPSED_TIME_HEXA );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 3} );
        expectedFrame.write( ':' );
        expectedFrame.write( exceptionMessage.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 18} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getStackTraceWriter().smartTrimmedStackTrace().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 25} );
        expectedFrame.write( ':' );
        expectedFrame.write( stackTrace.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        assertThat( out.toByteArray() )
            .isEqualTo( expectedFrame.toByteArray() );
    }

    @Test
    public void testFailed() throws IOException
    {
        String exceptionMessage = "msg";
        String smartStackTrace = "MyTest:86 >> Error";
        String stackTrace = "trace line 1\ntrace line 2";
        String trimmedStackTrace = "trace line 1\ntrace line 2";

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

        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.testFailed( reportEntry, false );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 11 );
        expectedFrame.write( ":test-failed:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 5 );
        expectedFrame.write( ":UTF-8:".getBytes( UTF_8 ) );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getSourceName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 7} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getGroup().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 12} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getMessage().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( 0xff );
        expectedFrame.write( ELAPSED_TIME_HEXA );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 3} );
        expectedFrame.write( ':' );
        expectedFrame.write( exceptionMessage.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 18} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getStackTraceWriter().smartTrimmedStackTrace().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 25} );
        expectedFrame.write( ':' );
        expectedFrame.write( stackTrace.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        assertThat( out.toByteArray() )
            .isEqualTo( expectedFrame.toByteArray() );
    }

    @Test
    public void testSkipped() throws IOException
    {
        String smartStackTrace = "MyTest:86 >> Error";
        String stackTrace = "trace line 1\ntrace line 2";
        String trimmedStackTrace = "trace line 1\ntrace line 2";

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

        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.testSkipped( reportEntry, false );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 12 );
        expectedFrame.write( ":test-skipped:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 5 );
        expectedFrame.write( ":UTF-8:".getBytes( UTF_8 ) );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getSourceName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 7} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getGroup().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 12} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getMessage().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( 0xFF );
        expectedFrame.write( ELAPSED_TIME_HEXA );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 18} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getStackTraceWriter().smartTrimmedStackTrace().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 25} );
        expectedFrame.write( ':' );
        expectedFrame.write( stackTrace.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        assertThat( out.toByteArray() )
            .isEqualTo( expectedFrame.toByteArray() );
    }

    @Test
    public void testError() throws IOException
    {
        String stackTrace = "trace line 1\ntrace line 2";

        String trimmedStackTrace = "trace line 1";

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

        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );
        encoder.testError( reportEntry, false );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":test-error:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 5 );
        expectedFrame.write( ":UTF-8:".getBytes( UTF_8 ) );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getSourceName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 7} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getGroup().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 12} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getMessage().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( 0xff );
        expectedFrame.write( ELAPSED_TIME_HEXA );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 25} );
        expectedFrame.write( ':' );
        expectedFrame.write( stackTrace.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        assertThat( out.toByteArray() )
            .isEqualTo( expectedFrame.toByteArray() );
    }

    @Test
    public void testAssumptionFailure() throws IOException
    {
        String exceptionMessage = "msg";

        String smartStackTrace = "MyTest:86 >> Error";

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

        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.testAssumptionFailure( reportEntry, false );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 23 );
        expectedFrame.write( ":test-assumption-failure:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 5 );
        expectedFrame.write( ":UTF-8:".getBytes( UTF_8 ) );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getSourceName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 7} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getName().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 10} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getGroup().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 12} );
        expectedFrame.write( ':' );
        expectedFrame.write( reportEntry.getMessage().getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 3} );
        expectedFrame.write( ':' );
        expectedFrame.write( exceptionMessage.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 18} );
        expectedFrame.write( ':' );
        expectedFrame.write( smartStackTrace.getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( new byte[] {0, 0, 0, 1} );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        assertThat( out.toByteArray() )
                .isEqualTo( expectedFrame.toByteArray() );
    }

    @Test
    public void testBye()
    {
        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.bye();

        String encoded = new String( out.toByteArray(), UTF_8 );

        assertThat( encoded )
                .isEqualTo( ":maven-surefire-event:\u0003:bye:" );
    }

    @Test
    public void testStopOnNextTest()
    {
        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.stopOnNextTest();

        String encoded = new String( out.toByteArray(), UTF_8 );
        assertThat( encoded )
                .isEqualTo( ":maven-surefire-event:\u0011:stop-on-next-test:" );
    }

    @Test
    public void testAcquireNextTest()
    {
        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.acquireNextTest();

        String encoded = new String( out.toByteArray(), UTF_8 );
        assertThat( encoded )
                .isEqualTo( ":maven-surefire-event:\u0009:next-test:" );
    }

    @Test
    public void testSendOpcode()
    {
        CharsetEncoder encoder = UTF_8.newEncoder();
        ByteBuffer result = ByteBuffer.allocate( 128 );
        encodeOpcode( result, BOOTERCODE_TEST_ERROR, NORMAL_RUN );
        assertThat( toString( result ) )
                .isEqualTo( ":maven-surefire-event:" + (char) 10 + ":test-error:" + (char) 10 + ":normal-run:" );

        result = ByteBuffer.allocate( 1024 );
        encodeHeader( encoder, result, BOOTERCODE_TEST_ERROR, NORMAL_RUN );
        assertThat( toString( result ) )
                .isEqualTo( ":maven-surefire-event:" + (char) 10 + ":test-error:" + (char) 10 + ":normal-run:"
                    + (char) 5 + ":UTF-8:" );

        result = encodeMessage( BOOTERCODE_TEST_ERROR, NORMAL_RUN, "msg" );
        assertThat( toString( result ) )
                .isEqualTo( ":maven-surefire-event:" + (char) 10 + ":test-error:" + (char) 10 + ":normal-run:"
                    + (char) 5 + ":UTF-8:\u0000\u0000\u0000\u0003:msg:" );

        Channel channel = new Channel();
        new LegacyMasterProcessChannelEncoder( channel ).stdOut( "msg", false );
        assertThat( toString( channel.src ) )
                .isEqualTo( ":maven-surefire-event:" + (char) 14 + ":std-out-stream:" + (char) 10 + ":normal-run:"
                    + (char) 5 + ":UTF-8:\u0000\u0000\u0000\u0003:msg:" );

        channel = new Channel();
        new LegacyMasterProcessChannelEncoder( channel ).stdErr( null, false );
        assertThat( toString( channel.src ) )
                .isEqualTo( ":maven-surefire-event:" + (char) 14 + ":std-err-stream:" + (char) 10 + ":normal-run:"
                    + (char) 5 + ":UTF-8:\u0000\u0000\u0000\u0001:\u0000:" );
    }

    @Test
    public void testConsoleInfo()
    {
        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.consoleInfoLog( "msg" );

        String encoded = new String( out.toByteArray(), UTF_8 );

        String expected = ":maven-surefire-event:\u0010:console-info-log:\u0005:UTF-8:\u0000\u0000\u0000\u0003:msg:";

        assertThat( encoded )
                .isEqualTo( expected );
    }

    @Test
    public void testConsoleError()
    {
        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.consoleErrorLog( "msg" );

        String encoded = new String( out.toByteArray(), UTF_8 );

        String expected = ":maven-surefire-event:\u0011:console-error-log:\u0005:UTF-8:"
            + "\u0000\u0000\u0000\u0003:msg:"
            + "\u0000\u0000\u0000\u0001:\u0000:"
            + "\u0000\u0000\u0000\u0001:\u0000:";

        assertThat( encoded )
                .isEqualTo( expected );
    }

    @Test
    public void testConsoleErrorLog1() throws IOException
    {
        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        Exception e = new Exception( "msg" );
        encoder.consoleErrorLog( e );
        String stackTrace = ConsoleLoggerUtils.toString( e );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:\u0011:console-error-log:\u0005:UTF-8:".getBytes( UTF_8 ) );
        expectedFrame.write( 0 );
        expectedFrame.write( 0 );
        expectedFrame.write( 0 );
        expectedFrame.write( 3 );
        expectedFrame.write( ':' );
        expectedFrame.write( "msg".getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( 0 );
        expectedFrame.write( 0 );
        expectedFrame.write( 1 );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        byte[] stackTraceBytes = stackTrace.getBytes( UTF_8 );
        int[] stackTraceLength = toBytes( stackTraceBytes.length );
        expectedFrame.write( stackTraceLength[0] );
        expectedFrame.write( stackTraceLength[1] );
        expectedFrame.write( stackTraceLength[2] );
        expectedFrame.write( stackTraceLength[3] );
        expectedFrame.write( ':' );
        expectedFrame.write( stackTraceBytes );
        expectedFrame.write( ':' );
        assertThat( out.toByteArray() )
            .isEqualTo( expectedFrame.toByteArray() );
    }

    @Test
    public void testConsoleErrorLog2() throws IOException
    {
        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        Exception e = new Exception( "msg" );
        encoder.consoleErrorLog( "msg2", e );
        String stackTrace = ConsoleLoggerUtils.toString( e );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:\u0011:console-error-log:\u0005:UTF-8:".getBytes( UTF_8 ) );
        expectedFrame.write( 0 );
        expectedFrame.write( 0 );
        expectedFrame.write( 0 );
        expectedFrame.write( 4 );
        expectedFrame.write( ':' );
        expectedFrame.write( "msg2".getBytes( UTF_8 ) );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( 0 );
        expectedFrame.write( 0 );
        expectedFrame.write( 1 );
        expectedFrame.write( ':' );
        expectedFrame.write( 0 );
        expectedFrame.write( ':' );
        byte[] stackTraceBytes = stackTrace.getBytes( UTF_8 );
        int[] stackTraceLength = toBytes( stackTraceBytes.length );
        expectedFrame.write( stackTraceLength[0] );
        expectedFrame.write( stackTraceLength[1] );
        expectedFrame.write( stackTraceLength[2] );
        expectedFrame.write( stackTraceLength[3] );
        expectedFrame.write( ':' );
        expectedFrame.write( stackTraceBytes );
        expectedFrame.write( ':' );
        assertThat( out.toByteArray() )
            .isEqualTo( expectedFrame.toByteArray() );
    }

    @Test
    public void testConsoleErrorLog3()
    {
        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( new SafeThrowable( "1" ) );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( "2" );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( "3" );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( "4" );

        encoder.consoleErrorLog( stackTraceWriter, true );
        String encoded = new String( out.toByteArray(), UTF_8 );
        assertThat( encoded )
                .startsWith( ":maven-surefire-event:\u0011:console-error-log:\u0005:UTF-8:\u0000\u0000\u0000\u0001:1:\u0000\u0000\u0000\u0001:2:\u0000\u0000\u0000\u0001:4:" );
    }

    @Test
    public void testConsoleDebug()
    {
        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.consoleDebugLog( "msg" );

        String encoded = new String( out.toByteArray(), UTF_8 );

        String expected = ":maven-surefire-event:\u0011:console-debug-log:\u0005:UTF-8:\u0000\u0000\u0000\u0003:msg:";

        assertThat( encoded )
                .isEqualTo( expected );
    }

    @Test
    public void testConsoleWarning()
    {
        Stream out = Stream.newStream();
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );

        encoder.consoleWarningLog( "msg" );

        String encoded = new String( out.toByteArray(), UTF_8 );

        String expected = ":maven-surefire-event:\u0013:console-warning-log:\u0005:UTF-8:\u0000\u0000\u0000\u0003:msg:";

        assertThat( encoded )
                .isEqualTo( expected );
    }

    @Test
    public void testStdOutStream() throws IOException
    {
        Stream out = Stream.newStream();
        WritableBufferedByteChannel channel = newBufferedChannel( out );
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( channel );

        encoder.stdOut( "msg", false );
        channel.close();

        String expected = ":maven-surefire-event:\u000e:std-out-stream:"
            + (char) 10 + ":normal-run:\u0005:UTF-8:\u0000\u0000\u0000\u0003:msg:";

        assertThat( new String( out.toByteArray(), UTF_8 ) )
                .isEqualTo( expected );
    }

    @Test
    public void testStdOutStreamLn() throws IOException
    {
        Stream out = Stream.newStream();
        WritableBufferedByteChannel channel = newBufferedChannel( out );
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( channel );

        encoder.stdOut( "msg", true );
        channel.close();

        String expected = ":maven-surefire-event:\u0017:std-out-stream-new-line:"
            + (char) 10 + ":normal-run:\u0005:UTF-8:\u0000\u0000\u0000\u0003:msg:";

        assertThat( new String( out.toByteArray(), UTF_8 ) )
                .isEqualTo( expected );
    }

    @Test
    public void testStdErrStream() throws IOException
    {
        Stream out = Stream.newStream();
        WritableBufferedByteChannel channel = newBufferedChannel( out );
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( channel );

        encoder.stdErr( "msg", false );
        channel.close();

        String expected = ":maven-surefire-event:\u000e:std-err-stream:"
            + (char) 10 + ":normal-run:\u0005:UTF-8:\u0000\u0000\u0000\u0003:msg:";

        assertThat( new String( out.toByteArray(), UTF_8 ) )
                .isEqualTo( expected );
    }

    @Test
    public void testStdErrStreamLn() throws IOException
    {
        Stream out = Stream.newStream();
        WritableBufferedByteChannel channel = newBufferedChannel( out );
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( channel );

        encoder.stdErr( "msg", true );
        channel.close();

        String expected = ":maven-surefire-event:\u0017:std-err-stream-new-line:"
            + (char) 10 + ":normal-run:\u0005:UTF-8:\u0000\u0000\u0000\u0003:msg:";

        assertThat( new String( out.toByteArray(), UTF_8 ) )
                .isEqualTo( expected );
    }

    @Test
    @SuppressWarnings( "checkstyle:innerassignment" )
    public void shouldCountSameNumberOfSystemProperties() throws IOException
    {
        Stream stream = Stream.newStream();
        WritableBufferedByteChannel channel = newBufferedChannel( stream );
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( channel );

        Map<String, String> sysProps = ObjectUtils.systemProps();
        encoder.sendSystemProperties( sysProps );
        channel.close();

        for ( Entry<String, String> entry : sysProps.entrySet() )
        {
            int[] k = toBytes( entry.getKey().length() );
            int[] v = toBytes( entry.getValue() == null ? 1 : entry.getValue().getBytes( UTF_8 ).length );
            ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
            expectedFrame.write( ":maven-surefire-event:sys-prop:normal-run:UTF-8:".getBytes( UTF_8 ) );
            expectedFrame.write( k[0] );
            expectedFrame.write( k[1] );
            expectedFrame.write( k[2] );
            expectedFrame.write( k[3] );
            expectedFrame.write( ':' );
            expectedFrame.write( v[0] );
            expectedFrame.write( v[1] );
            expectedFrame.write( v[2] );
            expectedFrame.write( v[3] );
            expectedFrame.write( ':' );
            expectedFrame.write( ( entry.getValue() == null ? "\u0000" : entry.getValue() ).getBytes( UTF_8 ) );
            expectedFrame.write( ':' );
            assertThat( stream.toByteArray() )
                .contains( expectedFrame.toByteArray() );
        }
    }

    @Test
    public void shouldHandleExit()
    {
        Stream out = Stream.newStream();

        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( new SafeThrowable( "1" ) );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( "2" );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( "3" );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( "4" );
        encoder.sendExitError( stackTraceWriter, false );

        assertThat( new String( out.toByteArray(), UTF_8 ) )
                .startsWith( ":maven-surefire-event:\u000e:jvm-exit-error:\u0005:UTF-8:\u0000\u0000\u0000\u0001:1:\u0000\u0000\u0000\u0001:2:\u0000\u0000\u0000\u0001:3:" );
    }

    @Test
    public void shouldHandleExitWithTrimmedTrace()
    {
        Stream out = Stream.newStream();

        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( newBufferedChannel( out ) );
        StackTraceWriter stackTraceWriter = mock( StackTraceWriter.class );
        when( stackTraceWriter.getThrowable() ).thenReturn( new SafeThrowable( "1" ) );
        when( stackTraceWriter.smartTrimmedStackTrace() ).thenReturn( "2" );
        when( stackTraceWriter.writeTraceToString() ).thenReturn( "3" );
        when( stackTraceWriter.writeTrimmedTraceToString() ).thenReturn( "4" );
        encoder.sendExitError( stackTraceWriter, true );

        assertThat( new String( out.toByteArray(), UTF_8 ) )
            .startsWith( ":maven-surefire-event:\u000e:jvm-exit-error:\u0005:UTF-8:\u0000\u0000\u0000\u0001:1:\u0000\u0000\u0000\u0001:2:\u0000\u0000\u0000\u0001:4:" );
    }

    @Test
    public void testInterruptHandling() throws IOException
    {
        Stream out = Stream.newStream();
        WritableBufferedByteChannel channel = newBufferedChannel( out );
        LegacyMasterProcessChannelEncoder encoder = new LegacyMasterProcessChannelEncoder( channel );

        Thread.currentThread().interrupt();
        try
        {
            encoder.stdOut( "msg", false );
            channel.close();
        }
        finally
        {
            // Clear the interrupt and make sure it survived the invocation
            assertThat( Thread.interrupted() )
                    .isTrue();
        }

        assertThat( new String( out.toByteArray(), UTF_8 ) )
                .isEqualTo( ":maven-surefire-event:\u000e:std-out-stream:"
                    + (char) 10 + ":normal-run:\u0005:UTF-8:\u0000\u0000\u0000\u0003:msg:" );
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

    private static String toString( ByteBuffer frame )
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        frame.flip();
        os.write( frame.array(), frame.arrayOffset() + frame.position(), frame.remaining() );
        return new String( os.toByteArray(), UTF_8 );
    }

    private static int[] toBytes( int i )
    {
        int[] result = new int[4];
        result[0] = 0xff & ( i >> 24 );
        result[1] = 0xff & ( i >> 16 );
        result[2] = 0xff & ( i >> 8 );
        result[3] = 0xff & i;
        return result;
    }

    private static final class Channel implements WritableBufferedByteChannel
    {
        ByteBuffer src;

        @Override
        public void writeBuffered( ByteBuffer src ) throws IOException
        {
            this.src = src;
        }

        @Override
        public long countBufferOverflows()
        {
            return 0;
        }

        @Override
        public int write( ByteBuffer src ) throws IOException
        {
            this.src = src;
            return 0;
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
}
