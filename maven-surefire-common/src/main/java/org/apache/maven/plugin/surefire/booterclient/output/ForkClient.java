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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.TestProvidingInputStream;
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

    private final TestProvidingInputStream testProvidingInputStream;

    private final Map<Integer, RunListener> testSetReporters =
        Collections.synchronizedMap( new HashMap<Integer, RunListener>() );

    private final Properties testVmSystemProperties;

    private volatile boolean saidGoodBye = false;

    private volatile StackTraceWriter errorInFork = null;

    public ForkClient( DefaultReporterFactory defaultReporterFactory, Properties testVmSystemProperties )
    {
        this( defaultReporterFactory, testVmSystemProperties, null );
    }

    public ForkClient( DefaultReporterFactory defaultReporterFactory, Properties testVmSystemProperties,
                       TestProvidingInputStream testProvidingInputStream )
    {
        this.defaultReporterFactory = defaultReporterFactory;
        this.testVmSystemProperties = testVmSystemProperties;
        this.testProvidingInputStream = testProvidingInputStream;
    }

    public void consumeLine( String s )
    {
        try
        {
            if ( s.length() == 0 )
            {
                return;
            }
            final byte operationId = (byte) s.charAt( 0 );
            int commma = s.indexOf( ",", 3 );
            if ( commma < 0 )
            {
                System.out.println( s );
                return;
            }
            final Integer channelNumber = Integer.parseInt( s.substring( 2, commma ), 16 );
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
                    byte[] bytes = new byte[remaining.length()];
                    int len = StringUtils.unescapeBytes( bytes, remaining );
                    getOrCreateConsoleOutputReceiver( channelNumber ).writeTestOutput( bytes, 0, len, true );
                    break;
                case ForkingRunListener.BOOTERCODE_STDERR:
                    bytes = new byte[remaining.length()];
                    len = StringUtils.unescapeBytes( bytes, remaining );
                    getOrCreateConsoleOutputReceiver( channelNumber ).writeTestOutput( bytes, 0, len, false );
                    break;
                case ForkingRunListener.BOOTERCODE_CONSOLE:
                    getOrCreateConsoleLogger( channelNumber ).info( createConsoleMessage( remaining ) );
                    break;
                case ForkingRunListener.BOOTERCODE_NEXT_TEST:
                    if ( null != testProvidingInputStream )
                    {
                        testProvidingInputStream.provideNewTest();
                    }
                    break;
                case ForkingRunListener.BOOTERCODE_ERROR:
                    errorInFork = deserializeStackStraceWriter( new StringTokenizer( remaining, "," ) );
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
            System.out.println( s );
        }
        catch ( ReporterException e )
        {
            throw new RuntimeException( e );
        }
    }

    public void consumeMultiLineContent( String s )
        throws IOException
    {
        BufferedReader stringReader = new BufferedReader( new StringReader( s ) );
        String s1;
        while ( ( s1 = stringReader.readLine() ) != null )
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
                tokens.hasMoreTokens() ? deserializeStackStraceWriter( tokens ) : null;

            return CategorizedReportEntry.reportEntry( source, name, group, stackTraceWriter, elapsed, message );
        }
        catch ( RuntimeException e )
        {
            throw new RuntimeException( untokenized, e );
        }
    }

    private StackTraceWriter deserializeStackStraceWriter( StringTokenizer tokens )
    {
        StackTraceWriter stackTraceWriter;
        String stackTraceMessage = nullableCsv( tokens.nextToken() );
        String smartStackTrace = nullableCsv( tokens.nextToken() );
        String stackTrace = tokens.hasMoreTokens() ? nullableCsv( tokens.nextToken() ) : null;
        stackTraceWriter =
            stackTrace != null ? new DeserializedStacktraceWriter( stackTraceMessage, smartStackTrace, stackTrace )
                            : null;
        return stackTraceWriter;
    }

    private String nullableCsv( String source )
    {
        if ( "null".equals( source ) )
        {
            return null;
        }
        return unescape( source );
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
    public RunListener getReporter( Integer channelNumber )
    {
        return testSetReporters.get( channelNumber );
    }

    private RunListener getOrCreateReporter( Integer channelNumber )
    {
        RunListener reporter = testSetReporters.get( channelNumber );
        if ( reporter == null )
        {
            reporter = defaultReporterFactory.createReporter();
            testSetReporters.put( channelNumber, reporter );
        }
        return reporter;
    }

    private ConsoleOutputReceiver getOrCreateConsoleOutputReceiver( Integer channelNumber )
    {
        return (ConsoleOutputReceiver) getOrCreateReporter( channelNumber );
    }

    private ConsoleLogger getOrCreateConsoleLogger( Integer channelNumber )
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
