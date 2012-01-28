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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.util.NestedRuntimeException;

/**
 * Surefire output consumer proxy that writes test output to a {@link java.io.File} for each test suite.
 * <p/>
 * This class is not threadsafe, but can be encapsulated with a SynchronizedOutputConsumer. It may still be
 * accessed from different threads (serially).
 *
 * @author <a href="mailto:carlos@apache.org">Carlos Sanchez</a>
 * @version $Id$
 * @since 2.1
 */
public class ConsoleOutputFileReporter
    implements Reporter
{
    private final File reportsDirectory;

    private PrintWriter printWriter = null;

    private String reportEntryName;

    private final String reportNameSuffix;

    public ConsoleOutputFileReporter( File reportsDirectory )
    {
        this( reportsDirectory, null );
    }

    public ConsoleOutputFileReporter( File reportsDirectory, String reportNameSuffix )
    {
        this.reportsDirectory = reportsDirectory;
        this.reportNameSuffix = reportNameSuffix;
    }

    public void testSetStarting( ReportEntry reportEntry )
    {
        this.reportEntryName = reportEntry.getName();
    }

    public void testSetCompleted( ReportEntry report )
        throws ReporterException
    {
        if ( printWriter != null )
        {
            printWriter.close();
            printWriter = null;
        }
    }

    public void writeMessage( byte[] b, int off, int len )
    {
        try
        {
            if ( printWriter == null )
            {
                if ( !reportsDirectory.exists() )
                {
                    //noinspection ResultOfMethodCallIgnored
                    reportsDirectory.mkdirs();
                }
                File file = AbstractFileReporter.getReportFile( reportsDirectory, reportEntryName, reportNameSuffix,
                                                                "-output.txt" );
                printWriter = new PrintWriter( new BufferedWriter( new FileWriter( file ) ) );
            }
            printWriter.write( new String( b, off, len ) );
        }
        catch ( IOException e )
        {
            throw new NestedRuntimeException( e );
        }
    }


    public void testStarting( ReportEntry report )
    {
    }

    public void testSucceeded( ReportEntry report )
    {
    }

    public void testSkipped( ReportEntry report )
    {
    }

    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
    }

    public void writeMessage( String message )
    {
    }

    public void reset()
    {
    }
}
