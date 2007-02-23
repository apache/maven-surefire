package org.apache.maven.surefire.report;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Text based reporter.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractTextReporter
    extends AbstractReporter
{
    protected static final String BRIEF = "brief";

    protected static final String PLAIN = "plain";

    protected static final String SUMMARY = "summary";

    protected PrintWriter writer;

    private static final String TEST_SET_COMPLETED_PREFIX = "Tests run: ";

    private final String format;

    private List testResults;

    protected AbstractTextReporter( String format, Boolean trimStackTrace )
    {
        super( trimStackTrace );

        this.format = format;
    }

    protected AbstractTextReporter( PrintWriter writer, String format, Boolean trimStackTrace )
    {
        super( trimStackTrace );

        this.writer = writer;

        this.format = format;
    }

    public void setWriter( PrintWriter writer )
    {
        this.writer = writer;
    }

    public void writeMessage( String message )
    {
        if ( writer != null )
        {
            writer.println( message );

            writer.flush();
        }
    }

    public void testSucceeded( ReportEntry report )
    {
        super.testSucceeded( report );

        if ( PLAIN.equals( format ) )
        {
            testResults.add( getElapsedTimeSummary( report ) );
        }
    }

    public void testSkipped( ReportEntry report )
    {
        super.testSkipped( report );

        if ( PLAIN.equals( format ) )
        {
            testResults.add( report.getName() + " skipped" );
        }
    }

    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
        super.testError( report, stdOut, stdErr );

        testResults.add( getOutput( report, "ERROR" ) );
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        super.testFailed( report, stdOut, stdErr );

        testResults.add( getOutput( report, "FAILURE" ) );
    }

    public void testSetStarting( ReportEntry report )
        throws ReporterException
    {
        super.testSetStarting( report );

        testResults = new ArrayList();
    }

    public void testSetCompleted( ReportEntry report )
        throws ReporterException
    {
        super.testSetCompleted( report );

        writeMessage( getTestSetSummary() );

        if ( format.equals( BRIEF ) || format.equals( PLAIN ) )
        {
            for ( Iterator i = testResults.iterator(); i.hasNext(); )
            {
                writeMessage( (String) i.next() );
            }
        }
    }

    protected String getTestSetSummary()
    {
        StringBuffer buf = new StringBuffer();

        buf.append( TEST_SET_COMPLETED_PREFIX );
        buf.append( completedCount );
        buf.append( ", Failures: " );
        buf.append( failures );
        buf.append( ", Errors: " );
        buf.append( errors );
        buf.append( ", Skipped: " );
        buf.append( skipped );
        buf.append( ", Time elapsed: " );
        buf.append( elapsedTimeAsString( System.currentTimeMillis() - testSetStartTime ) );
        buf.append( " sec" );

        if ( failures > 0 || errors > 0 )
        {
            buf.append( " <<< FAILURE!" );
        }

        return buf.toString();
    }

    protected String getElapsedTimeSummary( ReportEntry report )
    {
        StringBuffer reportContent = new StringBuffer();
        long runTime = this.endTime - this.startTime;

        reportContent.append( report.getName() );
        reportContent.append( "  Time elapsed: " );
        reportContent.append( elapsedTimeAsString( runTime ) );
        reportContent.append( " sec" );

        return reportContent.toString();
    }

    protected String getOutput( ReportEntry report, String msg )
    {
        StringBuffer buf = new StringBuffer();

        buf.append( getElapsedTimeSummary( report ) );

        buf.append( "  <<< " ).append( msg ).append( "!" ).append( NL );

        buf.append( getStackTrace( report ) );

        return buf.toString();
    }

    /**
     * Check if the String passed as argument is a "test set completed" message.
     *
     * @param message the message to check
     * @return true if it is a "test set completed" message
     */
    public static boolean isTestSetCompletedMessage( String message )
    {
        return message.startsWith( TEST_SET_COMPLETED_PREFIX );
    }

}
