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
 * Integration test for JUnit 4 @AfterClass that always fails.
 * Verifies that surefire properly reports errors when class-level teardown throws.
 *
 * <p>Current behavior on master is broken:
 * <ul>
 *   <li>The error is counted as a flake instead of an error in the XML summary</li>
 *   <li>tests count is inflated (3 instead of 2)</li>
 * </ul>
 *
 * <p>Once fixed, the XML should report errors="1".
 */
public class JUnit4FailingAfterClassIT extends SurefireJUnit4IntegrationTestCase {
    private static final String VERSION = "4.13.2";

    @Test
    public void testAfterClassFailureIsReported() {
        OutputValidator outputValidator = unpack("junit4-failing-after-class", "-norerun")
                .setJUnitVersion(VERSION)
                .maven()
                .withFailure()
                .executeTest();

        // The @AfterClass failure should be reported in the log
        outputValidator.verifyTextInLog("AfterClass always fails");

        // The passing test class should still be error-free
        outputValidator
                .getSurefireReportsXmlFile("TEST-junit4.PassingTest.xml")
                .assertContainsText("tests=\"1\" errors=\"0\"");

        // BUG: The XML summary says errors="0" despite the @AfterClass error being present as a testcase.
        //      The error is misclassified as a flake.
        //      After fix, this should assert errors="1" and flakes="0".
        outputValidator
                .getSurefireReportsXmlFile("TEST-junit4.AlwaysFailingAfterClassTest.xml")
                .assertContainsText("errors=\"0\"")
                .assertContainsText("flakes=\"1\"");

        // The @AfterClass error testcase is named "initializationError".
        outputValidator
                .getSurefireReportsXmlFile("TEST-junit4.AlwaysFailingAfterClassTest.xml")
                .assertContainsText("name=\"initializationError\"");
    }

    @Test
    public void testAfterClassFailureWithRerun() throws VerificationException {
        // BUG: With reruns, the build actually PASSES because the @AfterClass error is
        //      misclassified as a flake. This is the scenario reported in PR #3329:
        //      the error is swallowed and the build appears green.
        //      After fix, the build should fail because the @AfterClass always errors.
        OutputValidator outputValidator = unpack("junit4-failing-after-class", "-rerun")
                .setJUnitVersion(VERSION)
                .maven()
                .addGoal("-Dsurefire.rerunFailingTestsCount=2")
                .executeTest();

        // The error is misclassified as a flake in the XML
        outputValidator
                .getSurefireReportsXmlFile("TEST-junit4.AlwaysFailingAfterClassTest.xml")
                .assertContainsText("errors=\"0\"")
                .assertContainsText("flakes=\"1\"");

        // BUG: The passing test methods get re-executed 3 times (rerunFailingTestsCount + 1)
        //      because the @AfterClass error triggers a class-level rerun.
        //      After fix, passing test methods should not be rerun due to a teardown failure.
        outputValidator.assertThatLogLine(containsString("testOne passed"), is(3));
        outputValidator.assertThatLogLine(containsString("testTwo passed"), is(3));
    }
}
