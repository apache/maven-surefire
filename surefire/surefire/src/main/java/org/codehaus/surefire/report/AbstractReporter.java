package org.codehaus.surefire.report;

import java.text.NumberFormat;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public abstract class AbstractReporter
    implements Reporter
{
    private String reportsDirectory;

    protected int completedCount;

    protected int errors;

    protected int failures;

    protected long startTime;

    protected long endTime;

    public void setReportsDirectory( String reportsDirectory )
    {
        this.reportsDirectory = reportsDirectory;
    }

    public String getReportsDirectory()
    {
        return reportsDirectory;
    }

    // ----------------------------------------------------------------------
    // Report interface
    // ----------------------------------------------------------------------

    public void writeMessage( String message )
    {
    }

    public void runStarting( int testCount )
    {
    }

    public void runCompleted()
    {
    }

    public void runStopped()
    {
    }

    public void runAborted( ReportEntry report )
    {
    }

    public void batteryStarting( ReportEntry report )
        throws Exception
    {
    }

    public void batteryCompleted( ReportEntry report )
    {
    }

    public void batteryAborted( ReportEntry report )
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
        ++completedCount;

        endTime = System.currentTimeMillis();
    }

    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
        ++completedCount;

        ++errors;

        endTime = System.currentTimeMillis();
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        ++completedCount;

        ++failures;

        endTime = System.currentTimeMillis();
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

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void dispose()
    {
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected String elapsedTimeAsString( long runTime )
    {
        return NumberFormat.getInstance().format( (double) runTime / 1000 );
    }
}
