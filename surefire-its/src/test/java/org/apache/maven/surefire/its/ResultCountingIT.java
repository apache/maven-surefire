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
import org.apache.maven.surefire.its.fixture.SurefireJUnitIntegrationTestCase;
import org.junit.Test;

/**
 * Verifies that the providers get the result summary at the bottom of the run correctly, in different forkmodes
 * SUREFIRE-613 Asserts proper test counts when running in parallel
 *
 * @author Kristian Rosenvold
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ResultCountingIT extends SurefireJUnitIntegrationTestCase {
    @Test
    public void testCountingWithJunit481ForkNever() {
        assertForkCount(0, true);
    }

    @Test
    public void testCountingWithJunit481ForkOnce() {
        assertForkCount(1, true);
    }

    @Test
    public void testCountingWithJunit481ForkAlways() {
        assertForkCount(1, false);
    }

    private void assertForkCount(int forkCount, boolean reuseForks) {
        OutputValidator outputValidator = unpack("result-counting")
                .failNever()
                .forkCount(forkCount)
                .reuseForks(reuseForks)
                .executeTest();
        outputValidator.assertTestSuiteResults(36, 23, 4, 2);
        outputValidator.verifyTextInLog("Tests run: 36, Failures: 4, Errors: 23, Skipped: 2");
    }
}
