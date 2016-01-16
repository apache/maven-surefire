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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.plugin.surefire.StartupReportConfiguration;
import org.apache.maven.surefire.report.DefaultDirectConsoleReporter;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.StackTraceWriter;

import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.error;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.failure;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.flake;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.skipped;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.success;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.unknown;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultReporterFactoryTest
    extends TestCase
{

    private final static String TEST_ONE = "testOne";

    private final static String TEST_TWO = "testTwo";

    private final static String TEST_THREE = "testThree";

    private final static String TEST_FOUR = "testFour";

    private final static String TEST_FIVE = "testFive";

    private final static String ASSERTION_FAIL = "assertionFail";

    private final static String ERROR = "error";

    public void testMergeTestHistoryResult()
    {
        StartupReportConfiguration reportConfig = new StartupReportConfiguration( true, true, "PLAIN", false, false, new File("target"), false, null, "TESTHASH",
                                                                                                 false, 1, null );

        DefaultReporterFactory factory = new DefaultReporterFactory( reportConfig );

        // First run, four tests failed and one passed
        List<TestMethodStats> firstRunStats = new ArrayList<TestMethodStats>();
        firstRunStats.add( new TestMethodStats( TEST_ONE, ReportEntryType.ERROR, new DummyStackTraceWriter( ERROR ) ) );
        firstRunStats.add( new TestMethodStats( TEST_TWO, ReportEntryType.ERROR, new DummyStackTraceWriter( ERROR ) ) );
        firstRunStats.add(
            new TestMethodStats( TEST_THREE, ReportEntryType.FAILURE, new DummyStackTraceWriter( ASSERTION_FAIL ) ) );
        firstRunStats.add(
            new TestMethodStats( TEST_FOUR, ReportEntryType.FAILURE, new DummyStackTraceWriter( ASSERTION_FAIL ) ) );
        firstRunStats.add(
            new TestMethodStats( TEST_FIVE, ReportEntryType.SUCCESS, null ) );

        // Second run, two tests passed
        List<TestMethodStats> secondRunStats = new ArrayList<TestMethodStats>();
        secondRunStats.add(
            new TestMethodStats( TEST_ONE, ReportEntryType.FAILURE, new DummyStackTraceWriter( ASSERTION_FAIL ) ) );
        secondRunStats.add( new TestMethodStats( TEST_TWO, ReportEntryType.SUCCESS, null ) );
        secondRunStats.add(
            new TestMethodStats( TEST_THREE, ReportEntryType.ERROR, new DummyStackTraceWriter( ERROR ) ) );
        secondRunStats.add( new TestMethodStats( TEST_FOUR, ReportEntryType.SUCCESS, null ) );

        // Third run, another test passed
        List<TestMethodStats> thirdRunStats = new ArrayList<TestMethodStats>();
        thirdRunStats.add( new TestMethodStats( TEST_ONE, ReportEntryType.SUCCESS, null ) );
        thirdRunStats.add(
            new TestMethodStats( TEST_THREE, ReportEntryType.ERROR, new DummyStackTraceWriter( ERROR ) ) );

        TestSetRunListener firstRunListener = mock( TestSetRunListener.class );
        TestSetRunListener secondRunListener = mock( TestSetRunListener.class );
        TestSetRunListener thirdRunListener = mock( TestSetRunListener.class );
        when( firstRunListener.getTestMethodStats() ).thenReturn( firstRunStats );
        when( secondRunListener.getTestMethodStats() ).thenReturn( secondRunStats );
        when( thirdRunListener.getTestMethodStats() ).thenReturn( thirdRunStats );

        factory.addListener( firstRunListener );
        factory.addListener( secondRunListener );
        factory.addListener( thirdRunListener );

        factory.mergeTestHistoryResult();
        RunStatistics mergedStatistics = factory.getGlobalRunStatistics();

        // Only TEST_THREE is a failing test, other three are flaky tests
        assertEquals( 5, mergedStatistics.getCompletedCount() );
        assertEquals( 1, mergedStatistics.getErrors() );
        assertEquals( 0, mergedStatistics.getFailures() );
        assertEquals( 3, mergedStatistics.getFlakes() );
        assertEquals( 0, mergedStatistics.getSkipped() );

        // Now test the result will be printed out correctly
        DummyTestReporter reporter = new DummyTestReporter();
        factory.printTestFailures( reporter, DefaultReporterFactory.TestResultType.flake );
        String[] expectedFlakeOutput =
            { "Flaked tests: ", TEST_FOUR, "  Run 1: " + ASSERTION_FAIL, "  Run 2: PASS", "", TEST_ONE,
                "  Run 1: " + ERROR, "  Run 2: " + ASSERTION_FAIL, "  Run 3: PASS", "", TEST_TWO, "  Run 1: " + ERROR,
                "  Run 2: PASS", "" };
        assertEquals( Arrays.asList( expectedFlakeOutput ), reporter.getMessages() );

        reporter = new DummyTestReporter();
        factory.printTestFailures( reporter, DefaultReporterFactory.TestResultType.error );
        String[] expectedFailureOutput =
            { "Tests in error: ", TEST_THREE, "  Run 1: " + ASSERTION_FAIL, "  Run 2: " + ERROR, "  Run 3: " + ERROR, ""
            };
        assertEquals( Arrays.asList( expectedFailureOutput ), reporter.getMessages() );

        reporter = new DummyTestReporter();
        factory.printTestFailures( reporter, DefaultReporterFactory.TestResultType.failure );
        String[] expectedErrorOutput = { };
        assertEquals( Arrays.asList( expectedErrorOutput ), reporter.getMessages() );
    }

    static class DummyTestReporter
        extends DefaultDirectConsoleReporter
    {

        private final List<String> messages = new ArrayList<String>();

        public DummyTestReporter()
        {
            super( System.out );
        }

        @Override
        public void info( String msg )
        {
            messages.add( msg );
        }

        public List<String> getMessages()
        {
            return messages;
        }
    }

    public void testGetTestResultType()
    {
        List<ReportEntryType> emptyList = new ArrayList<ReportEntryType>();
        assertEquals( unknown, DefaultReporterFactory.getTestResultType( emptyList, 1 ) );

        List<ReportEntryType> successList = new ArrayList<ReportEntryType>();
        successList.add( ReportEntryType.SUCCESS );
        successList.add( ReportEntryType.SUCCESS );
        assertEquals( success, DefaultReporterFactory.getTestResultType( successList, 1 ) );

        List<ReportEntryType> failureErrorList = new ArrayList<ReportEntryType>();
        failureErrorList.add( ReportEntryType.FAILURE );
        failureErrorList.add( ReportEntryType.ERROR );
        assertEquals( error, DefaultReporterFactory.getTestResultType( failureErrorList, 1 ) );

        List<ReportEntryType> errorFailureList = new ArrayList<ReportEntryType>();
        errorFailureList.add( ReportEntryType.ERROR );
        errorFailureList.add( ReportEntryType.FAILURE );
        assertEquals( error, DefaultReporterFactory.getTestResultType( errorFailureList, 1 ) );

        List<ReportEntryType> flakeList = new ArrayList<ReportEntryType>();
        flakeList.add( ReportEntryType.SUCCESS );
        flakeList.add( ReportEntryType.FAILURE );
        assertEquals( flake, DefaultReporterFactory.getTestResultType( flakeList, 1 ) );

        assertEquals( failure, DefaultReporterFactory.getTestResultType( flakeList, 0 ) );

        flakeList = new ArrayList<ReportEntryType>();
        flakeList.add( ReportEntryType.ERROR );
        flakeList.add( ReportEntryType.SUCCESS );
        flakeList.add( ReportEntryType.FAILURE );
        assertEquals( flake, DefaultReporterFactory.getTestResultType( flakeList, 1 ) );

        assertEquals( error, DefaultReporterFactory.getTestResultType( flakeList, 0 ) );

        List<ReportEntryType> skippedList = new ArrayList<ReportEntryType>();
        skippedList.add( ReportEntryType.SKIPPED );
        assertEquals( skipped, DefaultReporterFactory.getTestResultType( skippedList, 1 ) );
    }

    static class DummyStackTraceWriter
        implements StackTraceWriter
    {

        private final String stackTrace;

        public DummyStackTraceWriter( String stackTrace )
        {
            this.stackTrace = stackTrace;
        }

        public String writeTraceToString()
        {
            return "";
        }

        public String writeTrimmedTraceToString()
        {
            return "";
        }

        public String smartTrimmedStackTrace()
        {
            return stackTrace;
        }

        public SafeThrowable getThrowable()
        {
            return null;
        }
    }
}