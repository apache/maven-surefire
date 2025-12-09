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
package org.apache.maven.plugins.surefire.report;

import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.shared.utils.io.DirectoryScanner;
import org.xml.sax.SAXException;

import static org.apache.maven.shared.utils.StringUtils.split;

public final class SurefireReportParser {
    private static final String INCLUDES = "*.xml";

    private static final String EXCLUDES =
            "*.txt, testng-failed.xml, testng-failures.xml, testng-results.xml, failsafe-summary*.xml";

    private final List<ReportTestSuite> testSuites = new ArrayList<>();

    private final ConsoleLogger consoleLogger;

    private final List<File> reportsDirectories;

    public SurefireReportParser(List<File> reportsDirectories, ConsoleLogger consoleLogger) {
        this.reportsDirectories = reportsDirectories;
        this.consoleLogger = consoleLogger;
    }

    public List<ReportTestSuite> parseXMLReportFiles() {
        final Collection<File> xmlReportFiles = new ArrayList<>();
        for (File reportsDirectory : reportsDirectories) {
            if (reportsDirectory.exists()) {
                for (String xmlReportFile : getIncludedFiles(reportsDirectory, INCLUDES, EXCLUDES)) {
                    xmlReportFiles.add(new File(reportsDirectory, xmlReportFile));
                }
            }
        }
        final TestSuiteXmlParser parser = new TestSuiteXmlParser(consoleLogger);
        for (File aXmlReportFileList : xmlReportFiles) {
            try {
                testSuites.addAll(parser.parse(aXmlReportFileList.getAbsolutePath()));
            } catch (ParserConfigurationException e) {
                throw new RuntimeException("Error setting up parser for JUnit XML report", e);
            } catch (SAXException e) {
                throw new RuntimeException("Error parsing JUnit XML report " + aXmlReportFileList, e);
            } catch (IOException e) {
                throw new RuntimeException("Error reading JUnit XML report " + aXmlReportFileList, e);
            }
        }

        return testSuites;
    }

    public Map<String, Object> getSummary(List<ReportTestSuite> suites) {
        Map<String, Object> totalSummary = new HashMap<>();

        int totalNumberOfTests = 0;

        int totalNumberOfErrors = 0;

        int totalNumberOfFailures = 0;

        int totalNumberOfSkipped = 0;

        float totalElapsedTime = 0.0f;

        for (ReportTestSuite suite : suites) {
            totalNumberOfTests += suite.getNumberOfTests();

            totalNumberOfErrors += suite.getNumberOfErrors();

            totalNumberOfFailures += suite.getNumberOfFailures();

            totalNumberOfSkipped += suite.getNumberOfSkipped();

            totalElapsedTime += suite.getTimeElapsed();
        }

        float totalPercentage =
                computePercentage(totalNumberOfTests, totalNumberOfErrors, totalNumberOfFailures, totalNumberOfSkipped);

        totalSummary.put("totalTests", totalNumberOfTests);

        totalSummary.put("totalErrors", totalNumberOfErrors);

        totalSummary.put("totalFailures", totalNumberOfFailures);

        totalSummary.put("totalSkipped", totalNumberOfSkipped);

        totalSummary.put("totalElapsedTime", totalElapsedTime);

        totalSummary.put("totalPercentage", totalPercentage);

        return totalSummary;
    }

    public Map<String, List<ReportTestSuite>> getSuitesGroupByPackage(List<ReportTestSuite> testSuitesList) {
        Map<String, List<ReportTestSuite>> suitePackage = new HashMap<>();

        for (ReportTestSuite suite : testSuitesList) {
            List<ReportTestSuite> suiteList = new ArrayList<>();

            if (suitePackage.get(suite.getPackageName()) != null) {
                suiteList = suitePackage.get(suite.getPackageName());
            }

            suiteList.add(suite);

            suitePackage.put(suite.getPackageName(), suiteList);
        }

        return suitePackage;
    }

    /**
     * @return a float between 0.0 and 1.0
     */
    public float computePercentage(int tests, int errors, int failures, int skipped) {
        float percentage = tests == 0 ? 0 : ((float) (tests - errors - failures - skipped) / (float) tests);
        return percentage;
    }

    public List<ReportTestCase> getFailureDetails(List<ReportTestSuite> testSuites) {
        List<ReportTestCase> failureDetails = new ArrayList<>();

        for (ReportTestSuite suite : testSuites) {
            for (ReportTestCase tCase : suite.getTestCases()) {
                if (!tCase.isSuccessful()) {
                    failureDetails.add(tCase);
                }
            }
        }

        return failureDetails;
    }

    /**
     * Returns {@code true} if the specified directory contains at least one report file.
     *
     * @param directory the directory
     * @return {@code true} if the specified directory contains at least one report file
     */
    public static boolean hasReportFiles(File directory) {
        return directory != null
                && directory.isDirectory()
                && getIncludedFiles(directory, INCLUDES, EXCLUDES).length != 0;
    }

    private static String[] getIncludedFiles(File directory, String includes, String excludes) {
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(directory);

        scanner.setIncludes(split(includes, ","));

        scanner.setExcludes(split(excludes, ","));

        scanner.scan();

        return scanner.getIncludedFiles();
    }
}
