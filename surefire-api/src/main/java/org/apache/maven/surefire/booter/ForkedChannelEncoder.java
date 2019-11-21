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

import org.apache.maven.surefire.shared.codec.binary.Base64;
import org.apache.maven.plugin.surefire.log.api.ConsoleLoggerUtils;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunMode;
import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.StackTraceWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.MAGIC_NUMBER;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_SYSPROPS;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STDERR;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STDERR_NEW_LINE;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STDOUT;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STDOUT_NEW_LINE;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_BYE;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_CONSOLE_ERROR;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_CONSOLE_DEBUG;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_CONSOLE_INFO;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_CONSOLE_WARNING;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_NEXT_TEST;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STOP_ON_NEXT_TEST;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_ASSUMPTIONFAILURE;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_ERROR;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_FAILED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_SKIPPED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_STARTING;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_SUCCEEDED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TESTSET_COMPLETED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TESTSET_STARTING;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_JVM_EXIT_ERROR;
import static org.apache.maven.surefire.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.report.RunMode.RERUN_TEST_AFTER_FAILURE;
import static java.util.Objects.requireNonNull;

/**
 * magic number : opcode : run mode [: opcode specific data]*
 * <br>
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public final class ForkedChannelEncoder
{
    private static final Base64 BASE64 = new Base64();
    private static final Charset STREAM_ENCODING = US_ASCII;
    private static final Charset STRING_ENCODING = UTF_8;

    private final OutputStream out;
    private final RunMode runMode;
    private volatile boolean trouble;

    public ForkedChannelEncoder( OutputStream out )
    {
        this( out, NORMAL_RUN );
    }

    private ForkedChannelEncoder( OutputStream out, RunMode runMode )
    {
        this.out = requireNonNull( out );
        this.runMode = requireNonNull( runMode );
    }

    public ForkedChannelEncoder asRerunMode() // todo apply this and rework providers
    {
        return new ForkedChannelEncoder( out, RERUN_TEST_AFTER_FAILURE );
    }

    public ForkedChannelEncoder asNormalMode()
    {
        return new ForkedChannelEncoder( out, NORMAL_RUN );
    }

    public boolean checkError()
    {
        return trouble;
    }

    public void sendSystemProperties( Map<String, String> sysProps )
    {
        for ( Entry<String, String> entry : sysProps.entrySet() )
        {
            String key = entry.getKey();
            String value = entry.getValue();
            StringBuilder event = encode( BOOTERCODE_SYSPROPS, runMode, key, value );
            encodeAndPrintEvent( event );
        }
    }

    public void testSetStarting( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TESTSET_STARTING, runMode, reportEntry, trimStackTraces );
    }

    public void testSetCompleted( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TESTSET_COMPLETED, runMode, reportEntry, trimStackTraces );
    }

    public void testStarting( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_STARTING, runMode, reportEntry, trimStackTraces );
    }

    public void testSucceeded( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_SUCCEEDED, runMode, reportEntry, trimStackTraces );
    }

    public void testFailed( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_FAILED, runMode, reportEntry, trimStackTraces );
    }

    public void testSkipped( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_SKIPPED, runMode, reportEntry, trimStackTraces );
    }

    public void testError( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_ERROR, runMode, reportEntry, trimStackTraces );
    }

    public void testAssumptionFailure( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_ASSUMPTIONFAILURE, runMode, reportEntry, trimStackTraces );
    }

    public void stdOut( String msg, boolean newLine )
    {
        ForkedProcessEvent event = newLine ? BOOTERCODE_STDOUT_NEW_LINE : BOOTERCODE_STDOUT;
        setOutErr( event.getOpcode(), msg );
    }

    public void stdErr( String msg, boolean newLine )
    {
        ForkedProcessEvent event = newLine ? BOOTERCODE_STDERR_NEW_LINE : BOOTERCODE_STDERR;
        setOutErr( event.getOpcode(), msg );
    }

    private void setOutErr( String eventType, String message )
    {
        String base64Message = toBase64( message );
        StringBuilder event = encodeMessage( eventType, runMode.geRunName(), base64Message );
        encodeAndPrintEvent( event );
    }

    public void consoleInfoLog( String msg )
    {
        StringBuilder event = print( BOOTERCODE_CONSOLE_INFO.getOpcode(), msg );
        encodeAndPrintEvent( event );
    }

    public void consoleErrorLog( String msg )
    {
        StringBuilder event = print( BOOTERCODE_CONSOLE_ERROR.getOpcode(), msg );
        encodeAndPrintEvent( event );
    }

    public void consoleErrorLog( Throwable t )
    {
        consoleErrorLog( t.getLocalizedMessage(), t );
    }

    public void consoleErrorLog( String msg, Throwable t )
    {
        StringBuilder encoded = encodeHeader( BOOTERCODE_CONSOLE_ERROR.getOpcode(), null );
        encode( encoded, msg, null, ConsoleLoggerUtils.toString( t ) );
        encodeAndPrintEvent( encoded );
    }

    public void consoleErrorLog( StackTraceWriter stackTraceWriter, boolean trimStackTraces )
    {
        error( stackTraceWriter, trimStackTraces, BOOTERCODE_CONSOLE_ERROR );
    }

    public void consoleDebugLog( String msg )
    {
        StringBuilder event = print( BOOTERCODE_CONSOLE_DEBUG.getOpcode(), msg );
        encodeAndPrintEvent( event );
    }

    public void consoleWarningLog( String msg )
    {
        StringBuilder event = print( BOOTERCODE_CONSOLE_WARNING.getOpcode(), msg );
        encodeAndPrintEvent( event );
    }

    public void bye()
    {
        encodeOpcode( BOOTERCODE_BYE );
    }

    public void stopOnNextTest()
    {
        encodeOpcode( BOOTERCODE_STOP_ON_NEXT_TEST );
    }

    public void acquireNextTest()
    {
        encodeOpcode( BOOTERCODE_NEXT_TEST );
    }

    public void sendExitEvent( StackTraceWriter stackTraceWriter, boolean trimStackTraces )
    {
        error( stackTraceWriter, trimStackTraces, BOOTERCODE_JVM_EXIT_ERROR );
    }

    private void error( StackTraceWriter stackTraceWriter, boolean trimStackTraces, ForkedProcessEvent event )
    {
        StringBuilder encoded = encodeHeader( event.getOpcode(), null );
        encode( encoded, stackTraceWriter, trimStackTraces );
        encodeAndPrintEvent( encoded );
    }

    private void encode( ForkedProcessEvent operation, RunMode runMode, ReportEntry reportEntry,
                         boolean trimStackTraces )
    {
        StringBuilder event = encode( operation.getOpcode(), runMode.geRunName(), reportEntry, trimStackTraces );
        encodeAndPrintEvent( event );
    }

    private void encodeOpcode( ForkedProcessEvent operation )
    {
        StringBuilder event = encodeOpcode( operation.getOpcode(), null );
        encodeAndPrintEvent( event );
    }

    private void encodeAndPrintEvent( StringBuilder command )
    {
        byte[] array = command.append( '\n' ).toString().getBytes( STREAM_ENCODING );
        synchronized ( out )
        {
            try
            {
                out.write( array );
                out.flush();
            }
            catch ( IOException e )
            {
                DumpErrorSingleton.getSingleton().dumpException( e );
                trouble = true;
            }
        }
    }

    static StringBuilder encode( ForkedProcessEvent operation, RunMode runMode, String... args )
    {
        StringBuilder encodedTo = encodeHeader( operation.getOpcode(), runMode.geRunName() )
                                          .append( ':' );

        for ( int i = 0; i < args.length; )
        {
            String arg = args[i++];
            encodedTo.append( toBase64( arg ) );
            if ( i != args.length )
            {
                encodedTo.append( ':' );
            }
        }
        return encodedTo;
    }

    static void encode( StringBuilder encoded, StackTraceWriter stw, boolean trimStackTraces )
    {
        SafeThrowable throwable = stw == null ? null : stw.getThrowable();
        String message = throwable == null ? null : throwable.getLocalizedMessage();
        String smartStackTrace = stw == null ? null : stw.smartTrimmedStackTrace();
        String stackTrace = stw == null ? null : toStackTrace( stw, trimStackTraces );
        encode( encoded, message, smartStackTrace, stackTrace );
    }

    private static void encode( StringBuilder encoded, String message, String smartStackTrace, String stackTrace )
    {
        encoded.append( ':' )
                .append( toBase64( message ) )
                .append( ':' )
                .append( toBase64( smartStackTrace ) )
                .append( ':' )
                .append( toBase64( stackTrace ) );
    }

    /**
     * Used operations:<br>
     * <ul>
     * <li>{@link ForkedProcessEvent#BOOTERCODE_TESTSET_STARTING},</li>
     * <li>{@link ForkedProcessEvent#BOOTERCODE_TESTSET_COMPLETED},</li>
     * <li>{@link ForkedProcessEvent#BOOTERCODE_TEST_STARTING},</li>
     * <li>{@link ForkedProcessEvent#BOOTERCODE_TEST_SUCCEEDED},</li>
     * <li>{@link ForkedProcessEvent#BOOTERCODE_TEST_FAILED},</li>
     * <li>{@link ForkedProcessEvent#BOOTERCODE_TEST_ERROR},</li>
     * <li>{@link ForkedProcessEvent#BOOTERCODE_TEST_SKIPPED},</li>
     * <li>{@link ForkedProcessEvent#BOOTERCODE_TEST_ASSUMPTIONFAILURE}.</li>
     * </ul>
     */
    static StringBuilder encode( String operation, String runMode, ReportEntry reportEntry,
                                         boolean trimStackTraces )
    {
        StringBuilder encodedTo = encodeHeader( operation, runMode )
                .append( ':' )
                .append( toBase64( reportEntry.getSourceName() ) )
                .append( ':' )
                .append( toBase64( reportEntry.getSourceText() ) )
                .append( ':' )
                .append( toBase64( reportEntry.getName() ) )
                .append( ':' )
                .append( toBase64( reportEntry.getNameText() ) )
                .append( ':' )
                .append( toBase64( reportEntry.getGroup() ) )
                .append( ':' )
                .append( toBase64( reportEntry.getMessage() ) )
                .append( ':' )
                .append( reportEntry.getElapsed() == null ? "-" : reportEntry.getElapsed().toString() );

        encode( encodedTo, reportEntry.getStackTraceWriter(), trimStackTraces );

        return encodedTo;
    }

    /**
     * Used in {@link #consoleInfoLog(String)}, {@link #consoleErrorLog(String)}, {@link #consoleDebugLog(String)},
     * {@link #consoleWarningLog(String)} and private methods extending the buffer.
     */
    StringBuilder print( String operation, String... msgs )
    {
        String[] encodedMsgs = new String[msgs.length];
        for ( int i = 0; i < encodedMsgs.length; i++ )
        {
            String msg = msgs[i];
            encodedMsgs[i] = toBase64( msg );
        }
        return encodeMessage( operation, null, encodedMsgs );
    }

    static StringBuilder encodeMessage( String operation, String runMode, String... encodedMsgs )
    {
        StringBuilder builder = encodeHeader( operation, runMode );
        for ( String encodedMsg : encodedMsgs )
        {
            builder.append( ':' )
                    .append( encodedMsg );

        }
        return builder;
    }

    static StringBuilder encodeHeader( String operation, String runMode )
    {
        return encodeOpcode( operation, runMode )
                       .append( ':' )
                       .append( STRING_ENCODING.name() );
    }

    /**
     * Used in {@link #bye()}, {@link #stopOnNextTest()} and {@link #encodeOpcode(ForkedProcessEvent)}
     * and private methods extending the buffer.
     *
     * @param operation opcode
     * @param runMode   run mode
     * @return encoded command
     */
    static StringBuilder encodeOpcode( String operation, String runMode )
    {
        StringBuilder s = new StringBuilder( 128 )
                .append( MAGIC_NUMBER )
                .append( operation );

        return runMode == null ? s : s.append( ':' ).append( runMode );
    }

    private static String toStackTrace( StackTraceWriter stw, boolean trimStackTraces )
    {
        return trimStackTraces ? stw.writeTrimmedTraceToString() : stw.writeTraceToString();
    }

    static String toBase64( String msg )
    {
        return msg == null ? "-" : new String( BASE64.encode( msg.getBytes( STRING_ENCODING ) ), STREAM_ENCODING );
    }
}
