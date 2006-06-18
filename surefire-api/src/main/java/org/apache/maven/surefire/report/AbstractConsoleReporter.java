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

/**
 * Base class for console reporters.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractConsoleReporter
    extends AbstractTextReporter
{
    private static final String TEST_SET_STARTING_PREFIX = "Running ";

    private static final String TEST_SET_STARTING_GROUP_PREFIX = " (of ";

    private static final String TEST_SET_STARTING_GROUP_SUFIX = ")";

    protected static final int BUFFER_SIZE = 4096;

    protected AbstractConsoleReporter( String format, Boolean trimStackTrace )
    {
        // TODO: use logger
        super( new PrintWriter( new OutputStreamWriter( new BufferedOutputStream( System.out, BUFFER_SIZE ) ) ), format,
               trimStackTrace );
    }

    public void testSetStarting( ReportEntry report )
        throws ReporterException
    {
        super.testSetStarting( report );

        writeMessage( getTestSetStartingMessage( report ) );
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
        writer.println( getStackTrace( report ) );
        writer.flush();
    }

    /**
     * Get the test set starting message for a report.
     * eg. "Running org.foo.BarTest ( of group )"
     * 
     * @todo internationalize
     * @param report report whose test set is starting
     * @return the message
     */
    public static String getTestSetStartingMessage( ReportEntry report )
    {
        StringBuffer message = new StringBuffer();
        message.append( TEST_SET_STARTING_PREFIX );
        message.append( report.getName() );

        if ( report.getGroup() != null && !report.getName().equals( report.getGroup() ) )
        {
            message.append( TEST_SET_STARTING_GROUP_PREFIX );
            message.append( report.getGroup() );
            message.append( TEST_SET_STARTING_GROUP_SUFIX );
        }
        return message.toString();
    }

    /**
     * Parses a Surefire test set starting message into a {@link ReportEntry} object.
     * Only name and group will be set if applicable.
     * 
     * @param message
     * @return the parsed {@link ReportEntry}
     */
    public static ReportEntry parseTestSetStartingMessage( String message )
    {
        ReportEntry reportEntry = new ReportEntry();
        int i = message.indexOf( TEST_SET_STARTING_GROUP_PREFIX );
        int j;
        if ( i >= 0 )
        {
            j = message.indexOf( TEST_SET_STARTING_GROUP_SUFIX );
            if ( j <= 0 )
            {
                throw new RuntimeException( "Message provided can not be parsed" );
            }
            reportEntry.setGroup( message.substring( i + TEST_SET_STARTING_GROUP_PREFIX.length(), j ) );
        }
        else
        {
            i = message.length();
            if ( i <= 0 )
            {
                throw new RuntimeException( "Message provided can not be parsed" );
            }
        }
        reportEntry.setName( message.substring( TEST_SET_STARTING_PREFIX.length(), i ) );
        return reportEntry;
    }

    /**
     * Check if the String passed as argument is a "test starting" message.
     * If so it can be passed to {@link #parseTestSetStartingMessage(String)}
     * 
     * @param message the message to check
     * @return true if it is a "test starting" message
     */
    public static boolean isTestSetStartingMessage( String message )
    {
        return message.startsWith( TEST_SET_STARTING_PREFIX );
    }

}
