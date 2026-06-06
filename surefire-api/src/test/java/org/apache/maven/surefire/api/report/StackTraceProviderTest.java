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
package org.apache.maven.surefire.api.report;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import static org.apache.maven.surefire.api.report.StackTraceProvider.DEFAULT_MAX_FRAMES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for {@link StackTraceProvider} covering both the {@code StackWalker} path (Java 9+) and the
 * {@link Thread#getStackTrace()} fallback path.
 * <p>
 * Marked {@link Isolated} because these tests mutate {@link StackTraceProvider}'s global static configuration
 * via {@code configure(...)}; running concurrently with other test classes would be unsafe.
 */
@Isolated
class StackTraceProviderTest {

    @BeforeEach
    @AfterEach
    void resetDefaults() {
        StackTraceProvider.configure(null, DEFAULT_MAX_FRAMES);
    }

    @Test
    void getStackContainsCallerAsFirstFrame() {
        List<String> stack = StackTraceProvider.getStack();
        assertThat(stack).isNotEmpty();
        assertThat(stack.get(0)).isEqualTo(getClass().getName() + "#getStackContainsCallerAsFirstFrame");
    }

    @Test
    void internalAndJdkFramesAreExcluded() {
        List<String> stack = StackTraceProvider.getStack();
        assertThat(stack)
                .noneMatch(frame -> frame.startsWith("java.")
                        || frame.startsWith("javax.")
                        || frame.startsWith("sun.")
                        || frame.startsWith("jdk."));
        assertThat(stack).noneMatch(frame -> frame.startsWith(StackTraceProvider.class.getName() + "#"));
        assertThat(stack).noneMatch(frame -> frame.startsWith(StackWalkerStrategy.class.getName() + "#"));
    }

    @Test
    void zeroMaxFramesReturnsEmpty() {
        StackTraceProvider.configure(null, 0);
        assertThat(StackTraceProvider.getStack()).isEmpty();
        assertThat(StackTraceProvider.getStackLegacy()).isEmpty();
    }

    @Test
    void negativeMaxFramesReturnsEmpty() {
        StackTraceProvider.configure(null, -5);
        assertThat(StackTraceProvider.getStack()).isEmpty();
        assertThat(StackTraceProvider.getStackLegacy()).isEmpty();
    }

    @Test
    void frameLimitIsRespected() {
        StackTraceProvider.configure(null, 3);
        assertThat(StackTraceProvider.getStack().size()).isLessThanOrEqualTo(3);
        assertThat(StackTraceProvider.getStackLegacy().size()).isLessThanOrEqualTo(3);
    }

    @Test
    void customPrefixesReplaceDefaults() {
        // Default configuration filters JDK frames, so the Thread.getStackTrace() frame is removed.
        assertThat(StackTraceProvider.getStackLegacy()).noneMatch(frame -> frame.startsWith("java."));

        // A custom prefix replaces the defaults, so "java." is no longer filtered and the leading
        // java.lang.Thread#getStackTrace frame reappears in the legacy path.
        StackTraceProvider.configure("com.example.nonexistent.", DEFAULT_MAX_FRAMES);
        assertThat(StackTraceProvider.getStackLegacy()).anyMatch(frame -> frame.startsWith("java."));
    }

    @Test
    void emptyPrefixesDisableFiltering() {
        StackTraceProvider.configure("", DEFAULT_MAX_FRAMES);
        assertThat(StackTraceProvider.getStackLegacy()).anyMatch(frame -> frame.startsWith("java."));
    }

    @Test
    void nullPrefixesRestoreDefaults() {
        StackTraceProvider.configure("", DEFAULT_MAX_FRAMES);
        StackTraceProvider.configure(null, DEFAULT_MAX_FRAMES);
        assertThat(StackTraceProvider.getStackLegacy()).noneMatch(frame -> frame.startsWith("java."));
    }

    @Test
    void stackWalkerAndLegacyShareFirstFrame() {
        String expected = getClass().getName() + "#stackWalkerAndLegacyShareFirstFrame";
        assertThat(StackTraceProvider.getStack().get(0)).isEqualTo(expected);
        assertThat(StackTraceProvider.getStackLegacy().get(0)).isEqualTo(expected);
    }

    @Test
    void strategyAvailabilityMatchesRuntime() {
        boolean java9OrLater = !System.getProperty("java.specification.version").startsWith("1.");
        assertThat(StackWalkerStrategy.isAvailable()).isEqualTo(java9OrLater);
    }

    @Test
    void strategyWalkReturnsBoundedFrames() {
        assumeTrue(StackWalkerStrategy.isAvailable());
        List<String> frames = StackWalkerStrategy.walk(5, className -> false);
        assertThat(frames).isNotNull().isNotEmpty();
        assertThat(frames.size()).isLessThanOrEqualTo(5);
        assertThat(frames).allMatch(frame -> frame.contains("#"));
        // Nothing excluded, so the first frame is StackWalkerStrategy.walk itself.
        assertThat(frames.get(0)).isEqualTo(StackWalkerStrategy.class.getName() + "#walk");
        assertThat(frames).anyMatch(frame -> frame.equals(getClass().getName() + "#strategyWalkReturnsBoundedFrames"));
    }

    @Test
    void strategyWalkAppliesExcludePredicate() {
        assumeTrue(StackWalkerStrategy.isAvailable());
        List<String> frames =
                StackWalkerStrategy.walk(10, className -> className.equals(StackWalkerStrategy.class.getName()));
        assertThat(frames).noneMatch(frame -> frame.startsWith(StackWalkerStrategy.class.getName() + "#"));
        assertThat(frames.get(0)).isEqualTo(getClass().getName() + "#strategyWalkAppliesExcludePredicate");
    }
}
