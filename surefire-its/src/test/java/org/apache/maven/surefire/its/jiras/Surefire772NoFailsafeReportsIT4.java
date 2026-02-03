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
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test Surefire-740 Truncated comma with non us locale
 *
 * @author Kristian Rosenvold
 */
public class Surefire772NoFailsafeReportsIT4 extends SurefireJUnit4IntegrationTestCase {

    @Test
    public void testReportGeneration() throws Exception {
        final OutputValidator site =
                unpack().addFailsafeReportOnlyGoal().addSurefireReportOnlyGoal().executeCurrentGoals();

        assertSurefireReportPresent(site);
        assertNoFailsafeReport(site);
    }

    @Test
    public void testSkippedFailsafeReportGeneration() throws Exception {
        final OutputValidator validator = unpack().activateProfile("skipFailsafe")
                .addFailsafeReportOnlyGoal()
                .addSurefireReportOnlyGoal()
                .executeCurrentGoals();
        assertSurefireReportPresent(validator);
        assertNoFailsafeReport(validator);
    }

    @Test
    public void testForcedFailsafeReportGeneration() throws Exception {
        final OutputValidator validator = unpack().activateProfile("forceFailsafe")
                .addFailsafeReportOnlyGoal()
                .addSurefireReportOnlyGoal()
                .executeCurrentGoals();
        assertSurefireReportPresent(validator);
        assertFailsafeReport(validator);
    }

    @Test
    public void testSkipForcedFailsafeReportGeneration() throws Exception {
        final OutputValidator validator = unpack().activateProfile("forceFailsafe")
                .activateProfile("skipFailsafe")
                .addFailsafeReportOnlyGoal()
                .addSurefireReportOnlyGoal()
                .executeCurrentGoals();

        assertSurefireReportPresent(validator);
        assertNoFailsafeReport(validator);
    }

    private void assertNoFailsafeReport(OutputValidator validator) {
        TestFile reportFile = validator.getReportsFile("failsafe.html");
        assertFalse("Expecting no failsafe report file", reportFile.isFile());
    }

    private void assertFailsafeReport(OutputValidator validator) {
        TestFile reportFile = validator.getReportsFile("failsafe.html");
        assertTrue("Expecting no failsafe report file", reportFile.isFile());
    }

    private void assertSurefireReportPresent(OutputValidator validator) {
        TestFile reportFile = validator.getReportsFile("surefire.html");
        assertTrue("Expecting surefire report file", reportFile.isFile());
    }

    private SurefireLauncher unpack() throws VerificationException {
        final SurefireLauncher unpack = unpack("surefire-772-no-failsafe-reports");
        unpack.maven().deleteReportsDir().skipClean().failNever().verifyFileNotPresent("reports");
        return unpack;
    }
}
