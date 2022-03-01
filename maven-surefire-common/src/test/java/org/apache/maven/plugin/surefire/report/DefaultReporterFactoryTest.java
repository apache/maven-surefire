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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

import junit.framework.TestCase;

import org.apache.maven.plugin.surefire.StartupReportConfiguration;
import org.apache.maven.plugin.surefire.extensions.SurefireConsoleOutputReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessTestsetInfoReporter;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.shared.utils.logging.MessageUtils;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.api.report.SafeThrowable;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.suite.RunResult;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.error;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.failure;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.flake;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.skipped;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.success;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.unknown;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.getTestResultType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.getInternalState;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 *
 */
public class DefaultReporterFactoryTest
    extends TestCase
{
    private static final String TEST_ONE = "testOne";

    private static final String TEST_TWO = "testTwo";

    private static final String TEST_THREE = "testThree";

    private static final String TEST_FOUR = "testFour";

    private static final String TEST_FIVE = "testFive";

    private static final String ASSERTION_FAIL = "assertionFail";

    private static final String ERROR = "error";

    public void testMergeTestHistoryResult()
            throws Exception
    {
        MessageUtils.setColorEnabled( false );
        File target = new File( System.getProperty( "user.dir" ), "target" );
        File reportsDirectory = new File( target, "tmp5" );
        StartupReportConfiguration reportConfig =
                new StartupReportConfiguration( true, true, "PLAIN", false, reportsDirectory, false, null,
                        new File( reportsDirectory, "TESTHASH" ), false, 1, null, null, false,
                        new SurefireStatelessReporter(), new SurefireConsoleOutputReporter(),
                        new SurefireStatelessTestsetInfoReporter() );

        DummyTestReporter reporter = new DummyTestReporter();

        DefaultReporterFactory factory = new DefaultReporterFactory( reportConfig, reporter );

        // First run, four tests failed and one passed
        Queue<TestMethodStats> firstRunStats = new ArrayDeque<>();
        firstRunStats.add( new TestMethodStats( TEST_ONE, ReportEntryType.ERROR, new DummyStackTraceWriter( ERROR ) ) );
        firstRunStats.add( new TestMethodStats( TEST_TWO, ReportEntryType.ERROR, new DummyStackTraceWriter( ERROR ) ) );
        firstRunStats.add(
            new TestMethodStats( TEST_THREE, ReportEntryType.FAILURE, new DummyStackTraceWriter( ASSERTION_FAIL ) ) );
        firstRunStats.add(
            new TestMethodStats( TEST_FOUR, ReportEntryType.FAILURE, new DummyStackTraceWriter( ASSERTION_FAIL ) ) );
        firstRunStats.add(
            new TestMethodStats( TEST_FIVE, ReportEntryType.SUCCESS, null ) );

        // Second run, two tests passed
        Queue<TestMethodStats> secondRunStats = new ArrayDeque<>();
        secondRunStats.add(
            new TestMethodStats( TEST_ONE, ReportEntryType.FAILURE, new DummyStackTraceWriter( ASSERTION_FAIL ) ) );
        secondRunStats.add( new TestMethodStats( TEST_TWO, ReportEntryType.SUCCESS, null ) );
        secondRunStats.add(
            new TestMethodStats( TEST_THREE, ReportEntryType.ERROR, new DummyStackTraceWriter( ERROR ) ) );
        secondRunStats.add( new TestMethodStats( TEST_FOUR, ReportEntryType.SUCCESS, null ) );

        // Third run, another test passed
        Queue<TestMethodStats> thirdRunStats = new ArrayDeque<>();
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

        invokeMethod( factory, "mergeTestHistoryResult" );
        RunStatistics mergedStatistics = factory.getGlobalRunStatistics();

        // Only TEST_THREE is a failing test, other three are flaky tests
        assertEquals( 5, mergedStatistics.getCompletedCount() );
        assertEquals( 1, mergedStatistics.getErrors() );
        assertEquals( 0, mergedStatistics.getFailures() );
        assertEquals( 3, mergedStatistics.getFlakes() );
        assertEquals( 0, mergedStatistics.getSkipped() );

        // Now test the result will be printed out correctly
        factory.printTestFailures( flake );
        String[] expectedFlakeOutput =
            { "Flakes: ", TEST_FOUR, "  Run 1: " + ASSERTION_FAIL, "  Run 2: PASS", "", TEST_ONE,
                "  Run 1: " + ERROR, "  Run 2: " + ASSERTION_FAIL, "  Run 3: PASS", "", TEST_TWO, "  Run 1: " + ERROR,
                "  Run 2: PASS", "" };
        assertEquals( asList( expectedFlakeOutput ), reporter.getMessages() );

        reporter.reset();
        factory.printTestFailures( error );
        String[] expectedFailureOutput =
            { "Errors: ", TEST_THREE, "  Run 1: " + ASSERTION_FAIL, "  Run 2: " + ERROR, "  Run 3: " + ERROR, "" };
        assertEquals( asList( expectedFailureOutput ), reporter.getMessages() );

        reporter.reset();
        factory.printTestFailures( failure );
        assertEquals( emptyList(), reporter.getMessages() );
    }

    static final class DummyTestReporter implements ConsoleLogger
    {
        private final List<String> messages = new ArrayList<>();

        @Override
        public boolean isDebugEnabled()
        {
            return true;
        }

        @Override
        public void debug( String message )
        {
            messages.add( message );
        }

        @Override
        public boolean isInfoEnabled()
        {
            return true;
        }

        @Override
        public void info( String message )
        {
            messages.add( message );
        }

        @Override
        public boolean isWarnEnabled()
        {
            return true;
        }

        @Override
        public void warning( String message )
        {
            messages.add( message );
        }

        @Override
        public boolean isErrorEnabled()
        {
            return true;
        }

        @Override
        public void error( String message )
        {
            messages.add( message );
        }

        @Override
        public void error( String message, Throwable t )
        {
            messages.add( message + " " + t.getLocalizedMessage() );
        }

        @Override
        public void error( Throwable t )
        {
            messages.add( t.getLocalizedMessage() );
        }

        List<String> getMessages()
        {
            return messages;
        }

        void reset()
        {
            messages.clear();
        }
    }

    public void testGetTestResultType()
    {
        List<ReportEntryType> emptyList = new ArrayList<>();
        assertEquals( unknown, getTestResultType( emptyList, 1 ) );

        List<ReportEntryType> successList = new ArrayList<>();
        successList.add( ReportEntryType.SUCCESS );
        successList.add( ReportEntryType.SUCCESS );
        assertEquals( success, getTestResultType( successList, 1 ) );

        List<ReportEntryType> failureErrorList = new ArrayList<>();
        failureErrorList.add( ReportEntryType.FAILURE );
        failureErrorList.add( ReportEntryType.ERROR );
        assertEquals( error, getTestResultType( failureErrorList, 1 ) );

        List<ReportEntryType> errorFailureList = new ArrayList<>();
        errorFailureList.add( ReportEntryType.ERROR );
        errorFailureList.add( ReportEntryType.FAILURE );
        assertEquals( error, getTestResultType( errorFailureList, 1 ) );

        List<ReportEntryType> flakeList = new ArrayList<>();
        flakeList.add( ReportEntryType.SUCCESS );
        flakeList.add( ReportEntryType.FAILURE );
        assertEquals( flake, getTestResultType( flakeList, 1 ) );

        assertEquals( failure, getTestResultType( flakeList, 0 ) );

        flakeList = new ArrayList<>();
        flakeList.add( ReportEntryType.ERROR );
        flakeList.add( ReportEntryType.SUCCESS );
        flakeList.add( ReportEntryType.FAILURE );
        assertEquals( flake, getTestResultType( flakeList, 1 ) );

        assertEquals( error, getTestResultType( flakeList, 0 ) );

        List<ReportEntryType> skippedList = new ArrayList<>();
        skippedList.add( ReportEntryType.SKIPPED );
        assertEquals( skipped, getTestResultType( skippedList, 1 ) );
    }

    public void testLogger()
    {
        MessageUtils.setColorEnabled( false );
        File target = new File( System.getProperty( "user.dir" ), "target" );
        File reportsDirectory = new File( target, "tmp6" );
        StartupReportConfiguration reportConfig =
                new StartupReportConfiguration( true, true, "PLAIN", false, reportsDirectory, false, null,
                        new File( reportsDirectory, "TESTHASH" ), false, 1, null, null, false,
                        new SurefireStatelessReporter(), new SurefireConsoleOutputReporter(),
                        new SurefireStatelessTestsetInfoReporter() );

        DummyTestReporter reporter = new DummyTestReporter();

        DefaultReporterFactory factory = new DefaultReporterFactory( reportConfig, reporter );

        TestReportListener<TestOutputReportEntry> runListener = factory.createTestReportListener();

        assertTrue( runListener.isDebugEnabled() );
        assertTrue( runListener.isInfoEnabled() );
        assertTrue( runListener.isWarnEnabled() );
        assertTrue( runListener.isErrorEnabled() );

        runListener.debug( "msg" );
        assertEquals( 1, reporter.getMessages().size() );
        assertEquals( "msg", reporter.getMessages().get( 0 ) );
        reporter.reset();

        runListener.info( "msg\n" );
        assertEquals( 1, reporter.getMessages().size() );
        assertEquals( "msg", reporter.getMessages().get( 0 ) );
        reporter.reset();

        runListener.warning( "msg\r\n" );
        assertEquals( 1, reporter.getMessages().size() );
        assertEquals( "msg", reporter.getMessages().get( 0 ) );
        reporter.reset();

        runListener.error( "msg" );
        assertEquals( 1, reporter.getMessages().size() );
        assertEquals( "msg", reporter.getMessages().get( 0 ) );
        reporter.reset();

        runListener.error( "msg\n", new Exception( "e" ) );
        assertEquals( 1, reporter.getMessages().size() );
        assertEquals( "msg e", reporter.getMessages().get( 0 ) );
        reporter.reset();

        runListener.error( new Exception( "e" ) );
        assertEquals( 1, reporter.getMessages().size() );
        assertEquals( "e", reporter.getMessages().get( 0 ) );
        reporter.reset();
    }

    public void testCreateReporterWithZeroStatistics()
    {
        MessageUtils.setColorEnabled( false );
        File target = new File( System.getProperty( "user.dir" ), "target" );
        File reportsDirectory = new File( target, "tmp7" );
        StartupReportConfiguration reportConfig =
                new StartupReportConfiguration( true, true, "PLAIN", false, reportsDirectory, false, null,
                        new File( reportsDirectory, "TESTHASH" ), false, 0, null, null, false,
                        new SurefireStatelessReporter(), new SurefireConsoleOutputReporter(),
                        new SurefireStatelessTestsetInfoReporter() );

        assertTrue( reportConfig.isUseFile() );
        assertTrue( reportConfig.isPrintSummary() );
        assertEquals( "PLAIN", reportConfig.getReportFormat() );
        assertFalse( reportConfig.isRedirectTestOutputToFile() );
        assertEquals( reportsDirectory, reportConfig.getReportsDirectory() );
        assertFalse( reportConfig.isTrimStackTrace() );
        assertNull( reportConfig.getReportNameSuffix() );
        assertEquals( new File( reportsDirectory, "TESTHASH" ), reportConfig.getStatisticsFile() );
        assertFalse( reportConfig.isRequiresRunHistory() );
        assertEquals( 0, reportConfig.getRerunFailingTestsCount() );
        assertNull( reportConfig.getXsdSchemaLocation() );
        assertEquals( UTF_8, reportConfig.getEncoding() );
        assertFalse( reportConfig.isForkMode() );
        assertNotNull( reportConfig.getXmlReporter() );
        assertNotNull( reportConfig.getConsoleOutputReporter() );
        assertNotNull( reportConfig.getTestsetReporter() );
        assertNull( reportConfig.getStatisticsReporter() );

        DummyTestReporter reporter = new DummyTestReporter();

        DefaultReporterFactory factory = new DefaultReporterFactory( reportConfig, reporter );
        assertEquals( reportsDirectory, factory.getReportsDirectory() );

        TestSetRunListener runListener = (TestSetRunListener) factory.createTestReportListener();
        Collection listeners = getInternalState( factory, "listeners" );
        assertEquals( 1, listeners.size() );
        assertTrue( listeners.contains( runListener ) );

        assertNotNull( runListener.getTestMethodStats() );

        factory.runStarting();

        factory.close();

        RunStatistics statistics = factory.getGlobalRunStatistics();
        assertEquals( 0, statistics.getCompletedCount() );
        assertEquals( new RunResult( 0, 0, 0, 0 ), statistics.getRunResult() );
        assertEquals( 0, statistics.getFailures() );
        assertEquals( 0, statistics.getErrors() );
        assertEquals( 0, statistics.getSkipped() );
        assertEquals( 0, statistics.getFlakes() );
        assertEquals( "Tests run: 0, Failures: 0, Errors: 0, Skipped: 0", statistics.getSummary() );
        assertEquals( 0, statistics.getCompletedCount() );

        List<String> messages = reporter.getMessages();
        assertEquals( "", messages.get( 0 ) );
        assertEquals( "-------------------------------------------------------", messages.get( 1 ) );
        assertEquals( " T E S T S", messages.get( 2 ) );
        assertEquals( "-------------------------------------------------------", messages.get( 3 ) );
        assertEquals( "", messages.get( 4 ) );
        assertEquals( "Results:", messages.get( 5 ) );
        assertEquals( "", messages.get( 6 ) );
        assertEquals( "Tests run: 0, Failures: 0, Errors: 0, Skipped: 0", messages.get( 7 ) );
        assertEquals( "", messages.get( 8 ) );
        assertEquals( 9, messages.size() );
    }

    static class DummyStackTraceWriter
        implements StackTraceWriter
    {

        private final String stackTrace;

        DummyStackTraceWriter( String stackTrace )
        {
            this.stackTrace = stackTrace;
        }

        @Override
        public String writeTraceToString()
        {
            return "";
        }

        @Override
        public String writeTrimmedTraceToString()
        {
            return "";
        }

        @Override
        public String smartTrimmedStackTrace()
        {
            return stackTrace;
        }

        @Override
        public SafeThrowable getThrowable()
        {
            return null;
        }
    }
}
