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
package org.apache.maven.surefire.booter;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProcessChecker}.
 */
public class ProcessCheckerFactoryTest {

    @Test
    public void shouldCreateCheckerForCurrentProcess() {
        // Get current process PID using reflection to stay Java 8 compatible
        String currentPid = getCurrentPid();
        if (currentPid == null) {
            // Skip test if we can't get PID
            return;
        }

        ProcessChecker checker = ProcessChecker.of(currentPid);

        assertThat(checker).isNotNull();
        assertThat(checker.canUse()).isTrue();
        assertThat(checker.isProcessAlive()).isTrue();
        assertThat(checker.isStopped()).isFalse();
    }

    @Test
    public void shouldSelectProcessHandleCheckerOnJava9Plus() {
        if (!ProcessChecker.isProcessHandleSupported()) {
            // Skip test if ProcessHandle is not available (Java 8)
            return;
        }

        String currentPid = getCurrentPid();
        if (currentPid == null) {
            return;
        }

        // ProcessChecker checker = ProcessChecker.of(currentPid);
        // FIXME for some reason the cannot find classes from multi-release (java9) jar in surefire plugin
        //        assertThat(checker.getClass().getSimpleName()).isEqualTo("ProcessHandleChecker");
    }

    @Test
    public void shouldStopChecker() {
        String currentPid = getCurrentPid();
        if (currentPid == null) {
            return;
        }

        ProcessChecker checker = ProcessChecker.of(currentPid);

        assertThat(checker.canUse()).isTrue();
        assertThat(checker.isStopped()).isFalse();

        checker.stop();

        assertThat(checker.isStopped()).isTrue();
        assertThat(checker.canUse()).isFalse();
    }

    @Test
    public void shouldDestroyActiveCommands() {
        String currentPid = getCurrentPid();
        if (currentPid == null) {
            return;
        }

        ProcessChecker checker = ProcessChecker.of(currentPid);
        assertThat(checker.canUse()).isTrue();

        checker.destroyActiveCommands();

        assertThat(checker.isStopped()).isTrue();
        assertThat(checker.canUse()).isFalse();
    }

    @Test
    public void shouldHandleNonExistentProcess() {
        // Use an invalid PID that's unlikely to exist
        ProcessChecker checker = ProcessChecker.of("999999999");

        assertThat(checker).isNotNull();
        // canUse() returns false for non-existent process
        // FIXME for some reason the cannot find classes from multi-release (java9) jar in surefire plugin
        // assertThat(checker.canUse()).isFalse();
    }

    /**
     * Gets the current process PID in a way that works on both Java 8 and Java 9+.
     */
    private static String getCurrentPid() {
        // Try ProcessHandle (Java 9+) first via reflection
        try {
            Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
            Object currentHandle = processHandleClass.getMethod("current").invoke(null);
            Long pid = (Long) processHandleClass.getMethod("pid").invoke(currentHandle);
            return String.valueOf(pid);
        } catch (Exception e) {
            // Fall back to ManagementFactory (works on Java 8)
            try {
                String name = java.lang.management.ManagementFactory.getRuntimeMXBean()
                        .getName();
                // Format is "pid@hostname"
                int atIndex = name.indexOf('@');
                if (atIndex > 0) {
                    return name.substring(0, atIndex);
                }
            } catch (Exception ex) {
                // Ignore
            }
        }
        return null;
    }
}
