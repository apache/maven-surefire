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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

public class BriefConsoleReporter
    extends AbstractReporter
{
    private static final int BUFFER_SIZE = 4096;
    
    private PrintWriter writer;
    
    private StringBuffer reportContent;
    
    private long batteryStartTime;
    
    String newLine = System.getProperty("line.separator");
    
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
        throws Exception
    {
        batteryStartTime = System.currentTimeMillis();
        
        reportContent = new StringBuffer();
        
        writer.println( "[surefire] Running " + report.getName() );
    }

    public void batteryCompleted( ReportEntry report )
    {
        long runTime = System.currentTimeMillis() - this.batteryStartTime;
        
        StringBuffer batterySummary = new StringBuffer();

        batterySummary.append( "[surefire] Tests run: " + String.valueOf( this.getNbTests() ) )
                      .append( ", Failures: " + String.valueOf( this.getNbFailures() ) )
                      .append( ", Errors: " + String.valueOf( this.getNbErrors() ))
                      .append( ", Time elapsed: " + elapsedTimeAsString( runTime ))
                      .append(" sec")
                      .append(newLine)
                      .append("[surefire] " +  newLine);
                      
        reportContent = batterySummary.append(reportContent);
        
        writer.println( reportContent.toString() );
        
        writer.flush();
    }

    public void testStarting( ReportEntry report )
    { 
        super.testStarting(report);        
    }


    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
        super.testError(report, stdOut, stdErr);

        reportContent.append("[surefire] " + report.getName() );
        
        long runTime = this.endTime - this.startTime;
        
        writeTimeElapsed(runTime);
        
        reportContent.append("  <<< ERROR!" + newLine);
 
        reportContent.append( getStackTrace( report ) + newLine );
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        super.testFailed(report, stdOut, stdErr);
        
        reportContent.append("[surefire] " + report.getName() );
        
        long runTime = this.endTime - this.startTime;
        
        writeTimeElapsed(runTime);
        
        reportContent.append("  <<< FAILURE!" + newLine);

        reportContent.append( getStackTrace( report ) + newLine );
    }

    public void dispose()
    {
        errors = 0;
        
        failures = 0;
        
        completedCount = 0;       
    }

    private void writeTimeElapsed(long sec)
    {
        reportContent.append( "  Time elapsed: " + elapsedTimeAsString( sec ) + " sec" );
    }
    
   
    private String getStackTrace(ReportEntry report)
    {   
        StringWriter writer = new StringWriter();
        
        report.getThrowable().printStackTrace(new PrintWriter(writer));
      
        writer.flush();
        
        return writer.toString();
    }
}
