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
 * Integration test for #3503: tags must still work with JUnit 4 on the classpath.
 */
public class Surefire3503ExcludedGroupsWithJUnit4IT extends SurefireJUnit4IntegrationTestCase {
    @Test
    void excludedGroupsShouldSkipTaggedTests() {
        OutputValidator outputValidator = unpack("surefire-3503-excluded-groups-junit4")
                .activateProfile("excluded-groups")
                .executeTest()
                .verifyErrorFree(1);

        assertReport(outputValidator, "FastTest", true);
        assertReport(outputValidator, "SlowTest", false);
        assertReport(outputValidator, "TimeOutTest", false);
        assertReport(outputValidator, "StaleConnectionTest", false);
    }

    @Test
    void groupsShouldOnlyRunTaggedTests() {
        OutputValidator outputValidator = unpack("surefire-3503-excluded-groups-junit4")
                .activateProfile("groups")
                .executeTest()
                .verifyErrorFree(2);

        assertReport(outputValidator, "FastTest", false);
        assertReport(outputValidator, "SlowTest", true);
        assertReport(outputValidator, "TimeOutTest", true);
        assertReport(outputValidator, "StaleConnectionTest", false);
    }

    private static void assertReport(OutputValidator outputValidator, String testClass, boolean ran) {
        if (ran) {
            outputValidator
                    .getSurefireReportsXmlFile("TEST-issue3503." + testClass + ".xml")
                    .assertFileExists();
        } else {
            outputValidator
                    .getSurefireReportsXmlFile("TEST-issue3503." + testClass + ".xml")
                    .assertFileNotExists();
        }
    }
}
