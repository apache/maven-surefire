/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.surefire.report;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.maven.plugin.surefire.StartupReportConfiguration;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.Level;
import org.apache.maven.plugin.surefire.runorder.StatisticsReporter;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.extensions.ConsoleOutputReportEventListener;
import org.apache.maven.surefire.extensions.StatelessReportEventListener;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoConsoleReportEventListener;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoFileReportEventListener;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.shared.utils.logging.MessageBuilder;

import static org.apache.maven.plugin.surefire.log.api.Level.resolveLevel;
import static org.apache.maven.plugin.surefire.report.ConsoleReporter.PLAIN;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.ERROR;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.FAILURE;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.FLAKE;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.SKIPPED;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.SUCCESS;
import static org.apache.maven.plugin.surefire.report.DefaultReporterFactory.TestResultType.UNKNOWN;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.ERROR;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.FAILURE;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.SUCCESS;
import static org.apache.maven.surefire.api.util.internal.ObjectUtils.useNonNull;
import static org.apache.maven.surefire.shared.utils.logging.MessageUtils.buffer;

/**
 * Provides reporting modules on the plugin side.
 * <br>
 * Keeps a centralized count of test run results.
 *
 * @author Kristian Rosenvold
 */
public class DefaultReporterFactory implements ReporterFactory, ReportsMerger {
    private final Collection<TestSetRunListener> listeners = new ConcurrentLinkedQueue<>();
    private final StartupReportConfiguration reportConfiguration;
    private final ConsoleLogger consoleLogger;
    private final Integer forkNumber;

    private RunStatistics globalStats = new RunStatistics();

    // from "<testclass>.<testmethod>" -> statistics about all the runs for flaky tests
    private Map<String, List<TestMethodStats>> flakyTests;

    // from "<testclass>.<testmethod>" -> statistics about all the runs for failed tests
    private Map<String, List<TestMethodStats>> failedTests;

    // from "<testclass>.<testmethod>" -> statistics about all the runs for error tests
    private Map<String, List<TestMethodStats>> errorTests;

    public DefaultReporterFactory(StartupReportConfiguration reportConfiguration, ConsoleLogger consoleLogger) {
        this(reportConfiguration, consoleLogger, null);
    }

    public DefaultReporterFactory(
            StartupReportConfiguration reportConfiguration, ConsoleLogger consoleLogger, Integer forkNumber) {
        this.reportConfiguration = reportConfiguration;
        this.consoleLogger = consoleLogger;
        this.forkNumber = forkNumber;
    }

    @Override
    public TestReportListener<TestOutputReportEntry> createTestReportListener() {
        TestSetRunListener testSetRunListener = new TestSetRunListener(
                createConsoleReporter(),
                createFileReporter(),
                createSimpleXMLReporter(),
                createConsoleOutputReceiver(),
                createStatisticsReporter(),
                reportConfiguration.isTrimStackTrace(),
                PLAIN.equals(reportConfiguration.getReportFormat()),
                reportConfiguration.isBriefOrPlainFormat(),
                consoleLogger,
                reportConfiguration.getReporterFactoryOptions().isStatPerSourceName());
        addListener(testSetRunListener);
        return testSetRunListener;
    }

    @Override
    public File getReportsDirectory() {
        return reportConfiguration.getReportsDirectory();
    }

    private StatelessTestsetInfoConsoleReportEventListener<WrappedReportEntry, TestSetStats> createConsoleReporter() {
        StatelessTestsetInfoConsoleReportEventListener<WrappedReportEntry, TestSetStats> consoleReporter =
                reportConfiguration.instantiateConsoleReporter(consoleLogger);
        return useNonNull(consoleReporter, NullConsoleReporter.INSTANCE);
    }

    private StatelessTestsetInfoFileReportEventListener<WrappedReportEntry, TestSetStats> createFileReporter() {
        StatelessTestsetInfoFileReportEventListener<WrappedReportEntry, TestSetStats> fileReporter =
                reportConfiguration.instantiateFileReporter(forkNumber);
        return useNonNull(fileReporter, NullFileReporter.INSTANCE);
    }

    private StatelessReportEventListener<WrappedReportEntry, TestSetStats> createSimpleXMLReporter() {
        StatelessReportEventListener<WrappedReportEntry, TestSetStats> xmlReporter =
                reportConfiguration.instantiateStatelessXmlReporter(forkNumber);
        return useNonNull(xmlReporter, NullStatelessXmlReporter.INSTANCE);
    }

