package org.codehaus.surefire.report;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.NumberFormat;

public class OutputStreamReport
    implements Report
{
    private static final int BUFFER_SIZE = 4096;

    private PrintWriter writer;

    private int completedCount;

    private int failures;

    private long startTime;

    private long endTime;

    public OutputStreamReport( OutputStream os )
    {
        super();

        if ( os == null )
        {
            throw new NullPointerException( "os is null" );
        }

        writer = new PrintWriter( new OutputStreamWriter( new BufferedOutputStream( os, BUFFER_SIZE ) ) );
    }

    public OutputStreamReport( String filename ) throws IOException
    {
        if ( filename == null )
        {

            throw new NullPointerException( "filename is null" );
        }

        File file = new File( filename );

        writer = new PrintWriter( new BufferedOutputStream( new FileOutputStream( file ), BUFFER_SIZE ) );
    }

    // ----------------------------------------------------------------------
    // Run
    // ----------------------------------------------------------------------

    public void runStarting( int testCount )
    {
        writer.write( "\n" );
        writer.write( "-------------------------------------------------------\n" );
        writer.write( "T E S T S\n" );
        writer.write( "-------------------------------------------------------\n" );
        writer.flush();
    }

    public void runStopped()
    {
    }

    public void runAborted( ReportEntry report )
    {
        report.getThrowable().printStackTrace();
    }

    public void runCompleted()
    {
    }

    // ----------------------------------------------------------------------
    // Battery
    // ----------------------------------------------------------------------

    public void batteryStarting( ReportEntry report )
    {
        writer.println( "[surefire] Running " + report.getName() );
    }

    public void batteryCompleted( ReportEntry report )
    {
        long runTime = endTime - startTime;

        writer.println( "[surefire] Tests run: " + completedCount +
                             ", Failures: " + failures +
                             ", Errors: " + failures +
                             ", Time elapsed: " + elapsedTimeAsString( runTime ) + " sec" );

        writer.println();

        writer.flush();

        completedCount = 0;

        failures = 0;
    }

    public void batteryAborted( ReportEntry report )
    {
        report.getThrowable().printStackTrace();
    }

    private String elapsedTimeAsString( long runTime )
    {
        return NumberFormat.getInstance().format( (double) runTime / 1000 );
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
        if ( report == null )
        {
            throw new NullPointerException( "report is null" );
        }

        ++completedCount;

        endTime = System.currentTimeMillis();
    }

    public void testFailed( ReportEntry report )
    {
        if ( report == null )
        {
            throw new NullPointerException( "report is null" );
        }

        ++completedCount;

        ++failures;

        endTime = System.currentTimeMillis();

        report.getThrowable().printStackTrace();
    }

    public void dispose()
    {
        writer.close();
    }
}
