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
import org.apache.maven.surefire.api.booter.DumpErrorSingleton;
import org.apache.maven.surefire.api.booter.ForkedProcessEventType;
import org.apache.maven.surefire.api.booter.MasterProcessChannelEncoder;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.report.SafeThrowable;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.api.util.internal.WritableBufferedByteChannel;
import org.apache.maven.surefire.booter.stream.EventEncoder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

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

/**
 * magic number : opcode : run mode [: opcode specific data]*
 * <br>
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
@SuppressWarnings( "checkstyle:linelength" )
public class EventChannelEncoder extends EventEncoder implements MasterProcessChannelEncoder
{
    private final AtomicBoolean trouble = new AtomicBoolean();
    private volatile boolean onExit;

    /**
     * The encoder for events.
     *
     * @param out     the channel available for writing the events
     */
    public EventChannelEncoder( @Nonnull WritableBufferedByteChannel out )
    {
        super( out );
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
        write( ByteBuffer.wrap( new byte[] {'\n'} ), true );
    }

    void encodeSystemProperties( Map<String, String> sysProps, RunMode runMode, Long testRunId )
    {
        CharsetEncoder encoder = newCharsetEncoder();
        ByteBuffer result = null;
        for ( Iterator<Entry<String, String>> it = sysProps.entrySet().iterator(); it.hasNext(); )
        {
            Entry<String, String> entry = it.next();
            String key = entry.getKey();
            String value = entry.getValue();

            int bufferLength =
                estimateBufferLength( BOOTERCODE_SYSPROPS.getOpcode().length(), runMode, encoder, 0, 1, key, value );
            result = result != null && result.capacity() >= bufferLength ? result : ByteBuffer.allocate( bufferLength );
            ( (Buffer) result ).clear();
            // :maven-surefire-event:sys-prop:<runMode>:<testRunId>:UTF-8:<integer>:<key>:<integer>:<value>:
            encode( encoder, result, BOOTERCODE_SYSPROPS, runMode, testRunId, key, value );
            boolean sync = !it.hasNext();
            write( result, sync );
        }
    }

    @Override
    public void testSetStarting( TestSetReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TESTSET_STARTING, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testSetCompleted( TestSetReportEntry reportEntry, boolean trimStackTraces )
    {
        encodeSystemProperties( reportEntry.getSystemProperties(), reportEntry.getRunMode(), reportEntry.getTestRunId() );
        encode( BOOTERCODE_TESTSET_COMPLETED, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testStarting( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_STARTING, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testSucceeded( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_SUCCEEDED, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testFailed( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_FAILED, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testSkipped( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_SKIPPED, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testError( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_ERROR, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testAssumptionFailure( ReportEntry reportEntry, boolean trimStackTraces )
    {
        encode( BOOTERCODE_TEST_ASSUMPTIONFAILURE, reportEntry, trimStackTraces, true );
    }

    @Override
    public void testOutput( TestOutputReportEntry reportEntry )
    {
        boolean stdout = reportEntry.isStdOut();
        boolean newLine = reportEntry.isNewLine();
        String msg = reportEntry.getLog();
        ForkedProcessEventType event =
            stdout ? ( newLine ? BOOTERCODE_STDOUT_NEW_LINE : BOOTERCODE_STDOUT )
                : ( newLine ? BOOTERCODE_STDERR_NEW_LINE : BOOTERCODE_STDERR );
        setOutErr( event, reportEntry.getRunMode(), reportEntry.getTestRunId(), msg );
    }

    private void setOutErr( ForkedProcessEventType eventType, RunMode runMode, Long testRunId, String message )
    {
        ByteBuffer result = encodeMessage( eventType, runMode, testRunId, message );
        write( result, false );
    }

    @Override
    public void consoleInfoLog( String message )
    {
        ByteBuffer result = encodeMessage( BOOTERCODE_CONSOLE_INFO, message );
        write( result, true );
    }

    @Override
    public void consoleErrorLog( String message )
    {
        consoleErrorLog( message, null );
    }

    @Override
    public void consoleErrorLog( Throwable t )
    {
        consoleErrorLog( t.getLocalizedMessage(), t );
    }

    @Override
    public void consoleErrorLog( String message, Throwable t )
    {
        CharsetEncoder encoder = newCharsetEncoder();
        String stackTrace = t == null ? null : ConsoleLoggerUtils.toString( t );
        int bufferMaxLength = estimateBufferLength( BOOTERCODE_CONSOLE_ERROR.getOpcode().length(), null, encoder, 0, 0,
            message, stackTrace );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );
        encodeHeader( result, BOOTERCODE_CONSOLE_ERROR );
        encodeCharset( result );
        encode( encoder, result, message, null, stackTrace );
        write( result, true );
    }

    @Override
    public void consoleErrorLog( StackTraceWriter stackTraceWriter, boolean trimStackTraces )
    {
        error( stackTraceWriter, trimStackTraces, BOOTERCODE_CONSOLE_ERROR, true );
    }

    @Override
    public void consoleDebugLog( String message )
    {
        ByteBuffer result = encodeMessage( BOOTERCODE_CONSOLE_DEBUG, message );
        write( result, true );
    }

    @Override
    public void consoleWarningLog( String message )
    {
        ByteBuffer result = encodeMessage( BOOTERCODE_CONSOLE_WARNING, message );
        write( result, true );
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

    private void error( StackTraceWriter stackTraceWriter, boolean trimStackTraces, ForkedProcessEventType eventType,
                        @SuppressWarnings( "SameParameterValue" ) boolean sync )
    {
        CharsetEncoder encoder = newCharsetEncoder();
        StackTrace stackTraceWrapper = new StackTrace( stackTraceWriter, trimStackTraces );
        int bufferMaxLength = estimateBufferLength( eventType.getOpcode().length(), null, encoder, 0, 0,
            stackTraceWrapper.message, stackTraceWrapper.smartTrimmedStackTrace, stackTraceWrapper.stackTrace );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );

        encodeHeader( result, eventType );
        encodeCharset( result );
        encode( encoder, result, stackTraceWrapper );
        write( result, sync );
    }

    // example
    // :maven-surefire-event:testset-starting:rerun-test-after-failure:1:5:UTF-8:<integer>:SourceName:<integer>:SourceText:<integer>:Name:<integer>:NameText:<integer>:Group:<integer>:Message:<integer>:ElapsedTime:<integer>:LocalizedMessage:<integer>:SmartTrimmedStackTrace:<integer>:toStackTrace( stw, trimStackTraces ):<integer>:
    private void encode( ForkedProcessEventType operation, ReportEntry reportEntry,
                         boolean trimStackTraces, @SuppressWarnings( "SameParameterValue" ) boolean sync )
    {
        ByteBuffer result = encode( operation, reportEntry, trimStackTraces );
        write( result, sync );
    }

    private void encodeOpcode( ForkedProcessEventType eventType, boolean sync )
    {
        int bufferMaxLength = estimateBufferLength( eventType.getOpcode().length(), null, null, 0, 0 );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );
        encodeHeader( result, eventType );
        write( result, sync );
    }

    @Override
    protected void write( ByteBuffer frame, boolean sync )
    {
        final boolean wasInterrupted = Thread.interrupted();
        try
        {
            super.write( frame, sync );
        }
        catch ( ClosedChannelException e )
        {
            if ( !onExit )
            {
                String event = new String( frame.array(), frame.arrayOffset() + ( (Buffer) frame ).position(), frame.remaining(),
                    getCharset() );

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
        finally
        {
            if ( wasInterrupted )
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void encode( CharsetEncoder encoder, ByteBuffer result, StackTrace stw )
    {
        encode( encoder, result, stw.message, stw.smartTrimmedStackTrace, stw.stackTrace );
    }

    private void encode( CharsetEncoder encoder, ByteBuffer result,
                         String message, String smartStackTrace, String stackTrace )
    {
        encodeString( encoder, result, message );
        encodeString( encoder, result, smartStackTrace );
        encodeString( encoder, result, stackTrace );
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
    ByteBuffer encode( ForkedProcessEventType operation, ReportEntry reportEntry, boolean trimStackTraces )
    {
        StackTrace stackTraceWrapper = new StackTrace( reportEntry.getStackTraceWriter(), trimStackTraces );

        CharsetEncoder encoder = newCharsetEncoder();

        int bufferMaxLength = estimateBufferLength( operation.getOpcode().length(), reportEntry.getRunMode(), encoder,
            1, 1, reportEntry.getSourceName(), reportEntry.getSourceText(), reportEntry.getName(),
            reportEntry.getNameText(), reportEntry.getGroup(), reportEntry.getMessage(), stackTraceWrapper.message,
            stackTraceWrapper.smartTrimmedStackTrace, stackTraceWrapper.stackTrace );

        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );

        encodeHeader( result, operation, reportEntry.getRunMode(), reportEntry.getTestRunId() );
        encodeCharset( result );

        encodeString( encoder, result, reportEntry.getSourceName() );
        encodeString( encoder, result, reportEntry.getSourceText() );
        encodeString( encoder, result, reportEntry.getName() );
        encodeString( encoder, result, reportEntry.getNameText() );
        encodeString( encoder, result, reportEntry.getGroup() );
        encodeString( encoder, result, reportEntry.getMessage() );
        encodeInteger( result, reportEntry.getElapsed() );

        encode( encoder, result, stackTraceWrapper );

        return result;
    }

    ByteBuffer encodeMessage( ForkedProcessEventType eventType, RunMode runMode, Long testRunId, String message )
    {
        CharsetEncoder encoder = newCharsetEncoder();
        int bufferMaxLength = estimateBufferLength( eventType.getOpcode().length(), runMode, encoder, 0,
            1, message );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );
        encode( encoder, result, eventType, runMode, testRunId, message );
        return result;
    }

    ByteBuffer encodeMessage( ForkedProcessEventType eventType, String message )
    {
        CharsetEncoder encoder = newCharsetEncoder();
        int bufferMaxLength = estimateBufferLength( eventType.getOpcode().length(), null, encoder, 0, 0, message );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );
        encode( encoder, result, eventType, message );
        return result;
    }

    private static String toStackTrace( StackTraceWriter stw, boolean trimStackTraces )
    {
        if ( stw == null )
        {
            return null;
        }

        return trimStackTraces ? stw.writeTrimmedTraceToString() : stw.writeTraceToString();
    }

    private static final class StackTrace
    {
        final String message;
        final String smartTrimmedStackTrace;
        final String stackTrace;

        StackTrace( StackTraceWriter stackTraceWriter, boolean trimStackTraces )
        {
            SafeThrowable throwable = stackTraceWriter == null ? null : stackTraceWriter.getThrowable();
            message = throwable == null ? null : throwable.getLocalizedMessage();
            smartTrimmedStackTrace = stackTraceWriter == null ? null : stackTraceWriter.smartTrimmedStackTrace();
            stackTrace = stackTraceWriter == null ? null : toStackTrace( stackTraceWriter, trimStackTraces );
        }
    }
}
