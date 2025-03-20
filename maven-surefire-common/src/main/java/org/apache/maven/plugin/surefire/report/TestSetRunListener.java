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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import org.apache.maven.plugin.surefire.runorder.StatisticsReporter;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.extensions.ConsoleOutputReportEventListener;
import org.apache.maven.surefire.extensions.StatelessReportEventListener;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoConsoleReportEventListener;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoFileReportEventListener;

import static org.apache.maven.plugin.surefire.report.ReportEntryType.ERROR;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.FAILURE;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.SKIPPED;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.SUCCESS;

/**
 * Reports data for a single test set.
 * <br>
 *
 * @author Kristian Rosenvold
 */
public class TestSetRunListener implements TestReportListener<TestOutputReportEntry> {
    private final Queue<TestMethodStats> testMethodStats = new ConcurrentLinkedQueue<>();

    /**
     * will be used only if report entry have a sourceName other than that #currentTestSetStats will be used
     * it looks some provider doesn't provide enough information so we assume to use previous technique
     * class field (this is definitely hackish)
     */
    private final ConcurrentMap<String, TestSetStats> detailsPerSource = new ConcurrentHashMap<>();

    private final TestSetStats currentTestSetStats;

    private final ConsoleOutputReportEventListener testOutputReceiver;

    private final boolean briefOrPlainFormat;

    private final StatelessReportEventListener<WrappedReportEntry, TestSetStats> simpleXMLReporter;

    private final StatelessTestsetInfoConsoleReportEventListener<WrappedReportEntry, TestSetStats> consoleReporter;

    private final StatelessTestsetInfoFileReportEventListener<WrappedReportEntry, TestSetStats> fileReporter;

    private final StatisticsReporter statisticsReporter;

    private final Object lock;

    private final boolean trimStackTrace;

    private final boolean isPlainFormat;

    private final boolean statPerSourceName;

    private Utf8RecodingDeferredFileOutputStream testStdOut = initDeferred("stdout");

    private Utf8RecodingDeferredFileOutputStream testStdErr = initDeferred("stderr");

    @SuppressWarnings("checkstyle:parameternumber")
    public TestSetRunListener(
            StatelessTestsetInfoConsoleReportEventListener<WrappedReportEntry, TestSetStats> consoleReporter,
            StatelessTestsetInfoFileReportEventListener<WrappedReportEntry, TestSetStats> fileReporter,
            StatelessReportEventListener<WrappedReportEntry, TestSetStats> simpleXMLReporter,
            ConsoleOutputReportEventListener testOutputReceiver,
            StatisticsReporter statisticsReporter,
            boolean trimStackTrace,
            boolean isPlainFormat,
            boolean briefOrPlainFormat,
            Object lock,
            boolean statPerSourceName) {
        this.consoleReporter = consoleReporter;
        this.fileReporter = fileReporter;
        this.statisticsReporter = statisticsReporter;
        this.simpleXMLReporter = simpleXMLReporter;
        this.testOutputReceiver = testOutputReceiver;
        this.briefOrPlainFormat = briefOrPlainFormat;
        this.trimStackTrace = trimStackTrace;
        this.isPlainFormat = isPlainFormat;
        this.currentTestSetStats = new TestSetStats(trimStackTrace, isPlainFormat);
        this.lock = lock;
        this.statPerSourceName = statPerSourceName;
    }

    @Override
    public boolean isDebugEnabled() {
        return consoleReporter.getConsoleLogger().isDebugEnabled();
    }

