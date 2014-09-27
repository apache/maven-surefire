package org.apache.maven.plugin.surefire.report;

import junit.framework.TestCase;
import org.apache.maven.plugin.surefire.StartupReportConfiguration;
import org.apache.maven.surefire.report.DefaultDirectConsoleReporter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.report.SafeThrowable;
import org.apache.maven.surefire.report.StackTraceWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.*;
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
        DefaultReporterFactory factory = new DefaultReporterFactory( StartupReportConfiguration.defaultValue() );

        // First run, four tests failed and one passed
        List<TestMethodStats> firstRunStats = new ArrayList<TestMethodStats>();
        firstRunStats.add( new TestMethodStats( TEST_ONE, ReportEntryType.error, new DummyStackTraceWriter( ERROR ) ) );
        firstRunStats.add( new TestMethodStats( TEST_TWO, ReportEntryType.error, new DummyStackTraceWriter( ERROR ) ) );
        firstRunStats.add(
            new TestMethodStats( TEST_THREE, ReportEntryType.failure, new DummyStackTraceWriter( ASSERTION_FAIL ) ) );
        firstRunStats.add(
            new TestMethodStats( TEST_FOUR, ReportEntryType.failure, new DummyStackTraceWriter( ASSERTION_FAIL ) ) );
        firstRunStats.add(
            new TestMethodStats( TEST_FIVE, ReportEntryType.success, null ) );

        // Second run, two tests passed
        List<TestMethodStats> secondRunStats = new ArrayList<TestMethodStats>();
        secondRunStats.add(
            new TestMethodStats( TEST_ONE, ReportEntryType.failure, new DummyStackTraceWriter( ASSERTION_FAIL ) ) );
        secondRunStats.add( new TestMethodStats( TEST_TWO, ReportEntryType.success, null ) );
        secondRunStats.add(
            new TestMethodStats( TEST_THREE, ReportEntryType.error, new DummyStackTraceWriter( ERROR ) ) );
        secondRunStats.add( new TestMethodStats( TEST_FOUR, ReportEntryType.success, null ) );

        // Third run, another test passed
        List<TestMethodStats> thirdRunStats = new ArrayList<TestMethodStats>();
        thirdRunStats.add( new TestMethodStats( TEST_ONE, ReportEntryType.success, null ) );
        thirdRunStats.add(
            new TestMethodStats( TEST_THREE, ReportEntryType.error, new DummyStackTraceWriter( ERROR ) ) );

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
        assertEquals( 0, mergedStatistics.getErrors() );
        assertEquals( 1, mergedStatistics.getFailures() );
        assertEquals( 3, mergedStatistics.getFlakes() );
        assertEquals( 0, mergedStatistics.getSkipped() );

        // Now test the result will be printed out correctly
        DummyTestReporter reporter = new DummyTestReporter();
        factory.printTestFailures( reporter, DefaultReporterFactory.TestResultType.flake );
        String[] expectedFlakeOutput =
            { "Flaked tests: ", TEST_FOUR, "  Run 1: " + ASSERTION_FAIL, "  Run 2: PASS", "", TEST_ONE,
                "  Run 1: " + ERROR, "  Run 2: " + ASSERTION_FAIL, "  Run 3: PASS", "", TEST_TWO, "  Run 1: " + ERROR,
                "  Run 2: PASS", "", "" };
        assertEquals( Arrays.asList( expectedFlakeOutput ), reporter.getMessages() );

        reporter = new DummyTestReporter();
        factory.printTestFailures( reporter, DefaultReporterFactory.TestResultType.failure );
        String[] expectedFailureOutput =
            { "Failed tests: ", TEST_THREE, "  Run 1: " + ASSERTION_FAIL, "  Run 2: " + ERROR, "  Run 3: " + ERROR, "",
                "" };
        assertEquals( Arrays.asList( expectedFailureOutput ), reporter.getMessages() );

        reporter = new DummyTestReporter();
        factory.printTestFailures( reporter, DefaultReporterFactory.TestResultType.error );
        String[] expectedErrorOutput = { "" };
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
        DefaultReporterFactory factory = new DefaultReporterFactory( StartupReportConfiguration.defaultValue() );

        List<ReportEntryType> emptyList = new ArrayList<ReportEntryType>();
        assertEquals( unknown, factory.getTestResultType( emptyList ) );

        List<ReportEntryType> successList = new ArrayList<ReportEntryType>();
        successList.add( ReportEntryType.success );
        successList.add( ReportEntryType.success );
        assertEquals( success, factory.getTestResultType( successList ) );

        List<ReportEntryType> failureErrorList = new ArrayList<ReportEntryType>();
        failureErrorList.add( ReportEntryType.failure );
        failureErrorList.add( ReportEntryType.error );
        assertEquals( failure, factory.getTestResultType( failureErrorList ) );

        List<ReportEntryType> errorFailureList = new ArrayList<ReportEntryType>();
        errorFailureList.add( ReportEntryType.error );
        errorFailureList.add( ReportEntryType.failure );
        assertEquals( error, factory.getTestResultType( errorFailureList ) );

        List<ReportEntryType> flakeList = new ArrayList<ReportEntryType>();
        flakeList.add( ReportEntryType.success );
        flakeList.add( ReportEntryType.failure );
        assertEquals( flake, factory.getTestResultType( flakeList ) );

        flakeList = new ArrayList<ReportEntryType>();
        flakeList.add( ReportEntryType.error );
        flakeList.add( ReportEntryType.success );
        flakeList.add( ReportEntryType.failure );
        assertEquals( flake, factory.getTestResultType( flakeList ) );

        List<ReportEntryType> skippedList = new ArrayList<ReportEntryType>();
        skippedList.add( ReportEntryType.skipped );
        assertEquals( skipped, factory.getTestResultType( skippedList ) );
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