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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.jupiter.api.Test;

/**
 * Integration test for #3468: groups and excludedGroups with Spock, Jupiter and JUnit 4 tests together.
 */
public class Surefire3468SpockGroupsIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    void excludedGroupsShouldSkipSlowTestsOfEveryEngine() {
        OutputValidator outputValidator = unpack("surefire-3468-spock-groups")
                .activateProfile("excluded-groups")
                .executeTest()
                .verifyErrorFree(3);

        outputValidator
                .getSurefireReportsXmlFile("TEST-issue3468.CalculatorSpec.xml")
                .assertContainsText("name=\"addition works\"")
                .assertNotContainsText("name=\"slow addition works\"");
        outputValidator
                .getSurefireReportsXmlFile("TEST-issue3468.JupiterTest.xml")
                .assertContainsText("name=\"fast\"")
                .assertNotContainsText("name=\"slow\"");
        outputValidator
                .getSurefireReportsXmlFile("TEST-issue3468.JUnit4Test.xml")
                .assertContainsText("name=\"fast\"")
                .assertNotContainsText("name=\"slow\"");
    }

    @Test
    void groupsShouldOnlyRunSlowTestsOfEveryEngine() {
        OutputValidator outputValidator = unpack("surefire-3468-spock-groups")
                .activateProfile("groups")
                .executeTest()
                .verifyErrorFree(3);

        outputValidator
                .getSurefireReportsXmlFile("TEST-issue3468.CalculatorSpec.xml")
                .assertContainsText("name=\"slow addition works\"")
                .assertNotContainsText("name=\"addition works\"");
        outputValidator
                .getSurefireReportsXmlFile("TEST-issue3468.JupiterTest.xml")
                .assertContainsText("name=\"slow\"")
                .assertNotContainsText("name=\"fast\"");
        outputValidator
                .getSurefireReportsXmlFile("TEST-issue3468.JUnit4Test.xml")
                .assertContainsText("name=\"slow\"")
                .assertNotContainsText("name=\"fast\"");
    }
}
