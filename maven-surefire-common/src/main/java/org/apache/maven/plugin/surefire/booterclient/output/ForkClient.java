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
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.NotifiableTestStream;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.shared.utils.cli.StreamConsumer;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.StackTraceWriter;

import static java.lang.Integer.decode;
import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_BYE;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_CONSOLE;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_DEBUG;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_ERROR;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_NEXT_TEST;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_STDERR;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_STDOUT;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_STOP_ON_NEXT_TEST;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_SYSPROPS;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TEST_ASSUMPTIONFAILURE;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TEST_ERROR;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TEST_FAILED;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TEST_SKIPPED;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TEST_STARTING;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TEST_SUCCEEDED;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TESTSET_COMPLETED;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TESTSET_STARTING;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_WARNING;
import static org.apache.maven.surefire.booter.Shutdown.KILL;
import static org.apache.maven.surefire.report.CategorizedReportEntry.reportEntry;
import static org.apache.maven.surefire.util.internal.StringUtils.isNotBlank;
import static org.apache.maven.surefire.util.internal.StringUtils.unescapeBytes;
import static org.apache.maven.surefire.util.internal.StringUtils.unescapeString;

/**
 * Knows how to reconstruct *all* the state transmitted over stdout by the forked process.
 *
 * @author Kristian Rosenvold
 */
