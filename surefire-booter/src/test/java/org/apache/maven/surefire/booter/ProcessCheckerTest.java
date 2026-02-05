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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Testing {@link ProcessChecker} via {@link ProcessCheckerFactory}.
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

        ProcessChecker checker = ProcessCheckerFactory.of(expectedPid);

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
        ProcessChecker checker = ProcessCheckerFactory.of("0");
        checker.stop();

        assertThat(checker.canUse()).isFalse();

        exceptions.expect(IllegalStateException.class);
        exceptions.expectMessage("irrelevant to call isProcessAlive()");

        checker.isProcessAlive();

        fail("this test should throw exception");
    }

    @Test
    public void shouldNotBeAlive() {
        ProcessChecker checker = ProcessCheckerFactory.of(Long.toString(Integer.MAX_VALUE));

        assertThat(checker.canUse()).isFalse();
    }

    @Test
    public void shouldReturnNullForNullPpid() {
        ProcessChecker checker = ProcessCheckerFactory.of(null);
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
}
