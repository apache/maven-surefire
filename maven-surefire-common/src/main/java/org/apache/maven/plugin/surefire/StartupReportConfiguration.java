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
package org.apache.maven.plugin.surefire;

import javax.annotation.Nonnull;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.plugin.surefire.extensions.DefaultStatelessReportMojoConfiguration;
import org.apache.maven.plugin.surefire.extensions.SurefireConsoleOutputReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessTestsetInfoReporter;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.report.TestSetStats;
import org.apache.maven.plugin.surefire.report.WrappedReportEntry;
import org.apache.maven.plugin.surefire.runorder.StatisticsReporter;
import org.apache.maven.surefire.api.report.ReporterFactoryOptions;
import org.apache.maven.surefire.extensions.ConsoleOutputReportEventListener;
import org.apache.maven.surefire.extensions.StatelessReportEventListener;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoConsoleReportEventListener;
import org.apache.maven.surefire.extensions.StatelessTestsetInfoFileReportEventListener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.plugin.surefire.SurefireHelper.replaceForkThreadsInPath;
import static org.apache.maven.plugin.surefire.report.ConsoleReporter.BRIEF;
import static org.apache.maven.plugin.surefire.report.ConsoleReporter.PLAIN;
import static org.apache.maven.surefire.shared.lang3.StringUtils.trimToNull;

/**
 * All the parameters used to construct reporters
 * <br>
 *
 * @author Kristian Rosenvold
 */
public final class StartupReportConfiguration {
    private final PrintStream originalSystemOut;

    private final PrintStream originalSystemErr;

    private final boolean useFile;

    private final boolean printSummary;

    private final String reportFormat;

    private final String reportNameSuffix;

    private final File statisticsFile;

    private final boolean requiresRunHistory;

    private final boolean redirectTestOutputToFile;

    private final File reportsDirectory;

    private final boolean trimStackTrace;

    private final int rerunFailingTestsCount;

    private final String xsdSchemaLocation;

    private final Map<String, Deque<WrappedReportEntry>> testClassMethodRunHistory = new ConcurrentHashMap<>();

    private final Charset encoding;

    private final boolean isForking;

    private final boolean enableOutErrElements;

    private final boolean enablePropertiesElement;

    private final SurefireStatelessReporter xmlReporter;

    private final SurefireConsoleOutputReporter consoleOutputReporter;

    private final SurefireStatelessTestsetInfoReporter testsetReporter;

    private StatisticsReporter statisticsReporter;

    private final ReporterFactoryOptions reporterFactoryOptions;

    /**
     * @since 3.3.1
     */
    @SuppressWarnings("checkstyle:parameternumber")
    public StartupReportConfiguration(
            boolean useFile,
            boolean printSummary,
            String reportFormat,
            boolean redirectTestOutputToFile,
            @Nonnull File reportsDirectory,
            boolean trimStackTrace,
            String reportNameSuffix,
            File statisticsFile,
            boolean requiresRunHistory,
            int rerunFailingTestsCount,
            String xsdSchemaLocation,
            String encoding,
            boolean isForking,
            boolean enableOutErrElements,
            boolean enablePropertiesElement,
            SurefireStatelessReporter xmlReporter,
            SurefireConsoleOutputReporter consoleOutputReporter,
            SurefireStatelessTestsetInfoReporter testsetReporter,
            ReporterFactoryOptions reporterFactoryOptions) {
        this.useFile = useFile;
        this.printSummary = printSummary;
        this.reportFormat = reportFormat;
        this.redirectTestOutputToFile = redirectTestOutputToFile;
        this.reportsDirectory = reportsDirectory;
        this.trimStackTrace = trimStackTrace;
        this.reportNameSuffix = reportNameSuffix;
        this.statisticsFile = statisticsFile;
        this.requiresRunHistory = requiresRunHistory;
        this.originalSystemOut = System.out;
        this.originalSystemErr = System.err;
        this.rerunFailingTestsCount = rerunFailingTestsCount;
        this.xsdSchemaLocation = xsdSchemaLocation;
        String charset = trimToNull(encoding);
        this.encoding = charset == null ? UTF_8 : Charset.forName(charset);
        this.isForking = isForking;
        this.enableOutErrElements = enableOutErrElements;
        this.enablePropertiesElement = enablePropertiesElement;
        this.xmlReporter = xmlReporter;
        this.consoleOutputReporter = consoleOutputReporter;
        this.testsetReporter = testsetReporter;
        this.reporterFactoryOptions = reporterFactoryOptions;
    }

    @SuppressWarnings("checkstyle:parameternumber")
    @Deprecated
    public StartupReportConfiguration(
            boolean useFile,
            boolean printSummary,
            String reportFormat,
            boolean redirectTestOutputToFile,
            @Nonnull File reportsDirectory,
            boolean trimStackTrace,
            String reportNameSuffix,
            File statisticsFile,
            boolean requiresRunHistory,
            int rerunFailingTestsCount,
            String xsdSchemaLocation,
            String encoding,
            boolean isForking,
            SurefireStatelessReporter xmlReporter,
            SurefireConsoleOutputReporter consoleOutputReporter,
            SurefireStatelessTestsetInfoReporter testsetReporter) {
        this(
                useFile,
                printSummary,
                reportFormat,
                redirectTestOutputToFile,
                reportsDirectory,
                trimStackTrace,
                reportNameSuffix,
                statisticsFile,
                requiresRunHistory,
                rerunFailingTestsCount,
                xsdSchemaLocation,
                encoding,
                isForking,
                true,
                true,
                xmlReporter,
                consoleOutputReporter,
                testsetReporter,
                new ReporterFactoryOptions(false));
    }

