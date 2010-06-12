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

import java.text.NumberFormat;
import java.util.Locale;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class AbstractReporter
    implements Reporter
{
    protected int completedCount;

    protected int errors;

    protected int failures;

    protected long startTime;

    protected long endTime;

    private NumberFormat numberFormat = NumberFormat.getInstance( Locale.ENGLISH );

    protected static final String NL = System.getProperty( "line.separator" );

    private static final int MS_PER_SEC = 1000;

    protected long testSetStartTime;

    protected int skipped;

    private boolean trimStackTrace;

    // ----------------------------------------------------------------------
    // Report interface
    // ----------------------------------------------------------------------


    protected AbstractReporter( Boolean trimStackTrace )
    {
        this.trimStackTrace = trimStackTrace.booleanValue();
    }

    public void writeFooter( String footer )
    {
        writeMessage( footer );
    }

    public void runStarting()
    {
    }

    public void runCompleted()
    {
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

    public int getNumErrors()
    {
        return errors;
    }

    public int getNumSkipped()
    {
        return skipped;
    }

    public int getNumFailures()
    {
        return failures;
    }

    public int getNumTests()
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

    protected String elapsedTimeAsString( long runTime )
    {
        return numberFormat.format( (double) runTime / MS_PER_SEC );
    }

    /**
     * Returns stacktrace as String.
     *
     * @param report ReportEntry object.
     * @return stacktrace as string.
     */
    protected String getStackTrace( ReportEntry report )
    {
        StackTraceWriter writer = report.getStackTraceWriter();
        if ( writer == null )
        {
            return null;
        }
        return trimStackTrace ? writer.writeTrimmedTraceToString()
            : writer.writeTraceToString();
    }
}
