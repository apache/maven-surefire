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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

/**
 * Testing {@link ProcessChecker} via {@link ProcessChecker#of(String)}.
 *
 * @since 2.20.1
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ProcessCheckerTest {

    @Rule
    public final ExpectedException exceptions = ExpectedException.none();

    @Test
    public void shouldHavePidAtBegin() {
        String expectedPid =
                ManagementFactory.getRuntimeMXBean().getName().split("@")[0].trim();

        ProcessChecker checker = ProcessChecker.of(expectedPid);

        assertThat(checker).isNotNull();

        assertThat(checker.canUse()).isTrue();

        assertThat(checker.isProcessAlive()).isTrue();

        ProcessInfo processInfo = checker.processInfo();
        assertThat(processInfo).isNotNull();
        assertThat(processInfo.getPID()).isEqualTo(expectedPid);
        assertThat(processInfo.getTime()).isGreaterThan(0L);
    }

    @Test
    public void shouldBeStopped() {
        ProcessChecker checker = ProcessChecker.of("0");
        checker.stop();

        assertThat(checker.canUse()).isFalse();

        exceptions.expect(IllegalStateException.class);
        exceptions.expectMessage("irrelevant to call isProcessAlive()");

        checker.isProcessAlive();

        fail("this test should throw exception");
    }

    @Test
    public void exceptionCallIsProcessAlive() {
        // FIXME DisabledOnJre when we migrate to junit5 and run on unix too
        // winddows java 8 must depends on wwmc something available
        double v = Double.parseDouble(System.getProperty("java.specification.version"));
        Assume.assumeTrue(v >= 9.0);
        ProcessChecker checker = ProcessChecker.of(Long.toString(Integer.MAX_VALUE));

        assertThatThrownBy(checker::isProcessAlive).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void shouldReturnNullForNullPpid() {
        ProcessChecker checker = ProcessChecker.of(null);
        assertThat(checker).isNull();
    }

    @Test
    public void shouldBeTypeNull() {
        assertThat(ProcessCheckerType.toEnum(null)).isNull();

        assertThat(ProcessCheckerType.toEnum("   ")).isNull();

        assertThat(ProcessCheckerType.isValid(null)).isTrue();
    }

    @Test
    public void shouldBeException() {
        exceptions.expect(IllegalArgumentException.class);
        exceptions.expectMessage("unknown process checker");

        assertThat(ProcessCheckerType.toEnum("anything else")).isNull();
    }

    @Test
    public void shouldNotBeValid() {
        assertThat(ProcessCheckerType.isValid("anything")).isFalse();
    }

    @Test
    public void shouldBeTypePing() {
        assertThat(ProcessCheckerType.toEnum("ping")).isEqualTo(ProcessCheckerType.PING);

        assertThat(ProcessCheckerType.isValid("ping")).isTrue();

        assertThat(ProcessCheckerType.PING.getType()).isEqualTo("ping");
    }

    @Test
    public void shouldBeTypeNative() {
        assertThat(ProcessCheckerType.toEnum("native")).isEqualTo(ProcessCheckerType.NATIVE);

        assertThat(ProcessCheckerType.isValid("native")).isTrue();

        assertThat(ProcessCheckerType.NATIVE.getType()).isEqualTo("native");
    }

    @Test
    public void shouldBeTypeAll() {
        assertThat(ProcessCheckerType.toEnum("all")).isEqualTo(ProcessCheckerType.ALL);

        assertThat(ProcessCheckerType.isValid("all")).isTrue();

        assertThat(ProcessCheckerType.ALL.getType()).isEqualTo("all");
    }

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

        ProcessChecker checker = ProcessChecker.of(currentPid);
        assertThat(checker.getClass().getSimpleName()).isEqualTo("ProcessHandleChecker");
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
        assertThat(checker.canUse()).isFalse();
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
