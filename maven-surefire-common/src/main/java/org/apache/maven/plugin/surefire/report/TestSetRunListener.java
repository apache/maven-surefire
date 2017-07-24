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
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.runorder.StatisticsReporter;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.TestSetReportEntry;

import static org.apache.maven.plugin.surefire.report.ReportEntryType.ERROR;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.FAILURE;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.SKIPPED;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.SUCCESS;

/**
 * Reports data for a single test set.
 * <br>
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
        detailsForThis = new TestSetStats( trimStackTrace, isPlainFormat );
        testMethodStats = new ArrayList<TestMethodStats>();
    }

    @Override
    public void debug( String message )
    {
        consoleReporter.getConsoleLogger().debug( trimTrailingNewLine( message ) );
    }

    @Override
    public void info( String message )
    {
        consoleReporter.getConsoleLogger().info( trimTrailingNewLine( message ) );
    }

    @Override
    public void warning( String message )
    {
        consoleReporter.getConsoleLogger().warning( trimTrailingNewLine( message ) );
    }

    @Override
    public void error( String message )
    {
        consoleReporter.getConsoleLogger().error( trimTrailingNewLine( message ) );
    }

    @Override
    public void error( String message, Throwable t )
    {
        consoleReporter.getConsoleLogger().error( message, t );
    }

    @Override
    public void error( Throwable t )
    {
        consoleReporter.getConsoleLogger().error( t );
    }

    @Override
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

    @Override
    public void testSetStarting( TestSetReportEntry report )
    {
        detailsForThis.testSetStart();
        consoleReporter.testSetStarting( report );
        consoleOutputReceiver.testSetStarting( report );
    }

    public void clearCapture()
    {
        testStdOut = initDeferred( "stdout" );
        testStdErr = initDeferred( "stderr" );
    }

    @Override
    public void testSetCompleted( TestSetReportEntry report )
    {
        final WrappedReportEntry wrap = wrapTestSet( report );
        final List<String> testResults =
                briefOrPlainFormat ? detailsForThis.getTestResults() : Collections.<String>emptyList();
        fileReporter.testSetCompleted( wrap, detailsForThis, testResults );
        simpleXMLReporter.testSetCompleted( wrap, detailsForThis );
        statisticsReporter.testSetCompleted();
        consoleReporter.testSetCompleted( wrap, detailsForThis, testResults );
        consoleOutputReceiver.testSetCompleted( wrap );
        consoleReporter.reset();

        wrap.getStdout().free();
        wrap.getStdErr().free();

        addTestMethodStats();
        detailsForThis.reset();
        clearCapture();
    }

    // ----------------------------------------------------------------------
    // Test
    // ----------------------------------------------------------------------

    @Override
    public void testStarting( ReportEntry report )
    {
        detailsForThis.testStart();
    }

    @Override
    public void testSucceeded( ReportEntry reportEntry )
    {
        WrappedReportEntry wrapped = wrap( reportEntry, SUCCESS );
        detailsForThis.testSucceeded( wrapped );
        statisticsReporter.testSucceeded( reportEntry );
        clearCapture();
    }

    @Override
    public void testError( ReportEntry reportEntry )
    {
        WrappedReportEntry wrapped = wrap( reportEntry, ERROR );
        detailsForThis.testError( wrapped );
        statisticsReporter.testError( reportEntry );
        clearCapture();
    }

    @Override
    public void testFailed( ReportEntry reportEntry )
    {
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
        testSkipped( report );
    }

    private WrappedReportEntry wrap( ReportEntry other, ReportEntryType reportEntryType )
    {
        final int estimatedElapsed;
        if ( reportEntryType != SKIPPED )
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

    private WrappedReportEntry wrapTestSet( TestSetReportEntry other )
    {
        return new WrappedReportEntry( other, null, other.getElapsed() != null
            ? other.getElapsed()
            : detailsForThis.getElapsedSinceTestSetStart(), testStdOut, testStdErr, other.getSystemProperties() );
    }

    public void close()
    {
        consoleOutputReceiver.close();
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

    public static String trimTrailingNewLine( final String message )
    {
        final int e = message == null ? 0 : lineBoundSymbolWidth( message );
        return message != null && e != 0 ? message.substring( 0, message.length() - e ) : message;
    }

    private static int lineBoundSymbolWidth( String message )
    {
        return message.endsWith( "\n" ) || message.endsWith( "\r" ) ? 1 : ( message.endsWith( "\r\n" ) ? 2 : 0 );
    }
}
