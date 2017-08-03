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

import org.apache.maven.plugin.surefire.log.api.ConsoleLoggerUtils;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunMode;
import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.StackTraceWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.lang.System.arraycopy;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_BYE;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_CONSOLE_INFO;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_CONSOLE_DEBUG;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_CONSOLE_ERROR;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_NEXT_TEST;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STDERR;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STDOUT;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_STOP_ON_NEXT_TEST;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_SYSPROPS;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TESTSET_COMPLETED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TESTSET_STARTING;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_ASSUMPTIONFAILURE;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_ERROR;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_FAILED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_SKIPPED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_STARTING;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_TEST_SUCCEEDED;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.BOOTERCODE_CONSOLE_WARNING;
import static org.apache.maven.surefire.booter.ForkedProcessEvent.MAGIC_NUMBER;
import static org.apache.maven.surefire.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.report.RunMode.RERUN;
import static org.apache.maven.surefire.util.internal.ObjectUtils.requireNonNull;

/**
 * magic number : opcode : run mode [: opcode specific data]*
 * <p/>
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public final class ForkedChannelEncoder
{
    private final Charset streamCharset;
    private final OutputStream out;
    private final RunMode runMode;
    private volatile boolean trouble;

    public ForkedChannelEncoder( OutputStream out )
    {
        this( US_ASCII, out, NORMAL_RUN );
    }

    /**
     * For testing purposes.
     *
     * @param streamCharset    pipe encoding
     * @param out              pipe
     */
    public ForkedChannelEncoder( Charset streamCharset, OutputStream out )
    {
        this( streamCharset, out, NORMAL_RUN );
    }

    private ForkedChannelEncoder( Charset streamCharset, OutputStream out, RunMode runMode )
    {
        this.streamCharset = requireNonNull( streamCharset );
        this.out = requireNonNull( out );
        this.runMode = requireNonNull( runMode );
    }

    public ForkedChannelEncoder asRerunMode() // todo apply this and rework providers
    {
        return new ForkedChannelEncoder( streamCharset, out, RERUN );
    }

    public ForkedChannelEncoder asNormalMode()
    {
        return new ForkedChannelEncoder( streamCharset, out, NORMAL_RUN );
    }

    public boolean checkError()
    {
        return trouble;
    }

    public void sendSystemProperties()
    {
        SortedMap<String, String> sortedProperties = new TreeMap<String, String>();
        for ( Entry<?, ?> entry : System.getProperties().entrySet() )
        {
            Object key = entry.getKey();
            Object value = entry.getValue();
            if ( key instanceof String && ( value == null || value instanceof String ) )
            {
                sortedProperties.put( (String) key, (String) value );
            }
        }

        for ( Entry<String, String> entry : sortedProperties.entrySet() )
        {
            String key = entry.getKey();
            Object value = entry.getValue();
            String valueAsString = value == null ? null : value.toString();
            StringBuilder event = encode( BOOTERCODE_SYSPROPS, runMode, key, valueAsString );
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

    public void stdOut( byte[] buf, int off, int len )
    {
        StringBuilder event =
                stdOutErr( BOOTERCODE_STDOUT.getOpcode(), runMode.geRunName(), buf, off, len, defaultCharset() );
        encodeAndPrintEvent( event );
    }

    public void stdOut( String msg )
    {
        byte[] buf = ( msg == null ? "null" : msg ).getBytes( UTF_8 );
        StringBuilder event =
                stdOutErr( BOOTERCODE_STDOUT.getOpcode(), runMode.geRunName(), buf, 0, buf.length, UTF_8 );
        encodeAndPrintEvent( event );
    }

    public void stdErr( byte[] buf, int off, int len )
    {
        StringBuilder event =
                stdOutErr( BOOTERCODE_STDERR.getOpcode(), runMode.geRunName(), buf, off, len, defaultCharset() );
        encodeAndPrintEvent( event );
    }

    public void stdErr( String msg )
    {
        byte[] buf = ( msg == null ? "null" : msg ).getBytes( UTF_8 );
        StringBuilder event =
                stdOutErr( BOOTERCODE_STDERR.getOpcode(), runMode.geRunName(), buf, 0, buf.length, UTF_8 );
        encodeAndPrintEvent( event );
    }

    public void console( String msg )
    {
        StringBuilder event = print( BOOTERCODE_CONSOLE_INFO.getOpcode(), runMode.geRunName(), UTF_8, msg );
        encodeAndPrintEvent( event );
    }

    public void error( String msg )
    {
        StringBuilder event = print( BOOTERCODE_CONSOLE_ERROR.getOpcode(), runMode.geRunName(), UTF_8, msg );
        encodeAndPrintEvent( event );
    }

    public void error( Throwable t )
    {
        error( t.getLocalizedMessage(), t );
    }

    public void error( String msg, Throwable t )
    {
        StringBuilder encoded = encodeHeader( BOOTERCODE_CONSOLE_ERROR.getOpcode(), runMode.geRunName(), UTF_8 );
        encode( encoded, msg, null, ConsoleLoggerUtils.toString( t ) );
        encodeAndPrintEvent( encoded );
    }

    public void error( StackTraceWriter stackTraceWriter, boolean trimStackTraces )
    {
        StringBuilder encoded = encodeHeader( BOOTERCODE_CONSOLE_ERROR.getOpcode(), runMode.geRunName(), UTF_8 );
        encode( encoded, stackTraceWriter, trimStackTraces );
        encodeAndPrintEvent( encoded );
    }

    public void debug( String msg )
    {
        StringBuilder event = print( BOOTERCODE_CONSOLE_DEBUG.getOpcode(), runMode.geRunName(), UTF_8, msg );
        encodeAndPrintEvent( event );
    }

    public void warning( String msg )
    {
        StringBuilder event = print( BOOTERCODE_CONSOLE_WARNING.getOpcode(), runMode.geRunName(), UTF_8, msg );
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

    private void encode( ForkedProcessEvent operation, RunMode runMode, ReportEntry reportEntry,
                         boolean trimStackTraces )
    {
        StringBuilder event = encode( operation.getOpcode(), runMode.geRunName(), reportEntry, trimStackTraces );
        encodeAndPrintEvent( event );
    }

    private void encodeOpcode( ForkedProcessEvent operation )
    {
        StringBuilder event = encodeOpcode( runMode.geRunName(), operation.getOpcode() );
        encodeAndPrintEvent( event );
    }

    private void encodeAndPrintEvent( StringBuilder command )
    {
        byte[] array = command.append( '\n' ).toString().getBytes( streamCharset );
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
        StringBuilder encodedTo = encodeHeader( operation.getOpcode(), runMode.geRunName(), UTF_8 )
                                          .append( ':' );

        for ( int i = 0; i < args.length; )
        {
            String arg = args[i++];
            base64WithUtf8( encodedTo, arg == null ? "-" : arg );
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
        encoded.append( ':' );
        base64WithUtf8( encoded, message );
        encoded.append( ':' );
        base64WithUtf8( encoded, smartStackTrace );
        encoded.append( ':' );
        base64WithUtf8( encoded, stackTrace );
    }

    /**
     * Used operations:<br/>
     * <p>
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
     * </p>
     */
    static StringBuilder encode( String operation, String runMode, ReportEntry reportEntry, boolean trimStackTraces )
    {
        StringBuilder encodedTo = encodeHeader( operation, runMode, UTF_8 )
                                          .append( ':' );

        base64WithUtf8( encodedTo, reportEntry.getSourceName() );
        encodedTo.append( ':' );
        base64WithUtf8( encodedTo, reportEntry.getName() );
        encodedTo.append( ':' );
        base64WithUtf8( encodedTo, reportEntry.getGroup() );
        encodedTo.append( ':' );
        base64WithUtf8( encodedTo, reportEntry.getMessage() );
        encodedTo.append( ':' )
                .append( reportEntry.getElapsed() == null ? "-" : reportEntry.getElapsed().toString() );
        encode( encodedTo, reportEntry.getStackTraceWriter(), trimStackTraces );

        return encodedTo;
    }

    static StringBuilder stdOutErr( String operation, String runMode, byte[] buf, int off, int len,
                                    Charset bufEncoding )
    {
        final byte[] encodeBytes;
        if ( off == 0 && buf.length == len )
        {
            encodeBytes = buf;
        }
        else
        {
            encodeBytes = new byte[len];
            arraycopy( buf, off, encodeBytes, 0, len );
        }
        return encodeMessage( operation, runMode, bufEncoding, printBase64Binary( encodeBytes ) );
    }

    /**
     * Used in {@link #console(String)}, {@link #error(String)}, {@link #debug(String)} and {@link #warning(String)}
     * and private methods extending the buffer.
     */
    static StringBuilder print( String operation, String runMode, Charset msgEncoding, String... msgs )
    {
        String[] encodedMsgs = new String[msgs.length];
        for ( int i = 0; i < encodedMsgs.length; i++ )
        {
            String msg = msgs[i];
            encodedMsgs[i] = msg == null ? "-" : toBase64( msg, msgEncoding );
        }
        return encodeMessage( operation, runMode, msgEncoding, encodedMsgs );
    }

    static StringBuilder encodeMessage( String operation, String runMode, Charset encoding, String... msgs )
    {
        StringBuilder builder = encodeHeader( operation, runMode, encoding );
        for ( String msg : msgs )
        {
            builder.append( ':' )
                    .append( msg );

        }
        return builder;
    }

    static StringBuilder encodeHeader( String operation, String runMode, Charset encoding )
    {
        return encodeOpcode( runMode, operation )
                       .append( ':' )
                       .append( encoding.name() );
    }

    /**
     * Used in {@link #bye()}, {@link #stopOnNextTest()} and {@link #encodeOpcode(ForkedProcessEvent)}
     * and private methods extending the buffer.
     *
     * @param runMode   run mode
     * @param operation opcode
     * @return encoded command
     */
    static StringBuilder encodeOpcode( String runMode, String operation )
    {
        return new StringBuilder( 128 )
                .append( MAGIC_NUMBER )
                .append( runMode )
                .append( ':' )
                .append( operation );
    }

    static String base64WithUtf8( String msg )
    {
        if ( msg == null )
        {
            return "-";
        }
        else
        {
            byte[] binary = msg.getBytes( UTF_8 );
            return printBase64Binary( binary );
        }
    }

    static void base64WithUtf8( StringBuilder encoded, String msg )
    {
        encoded.append( base64WithUtf8( msg ) );
    }

    private static String toStackTrace( StackTraceWriter stw, boolean trimStackTraces )
    {
        return trimStackTraces ? stw.writeTrimmedTraceToString() : stw.writeTraceToString();
    }

    static String toBase64( String msg, Charset encoding )
    {
        return msg == null ? "-" : printBase64Binary( msg.getBytes( encoding ) );
    }
}
