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
package org.apache.maven.surefire.its.fixture;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.PrintStreamLogger;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;

import static java.lang.Double.parseDouble;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 *
 */
@SuppressWarnings({"JavaDoc"})
public final class HelperAssertions {
    private HelperAssertions() {
        throw new IllegalStateException("no instantiable constructor");
    }

    /**
     * assert that the reports in the specified testDir have the right summary statistics
     */
    public static void assertTestSuiteResults(int total, int errors, int failures, int skipped, File testDir) {
        IntegrationTestSuiteResults suite = parseTestResults(testDir);
        assertTestSuiteResults(total, errors, failures, skipped, suite);
    }

    public static void assertTestSuiteResults(
            int total, int errors, int failures, int skipped, int flakes, File testDir) {
        IntegrationTestSuiteResults suite = parseTestResults(testDir);
        assertTestSuiteResults(total, errors, failures, skipped, flakes, suite);
    }

    public static void assertTestSuiteResults(int total, File testDir) {
        IntegrationTestSuiteResults suite = parseTestResults(testDir);
        assertTestSuiteResults(total, suite);
    }

    /**
     * assert that the reports in the specified testDir have the right summary statistics
     */
    public static void assertIntegrationTestSuiteResults(
            int total, int errors, int failures, int skipped, File testDir) {
        IntegrationTestSuiteResults suite = parseIntegrationTestResults(testDir);
        assertTestSuiteResults(total, errors, failures, skipped, suite);
    }

    public static void assertIntegrationTestSuiteResults(int total, File testDir) {
        IntegrationTestSuiteResults suite = parseIntegrationTestResults(testDir);
        assertTestSuiteResults(total, suite);
    }

    public static void assertTestSuiteResults(
            int total, int errors, int failures, int skipped, IntegrationTestSuiteResults actualSuite) {
        assertEquals(total, actualSuite.getTotal(), "wrong number of tests");
        assertEquals(errors, actualSuite.getErrors(), "wrong number of errors");
        assertEquals(failures, actualSuite.getFailures(), "wrong number of failures");
        assertEquals(skipped, actualSuite.getSkipped(), "wrong number of skipped");
    }

    public static void assertTestSuiteResults(int total, IntegrationTestSuiteResults actualSuite) {
        assertEquals(total, actualSuite.getTotal(), "wrong number of tests");
    }

    public static void assertTestSuiteResults(
            int total, int errors, int failures, int skipped, int flakes, IntegrationTestSuiteResults actualSuite) {
        assertTestSuiteResults(total, errors, failures, skipped, actualSuite);
        assertEquals(flakes, actualSuite.getFlakes(), "wrong number of flaky tests");
    }

    public static IntegrationTestSuiteResults parseTestResults(File... testDirs) {
        List<ReportTestSuite> reports = extractReports(testDirs);
        return parseReportList(reports);
    }

    private static IntegrationTestSuiteResults parseIntegrationTestResults(File... testDirs) {
        List<ReportTestSuite> reports = extractITReports(testDirs);
        return parseReportList(reports);
    }

    /**
     * Converts a list of ReportTestSuites into an IntegrationTestSuiteResults object, suitable for summary assertions
     */
    public static IntegrationTestSuiteResults parseReportList(List<ReportTestSuite> reports) {
        assertFalse(reports.isEmpty(), "No reports!");
        int total = 0, errors = 0, failures = 0, skipped = 0, flakes = 0;
        for (ReportTestSuite report : reports) {
            total += report.getNumberOfTests();
            errors += report.getNumberOfErrors();
            failures += report.getNumberOfFailures();
            skipped += report.getNumberOfSkipped();
            flakes += report.getNumberOfFlakes();
        }
        return new IntegrationTestSuiteResults(total, errors, failures, skipped, flakes);
    }

    public static List<ReportTestSuite> extractReports(File... testDirs) {
        List<File> reportsDirs = new ArrayList<>();
        for (File testDir : testDirs) {
            File reportsDir = new File(testDir, "target/surefire-reports");
            assertTrue(reportsDir.exists(), "Reports directory is missing: " + reportsDir.getAbsolutePath());
            reportsDirs.add(reportsDir);
        }
        ConsoleLogger logger = new PrintStreamLogger(System.out);
        SurefireReportParser parser = new SurefireReportParser(reportsDirs, logger);
        try {
            List<ReportTestSuite> suites = parser.parseXMLReportFiles();
            suites.sort(Comparator.comparing(ReportTestSuite::getLastModified));
            return suites;
        } catch (Exception e) {
            throw new RuntimeException("Couldn't parse XML reports", e);
        }
    }

    private static List<ReportTestSuite> extractITReports(File... testDirs) {
        List<File> reportsDirs = new ArrayList<>();
        for (File testDir : testDirs) {
            File reportsDir = new File(testDir, "target/failsafe-reports");
            assertTrue(reportsDir.exists(), "Reports directory is missing: " + reportsDir.getAbsolutePath());
            reportsDirs.add(reportsDir);
        }
        ConsoleLogger logger = new PrintStreamLogger(System.out);
        SurefireReportParser parser = new SurefireReportParser(reportsDirs, logger);
        try {
            return parser.parseXMLReportFiles();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't parse XML reports", e);
        }
    }

    public static void assumeJavaVersion(double expectedVersion) {
        String thisVersion = System.getProperty("java.specification.version");
        assumeTrue(parseDouble(thisVersion) >= expectedVersion, "java.specification.version: " + thisVersion);
    }

    public static void assumeJavaMaxVersion(double expectedMaxVersion) {
        String thisVersion = System.getProperty("java.specification.version");
        assumeTrue(parseDouble(thisVersion) <= expectedMaxVersion, "java.specification.version: " + thisVersion);
    }

    public static void assumeJavaVersionExcluded(double excludedVersion) {
        String thisVersion = System.getProperty("java.specification.version");
        assumeTrue(parseDouble(thisVersion) != excludedVersion, "java.specification.version: " + thisVersion);
    }

    public static String convertUnicodeToUTF8(String unicode) {
        return new String(unicode.getBytes(UTF_8), UTF_8);
    }
}
