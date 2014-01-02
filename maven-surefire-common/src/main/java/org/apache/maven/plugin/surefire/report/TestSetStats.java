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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Maintains per-thread test result state. Not thread safe.
 */
public class TestSetStats
{
    private final boolean trimStackTrace;

    private final boolean plainFormat;

    private long testSetStartAt;

    private long testStartAt;

    private int completedCount;

    private int errors;

    private int failures;

    private int skipped;

    private long lastStartAt;

    private long elapsedForTestSet;

    private final List<WrappedReportEntry> reportEntries = new ArrayList<WrappedReportEntry>();

    public TestSetStats( boolean trimStackTrace, boolean plainFormat )
    {
        this.trimStackTrace = trimStackTrace;
        this.plainFormat = plainFormat;
    }

    public int getElapsedSinceTestSetStart()
    {
        return (int) ( System.currentTimeMillis() - testSetStartAt );
    }

    public int getElapsedSinceLastStart()
    {
        return (int) ( System.currentTimeMillis() - lastStartAt );
    }

    public void testSetStart()
    {
        lastStartAt = testSetStartAt = System.currentTimeMillis();
    }

    public void testStart()
    {
        lastStartAt = testStartAt = System.currentTimeMillis();
    }

    private long finishTest( WrappedReportEntry reportEntry )
    {
        reportEntries.add( reportEntry );
        incrementCompletedCount();
        long testEndAt = System.currentTimeMillis();
        // SUREFIRE-398 skipped tests call endTest without calling testStarting
        // if startTime = 0, set it to endTime, so the diff will be 0
        if ( testStartAt == 0 )
        {
            testStartAt = testEndAt;
        }
        long elapsedForThis = reportEntry.getElapsed() != null ? reportEntry.getElapsed() : testEndAt - testStartAt;
        elapsedForTestSet += elapsedForThis;
        return elapsedForThis;
    }

    public void testSucceeded( WrappedReportEntry reportEntry )
    {
        finishTest( reportEntry );
    }


    public void testError( WrappedReportEntry reportEntry )
    {
        errors += 1;
        finishTest( reportEntry );

    }

    public void testFailure( WrappedReportEntry reportEntry )
    {
        failures += 1;
        finishTest( reportEntry );
    }

    public void testSkipped( WrappedReportEntry reportEntry )
    {
        skipped += 1;
        finishTest( reportEntry );
    }

    public void reset()
    {
        completedCount = 0;
        errors = 0;
        failures = 0;
        skipped = 0;
        elapsedForTestSet = 0;

        for ( WrappedReportEntry entry : reportEntries )
        {
            entry.getStdout().free();
            entry.getStdErr().free();
        }

        reportEntries.clear();
    }

    public int getCompletedCount()
    {
        return completedCount;
    }

    public int getErrors()
    {
        return errors;
    }

    public int getFailures()
    {
        return failures;
    }

    public int getSkipped()
    {
        return skipped;
    }

    private static final String TEST_SET_COMPLETED_PREFIX = "Tests run: ";

    private final NumberFormat numberFormat = NumberFormat.getInstance( Locale.ENGLISH );

    private static final int MS_PER_SEC = 1000;

    String elapsedTimeAsString( long runTime )
    {
        return numberFormat.format( (double) runTime / MS_PER_SEC );
    }

    public String getElapsedForTestSet()
    {
        return elapsedTimeAsString( elapsedForTestSet );
    }

    private void incrementCompletedCount()
    {
        completedCount += 1;
    }

    public String getTestSetSummary( WrappedReportEntry reportEntry )
    {
        StringBuilder buf = new StringBuilder();

        buf.append( TEST_SET_COMPLETED_PREFIX );
        buf.append( completedCount );
        buf.append( ", Failures: " );
        buf.append( failures );
        buf.append( ", Errors: " );
        buf.append( errors );
        buf.append( ", Skipped: " );
        buf.append( skipped );
        buf.append( ", Time elapsed: " );
        buf.append( reportEntry.elapsedTimeAsString() );
        buf.append( " sec" );

        if ( failures > 0 || errors > 0 )
        {
            buf.append( " <<< FAILURE!" );
        }

        buf.append( " - in " );
        buf.append( reportEntry.getNameWithGroup() );

        buf.append( "\n" );

        return buf.toString();
    }

    public List<String> getTestResults()
    {
        List<String> result = new ArrayList<String>();
        for ( WrappedReportEntry testResult : reportEntries )
        {
            if ( testResult.isErrorOrFailure() )
            {
                result.add( testResult.getOutput( trimStackTrace ) );
            }
            else if ( plainFormat && testResult.isSkipped() )
            {
                result.add( testResult.getName() + " skipped" );
            }
            else if ( plainFormat && testResult.isSucceeded() )
            {
                result.add( testResult.getElapsedTimeSummary() );
            }
        }
        return result;
    }

    public List<WrappedReportEntry> getReportEntries()
    {
        return reportEntries;
    }
}