    private ConsoleOutputReportEventListener createConsoleOutputReceiver() {
        ConsoleOutputReportEventListener outputReporter =
                reportConfiguration.instantiateConsoleOutputFileReporter(forkNumber);
        return useNonNull(outputReporter, NullConsoleOutputReceiver.INSTANCE);
    }

    private StatisticsReporter createStatisticsReporter() {
        StatisticsReporter statisticsReporter = reportConfiguration.getStatisticsReporter();
        return useNonNull(statisticsReporter, NullStatisticsReporter.INSTANCE);
    }

    @Override
    public void mergeFromOtherFactories(Collection<DefaultReporterFactory> factories) {
        for (DefaultReporterFactory factory : factories) {
            listeners.addAll(factory.listeners);
        }
    }

    final void addListener(TestSetRunListener listener) {
        listeners.add(listener);
    }

    @Override
    public RunResult close() {
        mergeTestHistoryResult();
        runCompleted();
        for (TestSetRunListener listener : listeners) {
            listener.close();
        }
        return globalStats.getRunResult();
    }

    @Override
    public void runStarting() {
        if (reportConfiguration.isPrintSummary()) {
            log("");
            log("-------------------------------------------------------");
            log(" T E S T S");
            log("-------------------------------------------------------");
        }
    }

    private void runCompleted() {
        if (reportConfiguration.isPrintSummary()) {
            log("");
            log("Results:");
            log("");
        }
        boolean printedFailures = printTestFailures(TestResultType.FAILURE);
        boolean printedErrors = printTestFailures(TestResultType.ERROR);
        boolean printedFlakes = printTestFailures(TestResultType.FLAKE);
        if (reportConfiguration.isPrintSummary()) {
            if (printedFailures | printedErrors | printedFlakes) {
                log("");
            }
            boolean hasSuccessful = globalStats.getCompletedCount() > 0;
            boolean hasSkipped = globalStats.getSkipped() > 0;
            log(globalStats.getSummary(), hasSuccessful, printedFailures, printedErrors, hasSkipped, printedFlakes);
            log("");
        }
    }

