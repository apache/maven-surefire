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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.surefire.its.fixture.HelperAssertions;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test forking in a multi-module project with parallel maven builds
 *
 * @author Andreas Gudian
 */
public class ForkCountMultiModuleIT extends SurefireJUnit4IntegrationTestCase {
    @Test
    public void testForkCountOneNoReuse() {
        List<String> pids = doTest(unpack(getProject()).forkCount(1).reuseForks(false));
        assertAllDifferentPids(pids);
        int matchesOne = countSuffixMatches(pids, "_1_1");
        int matchesTwo = countSuffixMatches(pids, "_2_2");
        assertTrue(matchesOne >= 1, "At least one fork had forkNumber 1");
        assertTrue(matchesTwo >= 1, "At least one fork had forkNumber 2");
        assertEquals(6, matchesOne + matchesTwo, "No other forkNumbers than 1 and 2 have been used");
    }

    @Test
    public void testForkCountOneReuse() {
        List<String> pids = doTest(unpack(getProject()).forkCount(1).reuseForks(true));
        assertDifferentPids(pids, 2);
        assertEndWith(pids, "_1_1", 3);
        assertEndWith(pids, "_2_2", 3);
    }

    @Test
    public void testForkCountTwoNoReuse() {
        List<String> pids = doTest(unpack(getProject()).forkCount(2).reuseForks(false));
        assertAllDifferentPids(pids);
        int matchesOne = countSuffixMatches(pids, "_1_1");
        int matchesTwo = countSuffixMatches(pids, "_2_2");
        int matchesThree = countSuffixMatches(pids, "_3_3");
        int matchesFour = countSuffixMatches(pids, "_4_4");
        assertTrue(matchesOne >= 1, "At least one fork had forkNumber 1");
        assertTrue(matchesTwo >= 1, "At least one fork had forkNumber 2");
        assertTrue(matchesThree >= 1, "At least one fork had forkNumber 3");
        assertTrue(matchesFour >= 1, "At least one fork had forkNumber 4");
        assertEquals(
                6,
                matchesOne + matchesTwo + matchesThree + matchesFour,
                "No other forkNumbers than 1, 2, 3, or 4 have been used");
    }

    @Test
    public void testForkCountTwoReuse() {
        List<String> pids = doTest(unpack(getProject()).forkCount(2).reuseForks(true));
        assertDifferentPids(pids, 4);

        int matchesOne = countSuffixMatches(pids, "_1_1");
        int matchesTwo = countSuffixMatches(pids, "_2_2");
        int matchesThree = countSuffixMatches(pids, "_3_3");
        int matchesFour = countSuffixMatches(pids, "_4_4");
        assertTrue(matchesOne >= 1, "At least one fork had forkNumber 1");
        assertTrue(matchesTwo >= 1, "At least one fork had forkNumber 2");
        assertTrue(matchesThree >= 1, "At least one fork had forkNumber 3");
        assertTrue(matchesFour >= 1, "At least one fork had forkNumber 4");
        assertEquals(
                6,
                matchesOne + matchesTwo + matchesThree + matchesFour,
                "No other forkNumbers than 1, 2, 3, or 4 have been used");
    }

    private void assertEndWith(List<String> pids, String suffix, int expectedMatches) {
        int matches = countSuffixMatches(pids, suffix);

        assertEquals(expectedMatches, matches, "suffix " + suffix + " matched the correct number of pids");
    }

    private int countSuffixMatches(List<String> pids, String suffix) {
        int matches = 0;
        for (String pid : pids) {
            if (pid.endsWith(suffix)) {
                matches++;
            }
        }
        return matches;
    }

    private void assertDifferentPids(List<String> pids, int numOfDifferentPids) {
        Set<String> pidSet = new HashSet<>(pids);
        assertEquals(numOfDifferentPids, pidSet.size(), "number of different pids is not as expected");
    }

    private void assertAllDifferentPids(List<String> pids) {
        assertDifferentPids(pids, pids.size());
    }

    private List<String> doTest(SurefireLauncher forkLauncher) {
        forkLauncher.addGoal("-T2");
        forkLauncher.sysProp("testProperty", "testValue_${surefire.threadNumber}_${surefire.forkNumber}");
        final OutputValidator outputValidator = forkLauncher.setForkJvm().executeTest();
        List<String> pids = new ArrayList<>(6);
        pids.addAll(validateModule(outputValidator, "module-a"));
        pids.addAll(validateModule(outputValidator, "module-b"));

        return pids;
    }

    private List<String> validateModule(OutputValidator outputValidator, String module) {
        HelperAssertions.assertTestSuiteResults(3, 0, 0, 0, new File(outputValidator.getBaseDir(), module));

        List<String> pids = new ArrayList<>(3);
        for (int i = 1; i <= 3; i++) {
            final TestFile targetFile = outputValidator.getTargetFile(module, "test" + i + "-pid");
            String pid = targetFile.slurpFile();
            pids.add(pid);
        }

        return pids;
    }

    protected String getProject() {
        return "fork-count-multimodule";
    }
}
