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

import org.apache.maven.surefire.extensions.StatelessTestsetInfoFileReportEventListener;
import org.apache.maven.surefire.report.ReporterException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;

import static org.apache.maven.plugin.surefire.report.FileReporterUtils.stripIllegalFilenameChars;
import static org.apache.maven.surefire.util.internal.StringUtils.isNotBlank;

/**
 * Base class for file reporters.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author Kristian Rosenvold
 */
public class FileReporter
        extends StatelessTestsetInfoFileReportEventListener<WrappedReportEntry, TestSetStats>
{
    private final boolean usePhrasedFileName;
    private final boolean usePhrasedClassNameInRunning;
    private final boolean usePhrasedClassNameInTestCaseSummary;

    public FileReporter( File reportsDirectory, String reportNameSuffix, Charset encoding, boolean usePhrasedFileName,
                         boolean usePhrasedClassNameInRunning, boolean usePhrasedClassNameInTestCaseSummary )
    {
        super( reportsDirectory, reportNameSuffix, encoding );
        this.usePhrasedFileName = usePhrasedFileName;
        this.usePhrasedClassNameInRunning = usePhrasedClassNameInRunning;
        this.usePhrasedClassNameInTestCaseSummary = usePhrasedClassNameInTestCaseSummary;
    }

    static File getReportFile( File reportsDirectory, String reportEntryName, String reportNameSuffix,
                               String fileExtension )
    {
        String fileName =
                reportEntryName + ( isNotBlank( reportNameSuffix ) ? "-" + reportNameSuffix : "" ) + fileExtension;
        return new File( reportsDirectory, stripIllegalFilenameChars( fileName ) );
    }

    @Override
    public void testSetCompleted( WrappedReportEntry report, TestSetStats testSetStats, List<String> testResults )
    {
        File reportFile = getReportFile( getReportsDirectory(),
                                         usePhrasedFileName ? report.getReportSourceName() : report.getSourceName(),
                                         getReportNameSuffix(),
                                         ".txt" );

        File reportDir = reportFile.getParentFile();

        // noinspection ResultOfMethodCallIgnored
        reportDir.mkdirs();

        try ( BufferedWriter writer = createFileReporterWriter( reportFile, getEncoding() ) )
        {
            writer.write( "-------------------------------------------------------------------------------" );
            writer.newLine();

            String tesSet = usePhrasedClassNameInRunning ? report.getReportSourceName() : report.getSourceName();
            writer.write( "Test set: " + tesSet );
            writer.newLine();

            writer.write( "-------------------------------------------------------------------------------" );
            writer.newLine();

            writer.write( testSetStats.getTestSetSummary( report, usePhrasedClassNameInTestCaseSummary ) );
            writer.newLine();
            for ( String testResult : testResults )
            {
                writer.write( testResult );
                writer.newLine();
            }
        }
        catch ( IOException e )
        {
            throw new ReporterException( "Unable to create file for report: " + e.getLocalizedMessage(), e );
        }
    }

    private static BufferedWriter createFileReporterWriter( File reportFile, Charset encoding )
            throws FileNotFoundException
    {
        return new BufferedWriter( new OutputStreamWriter( new FileOutputStream( reportFile ), encoding ), 64 * 1024 );
    }
}
