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
 * Integration test verifying that a JUnit Platform {@code TestExecutionListener} configured through the
 * Surefire {@code listener} property is instantiated and invoked.
 */
public class JUnit5TestExecutionListenerIT extends SurefireJUnit4IntegrationTestCase {

    @Test
    public void testJUnit5TestExecutionListener() {
        OutputValidator outputValidator = unpack("junit5-testexecutionlistener")
                .setJUnitVersion("5.14.4")
                .sysProp("junit.platform.version", "1.14.4")
                .executeTest()
                .verifyErrorFreeLog();

        outputValidator.assertTestSuiteResults(1, 0, 0, 0);
        outputValidator.getTargetFile("testexecutionlistener-output.txt").assertFileExists();
    }
}
