package org.apache.maven.surefire.report;

import java.io.PrintStream;

/**
 * @author <a href="mailto:kristian@zenior.no">Kristian Rosenvold</a>
 */
public class DefaultDirectConsoleReporter
    implements ConsoleLogger
{
    private final PrintStream systemOut;

    public DefaultDirectConsoleReporter( PrintStream systemOut )
    {
        this.systemOut = systemOut;
    }

    public void info( String message )
    {
        systemOut.println( message );
    }
}
