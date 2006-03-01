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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Text file reporter.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 */
public class FileReporter
    extends AbstractReporter
{
    private PrintWriter writer;

    private StringBuffer reportContent;

    private long batteryStartTime;

    private static final String NL = System.getProperty( "line.separator" );

    public void batteryStarting( ReportEntry report )
        throws IOException
    {
        batteryStartTime = System.currentTimeMillis();

        reportContent = new StringBuffer();

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
        long runTime = System.currentTimeMillis() - this.batteryStartTime;

        StringBuffer batterySummary = new StringBuffer();

        batterySummary.append( "Tests run: " ).append( String.valueOf( this.getNbTests() ) );
        batterySummary.append( ", Failures: " ).append( String.valueOf( this.getNbFailures() ) );
        batterySummary.append( ", Errors: " ).append( String.valueOf( this.getNbErrors() ) );
        batterySummary.append( ", Time elapsed: " ).append( elapsedTimeAsString( runTime ) );
        batterySummary.append( " sec" );
        batterySummary.append( NL );
        batterySummary.append( NL );

        reportContent = batterySummary.append( reportContent );

        writer.println( reportContent.toString() );

        writer.flush();

        writer.close();
    }

    public void testStarting( ReportEntry report )
    {
        super.testStarting( report );

        reportContent.append( report.getName() );
    }

    public void testSucceeded( ReportEntry report )
    {
        super.testSucceeded( report );

        long runTime = this.endTime - this.startTime;

        writeTimeElapsed( runTime );

        reportContent.append( NL );
    }

    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
        super.testError( report, stdOut, stdErr );

        long runTime = this.endTime - this.startTime;

        writeTimeElapsed( runTime );

        reportContent.append( "  <<< ERROR!" ).append( NL );

        writeStdLogs( stdOut, stdErr );

        reportContent.append( getStackTrace( report ) ).append( NL );
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        super.testFailed( report, stdOut, stdErr );

        long runTime = this.endTime - this.startTime;

        writeTimeElapsed( runTime );

        reportContent.append( "  <<< FAILURE!" ).append( NL );

        writeStdLogs( stdOut, stdErr );

        reportContent.append( getStackTrace( report ) ).append( NL );
    }

    public void dispose()
    {
        errors = 0;

        failures = 0;

        completedCount = 0;
    }

    private void writeTimeElapsed( long sec )
    {
        reportContent.append( "  Time elapsed: " ).append( elapsedTimeAsString( sec ) ).append( " sec" );
    }

    private void writeStdLogs( String stdOut, String stdErr )
    {
        reportContent.append( NL );

        reportContent.append( "[ stdout ] ---------------------------------------------------------------" );

        reportContent.append( NL );

        reportContent.append( stdOut ).append( NL );

        reportContent.append( NL );

        reportContent.append( "[ stderr ] ---------------------------------------------------------------" );

        reportContent.append( NL );

        reportContent.append( stdErr ).append( NL );

        reportContent.append( NL );

        reportContent.append( "[ stacktrace ] -----------------------------------------------------------" );

        reportContent.append( NL );
    }

    /**
     * @param report ReportEntry Object.
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
