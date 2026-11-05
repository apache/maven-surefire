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

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.jupiter.api.Test;

/**
 * A module with no tests to run should not need a test framework just because it
 * inherits groups from a parent. Modules that do have tests are still checked.
 * See #2795.
 */
public class Surefire2795GroupsWithoutTestsIT extends SurefireJUnit4IntegrationTestCase {

    @Test
    void shouldSkipGroupsCheckWhenThereAreNoTestsToRun() {
        unpack("surefire-2795-groups-no-tests").executeTest().verifyErrorFreeLog();
    }

    @Test
    void shouldStillFailWhenTestsExistWithoutATestFramework() {
        unpack("surefire-2795-groups-test-without-framework")
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog("groups/excludedGroups are specified but no test framework "
                        + "supporting groups was found on the test classpath");
    }
}
