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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.ConsoleLoggerUtils;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ConsoleStream;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.report.TestSetReportEntry;

import java.io.PrintStream;
import java.util.Map.Entry;

import static java.lang.Integer.toHexString;
import static java.nio.charset.Charset.defaultCharset;
import static org.apache.maven.surefire.util.internal.ObjectUtils.systemProps;
import static org.apache.maven.surefire.util.internal.ObjectUtils.useNonNull;
import static org.apache.maven.surefire.util.internal.StringUtils.encodeStringForForkCommunication;
import static org.apache.maven.surefire.util.internal.StringUtils.escapeBytesToPrintable;
import static org.apache.maven.surefire.util.internal.StringUtils.escapeToPrintable;

/**
 * Encodes the full output of the test run to the stdout stream.
 * <br>
 * This class and the ForkClient contain the full definition of the
 * "wire-level" protocol used by the forked process. The protocol
 * is *not* part of any public api and may change without further
 * notice.
 * <br>
 * This class is threadsafe.
 * <br>
 * The synchronization in the underlying PrintStream (target instance)
 * is used to preserve thread safety of the output stream. To perform
 * multiple writes/prints for a single request, they must
 * synchronize on "target" variable in this class.
 *
 * @author Kristian Rosenvold
 */
public class ForkingRunListener
    implements RunListener, ConsoleLogger, ConsoleOutputReceiver, ConsoleStream
{
    public static final byte BOOTERCODE_TESTSET_STARTING = (byte) '1';

    public static final byte BOOTERCODE_TESTSET_COMPLETED = (byte) '2';

    public static final byte BOOTERCODE_STDOUT = (byte) '3';

    public static final byte BOOTERCODE_STDERR = (byte) '4';

    public static final byte BOOTERCODE_TEST_STARTING = (byte) '5';

    public static final byte BOOTERCODE_TEST_SUCCEEDED = (byte) '6';

    public static final byte BOOTERCODE_TEST_ERROR = (byte) '7';

    public static final byte BOOTERCODE_TEST_FAILED = (byte) '8';

    public static final byte BOOTERCODE_TEST_SKIPPED = (byte) '9';

    public static final byte BOOTERCODE_TEST_ASSUMPTIONFAILURE = (byte) 'G';

    /**
     * INFO logger
     * @see ConsoleLogger#info(String)
     */
    public static final byte BOOTERCODE_CONSOLE = (byte) 'H';

    public static final byte BOOTERCODE_SYSPROPS = (byte) 'I';

    public static final byte BOOTERCODE_NEXT_TEST = (byte) 'N';

    public static final byte BOOTERCODE_STOP_ON_NEXT_TEST = (byte) 'S';

    /**
     * ERROR logger
     * @see ConsoleLogger#error(String)
     */
    public static final byte BOOTERCODE_ERROR = (byte) 'X';

    public static final byte BOOTERCODE_BYE = (byte) 'Z';

    /**
     * DEBUG logger
     * @see ConsoleLogger#debug(String)
     */
    public static final byte BOOTERCODE_DEBUG = (byte) 'D';

    /**
     * WARNING logger
     * @see ConsoleLogger#warning(String)
     */
    public static final byte BOOTERCODE_WARNING = (byte) 'W';


    private final PrintStream target;

    private final int testSetChannelId;

    private final boolean trimStackTraces;

    private final byte[] stdOutHeader;

    private final byte[] stdErrHeader;

    public ForkingRunListener( PrintStream target, int testSetChannelId, boolean trimStackTraces )
    {
        this.target = target;
        this.testSetChannelId = testSetChannelId;
        this.trimStackTraces = trimStackTraces;
        stdOutHeader = createHeader( BOOTERCODE_STDOUT, testSetChannelId );
        stdErrHeader = createHeader( BOOTERCODE_STDERR, testSetChannelId );
        sendProps();
    }

    @Override
    public void testSetStarting( TestSetReportEntry report )
    {
        encodeAndWriteToTarget( toString( BOOTERCODE_TESTSET_STARTING, report, testSetChannelId ) );
    }

    @Override
    public void testSetCompleted( TestSetReportEntry report )
    {
        encodeAndWriteToTarget( toString( BOOTERCODE_TESTSET_COMPLETED, report, testSetChannelId ) );
    }

    @Override
    public void testStarting( ReportEntry report )
    {
        encodeAndWriteToTarget( toString( BOOTERCODE_TEST_STARTING, report, testSetChannelId ) );
    }

    @Override
    public void testSucceeded( ReportEntry report )
    {
        encodeAndWriteToTarget( toString( BOOTERCODE_TEST_SUCCEEDED, report, testSetChannelId ) );
    }

    @Override
    public void testAssumptionFailure( ReportEntry report )
    {
        encodeAndWriteToTarget( toString( BOOTERCODE_TEST_ASSUMPTIONFAILURE, report, testSetChannelId ) );
    }

    @Override
    public void testError( ReportEntry report )
    {
        encodeAndWriteToTarget( toString( BOOTERCODE_TEST_ERROR, report, testSetChannelId ) );
    }

    @Override
    public void testFailed( ReportEntry report )
    {
        encodeAndWriteToTarget( toString( BOOTERCODE_TEST_FAILED, report, testSetChannelId ) );
    }

    @Override
    public void testSkipped( ReportEntry report )
    {
        encodeAndWriteToTarget( toString( BOOTERCODE_TEST_SKIPPED, report, testSetChannelId ) );
    }

    @Override
    public void testExecutionSkippedByUser()
    {
        encodeAndWriteToTarget( toString( BOOTERCODE_STOP_ON_NEXT_TEST, new SimpleReportEntry(), testSetChannelId ) );
    }

    void sendProps()
    {
        for ( Entry<String, String> entry : systemProps().entrySet() )
        {
            String value = entry.getValue();
            encodeAndWriteToTarget( toPropertyString( entry.getKey(), useNonNull( value, "null" ) ) );
        }
    }

    @Override
    public void writeTestOutput( byte[] buf, int off, int len, boolean stdout )
    {
        byte[] header = stdout ? stdOutHeader : stdErrHeader;
        byte[] content =
            new byte[buf.length * 3 + 1]; // Hex-escaping can be up to 3 times length of a regular byte.
        int i = escapeBytesToPrintable( content, 0, buf, off, len );
        content[i++] = (byte) '\n';
        byte[] encodeBytes = new byte[header.length + i];
        System.arraycopy( header, 0, encodeBytes, 0, header.length );
        System.arraycopy( content, 0, encodeBytes, header.length, i );

        synchronized ( target ) // See notes about synchronization/thread safety in class javadoc
        {
            target.write( encodeBytes, 0, encodeBytes.length );
            target.flush();
            if ( target.checkError() )
            {
                // We MUST NOT throw any exception from this method; otherwise we are in loop and CPU goes up:
                // ForkingRunListener -> Exception -> JUnit Notifier and RunListener -> ForkingRunListener -> Exception
                DumpErrorSingleton.getSingleton()
                        .dumpStreamText( "Unexpected IOException with stream: " + new String( buf, off, len ) );
            }
        }
    }

    public static byte[] createHeader( byte booterCode, int testSetChannel )
    {
        return encodeStringForForkCommunication( String.valueOf( (char) booterCode )
                + ','
                + Integer.toString( testSetChannel, 16 )
                + ',' + defaultCharset().name()
                + ',' );
    }

    private void log( byte bootCode, String message )
    {
        if ( message != null )
        {
            StringBuilder sb = new StringBuilder( 7 + message.length() * 5 );
            append( sb, bootCode ); comma( sb );
            append( sb, toHexString( testSetChannelId ) ); comma( sb );
            escapeToPrintable( sb, message );

            sb.append( '\n' );
            encodeAndWriteToTarget( sb.toString() );
        }
    }

    @Override
    public void debug( String message )
    {
        log( BOOTERCODE_DEBUG, message );
    }

    @Override
    public void info( String message )
    {
        log( BOOTERCODE_CONSOLE, message );
    }

    @Override
    public void warning( String message )
    {
        log( BOOTERCODE_WARNING, message );
    }

    @Override
    public void error( String message )
    {
        log( BOOTERCODE_ERROR, message );
    }

    @Override
    public void error( String message, Throwable t )
    {
        error( ConsoleLoggerUtils.toString( message, t ) );
    }

    @Override
    public void error( Throwable t )
    {
        error( null, t );
    }

    private void encodeAndWriteToTarget( String string )
    {
        byte[] encodeBytes = encodeStringForForkCommunication( string );
        synchronized ( target ) // See notes about synchronization/thread safety in class javadoc
        {
            target.write( encodeBytes, 0, encodeBytes.length );
            target.flush();
            if ( target.checkError() )
            {
                // We MUST NOT throw any exception from this method; otherwise we are in loop and CPU goes up:
                // ForkingRunListener -> Exception -> JUnit Notifier and RunListener -> ForkingRunListener -> Exception
                DumpErrorSingleton.getSingleton().dumpStreamText( "Unexpected IOException: " + string );
            }
        }
    }

    private String toPropertyString( String key, String value )
    {
        StringBuilder stringBuilder = new StringBuilder();

        append( stringBuilder, BOOTERCODE_SYSPROPS ); comma( stringBuilder );
        append( stringBuilder, toHexString( testSetChannelId ) ); comma( stringBuilder );

        escapeToPrintable( stringBuilder, key );
        comma( stringBuilder );
        escapeToPrintable( stringBuilder, value );
        stringBuilder.append( "\n" );
        return stringBuilder.toString();
    }

    private String toString( byte operationCode, ReportEntry reportEntry, int testSetChannelId )
    {
        StringBuilder stringBuilder = new StringBuilder();
        append( stringBuilder, operationCode ); comma( stringBuilder );
        append( stringBuilder, toHexString( testSetChannelId ) ); comma( stringBuilder );

        nullableEncoding( stringBuilder, reportEntry.getSourceName() );
        comma( stringBuilder );
        nullableEncoding( stringBuilder, reportEntry.getName() );
        comma( stringBuilder );
        nullableEncoding( stringBuilder, reportEntry.getGroup() );
        comma( stringBuilder );
        nullableEncoding( stringBuilder, reportEntry.getMessage() );
        comma( stringBuilder );
        nullableEncoding( stringBuilder, reportEntry.getElapsed() );
        encode( stringBuilder, reportEntry.getStackTraceWriter() );
        stringBuilder.append( "\n" );
        return stringBuilder.toString();
    }

    private static void comma( StringBuilder stringBuilder )
    {
        stringBuilder.append( "," );
    }

    private ForkingRunListener append( StringBuilder stringBuilder, String message )
    {
        stringBuilder.append( encode( message ) );
        return this;
    }

    private ForkingRunListener append( StringBuilder stringBuilder, byte b )
    {
        stringBuilder.append( (char) b );
        return this;
    }

    private void nullableEncoding( StringBuilder stringBuilder, Integer source )
    {
        if ( source == null )
        {
            stringBuilder.append( "null" );
        }
        else
        {
            stringBuilder.append( source.toString() );
        }
    }

    private String encode( String source )
    {
        return source;
    }


    private static void nullableEncoding( StringBuilder stringBuilder, String source )
    {
        if ( source == null || source.length() == 0 )
        {
            stringBuilder.append( "null" );
        }
        else
        {
            escapeToPrintable( stringBuilder, source );
        }
    }

    private void encode( StringBuilder stringBuilder, StackTraceWriter stackTraceWriter )
    {
        encode( stringBuilder, stackTraceWriter, trimStackTraces );
    }

    public static void encode( StringBuilder stringBuilder, StackTraceWriter stackTraceWriter, boolean trimStackTraces )
    {
        if ( stackTraceWriter != null )
        {
            comma( stringBuilder );
            //noinspection ThrowableResultOfMethodCallIgnored
            final SafeThrowable throwable = stackTraceWriter.getThrowable();
            if ( throwable != null )
            {
                String message = throwable.getLocalizedMessage();
                nullableEncoding( stringBuilder, message );
            }
            comma( stringBuilder );
            nullableEncoding( stringBuilder, stackTraceWriter.smartTrimmedStackTrace() );
            comma( stringBuilder );
            nullableEncoding( stringBuilder, trimStackTraces
                ? stackTraceWriter.writeTrimmedTraceToString()
                : stackTraceWriter.writeTraceToString() );
        }
    }

    @Override
    public void println( String message )
    {
        byte[] buf = message.getBytes();
        println( buf, 0, buf.length );
    }

    @Override
    public void println( byte[] buf, int off, int len )
    {
        writeTestOutput( buf, off, len, true );
    }
}
