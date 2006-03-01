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

public class ConsoleReporter
    extends AbstractReporter
{
    protected static final int BUFFER_SIZE = 4096;

    protected PrintWriter writer;

    protected long batteryStartTime;

    public ConsoleReporter()
    {
        writer = new PrintWriter( new OutputStreamWriter( new BufferedOutputStream( System.out, BUFFER_SIZE ) ) );
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void println( String message )
    {
        writer.println( message );

        writer.flush();
    }

    public void print( String message )
    {
        writer.print( message );

        writer.flush();
    }

    // ----------------------------------------------------------------------
    // Run
    // ----------------------------------------------------------------------

    public void runStarting( int testCount )
    {
        println( "" );
        println( "-------------------------------------------------------" );
        println( " T E S T S" );
        println( "-------------------------------------------------------" );
    }

    public void runAborted( ReportEntry report )
    {
        println( "RUN ABORTED" );
        println( report.getSource().getClass().getName() );
        println( report.getName() );
        println( report.getMessage() );
        println( report.getThrowable().getMessage() );
    }

    public void batteryAborted( ReportEntry report )
    {
        println( "BATTERY ABORTED" );
        println( report.getSource().getClass().getName() );
        println( report.getName() );
        println( report.getMessage() );
        println( report.getThrowable().getMessage() );
    }

    // ----------------------------------------------------------------------
    // Battery
    // ----------------------------------------------------------------------

    public void batteryStarting( ReportEntry report )
        throws Exception
    {
        batteryStartTime = System.currentTimeMillis();

        println( "[surefire] Running " + report.getName() );
    }

    public void batteryCompleted( ReportEntry report )
    {
        long runTime = System.currentTimeMillis() - batteryStartTime;

        print( "[surefire] Tests run: " + completedCount + ", Failures: " + failures + ", Errors: " + errors +
            ", Time elapsed: " + elapsedTimeAsString( runTime ) + " sec" );

        if ( failures > 0 || errors > 0 )
        {
            writer.print( " <<<<<<<< FAILURE !! " );
        }

        writer.println( "" );

        writer.flush();

        completedCount = 0;

        errors = 0;

        failures = 0;
    }
}
