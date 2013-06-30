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

import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.util.internal.ByteBuffer;
import org.apache.maven.surefire.util.internal.StringUtils;

/**
 * Encodes the full output of the test run to the stdout stream.
 * <p/>
 * This class and the ForkClient contain the full definition of the
 * "wire-level" protocol used by the forked process. The protocol
 * is *not* part of any public api and may change without further
 * notice.
 * <p/>
 * This class is threadsafe.
 * <p/>
 * The synchronization in the underlying PrintStream (target instance)
 * is used to preserve thread safety of the output stream. To perform
 * multiple writes/prints for a single request, they must
 * synchronize on "target" variable in this class.
 *
 * @author Kristian Rosenvold
 */
public class ForkingRunListener
    implements RunListener, ConsoleLogger, ConsoleOutputReceiver
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

    public static final byte BOOTERCODE_CONSOLE = (byte) 'H';

    public static final byte BOOTERCODE_SYSPROPS = (byte) 'I';

    public static final byte BOOTERCODE_NEXT_TEST = (byte) 'N';

    public static final byte BOOTERCODE_ERROR = (byte) 'X';

    public static final byte BOOTERCODE_BYE = (byte) 'Z';


    private final PrintStream target;

    private final Integer testSetChannelId;

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

    public void testSetStarting( ReportEntry report )
    {
        target.print( toString( BOOTERCODE_TESTSET_STARTING, report, testSetChannelId ) );
    }

    public void testSetCompleted( ReportEntry report )
    {
        target.print( toString( BOOTERCODE_TESTSET_COMPLETED, report, testSetChannelId ) );
    }

    public void testStarting( ReportEntry report )
    {
        target.print( toString( BOOTERCODE_TEST_STARTING, report, testSetChannelId ) );
    }

    public void testSucceeded( ReportEntry report )
    {
        target.print( toString( BOOTERCODE_TEST_SUCCEEDED, report, testSetChannelId ) );
    }

    public void testAssumptionFailure( ReportEntry report )
    {
        target.print( toString( BOOTERCODE_TEST_ASSUMPTIONFAILURE, report, testSetChannelId ) );
    }

    public void testError( ReportEntry report )
    {
        target.print( toString( BOOTERCODE_TEST_ERROR, report, testSetChannelId ) );
    }

    public void testFailed( ReportEntry report )
    {
        target.print( toString( BOOTERCODE_TEST_FAILED, report, testSetChannelId ) );
    }

    public void testSkipped( ReportEntry report )
    {
        target.print( toString( BOOTERCODE_TEST_SKIPPED, report, testSetChannelId ) );
    }

    void sendProps()
    {
        Properties systemProperties = System.getProperties();

        if ( systemProperties != null )
        {
            Enumeration propertyKeys = systemProperties.propertyNames();

            while ( propertyKeys.hasMoreElements() )
            {
                String key = (String) propertyKeys.nextElement();

                String value = systemProperties.getProperty( key );

                if ( value == null )
                {
                    value = "null";
                }
                target.print( toPropertyString( key, value ) );
            }
        }
    }

    public void writeTestOutput( byte[] buf, int off, int len, boolean stdout )
    {
        byte[] header = stdout ? stdOutHeader : stdErrHeader;
        byte[] content =
            new byte[buf.length * 6 + 1]; // Unicode escapes can be up to 6 times length of regular char. Yuck.
        int i = StringUtils.escapeJavaStyleString( content, 0, buf, off, len );
        content[i++] = (byte) '\n';

        synchronized ( target ) // See notes about synhronization/thread safety in class javadoc
        {
            target.write( header, 0, header.length );
            target.write( content, 0, i );
        }
    }

    public static byte[] createHeader( byte booterCode, int testSetChannel )
    {
        byte[] header = new byte[7];
        header[0] = booterCode;
        header[1] = (byte) ',';
        header[6] = (byte) ',';

        int i = testSetChannel;
        int charPos = 6;
        int radix = 1 << 4;
        int mask = radix - 1;
        do
        {
            header[--charPos] = (byte) digits[i & mask];
            i >>>= 4;
        }
        while ( i != 0 );

        while ( charPos > 2 )
        {
            header[--charPos] = (byte) '0';
        }
        return header;
    }

    private final static char[] digits =
        { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };


    public void info( String message )
    {
        byte[] buf = message.getBytes();
        ByteBuffer byteBuffer = new ByteBuffer( 7 + buf.length * 6 ); // 7 => Allow 3 digit testSetChannelId
        byteBuffer.append( BOOTERCODE_CONSOLE );
        byteBuffer.comma();
        byteBuffer.append( testSetChannelId );
        byteBuffer.comma();
        final int i =
            StringUtils.escapeJavaStyleString( byteBuffer.getData(), byteBuffer.getlength(), buf, 0, buf.length );
        byteBuffer.advance( i );
        byteBuffer.append( '\n' );
        synchronized ( target )
        {
            target.write( byteBuffer.getData(), 0, byteBuffer.getlength() );
            target.flush();
        }
    }

    private String toPropertyString( String key, String value )
    {
        StringBuffer stringBuffer = new StringBuffer();

        append( stringBuffer, BOOTERCODE_SYSPROPS );comma( stringBuffer );
        append( stringBuffer, Integer.toHexString( testSetChannelId ) );comma( stringBuffer );

        StringUtils.escapeJavaStyleString( stringBuffer, key );
        append( stringBuffer, "," );
        StringUtils.escapeJavaStyleString( stringBuffer, value );
        stringBuffer.append( "\n" );
        return stringBuffer.toString();
    }

    private String toString( byte operationCode, ReportEntry reportEntry, Integer testSetChannelId )
    {
        StringBuffer stringBuffer = new StringBuffer();
        append( stringBuffer, operationCode ); comma( stringBuffer );
        append( stringBuffer, Integer.toHexString( testSetChannelId ) );comma( stringBuffer );

        nullableEncoding( stringBuffer, reportEntry.getSourceName() );
        comma( stringBuffer );
        nullableEncoding( stringBuffer, reportEntry.getName() );
        comma( stringBuffer );
        nullableEncoding( stringBuffer, reportEntry.getGroup() );
        comma( stringBuffer );
        nullableEncoding( stringBuffer, reportEntry.getMessage() );
        comma( stringBuffer );
        nullableEncoding( stringBuffer, reportEntry.getElapsed() );
        encode( stringBuffer, reportEntry.getStackTraceWriter() );
        stringBuffer.append( "\n" );
        return stringBuffer.toString();
    }

    private static void comma( StringBuffer stringBuffer )
    {
        stringBuffer.append( "," );
    }

    private ForkingRunListener append( StringBuffer stringBuffer, String message )
    {
        stringBuffer.append( encode( message ) );
        return this;
    }

    private ForkingRunListener append( StringBuffer stringBuffer, byte b )
    {
        stringBuffer.append( (char) b );
        return this;
    }

    private void nullableEncoding( StringBuffer stringBuffer, Integer source )
    {
        if ( source == null )
        {
            stringBuffer.append( "null" );
        }
        else
        {
            stringBuffer.append( source.toString() );
        }
    }

    private String encode( String source )
    {
        return source;
    }


    private static void nullableEncoding( StringBuffer stringBuffer, String source )
    {
        if ( source == null || source.length() == 0 )
        {
            stringBuffer.append( "null" );
        }
        else
        {
            StringUtils.escapeJavaStyleString( stringBuffer, source );
        }
    }

    private void encode( StringBuffer stringBuffer, StackTraceWriter stackTraceWriter )
    {
        encode( stringBuffer, stackTraceWriter, trimStackTraces );
    }

    public static void encode( StringBuffer stringBuffer, StackTraceWriter stackTraceWriter, boolean trimStackTraces )
    {
        if ( stackTraceWriter != null )
        {
            comma( stringBuffer );
            //noinspection ThrowableResultOfMethodCallIgnored
            final SafeThrowable throwable = stackTraceWriter.getThrowable();
            if ( throwable != null )
            {
                String message = throwable.getLocalizedMessage();
                nullableEncoding( stringBuffer, message );
            }
            comma( stringBuffer );
            nullableEncoding( stringBuffer, stackTraceWriter.smartTrimmedStackTrace() );
            comma( stringBuffer );
            nullableEncoding( stringBuffer, trimStackTraces
                ? stackTraceWriter.writeTrimmedTraceToString()
                : stackTraceWriter.writeTraceToString() );
        }
    }
}
