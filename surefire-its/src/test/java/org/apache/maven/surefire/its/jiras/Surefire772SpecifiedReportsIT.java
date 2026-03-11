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
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test Surefire-740 Truncated comma with non us locale
 *
 * @author Kristian Rosenvold
 */
public class Surefire772SpecifiedReportsIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void testReportGeneration() {
        OutputValidator validator =
                unpack().addFailsafeReportOnlyGoal().addSurefireReportOnlyGoal().executeCurrentGoals();

        TestFile reportFile = validator.getReportsFile("surefire.html");
        assertTrue(reportFile.isFile(), "Expecting surefire report file");

        reportFile = validator.getReportsFile("failsafe.html");
        assertTrue(reportFile.isFile(), "Expecting failsafe report file");
    }

    @Test
    public void testSkippedFailsafeReportGeneration() {
        OutputValidator validator = unpack().activateProfile("skipFailsafe")
                .addFailsafeReportOnlyGoal()
                .addSurefireReportOnlyGoal()
                .executeCurrentGoals();

        TestFile reportFile = validator.getReportsFile("surefire.html");
        assertTrue(reportFile.isFile(), "Expecting surefire report file");

        reportFile = validator.getReportsFile("failsafe.html");
        assertFalse(reportFile.isFile(), "Expecting no failsafe report file");
    }

    @Test
    public void testSkippedSurefireReportGeneration() {
        OutputValidator validator = unpack().activateProfile("skipSurefire")
                .addFailsafeReportOnlyGoal()
                .addSurefireReportOnlyGoal()
                .executeCurrentGoals();

        TestFile reportFile = validator.getReportsFile("surefire.html");
        assertFalse(reportFile.isFile(), "Expecting no surefire report file");

        reportFile = validator.getReportsFile("failsafe.html");
        assertTrue(reportFile.isFile(), "Expecting failsafe report file");
    }

    public SurefireLauncher unpack() {
        SurefireLauncher unpack = unpack("/surefire-772-specified-reports");
        unpack.maven().deleteReportsDir().skipClean().failNever();
        return unpack;
    }
}
