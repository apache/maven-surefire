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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.maven.plugin.surefire.runorder.StatisticsReporter;
import org.apache.maven.surefire.report.*;
import org.apache.maven.surefire.util.internal.ByteBuffer;

/**
 * Reports data for a single test set.
 * <p/>
 */
public class TestSetRunListener
    implements RunListener, ConsoleOutputReceiver, ConsoleLogger
{
    private final RunStatistics globalStatistics;

    private final TestSetStats detailsForThis;


    private final MulticastingReporter multicastingReporter;

    private final List<ByteBuffer> testStdOut = Collections.synchronizedList( new ArrayList<ByteBuffer>() );

    private final List<ByteBuffer> testStdErr = Collections.synchronizedList( new ArrayList<ByteBuffer>() );

    private final TestcycleConsoleOutputReceiver consoleOutputReceiver;

    public TestSetRunListener( ConsoleReporter consoleReporter, FileReporter fileReporter, XMLReporter xmlReporter,
                               TestcycleConsoleOutputReceiver consoleOutputReceiver,
                               StatisticsReporter statisticsReporter, RunStatistics globalStats, boolean trimStackTrace,
                               boolean isPlainFormat )
    {
        List<Reporter> reporters = new ArrayList<Reporter>();
        if ( consoleReporter != null )
        {
            reporters.add( consoleReporter );
        }
        if ( fileReporter != null )
        {
            reporters.add( fileReporter );
        }
        if ( xmlReporter != null )
        {
            reporters.add( xmlReporter );
        }
        if ( statisticsReporter != null )
        {
            reporters.add( statisticsReporter );
        }

        this.consoleOutputReceiver = consoleOutputReceiver;
        this.detailsForThis = new TestSetStats( trimStackTrace, isPlainFormat );
        multicastingReporter = new MulticastingReporter( reporters );
        this.globalStatistics = globalStats;
    }

    public void info( String message )
    {
        multicastingReporter.writeMessage( message );
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
        multicastingReporter.testSetStarting( report );
        consoleOutputReceiver.testSetStarting( report );
    }

    public void clearCapture()
    {
        testStdErr.clear();
        testStdOut.clear();
    }

    public void testSetCompleted( ReportEntry report )
    {
        multicastingReporter.testSetCompleted( report, detailsForThis );
        consoleOutputReceiver.testSetCompleted( report );
        globalStatistics.add( detailsForThis );
        detailsForThis.reset();
        multicastingReporter.reset();
    }

    // ----------------------------------------------------------------------
    // Test
    // ----------------------------------------------------------------------

    public void testStarting( ReportEntry report )
    {
        detailsForThis.testStart();
        multicastingReporter.testStarting( report );
    }

    public void testSucceeded( ReportEntry reportEntry )
    {
        detailsForThis.testEnd( reportEntry );
        multicastingReporter.testSucceeded( wrap( reportEntry ), detailsForThis );
        clearCapture();
    }

    public void testError( ReportEntry reportEntry )
    {
        detailsForThis.testError( reportEntry );
        multicastingReporter.testError( wrap( reportEntry ), getAsString( testStdOut ), getAsString( testStdErr ),
                                        detailsForThis );
        globalStatistics.addErrorSource( reportEntry.getName(), reportEntry.getStackTraceWriter() );
        clearCapture();
    }

    public void testFailed( ReportEntry reportEntry )
    {
        detailsForThis.testFailure( reportEntry );
        multicastingReporter.testFailed( wrap( reportEntry ), getAsString( testStdOut ), getAsString( testStdErr ),
                                         detailsForThis );
        globalStatistics.addFailureSource( reportEntry.getName(), reportEntry.getStackTraceWriter() );
        clearCapture();
    }

    // ----------------------------------------------------------------------
    // Counters
    // ----------------------------------------------------------------------

    public void testSkipped( ReportEntry reportEntry )
    {

        detailsForThis.testSkipped( reportEntry );
        clearCapture();
        multicastingReporter.testSkipped( wrap( reportEntry ), detailsForThis );
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

    private ReportEntry wrap( ReportEntry other )
    {
        if ( other.getElapsed() != null )
        {
            return other;
        }
        return CategorizedReportEntry.reportEntry( other.getSourceName(), other.getName(), other.getGroup(),
                                                   other.getStackTraceWriter(),
                                                   detailsForThis.getElapsedSinceLastStart(), other.getMessage() );
    }

}
