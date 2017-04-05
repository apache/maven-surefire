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

import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.NotifiableTestStream;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.shared.utils.cli.StreamConsumer;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.StackTraceWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Integer.decode;
import static java.lang.System.currentTimeMillis;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_BYE;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_CONSOLE;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_DEBUG;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_ERROR;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_NEXT_TEST;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_STDERR;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_STDOUT;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_STOP_ON_NEXT_TEST;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_SYSPROPS;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TESTSET_COMPLETED;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TESTSET_STARTING;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TEST_ASSUMPTIONFAILURE;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TEST_ERROR;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TEST_FAILED;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TEST_SKIPPED;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TEST_STARTING;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_TEST_SUCCEEDED;
import static org.apache.maven.surefire.booter.ForkingRunListener.BOOTERCODE_WARNING;
import static org.apache.maven.surefire.booter.Shutdown.KILL;
import static org.apache.maven.surefire.report.CategorizedReportEntry.reportEntry;
import static org.apache.maven.surefire.util.internal.StringUtils.isNotBlank;
import static org.apache.maven.surefire.util.internal.StringUtils.unescapeBytes;
import static org.apache.maven.surefire.util.internal.StringUtils.unescapeString;

// todo move to the same package with ForkStarter

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

    private final DefaultReporterFactory defaultReporterFactory;

    private final Properties testVmSystemProperties;

    private final NotifiableTestStream notifiableTestStream;

    private final Queue<String> testsInProgress = new ConcurrentLinkedQueue<String>();

    /**
     * <t>testSetStartedAt</t> is set to non-zero after received
     * {@link org.apache.maven.surefire.booter.ForkingRunListener#BOOTERCODE_TESTSET_STARTING test-set}.
     */
    private final AtomicLong testSetStartedAt = new AtomicLong( START_TIME_ZERO );

    private final ConsoleLogger log;

    private RunListener testSetReporter;

    private volatile boolean saidGoodBye;

    private volatile StackTraceWriter errorInFork;

    private volatile int forkNumber;

    // prevents from printing same warning
    private boolean printedErrorStream;

    public ForkClient( DefaultReporterFactory defaultReporterFactory, Properties testVmSystemProperties,
                       NotifiableTestStream notifiableTestStream, ConsoleLogger log )
    {
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

    private RunListener getTestSetReporter()
    {
        if ( testSetReporter == null )
        {
            testSetReporter = defaultReporterFactory.createReporter();
        }
        return testSetReporter;
    }

    private void processLine( String event )
    {
        final OperationalData op;
        try
        {
            op = new OperationalData( event );
        }
        catch ( RuntimeException e )
        {
            logStreamWarning( e, event );
            return;
        }
        final String remaining = op.getData();
        switch ( op.getOperationId() )
        {
            case BOOTERCODE_TESTSET_STARTING:
                getTestSetReporter().testSetStarting( createReportEntry( remaining ) );
                setCurrentStartTime();
                break;
            case BOOTERCODE_TESTSET_COMPLETED:
                testsInProgress.clear();

                getTestSetReporter().testSetCompleted( createReportEntry( remaining ) );
                break;
            case BOOTERCODE_TEST_STARTING:
                ReportEntry reportEntry = createReportEntry( remaining );
                testsInProgress.offer( reportEntry.getSourceName() );

                getTestSetReporter().testStarting( createReportEntry( remaining ) );
                break;
            case BOOTERCODE_TEST_SUCCEEDED:
                reportEntry = createReportEntry( remaining );
                testsInProgress.remove( reportEntry.getSourceName() );

                getTestSetReporter().testSucceeded( createReportEntry( remaining ) );
                break;
            case BOOTERCODE_TEST_FAILED:
                reportEntry = createReportEntry( remaining );
                testsInProgress.remove( reportEntry.getSourceName() );

                getTestSetReporter().testFailed( createReportEntry( remaining ) );
                break;
            case BOOTERCODE_TEST_SKIPPED:
                reportEntry = createReportEntry( remaining );
                testsInProgress.remove( reportEntry.getSourceName() );

                getTestSetReporter().testSkipped( createReportEntry( remaining ) );
                break;
            case BOOTERCODE_TEST_ERROR:
                reportEntry = createReportEntry( remaining );
                testsInProgress.remove( reportEntry.getSourceName() );

                getTestSetReporter().testError( createReportEntry( remaining ) );
                break;
            case BOOTERCODE_TEST_ASSUMPTIONFAILURE:
                reportEntry = createReportEntry( remaining );
                testsInProgress.remove( reportEntry.getSourceName() );

                getTestSetReporter().testAssumptionFailure( createReportEntry( remaining ) );
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
                writeTestOutput( remaining, true );
                break;
            case BOOTERCODE_STDERR:
                writeTestOutput( remaining, false );
                break;
            case BOOTERCODE_CONSOLE:
                getOrCreateConsoleLogger()
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
                notifiableTestStream.acknowledgeByeEventReceived();
                break;
            case BOOTERCODE_STOP_ON_NEXT_TEST:
                stopOnNextTest();
                break;
            case BOOTERCODE_DEBUG:
                getOrCreateConsoleLogger()
                        .debug( createConsoleMessage( remaining ) );
                break;
            case BOOTERCODE_WARNING:
                getOrCreateConsoleLogger()
                        .warning( createConsoleMessage( remaining ) );
                break;
            default:
                logStreamWarning( event );
        }
    }

    private void logStreamWarning( String event )
    {
        logStreamWarning( null, event );
    }

    private void logStreamWarning( Throwable e, String event )
    {
        final String msg = "Corrupted stdin stream in forked JVM " + forkNumber + ".";
        final InPluginProcessDumpSingleton util = InPluginProcessDumpSingleton.getSingleton();
        final File dump =
                e == null ? util.dumpText( msg + " Stream '" + event + "'.", defaultReporterFactory, forkNumber )
                        : util.dumpException( e, msg + " Stream '" + event + "'.", defaultReporterFactory, forkNumber );

        if ( !printedErrorStream )
        {
            printedErrorStream = true;
            log.warning( msg + " See the dump file " + dump.getAbsolutePath() );
        }
    }

    private void writeTestOutput( String remaining, boolean isStdout )
    {
        int csNameEnd = remaining.indexOf( ',' );
        String charsetName = remaining.substring( 0, csNameEnd );
        String byteEncoded = remaining.substring( csNameEnd + 1 );
        ByteBuffer unescaped = unescapeBytes( byteEncoded, charsetName );

        if ( unescaped.hasArray() )
        {
            byte[] convertedBytes = unescaped.array();
            getOrCreateConsoleOutputReceiver()
                .writeTestOutput( convertedBytes, unescaped.position(), unescaped.remaining(), isStdout );
        }
        else
        {
            byte[] convertedBytes = new byte[unescaped.remaining()];
            unescaped.get( convertedBytes, 0, unescaped.remaining() );
            getOrCreateConsoleOutputReceiver()
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
     * Used by testing purposes only. May not be volatile variable.
     *
     * @return A mock provider reporter
     */
    public final RunListener getReporter()
    {
        return getTestSetReporter();
    }

    private ConsoleOutputReceiver getOrCreateConsoleOutputReceiver()
    {
        return (ConsoleOutputReceiver) getTestSetReporter();
    }

    private ConsoleLogger getOrCreateConsoleLogger()
    {
        return (ConsoleLogger) getTestSetReporter();
    }

    public void close( boolean hadTimeout )
    {
        // no op
    }

    public final boolean isSaidGoodBye()
    {
        return saidGoodBye;
    }

    public final StackTraceWriter getErrorInFork()
    {
        return errorInFork;
    }

    public final boolean isErrorInFork()
    {
        return errorInFork != null;
    }

    public Set<String> testsInProgress()
    {
        return new TreeSet<String>( testsInProgress );
    }

    public boolean hasTestsInProgress()
    {
        return !testsInProgress.isEmpty();
    }

    public void setForkNumber( int forkNumber )
    {
        assert this.forkNumber == 0;
        this.forkNumber = forkNumber;
    }

    private static final class OperationalData
    {
        private final byte operationId;
        private final String data;

        OperationalData( String event )
        {
            operationId = (byte) event.charAt( 0 );
            int comma = event.indexOf( ",", 3 );
            if ( comma < 0 )
            {
                throw new IllegalArgumentException( "Stream stdin corrupted. Expected comma after third character "
                                                            + "in command '" + event + "'." );
            }
            int rest = event.indexOf( ",", comma );
            data = event.substring( rest + 1 );
        }

        byte getOperationId()
        {
            return operationId;
        }

        String getData()
        {
            return data;
        }
    }
}
