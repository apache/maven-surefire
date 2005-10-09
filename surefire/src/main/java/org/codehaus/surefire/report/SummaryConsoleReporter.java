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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Summary format console reporter.
 * @author <a href="mailto:jruiz@exist.com">Johnny R. Ruiz III</a>
 * @version $Id: SummaryConsoleReporter.java 61 2005-10-07 04:07:33Z jruiz $
 */
public class SummaryConsoleReporter
    extends AbstractReporter
{
    private static final int BUFFER_SIZE = 4096;

    private PrintWriter writer;
    
    public SummaryConsoleReporter()
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

    public void batteryCompleted( ReportEntry report )
    {
        if ( failures > 0 || errors > 0 )
        {
            writer.println( "[surefire] " + report.getName() + " <<<<<<<< FAILURE !! " );
        }
        
        writer.flush();

        completedCount = 0;

        errors = 0;

        failures = 0;
    }
}
