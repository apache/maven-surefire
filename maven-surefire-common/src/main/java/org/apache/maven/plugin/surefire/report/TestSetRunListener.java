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

import org.apache.maven.plugin.surefire.runorder.StatisticsReporter;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.util.internal.ByteBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reports data for a single test set.
 * <p/>
 *
 * @author Kristian Rosenvold
 */
public class TestSetRunListener
    implements RunListener, ConsoleOutputReceiver, ConsoleLogger
{
    private final RunStatistics globalStatistics;

    private final TestSetStats detailsForThis;


    private final List<ByteBuffer> testStdOut = Collections.synchronizedList( new ArrayList<ByteBuffer>() );

    private final List<ByteBuffer> testStdErr = Collections.synchronizedList( new ArrayList<ByteBuffer>() );

    private final TestcycleConsoleOutputReceiver consoleOutputReceiver;

    private final boolean briefOrPlainFormat;

    private final StatelessXmlReporter simpleXMLReporter;

    private final ConsoleReporter consoleReporter;

    private final FileReporter fileReporter;

    private final StatisticsReporter statisticsReporter;

    public TestSetRunListener( ConsoleReporter consoleReporter, FileReporter fileReporter,
                               StatelessXmlReporter simpleXMLReporter,
                               TestcycleConsoleOutputReceiver consoleOutputReceiver,
                               StatisticsReporter statisticsReporter, RunStatistics globalStats, boolean trimStackTrace,
                               boolean isPlainFormat, boolean briefOrPlainFormat )
    {
        this.consoleReporter = consoleReporter;
        this.fileReporter = fileReporter;
        this.statisticsReporter = statisticsReporter;
        this.simpleXMLReporter = simpleXMLReporter;
        this.consoleOutputReceiver = consoleOutputReceiver;
        this.briefOrPlainFormat = briefOrPlainFormat;
        this.detailsForThis = new TestSetStats( trimStackTrace, isPlainFormat );
        this.globalStatistics = globalStats;
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
        ByteBuffer byteBuffer = new ByteBuffer( buf, off, len );
        if ( stdout )
        {
            testStdOut.add( byteBuffer );
        }
        else
        {
            testStdErr.add( byteBuffer );
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
        testStdErr.clear();
        testStdOut.clear();
    }

    public void testSetCompleted( ReportEntry report )
    {
        WrappedReportEntry wrap = wrapTestSet( report, null );
        List<String> testResults = briefOrPlainFormat ? detailsForThis.getTestResults() : null;
        if ( consoleReporter != null )
        {
            consoleReporter.testSetCompleted( wrap, detailsForThis, testResults );
        }
        consoleOutputReceiver.testSetCompleted( wrap );
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
            consoleReporter.reset();
        }

        globalStatistics.add( detailsForThis );
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
        WrappedReportEntry wrapped = wrap( reportEntry, ReportEntryType.success );
        detailsForThis.testSucceeded( wrapped );
        if ( statisticsReporter != null )
        {
            statisticsReporter.testSucceeded( reportEntry );
        }
        clearCapture();
    }

    public void testError( ReportEntry reportEntry )
    {
        WrappedReportEntry wrapped = wrap( reportEntry, ReportEntryType.error );
        detailsForThis.testError( wrapped );
        if ( statisticsReporter != null )
        {
            statisticsReporter.testError( reportEntry );
        }
        globalStatistics.addErrorSource( reportEntry.getName(), reportEntry.getStackTraceWriter() );
        clearCapture();
    }

    public void testFailed( ReportEntry reportEntry )
    {
        WrappedReportEntry wrapped = wrap( reportEntry, ReportEntryType.failure );
        detailsForThis.testFailure( wrapped );
        if ( statisticsReporter != null )
        {
            statisticsReporter.testFailed( reportEntry );
        }
        globalStatistics.addFailureSource( reportEntry.getName(), reportEntry.getStackTraceWriter() );
        clearCapture();
    }

    // ----------------------------------------------------------------------
    // Counters
    // ----------------------------------------------------------------------

    public void testSkipped( ReportEntry reportEntry )
    {

        WrappedReportEntry wrapped = wrap( reportEntry, ReportEntryType.skipped );
        detailsForThis.testSkipped( wrapped );
        if ( statisticsReporter != null )
        {
            statisticsReporter.testSkipped( reportEntry );
        }
        clearCapture();
    }

    public void testAssumptionFailure( ReportEntry report )
    {
        testSkipped( report );
    }

    public String getAsString( List<ByteBuffer> byteBufferList )
    {
        StringBuilder stringBuffer = new StringBuilder();
        // To avoid getting a java.util.ConcurrentModificationException while iterating (see SUREFIRE-879) we need to
        // iterate over a copy or the elements array. Since the passed in byteBufferList is always wrapped with
        // Collections.synchronizedList( ) we are guaranteed toArray() is going to be atomic, so we are safe.
        for ( Object byteBuffer : byteBufferList.toArray() )
        {
            stringBuffer.append( byteBuffer.toString() );
        }
        return stringBuffer.toString();
    }

    private WrappedReportEntry wrap( ReportEntry other, ReportEntryType reportEntryType )
    {
        return new WrappedReportEntry( other, reportEntryType, other.getElapsed() != null
            ? other.getElapsed()
            : detailsForThis.getElapsedSinceLastStart(), getAsString( testStdOut ), getAsString( testStdErr ) );
    }

    private WrappedReportEntry wrapTestSet( ReportEntry other, ReportEntryType reportEntryType )
    {
        return new WrappedReportEntry( other, reportEntryType, other.getElapsed() != null
            ? other.getElapsed()
            : detailsForThis.getElapsedSinceTestSetStart(), getAsString( testStdOut ), getAsString( testStdErr ) );
    }

    public void close()
    {
        if (consoleOutputReceiver != null) consoleOutputReceiver.close();
    }
}
