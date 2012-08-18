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
import org.apache.maven.surefire.util.NestedRuntimeException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Surefire output consumer proxy that writes test output to a {@link java.io.File} for each test suite.
 * <p/>
 * This class is not threadsafe, but can be serially handed off from thread to thread.
 *
 * @author Kristian Rosenvold
 * @author Carlos Sanchez
 */
public class ConsoleOutputFileReporter
    implements TestcycleConsoleOutputReceiver
{
    private final File reportsDirectory;

    private final String reportNameSuffix;

    private PrintWriter printWriter = null;

    private String reportEntryName;

    public ConsoleOutputFileReporter( File reportsDirectory, String reportNameSuffix )
    {
        this.reportsDirectory = reportsDirectory;
        this.reportNameSuffix = reportNameSuffix;
    }

    public void testSetStarting( ReportEntry reportEntry )
    {
        close();
        this.reportEntryName = reportEntry.getName();
    }

    public void testSetCompleted( ReportEntry report )
        throws ReporterException
    {
    }

    public void close(){
        if ( printWriter != null )
        {
            printWriter.close();
            printWriter = null;
        }
    }
    public void writeTestOutput( byte[] buf, int off, int len, boolean stdout )
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
                File file =
                    FileReporter.getReportFile( reportsDirectory, reportEntryName, reportNameSuffix, "-output.txt" );
                printWriter = new PrintWriter( new BufferedWriter( new FileWriter( file ) ) );
            }
            printWriter.write( new String( buf, off, len ) );
        }
        catch ( IOException e )
        {
            throw new NestedRuntimeException( e );
        }
    }
}
