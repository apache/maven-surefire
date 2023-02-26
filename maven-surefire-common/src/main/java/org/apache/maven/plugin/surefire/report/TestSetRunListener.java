package org.apache.maven.plugin.surefire.report;

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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.maven.plugin.surefire.runorder.StatisticsReporter;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.extensions.StatelessTestSetSummaryListener;
import org.apache.maven.surefire.extensions.TestOutputReportOperation;
import org.apache.maven.surefire.extensions.TestSetReportOperation;
import org.apache.maven.surefire.extensions.ConsoleOutputReportEventListener;
import org.apache.maven.surefire.extensions.TestReportOperation;
import org.apache.maven.surefire.extensions.StatelessReportEventListener;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoConsoleReportEventListener;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoFileReportEventListener;
import org.apache.maven.surefire.extensions.testoperations.TestOperation;

import static org.apache.maven.plugin.surefire.report.ReportEntryType.ERROR;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.FAILURE;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.SKIPPED;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.SUCCESS;
import static org.apache.maven.surefire.api.report.RunMode.RERUN_TEST_AFTER_FAILURE;

/**
 * Reports data for a single test set.
 * <br>
 *
 * @author Kristian Rosenvold
 */
public class TestSetRunListener
    implements TestReportListener<TestOutputReportEntry>
{
    private final Map<Integer, List<TestOperation<?>>> operationsPerSource = new HashMap<>();
    private final Map<Integer, List<TestOperation<?>>> rerunOperationsPerSource = new HashMap<>();

    private final ConsoleOutputReportEventListener testOutputReceiver;

    private final boolean briefOrPlainFormat;

    private final StatelessReportEventListener<WrappedReportEntry, TestSetStats> simpleXMLReporter;

    private final StatelessTestsetInfoConsoleReportEventListener<WrappedReportEntry, TestSetStats> consoleReporter;

    private final StatelessTestsetInfoFileReportEventListener<WrappedReportEntry, TestSetStats> fileReporter;

    private final StatisticsReporter statisticsReporter;

    private final StatelessTestSetSummaryListener testSetSummaryReport;

    private final Object lock;

    public TestSetRunListener( ReportersAggregator reporters, Object lock )
    {
        consoleReporter = reporters.getConsoleReporter();
        fileReporter = reporters.getFileReporter();
        statisticsReporter = reporters.getStatisticsReporter();
        simpleXMLReporter = reporters.getSimpleXMLReporter();
        testOutputReceiver = reporters.getTestOutputReceiver();
        briefOrPlainFormat = reporters.isBriefOrPlainFormat();
        testSetSummaryReport = reporters.getTestSetSummaryReporter();
        detailsForThis = new TestSetStats( reporters.isTrimStackTrace(), reporters.isPlainFormat() );
        this.lock = lock;
    }

    @Override
    public boolean isDebugEnabled()
    {
        return consoleReporter.getConsoleLogger().isDebugEnabled();
    }

    @Override
    public void debug( String message )
    {
        synchronized ( lock )
        {
            consoleReporter.getConsoleLogger().debug( trimTrailingNewLine( message ) );
        }
    }

    @Override
    public boolean isInfoEnabled()
    {
        return consoleReporter.getConsoleLogger().isInfoEnabled();
    }

    @Override
    public void info( String message )
    {
        synchronized ( lock )
        {
            consoleReporter.getConsoleLogger().info( trimTrailingNewLine( message ) );
        }
    }

    @Override
    public boolean isWarnEnabled()
    {
        return consoleReporter.getConsoleLogger().isWarnEnabled();
    }

    @Override
    public void warning( String message )
    {
        synchronized ( lock )
        {
            consoleReporter.getConsoleLogger().warning( trimTrailingNewLine( message ) );
        }
    }

    @Override
    public boolean isErrorEnabled()
    {
        return consoleReporter.getConsoleLogger().isErrorEnabled();
    }

    @Override
    public void error( String message )
    {
        synchronized ( lock )
        {
            consoleReporter.getConsoleLogger().error( trimTrailingNewLine( message ) );
        }
    }

    @Override
    public void error( String message, Throwable t )
    {
        synchronized ( lock )
        {
            consoleReporter.getConsoleLogger().error( trimTrailingNewLine( message ), t );
        }
    }

    @Override
    public void error( Throwable t )
    {
        synchronized ( lock )
        {
            consoleReporter.getConsoleLogger().error( t );
        }
    }

    @Override
    public void writeTestOutput( TestOutputReportEntry reportEntry )
    {
        try
        {
            synchronized ( lock )
            {
                addEntry( reportEntry.getSourceId(), reportEntry.getRunMode(), new TestOutputReportOperation( reportEntry ) );
                Utf8RecodingDeferredFileOutputStream stream = reportEntry.isStdOut() ? testStdOut : testStdErr;
                stream.write( reportEntry.getLog(), reportEntry.isNewLine() );
                testOutputReceiver.writeTestOutput( reportEntry );
            }
        }
        catch ( IOException e )
        {
            throw new UncheckedIOException( e );
        }
    }

    @Override
    public void testSetStarting( TestSetReportEntry report )
    {
        addEntry( report.getSourceId(), report.getRunMode(), new TestSetReportOperation( report ) );
        detailsForThis.testSetStart();
        consoleReporter.testSetStarting( report );
        testOutputReceiver.testSetStarting( report );
    }

    private void clearCapture()
    {
        testStdOut = initDeferred( "stdout" );
        testStdErr = initDeferred( "stderr" );
    }

    @Override
    public void testSetCompleted( TestSetReportEntry report )
    {
        addEntry( report.getSourceId(), report.getRunMode(), new TestSetReportOperation( report ) );
        final WrappedReportEntry wrap = wrapTestSet( report );
        final List<String> testResults =
                briefOrPlainFormat ? detailsForThis.getTestResults() : Collections.<String>emptyList();
        fileReporter.testSetCompleted( wrap, detailsForThis, testResults );
        simpleXMLReporter.testSetCompleted( wrap, detailsForThis );
        statisticsReporter.testSetCompleted();
        consoleReporter.testSetCompleted( wrap, detailsForThis, testResults );
        testOutputReceiver.testSetCompleted( wrap );
        consoleReporter.reset();

        wrap.getStdout().free();
        wrap.getStdErr().free();

        addTestMethodStats();
        detailsForThis.reset();
        clearCapture();
    }

    // ----------------------------------------------------------------------
    // Test callback methods:
    // ----------------------------------------------------------------------

    @Override
    public void testStarting( ReportEntry report )
    {
        addEntry( report.getSourceId(), report.getRunMode(), new TestReportOperation( report ) );
        detailsForThis.testStart();
    }

    @Override
    public void testSucceeded( ReportEntry reportEntry )
    {
        addEntry( reportEntry.getSourceId(), reportEntry.getRunMode(), new TestReportOperation( reportEntry ) );
        WrappedReportEntry wrapped = wrap( reportEntry, SUCCESS );
        detailsForThis.testSucceeded( wrapped );
        statisticsReporter.testSucceeded( reportEntry );
        clearCapture();
    }

    @Override
    public void testError( ReportEntry reportEntry )
    {
        addEntry( reportEntry.getSourceId(), reportEntry.getRunMode(), new TestReportOperation( reportEntry ) );
        WrappedReportEntry wrapped = wrap( reportEntry, ERROR );
        detailsForThis.testError( wrapped );
        statisticsReporter.testError( reportEntry );
        clearCapture();
    }

    @Override
    public void testFailed( ReportEntry reportEntry )
    {
        addEntry( reportEntry.getSourceId(), reportEntry.getRunMode(), new TestReportOperation( reportEntry ) );
        WrappedReportEntry wrapped = wrap( reportEntry, FAILURE );
        detailsForThis.testFailure( wrapped );
        statisticsReporter.testFailed( reportEntry );
        clearCapture();
    }

    // ----------------------------------------------------------------------
    // Counters
    // ----------------------------------------------------------------------

    @Override
    public void testSkipped( ReportEntry reportEntry )
    {
        addEntry( reportEntry.getSourceId(), reportEntry.getRunMode(), new TestReportOperation( reportEntry ) );
        WrappedReportEntry wrapped = wrap( reportEntry, SKIPPED );
        detailsForThis.testSkipped( wrapped );
        statisticsReporter.testSkipped( reportEntry );
        clearCapture();
    }

    @Override
    public void testExecutionSkippedByUser()
    {
    }

    @Override
    public void testAssumptionFailure( ReportEntry report )
    {
        addEntry( report.getSourceId(), report.getRunMode(), new TestReportOperation( report ) );
        testSkipped( report );
    }

    private WrappedReportEntry wrap( ReportEntry other, ReportEntryType reportEntryType )
    {
        int estimatedElapsed = 0;
        if ( reportEntryType != SKIPPED )
        {
            Integer etime = other.getElapsed();
            estimatedElapsed = etime == null ? detailsForThis.getElapsedSinceLastStart() : etime;
        }

        return new WrappedReportEntry( other, reportEntryType, estimatedElapsed, testStdOut, testStdErr );
    }

    private WrappedReportEntry wrapTestSet( TestSetReportEntry other )
    {
        return new WrappedReportEntry( other, null, other.getElapsed() != null
            ? other.getElapsed()
            : detailsForThis.getElapsedSinceTestSetStart(), testStdOut, testStdErr, other.getSystemProperties() );
    }

    public void close()
    {
        testOutputReceiver.close();
    }

    private void addTestMethodStats()
    {
        for ( WrappedReportEntry reportEntry : detailsForThis.getReportEntries() )
        {
            TestMethodStats methodStats =
                new TestMethodStats( reportEntry.getClassMethodName(), reportEntry.getReportEntryType(),
                                     reportEntry.getStackTraceWriter() );
            testMethodStats.add( methodStats );
        }
    }

    public Queue<TestMethodStats> getTestMethodStats()
    {
        return testMethodStats;
    }

    private void addEntry( Integer source, RunMode runMode, TestOperation<?> operation )
    {
        Map<Integer, List<TestOperation<?>>> sourceOperations =
            runMode == RERUN_TEST_AFTER_FAILURE ? rerunOperationsPerSource : operationsPerSource;
        sourceOperations.compute( source, ( k, v ) ->
        {
            List<TestOperation<?>> operations = v == null ? new ArrayList<>() : v;
            operations.add( operation );
            return operations;
        } );
    }

    private static String trimTrailingNewLine( final String message )
    {
        final int e = message == null ? 0 : lineBoundSymbolWidth( message );
        return message != null && e != 0 ? message.substring( 0, message.length() - e ) : message;
    }

    private static int lineBoundSymbolWidth( String message )
    {
        return message.endsWith( "\r\n" ) ? 2 : ( message.endsWith( "\n" ) || message.endsWith( "\r" ) ? 1 : 0 );
    }

    private static Utf8RecodingDeferredFileOutputStream initDeferred( String channel )
    {
        return new Utf8RecodingDeferredFileOutputStream( channel );
    }
}
