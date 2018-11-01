package org.apache.maven.surefire.its;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * Tests using the JUnit 47 provider to rerun failing tests with the cucumber runner. The main
 * problem that the junit4 provider has with the cucumber runner is that the junit Description
 * instance created by the runner has a null test class attribute. This requires that tests are
 * rerun based on their description.
 *
 * @author mpkorstanje
 */
public class JUnit47RerunFailingTestWithCucumberIT
    extends SurefireJUnit4IntegrationTestCase {

    private SurefireLauncher unpack() {
        return unpack("junit47-rerun-failing-tests-with-cucumber")
            .setJUnitVersion("4.12");
    }

    @Test
    public void testRerunFailingErrorTestsFalse() {
        unpack()
            .maven()
            .addGoal("-Dsurefire.rerunFailingTestsCount=" + 0)
            .withFailure()
            .executeTest()
            .assertTestSuiteResults(1, 0, 1, 0, 0);
    }

    @Test
    public void testRerunFailingErrorTestsWithOneRetry() {
        unpack()
            .maven()
            .addGoal("-Dsurefire.rerunFailingTestsCount=" + 1)
            .withFailure()
            .executeTest()
            .assertTestSuiteResults(1, 0, 1, 0, 0);
    }

    @Test
    public void testRerunFailingErrorTestsTwoRetry() {
        unpack()
            .maven()
            .addGoal("-Dsurefire.rerunFailingTestsCount=" + 2)
            .executeTest()
            .assertTestSuiteResults(1, 0, 0, 0, 2);
    }

}
