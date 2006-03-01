package org.apache.maven.surefire.report;

public class ForkingConsoleReporter
    extends ConsoleReporter
{
    public void println( String message )
    {
        writer.write( ForkingReport.FORKING_PREFIX_STANDARD );

        writer.println( message );

        writer.flush();
    }

    public void print( String message )
    {
        writer.write( ForkingReport.FORKING_PREFIX_STANDARD );

        writer.print( message );

        writer.flush();
    }

    public void runStarting( int testCount )
    {
        writer.print( ForkingReport.FORKING_PREFIX_HEADING );

        writer.println( "" );

        writer.print( ForkingReport.FORKING_PREFIX_HEADING );

        writer.println( "-------------------------------------------------------" );

        writer.print( ForkingReport.FORKING_PREFIX_HEADING );

        writer.println( " T E S T S" );

        writer.print( ForkingReport.FORKING_PREFIX_HEADING );

        writer.println( "-------------------------------------------------------" );

        writer.flush();
    }
}
