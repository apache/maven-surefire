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
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.api.booter.MasterProcessChannelEncoder;
import org.apache.maven.surefire.api.report.TestOutputReceiver;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.RunListener;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.TestSetReportEntry;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.unmodifiableMap;
import static org.apache.maven.surefire.api.booter.Shutdown.KILL;
import static org.apache.maven.surefire.api.report.CategorizedReportEntry.reportEntry;

// todo move to the same package with ForkStarter

/**
 * Knows how to reconstruct *all* the state transmitted over stdout by the forked process.
 *
 * @author Kristian Rosenvold
 */
public final class ForkClient
    implements EventHandler<Event>
{
    private static final long START_TIME_ZERO = 0L;
    private static final long START_TIME_NEGATIVE_TIMEOUT = -1L;

    private final DefaultReporterFactory defaultReporterFactory;

    private final Map<String, String> testVmSystemProperties = new ConcurrentHashMap<>();

    private final NotifiableTestStream notifiableTestStream;

    private final Queue<String> testsInProgress = new ConcurrentLinkedQueue<>();

    /**
     * <em>testSetStartedAt</em> is set to non-zero after received
     * {@link MasterProcessChannelEncoder#testSetStarting(TestSetReportEntry, boolean)}.
     */
    private final AtomicLong testSetStartedAt = new AtomicLong( START_TIME_ZERO );

    private final ForkedProcessEventNotifier notifier = new ForkedProcessEventNotifier();

    private final int forkNumber;

    private volatile TestReportListener<TestOutputReportEntry> testSetReporter;

    /**
     * Written by one Thread and read by another: Main Thread and ForkStarter's Thread.
     */
    private volatile boolean saidGoodBye;

    private volatile StackTraceWriter errorInFork;

    public ForkClient( DefaultReporterFactory defaultReporterFactory,
                       NotifiableTestStream notifiableTestStream,
                       int forkNumber )
    {
        this.defaultReporterFactory = defaultReporterFactory;
        this.notifiableTestStream = notifiableTestStream;
        this.forkNumber = forkNumber;
        notifier.setTestSetStartingListener( new TestSetStartingListener() );
        notifier.setTestSetCompletedListener( new TestSetCompletedListener() );
        notifier.setTestStartingListener( new TestStartingListener() );
        notifier.setTestSucceededListener( new TestSucceededListener() );
        notifier.setTestFailedListener( new TestFailedListener() );
        notifier.setTestSkippedListener( new TestSkippedListener() );
        notifier.setTestErrorListener( new TestErrorListener() );
        notifier.setTestAssumptionFailureListener( new TestAssumptionFailureListener() );
        notifier.setSystemPropertiesListener( new SystemPropertiesListener() );
        notifier.setStdOutListener( new StdOutListener() );
        notifier.setStdErrListener( new StdErrListener() );
        notifier.setConsoleInfoListener( new ConsoleListener() );
        notifier.setAcquireNextTestListener( new AcquireNextTestListener() );
        notifier.setConsoleErrorListener( new ErrorListener() );
        notifier.setByeListener( new ByeListener() );
        notifier.setConsoleDebugListener( new DebugListener() );
        notifier.setConsoleWarningListener( new WarningListener() );
        notifier.setExitErrorEventListener( new ExitErrorEventListener() );
    }

    public void setStopOnNextTestListener( ForkedProcessEventListener listener )
    {
        notifier.setStopOnNextTestListener( listener );
    }

    private final class TestSetStartingListener
            implements ForkedProcessReportEventListener<TestSetReportEntry>
    {
        @Override
        public void handle( TestSetReportEntry reportEntry )
        {
            getTestSetReporter().testSetStarting( reportEntry );
            setCurrentStartTime();
        }
    }

    private final class TestSetCompletedListener
            implements ForkedProcessReportEventListener<TestSetReportEntry>
    {
        @Override
        public void handle( TestSetReportEntry reportEntry )
        {
            testsInProgress.clear();
            TestSetReportEntry entry = reportEntry( reportEntry.getRunMode(), reportEntry.getTestRunId(),
                reportEntry.getSourceName(), reportEntry.getSourceText(),
                reportEntry.getName(), reportEntry.getNameText(),
                reportEntry.getGroup(), reportEntry.getStackTraceWriter(), reportEntry.getElapsed(),
                reportEntry.getMessage(), getTestVmSystemProperties() );
            getTestSetReporter().testSetCompleted( entry );
        }
    }

    private final class TestStartingListener implements ForkedProcessReportEventListener<ReportEntry>
    {
        @Override
        public void handle( ReportEntry reportEntry )
        {
            testsInProgress.offer( reportEntry.getSourceName() );
            getTestSetReporter().testStarting( reportEntry );
        }
    }

    private final class TestSucceededListener implements ForkedProcessReportEventListener<ReportEntry>
    {
        @Override
        public void handle( ReportEntry reportEntry )
        {
            testsInProgress.remove( reportEntry.getSourceName() );
            getTestSetReporter().testSucceeded( reportEntry );
        }
    }

    private final class TestFailedListener implements ForkedProcessReportEventListener<ReportEntry>
    {
        @Override
        public void handle( ReportEntry reportEntry )
        {
            testsInProgress.remove( reportEntry.getSourceName() );
            getTestSetReporter().testFailed( reportEntry );
        }
    }

    private final class TestSkippedListener implements ForkedProcessReportEventListener<ReportEntry>
    {
        @Override
        public void handle( ReportEntry reportEntry )
        {
            testsInProgress.remove( reportEntry.getSourceName() );
            getTestSetReporter().testSkipped( reportEntry );
        }
    }

    private final class TestErrorListener implements ForkedProcessReportEventListener<ReportEntry>
    {
        @Override
        public void handle( ReportEntry reportEntry )
        {
            testsInProgress.remove( reportEntry.getSourceName() );
            getTestSetReporter().testError( reportEntry );
        }
    }

    private final class TestAssumptionFailureListener implements ForkedProcessReportEventListener<ReportEntry>
    {
        @Override
        public void handle( ReportEntry reportEntry )
        {
            testsInProgress.remove( reportEntry.getSourceName() );
            getTestSetReporter().testAssumptionFailure( reportEntry );
        }
    }

    private final class SystemPropertiesListener implements ForkedProcessPropertyEventListener
    {
        @Override
        public void handle( String key, String value, RunMode runMode, Long testRunId )
        {
            testVmSystemProperties.put( key, value );
        }
    }

    private final class StdOutListener implements ForkedProcessStandardOutErrEventListener
    {
        @Override
        public void handle( String output, boolean newLine, RunMode runMode, Long testRunId )
        {
            writeTestOutput( output, true, newLine, runMode, testRunId );
        }
    }

    private final class StdErrListener implements ForkedProcessStandardOutErrEventListener
    {
        @Override
        public void handle( String output, boolean newLine, RunMode runMode, Long testRunId )
        {
            writeTestOutput( output, false, newLine, runMode, testRunId );
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
        public void handle( @Nonnull StackTraceWriter stackTrace )
        {
            String msg = stackTrace.getThrowable().getMessage();
            if ( errorInFork == null )
            {
                errorInFork = stackTrace.writeTraceToString() != null ? stackTrace : null;
                if ( msg != null )
                {
                    getOrCreateConsoleLogger()
                            .error( msg );
                }
            }
            dumpToLoFile( msg );
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

    private final class ExitErrorEventListener implements ForkedProcessExitErrorListener
    {
        @Override
        public void handle( StackTraceWriter stackTrace )
        {
            getOrCreateConsoleLogger()
                .error( "The surefire booter JVM" + forkNumber + " was interrupted and exits." );
        }
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
    public void tryToTimeout( long currentTimeMillis, int forkedProcessTimeoutInSeconds )
    {
        if ( forkedProcessTimeoutInSeconds > 0 )
        {
            final long forkedProcessTimeoutInMillis = 1000L * forkedProcessTimeoutInSeconds;
            final long startedAt = testSetStartedAt.get();
            if ( startedAt > START_TIME_ZERO && currentTimeMillis - startedAt >= forkedProcessTimeoutInMillis )
            {
                testSetStartedAt.set( START_TIME_NEGATIVE_TIMEOUT );
                notifiableTestStream.shutdown( KILL );
            }
        }
    }

    public DefaultReporterFactory getDefaultReporterFactory()
    {
        return defaultReporterFactory;
    }

    @Override
    public void handleEvent( @Nonnull Event event )
    {
        notifier.notifyEvent( event );
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

    public boolean hadTimeout()
    {
        return testSetStartedAt.get() == START_TIME_NEGATIVE_TIMEOUT;
    }

    /**
     * Only {@link #getConsoleOutputReceiver()} may call this method in another Thread.
     */
    private TestReportListener<TestOutputReportEntry> getTestSetReporter()
    {
        if ( testSetReporter == null )
        {
            synchronized ( this )
            {
                if ( testSetReporter == null )
                {
                    testSetReporter = defaultReporterFactory.createTestReportListener();
                }
            }
        }
        return testSetReporter;
    }

    void dumpToLoFile( String msg )
    {
        File reportsDir = defaultReporterFactory.getReportsDirectory();
        InPluginProcessDumpSingleton util = InPluginProcessDumpSingleton.getSingleton();
        util.dumpStreamText( msg, reportsDir, forkNumber );
    }

    private void writeTestOutput( String output, boolean isStdout, boolean newLine, RunMode runMode, Long testRunId )
    {
        getConsoleOutputReceiver()
                .writeTestOutput( new TestOutputReportEntry( output, isStdout, newLine, runMode, testRunId ) );
    }

    public Map<String, String> getTestVmSystemProperties()
    {
        return unmodifiableMap( testVmSystemProperties );
    }

    /**
     * Used when getting reporters on the plugin side of a fork.
     * Used by testing purposes only. May not be volatile variable.
     *
     * @return A mock provider reporter
     */
    public RunListener getReporter()
    {
        return getTestSetReporter();
    }

    public TestOutputReceiver<TestOutputReportEntry> getConsoleOutputReceiver()
    {
        return getTestSetReporter();
    }

    private ConsoleLogger getOrCreateConsoleLogger()
    {
        return getTestSetReporter();
    }

    public void close( boolean hadTimeout )
    {
        // no op
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

    public Set<String> testsInProgress()
    {
        return new TreeSet<>( testsInProgress );
    }

    public boolean hasTestsInProgress()
    {
        return !testsInProgress.isEmpty();
    }
}
