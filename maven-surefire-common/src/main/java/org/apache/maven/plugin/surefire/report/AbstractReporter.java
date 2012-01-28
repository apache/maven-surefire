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

import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.StackTraceWriter;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class AbstractReporter
    implements Reporter
{
    int completedCount;

    int errors;

    int failures;

    private long startTime;

    private long endTime;

    private final NumberFormat numberFormat = NumberFormat.getInstance( Locale.ENGLISH );

    static final String NL = System.getProperty( "line.separator" );

    private static final int MS_PER_SEC = 1000;

    long testSetStartTime;

    int skipped;

    private final boolean trimStackTrace;

    // ----------------------------------------------------------------------
    // Report interface
    // ----------------------------------------------------------------------


    protected AbstractReporter( boolean trimStackTrace )
    {
        this.trimStackTrace = trimStackTrace;
    }


    public void writeMessage( byte[] b, int off, int len )
    {
        // Keep quiet about console output
        // Reporting is itching for a cleanup
    }


    public void testSetStarting( ReportEntry report )
        throws ReporterException
    {
        testSetStartTime = System.currentTimeMillis();
    }

    public void testSetCompleted( ReportEntry report )
        throws ReporterException
    {
    }

    // ----------------------------------------------------------------------
    // Test
    // ----------------------------------------------------------------------

    public void testStarting( ReportEntry report )
    {
        startTime = System.currentTimeMillis();
    }

    public void testSucceeded( ReportEntry report )
    {
        endTest();
    }

    public void testSkipped( ReportEntry report )
    {
        ++skipped;

        endTest();
    }

    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
        ++errors;
        endTest();
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        ++failures;
        endTest();
    }

    private void endTest()
    {
        ++completedCount;

        endTime = System.currentTimeMillis();
        // SUREFIRE-398 skipped tests call endTest without calling testStarting
        // if startTime = 0, set it to endTime, so the diff will be 0
        if ( startTime == 0 )
        {
            startTime = endTime;
        }
    }

    // ----------------------------------------------------------------------
    // Counters
    // ----------------------------------------------------------------------

    int getNumErrors()
    {
        return errors;
    }

    int getNumSkipped()
    {
        return skipped;
    }

    int getNumFailures()
    {
        return failures;
    }

    int getNumTests()
    {
        return completedCount;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void reset()
    {
        errors = 0;

        skipped = 0;

        failures = 0;

        completedCount = 0;

    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    String elapsedTimeAsString( long runTime )
    {
        return numberFormat.format( (double) runTime / MS_PER_SEC );
    }

    /**
     * Returns stacktrace as String.
     *
     * @param report ReportEntry object.
     * @return stacktrace as string.
     */
    String getStackTrace( ReportEntry report )
    {
        StackTraceWriter writer = report.getStackTraceWriter();
        if ( writer == null )
        {
            return null;
        }
        return trimStackTrace ? writer.writeTrimmedTraceToString() : writer.writeTraceToString();
    }

    long getActualRunTime( ReportEntry reportEntry )
    {
        final Integer clientSpecifiedElapsed = reportEntry.getElapsed();
        return clientSpecifiedElapsed != null ? clientSpecifiedElapsed.intValue() : endTime - startTime;
    }

    void deleteIfExisting( File reportFile )
    {
        if ( reportFile.exists() )
        {
            //noinspection ResultOfMethodCallIgnored
            reportFile.delete();
        }
    }
}
