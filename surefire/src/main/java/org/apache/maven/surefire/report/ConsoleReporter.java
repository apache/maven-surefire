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

/**
 * Console reporter.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 */
public class ConsoleReporter
    extends AbstractConsoleReporter
{
    public void batteryCompleted( ReportEntry report )
    {
        StringBuffer batterySummary = getBatterySummary();

        if ( failures > 0 || errors > 0 )
        {
            batterySummary.append( " <<<<<<<< FAILURE !! " );
        }

        writer.println( batterySummary );

        writer.flush();

        completedCount = 0;

        errors = 0;

        failures = 0;
    }

    public void runAborted( ReportEntry report )
    {
        writer.println( "RUN ABORTED" );
        writer.println( report.getSource().getClass().getName() );
        writer.println( report.getName() );
        writer.println( report.getMessage() );
        writer.println( report.getThrowable().getMessage() );
        writer.flush();
    }

    public void batteryAborted( ReportEntry report )
    {
        writer.println( "BATTERY ABORTED" );
        writer.println( report.getSource().getClass().getName() );
        writer.println( report.getName() );
        writer.println( report.getMessage() );
        writer.println( report.getThrowable().getMessage() );
        writer.flush();
    }
}
