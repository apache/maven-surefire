package org.apache.maven.plugin.surefire.report;

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

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;

import java.io.PrintWriter;

/**
 * Text based reporter.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractTextReporter
    implements Reporter
{
    public static final String BRIEF = "brief";

    public static final String PLAIN = "plain";

    public static final String SUMMARY = "summary";

    private final boolean isPlain;

    private final boolean isBrief;

    protected PrintWriter writer;

    protected AbstractTextReporter( String format )
    {
        isPlain = PLAIN.equals( format );
        isBrief = BRIEF.equals( format );
    }

    protected AbstractTextReporter( PrintWriter writer, String format )
    {
        this( format );
        this.writer = writer;
    }


    public void setWriter( PrintWriter writer )
    {
        this.writer = writer;
    }

    public void writeMessage( String message )
    {
        if ( writer != null )
        {
            writer.print( message );

            writer.flush();
        }
    }


    public void testSetCompleted( ReportEntry report, TestSetStats testSetStats )
        throws ReporterException
    {
        writeMessage( testSetStats.getTestSetSummary( report.getElapsed() ) );

        if ( isBrief || isPlain )
        {
            for ( String testResult : testSetStats.getTestResults() )
            {
                writeMessage( testResult );
            }
        }
    }


    public void reset()
    {
        if ( writer != null )
        {
            writer.flush();
        }
    }

    public void testSucceeded( ReportEntry report, TestSetStats testSetStats )
    {
    }

    public void testSkipped( ReportEntry report, TestSetStats testSetStats )
    {
    }

    public void testError( ReportEntry report, String stdOut, String stdErr, TestSetStats testSetStats )
    {
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr, TestSetStats testSetStats )
    {
    }

    public void testStarting( ReportEntry report )
    {
    }
}
