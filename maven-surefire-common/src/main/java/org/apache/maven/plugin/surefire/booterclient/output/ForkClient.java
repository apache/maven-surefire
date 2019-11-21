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
import org.apache.maven.surefire.shared.utils.cli.StreamConsumer;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.RunMode;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.report.TestSetReportEntry;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.unmodifiableMap;
import static org.apache.maven.surefire.booter.Shutdown.KILL;
import static org.apache.maven.surefire.report.CategorizedReportEntry.reportEntry;
import static org.apache.maven.surefire.util.internal.StringUtils.isBlank;
import static org.apache.maven.surefire.util.internal.StringUtils.isNotBlank;

// todo move to the same package with ForkStarter

/**
 * Knows how to reconstruct *all* the state transmitted over stdout by the forked process.
 *
 * @author Kristian Rosenvold
 */
public class ForkClient
     implements StreamConsumer
{
    private static final String PRINTABLE_JVM_NATIVE_STREAM = "Listening for transport dt_socket at address:";
    private static final long START_TIME_ZERO = 0L;
    private static final long START_TIME_NEGATIVE_TIMEOUT = -1L;

    private final DefaultReporterFactory defaultReporterFactory;

    private final Map<String, String> testVmSystemProperties = new ConcurrentHashMap<>();

    private final NotifiableTestStream notifiableTestStream;

    private final Queue<String> testsInProgress = new ConcurrentLinkedQueue<>();

    /**
     * <em>testSetStartedAt</em> is set to non-zero after received
     * {@link org.apache.maven.surefire.booter.ForkedChannelEncoder#testSetStarting(ReportEntry, boolean)}.
     */
    private final AtomicLong testSetStartedAt = new AtomicLong( START_TIME_ZERO );

    private final ForkedChannelDecoder decoder = new ForkedChannelDecoder();

    private final ConsoleLogger log;

    /**
     * prevents from printing same warning
     */
    private final AtomicBoolean printedErrorStream;

    private final int forkNumber;

    /**
     * Used by single Thread started by {@link ThreadedStreamConsumer} and therefore does not need to be volatile.
     */
    private final ForkedChannelDecoderErrorHandler errorHandler;

    private RunListener testSetReporter;

    /**
     * Written by one Thread and read by another: Main Thread and ForkStarter's Thread.
     */
    private volatile boolean saidGoodBye;

    private volatile StackTraceWriter errorInFork;

    public ForkClient( DefaultReporterFactory defaultReporterFactory, NotifiableTestStream notifiableTestStream,
                       ConsoleLogger log, AtomicBoolean printedErrorStream, int forkNumber )
    {
        this.defaultReporterFactory = defaultReporterFactory;
        this.notifiableTestStream = notifiableTestStream;
        this.log = log;
        this.printedErrorStream = printedErrorStream;
        this.forkNumber = forkNumber;
        decoder.setTestSetStartingListener( new TestSetStartingListener() );
        decoder.setTestSetCompletedListener( new TestSetCompletedListener() );
        decoder.setTestStartingListener( new TestStartingListener() );
        decoder.setTestSucceededListener( new TestSucceededListener() );
        decoder.setTestFailedListener( new TestFailedListener() );
        decoder.setTestSkippedListener( new TestSkippedListener() );
        decoder.setTestErrorListener( new TestErrorListener() );
        decoder.setTestAssumptionFailureListener( new TestAssumptionFailureListener() );
        decoder.setSystemPropertiesListener( new SystemPropertiesListener() );
        decoder.setStdOutListener( new StdOutListener() );
        decoder.setStdErrListener( new StdErrListener() );
        decoder.setConsoleInfoListener( new ConsoleListener() );
        decoder.setAcquireNextTestListener( new AcquireNextTestListener() );
        decoder.setConsoleErrorListener( new ErrorListener() );
        decoder.setByeListener( new ByeListener() );
        decoder.setStopOnNextTestListener( new StopOnNextTestListener() );
        decoder.setConsoleDebugListener( new DebugListener() );
        decoder.setConsoleWarningListener( new WarningListener() );
        errorHandler = new ErrorHandler();
    }

    private final class ErrorHandler implements ForkedChannelDecoderErrorHandler
    {
        @Override
        public void handledError( String line, Throwable e )
        {
            logStreamWarning( line, e );
        }
    }

    private final class TestSetStartingListener
            implements ForkedProcessReportEventListener<TestSetReportEntry>
    {
        @Override
        public void handle( RunMode runMode, TestSetReportEntry reportEntry )
        {
            getTestSetReporter().testSetStarting( reportEntry );
            setCurrentStartTime();
        }
    }

    private final class TestSetCompletedListener
            implements ForkedProcessReportEventListener<TestSetReportEntry>
    {
        @Override
        public void handle( RunMode runMode, TestSetReportEntry reportEntry )
        {
            testsInProgress.clear();
            TestSetReportEntry entry = reportEntry( reportEntry.getSourceName(), reportEntry.getSourceText(),
                    reportEntry.getName(), reportEntry.getNameText(),
                    reportEntry.getGroup(), reportEntry.getStackTraceWriter(), reportEntry.getElapsed(),
                    reportEntry.getMessage(), getTestVmSystemProperties() );
            getTestSetReporter().testSetCompleted( entry );
        }
    }

    private final class TestStartingListener implements ForkedProcessReportEventListener
    {
        @Override
        public void handle( RunMode runMode, ReportEntry reportEntry )
        {
            testsInProgress.offer( reportEntry.getSourceName() );
            getTestSetReporter().testStarting( reportEntry );
        }
    }

    private final class TestSucceededListener implements ForkedProcessReportEventListener
    {
        @Override
        public void handle( RunMode runMode, ReportEntry reportEntry )
        {
            testsInProgress.remove( reportEntry.getSourceName() );
            getTestSetReporter().testSucceeded( reportEntry );
        }
    }

    private final class TestFailedListener implements ForkedProcessReportEventListener
    {
        @Override
        public void handle( RunMode runMode, ReportEntry reportEntry )
        {
            testsInProgress.remove( reportEntry.getSourceName() );
            getTestSetReporter().testFailed( reportEntry );
        }
    }

    private final class TestSkippedListener implements ForkedProcessReportEventListener
    {
        @Override
        public void handle( RunMode runMode, ReportEntry reportEntry )
        {
            testsInProgress.remove( reportEntry.getSourceName() );
            getTestSetReporter().testSkipped( reportEntry );
        }
    }

    private final class TestErrorListener implements ForkedProcessReportEventListener
    {
        @Override
        public void handle( RunMode runMode, ReportEntry reportEntry )
        {
            testsInProgress.remove( reportEntry.getSourceName() );
            getTestSetReporter().testError( reportEntry );
        }
    }

    private final class TestAssumptionFailureListener implements ForkedProcessReportEventListener
    {
        @Override
        public void handle( RunMode runMode, ReportEntry reportEntry )
        {
            testsInProgress.remove( reportEntry.getSourceName() );
            getTestSetReporter().testAssumptionFailure( reportEntry );
        }
    }

    private final class SystemPropertiesListener implements ForkedProcessPropertyEventListener
    {
        @Override
        public void handle( RunMode runMode, String key, String value )
        {
            testVmSystemProperties.put( key, value );
        }
    }

    private final class StdOutListener implements ForkedProcessStandardOutErrEventListener
    {
        @Override
        public void handle( RunMode runMode, String output, boolean newLine )
        {
            writeTestOutput( output, newLine, true );
        }
    }

    private final class StdErrListener implements ForkedProcessStandardOutErrEventListener
    {
        @Override
        public void handle( RunMode runMode, String output, boolean newLine )
        {
            writeTestOutput( output, newLine, false );
        }
    }

    private final class ConsoleListener implements ForkedProcessStringEventListener
    {
        @Override
        public void handle( String msg )
        {
            getOrCreateConsoleLogger()
                    .info( msg );
        }
    }

    private final class AcquireNextTestListener implements ForkedProcessEventListener
    {
        @Override
        public void handle()
        {
            notifiableTestStream.provideNewTest();
        }
    }

    private class ErrorListener implements ForkedProcessStackTraceEventListener
    {
        @Override
        public void handle( String msg, String smartStackTrace, String stackTrace )
        {
            if ( errorInFork == null )
            {
                errorInFork = deserializeStackTraceWriter( msg, smartStackTrace, stackTrace );
                if ( msg != null )
                {
                    getOrCreateConsoleLogger()
                            .error( msg );
                }
            }
            dumpToLoFile( msg, null );
        }
    }

    private final class ByeListener implements ForkedProcessEventListener
    {
        @Override
        public void handle()
        {
            saidGoodBye = true;
            notifiableTestStream.acknowledgeByeEventReceived();
        }
    }

    private final class StopOnNextTestListener implements ForkedProcessEventListener
    {
        @Override
        public void handle()
        {
            stopOnNextTest();
        }
    }

    private final class DebugListener implements ForkedProcessStringEventListener
    {
        @Override
        public void handle( String msg )
        {
            getOrCreateConsoleLogger()
                    .debug( msg );
        }
    }

    private final class WarningListener implements ForkedProcessStringEventListener
    {
        @Override
        public void handle( String msg )
        {
            getOrCreateConsoleLogger()
                    .warning( msg );
        }
    }

    /**
     * Overridden by a subclass, see {@link org.apache.maven.plugin.surefire.booterclient.ForkStarter}.
     */
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
     * Will shutdown if timeout was reached.
     *
     * @param currentTimeMillis    current time in millis seconds
     * @param forkedProcessTimeoutInSeconds timeout in seconds given by MOJO
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

    @Override
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
        decoder.handleEvent( event, errorHandler );
    }

    private File dumpToLoFile( String msg, Throwable e )
    {
        File reportsDir = defaultReporterFactory.getReportsDirectory();
        InPluginProcessDumpSingleton util = InPluginProcessDumpSingleton.getSingleton();
        return e == null
                ? util.dumpStreamText( msg, reportsDir, forkNumber )
                : util.dumpStreamException( e, msg, reportsDir, forkNumber );
    }

    private void logStreamWarning( String event, Throwable e )
    {
        if ( event == null || !event.contains( PRINTABLE_JVM_NATIVE_STREAM ) )
        {
            String msg = "Corrupted STDOUT by directly writing to native stream in forked JVM " + forkNumber + ".";
            File dump = dumpToLoFile( msg + " Stream '" + event + "'.", e );

            if ( printedErrorStream.compareAndSet( false, true ) )
            {
                log.warning( msg + " See FAQ web page and the dump file " + dump.getAbsolutePath() );
            }

            if ( log.isDebugEnabled() && event != null )
            {
                log.debug( event );
            }
        }
        else
        {
            if ( log.isDebugEnabled() )
            {
                log.debug( event );
            }
            else if ( log.isInfoEnabled() )
            {
                log.info( event );
            }
            else
            {
                // In case of debugging forked JVM, see PRINTABLE_JVM_NATIVE_STREAM.
                System.out.println( event );
            }
        }
    }

    private void writeTestOutput( String output, boolean newLine, boolean isStdout )
    {
        getOrCreateConsoleOutputReceiver()
                .writeTestOutput( output, newLine, isStdout );
    }

    public final void consumeMultiLineContent( String s )
            throws IOException
    {
        if ( isBlank( s ) )
        {
            logStreamWarning( s, null );
        }
        else
        {
            BufferedReader stringReader = new BufferedReader( new StringReader( s ) );
            for ( String s1 = stringReader.readLine(); s1 != null; s1 = stringReader.readLine() )
            {
                consumeLine( s1 );
            }
        }
    }

    public final Map<String, String> getTestVmSystemProperties()
    {
        return unmodifiableMap( testVmSystemProperties );
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
        return new TreeSet<>( testsInProgress );
    }

    public boolean hasTestsInProgress()
    {
        return !testsInProgress.isEmpty();
    }

    private StackTraceWriter deserializeStackTraceWriter( String stackTraceMessage,
                                                          String smartStackTrace, String stackTrace )
    {
        boolean hasTrace = stackTrace != null;
        return hasTrace ? new DeserializedStacktraceWriter( stackTraceMessage, smartStackTrace, stackTrace ) : null;
    }
}
