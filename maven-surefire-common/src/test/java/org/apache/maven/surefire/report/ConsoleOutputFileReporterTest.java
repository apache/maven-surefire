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
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.maven.plugin.surefire.report.ConsoleOutputFileReporter;

import junit.framework.TestCase;
import org.apache.maven.shared.utils.io.FileUtils;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.fest.assertions.Assertions.assertThat;

public class ConsoleOutputFileReporterTest
    extends TestCase
{
    /*
     * Test method for 'org.codehaus.surefire.report.ConsoleOutputFileReporter.testSetCompleted(ReportEntry report)'
     */
    public void testFileNameWithoutSuffix() throws IOException
    {
        File reportDir = new File( new File( System.getProperty( "user.dir" ), "target" ), "tmp1" );
        //noinspection ResultOfMethodCallIgnored
        reportDir.mkdirs();
        TestSetReportEntry reportEntry =
                new SimpleReportEntry( getClass().getName(), null, getClass().getName(), null );
        ConsoleOutputFileReporter reporter = new ConsoleOutputFileReporter( reportDir, null, false, null, "UTF-8" );
        reporter.testSetStarting( reportEntry );
        reporter.writeTestOutput( "some ", false, true );
        reporter.testSetCompleted( reportEntry );
        reporter.close();

        File expectedReportFile = new File( reportDir, getClass().getName() + "-output.txt" );

        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                    expectedReportFile.exists() );

        assertThat( FileUtils.fileRead( expectedReportFile, US_ASCII.name() ) )
                .contains( "some " );

        //noinspection ResultOfMethodCallIgnored
        expectedReportFile.delete();
    }

    /*
     * Test method for 'org.codehaus.surefire.report.ConsoleOutputFileReporter.testSetCompleted(ReportEntry report)'
     */
    public void testFileNameWithSuffix() throws IOException
    {
        File reportDir = new File( new File( System.getProperty( "user.dir" ), "target" ), "tmp2" );
        String suffixText = "sampleSuffixText";
        TestSetReportEntry reportEntry =
                new SimpleReportEntry( getClass().getName(), null, getClass().getName(), null );
        ConsoleOutputFileReporter reporter =
                new ConsoleOutputFileReporter( reportDir, suffixText, false, null, "UTF-8" );
        reporter.testSetStarting( reportEntry );
        reporter.writeTestOutput( null, true, true );
        reporter.writeTestOutput( "some ", true, true );
        reporter.testSetCompleted( reportEntry );
        reporter.close();

        File expectedReportFile = new File( reportDir, getClass().getName() + "-" + suffixText + "-output.txt" );

        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                    expectedReportFile.exists() );

        assertThat( FileUtils.fileRead( expectedReportFile, US_ASCII.name() ) )
                .contains( "some " );

        assertThat( expectedReportFile )
                .hasSize( 9 + 2 * System.lineSeparator().length() );

        //noinspection ResultOfMethodCallIgnored
        expectedReportFile.delete();
    }

    public void testNullReportFile() throws IOException
    {
        File reportDir = new File( new File( System.getProperty( "user.dir" ), "target" ), "tmp3" );
        ConsoleOutputFileReporter reporter = new ConsoleOutputFileReporter( reportDir, null, false, null, "UTF-8" );
        reporter.writeTestOutput( "some text", false, true );
        reporter.testSetCompleted( new SimpleReportEntry( getClass().getName(), null, getClass().getName(), null ) );
        reporter.close();

        File expectedReportFile = new File( reportDir, "null-output.txt" );

        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                expectedReportFile.exists() );

        assertThat( FileUtils.fileRead( expectedReportFile, US_ASCII.name() ) )
                .contains( "some " );

        //noinspection ResultOfMethodCallIgnored
        expectedReportFile.delete();
    }

    public void testConcurrentAccessReportFile() throws Exception
    {
        File reportDir = new File( new File( System.getProperty( "user.dir" ), "target" ), "tmp4" );
        final ConsoleOutputFileReporter reporter =
                new ConsoleOutputFileReporter( reportDir, null, false, null, "UTF-8" );
        reporter.testSetStarting( new SimpleReportEntry( getClass().getName(), null, getClass().getName(), null ) );
        ExecutorService scheduler = Executors.newFixedThreadPool( 10 );
        final ArrayList<Callable<Void>> jobs = new ArrayList<>();
        for ( int i = 0; i < 10; i++ )
        {
            jobs.add( new Callable<Void>() {
                @Override
                public Void call()
                {
                    reporter.writeTestOutput( "some text\n", false, true );
                    return null;
                }
            } );
        }
        scheduler.invokeAll( jobs );
        scheduler.shutdown();
        reporter.close();

        File expectedReportFile = new File( reportDir, getClass().getName() + "-output.txt" );

        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                expectedReportFile.exists() );

        assertThat( FileUtils.fileRead( expectedReportFile, US_ASCII.name() ) )
                .contains( "some text" );

        StringBuilder expectedText = new StringBuilder();
        for ( int i = 0; i < 10; i++ )
        {
            expectedText.append( "some text\n" );
        }

        assertThat( FileUtils.fileRead( expectedReportFile, US_ASCII.name() ) )
                .isEqualTo( expectedText.toString() );

        //noinspection ResultOfMethodCallIgnored
        expectedReportFile.delete();
    }
}
