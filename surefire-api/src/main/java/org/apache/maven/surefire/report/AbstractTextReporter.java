package org.apache.maven.surefire.report;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    private final String format;

    private List testResults;

    protected AbstractTextReporter( String format )
    {
        this.format = format;
    }

    protected AbstractTextReporter( PrintWriter writer, String format )
    {
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
        throws IOException
    {
        super.testSetStarting( report );

        testResults = new ArrayList();
    }

    public void testSetCompleted( ReportEntry report )
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

        buf.append( "Tests run: " );
        buf.append( completedCount );
        buf.append( ", Failures: " );
        buf.append( failures );
        buf.append( ", Errors: " );
        buf.append( errors );
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
}
