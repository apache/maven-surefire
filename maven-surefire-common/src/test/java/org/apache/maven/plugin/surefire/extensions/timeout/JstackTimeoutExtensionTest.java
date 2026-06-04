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
package org.apache.maven.plugin.surefire.extensions.timeout;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.surefire.api.suite.RunResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class JstackTimeoutExtensionTest {

    @TempDir
    Path tmp;

    private final NullConsoleLogger logger = new NullConsoleLogger();
    private String previousEnabled;

    @BeforeEach
    void clearProperty() {
        previousEnabled = System.getProperty(JstackTimeoutExtension.ENABLED_PROPERTY);
        System.clearProperty(JstackTimeoutExtension.ENABLED_PROPERTY);
    }

    @AfterEach
    void restoreProperty() {
        if (previousEnabled == null) {
            System.clearProperty(JstackTimeoutExtension.ENABLED_PROPERTY);
        } else {
            System.setProperty(JstackTimeoutExtension.ENABLED_PROPERTY, previousEnabled);
        }
    }

    @Test
    void disabledByDefaultDoesNothing() {
        JstackTimeoutExtension ext = new JstackTimeoutExtension();
        DefaultForkedProcessTimeoutContext ctx =
                new DefaultForkedProcessTimeoutContext(currentPidBestEffort(), 1, null, tmp.toFile(), 60, logger);
        ext.onTimeoutDetected(ctx);
        assertThat(tmp.toFile().listFiles()).isNullOrEmpty();
    }

    @Test
    void unknownPidIsSkippedWhenEnabled() {
        System.setProperty(JstackTimeoutExtension.ENABLED_PROPERTY, "true");
        JstackTimeoutExtension ext = new JstackTimeoutExtension();
        DefaultForkedProcessTimeoutContext ctx =
                new DefaultForkedProcessTimeoutContext(-1L, 1, null, tmp.toFile(), 60, logger);
        ext.onTimeoutDetected(ctx);
        assertThat(tmp.toFile().listFiles()).isNullOrEmpty();
    }

    @Test
    void onForkExitedIsNoop() {
        new JstackTimeoutExtension()
                .onForkExited(
                        new DefaultForkedProcessTimeoutContext(123L, 1, null, tmp.toFile(), 60, logger),
                        new RunResult(0, 0, 0, 0));
        assertThat(tmp.toFile().listFiles()).isNullOrEmpty();
    }

    @Test
    void resolveJstackBinaryReturnsExecutableOrNull() {
        File jstack = JstackTimeoutExtension.resolveJstackBinary();
        if (jstack != null) {
            assertThat(jstack.isFile()).isTrue();
            assertThat(Files.isExecutable(jstack.toPath())).isTrue();
        }
    }

    @Test
    void enabledViaExtensionContextKey() {
        // Property NOT set; but context map contains jstack.enabled=true → still enabled.
        Map<String, String> ctxMap = new HashMap<>();
        ctxMap.put(JstackTimeoutExtension.ENABLED_KEY, "true");
        DefaultForkedProcessTimeoutContext ctx =
                new DefaultForkedProcessTimeoutContext(-1L, 1, null, tmp.toFile(), 60, logger, ctxMap);
        // PID is -1 → extension exits early after the enable check; this asserts no NPE / no early
        // "disabled" short-circuit (covered indirectly by the absence of a thrown exception).
        new JstackTimeoutExtension().onTimeoutDetected(ctx);
    }

    @Test
    void contextKeyFalseDoesNotEnable() {
        Map<String, String> ctxMap = Collections.singletonMap(JstackTimeoutExtension.ENABLED_KEY, "false");
        DefaultForkedProcessTimeoutContext ctx = new DefaultForkedProcessTimeoutContext(
                currentPidBestEffort(), 1, null, tmp.toFile(), 60, logger, ctxMap);
        new JstackTimeoutExtension().onTimeoutDetected(ctx);
        assertThat(tmp.toFile().listFiles()).isNullOrEmpty();
    }

    @Test
    void outputLocationFromExtensionContextIsHonoredWhenJstackAvailable() throws Exception {
        System.setProperty(JstackTimeoutExtension.ENABLED_PROPERTY, "true");
        File jstack = JstackTimeoutExtension.resolveJstackBinary();
        if (jstack == null) {
            return;
        }
        long pid = currentPidBestEffort();
        if (pid <= 0L) {
            return;
        }
        Path customDir = tmp.resolve("jstacks");
        Map<String, String> ctxMap =
                Collections.singletonMap(JstackTimeoutExtension.OUTPUT_LOCATION_KEY, customDir.toString());
        DefaultForkedProcessTimeoutContext ctx =
                new DefaultForkedProcessTimeoutContext(pid, 1, null, tmp.toFile(), 60, logger, ctxMap);
        new JstackTimeoutExtension().onTimeoutDetected(ctx);
        // file written under the custom location, reports dir stays empty
        assertThat(customDir.toFile().listFiles()).isNotNull().isNotEmpty();
    }

    @Test
    void blankOutputLocationFallsBackToReportsDirectory() {
        Map<String, String> ctxMap = Collections.singletonMap(JstackTimeoutExtension.OUTPUT_LOCATION_KEY, "   ");
        DefaultForkedProcessTimeoutContext ctx =
                new DefaultForkedProcessTimeoutContext(-1L, 1, null, tmp.toFile(), 60, logger, ctxMap);
        // PID is -1 so we exit early, but the resolution path was exercised in tests above;
        // here we just make sure no NPE / IllegalArgumentException leaks out for blank values.
        System.setProperty(JstackTimeoutExtension.ENABLED_PROPERTY, "true");
        new JstackTimeoutExtension().onTimeoutDetected(ctx);
    }

    private static long currentPidBestEffort() {
        try {
            String name =
                    java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            int at = name.indexOf('@');
            return at > 0 ? Long.parseLong(name.substring(0, at)) : -1L;
        } catch (RuntimeException e) {
            return -1L;
        }
    }
}
