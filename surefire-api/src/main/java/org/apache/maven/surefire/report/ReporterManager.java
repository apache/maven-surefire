package org.apache.maven.surefire.report;

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

import java.util.Iterator;
import java.util.List;

/**
 * A reporting front-end for providers.
 * <p/>
 * Synchronization/Threading note:
 * <p/>
 * This design is really only good for single-threaded test execution. Although it is currently
 * used by multi-threaded providers too, the design does not really make sense (and is probably buggy).
 * <p/>
 * This is because to get correct results, the client basically needs to do something like this:
 * synchronized( ReporterManger.getClass()){
 * reporterManager.runStarted()
 * reporterManager.testSetStarting()
 * reporterManager.testStarting()
 * reporterManager.testSucceeded()
 * reporterManager.testSetCompleted()
 * reporterManager.runCompleted()
 * }
 * <p/>
 * This is because the underlying providers are singletons and keep state, if you remove the outer synchronized
 * block, you may get mixups between results from different tests; although the end result (total test count etc)
 * should probably be correct.
 * <p/>
 * The solution to this problem involves making a clearer separation between test-result collection and reporting,
 * preferably removing singleton state approach out of the reporting interface.
 * <p/>
 * Please also note that the synchronization requirements of this interface severely limit the concurrency
 * potential of all the parallel surefire providers, especially when runnning non-io bound tests,
 */
public class ReporterManager
{
    private final RunStatistics runStatisticsForThis;

    private final MulticastingReporter multicastingReporter;

    private final SystemStreamCapturer consoleCapturer = new SystemStreamCapturer();

    public ReporterManager( List reports, RunStatistics runStatisticsForThis )
    {
        multicastingReporter = new MulticastingReporter( reports );
        this.runStatisticsForThis = runStatisticsForThis;
    }

    public synchronized void writeMessage( String message )
    {
        multicastingReporter.writeMessage( message );
    }
    
    public synchronized void writeConsoleMessage( String message )
    {
        multicastingReporter.writeConsoleMessage( message );
    }

    // ----------------------------------------------------------------------
    // Run
    // ----------------------------------------------------------------------

    public synchronized void runStarting()
    {
        multicastingReporter.runStarting();
    }

    public synchronized void runCompleted()
    {
        multicastingReporter.runCompleted();
        multicastingReporter.writeFooter( "" );
        multicastingReporter.writeFooter( "Results :" );
        multicastingReporter.writeFooter( "" );
        if ( runStatisticsForThis.hadFailures() )
        {
            multicastingReporter.writeFooter( "Failed tests: " );
            for ( Iterator iterator = this.runStatisticsForThis.getFailureSources().iterator(); iterator.hasNext(); )
            {
                multicastingReporter.writeFooter( "  " + iterator.next() );
            }
            multicastingReporter.writeFooter( "" );
        }
        if ( runStatisticsForThis.hadErrors() )
        {
            writeFooter( "Tests in error: " );
            for ( Iterator iterator = this.runStatisticsForThis.getErrorSources().iterator(); iterator.hasNext(); )
            {
                multicastingReporter.writeFooter( "  " + iterator.next() );
            }
            multicastingReporter.writeFooter( "" );
        }
        multicastingReporter.writeFooter( runStatisticsForThis.getSummary() );
        multicastingReporter.writeFooter( "" );
        consoleCapturer.restoreStreams();
    }

    public synchronized void writeFooter( String footer )
    {
        multicastingReporter.writeFooter( footer );
    }


    public synchronized void testSetStarting( ReportEntry report )
        throws ReporterException
    {
        multicastingReporter.testSetStarting( report );
    }

    public synchronized void testSetCompleted( ReportEntry report )
    {
        multicastingReporter.testSetCompleted( report );
    }

    // ----------------------------------------------------------------------
    // Test
    // ----------------------------------------------------------------------

    public synchronized void testStarting( ReportEntry report )
    {
        multicastingReporter.testStarting( report );
    }

    public synchronized void testSucceeded( ReportEntry report )
    {
        consoleCapturer.clearCapturedContent();
        runStatisticsForThis.incrementCompletedCount();
        multicastingReporter.testSucceeded( report );
    }

    public synchronized void testError( ReportEntry reportEntry )
    {
        testError( reportEntry, consoleCapturer.getStdOutLog(), consoleCapturer.getStdErrLog() );
    }

    public synchronized void testError( ReportEntry reportEntry, String stdOutLog, String stdErrLog )
    {
        multicastingReporter.testError( reportEntry, stdOutLog, stdErrLog );
        runStatisticsForThis.incrementErrorsCount();
        runStatisticsForThis.incrementCompletedCount();
        runStatisticsForThis.addErrorSource( reportEntry.getName() );
        consoleCapturer.clearCapturedContent();
    }

    public synchronized void testFailed( ReportEntry reportEntry )
    {
        testFailed( reportEntry, consoleCapturer.getStdOutLog(), consoleCapturer.getStdErrLog() );
    }


    public synchronized void testFailed( ReportEntry reportEntry, String stdOutLog, String stdErrLog )
    {
        multicastingReporter.testFailed( reportEntry, stdOutLog, stdErrLog );
        runStatisticsForThis.incrementFailureCount();
        runStatisticsForThis.incrementCompletedCount();
        runStatisticsForThis.addFailureSource( reportEntry.getName() );
        consoleCapturer.clearCapturedContent();
    }

    // ----------------------------------------------------------------------
    // Counters
    // ----------------------------------------------------------------------

    public synchronized void testSkipped( ReportEntry report )
    {
        consoleCapturer.clearCapturedContent();
        runStatisticsForThis.incrementSkippedCount();
        runStatisticsForThis.incrementCompletedCount();
        multicastingReporter.testSkipped( report );
    }

    public synchronized void reset()
    {
        multicastingReporter.reset();
    }

}
