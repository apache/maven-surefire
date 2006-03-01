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

import java.io.IOException;

/**
 * Brief format console reporter.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 */

public class BriefConsoleReporter
    extends AbstractConsoleReporter
{
    protected StringBuffer reportContent;

    public void batteryStarting( ReportEntry report )
        throws IOException
    {
        super.batteryStarting( report );

        reportContent = new StringBuffer();
    }

    public void batteryCompleted( ReportEntry report )
    {
        StringBuffer batterySummary = getBatterySummary();

        batterySummary.append( NL );
        batterySummary.append( NL );

        batterySummary.append( reportContent );

        writeMessage( batterySummary.toString() );
    }

    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
        super.testError( report, stdOut, stdErr );

        appendOutput( report, "ERROR" );
    }

    private void appendOutput( ReportEntry report, String msg )
    {
        reportContent.append( "[surefire] " );
        reportContent.append( getOutput( report, msg ) );
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        super.testFailed( report, stdOut, stdErr );

        appendOutput( report, "FAILURE" );
    }
}
