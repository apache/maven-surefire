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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

/**
 * Integration tests for JUnit Platform @BeforeAll failures with rerun functionality.
 * Tests various scenarios where @BeforeAll lifecycle methods fail and are rerun.
 */
public class JUnitPlatformFailingBeforeAllRerunIT extends SurefireJUnit4IntegrationTestCase {
    private static final String VERSION = "5.9.1";

    private static final String TEST_PROJECT_BASE = "junit-platform-rerun-failing-before-all";

    @Test
    public void testBeforeAllFailures() {
        // Test that @BeforeAll failures are properly handled when they succeed on rerun
        OutputValidator outputValidator = unpack(TEST_PROJECT_BASE)
                .setJUnitVersion(VERSION)
                .maven()
                .debugLogging()
                .addGoal("-Dsurefire.rerunFailingTestsCount=3")
                .withFailure()
                .executeTest()
                .assertTestSuiteResults(7, 1, 0, 0, 4);

        // Verify the @BeforeAll is reported as a flake with proper formatting
        outputValidator.verifyTextInLog("junitplatform.FlakyFirstTimeTest.<beforeAll>");
        outputValidator.verifyTextInLog("Run 1: FlakyFirstTimeTest.setup:53 IllegalArgument");
        outputValidator.verifyTextInLog("Run 2: PASS");

        // Verify XML report doesn't contain error testcase with empty name
        outputValidator
                .getSurefireReportsXmlFile("TEST-junitplatform.FlakyFirstTimeTest.xml")
                .assertContainsText("tests=\"4\" errors=\"0\"")
                .assertContainsText("name=\"testFailingTestOne\"")
                .assertContainsText("name=\"testErrorTestOne\"")
                .assertContainsText("name=\"testPassingTest\"");

        // Verify @BeforeAll is reported as error
        outputValidator.verifyTextInLog("Errors:");
        outputValidator.verifyTextInLog("junitplatform.AlwaysFailingTest.<beforeAll>");
        outputValidator.verifyTextInLog("Run 3: AlwaysFailingTest.setup:15 IllegalArgument BeforeAll always fails");
    }
}
