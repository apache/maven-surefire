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
import java.io.IOException;

import org.apache.maven.plugin.surefire.report.ConsoleOutputFileReporter;

import junit.framework.TestCase;
import org.apache.maven.shared.utils.io.FileUtils;

import static org.apache.maven.surefire.util.internal.StringUtils.US_ASCII;
import static org.fest.assertions.Assertions.assertThat;

public class ConsoleOutputFileReporterTest
    extends TestCase
{

    private ConsoleOutputFileReporter reporter;

    private ReportEntry reportEntry;

    private static final String testName = ConsoleOutputFileReporterTest.class.getName();

    /*
     * Test method for 'org.codehaus.surefire.report.ConsoleOutputFileReporter.testSetCompleted(ReportEntry report)'
     */
    public void testFileNameWithoutSuffix() throws IOException
    {
        File reportDir = new File( new File( System.getProperty( "user.dir" ), "target" ), "tmp" );
        //noinspection ResultOfMethodCallIgnored
        reportDir.mkdirs();
        reportEntry = new SimpleReportEntry( getClass().getName(), testName );
        reporter = new ConsoleOutputFileReporter( reportDir, null );
        reporter.testSetStarting( reportEntry );
        reporter.writeTestOutput( "some text".getBytes( US_ASCII ), 0, 5, true );
        reporter.testSetCompleted( reportEntry );
        reporter.close();

        File expectedReportFile = new File( reportDir, testName + "-output.txt" );
        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                    expectedReportFile.exists() );
        assertThat( FileUtils.fileRead( expectedReportFile, US_ASCII.name() ) ).contains( "some " );
        //noinspection ResultOfMethodCallIgnored
        expectedReportFile.delete();
    }

    /*
     * Test method for 'org.codehaus.surefire.report.ConsoleOutputFileReporter.testSetCompleted(ReportEntry report)'
     */
    public void testFileNameWithSuffix() throws IOException
    {
        File reportDir = new File( new File( System.getProperty( "user.dir" ), "target" ), "tmp" );
        //noinspection ResultOfMethodCallIgnored
        reportDir.mkdirs();
        String suffixText = "sampleSuffixText";
        reportEntry = new SimpleReportEntry( getClass().getName(), testName );
        reporter = new ConsoleOutputFileReporter( reportDir, suffixText );
        reporter.testSetStarting( reportEntry );
        reporter.writeTestOutput( "some text".getBytes( US_ASCII ), 0, 5, true );
        reporter.testSetCompleted( reportEntry );
        reporter.close();

        File expectedReportFile = new File( reportDir, testName + "-" + suffixText + "-output.txt" );
        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                    expectedReportFile.exists() );
        assertThat( FileUtils.fileRead( expectedReportFile, US_ASCII.name() ) ).contains( "some " );
        //noinspection ResultOfMethodCallIgnored
        expectedReportFile.delete();
    }

}
