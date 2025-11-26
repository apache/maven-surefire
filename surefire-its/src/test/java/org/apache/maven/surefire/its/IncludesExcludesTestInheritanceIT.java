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
package org.apache.maven.surefire.its;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class IncludesExcludesTestInheritanceIT extends SurefireJUnit4IntegrationTestCase {

    @Parameter
    @SuppressWarnings("checkstyle:visibilitymodifier")
    public String provider;

    @Parameters(name = "Provider: {0}")
    public static List<String> providers() {
        return Arrays.asList("JUnit", "TestNG");
    }

    private final String[] allTests = new String[] {
        "OtherClass#testIncluded",
        "OtherClass#testNotIncluded",
        "SubClass#testInSubClass",
        "SuperClass#testInSuperClass"
    };

    private final String[] subClassTests = new String[] {"SubClass#testInSubClass", "SuperClass#testInSuperClass"};

    @Test
    public void testInclusionOfInheritedTest() throws VerificationException {
        // includesFile:
        // SubClass
        verifyOnlyExpectedTestsExecuted(unpack().activateProfile("profile1"), subClassTests);

        // same with -Dtest option
        verifyOnlyExpectedTestsExecuted(unpack().addGoal("-Dtest=SubClass"), subClassTests);
    }

    @Test
    public void testInclusionOfInheritedTestWhenMethodFilterUsed() throws VerificationException {
        // includesFile:
        // SubClass
        // OtherClass#testIncluded
        verifyOnlyExpectedTestsExecuted(
                unpack().activateProfile("profile2"),
                "OtherClass#testIncluded",
                "SubClass#testInSubClass",
                "SuperClass#testInSuperClass");
    }

    @Test
    public void testInclusionOfInheritedTestOnly() throws VerificationException {
        // includesFile:
        // SubClass#testInSuperClass
        verifyOnlyExpectedTestsExecuted(unpack().activateProfile("profile3"), "SuperClass#testInSuperClass");

        // same with -Dtest option
        verifyOnlyExpectedTestsExecuted(
                unpack().addGoal("-Dtest=SubClass#testInSuperClass"), "SuperClass#testInSuperClass");
    }

    @Test
    public void testExclusionOfInheritedTest() throws VerificationException {
        // includesFile:
        // SubClass
        // excludesFile:
        // SubClass#testInSuperClass
        verifyOnlyExpectedTestsExecuted(unpack().activateProfile("profile4"), "SubClass#testInSubClass");
    }

    @Test
    public void testIncorrectExclusionOfInheritedTest() throws VerificationException {
        // includesFile:
        // SubClass
        // excludesFile:
        // SuperClass
        verifyOnlyExpectedTestsExecuted(unpack().activateProfile("profile5"), subClassTests);

        // same with <includes> and <excludes>
        verifyOnlyExpectedTestsExecuted(unpack().activateProfile("profile6"), subClassTests);
    }

    @Test
    public void testIncorrectExclusionOfInheritedTestWithMethodFilter() throws VerificationException {
        // includesFile:
        // SubClass
        // excludesFile:
        // SuperClass#testInSuperClass
        verifyOnlyExpectedTestsExecuted(unpack().activateProfile("profile7"), subClassTests);
    }

    private void verifyOnlyExpectedTestsExecuted(SurefireLauncher launcher, String... expectedTests)
            throws VerificationException {
        verifyOnlyExpectedTestsExecuted(launcher, new HashSet<>(Arrays.asList(expectedTests)));
    }

    private void verifyOnlyExpectedTestsExecuted(SurefireLauncher launcher, Set<String> expectedTests)
            throws VerificationException {
        String module = provider.toLowerCase();
        OutputValidator outputValidator = launcher.addGoal("-pl=" + module).executeTest();
        for (String test : allTests) {
            if (expectedTests.contains(test)) {
                assertThatTestWasExecuted(outputValidator, test);
            } else {
                assertThatTestWasNotExecuted(outputValidator, test);
            }
        }
        launcher.getSubProjectValidator(module).verifyErrorFree(expectedTests.size());
    }

    private void assertThatTestWasExecuted(OutputValidator outputValidator, String test) throws VerificationException {
        assertEquals(
                String.format("Test %s was not executed with %s provider.", test, provider),
                1,
                testExecutionLogOccurrences(outputValidator, test));
    }

    private void assertThatTestWasNotExecuted(OutputValidator outputValidator, String test)
            throws VerificationException {
        assertEquals(
                String.format("Test %s was executed with %s provider.", test, provider),
                0,
                testExecutionLogOccurrences(outputValidator, test));
    }

    private int testExecutionLogOccurrences(OutputValidator outputValidator, String test) throws VerificationException {
        return outputValidator
                .loadLogLines(containsString(testExecutionLog(test)))
                .size();
    }

    private String testExecutionLog(String test) {
        return String.format("%s: %s executed", provider, test);
    }

    private SurefireLauncher unpack() {
        return unpack("/includes-excludes-test-inheritance");
    }
}
