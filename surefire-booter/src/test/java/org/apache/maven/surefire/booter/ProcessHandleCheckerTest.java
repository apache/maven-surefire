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

import java.lang.management.ManagementFactory;

import org.junit.Assume;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeTrue;

/**
 * Tests for {@link ProcessHandleChecker}.
 * <p>
 * These tests use reflection-based PID detection to work on both Java 8 and Java 9+.
 */
public class ProcessHandleCheckerTest {

    @Test
    public void shouldReportAvailableOnJava9Plus() {
        // This test runs on modern JVMs, so isAvailable() should return true
        // FIXME DisabledOnJre when we migrate to junit5
        double v = Double.parseDouble(System.getProperty("java.specification.version"));
        Assume.assumeTrue(v >= 9.0);
        assertThat(ProcessHandleChecker.isAvailable()).isTrue();
    }

    @Test
    public void shouldDetectCurrentProcessAsAlive() {
        assumeTrue("ProcessHandle not available", ProcessHandleChecker.isAvailable());

        String currentPid = getCurrentPid();
        assumeTrue("Could not determine current PID", currentPid != null);

        ProcessHandleChecker checker = new ProcessHandleChecker(currentPid);

        assertThat(checker.canUse()).isTrue();
        assertThat(checker.isProcessAlive()).isTrue();
        assertThat(checker.isStopped()).isFalse();
    }

    @Test
    public void shouldDetectNonExistentProcessAsNotUsable() {
        assumeTrue("ProcessHandle not available", ProcessHandleChecker.isAvailable());

        // Use an invalid PID that's unlikely to exist
        ProcessHandleChecker checker = new ProcessHandleChecker("999999999");

        assertThat(checker.canUse()).isFalse();
    }

    @Test
    public void shouldStopChecker() {
        assumeTrue("ProcessHandle not available", ProcessHandleChecker.isAvailable());

        String currentPid = getCurrentPid();
        assumeTrue("Could not determine current PID", currentPid != null);

        ProcessHandleChecker checker = new ProcessHandleChecker(currentPid);

        assertThat(checker.canUse()).isTrue();
        assertThat(checker.isStopped()).isFalse();

        checker.stop();

        assertThat(checker.isStopped()).isTrue();
        assertThat(checker.canUse()).isFalse();
    }

    @Test
    public void shouldDestroyActiveCommands() {
        assumeTrue("ProcessHandle not available", ProcessHandleChecker.isAvailable());

        String currentPid = getCurrentPid();
        assumeTrue("Could not determine current PID", currentPid != null);

        ProcessHandleChecker checker = new ProcessHandleChecker(currentPid);

        assertThat(checker.canUse()).isTrue();

        checker.destroyActiveCommands();

        assertThat(checker.isStopped()).isTrue();
        assertThat(checker.canUse()).isFalse();
    }

    @Test
    public void shouldReturnMeaningfulToString() {
        assumeTrue("ProcessHandle not available", ProcessHandleChecker.isAvailable());

        String currentPid = getCurrentPid();
        assumeTrue("Could not determine current PID", currentPid != null);

        ProcessHandleChecker checker = new ProcessHandleChecker(currentPid);

        String toString = checker.toString();

        assertThat(toString)
                .contains("ProcessHandleChecker")
                .contains("pid=" + currentPid)
                .contains("stopped=false");
    }

    @Test
    public void shouldReturnToStringWithStartInstantAfterCanUse() {
        assumeTrue("ProcessHandle not available", ProcessHandleChecker.isAvailable());

        String currentPid = getCurrentPid();
        assumeTrue("Could not determine current PID", currentPid != null);

        ProcessHandleChecker checker = new ProcessHandleChecker(currentPid);

        checker.canUse();
        String toString = checker.toString();

        assertThat(toString).contains("ProcessHandleChecker").contains("hasHandle=true");
    }

    @Test
    public void shouldCreateViaFactoryMethod() {
        assumeTrue("ProcessHandle not available", ProcessHandleChecker.isAvailable());

        String currentPid = getCurrentPid();
        assumeTrue("Could not determine current PID", currentPid != null);

        ProcessChecker checker = ProcessCheckerFactory.of(currentPid);

        assertThat(checker).isInstanceOf(ProcessHandleChecker.class);
        assertThat(checker.canUse()).isTrue();
        assertThat(checker.isProcessAlive()).isTrue();
    }

    @Test
    public void shouldReturnNullFromFactoryForNullPpid() {
        ProcessChecker checker = ProcessCheckerFactory.of(null);

        assertThat(checker).isNull();
    }

    @Test
    public void shouldThrowOnInvalidPpidFormat() {
        assertThatThrownBy(() -> new ProcessHandleChecker("not-a-number")).isInstanceOf(NumberFormatException.class);
    }

    @Test
    public void shouldReturnProcessInfoAfterCanUse() {
        assumeTrue("ProcessHandle not available", ProcessHandleChecker.isAvailable());

        String currentPid = getCurrentPid();
        assumeTrue("Could not determine current PID", currentPid != null);

        ProcessHandleChecker checker = new ProcessHandleChecker(currentPid);

        // Now processInfo() should return valid info
        ProcessInfo processInfo = checker.processInfo();
        assertThat(processInfo).isNotNull();
        assertThat(processInfo.getPID()).isEqualTo(currentPid);
        assertThat(processInfo.getTime()).isGreaterThan(0L);
    }

    /**
     * Gets the current process PID using reflection (Java 8 compatible).
     *
     * @return the current process PID as a string, or null if it cannot be determined
     */
    private static String getCurrentPid() {
        // Try ProcessHandle.current().pid() via reflection (Java 9+)
        try {
            Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
            Object currentHandle = processHandleClass.getMethod("current").invoke(null);
            Long pid = (Long) processHandleClass.getMethod("pid").invoke(currentHandle);
            return String.valueOf(pid);
        } catch (Exception e) {
            // Fall back to ManagementFactory (works on Java 8)
            try {
                String name = ManagementFactory.getRuntimeMXBean().getName();
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