public class ForkClient
    implements StreamConsumer
{
    private static final long START_TIME_ZERO = 0L;
    private static final long START_TIME_NEGATIVE_TIMEOUT = -1L;

    private final ConcurrentMap<Integer, RunListener> testSetReporters;

    private final DefaultReporterFactory defaultReporterFactory;

    private final Properties testVmSystemProperties;

    private final NotifiableTestStream notifiableTestStream;

    /**
     * <t>testSetStartedAt</t> is set to non-zero after received
     * {@link org.apache.maven.surefire.booter.ForkingRunListener#BOOTERCODE_TESTSET_STARTING test-set}.
     */
    private final AtomicLong testSetStartedAt = new AtomicLong( START_TIME_ZERO );

    private final ConsoleLogger log;

    private volatile boolean saidGoodBye;

    private volatile StackTraceWriter errorInFork;

    private final Map<Integer, String> testsInProgressByChannel = Collections.synchronizedMap( new HashMap() );

    public ForkClient( DefaultReporterFactory defaultReporterFactory, Properties testVmSystemProperties,
                       NotifiableTestStream notifiableTestStream, ConsoleLogger log )
    {
        testSetReporters = new ConcurrentHashMap<Integer, RunListener>();
        this.defaultReporterFactory = defaultReporterFactory;
        this.testVmSystemProperties = testVmSystemProperties;
        this.notifiableTestStream = notifiableTestStream;
        this.log = log;
    }

    protected void stopOnNextTest()
    {
    }

    public void kill()
    {
        if ( !saidGoodBye )
        {
            notifiableTestStream.shutdown( KILL );
        }
    }

    /**
     * Called in concurrent Thread.
     */
    public final void tryToTimeout( long currentTimeMillis, int forkedProcessTimeoutInSeconds )
    {
        if ( forkedProcessTimeoutInSeconds > 0 )
        {
            final long forkedProcessTimeoutInMillis = 1000 * forkedProcessTimeoutInSeconds;
            final long startedAt = testSetStartedAt.get();
            if ( startedAt > START_TIME_ZERO && currentTimeMillis - startedAt >= forkedProcessTimeoutInMillis )
            {
                testSetStartedAt.set( START_TIME_NEGATIVE_TIMEOUT );
                notifiableTestStream.shutdown( KILL );
            }
        }
    }

    public final DefaultReporterFactory getDefaultReporterFactory()
    {
        return defaultReporterFactory;
    }

    public final void consumeLine( String s )
    {
        if ( isNotBlank( s ) )
        {
            processLine( s );
        }
    }

    private void setCurrentStartTime()
    {
        if ( testSetStartedAt.get() == START_TIME_ZERO ) // JIT can optimize <= no JNI call
        {
            // Not necessary to call JNI library library #currentTimeMillis
            // which may waste 10 - 30 machine cycles in callback. Callbacks should be fast.
            testSetStartedAt.compareAndSet( START_TIME_ZERO, currentTimeMillis() );
        }
    }

    public final boolean hadTimeout()
    {
        return testSetStartedAt.get() == START_TIME_NEGATIVE_TIMEOUT;
    }

    private void processLine( String s )
    {
        try
        {
            final byte operationId = (byte) s.charAt( 0 );
            int comma = s.indexOf( ",", 3 );
            if ( comma < 0 )
            {
                log.warning( s );
                return;
            }
            final int channelNumber = parseInt( s.substring( 2, comma ), 16 );
            int rest = s.indexOf( ",", comma );
            final String remaining = s.substring( rest + 1 );

            switch ( operationId )
            {
                case BOOTERCODE_TESTSET_STARTING:
                    getOrCreateReporter( channelNumber )
                            .testSetStarting( createReportEntry( remaining ) );
                    setCurrentStartTime();
                    break;
                case BOOTERCODE_TESTSET_COMPLETED:
                    getOrCreateReporter( channelNumber )
                            .testSetCompleted( createReportEntry( remaining ) );
                    break;
                case BOOTERCODE_TEST_STARTING:
                    testsInProgressByChannel.put( channelNumber, s.substring( 1 ) );
                    getOrCreateReporter( channelNumber )
                            .testStarting( createReportEntry( remaining ) );
                    break;
                case BOOTERCODE_TEST_SUCCEEDED:
                    testsInProgressByChannel.remove( channelNumber );
                    getOrCreateReporter( channelNumber )
                            .testSucceeded( createReportEntry( remaining ) );
                    break;
                case BOOTERCODE_TEST_FAILED:
                    testsInProgressByChannel.remove( channelNumber );
                    getOrCreateReporter( channelNumber )
                            .testFailed( createReportEntry( remaining ) );
                    break;
                case BOOTERCODE_TEST_SKIPPED:
                    getOrCreateReporter( channelNumber )
                            .testSkipped( createReportEntry( remaining ) );
                    break;
                case BOOTERCODE_TEST_ERROR:
                    testsInProgressByChannel.remove( channelNumber );
                    getOrCreateReporter( channelNumber )
                            .testError( createReportEntry( remaining ) );
                    break;
                case BOOTERCODE_TEST_ASSUMPTIONFAILURE:
                    testsInProgressByChannel.remove( channelNumber );
                    getOrCreateReporter( channelNumber )
                            .testAssumptionFailure( createReportEntry( remaining ) );
                    break;
                case BOOTERCODE_SYSPROPS:
                    int keyEnd = remaining.indexOf( "," );
                    StringBuilder key = new StringBuilder();
                    StringBuilder value = new StringBuilder();
                    unescapeString( key, remaining.substring( 0, keyEnd ) );
                    unescapeString( value, remaining.substring( keyEnd + 1 ) );
                    synchronized ( testVmSystemProperties )
                    {
                        testVmSystemProperties.put( key.toString(), value.toString() );
                    }
                    break;
                case BOOTERCODE_STDOUT:
                    writeTestOutput( channelNumber, remaining, true );
                    break;
                case BOOTERCODE_STDERR:
                    writeTestOutput( channelNumber, remaining, false );
                    break;
                case BOOTERCODE_CONSOLE:
                    getOrCreateConsoleLogger( channelNumber )
                            .info( createConsoleMessage( remaining ) );
                    break;
                case BOOTERCODE_NEXT_TEST:
                    notifiableTestStream.provideNewTest();
                    break;
                case BOOTERCODE_ERROR:
                    errorInFork = deserializeStackTraceWriter( new StringTokenizer( remaining, "," ) );
                    break;
                case BOOTERCODE_BYE:
                    saidGoodBye = true;
                    break;
                case BOOTERCODE_STOP_ON_NEXT_TEST:
                    stopOnNextTest();
                    break;
                case BOOTERCODE_DEBUG:
                    getOrCreateConsoleLogger( channelNumber )
                            .debug( createConsoleMessage( remaining ) );
                    break;
                case BOOTERCODE_WARNING:
                    getOrCreateConsoleLogger( channelNumber )
                            .warning( createConsoleMessage( remaining ) );
                    break;
                default:
                    log.warning( s );
            }
        }
        catch ( NumberFormatException e )
        {
            // SUREFIRE-859
            log.warning( s );
        }
        catch ( NoSuchElementException e )
        {
            // SUREFIRE-859
            log.warning( s );
        }
        catch ( ReporterException e )
        {
            throw new RuntimeException( e );
        }
    }

    private void writeTestOutput( final int channelNumber, final String remaining, final boolean isStdout )
    {
        int csNameEnd = remaining.indexOf( ',' );
        String charsetName = remaining.substring( 0, csNameEnd );
        String byteEncoded = remaining.substring( csNameEnd + 1 );
        ByteBuffer unescaped = unescapeBytes( byteEncoded, charsetName );

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

    public final void consumeMultiLineContent( String s )
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
            Integer elapsed = "null".equals( elapsedStr ) ? null : decode( elapsedStr );
            final StackTraceWriter stackTraceWriter =
                tokens.hasMoreTokens() ? deserializeStackTraceWriter( tokens ) : null;

            return reportEntry( source, name, group, stackTraceWriter, elapsed, message );
        }
        catch ( RuntimeException e )
        {
            throw new RuntimeException( untokenized, e );
        }
    }

    private StackTraceWriter deserializeStackTraceWriter( StringTokenizer tokens )
    {
        String stackTraceMessage = nullableCsv( tokens.nextToken() );
        String smartStackTrace = nullableCsv( tokens.nextToken() );
        String stackTrace = tokens.hasMoreTokens() ? nullableCsv( tokens.nextToken() ) : null;
        boolean hasTrace = stackTrace != null;
        return hasTrace ? new DeserializedStacktraceWriter( stackTraceMessage, smartStackTrace, stackTrace ) : null;
    }

    private String nullableCsv( String source )
    {
        return "null".equals( source ) ? null : unescape( source );
    }

    private String unescape( String source )
    {
        StringBuilder stringBuffer = new StringBuilder( source.length() );
        unescapeString( stringBuffer, source );
        return stringBuffer.toString();
    }

    /**
     * Used when getting reporters on the plugin side of a fork.
     *
     * @param channelNumber The logical channel number
     * @return A mock provider reporter
     */
    public final RunListener getReporter( int channelNumber )
    {
        return testSetReporters.get( channelNumber );
    }

    private RunListener getOrCreateReporter( int channelNumber )
    {
        RunListener reporter = testSetReporters.get( channelNumber );
        if ( reporter == null )
        {
            reporter = defaultReporterFactory.createReporter();
            RunListener old = testSetReporters.putIfAbsent( channelNumber, reporter );
            if ( old != null )
            {
                reporter = old;
            }
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
        // no op
    }

    public final boolean isSaidGoodBye()
    {
        return saidGoodBye;
    }

    public boolean notifyOfMissingByeIfTestsRunning()
    {
        log.warning( "Missing goodbye handling engaged." );
        if ( testsInProgressByChannel.isEmpty() )
        {
            return false;
        }
        for ( Integer channelNumber : testsInProgressByChannel.keySet() )
        {
            String thisTest = testsInProgressByChannel.get( channelNumber );
            processLine( (char) BOOTERCODE_TEST_ERROR + thisTest );
            processLine( (char) BOOTERCODE_TESTSET_COMPLETED + thisTest );
        }
        return true;
    }

    public final StackTraceWriter getErrorInFork()
    {
        return errorInFork;
    }

    public final boolean isErrorInFork()
    {
        return errorInFork != null;
    }
}
