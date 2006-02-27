package org.apache.maven.surefire.report;

import org.apache.maven.surefire.report.SummaryConsoleReporter;
import org.apache.maven.surefire.report.ForkingReport;

/**
 * Summary format console reporter.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 */
public class ForkingSummaryConsoleReporter
    extends ForkingConsoleReporter
{
    public void batteryStarting( ReportEntry report )
        throws Exception
    {
    }

    public void batteryCompleted( ReportEntry report )
    {
        if ( failures > 0 || errors > 0 )
        {
            println( "[surefire] " + report.getName() + " <<<<<<<< FAILURE !! " );
        }

        completedCount = 0;

        errors = 0;

        failures = 0;
    }
}
