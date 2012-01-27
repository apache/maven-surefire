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

import java.io.File;
import org.apache.maven.plugin.surefire.report.FileReporter;

import junit.framework.TestCase;

public class FileReporterTest
    extends TestCase
{

    private FileReporter reporter;

    private ReportEntry reportEntry;

    private static final String testName = "org.apache.maven.surefire.report.FileReporterTest";

    /*
     * Test method for 'org.codehaus.surefire.report.FileReporter.testSetStarting(ReportEntry report)'
     */
    public void testFileNameWithoutSuffix()
    {
        File reportDir = new File( "." );
        reportEntry = new SimpleReportEntry( this.getClass().getName(), testName );
        reporter = new FileReporter( true, reportDir, null );
        reporter.testSetStarting( reportEntry );

        File expectedReportFile = new File( reportDir, testName + ".txt" );
        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                    expectedReportFile.exists() );
        expectedReportFile.delete();
    }

    /*
     * Test method for 'org.codehaus.surefire.report.FileReporter.testSetStarting(ReportEntry report)'
     */
    public void testFileNameWithSuffix()
    {
        File reportDir = new File( "." );
        String suffixText = "sampleSuffixText";
        reportEntry = new SimpleReportEntry( this.getClass().getName(), testName );
        reporter = new FileReporter( true, reportDir, suffixText );
        reporter.testSetStarting( reportEntry );

        File expectedReportFile = new File( reportDir, testName + "-" + suffixText + ".txt" );
        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                    expectedReportFile.exists() );
        expectedReportFile.delete();
    }

}
