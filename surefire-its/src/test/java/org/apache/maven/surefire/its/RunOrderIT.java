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

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnitIntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;

/**
 * Verifies the runOrder setting and its effect
 *
 * @author Kristian Rosenvold
 */
@SuppressWarnings("checkstyle:magicnumber")
public class RunOrderIT extends SurefireJUnitIntegrationTestCase {
    private static final String[] TESTS_IN_ALPHABETICAL_ORDER = {"TA", "TB", "TC"};

    private static final String[] TESTS_IN_REVERSE_ALPHABETICAL_ORDER = {"TC", "TB", "TA"};

    // testing random is left as an exercise to the reader. Patches welcome

    @Test
    public void testAlphabeticalJUnit4() throws VerificationException {
        OutputValidator validator = executeWithRunOrder("alphabetical");
        assertTestnamesAppearInSpecificOrder(validator, TESTS_IN_ALPHABETICAL_ORDER);
    }

    @Test
    public void testRandomJUnit4DifferentSeed() throws VerificationException {
        long seed = 0L;
        OutputValidator validator = executeWithRandomOrder(seed);
        String[] expected = validator.getStringsOrderInLog(TESTS_IN_ALPHABETICAL_ORDER);
        for (long i = seed; i < 5 + seed; i++) {
            OutputValidator validator2 = executeWithRandomOrder(i);
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
        OutputValidator validator = executeWithRandomOrder(seed);
        String[] expected = validator.getStringsOrderInLog(TESTS_IN_ALPHABETICAL_ORDER);
        for (long i = 0; i < 5; i++) {
            OutputValidator validator2 = executeWithRandomOrder(seed);
            String[] observed = validator2.getStringsOrderInLog(TESTS_IN_ALPHABETICAL_ORDER);
            if (!Arrays.equals(expected, observed)) {
                throw new VerificationException("Random orders with the same seed produced different orders");
            }
        }
    }

    @Test
    public void testRandomJUnit4PrintSeedWithGivenSeed() {
        OutputValidator validator = executeWithRandomOrder(0L);
        validator.verifyTextInLog("To reproduce ordering use flag");
    }

    @Test
    public void testReverseAlphabeticalJUnit4() throws VerificationException {
        OutputValidator validator = executeWithRunOrder("reversealphabetical");
        assertTestnamesAppearInSpecificOrder(validator, TESTS_IN_REVERSE_ALPHABETICAL_ORDER);
    }

    @Test
    public void testNonExistingRunOrderJUnit4() {
        unpack().forkCount(1)
                .reuseForks(reuseForks())
                .runOrder("nonExistingRunOrder")
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog("There's no RunOrder with the name nonExistingRunOrder.");
    }

    @Test
    public void testReverseAlphabeticalJUnit5() throws VerificationException {
        OutputValidator validator = executeWithRunOrder("reversealphabetical", "runOrder-junitX", "5.14.1");
        assertTestnamesAppearInSpecificOrder(validator, TESTS_IN_REVERSE_ALPHABETICAL_ORDER);
    }

    @Test
    public void testAlphabeticalJUnit5() throws VerificationException {
        OutputValidator validator = executeWithRunOrder("alphabetical", "runOrder-junitX", "5.14.1");
        assertTestnamesAppearInSpecificOrder(validator, TESTS_IN_ALPHABETICAL_ORDER);
    }

    @Test
    public void testReverseAlphabeticalJUnit6() throws VerificationException {
        // JUnit 6.0.0 requires Java 17+
        assumeJavaVersion(17);
        OutputValidator validator = executeWithRunOrder("reversealphabetical", "runOrder-junitX", "6.0.1");
        assertTestnamesAppearInSpecificOrder(validator, TESTS_IN_REVERSE_ALPHABETICAL_ORDER);
    }

    @Test
    public void testAlphabeticalJUnit6() throws VerificationException {
        // JUnit 6.0.0 requires Java 17+
        assumeJavaVersion(17);
        OutputValidator validator = executeWithRunOrder("alphabetical", "runOrder-junitX", "6.0.1");
        assertTestnamesAppearInSpecificOrder(validator, TESTS_IN_ALPHABETICAL_ORDER);
    }

    private OutputValidator executeWithRunOrder(String runOrder) {
        return executeWithRunOrder(runOrder, "runOrder", null);
    }

    private OutputValidator executeWithRunOrder(String runOrder, String sourceName, String junitVersion) {
        return unpack(sourceName)
                .setJUnitVersion(junitVersion)
                .forkCount(1)
                .reuseForks(reuseForks())
                .runOrder(runOrder)
                .executeTest()
                .verifyErrorFree(3);
    }

    private OutputValidator executeWithRandomOrder(long seed) {
        return unpack().forkCount(1)
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
