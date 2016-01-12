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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.surefire.runorder.StatisticsReporter;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;

/**
 * Reports data for a single test set.
 * <p/>
 *
 * @author Kristian Rosenvold
 */
public class TestSetRunListener
    implements RunListener, ConsoleOutputReceiver, ConsoleLogger
{
    private final TestSetStats detailsForThis;

    private List<TestMethodStats> testMethodStats;

    private Utf8RecodingDeferredFileOutputStream testStdOut = initDeferred( "stdout" );

    private Utf8RecodingDeferredFileOutputStream testStdErr = initDeferred( "stderr" );

    private Utf8RecodingDeferredFileOutputStream initDeferred( String channel )
    {
        return new Utf8RecodingDeferredFileOutputStream( channel );
    }

    private final TestcycleConsoleOutputReceiver consoleOutputReceiver;

    private final boolean briefOrPlainFormat;

    private final StatelessXmlReporter simpleXMLReporter;

    private final ConsoleReporter consoleReporter;

    private final FileReporter fileReporter;

    private final StatisticsReporter statisticsReporter;

    @SuppressWarnings( "checkstyle:parameternumber" )
    public TestSetRunListener( ConsoleReporter consoleReporter, FileReporter fileReporter,
                               StatelessXmlReporter simpleXMLReporter,
                               TestcycleConsoleOutputReceiver consoleOutputReceiver,
                               StatisticsReporter statisticsReporter, boolean trimStackTrace,
                               boolean isPlainFormat, boolean briefOrPlainFormat )
    {
        this.consoleReporter = consoleReporter;
        this.fileReporter = fileReporter;
        this.statisticsReporter = statisticsReporter;
        this.simpleXMLReporter = simpleXMLReporter;
        this.consoleOutputReceiver = consoleOutputReceiver;
        this.briefOrPlainFormat = briefOrPlainFormat;
        this.detailsForThis = new TestSetStats( trimStackTrace, isPlainFormat );
        this.testMethodStats = new ArrayList<TestMethodStats>(  );
    }

    public void info( String message )
    {
        if ( consoleReporter != null )
        {
            consoleReporter.writeMessage( message );
        }
    }

    public void writeTestOutput( byte[] buf, int off, int len, boolean stdout )
    {
        try
        {
            if ( stdout )
            {
                testStdOut.write( buf, off, len );
            }
            else
            {
                testStdErr.write( buf, off, len );
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        consoleOutputReceiver.writeTestOutput( buf, off, len, stdout );
    }

    public void testSetStarting( ReportEntry report )
    {
        detailsForThis.testSetStart();
        if ( consoleReporter != null )
        {
            consoleReporter.testSetStarting( report );
        }
        consoleOutputReceiver.testSetStarting( report );
    }

    public void clearCapture()
    {
        testStdOut = initDeferred( "stdout" );
        testStdErr = initDeferred( "stderr" );
    }

    public void testSetCompleted( ReportEntry report )
    {
        WrappedReportEntry wrap = wrapTestSet( report );
        List<String> testResults = briefOrPlainFormat ? detailsForThis.getTestResults() : null;
        if ( fileReporter != null )
        {
            fileReporter.testSetCompleted( wrap, detailsForThis, testResults );
        }
        if ( simpleXMLReporter != null )
        {
            simpleXMLReporter.testSetCompleted( wrap, detailsForThis );
        }
        if ( statisticsReporter != null )
        {
            statisticsReporter.testSetCompleted();
        }
        if ( consoleReporter != null )
        {
            consoleReporter.testSetCompleted( wrap, detailsForThis, testResults );
        }
        consoleOutputReceiver.testSetCompleted( wrap );
        if ( consoleReporter != null )
        {
            consoleReporter.reset();
        }

        wrap.getStdout().free();
        wrap.getStdErr().free();

        addTestMethodStats();
        detailsForThis.reset();

    }

    // ----------------------------------------------------------------------
    // Test
    // ----------------------------------------------------------------------

    public void testStarting( ReportEntry report )
    {
        detailsForThis.testStart();
    }

    public void testSucceeded( ReportEntry reportEntry )
    {
        WrappedReportEntry wrapped = wrap( reportEntry, ReportEntryType.SUCCESS );
        detailsForThis.testSucceeded( wrapped );
        if ( statisticsReporter != null )
        {
            statisticsReporter.testSucceeded( reportEntry );
        }
        clearCapture();
    }

    public void testError( ReportEntry reportEntry )
    {
        WrappedReportEntry wrapped = wrap( reportEntry, ReportEntryType.ERROR );
        detailsForThis.testError( wrapped );
        if ( statisticsReporter != null )
        {
            statisticsReporter.testError( reportEntry );
        }
        clearCapture();
    }

    public void testFailed( ReportEntry reportEntry )
    {
        WrappedReportEntry wrapped = wrap( reportEntry, ReportEntryType.FAILURE );
        detailsForThis.testFailure( wrapped );
        if ( statisticsReporter != null )
        {
            statisticsReporter.testFailed( reportEntry );
        }
        clearCapture();
    }

    // ----------------------------------------------------------------------
    // Counters
    // ----------------------------------------------------------------------

    public void testSkipped( ReportEntry reportEntry )
    {
        WrappedReportEntry wrapped = wrap( reportEntry, ReportEntryType.SKIPPED );

        detailsForThis.testSkipped( wrapped );
        if ( statisticsReporter != null )
        {
            statisticsReporter.testSkipped( reportEntry );
        }
        clearCapture();
    }

    public void testExecutionSkippedByUser()
    {
    }

    public void testAssumptionFailure( ReportEntry report )
    {
        testSkipped( report );
    }

    private WrappedReportEntry wrap( ReportEntry other, ReportEntryType reportEntryType )
    {
        final int estimatedElapsed;
        if ( reportEntryType != ReportEntryType.SKIPPED )
        {
            if ( other.getElapsed() != null )
            {
                estimatedElapsed = other.getElapsed();
            }
            else
            {
                estimatedElapsed = detailsForThis.getElapsedSinceLastStart();
            }
        }
        else
        {
            estimatedElapsed = 0;
        }

        return new WrappedReportEntry( other, reportEntryType, estimatedElapsed, testStdOut, testStdErr );
    }

    private WrappedReportEntry wrapTestSet( ReportEntry other )
    {
        return new WrappedReportEntry( other, null, other.getElapsed() != null
            ? other.getElapsed()
            : detailsForThis.getElapsedSinceTestSetStart(), testStdOut, testStdErr );
    }

    public void close()
    {
        if ( consoleOutputReceiver != null )
        {
            consoleOutputReceiver.close();
        }
    }

    public void  addTestMethodStats()
    {
        for ( WrappedReportEntry reportEntry : detailsForThis.getReportEntries() )
        {
            TestMethodStats methodStats =
                new TestMethodStats( reportEntry.getClassMethodName(), reportEntry.getReportEntryType(),
                                     reportEntry.getStackTraceWriter() );
            testMethodStats.add( methodStats );
        }
    }

    public List<TestMethodStats> getTestMethodStats()
    {
        return testMethodStats;
    }
}
