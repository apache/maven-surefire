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

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.surefire.its.fixture.SurefireJUnitIntegrationTestCase;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Test https://issues.apache.org/jira/browse/SUREFIRE-2095
 *
 */
public class Surefire2095FailsafeJvmCrashShouldNotBeIgnoredIT extends SurefireJUnitIntegrationTestCase {
    @Test
    public void mavenShouldFail() throws VerificationException {
        // Run failsafe with testFailureIgnore=true and an unknown JVM option that will cause a crash
        unpack("surefire-2095-failsafe-jvm-crash")
                .maven()
                .withFailure()
                .debugLogging()
                .executeVerify()
                .assertThatLogLine(containsString("BUILD SUCCESS"), is(0))
                .verifyTextInLog("BUILD FAILURE");
    }
}
