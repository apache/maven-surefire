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
import org.apache.maven.surefire.booter.DumpErrorSingleton;
import org.apache.maven.surefire.booter.ForkedProcessEventType;
import org.apache.maven.surefire.booter.MasterProcessChannelEncoder;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunMode;
import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.shared.codec.binary.Base64;
import org.apache.maven.surefire.util.internal.WritableBufferedByteChannel;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_BYE;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_DEBUG;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_ERROR;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_INFO;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_CONSOLE_WARNING;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_JVM_EXIT_ERROR;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_NEXT_TEST;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_STDERR;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_STDERR_NEW_LINE;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_STDOUT;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_STDOUT_NEW_LINE;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_STOP_ON_NEXT_TEST;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_SYSPROPS;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_TESTSET_COMPLETED;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_TESTSET_STARTING;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_TEST_ASSUMPTIONFAILURE;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_TEST_ERROR;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_TEST_FAILED;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_TEST_SKIPPED;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_TEST_STARTING;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.BOOTERCODE_TEST_SUCCEEDED;
import static org.apache.maven.surefire.booter.ForkedProcessEventType.MAGIC_NUMBER;
import static org.apache.maven.surefire.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.report.RunMode.RERUN_TEST_AFTER_FAILURE;

/**
 * magic number : opcode : run mode [: opcode specific data]*
 * <br>
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public class LegacyMasterProcessChannelEncoder implements MasterProcessChannelEncoder
{
    private static final Base64 BASE64 = new Base64();
    private static final Charset STREAM_ENCODING = US_ASCII;
    private static final Charset STRING_ENCODING = UTF_8;

    private final WritableBufferedByteChannel out;
    private final RunMode runMode;
    private final AtomicBoolean trouble = new AtomicBoolean();
    private volatile boolean onExit;

    public LegacyMasterProcessChannelEncoder( @Nonnull WritableBufferedByteChannel out )
    {
        this( out, NORMAL_RUN );
    }

    protected LegacyMasterProcessChannelEncoder( @Nonnull WritableBufferedByteChannel out, @Nonnull RunMode runMode )
    {
        this.out = requireNonNull( out );
        this.runMode = requireNonNull( runMode );
    }

    @Override
    public MasterProcessChannelEncoder asRerunMode() // todo apply this and rework providers
    {
        return new LegacyMasterProcessChannelEncoder( out, RERUN_TEST_AFTER_FAILURE );
    }

    @Override
    public MasterProcessChannelEncoder asNormalMode()
    {
        return new LegacyMasterProcessChannelEncoder( out, NORMAL_RUN );
    }

    @Override
    public boolean checkError()
    {
        return trouble.get();
    }

    @Override
    public void onJvmExit()
    {
        onExit = true;
        encodeAndPrintEvent( new StringBuilder( "\n" ), true );
    }

    @Override
    public void sendSystemProperties( Map<String, String> sysProps )
    {
        for ( Entry<String, String> entry : sysProps.entrySet() )
        {
            String key = entry.getKey();
            String value = entry.getValue();
            StringBuilder event = encode( BOOTERCODE_SYSPROPS, runMode, key, value );
            encodeAndPrintEvent( event, false );
        }
    }

    @Override
    public void testSetStarting( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TESTSET_STARTING, runMode, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testSetCompleted( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TESTSET_COMPLETED, runMode, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testStarting( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_STARTING, runMode, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testSucceeded( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_SUCCEEDED, runMode, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testFailed( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_FAILED, runMode, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testSkipped( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_SKIPPED, runMode, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testError( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_ERROR, runMode, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testAssumptionFailure( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_ASSUMPTIONFAILURE, runMode, reportEntry, trimStackTraces, true );
    }

    @Override
    public void stdOut( String msg, boolean newLine )
    {
        ForkedProcessEventType event = newLine ? BOOTERCODE_STDOUT_NEW_LINE : BOOTERCODE_STDOUT;
        setOutErr( event.getOpcode(), msg );
    }

    @Override
    public void stdErr( String msg, boolean newLine )
    {
        ForkedProcessEventType event = newLine ? BOOTERCODE_STDERR_NEW_LINE : BOOTERCODE_STDERR;
        setOutErr( event.getOpcode(), msg );
    }

    private void setOutErr( String eventType, String message )
    {
        String base64Message = toBase64( message );
        StringBuilder event = encodeMessage( eventType, runMode.geRunName(), base64Message );
        encodeAndPrintEvent( event, false );
    }

    @Override
    public void consoleInfoLog( String msg )
    {
        StringBuilder event = print( BOOTERCODE_CONSOLE_INFO.getOpcode(), msg );
        encodeAndPrintEvent( event, true );
    }

    @Override
    public void consoleErrorLog( String msg )
    {
        StringBuilder encoded = encodeHeader( BOOTERCODE_CONSOLE_ERROR.getOpcode(), null );
        encode( encoded, msg, null, null );
        encodeAndPrintEvent( encoded, true );
    }

    @Override
    public void consoleErrorLog( Throwable t )
    {
        consoleErrorLog( t.getLocalizedMessage(), t );
    }

    @Override
    public void consoleErrorLog( String msg, Throwable t )
    {
        StringBuilder encoded = encodeHeader( BOOTERCODE_CONSOLE_ERROR.getOpcode(), null );
        encode( encoded, msg, null, ConsoleLoggerUtils.toString( t ) );
        encodeAndPrintEvent( encoded, true );
    }

    @Override
    public void consoleErrorLog( StackTraceWriter stackTraceWriter, boolean trimStackTraces )
    {
        error( stackTraceWriter, trimStackTraces, BOOTERCODE_CONSOLE_ERROR, true );
    }

    @Override
    public void consoleDebugLog( String msg )
    {
        StringBuilder event = print( BOOTERCODE_CONSOLE_DEBUG.getOpcode(), msg );
        encodeAndPrintEvent( event, true );
    }

    @Override
    public void consoleWarningLog( String msg )
    {
        StringBuilder event = print( BOOTERCODE_CONSOLE_WARNING.getOpcode(), msg );
        encodeAndPrintEvent( event, true );
    }

    @Override
    public void bye()
    {
        encodeOpcode( BOOTERCODE_BYE, true );
    }

    @Override
    public void stopOnNextTest()
    {
        encodeOpcode( BOOTERCODE_STOP_ON_NEXT_TEST, true );
    }

    @Override
    public void acquireNextTest()
    {
        encodeOpcode( BOOTERCODE_NEXT_TEST, true );
    }

    @Override
    public void sendExitError( StackTraceWriter stackTraceWriter, boolean trimStackTraces )
    {
        error( stackTraceWriter, trimStackTraces, BOOTERCODE_JVM_EXIT_ERROR, true );
    }

    private void error( StackTraceWriter stackTraceWriter, boolean trimStackTraces, ForkedProcessEventType event,
                        @SuppressWarnings( "SameParameterValue" ) boolean sendImmediately )
    {
        StringBuilder encoded = encodeHeader( event.getOpcode(), null );
        encode( encoded, stackTraceWriter, trimStackTraces );
        encodeAndPrintEvent( encoded, sendImmediately );
    }

    private void encode( ForkedProcessEventType operation, RunMode runMode, ReportEntry reportEntry,
                         boolean trimStackTraces, @SuppressWarnings( "SameParameterValue" ) boolean sendImmediately )
    {
        StringBuilder event = encode( operation.getOpcode(), runMode.geRunName(), reportEntry, trimStackTraces );
        encodeAndPrintEvent( event, sendImmediately );
    }

    private void encodeOpcode( ForkedProcessEventType operation, boolean sendImmediately )
    {
        StringBuilder event = encodeOpcode( operation.getOpcode(), null );
        encodeAndPrintEvent( event, sendImmediately );
    }

    private void encodeAndPrintEvent( StringBuilder event, boolean sendImmediately )
    {
        try
        {
            //noinspection ResultOfMethodCallIgnored
            Thread.interrupted();

            byte[] array = event.append( '\n' )
                .toString()
                .getBytes( STREAM_ENCODING );

            ByteBuffer bb = ByteBuffer.wrap( array );

            if ( sendImmediately )
            {
                out.write( bb );
            }
            else
            {
                out.writeBuffered( bb );
            }
        }
        catch ( ClosedChannelException e )
        {
            if ( !onExit )
            {
                DumpErrorSingleton.getSingleton()
                    .dumpException( e, "Channel closed while writing the event '" + event + "'." );
            }
        }
        catch ( IOException e )
        {
            if ( trouble.compareAndSet( false, true ) )
            {
                DumpErrorSingleton.getSingleton()
                    .dumpException( e );
            }
        }
    }

    static StringBuilder encode( ForkedProcessEventType operation, RunMode runMode, String... args )
    {
        StringBuilder encodedTo = encodeHeader( operation.getOpcode(), runMode.geRunName() );

        for ( int i = 0; i < args.length; )
        {
            String arg = args[i++];
            encodedTo.append( toBase64( arg ) )
                .append( ':' );
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
        encoded.append( toBase64( message ) )
            .append( ':' )
            .append( toBase64( smartStackTrace ) )
            .append( ':' )
            .append( toBase64( stackTrace ) )
            .append( ':' );
    }

    /**
     * Used operations:<br>
     * <ul>
     * <li>{@link ForkedProcessEventType#BOOTERCODE_TESTSET_STARTING},</li>
     * <li>{@link ForkedProcessEventType#BOOTERCODE_TESTSET_COMPLETED},</li>
     * <li>{@link ForkedProcessEventType#BOOTERCODE_TEST_STARTING},</li>
     * <li>{@link ForkedProcessEventType#BOOTERCODE_TEST_SUCCEEDED},</li>
     * <li>{@link ForkedProcessEventType#BOOTERCODE_TEST_FAILED},</li>
     * <li>{@link ForkedProcessEventType#BOOTERCODE_TEST_ERROR},</li>
     * <li>{@link ForkedProcessEventType#BOOTERCODE_TEST_SKIPPED},</li>
     * <li>{@link ForkedProcessEventType#BOOTERCODE_TEST_ASSUMPTIONFAILURE}.</li>
     * </ul>
     */
    static StringBuilder encode( String operation, String runMode, ReportEntry reportEntry, boolean trimStackTraces )
    {
        StringBuilder encodedTo = encodeHeader( operation, runMode )
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
                .append( reportEntry.getElapsed() == null ? "-" : reportEntry.getElapsed().toString() )
                .append( ':' );

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
            builder.append( encodedMsg ).append( ':' );

        }
        return builder;
    }

    static StringBuilder encodeHeader( String operation, String runMode )
    {
        return encodeOpcode( operation, runMode )
            .append( STRING_ENCODING.name() )
            .append( ':' );
    }

    /**
     * Used in {@link #bye()}, {@link #stopOnNextTest()} and {@link #encodeOpcode(ForkedProcessEventType, boolean)}
     * and private methods extending the buffer.
     *
     * @param operation opcode
     * @param runMode   run mode
     * @return encoded event
     */
    static StringBuilder encodeOpcode( String operation, String runMode )
    {
        StringBuilder s = new StringBuilder( 128 )
            .append( ':' )
            .append( MAGIC_NUMBER )
            .append( ':' )
            .append( operation )
            .append( ':' );

        return runMode == null ? s : s.append( runMode ).append( ':' );
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
