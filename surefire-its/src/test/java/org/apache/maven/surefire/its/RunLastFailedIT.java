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
import org.apache.maven.surefire.its.fixture.VerificationException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;

/**
 * Tests {@code runLastFailed}: the second Maven run only runs what failed or didn't finish in the first one.
 * <br>
 * The project has ATest to ETest, run in that order in one fork. With {@code -DfailMe=true}, BTest#fails,
 * one BTest#squares run and the nested CTest$Inner#innerFails fail. With {@code -Dcrash=true}, DCrashTest
 * kills the fork, so ETest never runs.
 */
public class RunLastFailedIT extends SurefireJUnit4IntegrationTestCase {

    @Test
    public void runsOnlyTheTestsThatFailed() throws VerificationException {
        SurefireLauncher launcher = unpack("/run-last-failed");

        launcher.setLogFileName("log-first.txt")
                .addGoal("-DfailMe=true")
                .addGoal("-Dmaven.test.failure.ignore=true")
                .executeTest()
                .assertTestSuiteResults(11, 0, 3, 0);

        launcher.reset();
        OutputValidator validator = launcher.setLogFileName("log-last-failed.txt")
                .addGoal("-Dsurefire.runLastFailed=true")
                .maven()
                .skipClean()
                .executeTest()
                .verifyTextInLog("Running only what went wrong last time: 3 failed test(s).");
        // BTest#fails, the 3 BTest#squares invocations and CTest$Inner#innerFails
        assertSummary(validator, 5);
        validator.getSurefireReportsXmlFile("TEST-lastfailed.BTest.xml").assertContainsText("tests=\"4\"");
        // ATest didn't run again, its report is still the first one
        validator.getSurefireReportsXmlFile("TEST-lastfailed.ATest.xml").assertFileExists();

        launcher.reset();
        launcher.setLogFileName("log-nothing-left.txt")
                .addGoal("-Dsurefire.runLastFailed=true")
                .maven()
                .skipClean()
                .executeTest()
                .verifyTextInLog("No failed or unfinished tests found in")
                .verifyTextNotInLog("Tests run:");
    }

    @Test
    public void runsAllTestsWithoutPreviousReports() throws VerificationException {
        OutputValidator validator = unpack("/run-last-failed")
                .addGoal("-Dsurefire.runLastFailed=true")
                .executeTest()
                .verifyTextInLog("No reports from a previous run in");
        assertSummary(validator, 11);
    }

    @Test
    public void staysInsideTheTestParameter() throws VerificationException {
        SurefireLauncher launcher = unpack("/run-last-failed");

        launcher.setLogFileName("log-first.txt")
                .addGoal("-DfailMe=true")
                .addGoal("-Dmaven.test.failure.ignore=true")
                .executeTest()
                .assertTestSuiteResults(11, 0, 3, 0);

        launcher.reset();
        OutputValidator validator = launcher.setLogFileName("log-last-failed.txt")
                .addGoal("-Dsurefire.runLastFailed=true")
                .setTestToRun("BTest")
                .maven()
                .skipClean()
                .executeTest()
                .verifyTextInLog("Running only what went wrong last time: 2 failed test(s).");
        // BTest#fails and the 3 BTest#squares invocations, not CTest
        assertSummary(validator, 4);
    }

    @Test
    public void runsTheClassesThatDidNotFinishAfterACrash() throws VerificationException {
        SurefireLauncher launcher = unpack("/run-last-failed");

        launcher.setLogFileName("log-crash.txt")
                .addGoal("-DfailMe=true")
                .addGoal("-Dcrash=true")
                .maven()
                .failNever()
                .executeTest()
                .verifyTextInLog("The forked VM terminated without properly saying goodbye")
                .getTargetFile(".surefire-reports-run-in-progress")
                .assertFileExists();

        launcher.reset();
        OutputValidator validator = launcher.setLogFileName("log-last-failed.txt")
                .addGoal("-Dsurefire.runLastFailed=true")
                .maven()
                .skipClean()
                .executeTest()
                .verifyTextInLog("Running only what went wrong last time: 3 failed test(s), "
                        + "2 class(es) that didn't finish.");
        // the 5 failed tests of BTest and CTest, then DCrashTest and ETest
        assertSummary(validator, 7);
        validator.getSurefireReportsXmlFile("TEST-lastfailed.ETest.xml").assertFileExists();
        validator.getTargetFile(".surefire-reports-run-in-progress").assertFileNotExists();
    }

    @Test
    public void failsafeRunsOnlyTheTestsThatFailed() throws VerificationException {
        SurefireLauncher launcher = unpack("/run-last-failed");

        launcher.setLogFileName("log-first.txt")
                .addGoal("-DfailMe=true")
                .addGoal("-Dmaven.test.failure.ignore=true")
                .executeVerify()
                .assertIntegrationTestSuiteResults(2, 0, 1, 0);

        launcher.reset();
        // failures aren't ignored now, so a passing verify means the old summary was replaced
        launcher.setLogFileName("log-last-failed.txt")
                .addGoal("-Dsurefire.runLastFailed=true")
                .addGoal("-Dfailsafe.runLastFailed=true")
                .maven()
                .skipClean()
                .executeVerify()
                .verifyTextInLog("Running only what went wrong last time: 1 failed test(s).")
                .getTargetFile("failsafe-reports/TEST-lastfailed.FooIT.xml")
                .assertContainsText("tests=\"1\"");
    }

    /**
     * Checks the final "Tests run" line, not the per class ones.
     */
    private static void assertSummary(OutputValidator validator, int total) throws VerificationException {
        validator.assertThatLogLine(endsWith("Tests run: " + total + ", Failures: 0, Errors: 0, Skipped: 0"), is(1));
    }
}
