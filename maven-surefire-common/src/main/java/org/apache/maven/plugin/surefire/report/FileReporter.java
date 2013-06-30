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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;

import static org.apache.maven.plugin.surefire.report.FileReporterUtils.stripIllegalFilenameChars;

/**
 * Base class for file reporters.
 * 
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author Kristian Rosenvold
 */
public class FileReporter
{
    private final File reportsDirectory;

    private final boolean deleteOnStarting;

    private final String reportNameSuffix;

    public FileReporter( File reportsDirectory, String reportNameSuffix )
    {
        this.reportsDirectory = reportsDirectory;
        this.deleteOnStarting = false;
        this.reportNameSuffix = reportNameSuffix;
    }

    private PrintWriter testSetStarting( ReportEntry report )
        throws ReporterException
    {
        File reportFile = getReportFile( reportsDirectory, report.getName(), reportNameSuffix, ".txt" );

        File reportDir = reportFile.getParentFile();

        // noinspection ResultOfMethodCallIgnored
        reportDir.mkdirs();

        if ( deleteOnStarting && reportFile.exists() )
        {
            // noinspection ResultOfMethodCallIgnored
            reportFile.delete();
        }

        try
        {
            PrintWriter writer = new PrintWriter( new FileWriter( reportFile ) );

            writer.println( "-------------------------------------------------------------------------------" );

            writer.println( "Test set: " + report.getName() );

            writer.println( "-------------------------------------------------------------------------------" );

            return writer;
        }
        catch ( IOException e )
        {
            throw new ReporterException( "Unable to create file for report: " + e.getMessage(), e );
        }
    }

    public static File getReportFile( File reportsDirectory, String reportEntryName, String reportNameSuffix,
                                      String fileExtension )
    {
        File reportFile;

        if ( reportNameSuffix != null && reportNameSuffix.length() > 0 )
        {
            reportFile =
                new File( reportsDirectory, stripIllegalFilenameChars( reportEntryName + "-" + reportNameSuffix
                    + fileExtension ) );
        }
        else
        {
            reportFile = new File( reportsDirectory, stripIllegalFilenameChars( reportEntryName + fileExtension ) );
        }
        return reportFile;
    }

    public void testSetCompleted( WrappedReportEntry report, TestSetStats testSetStats, List<String> testResults )
        throws ReporterException
    {
        PrintWriter writer = testSetStarting( report );
        writer.print( testSetStats.getTestSetSummary( report ) );

        if ( testResults != null )
        {
            for ( String testResult : testResults )
            {
                writer.println( testResult );
            }
        }

        writer.flush();

        writer.close();
    }
}
