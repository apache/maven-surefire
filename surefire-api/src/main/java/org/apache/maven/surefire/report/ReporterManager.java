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

import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.util.TeeStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

public class ReporterManager
{
    private int completedCount;

    private int errors;

    private int failures;

    private List reports;

    private String reportsDirectory;

    private PrintStream oldOut;

    private PrintStream oldErr;

    private PrintStream newErr;

    private PrintStream newOut;

    public ReporterManager( List reports, String reportsDirectory )
    {
        if ( reports == null )
        {
            throw new NullPointerException();
        }

        this.reportsDirectory = reportsDirectory;

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

    public void resume()
    {
        writeMessage( "" );
        writeMessage( "Results :" );
        writeMessage( "Tests run: " + completedCount + ", Failures: " + failures + ", Errors: " + errors );
        writeMessage( "" );
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

            try
            {
                report.runStarting( testCount );
            }
            catch ( Exception e )
            {
                handleReporterException( "runStarting", e );
            }
        }
    }

    public void runStopped()
    {
        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            try
            {
                reporter.runStopped();
            }
            catch ( Exception e )
            {
                handleReporterException( "runStopped", e );
            }
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

            try
            {
                reporter.runAborted( report );
            }
            catch ( Exception e )
            {
                handleReporterException( "runAborted", e );
            }
        }

        ++errors;
    }

    public void runCompleted()
    {
        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            try
            {
                reporter.runCompleted();
            }
            catch ( Exception e )
            {
                handleReporterException( "runCompleted", e );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Battery
    // ----------------------------------------------------------------------

    private ByteArrayOutputStream stdOut;

    private ByteArrayOutputStream stdErr;

    public void batteryStarting( ReportEntry report )
    {
        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            try
            {
                reporter.batteryStarting( report );
            }
            catch ( Exception e )
            {
                handleReporterException( "suiteStarting", e );
            }
        }
    }

    public void batteryCompleted( ReportEntry report )
    {
        if ( !reports.isEmpty() )
        {
            Reporter reporter = (Reporter) reports.get( 0 );

            errors += reporter.getNbErrors();

            failures += reporter.getNbFailures();

            completedCount += reporter.getNbTests();
        }

        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            try
            {
                reporter.batteryCompleted( report );
            }
            catch ( Exception e )
            {
            }
        }
    }

    public void batteryAborted( ReportEntry report )
    {
        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            try
            {
                reporter.batteryAborted( report );
            }
            catch ( Exception e )
            {
                handleReporterException( "suiteAborted", e );
            }
        }

        ++errors;
    }

    // ----------------------------------------------------------------------
    // Test
    // ----------------------------------------------------------------------

    public void testStarting( ReportEntry report )
    {
        File f = new File( reportsDirectory );

        if ( !f.exists() )
        {
            f.mkdirs();
        }

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

            try
            {
                reporter.testStarting( report );
            }
            catch ( Exception e )
            {
                handleReporterException( "testStarting", e );
            }
        }
    }

    public void testSucceeded( ReportEntry report )
    {
        resetStreams();

        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter reporter = (Reporter) it.next();

            try
            {
                reporter.testSucceeded( report );
            }
            catch ( Exception e )
            {
                handleReporterException( "testSucceeded", e );
            }
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

            try
            {
                if ( "failure".equals( typeError ) )
                {
                    reporter.testFailed( reportEntry, stdOutLog, stdErrLog );
                }
                else
                {
                    reporter.testError( reportEntry, stdOutLog, stdErrLog );
                }
            }
            catch ( Exception e )
            {
                handleReporterException( "testFailed", e );
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

    public void dispose()
    {
        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Reporter report = (Reporter) it.next();

            try
            {
                report.dispose();
            }
            catch ( Exception e )
            {
                handleReporterException( "dispose", e );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Counters
    // ----------------------------------------------------------------------

    public int getNbErrors()
    {
        return errors;
    }

    public int getNbFailures()
    {
        return failures;
    }

    public int getNbTests()
    {
        return completedCount;
    }

    /**
     * @todo is this here for throwables? most of these don't throw any checked exceptions
     */
    private void handleReporterException( String reporterMethod, Exception e )
    {
        String reporterThrewException = Surefire.getResourceString( "reporterThrew" );

        MessageFormat msgFmt = new MessageFormat( reporterThrewException );

        Object[] args = {reporterMethod};

        String stringToPrint = msgFmt.format( args );

        System.err.println( stringToPrint );

        e.printStackTrace( System.err );
    }

    public String getReportsDirectory()
    {
        return reportsDirectory;
    }
}
