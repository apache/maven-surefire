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
import org.apache.maven.surefire.api.util.internal.WritableBufferedByteChannel;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Math.ceil;
import static java.nio.CharBuffer.wrap;
import static java.util.Objects.requireNonNull;
import static org.apache.maven.surefire.api.booter.Constants.DEFAULT_STREAM_ENCODING;
import static org.apache.maven.surefire.api.booter.Constants.DEFAULT_STREAM_ENCODING_BYTES;
import static org.apache.maven.surefire.api.booter.Constants.MAGIC_NUMBER_BYTES;
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

/**
 * magic number : opcode : run mode [: opcode specific data]*
 * <br>
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
@SuppressWarnings( "checkstyle:linelength" )
public class LegacyMasterProcessChannelEncoder implements MasterProcessChannelEncoder
{
    private static final byte[] INT_BINARY = new byte[] {0, 0, 0, 0};
    private static final byte BOOLEAN_NON_NULL_OBJECT = (byte) 0xff;
    private static final byte BOOLEAN_NULL_OBJECT = (byte) 0;

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
        encodeAndPrintEvent( ByteBuffer.wrap( new byte[] {'\n'} ), true );
    }

    @Override
    public void sendSystemProperties( Map<String, String> sysProps )
    {
        CharsetEncoder encoder = DEFAULT_STREAM_ENCODING.newEncoder();
        ByteBuffer result = null;
        for ( Iterator<Entry<String, String>> it = sysProps.entrySet().iterator(); it.hasNext(); )
        {
            Entry<String, String> entry = it.next();
            String key = entry.getKey();
            String value = entry.getValue();

            int bufferLength = estimateBufferLength( BOOTERCODE_SYSPROPS, runMode, encoder, 0, key, value );
            result = result != null && result.capacity() >= bufferLength ? result : ByteBuffer.allocate( bufferLength );
            result.clear();
            // :maven-surefire-event:sys-prop:rerun-test-after-failure:UTF-8:0000000000:<key>:0000000000:<value>:
            encode( encoder, result, BOOTERCODE_SYSPROPS, runMode, key, value );
            boolean sendImmediately = !it.hasNext();
            encodeAndPrintEvent( result, sendImmediately );
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
        setOutErr( event, msg );
    }

    @Override
    public void stdErr( String msg, boolean newLine )
    {
        ForkedProcessEventType event = newLine ? BOOTERCODE_STDERR_NEW_LINE : BOOTERCODE_STDERR;
        setOutErr( event, msg );
    }

    private void setOutErr( ForkedProcessEventType eventType, String message )
    {
        ByteBuffer result = encodeMessage( eventType, runMode, message );
        encodeAndPrintEvent( result, false );
    }

    @Override
    public void consoleInfoLog( String message )
    {
        ByteBuffer result = encodeMessage( BOOTERCODE_CONSOLE_INFO, null, message );
        encodeAndPrintEvent( result, true );
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
        CharsetEncoder encoder = DEFAULT_STREAM_ENCODING.newEncoder();
        String stackTrace = t == null ? null : ConsoleLoggerUtils.toString( t );
        int bufferMaxLength = estimateBufferLength( BOOTERCODE_CONSOLE_ERROR, null, encoder, 0, message, stackTrace );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );
        encodeHeader( encoder, result, BOOTERCODE_CONSOLE_ERROR, null );
        encode( encoder, result, message, null, stackTrace );
        encodeAndPrintEvent( result, true );
    }

    @Override
    public void consoleErrorLog( StackTraceWriter stackTraceWriter, boolean trimStackTraces )
    {
        error( stackTraceWriter, trimStackTraces, BOOTERCODE_CONSOLE_ERROR, true );
    }

    @Override
    public void consoleDebugLog( String message )
    {
        ByteBuffer result = encodeMessage( BOOTERCODE_CONSOLE_DEBUG, null, message );
        encodeAndPrintEvent( result, true );
    }

    @Override
    public void consoleWarningLog( String message )
    {
        ByteBuffer result = encodeMessage( BOOTERCODE_CONSOLE_WARNING, null, message );
        encodeAndPrintEvent( result, true );
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
                        @SuppressWarnings( "SameParameterValue" ) boolean sendImmediately )
    {
        CharsetEncoder encoder = DEFAULT_STREAM_ENCODING.newEncoder();
        StackTrace stackTraceWrapper = new StackTrace( stackTraceWriter, trimStackTraces );
        int bufferMaxLength = estimateBufferLength( eventType, null, encoder, 0,
            stackTraceWrapper.message, stackTraceWrapper.smartTrimmedStackTrace, stackTraceWrapper.stackTrace );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );

        encodeHeader( encoder, result, eventType, null );
        encode( encoder, result, stackTraceWrapper );
        encodeAndPrintEvent( result, sendImmediately );
    }

    /**
     * :maven-surefire-event:testset-starting:rerun-test-after-failure:UTF-8:0000000000:SourceName:0000000000:SourceText:0000000000:Name:0000000000:NameText:0000000000:Group:0000000000:Message:0000000000:ElapsedTime:0000000000:LocalizedMessage:0000000000:SmartTrimmedStackTrace:0000000000:toStackTrace( stw, trimStackTraces ):0000000000:
     *
     */
    private void encode( ForkedProcessEventType operation, RunMode runMode, ReportEntry reportEntry,
                         boolean trimStackTraces, @SuppressWarnings( "SameParameterValue" ) boolean sendImmediately )
    {
        ByteBuffer result = encode( operation, runMode, reportEntry, trimStackTraces );
        encodeAndPrintEvent( result, sendImmediately );
    }

    private void encodeOpcode( ForkedProcessEventType eventType, boolean sendImmediately )
    {
        int bufferMaxLength = estimateBufferLength( eventType, null, null, 0 );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );
        encodeOpcode( result, eventType, null );
        encodeAndPrintEvent( result, sendImmediately );
    }

    private void encodeAndPrintEvent( ByteBuffer frame, boolean sendImmediately )
    {
        final boolean wasInterrupted = Thread.interrupted();
        try
        {
            if ( sendImmediately )
            {
                out.write( frame );
            }
            else
            {
                out.writeBuffered( frame );
            }
        }
        catch ( ClosedChannelException e )
        {
            if ( !onExit )
            {
                String event = new String( frame.array(), frame.arrayOffset() + frame.position(), frame.remaining(),
                    DEFAULT_STREAM_ENCODING );

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

    static void encode( CharsetEncoder encoder, ByteBuffer result,
                        ForkedProcessEventType operation, RunMode runMode, String... messages )
    {
        encodeHeader( encoder, result, operation, runMode );
        for ( String message : messages )
        {
            encodeString( encoder, result, message );
        }
    }

    static void encode( CharsetEncoder encoder, ByteBuffer result, StackTrace stw )
    {
        encode( encoder, result, stw.message, stw.smartTrimmedStackTrace, stw.stackTrace );
    }

    private static void encode( CharsetEncoder encoder, ByteBuffer result,
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
    static ByteBuffer encode( ForkedProcessEventType operation, RunMode runMode, ReportEntry reportEntry,
                              boolean trimStackTraces )
    {
        StackTrace stackTraceWrapper = new StackTrace( reportEntry.getStackTraceWriter(), trimStackTraces );

        CharsetEncoder encoder = DEFAULT_STREAM_ENCODING.newEncoder();

        int bufferMaxLength = estimateBufferLength( operation, runMode, encoder, 1, reportEntry.getSourceName(),
            reportEntry.getSourceText(), reportEntry.getName(), reportEntry.getNameText(), reportEntry.getGroup(),
            reportEntry.getMessage(), stackTraceWrapper.message, stackTraceWrapper.smartTrimmedStackTrace,
            stackTraceWrapper.stackTrace );

        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );

        encodeHeader( encoder, result, operation, runMode );

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

    static ByteBuffer encodeMessage( ForkedProcessEventType eventType, RunMode runMode, String message )
    {
        CharsetEncoder encoder = DEFAULT_STREAM_ENCODING.newEncoder();
        int bufferMaxLength = estimateBufferLength( eventType, runMode, encoder, 0, message );
        ByteBuffer result = ByteBuffer.allocate( bufferMaxLength );
        encodeHeader( encoder, result, eventType, runMode );
        encodeString( encoder, result, message );
        return result;
    }

    private static void encodeString( CharsetEncoder encoder, ByteBuffer result, String string )
    {
        String nonNullString = nonNull( string );

        int counterPosition = result.position();

        result.put( INT_BINARY ).put( (byte) ':' );

        int msgStart = result.position();
        encoder.encode( wrap( nonNullString ), result, true );
        int msgEnd = result.position();
        int encodedMsgSize = msgEnd - msgStart;
        result.putInt( counterPosition, encodedMsgSize );

        result.position( msgEnd );

        result.put( (byte) ':' );
    }

    private static void encodeInteger( ByteBuffer result, Integer i )
    {
        if ( i == null )
        {
            result.put( BOOLEAN_NULL_OBJECT );
        }
        else
        {
            result.put( BOOLEAN_NON_NULL_OBJECT ).putInt( i );
        }
        result.put( (byte) ':' );
    }

    static void encodeHeader( CharsetEncoder encoder, ByteBuffer result, ForkedProcessEventType operation,
                              RunMode runMode )
    {
        encodeOpcode( result, operation, runMode );
        String charsetName = encoder.charset().name();
        result.put( (byte) charsetName.length() );
        result.put( (byte) ':' );
        result.put( DEFAULT_STREAM_ENCODING_BYTES );
        result.put( (byte) ':' );
    }

    /**
     * Used in {@link #bye()}, {@link #stopOnNextTest()} and {@link #encodeOpcode(ForkedProcessEventType, boolean)}
     * and private methods extending the buffer.
     *
     * @param operation opcode
     * @param runMode   run mode
     */
    static void encodeOpcode( ByteBuffer result, ForkedProcessEventType operation, RunMode runMode )
    {
        result.put( (byte) ':' );
        result.put( MAGIC_NUMBER_BYTES );
        result.put( (byte) ':' );
        byte[] opcode = operation.getOpcodeBinary();
        result.put( (byte) opcode.length );
        result.put( (byte) ':' );
        result.put( opcode );
        result.put( (byte) ':' );

        if ( runMode != null )
        {
            byte[] runmode = runMode.getRunmodeBinary();
            result.put( (byte) runmode.length );
            result.put( (byte) ':' );
            result.put( runmode );
            result.put( (byte) ':' );
        }
    }

    private static String toStackTrace( StackTraceWriter stw, boolean trimStackTraces )
    {
        if ( stw == null )
        {
            return null;
        }

        return trimStackTraces ? stw.writeTrimmedTraceToString() : stw.writeTraceToString();
    }

    static String nonNull( String msg )
    {
        return msg == null ? "\u0000" : msg;
    }

    static int estimateBufferLength( ForkedProcessEventType eventType, RunMode runMode, CharsetEncoder encoder,
                                     int integersCounter, String... strings )
    {
        assert !( encoder == null && strings.length != 0 );

        // one delimiter character ':' + <string> + one delimiter character ':' +
        // one byte + one delimiter character ':' + <string> + one delimiter character ':'
        int lengthOfMetadata = 1 + MAGIC_NUMBER_BYTES.length + 1 + 1 + 1 + eventType.getOpcode().length() + 1;

        if ( runMode != null )
        {
            // one byte of length + one delimiter character ':' + <string> + one delimiter character ':'
            lengthOfMetadata += 1 + 1 + runMode.geRunmode().length() + 1;
        }

        if ( encoder != null )
        {
            // one byte of length + one delimiter character ':' + <string> + one delimiter character ':'
            lengthOfMetadata += 1 + 1 + encoder.charset().name().length() + 1;
        }

        // one byte (0x00 if NULL) + 4 bytes for integer + one delimiter character ':'
        int lengthOfData = ( 1 + 4 + 1 ) * integersCounter;

        for ( String string : strings )
        {
            String s = string == null ? "\u0000" : string;
            // 4 bytes of string length + one delimiter character ':' + <string> + one delimiter character ':'
            lengthOfData += 4 + 1 + (int) ceil( encoder.maxBytesPerChar() * s.length() ) + 1;
        }


        return lengthOfMetadata + lengthOfData;
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