    public boolean isUseFile() {
        return useFile;
    }

    public boolean isPrintSummary() {
        return printSummary;
    }

    public String getReportFormat() {
        return reportFormat;
    }

    public String getReportNameSuffix() {
        return reportNameSuffix;
    }

    public boolean isRedirectTestOutputToFile() {
        return redirectTestOutputToFile;
    }

    public File getReportsDirectory() {
        return reportsDirectory;
    }

    public int getRerunFailingTestsCount() {
        return rerunFailingTestsCount;
    }

    public StatelessReportEventListener<WrappedReportEntry, TestSetStats> instantiateStatelessXmlReporter(
            Integer forkNumber) {
        assert (forkNumber == null) == !isForking;

        // If forking TestNG the suites have same name 'TestSuite' and tend to override report statistics in stateful
        // reporter, see Surefire1535TestNGParallelSuitesIT. The testClassMethodRunHistory should be isolated.
        // In the in-plugin execution of parallel JUnit4.7 with rerun the map must be shared because reports and
        // listeners are in ThreadLocal, see Surefire1122ParallelAndFlakyTestsIT.
        Map<String, Deque<WrappedReportEntry>> testClassMethodRunHistory =
                isForking ? new ConcurrentHashMap<>() : this.testClassMethodRunHistory;

        DefaultStatelessReportMojoConfiguration xmlReporterConfig = new DefaultStatelessReportMojoConfiguration(
                resolveReportsDirectory(forkNumber),
                reportNameSuffix,
                trimStackTrace,
                rerunFailingTestsCount,
                xsdSchemaLocation,
                enableOutErrElements,
                enablePropertiesElement,
                testClassMethodRunHistory);

        return xmlReporter.isDisable() ? null : xmlReporter.createListener(xmlReporterConfig);
    }

    public StatelessTestsetInfoFileReportEventListener<WrappedReportEntry, TestSetStats> instantiateFileReporter(
            Integer forkNumber) {
        return !testsetReporter.isDisable() && isUseFile() && isBriefOrPlainFormat()
                ? testsetReporter.createListener(resolveReportsDirectory(forkNumber), reportNameSuffix, encoding)
                : null;
    }

    public StatelessTestsetInfoConsoleReportEventListener<WrappedReportEntry, TestSetStats> instantiateConsoleReporter(
            ConsoleLogger consoleLogger) {
        return !testsetReporter.isDisable() && shouldReportToConsole()
                ? testsetReporter.createListener(consoleLogger)
                : null;
    }

    public boolean isBriefOrPlainFormat() {
        String fmt = getReportFormat();
        return BRIEF.equals(fmt) || PLAIN.equals(fmt);
    }

    public ConsoleOutputReportEventListener instantiateConsoleOutputFileReporter(Integer forkNum) {
        ConsoleOutputReportEventListener outputReport = isRedirectTestOutputToFile()
                ? consoleOutputReporter.createListener(resolveReportsDirectory(forkNum), reportNameSuffix, forkNum)
                : consoleOutputReporter.createListener(originalSystemOut, originalSystemErr);
        return consoleOutputReporter.isDisable() ? null : outputReport;
    }

    public synchronized StatisticsReporter getStatisticsReporter() {
        if (statisticsReporter == null) {
            statisticsReporter = requiresRunHistory ? new StatisticsReporter(statisticsFile) : null;
        }
        return statisticsReporter;
    }

    public File getStatisticsFile() {
        return statisticsFile;
    }

    public boolean isTrimStackTrace() {
        return trimStackTrace;
    }

    public boolean isRequiresRunHistory() {
        return requiresRunHistory;
    }

    public String getXsdSchemaLocation() {
        return xsdSchemaLocation;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public boolean isForking() {
        return isForking;
    }

    public boolean isEnableOutErrElements() {
        return enableOutErrElements;
    }

    public boolean isEnablePropertiesElement() {
        return enablePropertiesElement;
    }

    private File resolveReportsDirectory(Integer forkNumber) {
        return forkNumber == null ? reportsDirectory : replaceForkThreadsInPath(reportsDirectory, forkNumber);
    }

    public SurefireStatelessReporter getXmlReporter() {
        return xmlReporter;
    }

    public SurefireConsoleOutputReporter getConsoleOutputReporter() {
        return consoleOutputReporter;
    }

    public SurefireStatelessTestsetInfoReporter getTestsetReporter() {
        return testsetReporter;
    }

    private boolean shouldReportToConsole() {
        return isUseFile() ? isPrintSummary() : isRedirectTestOutputToFile() || isBriefOrPlainFormat();
    }

    public ReporterFactoryOptions getReporterFactoryOptions() {
        return reporterFactoryOptions;
    }
}