    @Override
    public void debug(String message) {
        synchronized (lock) {
            consoleReporter.getConsoleLogger().debug(trimTrailingNewLine(message));
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return consoleReporter.getConsoleLogger().isInfoEnabled();
    }

    @Override
    public void info(String message) {
        synchronized (lock) {
            consoleReporter.getConsoleLogger().info(trimTrailingNewLine(message));
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return consoleReporter.getConsoleLogger().isWarnEnabled();
    }

    @Override
    public void warning(String message) {
        synchronized (lock) {
            consoleReporter.getConsoleLogger().warning(trimTrailingNewLine(message));
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return consoleReporter.getConsoleLogger().isErrorEnabled();
    }

    @Override
    public void error(String message) {
        synchronized (lock) {
            consoleReporter.getConsoleLogger().error(trimTrailingNewLine(message));
        }
    }

    @Override
    public void error(String message, Throwable t) {
        synchronized (lock) {
            consoleReporter.getConsoleLogger().error(trimTrailingNewLine(message), t);
        }
    }

    @Override
    public void error(Throwable t) {
        synchronized (lock) {
            consoleReporter.getConsoleLogger().error(t);
        }
    }

    @Override
    public void writeTestOutput(TestOutputReportEntry reportEntry) {
        try {
            synchronized (lock) {
                Utf8RecodingDeferredFileOutputStream stream = reportEntry.isStdOut() ? testStdOut : testStdErr;
                stream.write(reportEntry.getLog(), reportEntry.isNewLine());
                testOutputReceiver.writeTestOutput(reportEntry);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TestSetStats getTestSetStats(ReportEntry report) {
        if (statPerSourceName) {
            return detailsPerSource.computeIfAbsent(
                    report.getSourceName(), s -> new TestSetStats(trimStackTrace, isPlainFormat));
        }
        return currentTestSetStats;
    }

    @Override
    public void testSetStarting(TestSetReportEntry report) {
        getTestSetStats(report).testSetStart();
        consoleReporter.testSetStarting(report);
        testOutputReceiver.testSetStarting(report);
    }

    private void clearCapture() {
        if (testStdOut != null) {
            testStdOut.commit();
        }

        if (testStdErr != null) {
            testStdErr.commit();
        }

        testStdOut = initDeferred("stdout");
        testStdErr = initDeferred("stderr");
    }

    @Override
    public void testSetCompleted(TestSetReportEntry report) {
        final WrappedReportEntry wrap = wrapTestSet(report);
        TestSetStats testSetStats = getTestSetStats(report);
        final List<String> testResults = briefOrPlainFormat ? testSetStats.getTestResults() : Collections.emptyList();
        fileReporter.testSetCompleted(wrap, testSetStats, testResults);
        simpleXMLReporter.testSetCompleted(wrap, testSetStats);
        statisticsReporter.testSetCompleted();
        consoleReporter.testSetCompleted(wrap, testSetStats, testResults);
        testOutputReceiver.testSetCompleted(wrap);
        consoleReporter.reset();

        wrap.getStdout().free();
        wrap.getStdErr().free();

        addTestMethodStats(report);
        testSetStats.reset();
        clearCapture();
    }

    // ----------------------------------------------------------------------
    // Test callback methods:
    // ----------------------------------------------------------------------

    @Override
    public void testStarting(ReportEntry report) {
        getTestSetStats(report).testStart();
    }

    @Override
    public void testSucceeded(ReportEntry reportEntry) {
        WrappedReportEntry wrapped = wrap(reportEntry, SUCCESS);
        getTestSetStats(reportEntry).testSucceeded(wrapped);
        statisticsReporter.testSucceeded(reportEntry);
        clearCapture();
    }

    @Override
    public void testError(ReportEntry reportEntry) {
        WrappedReportEntry wrapped = wrap(reportEntry, ERROR);
        getTestSetStats(reportEntry).testError(wrapped);
        statisticsReporter.testError(reportEntry);
        clearCapture();
    }

    @Override
    public void testFailed(ReportEntry reportEntry) {
        WrappedReportEntry wrapped = wrap(reportEntry, FAILURE);
        getTestSetStats(reportEntry).testFailure(wrapped);
        statisticsReporter.testFailed(reportEntry);
        clearCapture();
    }

    // ----------------------------------------------------------------------
    // Counters
    // ----------------------------------------------------------------------

    @Override
    public void testSkipped(ReportEntry reportEntry) {
        WrappedReportEntry wrapped = wrap(reportEntry, SKIPPED);
        getTestSetStats(reportEntry).testSkipped(wrapped);
        statisticsReporter.testSkipped(reportEntry);
        clearCapture();
    }

    @Override
    public void testExecutionSkippedByUser() {
        clearCapture();
    }

    @Override
    public void testAssumptionFailure(ReportEntry report) {
        testSkipped(report);
    }

    private WrappedReportEntry wrap(ReportEntry other, ReportEntryType reportEntryType) {
        int estimatedElapsed = 0;
        if (reportEntryType != SKIPPED) {
            Integer etime = other.getElapsed();
            estimatedElapsed = etime == null ? getTestSetStats(other).getElapsedSinceLastStart() : etime;
        }

        return new WrappedReportEntry(other, reportEntryType, estimatedElapsed, testStdOut, testStdErr);
    }

    private WrappedReportEntry wrapTestSet(TestSetReportEntry other) {
        return new WrappedReportEntry(
                other,
                null,
                other.getElapsed() != null
                        ? other.getElapsed()
                        : getTestSetStats(other).getElapsedSinceTestSetStart(),
                testStdOut,
                testStdErr,
                other.getSystemProperties());
    }

    public void close() {
        testOutputReceiver.close();
    }

    private void addTestMethodStats(TestSetReportEntry report) {
        for (WrappedReportEntry reportEntry : getTestSetStats(report).getReportEntries()) {
            TestMethodStats methodStats = new TestMethodStats(
                    reportEntry.getClassMethodName(),
                    reportEntry.getReportEntryType(),
                    reportEntry.getStackTraceWriter());
            testMethodStats.add(methodStats);
        }
    }

    public Queue<TestMethodStats> getTestMethodStats() {
        return testMethodStats;
    }

    private static String trimTrailingNewLine(final String message) {
        final int e = message == null ? 0 : lineBoundSymbolWidth(message);
        return message != null && e != 0 ? message.substring(0, message.length() - e) : message;
    }

    private static int lineBoundSymbolWidth(String message) {
        return message.endsWith("\r\n") ? 2 : (message.endsWith("\n") || message.endsWith("\r") ? 1 : 0);
    }

    private static Utf8RecodingDeferredFileOutputStream initDeferred(String channel) {
        return new Utf8RecodingDeferredFileOutputStream(channel);
    }
}
