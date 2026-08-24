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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.maven.surefire.its.fixture.AbstractJava9PlusIT;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks that Surefire can list and run tests across several forks when a JDK toolchain is in use
 * (<a href="https://github.com/apache/maven-surefire/issues/2151">Issue 2151</a>).
 * <p>
 * With {@code forkCount > 1} (or {@code reuseForks=false}) Surefire has to list the test classes before
 * spreading them across the forks. When those classes were compiled for a newer JDK than the one running
 * Maven, listing them in Maven's JVM used to fail with {@code UnsupportedClassVersionError}. The fix does the
 * listing in a fork that uses the toolchain JDK instead.
 */
public class Surefire2151ToolchainForkDiscoveryIT extends AbstractJava9PlusIT {
    private static final String TOOLCHAIN_JDK_HOME_PROPERTY = "surefire.test.toolchain.jdkHome";

    /**
     * Guards the multi-fork listing and distribution path when a JDK toolchain is set. It uses the shared
     * single-JDK {@code toolchains.xml} so it always runs in CI.
     */
    @Test
    public void forkCountGreaterThanOneWithToolchainRunsAllTests() {
        assumeJava9()
                .setForkJvm()
                .forkCount(2)
                .reuseForks(true)
                .activateProfile("use-toolchains")
                .addGoal("--toolchains")
                .addGoal(System.getProperty("maven.toolchains.file"))
                .executeTest()
                .verifyErrorFree(3);
    }

    /**
     * Same as above but with {@code reuseForks=false}, which also makes Surefire list the tests first.
     */
    @Test
    public void reuseForksFalseWithToolchainRunsAllTests() {
        assumeJava9()
                .setForkJvm()
                .activateProfile("use-toolchains")
                .activateProfile("reuse-forks-false")
                .addGoal("--toolchains")
                .addGoal(System.getProperty("maven.toolchains.file"))
                .executeTest()
                .verifyErrorFree(3);
    }

    /**
     * The real reproduction: the tests are compiled and run with a JDK <em>newer</em> than the one running Maven,
     * so listing them in Maven's JVM would fail without the discovery fork. Runs only when
     * {@code -Dsurefire.test.toolchain.jdkHome} points at a newer JDK.
     */
    @Test
    public void newerToolchainJdkAcrossMultipleForks() throws Exception {
        String jdkHome = System.getProperty(TOOLCHAIN_JDK_HOME_PROPERTY);
        assumeTrue(
                jdkHome != null && !jdkHome.isEmpty(),
                "set -D" + TOOLCHAIN_JDK_HOME_PROPERTY + " to a JDK newer than the running JVM to run this test");

        SurefireLauncher launcher = assumeJava9();
        File toolchainsFile = writeToolchainsFile(launcher.getUnpackedAt(), jdkHome);

        launcher.setForkJvm()
                .forkCount(2)
                .reuseForks(true)
                .activateProfile("use-toolchains")
                .addGoal("--toolchains")
                .addGoal(toolchainsFile.getAbsolutePath())
                .executeTest()
                .verifyErrorFree(3);
    }

    @Override
    protected String getProjectDirectoryName() {
        return "surefire-2151-toolchain-fork-discovery";
    }

    private static File writeToolchainsFile(File projectDir, String jdkHome) throws Exception {
        File toolchainsFile = new File(projectDir, "toolchains.xml");
        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<toolchains>\n"
                + "  <toolchain>\n"
                + "    <type>jdk</type>\n"
                + "    <provides>\n"
                + "      <version>newer</version>\n"
                + "    </provides>\n"
                + "    <configuration>\n"
                + "      <jdkHome>" + jdkHome + "</jdkHome>\n"
                + "    </configuration>\n"
                + "  </toolchain>\n"
                + "</toolchains>\n";
        Files.write(toolchainsFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return toolchainsFile;
    }
}
