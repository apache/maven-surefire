package org.codehaus.surefire.report;

import org.codehaus.surefire.Surefire;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

public class ReportManager
{
    private int completedCount;

    private int errors;

    private int failures;

    private List reports;

    public ReportManager( List reports )
    {
        if ( reports == null )
        {
            throw new NullPointerException();
        }

        this.reports = reports;
    }

    public void addReporter( Report reporter )
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

    public void removeReport( Report report )
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
            Report report = (Report) i.next();

            report.writeMessage( message );
        }
    }

    public void resume()
    {
        writeMessage( "" );
        writeMessage( "Resume :" );
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
            Report report = (Report) i.next();

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
            Report reporter = (Report) it.next();

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
            Report reporter = (Report) it.next();

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
            Report reporter = (Report) it.next();

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

    public void batteryStarting( ReportEntry report )
    {

        if ( report == null )
        {
            throw new NullPointerException();
        }

        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Report reporter = (Report) it.next();

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

        if ( report == null )
        {
            throw new NullPointerException();
        }

        if ( !reports.isEmpty() )
        {
            Report reporter = (Report) reports.get( 0 );
            errors += reporter.getNbErrors();
            failures += reporter.getNbFailures();
            completedCount += reporter.getNbTests();
        }

        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Report reporter = (Report) it.next();

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
        if ( report == null )
        {
            throw new NullPointerException();
        }

        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Report reporter = (Report) it.next();

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
        if ( report == null )
        {
            throw new NullPointerException();
        }

        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Report reporter = (Report) it.next();

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

        if ( report == null )
        {
            throw new NullPointerException();
        }

        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Report reporter = (Report) it.next();

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
        if ( reportEntry == null )
        {
            throw new NullPointerException();
        }

        for ( Iterator it = reports.iterator(); it.hasNext(); )
        {
            Report reporter = (Report) it.next();

            try
            {
                if ( "failure".equals( typeError ) )
                {
                    reporter.testFailed( reportEntry );
                }
                else
                {
                    reporter.testError( reportEntry );
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
            Report reporter = (Report) it.next();

            try
            {
                reporter.dispose();
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
}
