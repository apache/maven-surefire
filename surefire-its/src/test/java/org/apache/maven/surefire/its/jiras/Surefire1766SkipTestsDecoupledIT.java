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

import org.apache.maven.surefire.its.fixture.FailsafeOutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies that {@code -DskipTests} only skips Surefire (unit tests) and no longer skips
 * Failsafe (integration tests), while {@code -DskipITs} and {@code -Dmaven.test.skip}
 * continue to control Failsafe execution.
 *
 * @see <a href="https://github.com/apache/maven-surefire/issues/1766">Surefire #1766 / SUREFIRE-823</a>
 */
public class Surefire1766SkipTestsDecoupledIT extends SurefireJUnit4IntegrationTestCase {

    @Test
    public void skipTestsShouldOnlySkipSurefireNotFailsafe() {
        FailsafeOutputValidator validator =
                unpack().sysProp("skipTests", "true").executeVerify();

        validator.verifyErrorFreeLog();
        // Surefire (unit tests) is skipped: the surefire skip log message is emitted
        // and no surefire-reports directory is produced.
        validator.verifyTextInLog("Tests are skipped.");
        assertFalse(
                validator.getSubFile("target/surefire-reports").exists(),
                "surefire-reports should not exist when -DskipTests is used");
        // Failsafe (integration tests) still runs the IT
        validator.assertIntegrationTestSuiteResults(1, 0, 0, 0);
    }

    @Test
    public void skipITsShouldSkipFailsafeOnly() {
        FailsafeOutputValidator validator = unpack().sysProp("skipITs", "true").executeVerify();

        validator.verifyErrorFreeLog();
        // Surefire still runs the unit test
        validator.assertTestSuiteResults(1, 0, 0, 0);
        // Failsafe is skipped: no failsafe-reports directory is produced
        assertFalse(
                validator.getSubFile("target/failsafe-reports").exists(),
                "failsafe-reports should not exist when -DskipITs is used");
    }

    @Test
    public void mavenTestSkipShouldSkipBoth() {
        FailsafeOutputValidator validator =
                unpack().sysProp("maven.test.skip", "true").executeVerify();

        validator.verifyErrorFreeLog();
        assertFalse(
                validator.getSubFile("target/surefire-reports").exists(),
                "surefire-reports should not exist when -Dmaven.test.skip is used");
        assertFalse(
                validator.getSubFile("target/failsafe-reports").exists(),
                "failsafe-reports should not exist when -Dmaven.test.skip is used");
    }

    private SurefireLauncher unpack() {
        return unpack("failsafe-1766-skip-decoupled");
    }
}
