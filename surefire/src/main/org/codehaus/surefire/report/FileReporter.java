package org.codehaus.surefire.report;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class FileReporter
    extends AbstractReporter
{
    private PrintWriter writer;

    public void runStarting( int testCount )
    {
    }

    public void batteryStarting( ReportEntry report )
        throws Exception
    {
        File reportFile = new File( getReportsDirectory(), report.getName() + ".txt" );

        writer = new PrintWriter( new FileWriter( reportFile ) );

        writer.println( "-------------------------------------------------------------------------------" );

        writer.println( "Battery: " + report.getName() );

        writer.println( "-------------------------------------------------------------------------------" );
    }

    public void batteryCompleted( ReportEntry report )
    {
        writer.flush();

        writer.close();
    }

    public void testStarting( ReportEntry report )
    {
        writer.println( report.getName() );
    }

    public void testSucceeded( ReportEntry report )
    {
    }

    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
        writeStdLogs( stdOut, stdErr );

        report.getThrowable().printStackTrace( writer );
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        writeStdLogs( stdOut, stdErr );

        report.getThrowable().printStackTrace( writer );
    }

    public void dispose()
    {
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void writeStdLogs( String stdOut, String stdErr )
    {
        writer.println();

        writer.println( "[ stdout ] ---------------------------------------------------------------" );

        writer.println();

        writer.print( stdOut );

        writer.println();

        writer.println( "[ stderr ] ---------------------------------------------------------------" );

        writer.println();

        writer.print( stdErr );

        writer.println();

        writer.println( "[ stacktrace ] -----------------------------------------------------------" );

        writer.println();
    }
}
