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
 * Picks runs of a JUnit 4 parameterized test by their index, e.g. {@code -Dtest=IndexedTest#test[1]}.
 * This worked with the old JUnit 4 provider and stopped working once JUnit 4 tests ran on the Vintage engine.
 *
 * @see <a href="https://github.com/apache/maven-surefire/issues/2808">GitHub issue #2808</a>
 */
public class Surefire2808ParameterizedIndexSelectionIT extends SurefireJUnit4IntegrationTestCase {

    private static final String PROJECT = "surefire-2808-parameterized-index-selection";

    @Test
    void oneRunByIndex() throws Exception {
        OutputValidator validator = unpack(PROJECT)
                .setTestToRun("IndexedTest#test[1]")
                .executeTest()
                .verifyErrorFree(1);
        validator.verifyTextInLog("IndexedTest.test value=1");
        validator.verifyTextNotInLog("IndexedTest.test value=0");
        validator.verifyTextNotInLog("IndexedTest.anotherTest");
    }

    @Test
    void allRunsWithWildcardIndex() throws Exception {
        OutputValidator validator = unpack(PROJECT)
                .setTestToRun("IndexedTest#test[*]")
                .executeTest()
                .verifyErrorFree(3);
        validator.verifyTextNotInLog("IndexedTest.anotherTest");
    }

    @Test
    void allRunsByMethodName() {
        unpack(PROJECT).setTestToRun("IndexedTest#test").executeTest().verifyErrorFree(3);
    }

    @Test
    void oneRunWithCustomName() throws Exception {
        OutputValidator validator = unpack(PROJECT)
                .setTestToRun("NamedIndexTest#square[2:*]")
                .executeTest()
                .verifyErrorFree(1);
        validator.verifyTextInLog("NamedIndexTest.square input=3");
    }

    @Test
    void excludeOneRun() throws Exception {
        OutputValidator validator = unpack(PROJECT)
                .setTestToRun("IndexedTest, !IndexedTest#test[1]")
                .executeTest()
                .verifyErrorFree(5);
        validator.verifyTextNotInLog("IndexedTest.test value=1");
    }

    @Test
    void oneRunByIndexWithJupiterOnClasspath() throws Exception {
        OutputValidator validator = unpack(PROJECT)
                .activateProfile("jupiter")
                .setTestToRun("IndexedTest#test[1]")
                .executeTest()
                .verifyErrorFree(1);
        validator.verifyTextInLog("IndexedTest.test value=1");
    }
}
