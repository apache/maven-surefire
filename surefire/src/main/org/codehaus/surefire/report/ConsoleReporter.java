package org.codehaus.surefire.report;

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class ConsoleReporter
    extends AbstractReporter
{
    private static final int BUFFER_SIZE = 4096;

    private PrintWriter writer;

    public ConsoleReporter()
    {
        writer = new PrintWriter( new OutputStreamWriter( new BufferedOutputStream( System.out, BUFFER_SIZE ) ) );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void writeMessage( String message )
    {
        writer.println( message );

        writer.flush();
    }

    // ----------------------------------------------------------------------
    // Run
    // ----------------------------------------------------------------------

    public void runStarting( int testCount )
    {
        writer.println();
        writer.println( "-------------------------------------------------------" );
        writer.println( " T E S T S" );
        writer.println( "-------------------------------------------------------" );
        writer.flush();
    }

    public void runAborted( ReportEntry report )
    {
        writer.println( "ABORTED" );
        writer.println( report.getSource().getClass().getName() );
        writer.println( report.getName() );
        writer.println( report.getMessage() );
        writer.println( report.getThrowable().getMessage() );
        writer.flush();
    }
    public void batteryAborted( ReportEntry report )
    {
        writer.println( "ABORTED" );
        writer.println( report.getSource().getClass().getName() );
        writer.println( report.getName() );
        writer.println( report.getMessage() );
        writer.println( report.getThrowable().getMessage() );
        writer.flush();
    }

    // ----------------------------------------------------------------------
    // Battery
    // ----------------------------------------------------------------------

    public void batteryStarting( ReportEntry report )
        throws Exception
    {
        writer.println( "[surefire] Running " + report.getName() );
    }

    public void batteryCompleted( ReportEntry report )
    {
        long runTime = endTime - startTime;

        writer.print( "[surefire] Tests run: " + completedCount +
                             ", Failures: " + failures +
                             ", Errors: " + errors +
                             ", Time elapsed: " + elapsedTimeAsString( runTime ) + " sec" );

        if ( failures > 0 || errors > 0 )
        {
            writer.print( " <<<<<<<< FAILURE !! " );
        }
        
        writer.println();
        
        writer.flush();

        completedCount = 0;

        errors = 0;

        failures = 0;
    }
}
