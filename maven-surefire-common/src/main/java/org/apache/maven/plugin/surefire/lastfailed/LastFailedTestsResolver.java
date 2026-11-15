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
package org.apache.maven.plugin.surefire.lastfailed;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.TestSuiteXmlParser;
import org.apache.maven.surefire.api.testset.ResolvedTest;
import org.apache.maven.surefire.api.testset.TestListResolver;

import static java.util.Collections.singletonList;
import static org.apache.maven.surefire.api.testset.TestListResolver.toClassFileName;
import static org.apache.maven.surefire.shared.lang3.StringUtils.isBlank;

/**
 * Finds the tests to run again for {@code runLastFailed}, by reading the XML reports of the last run.
 * <br>
 * It picks the test methods that failed, and the classes that didn't finish because the last run stopped
 * early. The result uses the {@code -Dtest} syntax and never goes beyond what the user asked for.
 */
public final class LastFailedTestsResolver {
    private static final String REPORT_PREFIX = "TEST-";

    private static final String REPORT_EXTENSION = ".xml";

    private final File reportsDirectory;

    private final RunInProgressMarker marker;

    private final String reportNameSuffix;

    private final boolean phrasedClassNames;

    private final boolean phrasedMethodNames;

    private final ConsoleLogger logger;

    /**
     * @param reportsDirectory where the last run wrote its {@code TEST-*.xml} files
     * @param marker tells us if the last run finished
     * @param reportNameSuffix the {@code reportNameSuffix} parameter, added to class names in the reports
     * @param phrasedClassNames the reports show display names instead of class names
     * @param phrasedMethodNames the reports show display names instead of method names
     * @param logger where to log
     */
    public LastFailedTestsResolver(
            File reportsDirectory,
            RunInProgressMarker marker,
            String reportNameSuffix,
            boolean phrasedClassNames,
            boolean phrasedMethodNames,
            ConsoleLogger logger) {
        this.reportsDirectory = reportsDirectory;
        this.marker = marker;
        this.reportNameSuffix = reportNameSuffix;
        this.phrasedClassNames = phrasedClassNames;
        this.phrasedMethodNames = phrasedMethodNames;
        this.logger = logger;
    }

    /**
     * @param scannedClasses the test classes this run would normally run
     * @param userFilter what the user asked for: {@code -Dtest}, includes and excludes
     * @return the tests to run
     */
    public LastFailedTests resolve(Collection<String> scannedClasses, TestListResolver userFilter) {
        if (phrasedClassNames) {
            logger.warning("runLastFailed can't map display names in the XML reports back to test classes"
                    + " (usePhrasedTestCaseClassName is on), running all tests.");
            return LastFailedTests.runAll();
        }

        List<ReportTestSuite> suites = readReports();
        Set<String> unfinishedRunClasses = null;
        long unfinishedRunStart = 0;
        if (marker.exists()) {
            try {
                unfinishedRunStart = marker.getStartTime();
                unfinishedRunClasses = marker.readTestClasses();
            } catch (IOException e) {
                logger.warning("Could not read " + marker.getFile() + ": " + e.getMessage());
            }
        }

        if (suites.isEmpty() && unfinishedRunClasses == null) {
            logger.info("No reports from a previous run in " + reportsDirectory + ", running all tests.");
            return LastFailedTests.runAll();
        }

        Set<String> scannedTopLevelClasses = new HashSet<>();
        for (String scannedClass : scannedClasses) {
            scannedTopLevelClasses.add(topLevelClass(scannedClass));
        }

        // sorted, so the result is always the same
        Map<String, Set<String>> failedMethods = new TreeMap<>();
        Set<String> failedClasses = new TreeSet<>();
        for (ReportTestSuite suite : suites) {
            for (ReportTestCase testCase : suite.getTestCases()) {
                if (!testCase.hasFailure() && !testCase.hasError()) {
                    continue;
                }
                String className = toClassName(testCase.getFullClassName());
                if (className == null || !scannedTopLevelClasses.contains(topLevelClass(className))) {
                    // the class is gone, or this run doesn't include it
                    logger.debug("runLastFailed ignores " + testCase.getFullName() + ", not a test class of this run");
                    continue;
                }
                String classFile = toClassFileName(className);
                String methodName = phrasedMethodNames ? null : toMethodName(testCase.getName(), className);
                if (methodName == null) {
                    if (userFilter.shouldRun(classFile, null)) {
                        failedClasses.add(className);
                    }
                } else if (userFilter.shouldRun(classFile, methodName)) {
                    failedMethods
                            .computeIfAbsent(className, c -> new TreeSet<>())
                            .add(methodName);
                }
            }
        }

        Set<String> unfinishedClasses = new TreeSet<>();
        if (unfinishedRunClasses != null) {
            Set<String> finishedTopLevelClasses = new HashSet<>();
            for (ReportTestSuite suite : suites) {
                String className = toClassName(suite.getFullClassName());
                if (className != null && suite.getLastModified() >= unfinishedRunStart) {
                    finishedTopLevelClasses.add(topLevelClass(className));
                }
            }
            for (String className : unfinishedRunClasses) {
                String topLevelClass = topLevelClass(className);
                if (scannedTopLevelClasses.contains(topLevelClass)
                        && !finishedTopLevelClasses.contains(topLevelClass)
                        && userFilter.shouldRun(toClassFileName(className), null)) {
                    unfinishedClasses.add(className);
                }
            }
            unfinishedClasses.removeAll(failedClasses);
        }

        Set<String> wholeClasses = new TreeSet<>(failedClasses);
        wholeClasses.addAll(unfinishedClasses);

        List<String> tests = new ArrayList<>();
        for (String className : wholeClasses) {
            tests.addAll(wholeClassPatterns(className, userFilter));
        }
        int failedMethodCount = 0;
        for (Map.Entry<String, Set<String>> entry : failedMethods.entrySet()) {
            // no need to list the failed methods of a class we run whole
            if (!wholeClasses.contains(entry.getKey())) {
                tests.add(entry.getKey() + "#" + String.join("+", entry.getValue()));
                failedMethodCount += entry.getValue().size();
            }
        }

        if (tests.isEmpty()) {
            return LastFailedTests.nothing();
        }

        // keep the user's excludes
        for (ResolvedTest excluded : userFilter.getExcludedPatterns()) {
            String pattern = excluded.toString();
            if (!pattern.isEmpty()) {
                tests.add("!" + pattern);
            }
        }

        return LastFailedTests.some(
                String.join(",", tests), failedMethodCount, failedClasses.size(), unfinishedClasses.size());
    }

