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

import java.util.Arrays;
import java.util.Calendar;

import org.apache.maven.it.VerificationException;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * Verifies the runOrder setting and its effect
 *
 * @author Kristian Rosenvold
 */
@SuppressWarnings("checkstyle:magicnumber")
public class RunOrderIT extends SurefireJUnit4IntegrationTestCase {
    private static final String[] TESTS_IN_ALPHABETICAL_ORDER = {"TA", "TB", "TC"};

    private static final String[] TESTS_IN_REVERSE_ALPHABETICAL_ORDER = {"TC", "TB", "TA"};

    // testing random is left as an exercise to the reader. Patches welcome

    @Test
    public void testAlphabeticalJUnit4() throws VerificationException {
        OutputValidator validator = executeWithRunOrder("alphabetical", "junit4");
        assertTestnamesAppearInSpecificOrder(validator, TESTS_IN_ALPHABETICAL_ORDER);
    }

    @Test
    public void testAlphabeticalJUnit5() throws VerificationException {
        OutputValidator validator = executeWithRunOrder("alphabetical", "junit5");
        assertTestnamesAppearInSpecificOrder(validator, TESTS_IN_ALPHABETICAL_ORDER);
    }

    @Test
    public void testRandomJUnit4DifferentSeed() throws VerificationException {
        long seed = 0L;
        OutputValidator validator = executeWithRandomOrder("junit4", seed);
        String[] expected = validator.getStringsOrderInLog(TESTS_IN_ALPHABETICAL_ORDER);
        for (long i = seed; i < 5 + seed; i++) {
            OutputValidator validator2 = executeWithRandomOrder("junit4", i);
            String[] observed = validator2.getStringsOrderInLog(TESTS_IN_ALPHABETICAL_ORDER);
            if (!Arrays.equals(expected, observed)) {
                return;
            }
        }
        throw new VerificationException("All random orders with the different seeds produced the same orders");
    }

    @Test
    public void testRandomJUnit4SameSeed() throws VerificationException {
        long seed = 0L;
        OutputValidator validator = executeWithRandomOrder("junit4", seed);
        String[] expected = validator.getStringsOrderInLog(TESTS_IN_ALPHABETICAL_ORDER);
        for (long i = 0; i < 5; i++) {
            OutputValidator validator2 = executeWithRandomOrder("junit4", seed);
            String[] observed = validator2.getStringsOrderInLog(TESTS_IN_ALPHABETICAL_ORDER);
            if (!Arrays.equals(expected, observed)) {
                throw new VerificationException("Random orders with the same seed produced different orders");
            }
        }
    }

    @Test
    public void testRandomJUnit4PrintSeedWithGivenSeed() {
        OutputValidator validator = executeWithRandomOrder("junit4", 0L);
        validator.verifyTextInLog("To reproduce ordering use flag");
    }

    @Test
    public void testRandomJUnit4PrintSeedWithNoGivenSeed() {
        OutputValidator validator = executeWithRandomOrder("junit4");
        validator.verifyTextInLog("To reproduce ordering use flag");
    }

    @Test
    public void testReverseAlphabeticalJUnit4() throws VerificationException {
        OutputValidator validator = executeWithRunOrder("reversealphabetical", "junit4");
        assertTestnamesAppearInSpecificOrder(validator, TESTS_IN_REVERSE_ALPHABETICAL_ORDER);
    }

    @Test
    public void testReverseAlphabeticalJUnit5() throws VerificationException {
        OutputValidator validator = executeWithRunOrder("reversealphabetical", "junit5");
        assertTestnamesAppearInSpecificOrder(validator, TESTS_IN_REVERSE_ALPHABETICAL_ORDER);
    }

    @Test
    public void testHourlyJUnit4() throws VerificationException {
        int startHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        OutputValidator validator = executeWithRunOrder("hourly", "junit4");
        int endHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (startHour != endHour) {
            return; // Race condition, cannot test when hour changed mid-run
        }

        String[] testnames = ((startHour % 2) == 0) ? TESTS_IN_ALPHABETICAL_ORDER : TESTS_IN_REVERSE_ALPHABETICAL_ORDER;
        assertTestnamesAppearInSpecificOrder(validator, testnames);
    }

    @Test
    public void testHourlyJUnit5() throws VerificationException {
        int startHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        OutputValidator validator = executeWithRunOrder("hourly", "junit5");
        int endHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (startHour != endHour) {
            return; // Race condition, cannot test when hour changed mid-run
        }

        String[] testnames = ((startHour % 2) == 0) ? TESTS_IN_ALPHABETICAL_ORDER : TESTS_IN_REVERSE_ALPHABETICAL_ORDER;
        assertTestnamesAppearInSpecificOrder(validator, testnames);
    }

    @Test
    public void testNonExistingRunOrderJUnit4() {
        unpack().activateProfile("junit4")
                .forkCount(1)
                .reuseForks(reuseForks())
                .runOrder("nonExistingRunOrder")
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog("There's no RunOrder with the name nonExistingRunOrder.");
    }

    @Test
    public void testNonExistingRunOrderJUnit5() {
        unpack().activateProfile("junit5")
                .forkCount(1)
                .reuseForks(reuseForks())
                .runOrder("nonExistingRunOrder")
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog("There's no RunOrder with the name nonExistingRunOrder.");
    }

    private OutputValidator executeWithRunOrder(String runOrder, String profile) {
        return unpack().activateProfile(profile)
                .forkCount(1)
                .reuseForks(reuseForks())
                .runOrder(runOrder)
                .executeTest()
                .verifyErrorFree(3);
    }

    private OutputValidator executeWithRandomOrder(String profile) {
        return unpack().activateProfile(profile)
                .forkCount(1)
                .reuseForks(reuseForks())
                .runOrder("random")
                .executeTest()
                .verifyErrorFree(3);
    }

    private OutputValidator executeWithRandomOrder(String profile, long seed) {
        return unpack().activateProfile(profile)
                .forkCount(1)
                .reuseForks(reuseForks())
                .runOrder("random")
                .runOrderRandomSeed(String.valueOf(seed))
                .executeTest()
                .verifyErrorFree(3);
    }

    protected boolean reuseForks() {
        return true;
    }

    private SurefireLauncher unpack() {
        return unpack("runOrder");
    }

    private void assertTestnamesAppearInSpecificOrder(OutputValidator validator, String[] testnames)
            throws VerificationException {
        if (!validator.stringsAppearInSpecificOrderInLog(testnames)) {
            throw new VerificationException("Response does not contain expected item");
        }
    }
}
