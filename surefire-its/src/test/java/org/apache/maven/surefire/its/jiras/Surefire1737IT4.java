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

import java.io.File;

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Integration Tests for SUREFIRE-1737
 */
public class Surefire1737IT4 extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void selectJUnit5UsingConfiguredProviderWithPlatformRunner() {
        SurefireLauncher launcher = unpack("surefire-1737");
        launcher.setForkJvm(false)
                .forkNever()
                .executeTest()
                .verifyTextInLog("Running pkg.JUnit5Test")
                .verifyTextInLog(
                        "Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider");

        File baseDir = launcher.getUnpackedAt();
        assertTrue(new File(baseDir, "target/surefire-reports/pkg.JUnit5Test.txt").exists());
        // xml report should be not generated
        File xmlReport = new File(baseDir, "target/surefire-reports/TEST-pkg.JUnit5Test.xml");
        assertFalse("xml report: " + xmlReport + " should not be generated", xmlReport.exists());
    }
}
