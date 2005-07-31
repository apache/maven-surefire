package org.codehaus.surefire.report;

/*
 * Copyright 2001-2005 The Codehaus.
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

        File reportDir = reportFile.getParentFile();

        reportDir.mkdirs();

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
