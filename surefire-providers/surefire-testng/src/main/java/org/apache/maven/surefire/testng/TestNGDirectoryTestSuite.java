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
package org.apache.maven.surefire.testng;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.surefire.api.cli.CommandLineOption;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.TestsToRun;

import static java.util.Collections.singleton;
import static org.apache.maven.surefire.shared.utils.StringUtils.isBlank;
import static org.apache.maven.surefire.testng.TestNGExecutor.run;

/**
 * Test suite for TestNG based on a directory of Java test classes. Can also execute JUnit tests.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
final class TestNGDirectoryTestSuite extends TestSuite {
    private final Map<String, String> options;

    private final Map<String, String> junitOptions;

    private final String testSourceDirectory;

    private final File reportsDirectory;

    private final TestListResolver methodFilter;

    private final Class<?> junitTestClass;

    private final Class<? extends Annotation> junitRunWithAnnotation;

    private final Class<? extends Annotation> junitTestAnnotation;

    private final List<CommandLineOption> mainCliOptions;

    private final int skipAfterFailureCount;

    TestNGDirectoryTestSuite(
            String testSourceDirectory,
            Map<String, String> confOptions,
            File reportsDirectory,
            TestListResolver methodFilter,
            List<CommandLineOption> mainCliOptions,
            int skipAfterFailureCount) {
        this.options = confOptions;
        this.testSourceDirectory = testSourceDirectory;
        this.reportsDirectory = reportsDirectory;
        this.methodFilter = methodFilter;
        this.junitTestClass = findJUnitTestClass();
        this.junitRunWithAnnotation = findJUnitRunWithAnnotation();
        this.junitTestAnnotation = findJUnitTestAnnotation();
        this.junitOptions = createJUnitOptions();
        this.mainCliOptions = mainCliOptions;
        this.skipAfterFailureCount = skipAfterFailureCount;
    }

    void execute(TestsToRun testsToRun, TestNGReporter testNGReporter) throws TestSetFailedException {
        if (!testsToRun.allowEagerReading()) {
            executeLazy(testsToRun, testNGReporter);
        } else if (testsToRun.containsAtLeast(2)) {
            executeMulti(testsToRun, testNGReporter);
        } else if (testsToRun.containsAtLeast(1)) {
            Class<?> testClass = testsToRun.iterator().next();
            executeSingleClass(testNGReporter, testClass);
        }
    }

    private void executeSingleClass(TestNGReporter testNGReporter, Class<?> testClass) throws TestSetFailedException {
        options.put("suitename", testClass.getName());

        startTestSuite(testNGReporter.getRunListener());

        Map<String, String> optionsToUse = isJUnitTest(testClass) ? junitOptions : options;

        run(
                singleton(testClass),
                testSourceDirectory,
                optionsToUse,
                testNGReporter,
                reportsDirectory,
                methodFilter,
                mainCliOptions,
                skipAfterFailureCount);

        finishTestSuite(testNGReporter.getRunListener());
    }

    private void executeLazy(TestsToRun testsToRun, TestNGReporter testNGReporter) throws TestSetFailedException {
        for (Class<?> testToRun : testsToRun) {
            executeSingleClass(testNGReporter, testToRun);
        }
    }

    private static Class<?> findJUnitTestClass() {
        return lookupClass("junit.framework.Test");
    }

    private static Class<Annotation> findJUnitRunWithAnnotation() {
        return lookupAnnotation("org.junit.runner.RunWith");
    }

    private static Class<Annotation> findJUnitTestAnnotation() {
        return lookupAnnotation("org.junit.Test");
    }

    @SuppressWarnings("unchecked")
    private static Class<Annotation> lookupAnnotation(String className) {
        try {
            return (Class<Annotation>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Class<?> lookupClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private void executeMulti(TestsToRun testsToRun, TestNGReporter testNGReporter) throws TestSetFailedException {
        List<Class<?>> testNgTestClasses = new ArrayList<>();
        List<Class<?>> junitTestClasses = new ArrayList<>();
        for (Class<?> testToRun : testsToRun) {
            if (isJUnitTest(testToRun)) {
                junitTestClasses.add(testToRun);
            } else {
                testNgTestClasses.add(testToRun);
            }
        }

        File testNgReportsDirectory = reportsDirectory, junitReportsDirectory = reportsDirectory;

        if (!junitTestClasses.isEmpty() && !testNgTestClasses.isEmpty()) {
            testNgReportsDirectory = new File(reportsDirectory, "testng-native-results");
            junitReportsDirectory = new File(reportsDirectory, "testng-junit-results");
        }
        startTestSuite(testNGReporter.getRunListener());

        run(
                testNgTestClasses,
                testSourceDirectory,
                options,
                testNGReporter,
                testNgReportsDirectory,
                methodFilter,
                mainCliOptions,
                skipAfterFailureCount);

        if (!junitTestClasses.isEmpty()) {
            run(
                    junitTestClasses,
                    testSourceDirectory,
                    junitOptions,
                    testNGReporter,
                    junitReportsDirectory,
                    methodFilter,
                    mainCliOptions,
                    skipAfterFailureCount);
        }

        finishTestSuite(testNGReporter.getRunListener());
    }

    private boolean isJUnitTest(Class<?> c) {
        return isJunit3Test(c) || isJunit4Test(c);
    }

    private boolean isJunit4Test(Class<?> c) {
        return hasJunit4RunWithAnnotation(c) || hasJunit4TestAnnotation(c);
    }

    private boolean hasJunit4RunWithAnnotation(Class<?> c) {
        return junitRunWithAnnotation != null && c.getAnnotation(junitRunWithAnnotation) != null;
    }

    private boolean hasJunit4TestAnnotation(Class<?> c) {
        if (junitTestAnnotation != null) {
            for (Method m : c.getMethods()) {
                if (m.getAnnotation(junitTestAnnotation) != null) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isJunit3Test(Class<?> c) {
        return junitTestClass != null && junitTestClass.isAssignableFrom(c);
    }

    private Map<String, String> createJUnitOptions() {
        Map<String, String> junitOptions = new HashMap<>(options);
        String onlyJUnit = options.get("junit");
        if (isBlank(onlyJUnit)) {
            onlyJUnit = "true";
        }
        junitOptions.put("junit", onlyJUnit);
        return junitOptions;
    }

    @Override
    Map<String, String> getOptions() {
        return options;
    }
}
