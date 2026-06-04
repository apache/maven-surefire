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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SUREFIRE-838: ForkedProcessTimeoutExtension is invoked on forked process
 * timeout (both {@code onTimeoutDetected} before kill and {@code onForkExited}
 * after the fork exits).
 */
public class Surefire838TimeoutExtensionIT extends SurefireJUnit4IntegrationTestCase {

    @BeforeAll
    public static void installExtension() {
        unpack(Surefire838TimeoutExtensionIT.class, "surefire-838-timeout-extension-ext", "ext")
                .executeInstall();
    }

    @Test
    public void extensionInvokedOnTimeout() {
        OutputValidator validator = unpack("surefire-838-timeout-extension")
                .maven()
                .withFailure()
                .executeTest()
                .verifyTextInLog("There was a timeout in the fork");

        assertThat(validator.getSurefireReportsFile("timeout-detected.txt").isFile())
                .as("timeout-detected.txt marker must be written by MarkerTimeoutExtension#onTimeoutDetected")
                .isTrue();
        validator.getSurefireReportsFile("timeout-detected.txt").assertContainsText("timeoutSeconds=5");

        assertThat(validator.getSurefireReportsFile("fork-exited.txt").isFile())
                .as("fork-exited.txt marker must be written by MarkerTimeoutExtension#onForkExited")
                .isTrue();
        validator.getSurefireReportsFile("fork-exited.txt").assertContainsText("isTimeout=true");

        // Verify the built-in JstackTimeoutExtension was enabled from the POM via
        // <forkedProcessTimeoutExtensionContext><jstack.enabled>true</jstack.enabled>...
        // The dump file is best-effort: it requires a JDK with `jstack` and a resolvable
        // PID (Java 9+). Skip the assertion when those preconditions are not met.
        if (isJava9OrLater() && jstackAvailable()) {
            File jstackDir = new File(validator.getBaseDir(), "target/jstacks");
            assertThat(jstackDir)
                    .as("custom jstack.output.location directory must exist")
                    .isDirectory();
            File[] dumps = jstackDir.listFiles(
                    (dir, name) -> name.startsWith("surefire-timeout-jstack-") && name.endsWith(".txt"));
            assertThat(dumps)
                    .as("JstackTimeoutExtension must have written at least one thread-dump file in " + jstackDir)
                    .isNotNull()
                    .isNotEmpty();
        }
    }

    private static boolean isJava9OrLater() {
        String spec = System.getProperty("java.specification.version", "1.8");
        try {
            // "1.8" -> 1.8 -> < 9; "11", "17", "21" -> >= 9
            return !spec.startsWith("1.") && Integer.parseInt(spec) >= 9;
        } catch (NumberFormatException e) {
            return false;
        }
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
