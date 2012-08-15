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
import java.util.Locale;

public class TestSetStats
{
    private final boolean trimStackTrace;

    private long testSetStartAt;

    private long testStartAt;

    private long testEndAt;

    private int completedCount;

    private int errors;

    private int failures;

    private int skipped;

    public TestSetStats( boolean trimStackTrace )
    {
        this.trimStackTrace = trimStackTrace;
    }

    public long getTestSetStartAt()
    {
        return testSetStartAt;
    }

    public int getElapsedSinceTestSetStart()
    {
        return (int) ( System.currentTimeMillis() - testSetStartAt );
    }

    public void testSetStart()
    {
        testSetStartAt = System.currentTimeMillis();
    }

    public void testStart()
    {
        testStartAt = System.currentTimeMillis();
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

    public synchronized void incrementCompletedCount()
    {
        completedCount += 1;
    }

    public synchronized void incrementErrorsCount()
    {
        errors += 1;
    }

    public synchronized void incrementFailureCount()
    {
        failures += 1;
    }

    public synchronized void incrementSkippedCount()
    {
        skipped += 1;
    }

    public synchronized void reset()
    {
        completedCount = 0;
        errors = 0;
        failures = 0;
        skipped = 0;
    }

    public synchronized int getCompletedCount()
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
        buf.append( ", Time elaXpsed: " );
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


}
