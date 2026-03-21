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
import org.junit.jupiter.api.Test;

/**
 * Integration test for JUnit 5 container-level failures (e.g. @BeforeAll).
 * Verifies that such failures generate a valid valid testcase named
 * "initializationError" in the XML report.
 */
public class JUnit5BeforeAllContainerFailureIT extends SurefireJUnit4IntegrationTestCase {

    @Test
    public void testBeforeAllContainerFailure() {
        OutputValidator outputValidator = unpack("junit5-beforeall-container-failure")
                .setJUnitVersion("5.9.1")
                .maven()
                .debugLogging()
                .withFailure()
                .executeTest();

        // One test fails at the container level: meaning tests run = 1, errors = 1, failures = 0, skipped = 0
        outputValidator.assertTestSuiteResults(1, 0, 1, 0);

        outputValidator
                .getSurefireReportsXmlFile("TEST-junitplatform.AlwaysFailingBeforeAllTest.xml")
                .assertContainsText("tests=\"1\" errors=\"1\" skipped=\"0\" failures=\"0\"")
                .assertContainsText(
                        "<testcase name=\"initializationError\" classname=\"junitplatform.AlwaysFailingBeforeAllTest\" time=")
                .assertContainsText("<error message=\"BeforeAll always fails\" type=\"java.lang.RuntimeException\">")
                .assertContainsText("<![CDATA[java.lang.RuntimeException: BeforeAll always fails")
                .assertContainsText(
                        "at junitplatform.AlwaysFailingBeforeAllTest.setup(AlwaysFailingBeforeAllTest.java:10)");
    }
}
