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

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.StackTraceWriter;

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

    private long testEndAt;

    private int completedCount;

    private int errors;

    private int failures;

    private int skipped;

    private long lastStartAt;

    private final List<String> testResults = new ArrayList<String>();

    public TestSetStats( boolean trimStackTrace, boolean plainFormat )
    {
        this.trimStackTrace = trimStackTrace;
        this.plainFormat = plainFormat;
    }

    public long getTestSetStartAt()
    {
        return testSetStartAt;
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
        testResults.clear();

    }

    public void testStart()
    {
        lastStartAt = testStartAt = System.currentTimeMillis();
    }

    public long testEnd()
    {
        incrementCompletedCount();
        testEndAt = System.currentTimeMillis();
        // SUREFIRE-398 skipped tests call endTest without calling testStarting
        // if startTime = 0, set it to endTime, so the diff will be 0
        if ( testStartAt == 0 )
        {
            testStartAt = testEndAt;
        }
        return testEndAt - testStartAt;
    }

    public void testEnd( ReportEntry reportEntry )
    {
        testEnd();
        if ( plainFormat )
        {
            addTestResult( reportEntry );
        }
    }


    public void incrementCompletedCount()
    {
        completedCount += 1;
    }

    public void testError( ReportEntry reportEntry )
    {
        errors += 1;
        testEnd();
        testResults.add( getOutput( reportEntry, "ERROR" ) );

    }

    public void testFailure( ReportEntry reportEntry )
    {
        failures += 1;
        testEnd();
        testResults.add( getOutput( reportEntry, "FAILURE" ) );
    }

    public void testSkipped( ReportEntry reportEntry )
    {
        skipped += 1;
        testEnd();
        if ( plainFormat )
        {
            testResults.add( reportEntry.getName() + " skipped" );
        }

    }

    public void reset()
    {
        completedCount = 0;
        errors = 0;
        failures = 0;
        skipped = 0;
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

    long getActualRunTime( ReportEntry reportEntry )
    {
        @SuppressWarnings( "deprecation" )
        final Integer clientSpecifiedElapsed = reportEntry.getElapsed();
        return clientSpecifiedElapsed != null ? clientSpecifiedElapsed : testEndAt - testStartAt;
    }

    private static final String TEST_SET_COMPLETED_PREFIX = "Tests run: ";

    private final NumberFormat numberFormat = NumberFormat.getInstance( Locale.ENGLISH );

    static final String NL = System.getProperty( "line.separator" );

    private static final int MS_PER_SEC = 1000;


    String elapsedTimeAsString( long runTime )
    {
        return numberFormat.format( (double) runTime / MS_PER_SEC );
    }

    public String getTestSetSummary( Integer elapsed )
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
        buf.append( elapsedTimeAsString( elapsed != null ? elapsed : getElapsedSinceTestSetStart() ) );
        buf.append( " sec" );

        if ( failures > 0 || errors > 0 )
        {
            buf.append( " <<< FAILURE!" );
        }

        buf.append( "\n" );

        return buf.toString();
    }

    public String getElapsedTimeSummary( ReportEntry report )
    {
        StringBuilder reportContent = new StringBuilder();
        reportContent.append( report.getName() );
        reportContent.append( "  Time elapsed: " );
        reportContent.append( getActualRunTime( report ) );
        reportContent.append( " sec" );

        return reportContent.toString();
    }


    public String getOutput( ReportEntry report, String msg )
    {
        StringBuilder buf = new StringBuilder();

        buf.append( getElapsedTimeSummary( report ) );

        buf.append( "  <<< " ).append( msg ).append( "!" ).append( NL );

        buf.append( getStackTrace( report ) );

        return buf.toString();
    }

    /**
     * Returns stacktrace as String.
     *
     * @param report ReportEntry object.
     * @return stacktrace as string.
     */
    public String getStackTrace( ReportEntry report )
    {
        StackTraceWriter writer = report.getStackTraceWriter();
        if ( writer == null )
        {
            return null;
        }
        return this.trimStackTrace ? writer.writeTrimmedTraceToString() : writer.writeTraceToString();
    }

    public void addTestResult( ReportEntry reportEntry )
    {
        testResults.add( getElapsedTimeSummary( reportEntry ) );
    }

    public List<String> getTestResults()
    {
        return testResults;
    }
}
