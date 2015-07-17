package org.apache.maven.plugin.surefire.booterclient.output;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.NotifiableTestStream;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.shared.utils.cli.StreamConsumer;
import org.apache.maven.surefire.booter.ForkingRunListener;
import org.apache.maven.surefire.report.CategorizedReportEntry;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.util.internal.StringUtils;

/**
 * Knows how to reconstruct *all* the state transmitted over stdout by the forked process.
 *
 * @author Kristian Rosenvold
 */
public class ForkClient
    implements StreamConsumer
{
    private final DefaultReporterFactory defaultReporterFactory;

    private final NotifiableTestStream notifiableTestStream;

    private final Map<Integer, RunListener> testSetReporters = new ConcurrentHashMap<Integer, RunListener>();

    private final Properties testVmSystemProperties;

    private volatile boolean saidGoodBye;

    private volatile StackTraceWriter errorInFork;

    public ForkClient( DefaultReporterFactory defaultReporterFactory, Properties testVmSystemProperties )
    {
        this( defaultReporterFactory, testVmSystemProperties, null );
    }

    public ForkClient( DefaultReporterFactory defaultReporterFactory, Properties testVmSystemProperties,
                       NotifiableTestStream notifiableTestStream )
    {
        this.defaultReporterFactory = defaultReporterFactory;
        this.testVmSystemProperties = testVmSystemProperties;
        this.notifiableTestStream = notifiableTestStream;
    }

    public DefaultReporterFactory getDefaultReporterFactory()
    {
        return defaultReporterFactory;
    }

    public void consumeLine( String s )
    {
        if ( StringUtils.isNotBlank( s ) )
        {
            processLine( s );
        }
    }

    private void processLine( String s )
    {
        try
        {
            final byte operationId = (byte) s.charAt( 0 );
            int commma = s.indexOf( ",", 3 );
            if ( commma < 0 )
            {
                System.out.println( s );
                return;
            }
            final int channelNumber = Integer.parseInt( s.substring( 2, commma ), 16 );
            int rest = s.indexOf( ",", commma );
            final String remaining = s.substring( rest + 1 );

            switch ( operationId )
            {
                case ForkingRunListener.BOOTERCODE_TESTSET_STARTING:
                    getOrCreateReporter( channelNumber ).testSetStarting( createReportEntry( remaining ) );
                    break;
                case ForkingRunListener.BOOTERCODE_TESTSET_COMPLETED:
                    getOrCreateReporter( channelNumber ).testSetCompleted( createReportEntry( remaining ) );
                    break;
                case ForkingRunListener.BOOTERCODE_TEST_STARTING:
                    getOrCreateReporter( channelNumber ).testStarting( createReportEntry( remaining ) );
                    break;
                case ForkingRunListener.BOOTERCODE_TEST_SUCCEEDED:
                    getOrCreateReporter( channelNumber ).testSucceeded( createReportEntry( remaining ) );
                    break;
                case ForkingRunListener.BOOTERCODE_TEST_FAILED:
                    getOrCreateReporter( channelNumber ).testFailed( createReportEntry( remaining ) );
                    break;
                case ForkingRunListener.BOOTERCODE_TEST_SKIPPED:
                    getOrCreateReporter( channelNumber ).testSkipped( createReportEntry( remaining ) );
                    break;
                case ForkingRunListener.BOOTERCODE_TEST_ERROR:
                    getOrCreateReporter( channelNumber ).testError( createReportEntry( remaining ) );
                    break;
                case ForkingRunListener.BOOTERCODE_TEST_ASSUMPTIONFAILURE:
                    getOrCreateReporter( channelNumber ).testAssumptionFailure( createReportEntry( remaining ) );
                    break;
                case ForkingRunListener.BOOTERCODE_SYSPROPS:
                    int keyEnd = remaining.indexOf( "," );
                    StringBuilder key = new StringBuilder();
                    StringBuilder value = new StringBuilder();
                    StringUtils.unescapeString( key, remaining.substring( 0, keyEnd ) );
                    StringUtils.unescapeString( value, remaining.substring( keyEnd + 1 ) );

                    synchronized ( testVmSystemProperties )
                    {
                        testVmSystemProperties.put( key.toString(), value.toString() );
                    }
                    break;
                case ForkingRunListener.BOOTERCODE_STDOUT:
                    writeTestOutput( channelNumber, remaining, true );
                    break;
                case ForkingRunListener.BOOTERCODE_STDERR:
                    writeTestOutput( channelNumber, remaining, false );
                    break;
                case ForkingRunListener.BOOTERCODE_CONSOLE:
                    getOrCreateConsoleLogger( channelNumber ).info( createConsoleMessage( remaining ) );
                    break;
                case ForkingRunListener.BOOTERCODE_NEXT_TEST:
                    if ( notifiableTestStream != null )
                    {
                        notifiableTestStream.provideNewTest();
                    }
                    break;
                case ForkingRunListener.BOOTERCODE_ERROR:
                    errorInFork = deserializeStackTraceWriter( new StringTokenizer( remaining, "," ) );
                    break;
                case ForkingRunListener.BOOTERCODE_BYE:
                    saidGoodBye = true;
                    break;
                default:
                    System.out.println( s );
            }
        }
        catch ( NumberFormatException e )
        {
            // SUREFIRE-859
            System.out.println( s );
        }
        catch ( NoSuchElementException e )
        {
            // SUREFIRE-859
            System.out.println( s );
        }
        catch ( ReporterException e )
        {
            throw new RuntimeException( e );
        }
    }

    private void writeTestOutput( final int channelNumber, final String remaining, boolean isStdout )
    {
        int csNameEnd = remaining.indexOf( ',' );
        String charsetName = remaining.substring( 0, csNameEnd );
        String byteEncoded = remaining.substring( csNameEnd + 1 );
        ByteBuffer unescaped = StringUtils.unescapeBytes( byteEncoded, charsetName );

        if ( unescaped.hasArray() )
        {
            byte[] convertedBytes = unescaped.array();

            getOrCreateConsoleOutputReceiver( channelNumber )
                .writeTestOutput( convertedBytes, unescaped.position(), unescaped.remaining(), isStdout );
        }
        else
        {
            byte[] convertedBytes = new byte[unescaped.remaining()];
            unescaped.get( convertedBytes, 0, unescaped.remaining() );

            getOrCreateConsoleOutputReceiver( channelNumber )
                .writeTestOutput( convertedBytes, 0, convertedBytes.length, isStdout );
        }
    }

    public void consumeMultiLineContent( String s )
        throws IOException
    {
        BufferedReader stringReader = new BufferedReader( new StringReader( s ) );
        for ( String s1 = stringReader.readLine(); s1 != null; s1 = stringReader.readLine() )
        {
            consumeLine( s1 );
        }
    }

    private String createConsoleMessage( String remaining )
    {
        return unescape( remaining );
    }

    private ReportEntry createReportEntry( String untokenized )
    {
        StringTokenizer tokens = new StringTokenizer( untokenized, "," );
        try
        {
            String source = nullableCsv( tokens.nextToken() );
            String name = nullableCsv( tokens.nextToken() );
            String group = nullableCsv( tokens.nextToken() );
            String message = nullableCsv( tokens.nextToken() );
            String elapsedStr = tokens.nextToken();
            Integer elapsed = "null".equals( elapsedStr ) ? null : Integer.decode( elapsedStr );
            final StackTraceWriter stackTraceWriter =
                tokens.hasMoreTokens() ? deserializeStackTraceWriter( tokens ) : null;

            return CategorizedReportEntry.reportEntry( source, name, group, stackTraceWriter, elapsed, message );
        }
        catch ( RuntimeException e )
        {
            throw new RuntimeException( untokenized, e );
        }
    }

    private StackTraceWriter deserializeStackTraceWriter( StringTokenizer tokens )
    {
        StackTraceWriter stackTraceWriter;
        String stackTraceMessage = nullableCsv( tokens.nextToken() );
        String smartStackTrace = nullableCsv( tokens.nextToken() );
        String stackTrace = tokens.hasMoreTokens() ? nullableCsv( tokens.nextToken() ) : null;
        stackTraceWriter = stackTrace != null
            ? new DeserializedStacktraceWriter( stackTraceMessage, smartStackTrace, stackTrace )
            : null;
        return stackTraceWriter;
    }

    private String nullableCsv( String source )
    {
        return "null".equals( source ) ? null : unescape( source );
    }

    private String unescape( String source )
    {
        StringBuilder stringBuffer = new StringBuilder( source.length() );
        StringUtils.unescapeString( stringBuffer, source );
        return stringBuffer.toString();
    }

    /**
     * Used when getting reporters on the plugin side of a fork.
     *
     * @param channelNumber The logical channel number
     * @return A mock provider reporter
     */
    public RunListener getReporter( int channelNumber )
    {
        return testSetReporters.get( channelNumber );
    }

    private RunListener getOrCreateReporter( int channelNumber )
    {
        RunListener reporter = testSetReporters.get( channelNumber );
        if ( reporter == null )
        {
            reporter = defaultReporterFactory.createReporter();
            testSetReporters.put( channelNumber, reporter );
        }
        return reporter;
    }

    private ConsoleOutputReceiver getOrCreateConsoleOutputReceiver( int channelNumber )
    {
        return (ConsoleOutputReceiver) getOrCreateReporter( channelNumber );
    }

    private ConsoleLogger getOrCreateConsoleLogger( int channelNumber )
    {
        return (ConsoleLogger) getOrCreateReporter( channelNumber );
    }

    public void close( boolean hadTimeout )
    {
    }

    public boolean isSaidGoodBye()
    {
        return saidGoodBye;
    }

    public StackTraceWriter getErrorInFork()
    {
        return errorInFork;
    }

    public boolean isErrorInFork()
    {
        return errorInFork != null;
    }
}
