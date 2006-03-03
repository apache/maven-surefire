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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Base class for console reporters.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractConsoleReporter
    extends AbstractTextReporter
{
    protected static final int BUFFER_SIZE = 4096;

    protected AbstractConsoleReporter( String format )
    {
        // TODO: use logger
        super( new PrintWriter( new OutputStreamWriter( new BufferedOutputStream( System.out, BUFFER_SIZE ) ) ),
               format );
    }

    public void testSetStarting( ReportEntry report )
        throws IOException
    {
        super.testSetStarting( report );

        String message = "Running " + report.getName();
        if ( !report.getGroup().equals( report.getName() ) )
        {
            message = message + " (of " + report.getGroup() + ")";
        }
        writeMessage( message );
    }

    public void runStarting( int testCount )
    {
        writeHeading( "" );
        writeHeading( "-------------------------------------------------------" );
        writeHeading( " T E S T S" );
        writeHeading( "-------------------------------------------------------" );
    }

    public void writeHeading( String message )
    {
        writer.println( message );
        writer.flush();
    }

    public void runAborted( ReportEntry report )
    {
        printAbortionError( "RUN ABORTED", report );
    }

    public void testSetAborted( ReportEntry report )
    {
        printAbortionError( "TEST SET ABORTED", report );
    }

    private void printAbortionError( String msg, ReportEntry report )
    {
        writer.println( msg );
        writer.println( report.getSource().getClass().getName() );
        writer.println( report.getName() );
        writer.println( report.getMessage() );
        writer.println( report.getThrowable().getMessage() );
        writer.flush();
    }
}
