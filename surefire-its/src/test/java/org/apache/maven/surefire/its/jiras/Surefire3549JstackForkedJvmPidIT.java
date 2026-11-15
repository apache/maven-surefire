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
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * GH-3549: jstack must dump the forked JVM, not the shell that starts it.
 */
public class Surefire3549JstackForkedJvmPidIT extends SurefireJUnit4IntegrationTestCase {

    private static final Pattern DUMP_NAME = Pattern.compile("surefire-timeout-jstack-\\d+-(\\d+)\\.txt");

    @BeforeEach
    public void needsJstack() {
        assumeTrue(IS_JAVA9_PLUS, "the fork PID is only known on Java 9+");
        assumeTrue(jstackAvailable(), "jstack not found in this JDK");
    }

    @Test
    public void jstackDumpsTheForkedJvm() throws Exception {
        assertEveryForkWasDumped(runWithForks(1), 1);
    }

    @Test
    public void jstackDumpsEachForkedJvm() throws Exception {
        assertEveryForkWasDumped(runWithForks(2), 2);
    }

    private OutputValidator runWithForks(int forkCount) {
        return unpack("surefire-3549-jstack-forked-jvm-pid")
                .forkCount(forkCount)
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog("There was a timeout in the fork");
    }

    private static void assertEveryForkWasDumped(OutputValidator validator, int forks) throws Exception {
        // PIDs written by the tests themselves
        File target = new File(validator.getBaseDir(), "target");
        Set<String> jvmPids = new TreeSet<>();
        File[] pidFiles = target.listFiles((dir, name) -> name.startsWith("pid-"));
        if (pidFiles != null) {
            for (File pidFile : pidFiles) {
                jvmPids.add(read(pidFile));
            }
        }
        assertThat(jvmPids).as("PIDs written by the sleeping tests").hasSize(forks);

        // PIDs jstack ran against, taken from the dump file names
        File jstackDir = new File(target, "jstacks");
        String[] dumps = jstackDir.list();
        Set<String> dumpedPids = new TreeSet<>();
        if (dumps != null) {
            for (String dump : dumps) {
                Matcher matcher = DUMP_NAME.matcher(dump);
                if (matcher.matches()) {
                    dumpedPids.add(matcher.group(1));
                }
            }
        }
        assertThat(dumpedPids)
                .as("jstack should target the forked JVMs %s but got:%n%s", jvmPids, describe(jstackDir, dumps))
                .isEqualTo(jvmPids);

        for (String dump : dumps) {
            assertThat(read(new File(jstackDir, dump)))
                    .as(dump)
                    .contains("Full thread dump")
                    .contains("surefire3549.");
        }
        validator.verifyTextNotInLog("jstack exited with code");
    }

    private static String describe(File dir, String[] files) throws IOException {
        if (files == null) {
            return dir + " does not exist";
        }
        if (files.length == 0) {
            return dir + " is empty";
        }
        StringBuilder sb = new StringBuilder();
        for (String file : files) {
            sb.append(file).append(": ").append(read(new File(dir, file))).append(System.lineSeparator());
        }
        return sb.toString();
    }

    private static String read(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()), UTF_8).trim();
    }

    private static boolean jstackAvailable() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            return false;
        }
        String exe = System.getProperty("os.name", "").toLowerCase().contains("win") ? "jstack.exe" : "jstack";
        if (new File(javaHome, "bin/" + exe).canExecute()) {
            return true;
        }
        File parent = new File(javaHome).getParentFile();
        return parent != null && new File(parent, "bin/" + exe).canExecute();
    }
}
