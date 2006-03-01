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

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Brief format console reporter.
 *
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id$
 */

public class BriefConsoleReporter
    extends AbstractReporter
{
    private static final int BUFFER_SIZE = 4096;

    private PrintWriter writer;

    private StringBuffer reportContent;

    private long batteryStartTime;

    private static final String NL = System.getProperty( "line.separator" );

    public BriefConsoleReporter()
    {
        writer = new PrintWriter( new OutputStreamWriter( new BufferedOutputStream( System.out, BUFFER_SIZE ) ) );
    }

    public void writeMessage( String message )
    {
        writer.println( message );
        writer.flush();
    }

    public void runStarting( int testCount )
    {
        writer.println();
        writer.println( "-------------------------------------------------------" );
        writer.println( " T E S T S" );
        writer.println( "-------------------------------------------------------" );
        writer.flush();

    }

    public void batteryStarting( ReportEntry report )
    {
        batteryStartTime = System.currentTimeMillis();

        reportContent = new StringBuffer();

        writer.println( "[surefire] Running " + report.getName() );
    }

    public void batteryCompleted( ReportEntry report )
    {
        long runTime = System.currentTimeMillis() - this.batteryStartTime;

        StringBuffer batterySummary = new StringBuffer();

        batterySummary.append( "[surefire] Tests run: " );
        batterySummary.append( String.valueOf( this.getNbTests() ) );
        batterySummary.append( ", Failures: " ).append( String.valueOf( this.getNbFailures() ) );
        batterySummary.append( ", Errors: " ).append( String.valueOf( this.getNbErrors() ) );
        batterySummary.append( ", Time elapsed: " ).append( elapsedTimeAsString( runTime ) );
        batterySummary.append( " sec" );
        batterySummary.append( NL );
        batterySummary.append( "[surefire] " ).append( NL );

        reportContent = batterySummary.append( reportContent );

        writer.println( reportContent.toString() );

        writer.flush();
    }

    public void testStarting( ReportEntry report )
    {
        super.testStarting( report );
    }


    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
        super.testError( report, stdOut, stdErr );

        reportContent.append( "[surefire] " ).append( report.getName() );

        long runTime = this.endTime - this.startTime;

        writeTimeElapsed( runTime );

        reportContent.append( "  <<< ERROR!" ).append( NL );

        reportContent.append( getStackTrace( report ) ).append( NL );
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        super.testFailed( report, stdOut, stdErr );

        reportContent.append( "[surefire] " ).append( report.getName() );

        long runTime = this.endTime - this.startTime;

        writeTimeElapsed( runTime );

        reportContent.append( "  <<< FAILURE!" ).append( NL );

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
