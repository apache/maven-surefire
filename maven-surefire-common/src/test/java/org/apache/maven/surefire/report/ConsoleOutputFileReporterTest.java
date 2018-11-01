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
        ReportEntry reportEntry = new SimpleReportEntry( getClass().getName(), getClass().getName() );
        ConsoleOutputFileReporter reporter = new ConsoleOutputFileReporter( reportDir, null, null );
        reporter.testSetStarting( reportEntry );
        reporter.writeTestOutput( "some text".getBytes( US_ASCII ), 0, 5, true );
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
        //noinspection ResultOfMethodCallIgnored
        reportDir.mkdirs();
        String suffixText = "sampleSuffixText";
        ReportEntry reportEntry = new SimpleReportEntry( getClass().getName(), getClass().getName() );
        ConsoleOutputFileReporter reporter = new ConsoleOutputFileReporter( reportDir, suffixText, null );
        reporter.testSetStarting( reportEntry );
        reporter.writeTestOutput( "some text".getBytes( US_ASCII ), 0, 5, true );
        reporter.testSetCompleted( reportEntry );
        reporter.close();

        File expectedReportFile = new File( reportDir, getClass().getName() + "-" + suffixText + "-output.txt" );

        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                    expectedReportFile.exists() );

        assertThat( FileUtils.fileRead( expectedReportFile, US_ASCII.name() ) )
                .contains( "some " );

        //noinspection ResultOfMethodCallIgnored
        expectedReportFile.delete();
    }

    public void testNullReportFile() throws IOException
    {
        File reportDir = new File( new File( System.getProperty( "user.dir" ), "target" ), "tmp3" );
        //noinspection ResultOfMethodCallIgnored
        reportDir.mkdirs();
        ConsoleOutputFileReporter reporter = new ConsoleOutputFileReporter( reportDir, null, null );
        reporter.writeTestOutput( "some text".getBytes( US_ASCII ), 0, 5, true );
        reporter.testSetCompleted( new SimpleReportEntry( getClass().getName(), getClass().getName() ) );
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
        //noinspection ResultOfMethodCallIgnored
        reportDir.mkdirs();
        final ConsoleOutputFileReporter reporter = new ConsoleOutputFileReporter( reportDir, null, null );
        reporter.testSetStarting( new SimpleReportEntry( getClass().getName(), getClass().getName() ) );
        ExecutorService scheduler = Executors.newFixedThreadPool( 10 );
        final ArrayList<Callable<Void>> jobs = new ArrayList<>();
        for ( int i = 0; i < 10; i++ )
        {
            jobs.add( new Callable<Void>() {
                @Override
                public Void call()
                {
                    byte[] stream = "some text\n".getBytes( US_ASCII );
                    reporter.writeTestOutput( stream, 0, stream.length, true );
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
