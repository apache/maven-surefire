package org.apache.maven.surefire.report;

import java.io.PrintStream;

/**
 * @author <a href="mailto:kristian@zenior.no">Kristian Rosenvold</a>
 */
public class DefaultDirectConsoleReporter
    implements DirectConsoleReporter
{
    private final PrintStream systemOut;

    public DefaultDirectConsoleReporter( PrintStream systemOut ) {
        this.systemOut = systemOut;
    }

    public void writeMessage(String message) {
        systemOut.println(message);
    }
}