    /**
     * Stays inside the user's filter: with {@code -Dtest=FooTest#a} we run {@code FooTest#a},
     * not the whole {@code FooTest}.
     */
    private static List<String> wholeClassPatterns(String className, TestListResolver userFilter) {
        String classFile = toClassFileName(className);
        List<String> patterns = new ArrayList<>();
        for (ResolvedTest included : userFilter.getIncludedPatterns()) {
            if (included.matchAsInclusive(classFile, null)) {
                if (!included.hasTestMethodPattern() || included.isRegexTestMethodPattern()) {
                    // we can't mix a regex with our class name, so run the whole class
                    return singletonList(className);
                }
                patterns.add(className + "#" + included.getTestMethodPattern());
            }
        }
        return patterns.isEmpty() ? singletonList(className) : patterns;
    }

    private List<ReportTestSuite> readReports() {
        File[] reports = reportsDirectory.listFiles(
                (dir, name) -> name.startsWith(REPORT_PREFIX) && name.endsWith(REPORT_EXTENSION));
        if (reports == null) {
            return Collections.emptyList();
        }
        List<ReportTestSuite> suites = new ArrayList<>();
        for (File report : reports) {
            try {
                suites.addAll(new TestSuiteXmlParser(logger).parse(report.getAbsolutePath()));
            } catch (Exception e) {
                // e.g. a report cut short when the run was killed
                logger.warning("runLastFailed skips the report " + report + ", it can't be read: " + e.getMessage());
            }
        }
        return suites;
    }

    /**
     * Removes the {@code (reportNameSuffix)} we add to class names in the reports.
     */
    private String toClassName(String reportClassName) {
        if (isBlank(reportClassName)) {
            return null;
        }
        String className = reportClassName.trim();
        if (!isBlank(reportNameSuffix)) {
            String suffix = "(" + reportNameSuffix + ")";
            if (className.endsWith(suffix)) {
                className = className.substring(0, className.length() - suffix.length());
            }
        }
        return className.isEmpty() ? null : className;
    }

    /**
     * Gets the Java method name from a test name in the report, e.g. {@code square} from
     * {@code square(int, int)[1]}.
     *
     * @return the method name, or {@code null} when we should run the whole class instead
     */
    static String toMethodName(String testName, String className) {
        if (isBlank(testName)) {
            return null;
        }
        String name = testName.trim();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '(' || c == '[') {
                name = name.substring(0, i).trim();
                break;
            }
        }
        if (name.isEmpty()
                || name.equals("initializationError")
                || name.equals("executionError")
                || name.equals(className)
                || name.equals(simpleClassName(className))
                || !Character.isJavaIdentifierStart(name.charAt(0))) {
            return null;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return null;
            }
        }
        return name;
    }

    private static String simpleClassName(String className) {
        int dot = className.lastIndexOf('.');
        String simpleName = dot == -1 ? className : className.substring(dot + 1);
        int dollar = simpleName.lastIndexOf('$');
        return dollar == -1 ? simpleName : simpleName.substring(dollar + 1);
    }

    /**
     * Returns {@code a.b.Outer} for {@code a.b.Outer$Inner}. The scanner only lists outer classes.
     */
    static String topLevelClass(String className) {
        int dollar = className.indexOf('$');
        return dollar == -1 ? className : className.substring(0, dollar);
    }
}
