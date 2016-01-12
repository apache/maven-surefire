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

import org.apache.maven.plugin.surefire.StartupReportConfiguration;
import org.apache.maven.plugin.surefire.runorder.StatisticsReporter;
import org.apache.maven.surefire.report.DefaultDirectConsoleReporter;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.suite.RunResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Provides reporting modules on the plugin side.
 * <p/>
 * Keeps a centralized count of test run results.
 *
 * @author Kristian Rosenvold
 */
public class DefaultReporterFactory
    implements ReporterFactory
{

    private RunStatistics globalStats = new RunStatistics();

    private final StartupReportConfiguration reportConfiguration;

    private final StatisticsReporter statisticsReporter;

    private final Collection<TestSetRunListener> listeners = new ConcurrentLinkedQueue<TestSetRunListener>();

    // from "<testclass>.<testmethod>" -> statistics about all the runs for flaky tests
    private Map<String, List<TestMethodStats>> flakyTests;

    // from "<testclass>.<testmethod>" -> statistics about all the runs for failed tests
    private Map<String, List<TestMethodStats>> failedTests;

    // from "<testclass>.<testmethod>" -> statistics about all the runs for error tests
    private Map<String, List<TestMethodStats>> errorTests;

    public DefaultReporterFactory( StartupReportConfiguration reportConfiguration )
    {
        this.reportConfiguration = reportConfiguration;
        this.statisticsReporter = reportConfiguration.instantiateStatisticsReporter();
    }

    public RunListener createReporter()
    {
        TestSetRunListener testSetRunListener =
            new TestSetRunListener( reportConfiguration.instantiateConsoleReporter(),
                                    reportConfiguration.instantiateFileReporter(),
                                    reportConfiguration.instantiateStatelessXmlReporter(),
                                    reportConfiguration.instantiateConsoleOutputFileReporter(), statisticsReporter,
                                    reportConfiguration.isTrimStackTrace(),
                                    ConsoleReporter.PLAIN.equals( reportConfiguration.getReportFormat() ),
                                    reportConfiguration.isBriefOrPlainFormat() );
        addListener( testSetRunListener );
        return testSetRunListener;
    }

    public void mergeFromOtherFactories( Collection<DefaultReporterFactory> factories )
    {
        for ( DefaultReporterFactory factory : factories )
        {
            for ( TestSetRunListener listener : factory.listeners )
            {
                listeners.add( listener );
            }
        }
    }

    final void addListener( TestSetRunListener listener )
    {
        listeners.add( listener );
    }

    public RunResult close()
    {
        mergeTestHistoryResult();
        runCompleted();
        for ( TestSetRunListener listener : listeners )
        {
            listener.close();
        }
        return globalStats.getRunResult();
    }

    private DefaultDirectConsoleReporter createConsoleLogger()
    {
        return new DefaultDirectConsoleReporter( reportConfiguration.getOriginalSystemOut() );
    }

    public void runStarting()
    {
        final DefaultDirectConsoleReporter consoleReporter = createConsoleLogger();
        consoleReporter.info( "" );
        consoleReporter.info( "-------------------------------------------------------" );
        consoleReporter.info( " T E S T S" );
        consoleReporter.info( "-------------------------------------------------------" );
    }

    private void runCompleted()
    {
        final DefaultDirectConsoleReporter logger = createConsoleLogger();
        if ( reportConfiguration.isPrintSummary() )
        {
            logger.info( "" );
            logger.info( "Results :" );
            logger.info( "" );
        }
        boolean printedFailures = printTestFailures( logger, TestResultType.failure );
        printedFailures |= printTestFailures( logger, TestResultType.error );
        printedFailures |= printTestFailures( logger, TestResultType.flake );
        if ( printedFailures )
        {
            logger.info( "" );
        }
        logger.info( globalStats.getSummary() );
        logger.info( "" );
    }

    public RunStatistics getGlobalRunStatistics()
    {
        mergeTestHistoryResult();
        return globalStats;
    }

    public static DefaultReporterFactory defaultNoXml()
    {
        return new DefaultReporterFactory( StartupReportConfiguration.defaultNoXml() );
    }

    /**
     * Get the result of a test based on all its runs. If it has success and failures/errors, then it is a flake;
     * if it only has errors or failures, then count its result based on its first run
     *
     * @param reportEntryList the list of test run report type for a given test
     * @param rerunFailingTestsCount configured rerun count for failing tests
     * @return the type of test result
     */
    // Use default visibility for testing
    static TestResultType getTestResultType( List<ReportEntryType> reportEntryList, int rerunFailingTestsCount  )
    {
        if ( reportEntryList == null || reportEntryList.isEmpty() )
        {
            return TestResultType.unknown;
        }

        boolean seenSuccess = false, seenFailure = false, seenError = false;
        for ( ReportEntryType resultType : reportEntryList )
        {
            if ( resultType == ReportEntryType.SUCCESS )
            {
                seenSuccess = true;
            }
            else if ( resultType == ReportEntryType.FAILURE )
            {
                seenFailure = true;
            }
            else if ( resultType == ReportEntryType.ERROR )
            {
                seenError = true;
            }
        }

        if ( seenFailure || seenError )
        {
            if ( seenSuccess && rerunFailingTestsCount > 0 )
            {
                return TestResultType.flake;
            }
            else
            {
                if ( seenError )
                {
                    return TestResultType.error;
                }
                else
                {
                    return TestResultType.failure;
                }
            }
        }
        else if ( seenSuccess )
        {
            return TestResultType.success;
        }
        else
        {
            return TestResultType.skipped;
        }
    }

    /**
     * Merge all the TestMethodStats in each TestRunListeners and put results into flakyTests, failedTests and
     * errorTests, indexed by test class and method name. Update globalStatistics based on the result of the merge.
     */
    void mergeTestHistoryResult()
    {
        globalStats = new RunStatistics();
        flakyTests = new TreeMap<String, List<TestMethodStats>>();
        failedTests = new TreeMap<String, List<TestMethodStats>>();
        errorTests = new TreeMap<String, List<TestMethodStats>>();

        Map<String, List<TestMethodStats>> mergedTestHistoryResult = new HashMap<String, List<TestMethodStats>>();
        // Merge all the stats for tests from listeners
        for ( TestSetRunListener listener : listeners )
        {
            List<TestMethodStats> testMethodStats = listener.getTestMethodStats();
            for ( TestMethodStats methodStats : testMethodStats )
            {
                List<TestMethodStats> currentMethodStats =
                    mergedTestHistoryResult.get( methodStats.getTestClassMethodName() );
                if ( currentMethodStats == null )
                {
                    currentMethodStats = new ArrayList<TestMethodStats>();
                    currentMethodStats.add( methodStats );
                    mergedTestHistoryResult.put( methodStats.getTestClassMethodName(), currentMethodStats );
                }
                else
                {
                    currentMethodStats.add( methodStats );
                }
            }
        }

        // Update globalStatistics by iterating through mergedTestHistoryResult
        int completedCount = 0, skipped = 0;

        for ( Map.Entry<String, List<TestMethodStats>> entry : mergedTestHistoryResult.entrySet() )
        {
            List<TestMethodStats> testMethodStats = entry.getValue();
            String testClassMethodName = entry.getKey();
            completedCount++;

            List<ReportEntryType> resultTypeList = new ArrayList<ReportEntryType>();
            for ( TestMethodStats methodStats : testMethodStats )
            {
                resultTypeList.add( methodStats.getResultType() );
            }

            TestResultType resultType = getTestResultType( resultTypeList,
                                                           reportConfiguration.getRerunFailingTestsCount() );

            switch ( resultType )
            {
                case success:
                    // If there are multiple successful runs of the same test, count all of them
                    int successCount = 0;
                    for ( ReportEntryType type : resultTypeList )
                    {
                        if ( type == ReportEntryType.SUCCESS )
                        {
                            successCount++;
                        }
                    }
                    completedCount += successCount - 1;
                    break;
                case skipped:
                    skipped++;
                    break;
                case flake:
                    flakyTests.put( testClassMethodName, testMethodStats );
                    break;
                case failure:
                    failedTests.put( testClassMethodName, testMethodStats );
                    break;
                case error:
                    errorTests.put( testClassMethodName, testMethodStats );
                    break;
                default:
                    throw new IllegalStateException( "Get unknown test result type" );
            }
        }

        globalStats.set( completedCount, errorTests.size(), failedTests.size(), skipped, flakyTests.size() );
    }

    /**
     * Print failed tests and flaked tests. A test is considered as a failed test if it failed/got an error with
     * all the runs. If a test passes in ever of the reruns, it will be count as a flaked test
     *
     * @param logger the logger used to log information
     * @param type   the type of results to be printed, could be error, failure or flake
     * @return {@code true} if printed some lines
     */
    // Use default visibility for testing
    boolean printTestFailures( DefaultDirectConsoleReporter logger, TestResultType type )
    {
        boolean printed = false;
        final Map<String, List<TestMethodStats>> testStats;
        switch ( type )
        {
            case failure:
                testStats = failedTests;
                break;
            case error:
                testStats = errorTests;
                break;
            case flake:
                testStats = flakyTests;
                break;
            default:
                return printed;
        }

        if ( !testStats.isEmpty() )
        {
            logger.info( type.getLogPrefix() );
            printed = true;
        }

        for ( Map.Entry<String, List<TestMethodStats>> entry : testStats.entrySet() )
        {
            printed = true;
            List<TestMethodStats> testMethodStats = entry.getValue();
            if ( testMethodStats.size() == 1 )
            {
                // No rerun, follow the original output format
                logger.info( "  " + testMethodStats.get( 0 ).getStackTraceWriter().smartTrimmedStackTrace() );
            }
            else
            {
                logger.info( entry.getKey() );
                for ( int i = 0; i < testMethodStats.size(); i++ )
                {
                    StackTraceWriter failureStackTrace = testMethodStats.get( i ).getStackTraceWriter();
                    if ( failureStackTrace == null )
                    {
                        logger.info( "  Run " + ( i + 1 ) + ": PASS" );
                    }
                    else
                    {
                        logger.info( "  Run " + ( i + 1 ) + ": " + failureStackTrace.smartTrimmedStackTrace() );
                    }
                }
                logger.info( "" );
            }
        }
        return printed;
    }

    // Describe the result of a given test
    static enum TestResultType
    {

        error( "Tests in error: " ),
        failure( "Failed tests: " ),
        flake( "Flaked tests: " ),
        success( "Success: " ),
        skipped( "Skipped: " ),
        unknown( "Unknown: " );

        private final String logPrefix;

        private TestResultType( String logPrefix )
        {
            this.logPrefix = logPrefix;
        }

        public String getLogPrefix()
        {
            return logPrefix;
        }
    }
}
