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
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

/**
 * Test forkCount and reuseForks
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class ForkCountIT extends SurefireJUnit4IntegrationTestCase {

    private OutputValidator outputValidator;

    @BeforeClass
    public static void installDumpPidPlugin() {
        unpack(ForkCountIT.class, "test-helper-dump-pid-plugin", "plugin").executeInstall();
    }

    @Test
    public void testForkNever() {
        String[] pids = doTest(unpack(getProject()).forkNever());
        assertSamePids(pids);
        assertEndWith(pids, "_1_1", 3);
        assertEquals("my pid is equal to pid 1 of the test", getMainPID(), pids[0]);
    }

    @Test
    public void testForkOncePerThreadSingleThread() {
        int threadCount = 1;
        String[] pids = doTest(
                unpack(getProject()).setForkJvm().forkPerThread(threadCount).threadCount(threadCount));
        assertSamePids(pids);
        assertEndWith(pids, "_1_1", 3);
        assertNotEquals("pid 1 is not the same as the main process' pid", pids[0], getMainPID());
    }

    @Test
    public void testForkOncePerThreadTwoThreads() {
        int threadCount = 2;
        String[] pids = doTest(unpack(getProject())
                .forkPerThread(threadCount)
                .threadCount(threadCount)
                .addGoal("-DsleepLength=7200"));
        assertDifferentPids(pids, 2);
        assertNotEquals("pid 1 is not the same as the main process' pid", pids[0], getMainPID());
    }

    @Test
    public void testForkCountOneNoReuse() {
        String[] pids = doTest(unpack(getProject()).setForkJvm().forkCount(1).reuseForks(false));
        assertDifferentPids(pids);
        assertEndWith(pids, "_1_1", 3);
        assertNotEquals("pid 1 is not the same as the main process' pid", pids[0], getMainPID());
    }

    @Test
    public void testForkCountOneReuse() {
        String[] pids = doTest(unpack(getProject()).setForkJvm().forkCount(1).reuseForks(true));
        assertSamePids(pids);
        assertEndWith(pids, "_1_1", 3);
        assertNotEquals("pid 1 is not the same as the main process' pid", pids[0], getMainPID());
    }

    @Test
    public void testForkCountTwoNoReuse() {
        String[] pids = doTest(
                unpack(getProject()).setForkJvm().forkCount(2).reuseForks(false).addGoal("-DsleepLength=7200"));
        assertDifferentPids(pids);
        assertNotEquals("pid 1 is not the same as the main process' pid", pids[0], getMainPID());
    }

    @Test
    public void testForkCountTwoReuse() {
        String[] pids =
                doTest(unpack(getProject()).forkCount(2).reuseForks(true).addGoal("-DsleepLength=7200"));
        assertDifferentPids(pids, 2);
        assertNotEquals("pid 1 is not the same as the main process' pid", pids[0], getMainPID());
    }

    private void assertEndWith(String[] pids, String suffix, int expectedMatches) {
        int matches = 0;
        for (String pid : pids) {
            if (pid.endsWith(suffix)) {
                matches++;
            }
        }

        assertEquals("suffix " + suffix + " matched the correct number of pids", expectedMatches, matches);
    }

    private void assertDifferentPids(String[] pids, int numOfDifferentPids) {
        Set<String> pidSet = new HashSet<>(Arrays.asList(pids));
        assertEquals("number of different pids is not as expected", numOfDifferentPids, pidSet.size());
    }

    @Test
    public void testForkOnce() {
        String[] pids = doTest(unpack(getProject()).forkOnce());
        assertSamePids(pids);
        assertNotEquals("pid 1 is not the same as the main process' pid", pids[0], getMainPID());
    }

    private String getMainPID() {
        final TestFile targetFile = outputValidator.getTargetFile("maven.pid");
        String pid = targetFile.slurpFile();
        return pid + " testValue_1_1";
    }

    private void assertSamePids(String[] pids) {
        assertEquals("pid 1 didn't match pid 2", pids[0], pids[1]);
        assertEquals("pid 1 didn't match pid 3", pids[0], pids[2]);
    }

    private void assertDifferentPids(String[] pids) {
        if (pids[0].equals(pids[1])) {
            fail("pid 1 matched pid 2: " + pids[0]);
        }

        if (pids[0].equals(pids[2])) {
            fail("pid 1 matched pid 3: " + pids[0]);
        }

        if (pids[1].equals(pids[2])) {
            fail("pid 2 matched pid 3: " + pids[0]);
        }
    }

    private String[] doTest(SurefireLauncher forkLauncher) {
        forkLauncher.sysProp("testProperty", "testValue_${surefire.threadNumber}_${surefire.forkNumber}");
        forkLauncher.addGoal("org.apache.maven.plugins.surefire:maven-dump-pid-plugin:dump-pid");
        outputValidator = forkLauncher.executeTest();
        outputValidator.verifyErrorFreeLog().assertTestSuiteResults(3, 0, 0, 0);
        String[] pids = new String[3];
        for (int i = 1; i <= pids.length; i++) {
            final TestFile targetFile = outputValidator.getTargetFile("test" + i + "-pid");
            String pid = targetFile.slurpFile();
            pids[i - 1] = pid;
        }
        return pids;
    }

    protected String getProject() {
        return "fork-count";
    }
}
