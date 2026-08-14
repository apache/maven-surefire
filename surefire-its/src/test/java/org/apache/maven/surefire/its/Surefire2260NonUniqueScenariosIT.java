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

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.jupiter.api.Test;

/**
 * Integration test for SUREFIRE-2260 / GitHub issue #2670.
 */
public class Surefire2260NonUniqueScenariosIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void identicallyNamedCucumberScenariosRemainDistinctDuringRerun() throws VerificationException {
        OutputValidator validator = unpack("surefire-2260-non-unique-scenarios")
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextNotInLog("Element name cannot be empty");

        validator.assertTestSuiteResults(3, 0, 1, 1, 0);
    }
}
