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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.extensions.ForkedProcessTimeoutContext;
import org.apache.maven.surefire.extensions.ForkedProcessTimeoutExtension;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TimeoutExtensionDispatcherTest {

    private final NullConsoleLogger logger = new NullConsoleLogger();

    private ForkedProcessTimeoutContext context() {
        return new DefaultForkedProcessTimeoutContext(123L, 2, new File("/java"), new File("."), 60, logger);
    }

    @Test
    void hasExtensionsReportsEmptyList() {
        TimeoutExtensionDispatcher d =
                new TimeoutExtensionDispatcher(logger, Collections.<ForkedProcessTimeoutExtension>emptyList());
        assertThat(d.hasExtensions()).isFalse();
        d.fireTimeoutDetected(context());
        d.fireForkExited(context(), new RunResult(0, 0, 0, 0));
        d.close();
    }

    @Test
    void invokesAllExtensionsForBothCallbacks() {
        AtomicInteger detected = new AtomicInteger();
        AtomicInteger exited = new AtomicInteger();
        ForkedProcessTimeoutExtension ext = new ForkedProcessTimeoutExtension() {
            @Override
            public void onTimeoutDetected(ForkedProcessTimeoutContext ctx) {
                detected.incrementAndGet();
            }

            @Override
            public void onForkExited(ForkedProcessTimeoutContext ctx, RunResult runResult) {
                exited.incrementAndGet();
            }
        };
        List<ForkedProcessTimeoutExtension> exts = Arrays.asList(ext, ext);
        TimeoutExtensionDispatcher d = new TimeoutExtensionDispatcher(logger, exts);
        try {
            d.fireTimeoutDetected(context());
            d.fireForkExited(context(), new RunResult(0, 0, 0, 0));
        } finally {
            d.close();
        }
        assertThat(detected).hasValue(2);
        assertThat(exited).hasValue(2);
    }

    @Test
    void exceptionInOneExtensionDoesNotPreventOthers() {
        final AtomicBoolean secondCalled = new AtomicBoolean();
        ForkedProcessTimeoutExtension throwing = new ForkedProcessTimeoutExtension() {
            @Override
            public void onTimeoutDetected(ForkedProcessTimeoutContext ctx) {
                throw new RuntimeException("boom");
            }

            @Override
            public void onForkExited(ForkedProcessTimeoutContext ctx, RunResult runResult) {}
        };
        ForkedProcessTimeoutExtension ok = new ForkedProcessTimeoutExtension() {
            @Override
            public void onTimeoutDetected(ForkedProcessTimeoutContext ctx) {
                secondCalled.set(true);
            }

            @Override
            public void onForkExited(ForkedProcessTimeoutContext ctx, RunResult runResult) {}
        };
        TimeoutExtensionDispatcher d = new TimeoutExtensionDispatcher(logger, Arrays.asList(throwing, ok));
        try {
            d.fireTimeoutDetected(context());
        } finally {
            d.close();
        }
        assertThat(secondCalled).isTrue();
    }

    @Test
    void contextCarriesAllValues() {
        AtomicReference<ForkedProcessTimeoutContext> received = new AtomicReference<>();
        ForkedProcessTimeoutExtension ext = new ForkedProcessTimeoutExtension() {
            @Override
            public void onTimeoutDetected(ForkedProcessTimeoutContext ctx) {
                received.set(ctx);
            }

            @Override
            public void onForkExited(ForkedProcessTimeoutContext ctx, RunResult runResult) {}
        };
        TimeoutExtensionDispatcher d =
                new TimeoutExtensionDispatcher(logger, Collections.<ForkedProcessTimeoutExtension>singletonList(ext));
        try {
            d.fireTimeoutDetected(context());
        } finally {
            d.close();
        }
        ForkedProcessTimeoutContext got = received.get();
        assertThat(got).isNotNull();
        assertThat(got.getPid()).isEqualTo(123L);
        assertThat(got.getForkNumber()).isEqualTo(2);
        assertThat(got.getJavaExecutable()).isEqualTo(new File("/java"));
        assertThat(got.getReportsDirectory()).isEqualTo(new File("."));
        assertThat(got.getTimeoutSeconds()).isEqualTo(60);
    }

    @Test
    void blockingExtensionDoesNotLeakAfterClose() throws Exception {
        final CountDownLatch entered = new CountDownLatch(1);
        ForkedProcessTimeoutExtension blocker = new ForkedProcessTimeoutExtension() {
            @Override
            public void onTimeoutDetected(ForkedProcessTimeoutContext ctx) {
                entered.countDown();
                try {
                    Thread.sleep(TimeUnit.MINUTES.toMillis(5));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            @Override
            public void onForkExited(ForkedProcessTimeoutContext ctx, RunResult runResult) {}
        };
        TimeoutExtensionDispatcher d = new TimeoutExtensionDispatcher(
                logger, Collections.<ForkedProcessTimeoutExtension>singletonList(blocker));
        try {
            Thread t = new Thread(() -> d.fireTimeoutDetected(context()));
            t.setDaemon(true);
            t.start();
            assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            d.close();
        }
    }

    @Test
    void extensionContextMapFlowsThroughContext() {
        AtomicReference<Map<String, String>> seen = new AtomicReference<>();
        ForkedProcessTimeoutExtension ext = new ForkedProcessTimeoutExtension() {
            @Override
            public void onTimeoutDetected(ForkedProcessTimeoutContext ctx) {
                seen.set(ctx.getExtensionContext());
            }

            @Override
            public void onForkExited(ForkedProcessTimeoutContext ctx, RunResult runResult) {}
        };
        Map<String, String> config = Collections.singletonMap("jstack.output.location", "/tmp/dumps");
        ForkedProcessTimeoutContext ctx =
                new DefaultForkedProcessTimeoutContext(123L, 2, new File("/java"), new File("."), 60, logger, config);
        TimeoutExtensionDispatcher d =
                new TimeoutExtensionDispatcher(logger, Collections.<ForkedProcessTimeoutExtension>singletonList(ext));
        try {
            d.fireTimeoutDetected(ctx);
        } finally {
            d.close();
        }
        assertThat(seen.get()).isNotNull().containsEntry("jstack.output.location", "/tmp/dumps");
    }

    @Test
    void extensionContextDefaultsToEmptyMap() {
        ForkedProcessTimeoutContext ctx =
                new DefaultForkedProcessTimeoutContext(1L, 1, null, new File("."), 60, logger);
        assertThat(ctx.getExtensionContext()).isNotNull().isEmpty();
    }
}