    public RunStatistics getGlobalRunStatistics() {
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
    static TestResultType getTestResultType(List<ReportEntryType> reportEntries, int rerunFailingTestsCount) {
        if (reportEntries == null || reportEntries.isEmpty()) {
            return UNKNOWN;
        }

        boolean seenSuccess = false, seenFailure = false, seenError = false;
        for (ReportEntryType resultType : reportEntries) {
            if (resultType == ReportEntryType.SUCCESS) {
                seenSuccess = true;
            } else if (resultType == ReportEntryType.FAILURE) {
                seenFailure = true;
            } else if (resultType == ReportEntryType.ERROR) {
                seenError = true;
            }
        }

        if (seenFailure || seenError) {
            if (seenSuccess && rerunFailingTestsCount > 0) {
                return TestResultType.FLAKE;
            } else {
                return seenError ? TestResultType.ERROR : TestResultType.FAILURE;
            }
        } else if (seenSuccess) {
            return TestResultType.SUCCESS;
        } else {
            return SKIPPED;
        }
    }

    /**
     * Merge all the TestMethodStats in each TestRunListeners and put results into flakyTests, failedTests and
     * errorTests, indexed by test class and method name. Update globalStatistics based on the result of the merge.
     */
    private void mergeTestHistoryResult() {
        globalStats = new RunStatistics();
        flakyTests = new TreeMap<>();
        failedTests = new TreeMap<>();
        errorTests = new TreeMap<>();

        Map<String, List<TestMethodStats>> mergedTestHistoryResult = new HashMap<>();
        // Merge all the stats for tests from listeners
        for (TestSetRunListener listener : listeners) {
            for (TestMethodStats methodStats : listener.getTestMethodStats()) {
                List<TestMethodStats> currentMethodStats =
                        mergedTestHistoryResult.get(methodStats.getTestClassMethodName());
                if (currentMethodStats == null) {
                    currentMethodStats = new ArrayList<>();
                    currentMethodStats.add(methodStats);
                    mergedTestHistoryResult.put(methodStats.getTestClassMethodName(), currentMethodStats);
                } else {
                    currentMethodStats.add(methodStats);
                }
            }
        }

        // Update globalStatistics by iterating through mergedTestHistoryResult
        int completedCount = 0, skipped = 0;

        for (Map.Entry<String, List<TestMethodStats>> entry : mergedTestHistoryResult.entrySet()) {
            List<TestMethodStats> testMethodStats = entry.getValue();
            String testClassMethodName = entry.getKey();
            completedCount++;

            List<ReportEntryType> resultTypes = new ArrayList<>();
            for (TestMethodStats methodStats : testMethodStats) {
                resultTypes.add(methodStats.getResultType());
            }

            switch (getTestResultType(resultTypes, reportConfiguration.getRerunFailingTestsCount())) {
                case SUCCESS:
                    // If there are multiple successful runs of the same test, count all of them
                    int successCount = 0;
                    for (ReportEntryType type : resultTypes) {
                        if (type == ReportEntryType.SUCCESS) {
                            successCount++;
                        }
                    }
                    completedCount += successCount - 1;
                    break;
                case SKIPPED:
                    skipped++;
                    break;
                case FLAKE:
                    flakyTests.put(testClassMethodName, testMethodStats);
                    break;
                case FAILURE:
                    failedTests.put(testClassMethodName, testMethodStats);
                    break;
                case ERROR:
                    errorTests.put(testClassMethodName, testMethodStats);
                    break;
                default:
                    throw new IllegalStateException("Get unknown test result type");
            }
        }

        globalStats.set(completedCount, errorTests.size(), failedTests.size(), skipped, flakyTests.size());
    }

    /**
     * Print failed tests and flaked tests. A test is considered as a failed test if it failed/got an error with
     * all the runs. If a test passes in ever of the reruns, it will be count as a flaked test
     *
     * @param type   the type of results to be printed, could be error, failure or flake
     * @return {@code true} if printed some lines
     */
    // Use default visibility for testing
    boolean printTestFailures(TestResultType type) {
        final Map<String, List<TestMethodStats>> testStats;
        final Level level;
        switch (type) {
            case FAILURE:
                testStats = failedTests;
                level = Level.FAILURE;
                break;
            case ERROR:
                testStats = errorTests;
                level = Level.FAILURE;
                break;
            case FLAKE:
                testStats = flakyTests;
                level = Level.UNSTABLE;
                break;
            default:
                return false;
        }

        boolean printed = false;
        if (!testStats.isEmpty()) {
            log(type.getLogPrefix(), level);
            printed = true;
        }

        for (Map.Entry<String, List<TestMethodStats>> entry : testStats.entrySet()) {
            List<TestMethodStats> testMethodStats = entry.getValue();
            if (testMethodStats.size() == 1) {
                // No rerun, follow the original output format
                failure("  " + testMethodStats.get(0).getStackTraceWriter().smartTrimmedStackTrace());
            } else {
                log(entry.getKey(), level);
                for (int i = 0; i < testMethodStats.size(); i++) {
                    StackTraceWriter failureStackTrace = testMethodStats.get(i).getStackTraceWriter();
                    if (failureStackTrace == null) {
                        success("  Run " + (i + 1) + ": PASS");
                    } else {
                        failure("  Run " + (i + 1) + ": " + failureStackTrace.smartTrimmedStackTrace());
                    }
                }
                log("");
            }
        }
        return printed;
    }

    // Describe the result of a given test
    enum TestResultType {
        ERROR("Errors: "),
        FAILURE("Failures: "),
        FLAKE("Flakes: "),
        SUCCESS("Success: "),
        SKIPPED("Skipped: "),
        UNKNOWN("Unknown: ");

        private final String logPrefix;

        TestResultType(String logPrefix) {
            this.logPrefix = logPrefix;
        }

        public String getLogPrefix() {
            return logPrefix;
        }
    }

    private void log(String s, boolean success, boolean failures, boolean errors, boolean skipped, boolean flakes) {
        Level level = resolveLevel(success, failures, errors, skipped, flakes);
        log(s, level);
    }

    private void log(String s, Level level) {
        switch (level) {
            case FAILURE:
                failure(s);
                break;
            case UNSTABLE:
                warning(s);
                break;
            case SUCCESS:
                success(s);
                break;
            default:
                info(s);
        }
    }

    private void log(String s) {
        consoleLogger.info(s);
    }

    private void info(String s) {
        MessageBuilder builder = buffer();
        consoleLogger.info(builder.a(s).toString());
    }

    private void warning(String s) {
        MessageBuilder builder = buffer();
        consoleLogger.warning(builder.warning(s).toString());
    }

    private void success(String s) {
        MessageBuilder builder = buffer();
        consoleLogger.info(builder.success(s).toString());
    }

    private void failure(String s) {
        MessageBuilder builder = buffer();
        consoleLogger.error(builder.failure(s).toString());
    }
}
