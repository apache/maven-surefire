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
 * Checks that groups and excludedGroups still filter Jupiter tests when TestNG is on the classpath,
 * and that TestNG still filters its own tests.
 */
public class GroupsWithTestNgOnClasspathIT extends SurefireJUnit4IntegrationTestCase {

    @Test
    void groupsOnlyRunTaggedJupiterTestsWhenTestNgIsOnTheClasspath() {
        OutputValidator outputValidator = unpack("groups-with-testng-on-classpath")
                .setGroups("base")
                .executeTest()
                .verifyTextInLog("TestNG is present")
                .verifyErrorFree(1);

        assertReport(outputValidator, "jupiteronly.BaseTest", true);
        assertReport(outputValidator, "jupiteronly.OtherTest", false);
    }

    @Test
    void excludedGroupsSkipTaggedJupiterTestsWhenTestNgIsOnTheClasspath() {
        OutputValidator outputValidator = unpack("groups-with-testng-on-classpath")
                .setExcludedGroups("base")
                .executeTest()
                .verifyTextInLog("TestNG is present")
                .verifyErrorFree(1);

        assertReport(outputValidator, "jupiteronly.BaseTest", false);
        assertReport(outputValidator, "jupiteronly.OtherTest", true);
    }

    @Test
    void groupsFilterTestNgAndJupiterTests() {
        OutputValidator outputValidator = unpack("groups-with-testng-and-jupiter-tests")
                .setGroups("fast")
                .executeTest()
                .verifyErrorFree(2);

        assertOnlyFastTestsRan(outputValidator);
    }

    @Test
    void excludedGroupsFilterTestNgAndJupiterTests() {
        OutputValidator outputValidator = unpack("groups-with-testng-and-jupiter-tests")
                .setExcludedGroups("slow")
                .executeTest()
                .verifyErrorFree(2);

        assertOnlyFastTestsRan(outputValidator);
    }

    @Test
    void groupsWhichAreNotTagExpressionsOnlyFilterTestNgTests() {
        // (?!slow).* works for TestNG, but JUnit can't parse it as a tag expression
        OutputValidator outputValidator = unpack("groups-with-testng-and-jupiter-tests")
                .activateProfile("testng-regex")
                .executeTest()
                .verifyTextInLog(
                        "These groups are not valid JUnit tag expressions, so they only filter TestNG tests: (?!slow).*")
                .verifyErrorFree(3);

        assertReport(outputValidator, "mixed.FastTestNgTest", true);
        assertReport(outputValidator, "mixed.SlowTestNgTest", false);
        assertReport(outputValidator, "mixed.FastJupiterTest", true);
        assertReport(outputValidator, "mixed.SlowJupiterTest", true);
    }

    private static void assertOnlyFastTestsRan(OutputValidator outputValidator) {
        assertReport(outputValidator, "mixed.FastTestNgTest", true);
        assertReport(outputValidator, "mixed.SlowTestNgTest", false);
        assertReport(outputValidator, "mixed.FastJupiterTest", true);
        assertReport(outputValidator, "mixed.SlowJupiterTest", false);
    }

    private static void assertReport(OutputValidator outputValidator, String testClass, boolean ran) {
        if (ran) {
            outputValidator
                    .getSurefireReportsXmlFile("TEST-" + testClass + ".xml")
                    .assertFileExists();
        } else {
            outputValidator
                    .getSurefireReportsXmlFile("TEST-" + testClass + ".xml")
                    .assertFileNotExists();
        }
    }
}
