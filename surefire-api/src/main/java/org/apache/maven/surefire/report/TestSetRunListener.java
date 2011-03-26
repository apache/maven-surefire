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

import org.apache.maven.surefire.util.internal.ByteBuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Reports data for a single test set.
 * <p/>
 * Synchronization/Threading note:
 * <p/>
 * This design is really only good for single-threaded test execution. With the use of the additional
 * SynchronizedReporterManager you can get a buggy version that sort-of supports multithreading.
 * <p/>
 * The underlying providers are singletons and keep state per ReporterManager instance
 * <p/>
 * The solution to this problem involves making a clearer separation between test-result collection and reporting,
 * preferably removing singleton state approach out of the reporting interface.
 * <p/>
 */
public class TestSetRunListener
    implements RunListener, RunReporter, Reporter, ConsoleOutputReceiver
{
    private final RunStatistics runStatisticsForThis;

    private final MulticastingReporter multicastingReporter;

    private final List testStdOut = Collections.synchronizedList( new ArrayList() );

    private final List testStdErr = Collections.synchronizedList( new ArrayList() );


    public TestSetRunListener( List reports, RunStatistics runStatisticsForThis )
    {
        multicastingReporter = new MulticastingReporter( reports );
        this.runStatisticsForThis = runStatisticsForThis;
    }

    public void writeMessage( String message )
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
        multicastingReporter.writeMessage( new String( buf, off, len ) );
    }

    public void writeDetailMessage( String message )
    {
        multicastingReporter.writeDetailMessage( message );
    }

    // ----------------------------------------------------------------------
    // Run
    // ----------------------------------------------------------------------

    public void runStarting()
    {
        multicastingReporter.runStarting();
    }

    public void runCompleted()
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
    }

    public void writeFooter( String footer )
    {
        multicastingReporter.writeFooter( footer );
    }


    public void testSetStarting( ReportEntry report )
        throws ReporterException
    {
        multicastingReporter.testSetStarting( report );
    }

    public void testSetCompleted( ReportEntry report )
    {
        multicastingReporter.testSetCompleted( report );
        multicastingReporter.reset();
    }

    // ----------------------------------------------------------------------
    // Test
    // ----------------------------------------------------------------------

    public void testStarting( ReportEntry report )
    {
        multicastingReporter.testStarting( report );
    }

    public void testSucceeded( ReportEntry report )
    {
        clearCapturedContent();
        runStatisticsForThis.incrementCompletedCount();
        multicastingReporter.testSucceeded( report );
    }

    public void testError( ReportEntry reportEntry )
    {
        testError( reportEntry, getAsString( testStdOut ), getAsString( testStdErr ) );
    }

    public void testError( ReportEntry reportEntry, String stdOutLog, String stdErrLog )
    {
        multicastingReporter.testError( reportEntry, stdOutLog, stdErrLog );
        runStatisticsForThis.incrementErrorsCount();
        runStatisticsForThis.incrementCompletedCount();
        runStatisticsForThis.addErrorSource( reportEntry.getName(), reportEntry.getStackTraceWriter() );
        clearCapturedContent();
    }

    public void testFailed( ReportEntry reportEntry )
    {
        testFailed( reportEntry, getAsString( testStdOut ), getAsString( testStdErr ) );
    }

    public void testFailed( ReportEntry reportEntry, String stdOutLog, String stdErrLog )
    {
        multicastingReporter.testFailed( reportEntry, stdOutLog, stdErrLog );
        runStatisticsForThis.incrementFailureCount();
        runStatisticsForThis.incrementCompletedCount();
        runStatisticsForThis.addFailureSource( reportEntry.getName(), reportEntry.getStackTraceWriter() );
        clearCapturedContent();
    }

    // ----------------------------------------------------------------------
    // Counters
    // ----------------------------------------------------------------------

    public void testSkipped( ReportEntry report )
    {
        clearCapturedContent();
        runStatisticsForThis.incrementSkippedCount();
        runStatisticsForThis.incrementCompletedCount();
        multicastingReporter.testSkipped( report );
    }

    public void testAssumptionFailure( ReportEntry report )
    {
        testSkipped( report );
    }

    public void reset()
    {
        multicastingReporter.reset();
    }

    public String getAsString( List byteBufferList )
    {
        StringBuffer stringBuffer = new StringBuffer();
        for ( Iterator iter = byteBufferList.iterator(); iter.hasNext(); )
        {
            ByteBuffer byteBuffer = (ByteBuffer) iter.next();
            stringBuffer.append( byteBuffer.toString() );
        }
        return stringBuffer.toString();
    }

    public void clearCapturedContent()
    {
        testStdErr.clear();
        testStdOut.clear();
    }


}
