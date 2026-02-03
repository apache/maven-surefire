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
package org.apache.maven.surefire.its.jiras;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.hamcrest.Matchers;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Integration Tests for SUREFIRE-2065
 */
@SuppressWarnings("checkstyle:magicnumber")
public class Surefire2065IT4 extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void shouldNotDetectFlakyTestsWhenCombiningJunit4And5Tests() throws VerificationException {
        OutputValidator validator = unpack("surefire-2065-common")
                .mavenTestFailureIgnore(true)
                .executeTest()
                .assertTestSuiteResults(8, 0, 4, 0, 0);

        assertJunit4(validator);
        assertJunit5(validator);
    }

    @Test
    public void shouldNotDetectFlakyTestsWhenRunningOnlyJunit4() throws VerificationException {
        OutputValidator validator = unpack("surefire-2065-junit4")
                .mavenTestFailureIgnore(true)
                .executeTest()
                .assertTestSuiteResults(4, 0, 2, 0, 0);

        assertJunit4(validator);
    }

    @Test
    public void shouldNotDetectFlakyTestsWhenRunningOnlyJunit5() throws VerificationException {
        OutputValidator validator = unpack("surefire-2065-junit5")
                .mavenTestFailureIgnore(true)
                .executeTest()
                .assertTestSuiteResults(4, 0, 2, 0, 0);

        assertJunit5(validator);
    }

    private static void assertJunit4(OutputValidator validator) {
        validator
                .getSurefireReportsFile("TEST-pkg.junit4.ParameterizedTest.xml", UTF_8)
                .assertContainsText(Matchers.matchesPattern("^ *<testcase name=\"notFlaky\\[0]\".*/>$"))
                .assertContainsText(Matchers.matchesPattern("^ *<testcase name=\"notFlaky\\[1]\".*[^/]>$"))
                .assertContainsText("<failure message=")
                .assertContainsText("<rerunFailure message=")
                .assertNotContainsText("<flakyFailure message=");

        validator
                .getSurefireReportsFile("TEST-pkg.junit4.ParameterizedWithDisplayNameTest.xml", UTF_8)
                .assertContainsText(Matchers.matchesPattern("^ *<testcase name=\"notFlaky\\[value=0]\".*/>$"))
                .assertContainsText(Matchers.matchesPattern("^ *<testcase name=\"notFlaky\\[value=1]\".*[^/]>$"))
                .assertContainsText("<failure message=")
                .assertContainsText("<rerunFailure message=")
                .assertNotContainsText("<flakyFailure message=");
    }

    private static void assertJunit5(OutputValidator validator) {
        validator
                .getSurefireReportsFile("TEST-pkg.junit5.ParameterizedTest.xml", UTF_8)
                .assertContainsText(Matchers.matchesPattern("^ *<testcase name=\"notFlaky\\(int\\)\\[1]\".*/>$"))
                .assertContainsText(Matchers.matchesPattern("^ *<testcase name=\"notFlaky\\(int\\)\\[2]\".*[^/]>$"))
                .assertContainsText("<failure message=")
                .assertContainsText("<rerunFailure message=")
                .assertNotContainsText("<flakyFailure message=");

        validator
                .getSurefireReportsFile("TEST-pkg.junit5.ParameterizedWithDisplayNameTest.xml", UTF_8)
                .assertContainsText(Matchers.matchesPattern("^ *<testcase name=\"notFlaky\\(int\\)\\[1]\".*/>$"))
                .assertContainsText(Matchers.matchesPattern("^ *<testcase name=\"notFlaky\\(int\\)\\[2]\".*[^/]>$"))
                .assertContainsText("<failure message=")
                .assertContainsText("<rerunFailure message=")
                .assertNotContainsText("<flakyFailure message=");
    }
}
