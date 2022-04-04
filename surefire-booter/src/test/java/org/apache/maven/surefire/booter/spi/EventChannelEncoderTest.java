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
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.api.util.internal.ObjectUtils;
import org.apache.maven.surefire.api.util.internal.WritableBufferedByteChannel;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Map.Entry;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.copyOfRange;
import static org.apache.maven.surefire.api.booter.ForkedProcessEventType.BOOTERCODE_TEST_ERROR;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.report.TestOutputReportEntry.stdErr;
import static org.apache.maven.surefire.api.report.TestOutputReportEntry.stdErrln;
import static org.apache.maven.surefire.api.report.TestOutputReportEntry.stdOut;
import static org.apache.maven.surefire.api.report.TestOutputReportEntry.stdOutln;
import static org.apache.maven.surefire.api.util.internal.Channels.newBufferedChannel;
import static org.apache.maven.surefire.api.util.internal.ObjectUtils.systemProps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link EventChannelEncoder}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
@SuppressWarnings( { "checkstyle:linelength", "checkstyle:magicnumber" } )
public class EventChannelEncoderTest
{
    private static final int ELAPSED_TIME = 102;
    private static final byte[] ELAPSED_TIME_HEXA = new byte[] {0, 0, 0, 0x66};

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

        TestSetReportEntry reportEntry = mock( TestSetReportEntry.class );
        when( reportEntry.getRunMode() ).thenReturn( NORMAL_RUN );
        when( reportEntry.getTestRunId() ).thenReturn( 1L );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        Stream out = Stream.newStream();
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );
        ByteBuffer encoded = encoder.encode( BOOTERCODE_TEST_ERROR, reportEntry, false );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":test-error:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001".getBytes() );
        expectedFrame.write( ':' );
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
        ( (Buffer) encoded ).flip();

        assertThat( toArray( encoded ) )
            .isEqualTo( expectedFrame.toByteArray() );

        out = Stream.newStream();
        encoder = new EventChannelEncoder( newBufferedChannel( out ) );
        encoded = encoder.encode( BOOTERCODE_TEST_ERROR, reportEntry, true );
        expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":test-error:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001".getBytes() );
        expectedFrame.write( ':' );
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
        ( (Buffer) encoded ).flip();

        assertThat( toArray( encoded ) )
            .isEqualTo( expectedFrame.toByteArray() );

        out = Stream.newStream();
        encoder = new EventChannelEncoder( newBufferedChannel( out ) );
        encoder.testSetStarting( reportEntry, true );
        expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 16 );
        expectedFrame.write( ":testset-starting:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001".getBytes() );
        expectedFrame.write( ':' );
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
        encoder = new EventChannelEncoder( newBufferedChannel( out ) );

        encoder.testSetStarting( reportEntry, false );
        expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 16 );
        expectedFrame.write( ":testset-starting:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001".getBytes() );
        expectedFrame.write( ':' );
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

        Map<String, String> props = systemProps();

        TestSetReportEntry reportEntry = mock( TestSetReportEntry.class );
        when( reportEntry.getRunMode() ).thenReturn( NORMAL_RUN );
        when( reportEntry.getTestRunId() ).thenReturn( 1L );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );
        when( reportEntry.getSystemProperties() ).thenReturn( props );

        Stream out = Stream.newStream();
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

        encoder.testSetCompleted( reportEntry, false );

        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        for ( Entry<String, String> entry : props.entrySet() )
        {
            expectedFrame.write( ":maven-surefire-event:".getBytes() );
            expectedFrame.write( 8 );
            expectedFrame.write( ":sys-prop:".getBytes() );
            expectedFrame.write( 10 );
            expectedFrame.write( ":normal-run:".getBytes() );
            expectedFrame.write( "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001".getBytes() );
            expectedFrame.write( ":".getBytes() );
            expectedFrame.write( 5 );
            expectedFrame.write( ":UTF-8:".getBytes() );
            int[] k = toBytes( entry.getKey().length() );
            expectedFrame.write( k[0] );
            expectedFrame.write( k[1] );
            expectedFrame.write( k[2] );
            expectedFrame.write( k[3] );
            expectedFrame.write( ':' );
            expectedFrame.write( entry.getKey().getBytes( UTF_8 ) );
            expectedFrame.write( ':' );
            int[] v = toBytes( entry.getValue() == null ? 1 : entry.getValue().getBytes( UTF_8 ).length );
            expectedFrame.write( v[0] );
            expectedFrame.write( v[1] );
            expectedFrame.write( v[2] );
            expectedFrame.write( v[3] );
            expectedFrame.write( ':' );
            expectedFrame.write( ( entry.getValue() == null ? "\u0000" : entry.getValue() ).getBytes( UTF_8 ) );
            expectedFrame.write( ':' );
        }

        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 17 );
        expectedFrame.write( ":testset-completed:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001".getBytes() );
        expectedFrame.write( ':' );
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
        when( reportEntry.getRunMode() ).thenReturn( NORMAL_RUN );
        when( reportEntry.getTestRunId() ).thenReturn( 1L );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        Stream out = Stream.newStream();
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

        encoder.testStarting( reportEntry, true );

        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 13 );
        expectedFrame.write( ":test-starting:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001".getBytes() );
        expectedFrame.write( ':' );
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
        when( reportEntry.getRunMode() ).thenReturn( NORMAL_RUN );
        when( reportEntry.getTestRunId() ).thenReturn( 1L );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        Stream out = Stream.newStream();
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

        encoder.testSucceeded( reportEntry, true );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 14 );
        expectedFrame.write( ":test-succeeded:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001".getBytes() );
        expectedFrame.write( ':' );
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
        when( reportEntry.getRunMode() ).thenReturn( NORMAL_RUN );
        when( reportEntry.getTestRunId() ).thenReturn( 1L );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        Stream out = Stream.newStream();
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

        encoder.testFailed( reportEntry, false );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 11 );
        expectedFrame.write( ":test-failed:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001".getBytes() );
        expectedFrame.write( ':' );
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
        when( reportEntry.getRunMode() ).thenReturn( NORMAL_RUN );
        when( reportEntry.getTestRunId() ).thenReturn( 1L );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        Stream out = Stream.newStream();
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

        encoder.testSkipped( reportEntry, false );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 12 );
        expectedFrame.write( ":test-skipped:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001".getBytes() );
        expectedFrame.write( ':' );
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
        when( reportEntry.getRunMode() ).thenReturn( NORMAL_RUN );
        when( reportEntry.getTestRunId() ).thenReturn( 1L );
        when( reportEntry.getElapsed() ).thenReturn( ELAPSED_TIME );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        Stream out = Stream.newStream();
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );
        encoder.testError( reportEntry, false );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":test-error:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001".getBytes() );
        expectedFrame.write( ':' );
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
        when( reportEntry.getRunMode() ).thenReturn( NORMAL_RUN );
        when( reportEntry.getTestRunId() ).thenReturn( 1L );
        when( reportEntry.getElapsed() ).thenReturn( null );
        when( reportEntry.getGroup() ).thenReturn( "this group" );
        when( reportEntry.getMessage() ).thenReturn( "skipped test" );
        when( reportEntry.getName() ).thenReturn( "my test" );
        when( reportEntry.getNameWithGroup() ).thenReturn( "name with group" );
        when( reportEntry.getSourceName() ).thenReturn( "pkg.MyTest" );
        when( reportEntry.getStackTraceWriter() ).thenReturn( stackTraceWriter );

        Stream out = Stream.newStream();
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

        encoder.testAssumptionFailure( reportEntry, false );
        ByteArrayOutputStream expectedFrame = new ByteArrayOutputStream();
        expectedFrame.write( ":maven-surefire-event:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 23 );
        expectedFrame.write( ":test-assumption-failure:".getBytes( UTF_8 ) );
        expectedFrame.write( (byte) 10 );
        expectedFrame.write( ":normal-run:".getBytes( UTF_8 ) );
        expectedFrame.write( "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001".getBytes() );
        expectedFrame.write( ':' );
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
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

        encoder.bye();

        String encoded = new String( out.toByteArray(), UTF_8 );

        assertThat( encoded )
                .isEqualTo( ":maven-surefire-event:\u0003:bye:" );
    }

    @Test
    public void testStopOnNextTest()
    {
        Stream out = Stream.newStream();
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

        encoder.stopOnNextTest();

        String encoded = new String( out.toByteArray(), UTF_8 );
        assertThat( encoded )
                .isEqualTo( ":maven-surefire-event:\u0011:stop-on-next-test:" );
    }

    @Test
    public void testAcquireNextTest()
    {
        Stream out = Stream.newStream();
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

        encoder.acquireNextTest();

        String encoded = new String( out.toByteArray(), UTF_8 );
        assertThat( encoded )
                .isEqualTo( ":maven-surefire-event:\u0009:next-test:" );
    }

    @Test
    public void testSendOpcode()
    {
        Channel channel = new Channel();
        new EventChannelEncoder( channel )
            .testOutput( new TestOutputReportEntry( stdOut( "msg" ), NORMAL_RUN, 1L )  );
        assertThat( toString( channel.src ) )
                .isEqualTo( ":maven-surefire-event:" + (char) 14 + ":std-out-stream:" + (char) 10 + ":normal-run:"
                    + "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001:"
                    + (char) 5 + ":UTF-8:\u0000\u0000\u0000\u0003:msg:" );

        channel = new Channel();
        new EventChannelEncoder( channel )
            .testOutput( new TestOutputReportEntry( stdErr( null ), NORMAL_RUN, 1L ) );
        assertThat( toString( channel.src ) )
                .isEqualTo( ":maven-surefire-event:" + (char) 14 + ":std-err-stream:" + (char) 10 + ":normal-run:"
                    + "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001:"
                    + (char) 5 + ":UTF-8:\u0000\u0000\u0000\u0001:\u0000:" );

        ByteBuffer result = new EventChannelEncoder( new Channel() )
            .encodeMessage( BOOTERCODE_TEST_ERROR, NORMAL_RUN, 1L, "msg" );
        assertThat( toString( result ) )
            .isEqualTo( ":maven-surefire-event:" + (char) 10 + ":test-error:" + (char) 10 + ":normal-run:"
                + "\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001:"
                + (char) 5 + ":UTF-8:\u0000\u0000\u0000\u0003:msg:" );
    }

    @Test
    public void testConsoleInfo()
    {
        Stream out = Stream.newStream();
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

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
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

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
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

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
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

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
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

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
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

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
        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );

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
        EventChannelEncoder encoder = new EventChannelEncoder( channel );

        encoder.testOutput( new TestOutputReportEntry( stdOut( "msg" ), NORMAL_RUN, 1L ) );
        channel.close();

        String expected = ":maven-surefire-event:\u000e:std-out-stream:"
            + (char) 10 + ":normal-run:\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001:"
            + "\u0005:UTF-8:\u0000\u0000\u0000\u0003:msg:";

        assertThat( new String( out.toByteArray(), UTF_8 ) )
                .isEqualTo( expected );
    }

    @Test
    public void testStdOutStreamLn() throws IOException
    {
        Stream out = Stream.newStream();
        WritableBufferedByteChannel channel = newBufferedChannel( out );
        EventChannelEncoder encoder = new EventChannelEncoder( channel );

        encoder.testOutput( new TestOutputReportEntry( stdOutln( "msg" ), NORMAL_RUN, 1L ) );
        channel.close();

        String expected = ":maven-surefire-event:\u0017:std-out-stream-new-line:"
            + (char) 10 + ":normal-run:\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001:"
            + "\u0005:UTF-8:\u0000\u0000\u0000\u0003:msg:";

        assertThat( new String( out.toByteArray(), UTF_8 ) )
                .isEqualTo( expected );
    }

    @Test
    public void testStdErrStream() throws IOException
    {
        Stream out = Stream.newStream();
        WritableBufferedByteChannel channel = newBufferedChannel( out );
        EventChannelEncoder encoder = new EventChannelEncoder( channel );

        encoder.testOutput( new TestOutputReportEntry( stdErr( "msg" ), NORMAL_RUN, 1L ) );
        channel.close();

        String expected = ":maven-surefire-event:\u000e:std-err-stream:"
            + (char) 10 + ":normal-run:\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001:"
            + "\u0005:UTF-8:\u0000\u0000\u0000\u0003:msg:";

        assertThat( new String( out.toByteArray(), UTF_8 ) )
                .isEqualTo( expected );
    }

    @Test
    public void testStdErrStreamLn() throws IOException
    {
        Stream out = Stream.newStream();
        WritableBufferedByteChannel channel = newBufferedChannel( out );
        EventChannelEncoder encoder = new EventChannelEncoder( channel );

        encoder.testOutput( new TestOutputReportEntry( stdErrln( "msg" ), NORMAL_RUN, 1L ) );
        channel.close();

        String expected = ":maven-surefire-event:\u0017:std-err-stream-new-line:"
            + (char) 10 + ":normal-run:\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0001:"
            + "\u0005:UTF-8:\u0000\u0000\u0000\u0003:msg:";

        assertThat( new String( out.toByteArray(), UTF_8 ) )
                .isEqualTo( expected );
    }

    @Test
    public void testStdErrStreamEmptyMessageNullTestId() throws IOException
    {
        Stream out = Stream.newStream();
        WritableBufferedByteChannel channel = newBufferedChannel( out );
        EventChannelEncoder encoder = new EventChannelEncoder( channel );

        // This used to produce a BufferOverflowException; see SUREFIRE-2056.
        // In essence, we used to under-allocate for the encoding of a null test ID
        // (we used to allocate 0 byte instead of 1 byte).
        // The message needs to be empty in order to reproduce the bug,
        // otherwise we over-allocate for the test message
        // (for safety, due to unpredictability of the size of encoded text)
        // and this over-allocation ends up compensating the under-allocation for the null test id.
        encoder.testOutput( new TestOutputReportEntry( stdErr( "" ), NORMAL_RUN, null ) );
        channel.close();

        String expected = ":maven-surefire-event:\u000e:std-err-stream:"
            + (char) 10 + ":normal-run:\u0000:"
            + "\u0005:UTF-8:\u0000\u0000\u0000\u0000::";

        assertThat( new String( out.toByteArray(), UTF_8 ) )
            .isEqualTo( expected );
    }


    @Test
    @SuppressWarnings( "checkstyle:innerassignment" )
    public void shouldCountSameNumberOfSystemProperties() throws IOException
    {
        Stream stream = Stream.newStream();
        WritableBufferedByteChannel channel = newBufferedChannel( stream );
        EventChannelEncoder encoder = new EventChannelEncoder( channel );

        Map<String, String> sysProps = ObjectUtils.systemProps();
        encoder.encodeSystemProperties( sysProps, NORMAL_RUN, 1L );
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

        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );
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

        EventChannelEncoder encoder = new EventChannelEncoder( newBufferedChannel( out ) );
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
        EventChannelEncoder encoder = new EventChannelEncoder( channel );

        Thread.currentThread().interrupt();
        try
        {
            encoder.testOutput( new TestOutputReportEntry( stdOut( "msg" ), NORMAL_RUN, 2L )  );
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
                    + (char) 10 + ":normal-run:\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0002"
                    + ":\u0005:UTF-8:\u0000\u0000\u0000\u0003:msg:" );
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
        ( (Buffer) frame ).flip();
        os.write( frame.array(), frame.arrayOffset() + ( (Buffer) frame ).position(), frame.remaining() );
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
        public void writeBuffered( ByteBuffer src )
        {
            this.src = src;
        }

        @Override
        public long countBufferOverflows()
        {
            return 0;
        }

        @Override
        public int write( ByteBuffer src )
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
