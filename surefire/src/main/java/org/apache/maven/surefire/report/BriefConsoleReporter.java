package org.apache.maven.surefire.report;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Brief format console reporter.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 */

public class BriefConsoleReporter
    extends AbstractConsoleReporter
{
    private StringBuffer reportContent;

    public void batteryStarting( ReportEntry report )
    {
        super.batteryStarting( report );

        reportContent = new StringBuffer();
    }

    public void batteryCompleted( ReportEntry report )
    {
        StringBuffer batterySummary = getBatterySummary();

        batterySummary.append( NL );
        batterySummary.append( "[surefire] " ).append( NL );

        reportContent = batterySummary.append( reportContent );

        writer.println( batterySummary );

        writer.flush();
    }

    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
        super.testError( report, stdOut, stdErr );

        reportContent.append( "[surefire] " );
        appendOutput( report, "ERROR" );
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        super.testFailed( report, stdOut, stdErr );

        reportContent.append( "[surefire] " );
        appendOutput( report, "FAILURE" );
    }

    private void appendOutput( ReportEntry report, String msg )
    {
        reportContent.append( report.getName() );

        long runTime = this.endTime - this.startTime;

        reportContent.append( "  Time elapsed: " ).append( elapsedTimeAsString( runTime ) ).append( " sec" );

        reportContent.append( "  <<< " ).append( msg ).append( "!" ).append( NL );

        reportContent.append( getStackTrace( report ) ).append( NL );
    }

    public void dispose()
    {
        errors = 0;

        failures = 0;

        completedCount = 0;
    }

    /**
     * Returns stacktrace as String.
     *
     * @param report ReportEntry object.
     * @return stacktrace as string.
     */
    private String getStackTrace( ReportEntry report )
    {
        StringWriter writer = new StringWriter();

        report.getThrowable().printStackTrace( new PrintWriter( writer ) );

        writer.flush();

        return writer.toString();
    }
}
