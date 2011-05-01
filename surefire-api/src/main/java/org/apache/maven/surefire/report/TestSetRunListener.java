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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.maven.surefire.util.internal.ByteBuffer;

/**
 * Reports data for a single test set.
 * <p/>
 */
public class TestSetRunListener
    implements RunListener, Reporter, ConsoleOutputReceiver,
    DirectConsoleReporter     // todo: Does this have to be a reporter ?
{
    private final TestSetStatistics testSetStatistics;

    private final RunStatistics globalStatistics;

    private final MulticastingReporter multicastingReporter;

    private final List testStdOut = Collections.synchronizedList( new ArrayList() );

    private final List testStdErr = Collections.synchronizedList( new ArrayList() );


    public TestSetRunListener( AbstractConsoleReporter consoleReporter, AbstractFileReporter fileReporter,
                               XMLReporter xmlReporter, Reporter reporter, RunStatistics globalStats )
    {

        ArrayList reportes = new ArrayList();
        if ( consoleReporter != null )
        {
            reportes.add( consoleReporter );
        }
        if ( fileReporter != null )
        {
            reportes.add( fileReporter );
        }
        if ( xmlReporter != null )
        {
            reportes.add( xmlReporter );
        }
        if ( reporter != null )
        {
            reportes.add( reporter );
        }
        multicastingReporter = new MulticastingReporter( reportes );
        this.testSetStatistics = new TestSetStatistics();
        this.globalStatistics = globalStats;
    }

    public void writeMessage( String message )
    {
        multicastingReporter.writeMessage( message );
    }


    public void writeMessage( byte[] b, int off, int len )
    {
        multicastingReporter.writeMessage( b, off, len );
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
        multicastingReporter.writeMessage( buf, off, len );
    }

    public void testSetStarting( ReportEntry report )
    {
        multicastingReporter.testSetStarting( report );
    }

    public void clearCapture()
    {
        testStdErr.clear();
        testStdOut.clear();
    }

    public void testSetCompleted( ReportEntry report )
    {
        multicastingReporter.testSetCompleted( report );
        multicastingReporter.reset();
        globalStatistics.add( testSetStatistics );
        testSetStatistics.reset();
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
        testSetStatistics.incrementCompletedCount();
        multicastingReporter.testSucceeded( report );
        clearCapture();
    }

    public void testError( ReportEntry reportEntry )
    {
        multicastingReporter.testError( reportEntry, getAsString( testStdOut ), getAsString( testStdErr ) );
        testSetStatistics.incrementErrorsCount();
        testSetStatistics.incrementCompletedCount();
        globalStatistics.addErrorSource( reportEntry.getName(), reportEntry.getStackTraceWriter() );
        clearCapture();
    }

    public void testError( ReportEntry reportEntry, String stdOutLog, String stdErrLog )
    {
        multicastingReporter.testError( reportEntry, stdOutLog, stdErrLog );
        testSetStatistics.incrementErrorsCount();
        testSetStatistics.incrementCompletedCount();
        globalStatistics.addErrorSource( reportEntry.getName(), reportEntry.getStackTraceWriter() );
        clearCapture();
    }

    public void testFailed( ReportEntry reportEntry )
    {
        multicastingReporter.testFailed( reportEntry, getAsString( testStdOut ), getAsString( testStdErr ) );
        testSetStatistics.incrementFailureCount();
        testSetStatistics.incrementCompletedCount();
        globalStatistics.addFailureSource( reportEntry.getName(), reportEntry.getStackTraceWriter() );
        clearCapture();
    }

    public void testFailed( ReportEntry reportEntry, String stdOutLog, String stdErrLog )
    {
        multicastingReporter.testFailed( reportEntry, stdOutLog, stdErrLog );
        testSetStatistics.incrementFailureCount();
        testSetStatistics.incrementCompletedCount();
        globalStatistics.addFailureSource( reportEntry.getName(), reportEntry.getStackTraceWriter() );
        clearCapture();
    }

    // ----------------------------------------------------------------------
    // Counters
    // ----------------------------------------------------------------------

    public void testSkipped( ReportEntry report )
    {
        clearCapture();
        testSetStatistics.incrementSkippedCount();
        testSetStatistics.incrementCompletedCount();
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
}
