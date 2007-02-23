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

import org.apache.maven.surefire.util.TeeStream;
import org.codehaus.plexus.util.IOUtil;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class ReporterManager
{
    private int completedCount;

    private int errors;

    /**
     * Holds the sources of the error.
     */
    private Collection errorSources = new ArrayList();

    private int failures;

    /**
     * Holds the sources of the failures.
     */
    private Collection failureSources = new ArrayList();

    private List reports;

    private PrintStream oldOut;

    private PrintStream oldErr;

    private PrintStream newErr;

    private PrintStream newOut;

    private int skipped;

    private static final String RESULTS_ERRORS = "errors";

    private static final String RESULTS_COMPLETED_COUNT = "completedCount";

    private static final String RESULTS_FAILURES = "failures";

    private static final String RESULTS_SKIPPED = "skipped";

    public ReporterManager( List reports )
    {
        this.reports = reports;
    }

    public void addReporter( Reporter reporter )
    {
        if ( reporter == null )
        {
            throw new NullPointerException();
        }

        if ( !reports.contains( reporter ) )
        {
            reports.add( reporter );
        }
    }

    public void removeReport( Reporter report )
    {
        if ( report == null )
        {
            throw new NullPointerException();
        }

        if ( reports.contains( report ) )
        {
            reports.remove( report );
        }
    }

    public List getReports()
    {
        return reports;
    }

    public void writeMessage( String message )
    {
        for ( Iterator i = reports.iterator(); i.hasNext(); )
        {
            Reporter report = (Reporter) i.next();

            report.writeMessage( message );
        }
    }

    // ----------------------------------------------------------------------
    // Run
    // ----------------------------------------------------------------------

    public void runStarting( int testCount )
    {
        if ( testCount < 0 )
        {
            throw new IllegalArgumentException( "testCount is less than zero" );
        }

        for ( Iterator i = reports.iterator(); i.hasNext(); )
        {
            Reporter report = (Reporter) i.next();

            report.runStarting( testCount );
        }
    }

    public void runStopped()
    {
        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            reporter.runStopped();
        }
    }

    public void runAborted( ReportEntry report )
    {
        if ( report == null )
        {
            throw new NullPointerException();
        }

        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            reporter.runAborted( report );
        }

        ++errors;
    }

    public void runCompleted()
    {
        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            reporter.runCompleted();
        }

        writeFooter( "" );
        writeFooter( "Results :" );
        writeFooter( "" );
        if ( failures > 0 )
        {
            writeFooter( "Failed tests: " );
            for ( Iterator iterator = this.failureSources.iterator(); iterator.hasNext(); )
            {
                writeFooter( "  " + iterator.next() );
            }
            writeFooter( "" );
        }
        if ( errors > 0 )
        {
            writeFooter( "Tests in error: " );
            for ( Iterator iterator = this.errorSources.iterator(); iterator.hasNext(); )
            {
                writeFooter( "  " + iterator.next() );
            }
            writeFooter( "" );
        }
        writeFooter( "Tests run: " + completedCount + ", Failures: " + failures + ", Errors: " + errors +
            ", Skipped: " + skipped );
        writeFooter( "" );
    }

    private void writeFooter( String footer )
    {
        for ( Iterator i = reports.iterator(); i.hasNext(); )
        {
            Reporter report = (Reporter) i.next();

            report.writeFooter( footer );
        }
    }

    private ByteArrayOutputStream stdOut;

    private ByteArrayOutputStream stdErr;

    public void testSetStarting( ReportEntry report )
        throws ReporterException
    {
        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            reporter.testSetStarting( report );
        }
    }

    public void testSetCompleted( ReportEntry report )
    {
        if ( !reports.isEmpty() )
        {
            Reporter reporter = (Reporter) reports.get( 0 );

            skipped += reporter.getNumSkipped();

            errors += reporter.getNumErrors();
            errorSources.addAll( reporter.getErrorSources() );

            failures += reporter.getNumFailures();
            failureSources.addAll( reporter.getFailureSources() );

            completedCount += reporter.getNumTests();
        }

        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            try
            {
                reporter.testSetCompleted( report );
            }
            catch ( Exception e )
            {
            }
        }
    }

    public void testSetAborted( ReportEntry report )
    {
        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            reporter.testSetAborted( report );
        }

        ++errors;
    }

    // ----------------------------------------------------------------------
    // Test
    // ----------------------------------------------------------------------

    public void testStarting( ReportEntry report )
    {
        stdOut = new ByteArrayOutputStream();

        newOut = new PrintStream( stdOut );

        oldOut = System.out;

        TeeStream tee = new TeeStream( oldOut, newOut );
        System.setOut( tee );

        stdErr = new ByteArrayOutputStream();

        newErr = new PrintStream( stdErr );

        oldErr = System.err;

        tee = new TeeStream( oldErr, newErr );
        System.setErr( tee );

        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            reporter.testStarting( report );
        }
    }

    public void testSucceeded( ReportEntry report )
    {
        resetStreams();

        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            reporter.testSucceeded( report );
        }
    }

    public void testError( ReportEntry reportEntry )
    {
        testFailed( reportEntry, "error" );
    }

    public void testFailed( ReportEntry reportEntry )
    {
        testFailed( reportEntry, "failure" );
    }

    private void testFailed( ReportEntry reportEntry, String typeError )
    {
        // Note that the fields can be null if the test hasn't even started yet (an early error)
        String stdOutLog = stdOut != null ? stdOut.toString() : "";

        String stdErrLog = stdErr != null ? stdErr.toString() : "";

        resetStreams();

        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            if ( "failure".equals( typeError ) )
            {
                reporter.testFailed( reportEntry, stdOutLog, stdErrLog );
            }
            else
            {
                reporter.testError( reportEntry, stdOutLog, stdErrLog );
            }
        }
    }

    private void resetStreams()
    {
        // Note that the fields can be null if the test hasn't even started yet (an early error)
        if ( oldOut != null )
        {
            System.setOut( oldOut );
        }
        if ( oldErr != null )
        {
            System.setErr( oldErr );
        }

        IOUtil.close( newOut );
        IOUtil.close( newErr );
    }

    public void reset()
    {
        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter report = (Reporter) it.next();

            report.reset();
        }
    }

    // ----------------------------------------------------------------------
    // Counters
    // ----------------------------------------------------------------------

    public int getNumErrors()
    {
        return errors;
    }

    public int getNumFailures()
    {
        return failures;
    }

    public int getNbTests()
    {
        return completedCount;
    }

    public void testSkipped( ReportEntry report )
    {
        resetStreams();

        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            reporter.testSkipped( report );
        }
    }

    public void initResultsFromProperties( Properties results )
    {
        errors = Integer.valueOf( results.getProperty( RESULTS_ERRORS, "0" ) ).intValue();
        skipped = Integer.valueOf( results.getProperty( RESULTS_SKIPPED, "0" ) ).intValue();
        failures = Integer.valueOf( results.getProperty( RESULTS_FAILURES, "0" ) ).intValue();
        completedCount = Integer.valueOf( results.getProperty( RESULTS_COMPLETED_COUNT, "0" ) ).intValue();
    }

    public void updateResultsProperties( Properties results )
    {
        results.setProperty( RESULTS_ERRORS, String.valueOf( errors ) );
        results.setProperty( RESULTS_COMPLETED_COUNT, String.valueOf( completedCount ) );
        results.setProperty( RESULTS_FAILURES, String.valueOf( failures ) );
        results.setProperty( RESULTS_SKIPPED, String.valueOf( skipped ) );
    }
}
