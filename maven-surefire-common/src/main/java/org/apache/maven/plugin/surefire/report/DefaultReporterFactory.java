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
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.Level;
import org.apache.maven.plugin.surefire.runorder.StatisticsReporter;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.suite.RunResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.apache.maven.plugin.surefire.log.api.Level.resolveLevel;
import static org.apache.maven.plugin.surefire.report.ConsoleReporter.PLAIN;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.error;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.failure;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.flake;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.skipped;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.success;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.unknown;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.ERROR;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.FAILURE;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.SUCCESS;
import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;
import static org.apache.maven.surefire.util.internal.ObjectUtils.useNonNull;

/**
 * Provides reporting modules on the plugin side.
 * <br>
 * Keeps a centralized count of test run results.
 *
 * @author Kristian Rosenvold
 */
public class DefaultReporterFactory
    implements ReporterFactory
{
    private final StartupReportConfiguration reportConfiguration;
    private final ConsoleLogger consoleLogger;
    private final Collection<TestSetRunListener> listeners;

    private RunStatistics globalStats = new RunStatistics();

    // from "<testclass>.<testmethod>" -> statistics about all the runs for flaky tests
    private Map<String, List<TestMethodStats>> flakyTests;

    // from "<testclass>.<testmethod>" -> statistics about all the runs for failed tests
    private Map<String, List<TestMethodStats>> failedTests;

    // from "<testclass>.<testmethod>" -> statistics about all the runs for error tests
    private Map<String, List<TestMethodStats>> errorTests;

    public DefaultReporterFactory( StartupReportConfiguration reportConfiguration, ConsoleLogger consoleLogger )
    {
        this.reportConfiguration = reportConfiguration;
        this.consoleLogger = consoleLogger;
        listeners = new ConcurrentLinkedQueue<TestSetRunListener>();
    }

    @Override
    public RunListener createReporter()
    {
        TestSetRunListener testSetRunListener =
            new TestSetRunListener( createConsoleReporter(),
                                    createFileReporter(),
                                    createSimpleXMLReporter(),
                                    createConsoleOutputReceiver(),
                                    createStatisticsReporter(),
                                    reportConfiguration.isTrimStackTrace(),
                                    PLAIN.equals( reportConfiguration.getReportFormat() ),
                                    reportConfiguration.isBriefOrPlainFormat() );
        addListener( testSetRunListener );
        return testSetRunListener;
    }

    public File getReportsDirectory()
    {
        return reportConfiguration.getReportsDirectory();
    }

    private ConsoleReporter createConsoleReporter()
    {
        return shouldReportToConsole() ? new ConsoleReporter( consoleLogger ) : NullConsoleReporter.INSTANCE;
    }

    private FileReporter createFileReporter()
    {
        final FileReporter fileReporter = reportConfiguration.instantiateFileReporter();
        return useNonNull( fileReporter, NullFileReporter.INSTANCE );
    }

    private StatelessXmlReporter createSimpleXMLReporter()
    {
        final StatelessXmlReporter xmlReporter = reportConfiguration.instantiateStatelessXmlReporter();
        return useNonNull( xmlReporter, NullStatelessXmlReporter.INSTANCE );
    }

    private TestcycleConsoleOutputReceiver createConsoleOutputReceiver()
    {
        final TestcycleConsoleOutputReceiver consoleOutputReceiver =
                reportConfiguration.instantiateConsoleOutputFileReporter();
        return useNonNull( consoleOutputReceiver, NullConsoleOutputReceiver.INSTANCE );
    }

    private StatisticsReporter createStatisticsReporter()
    {
        final StatisticsReporter statisticsReporter = reportConfiguration.getStatisticsReporter();
        return useNonNull( statisticsReporter, NullStatisticsReporter.INSTANCE );
    }

    private boolean shouldReportToConsole()
    {
        return reportConfiguration.isUseFile()
                       ? reportConfiguration.isPrintSummary()
                       : reportConfiguration.isRedirectTestOutputToFile() || reportConfiguration.isBriefOrPlainFormat();
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

    @Override
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

    public void runStarting()
    {
        log( "" );
        log( "-------------------------------------------------------" );
        log( " T E S T S" );
        log( "-------------------------------------------------------" );
    }

    private void runCompleted()
    {
        if ( reportConfiguration.isPrintSummary() )
        {
            log( "" );
            log( "Results:" );
            log( "" );
        }
        boolean printedFailures = printTestFailures( failure );
        boolean printedErrors = printTestFailures( error );
        boolean printedFlakes = printTestFailures( flake );
        if ( printedFailures | printedErrors | printedFlakes )
        {
            log( "" );
        }
        boolean hasSuccessful = globalStats.getCompletedCount() > 0;
        boolean hasSkipped = globalStats.getSkipped() > 0;
        log( globalStats.getSummary(), hasSuccessful, printedFailures, printedErrors, hasSkipped, printedFlakes );
        log( "" );
    }

    public RunStatistics getGlobalRunStatistics()
    {
        mergeTestHistoryResult();
        return globalStats;
    }

    /**
     * Get the result of a test based on all its runs. If it has success and failures/errors, then it is a flake;
     * if it only has errors or failures, then count its result based on its first run
     *
     * @param reportEntries the list of test run report type for a given test
     * @param rerunFailingTestsCount configured rerun count for failing tests
     * @return the type of test result
     */
    // Use default visibility for testing
    static TestResultType getTestResultType( List<ReportEntryType> reportEntries, int rerunFailingTestsCount  )
    {
        if ( reportEntries == null || reportEntries.isEmpty() )
        {
            return unknown;
        }

        boolean seenSuccess = false, seenFailure = false, seenError = false;
        for ( ReportEntryType resultType : reportEntries )
        {
            if ( resultType == SUCCESS )
            {
                seenSuccess = true;
            }
            else if ( resultType == FAILURE )
            {
                seenFailure = true;
            }
            else if ( resultType == ERROR )
            {
                seenError = true;
            }
        }

        if ( seenFailure || seenError )
        {
            if ( seenSuccess && rerunFailingTestsCount > 0 )
            {
                return flake;
            }
            else
            {
                return seenError ? error : failure;
            }
        }
        else if ( seenSuccess )
        {
            return success;
        }
        else
        {
            return skipped;
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

            List<ReportEntryType> resultTypes = new ArrayList<ReportEntryType>();
            for ( TestMethodStats methodStats : testMethodStats )
            {
                resultTypes.add( methodStats.getResultType() );
            }

            switch ( getTestResultType( resultTypes, reportConfiguration.getRerunFailingTestsCount() ) )
            {
                case success:
                    // If there are multiple successful runs of the same test, count all of them
                    int successCount = 0;
                    for ( ReportEntryType type : resultTypes )
                    {
                        if ( type == SUCCESS )
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
     * @param type   the type of results to be printed, could be error, failure or flake
     * @return {@code true} if printed some lines
     */
    // Use default visibility for testing
    boolean printTestFailures( TestResultType type )
    {
        final Map<String, List<TestMethodStats>> testStats;
        final Level level;
        switch ( type )
        {
            case failure:
                testStats = failedTests;
                level = Level.FAILURE;
                break;
            case error:
                testStats = errorTests;
                level = Level.FAILURE;
                break;
            case flake:
                testStats = flakyTests;
                level = Level.UNSTABLE;
                break;
            default:
                return false;
        }

        boolean printed = false;
        if ( !testStats.isEmpty() )
        {
            log( type.getLogPrefix(), level );
            printed = true;
        }

        for ( Map.Entry<String, List<TestMethodStats>> entry : testStats.entrySet() )
        {
            printed = true;
            List<TestMethodStats> testMethodStats = entry.getValue();
            if ( testMethodStats.size() == 1 )
            {
                // No rerun, follow the original output format
                failure( "  " + testMethodStats.get( 0 ).getStackTraceWriter().smartTrimmedStackTrace() );
            }
            else
            {
                log( entry.getKey(), level );
                for ( int i = 0; i < testMethodStats.size(); i++ )
                {
                    StackTraceWriter failureStackTrace = testMethodStats.get( i ).getStackTraceWriter();
                    if ( failureStackTrace == null )
                    {
                        success( "  Run " + ( i + 1 ) + ": PASS" );
                    }
                    else
                    {
                        failure( "  Run " + ( i + 1 ) + ": " + failureStackTrace.smartTrimmedStackTrace() );
                    }
                }
                log( "" );
            }
        }
        return printed;
    }

    // Describe the result of a given test
    enum TestResultType
    {

        error(   "Errors: "   ),
        failure( "Failures: " ),
        flake(   "Flakes: "   ),
        success( "Success: "  ),
        skipped( "Skipped: "  ),
        unknown( "Unknown: "  );

        private final String logPrefix;

        TestResultType( String logPrefix )
        {
            this.logPrefix = logPrefix;
        }

        public String getLogPrefix()
        {
            return logPrefix;
        }
    }

    private void log( String s, boolean success, boolean failures, boolean errors, boolean skipped, boolean flakes )
    {
        Level level = resolveLevel( success, failures, errors, skipped, flakes );
        log( s, level );
    }

    private void log( String s, Level level )
    {
        MessageBuilder builder = buffer();
        switch ( level )
        {
            case FAILURE:
                consoleLogger.error( builder.failure( s ).toString() );
                break;
            case UNSTABLE:
                consoleLogger.warning( builder.warning( s ).toString() );
                break;
            case SUCCESS:
                consoleLogger.info( builder.success( s ).toString() );
                break;
            default:
                consoleLogger.info( builder.a( s ).toString() );
        }
    }

    private void log( String s )
    {
        consoleLogger.info( s );
    }

    private void info( String s )
    {
        MessageBuilder builder = buffer();
        consoleLogger.info( builder.info( s ).toString() );
    }

    private void err( String s )
    {
        MessageBuilder builder = buffer();
        consoleLogger.error( builder.error( s ).toString() );
    }

    private void success( String s )
    {
        MessageBuilder builder = buffer();
        consoleLogger.info( builder.success( s ).toString() );
    }

    private void failure( String s )
    {
        MessageBuilder builder = buffer();
        consoleLogger.error( builder.failure( s ).toString() );
    }
}
