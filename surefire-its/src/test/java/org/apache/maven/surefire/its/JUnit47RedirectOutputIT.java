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
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class JUnit47RedirectOutputIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void testPrintSummaryTrueWithRedirect() {
        final OutputValidator clean =
                unpack().redirectToFile(true).addGoal("clean").executeTest();
        checkReports(clean);
    }

    @Test
    public void testClassesParallel() {
        final OutputValidator clean =
                unpack().redirectToFile(true).parallelClasses().addGoal("clean").executeTest();
        checkReports(clean);
    }

    private void checkReports(OutputValidator validator) {
        String report = validator
                .getSurefireReportsFile("junit47ConsoleOutput.Test1-output.txt")
                .readFileToString();
        assertTrue(report.isEmpty());
        String report2 = validator
                .getSurefireReportsFile("junit47ConsoleOutput.Test2-output.txt")
                .readFileToString();
        assertTrue(report2.isEmpty());
        assertFalse(validator
                .getSurefireReportsFile("junit47ConsoleOutput.Test3-output.txt")
                .exists());
    }

    private SurefireLauncher unpack() {
        return unpack("/junit47-redirect-output");
    }
}
