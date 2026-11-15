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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.surefire.lastfailed.LastFailedTests.Selection;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class LastFailedTestsResolverTest {
    private static final TestListResolver NO_FILTER = TestListResolver.getEmptyTestListResolver();

    @TempDir
    Path tempDir;

    private File reportsDirectory;

    private RunInProgressMarker marker;

    @BeforeEach
    void setUp() {
        reportsDirectory = tempDir.resolve("surefire-reports").toFile();
        assertThat(reportsDirectory.mkdirs()).isTrue();
        marker = RunInProgressMarker.of(reportsDirectory);
    }

    @Test
    void runsAllWithoutPreviousReports() {
        LastFailedTests result = resolve(singletonList("pkg.FooTest"), NO_FILTER);

        assertThat(result.getSelection()).isEqualTo(Selection.RUN_ALL);
    }

    @Test
    void runsAllWhenTheReportsDirectoryIsMissing() {
        File missing = new File(reportsDirectory, "missing");
        LastFailedTests result = new LastFailedTestsResolver(
                        missing, RunInProgressMarker.of(missing), null, false, false, new NullConsoleLogger())
                .resolve(singletonList("pkg.FooTest"), NO_FILTER);

        assertThat(result.getSelection()).isEqualTo(Selection.RUN_ALL);
    }

    @Test
    void picksFailuresAndErrors() throws IOException {
        report("pkg.FooTest", passed("pkg.FooTest", "a"), failed("pkg.FooTest", "b"), error("pkg.FooTest", "c"));
        report("pkg.BarTest", passed("pkg.BarTest", "a"));

        LastFailedTests result = resolve(asList("pkg.FooTest", "pkg.BarTest"), NO_FILTER);

        assertThat(result.getSelection()).isEqualTo(Selection.SOME);
        assertThat(result.getTests()).isEqualTo("pkg.FooTest#b+c");
        assertThat(result.getFailedMethods()).isEqualTo(2);
        assertThat(result.getFailedClasses()).isZero();
        assertThat(result.getUnfinishedClasses()).isZero();

        TestListResolver filter = new TestListResolver(result.getTests());
        assertThat(filter.shouldRun("pkg/FooTest.class", "b")).isTrue();
        assertThat(filter.shouldRun("pkg/FooTest.class", "c")).isTrue();
        assertThat(filter.shouldRun("pkg/FooTest.class", "a")).isFalse();
        assertThat(filter.shouldRun("pkg/BarTest.class", null)).isFalse();
    }

    @Test
    void leavesOutFlakyAndSkippedTests() throws IOException {
        report("pkg.FooTest", flaky("pkg.FooTest", "a"), skipped("pkg.FooTest", "b"), passed("pkg.FooTest", "c"));

        LastFailedTests result = resolve(singletonList("pkg.FooTest"), NO_FILTER);

        assertThat(result.getSelection()).isEqualTo(Selection.NOTHING);
    }

    @Test
    void trimsParameterizedNames() throws IOException {
        report(
                "pkg.FooTest",
                failed("pkg.FooTest", "square(int, int)[1]"),
                failed("pkg.FooTest", "square(int, int)[2]"));
        report("pkg.BarTest", failed("pkg.BarTest", "test[0]"));

        LastFailedTests result = resolve(asList("pkg.FooTest", "pkg.BarTest"), NO_FILTER);

        assertThat(result.getTests()).isEqualTo("pkg.BarTest#test,pkg.FooTest#square");
        assertThat(result.getFailedMethods()).isEqualTo(2);
    }

    @Test
    void runsTheWholeClassWhenItFailedOutsideATest() throws IOException {
        report("pkg.FooTest", failed("pkg.FooTest", "b"), error("pkg.FooTest", "initializationError"));
        report("pkg.BarTest", error("pkg.BarTest", "executionError"));

        LastFailedTests result = resolve(asList("pkg.FooTest", "pkg.BarTest"), NO_FILTER);

        assertThat(result.getTests()).isEqualTo("pkg.BarTest,pkg.FooTest");
        assertThat(result.getFailedClasses()).isEqualTo(2);
        assertThat(result.getFailedMethods()).isZero();
    }

    @Test
    void runsTheWholeClassWhenTheNameIsNotAJavaMethod() throws IOException {
        report("pkg.FooTest", failed("pkg.FooTest", "should add a, b and c"));

        LastFailedTests result = resolve(singletonList("pkg.FooTest"), NO_FILTER);

        assertThat(result.getTests()).isEqualTo("pkg.FooTest");
        assertThat(result.getFailedClasses()).isEqualTo(1);
    }

    @Test
    void keepsTheBinaryNameOfNestedClasses() throws IOException {
        report("pkg.FooTest", failed("pkg.FooTest$Inner", "m"));

        LastFailedTests result = resolve(singletonList("pkg.FooTest"), NO_FILTER);

        assertThat(result.getTests()).isEqualTo("pkg.FooTest$Inner#m");
        assertThat(new TestListResolver(result.getTests()).shouldRun("pkg/FooTest$Inner.class", "m"))
                .isTrue();
    }

    @Test
    void removesTheReportNameSuffix() throws IOException {
        report("pkg.FooTest-jdk17", failed("pkg.FooTest(jdk17)", "b"));

        LastFailedTests result = new LastFailedTestsResolver(
                        reportsDirectory, marker, "jdk17", false, false, new NullConsoleLogger())
                .resolve(singletonList("pkg.FooTest"), NO_FILTER);

        assertThat(result.getTests()).isEqualTo("pkg.FooTest#b");
    }

    @Test
    void ignoresClassesThatAreNotPartOfThisRun() throws IOException {
        report("pkg.GoneTest", failed("pkg.GoneTest", "a"));
        report("pkg.FooTest", passed("pkg.FooTest", "a"));

        LastFailedTests result = resolve(singletonList("pkg.FooTest"), NO_FILTER);

        assertThat(result.getSelection()).isEqualTo(Selection.NOTHING);
    }

    @Test
    void staysInsideTheMethodsOfTheTestParameter() throws IOException {
        report("pkg.FooTest", failed("pkg.FooTest", "a"), failed("pkg.FooTest", "b"));

        LastFailedTests result = resolve(singletonList("pkg.FooTest"), new TestListResolver("FooTest#a"));

        assertThat(result.getTests()).isEqualTo("pkg.FooTest#a");
    }

    @Test
    void keepsTheExcludesOfTheTestParameter() throws IOException {
        report("pkg.FooTest", failed("pkg.FooTest", "a"), failed("pkg.FooTest", "slow"));

        LastFailedTests result =
                resolve(singletonList("pkg.FooTest"), new TestListResolver("Foo*, !FooTest#slow, !FooSlowTest"));

        assertThat(result.getTests()).isEqualTo("pkg.FooTest#a,!FooTest#slow,!FooSlowTest");
        TestListResolver filter = new TestListResolver(result.getTests());
        assertThat(filter.shouldRun("pkg/FooTest.class", "a")).isTrue();
        assertThat(filter.shouldRun("pkg/FooSlowTest.class", null)).isFalse();
    }

    @Test
    void runsClassesThatDidNotFinishWhenThePreviousRunStoppedEarly() throws IOException {
        marker.write(asList("pkg.ATest", "pkg.BTest", "pkg.CTest"));
        long runStart = System.currentTimeMillis() - 60_000;
        assertThat(marker.getFile().setLastModified(runStart)).isTrue();
        // ATest finished before the run stopped
        setLastModified(report("pkg.ATest", passed("pkg.ATest", "a")), runStart + 10_000);
        // BTest only has an older report, from an earlier run
        setLastModified(report("pkg.BTest", failed("pkg.BTest", "b")), runStart - 10_000);
        // CTest has no report at all

        LastFailedTests result = resolve(asList("pkg.ATest", "pkg.BTest", "pkg.CTest"), NO_FILTER);

        assertThat(result.getTests()).isEqualTo("pkg.BTest,pkg.CTest");
        assertThat(result.getUnfinishedClasses()).isEqualTo(2);
        assertThat(result.getFailedMethods()).isZero();
    }

    @Test
    void ignoresClassesWithoutReportsWhenThePreviousRunFinished() throws IOException {
        // e.g. an abstract base class named *Test never gets a report
        report("pkg.ATest", passed("pkg.ATest", "a"));

        LastFailedTests result = resolve(asList("pkg.ATest", "pkg.AbstractBaseTest"), NO_FILTER);

        assertThat(result.getSelection()).isEqualTo(Selection.NOTHING);
    }

    @Test
    void runsEverythingTheStoppedRunWantedWhenNoReportWasWritten() throws IOException {
        marker.write(asList("pkg.ATest", "pkg.BTest"));

        LastFailedTests result = resolve(asList("pkg.ATest", "pkg.BTest", "pkg.NewTest"), NO_FILTER);

        assertThat(result.getTests()).isEqualTo("pkg.ATest,pkg.BTest");
    }

    @Test
    void narrowsUnfinishedClassesToTheMethodsOfTheTestParameter() throws IOException {
        marker.write(singletonList("pkg.FooTest"));

        LastFailedTests result = resolve(singletonList("pkg.FooTest"), new TestListResolver("FooTest#a+b"));

        assertThat(result.getTests()).isEqualTo("pkg.FooTest#a,pkg.FooTest#b");
        TestListResolver filter = new TestListResolver(result.getTests());
        assertThat(filter.shouldRun("pkg/FooTest.class", "a")).isTrue();
        assertThat(filter.shouldRun("pkg/FooTest.class", "c")).isFalse();
    }

    @Test
    void runsTheWholeUnfinishedClassWhenTheTestParameterUsesARegex() throws IOException {
        marker.write(singletonList("pkg.FooTest"));

        LastFailedTests result = resolve(singletonList("pkg.FooTest"), new TestListResolver("%regex[.*FooTest.*#a.*]"));

        assertThat(result.getTests()).isEqualTo("pkg.FooTest");
    }

    @Test
    void skipsReportsThatCantBeRead() throws IOException {
        Files.write(new File(reportsDirectory, "TEST-pkg.BrokenTest.xml").toPath(), "<testsuite".getBytes(UTF_8));
        report("pkg.FooTest", failed("pkg.FooTest", "b"));

        LastFailedTests result = resolve(asList("pkg.FooTest", "pkg.BrokenTest"), NO_FILTER);

        assertThat(result.getTests()).isEqualTo("pkg.FooTest#b");
    }

    @Test
    void runsAllWhenReportsUseDisplayNamesForClasses() throws IOException {
        report("pkg.FooTest", failed("Foo display name", "b"));

        LastFailedTests result = new LastFailedTestsResolver(
                        reportsDirectory, marker, null, true, false, new NullConsoleLogger())
                .resolve(singletonList("pkg.FooTest"), NO_FILTER);

        assertThat(result.getSelection()).isEqualTo(Selection.RUN_ALL);
    }

    @Test
    void runsWholeClassesWhenReportsUseDisplayNamesForMethods() throws IOException {
        report("pkg.FooTest", failed("pkg.FooTest", "adds two numbers"));

        LastFailedTests result = new LastFailedTestsResolver(
                        reportsDirectory, marker, null, false, true, new NullConsoleLogger())
                .resolve(singletonList("pkg.FooTest"), NO_FILTER);

        assertThat(result.getTests()).isEqualTo("pkg.FooTest");
    }

    @Test
    void readsTheMethodName() {
        assertThat(LastFailedTestsResolver.toMethodName("test", "pkg.FooTest")).isEqualTo("test");
        assertThat(LastFailedTestsResolver.toMethodName(" test(String)[1] ", "pkg.FooTest"))
                .isEqualTo("test");
        assertThat(LastFailedTestsResolver.toMethodName("test[0]", "pkg.FooTest"))
                .isEqualTo("test");
        assertThat(LastFailedTestsResolver.toMethodName("FooTest", "pkg.FooTest"))
                .isNull();
        assertThat(LastFailedTestsResolver.toMethodName("pkg.FooTest", "pkg.FooTest"))
                .isNull();
        assertThat(LastFailedTestsResolver.toMethodName("initializationError", "pkg.FooTest"))
                .isNull();
        assertThat(LastFailedTestsResolver.toMethodName("", "pkg.FooTest")).isNull();
        assertThat(LastFailedTestsResolver.toMethodName("1test", "pkg.FooTest")).isNull();
        assertThat(LastFailedTestsResolver.toMethodName("a+b", "pkg.FooTest")).isNull();
    }

    @Test
    void readsTheTopLevelClass() {
        assertThat(LastFailedTestsResolver.topLevelClass("pkg.FooTest")).isEqualTo("pkg.FooTest");
        assertThat(LastFailedTestsResolver.topLevelClass("pkg.FooTest$A$B")).isEqualTo("pkg.FooTest");
    }

    private LastFailedTests resolve(List<String> scannedClasses, TestListResolver userFilter) {
        return new LastFailedTestsResolver(reportsDirectory, marker, null, false, false, new NullConsoleLogger())
                .resolve(scannedClasses, userFilter);
    }

    private File report(String fileName, String... testCases) throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<testsuite name=\"")
                .append(fileName)
                .append("\" time=\"0.1\" tests=\"")
                .append(testCases.length)
                .append("\">\n");
        Arrays.stream(testCases).forEach(xml::append);
        xml.append("</testsuite>\n");
        File report = new File(reportsDirectory, "TEST-" + fileName + ".xml");
        Files.write(report.toPath(), xml.toString().getBytes(UTF_8));
        return report;
    }

    private static void setLastModified(File file, long time) {
        assertThat(file.setLastModified(time)).isTrue();
    }

    private static String passed(String className, String name) {
        return testCase(className, name, "");
    }

    private static String failed(String className, String name) {
        return testCase(className, name, "<failure message=\"boom\" type=\"java.lang.AssertionError\">trace</failure>");
    }

    private static String error(String className, String name) {
        return testCase(className, name, "<error message=\"boom\" type=\"java.lang.RuntimeException\">trace</error>");
    }

    private static String skipped(String className, String name) {
        return testCase(className, name, "<skipped message=\"disabled\"/>");
    }

    private static String flaky(String className, String name) {
        return testCase(
                className,
                name,
                "<flakyFailure message=\"boom\" type=\"java.lang.AssertionError\">"
                        + "<stackTrace>trace</stackTrace></flakyFailure>");
    }

    private static String testCase(String className, String name, String body) {
        return "  <testcase name=\"" + name + "\" classname=\"" + className + "\" time=\"0.01\">" + body
                + "</testcase>\n";
    }
}
