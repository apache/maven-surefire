package org.apache.maven.surefire.report;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.surefire.util.TeeStream;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

public class ReporterManager
{
    private int completedCount;

    private int errors;

    private int failures;

    private List reports;

    private PrintStream oldOut;

    private PrintStream oldErr;

    private PrintStream newErr;

    private PrintStream newOut;

    private int skipped;

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

        writeMessage( "" );
        writeMessage( "Results :" );
        writeMessage( "Tests run: " + completedCount + ", Failures: " + failures + ", Errors: " + errors +
            ", Skipped: " + skipped );
        writeMessage( "" );
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

            failures += reporter.getNumFailures();

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
        String stdOutLog = stdOut.toString();

        String stdErrLog = stdErr.toString();

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
        System.setOut( oldOut );
        System.setErr( oldErr );

        newOut.close();
        newErr.close();
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
}
