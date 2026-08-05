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

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Integration test for JUnit 5 {@code @AfterAll} that always fails (#3412).
 * Teardown failures must be reported as errors (not flakes) and must not
 * re-execute already-passing test methods when {@code rerunFailingTestsCount} is set.
 */
public class JUnit5FailingAfterAllIT extends SurefireJUnit4IntegrationTestCase {
    private static final String VERSION = "5.9.1";

    @Test
    public void testAfterAllFailureIsReported() {
        OutputValidator outputValidator = unpack("junit5-failing-after-all", "-norerun")
                .setJUnitVersion(VERSION)
                .maven()
                .withFailure()
                .executeTest();

        outputValidator.verifyTextInLog("AfterAll always fails");
        // 4 completed: PassingTest (1) + AlwaysFailingAfterAllTest (2 methods + executionError)
        // with 1 error — restore pre-3.5.5 behavior
        outputValidator.assertTestSuiteResults(4, 1, 0, 0);

        outputValidator
                .getSurefireReportsXmlFile("TEST-junit5.PassingTest.xml")
                .assertContainsText("tests=\"1\" errors=\"0\"");

        outputValidator
                .getSurefireReportsXmlFile("TEST-junit5.AlwaysFailingAfterAllTest.xml")
                .assertContainsText("name=\"executionError\"")
                .assertContainsText("errors=\"1\"");
    }

    @Test
    public void testAfterAllFailureWithRerunDoesNotRerunPassingTests() throws VerificationException {
        OutputValidator outputValidator = unpack("junit5-failing-after-all", "-rerun")
                .setJUnitVersion(VERSION)
                .maven()
                .addGoal("-Dsurefire.rerunFailingTestsCount=2")
                .withFailure()
                .executeTest();

        outputValidator
                .getSurefireReportsXmlFile("TEST-junit5.AlwaysFailingAfterAllTest.xml")
                .assertContainsText("name=\"executionError\"")
                .assertContainsText("errors=\"1\"");

        // Passing methods must not be re-executed solely because teardown failed
        outputValidator.assertThatLogLine(containsString("testOne passed"), is(1));
        outputValidator.assertThatLogLine(containsString("testTwo passed"), is(1));
    }
}
