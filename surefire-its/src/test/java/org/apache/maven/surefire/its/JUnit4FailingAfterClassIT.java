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

        outputValidator.verifyTextInLog("AfterClass always fails");

        outputValidator
                .getSurefireReportsXmlFile("TEST-junit4.PassingTest.xml")
                .assertContainsText("tests=\"1\" errors=\"0\"");

        outputValidator
                .getSurefireReportsXmlFile("TEST-junit4.AlwaysFailingAfterClassTest.xml")
                .assertContainsText("name=\"initializationError\"")
                .assertContainsText("errors=\"0\"")
                .assertContainsText("flakes=\"1\"");
    }

    @Test
    public void testAfterClassFailureWithRerun() throws VerificationException {
        OutputValidator outputValidator = unpack("junit4-failing-after-class", "-rerun")
                .setJUnitVersion(VERSION)
                .maven()
                .addGoal("-Dsurefire.rerunFailingTestsCount=2")
                .executeTest();

        outputValidator
                .getSurefireReportsXmlFile("TEST-junit4.AlwaysFailingAfterClassTest.xml")
                .assertContainsText("name=\"initializationError\"")
                .assertContainsText("errors=\"0\"")
                .assertContainsText("flakes=\"1\"");

        outputValidator.assertThatLogLine(containsString("testOne passed"), is(3));
        outputValidator.assertThatLogLine(containsString("testTwo passed"), is(3));
    }
}
