package org.codehaus.surefire.report;

import org.codehaus.surefire.Surefire;
import org.codehaus.surefire.util.TeeStream;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.ByteArrayOutputStream;

public class ReporterManager
{
    private int completedCount;

    private int errors;

    private int failures;

    private List reports;

    private String reportsDirectory;

    public ReporterManager( List reports, String reportsDirectory )
    {
        if ( reports == null )
        {
            throw new NullPointerException();
        }

        this.reportsDirectory = reportsDirectory;

        File f = new File( reportsDirectory );

        if ( !f.exists() )
        {
            f.mkdirs();
        }

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
        writeMessage( "[surefire] Tests run: " + completedCount +
                      ", Failures: " + failures +
                      ", Errors: " + errors );
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
                handleReporterException( "suiteCompleted", e );
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
    }

    // ----------------------------------------------------------------------
    // Test
    // ----------------------------------------------------------------------

    public void testStarting( ReportEntry report )
    {
        stdOut = new ByteArrayOutputStream();

        PrintStream out = new PrintStream( stdOut );

        PrintStream tee = new TeeStream( System.out, out );

        System.setOut( tee );

        stdErr = new ByteArrayOutputStream();

        PrintStream err = new PrintStream( stdErr );

        tee = new TeeStream( System.err, err );

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

    private void handleReporterException( String reporterMethod, Exception e )
    {
        String reporterThrewException = Surefire.getResources().getString( "reporterThrew" );

        MessageFormat msgFmt = new MessageFormat( reporterThrewException );

        Object[] args = {reporterMethod};

        String stringToPrint = msgFmt.format( args );

        System.err.println( stringToPrint );

        e.printStackTrace( System.err );
    }

    void poo()
    {
        try
        {
            // Tee standard output
            PrintStream out = new PrintStream( new FileOutputStream( "out.log" ) );

            PrintStream tee = new TeeStream( System.out, out );

            System.setOut( tee );

            // Tee standard error
            PrintStream err = new PrintStream( new FileOutputStream( "err.log" ) );

            tee = new TeeStream( System.err, err );

            System.setErr( tee );
        }
        catch ( FileNotFoundException e )
        {
        }

        // Write to standard output and error and the log files
        System.out.println( "welcome" );
        System.err.println( "error" );
    }

}
