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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ProcessHandleChecker}.
 * <p>
 * These tests require Java 9+ to run as they directly use the ProcessHandle API.
 */
public class ProcessHandleCheckerTest {

    @Test
    public void shouldDetectCurrentProcessAsAlive() {
        // Use current process PID - it's definitely alive
        long currentPid = ProcessHandle.current().pid();
        ProcessHandleChecker checker = new ProcessHandleChecker(String.valueOf(currentPid));

        assertThat(checker.canUse()).isTrue();
        assertThat(checker.isProcessAlive()).isTrue();
        assertThat(checker.isStopped()).isFalse();
    }

    @Test
    public void shouldDetectNonExistentProcessAsNotUsable() {
        // Use an invalid PID that's unlikely to exist
        ProcessHandleChecker checker = new ProcessHandleChecker("999999999");

        assertThat(checker.canUse()).isFalse();
    }

    @Test
    public void shouldThrowWhenCallingIsProcessAliveWithoutCanUse() {
        // Use an invalid PID
        ProcessHandleChecker checker = new ProcessHandleChecker("999999999");

        assertThatThrownBy(checker::isProcessAlive)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("irrelevant to call isProcessAlive()");
    }

    @Test
    public void shouldStopChecker() {
        long currentPid = ProcessHandle.current().pid();
        ProcessHandleChecker checker = new ProcessHandleChecker(String.valueOf(currentPid));

        assertThat(checker.canUse()).isTrue();
        assertThat(checker.isStopped()).isFalse();

        checker.stop();

        assertThat(checker.isStopped()).isTrue();
        assertThat(checker.canUse()).isFalse();
    }

    @Test
    public void shouldDestroyActiveCommands() {
        long currentPid = ProcessHandle.current().pid();
        ProcessHandleChecker checker = new ProcessHandleChecker(String.valueOf(currentPid));

        assertThat(checker.canUse()).isTrue();

        checker.destroyActiveCommands();

        assertThat(checker.isStopped()).isTrue();
        assertThat(checker.canUse()).isFalse();
    }

    @Test
    public void shouldReturnMeaningfulToString() {
        long currentPid = ProcessHandle.current().pid();
        ProcessHandleChecker checker = new ProcessHandleChecker(String.valueOf(currentPid));

        String toString = checker.toString();

        assertThat(toString)
                .contains("ProcessHandleChecker")
                .contains("ppid=" + currentPid)
                .contains("stopped=false");
    }

    @Test
    public void shouldReturnToStringWithStartInstantAfterCanUse() {
        long currentPid = ProcessHandle.current().pid();
        ProcessHandleChecker checker = new ProcessHandleChecker(String.valueOf(currentPid));

        checker.canUse();
        String toString = checker.toString();

        assertThat(toString).contains("ProcessHandleChecker").contains("hasHandle=true");
    }

    @Test
    public void shouldCreateViaFactoryMethod() {
        long currentPid = ProcessHandle.current().pid();
        ParentProcessChecker checker = ParentProcessCheckerFactory.of(String.valueOf(currentPid));

        assertThat(checker).isInstanceOf(ProcessHandleChecker.class);
        assertThat(checker.canUse()).isTrue();
        assertThat(checker.isProcessAlive()).isTrue();
    }

    @Test
    public void shouldReturnNullFromFactoryForNullPpid() {
        ParentProcessChecker checker = ParentProcessCheckerFactory.of(null);

        assertThat(checker).isNull();
    }

    @Test
    public void shouldThrowOnInvalidPpidFormat() {
        assertThatThrownBy(() -> new ProcessHandleChecker("not-a-number")).isInstanceOf(NumberFormatException.class);
    }
}
