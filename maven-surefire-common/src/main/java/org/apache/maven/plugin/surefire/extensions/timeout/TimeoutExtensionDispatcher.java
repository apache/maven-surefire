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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.extensions.ForkedProcessTimeoutContext;
import org.apache.maven.surefire.extensions.ForkedProcessTimeoutExtension;

/**
 * Discovers and invokes {@link ForkedProcessTimeoutExtension} instances when a
 * forked test JVM is killed due to {@code forkedProcessTimeoutInSeconds}.
 * <p>
 * Extensions are loaded once per Surefire run via {@link ServiceLoader} from
 * the plugin classloader (the same classloader that loaded the
 * {@link ForkedProcessTimeoutExtension} interface).
 * <p>
 * Each extension callback is invoked on an internal executor and capped at
 * {@link #PER_CALLBACK_TIMEOUT_SECONDS} seconds; any thrown {@link Throwable}
 * or timeout is logged at warn level and never affects the test result.
 *
 * @since 3.6.0
 */
public final class TimeoutExtensionDispatcher {

    /** Maximum wall-clock time allowed per extension callback. */
    public static final int PER_CALLBACK_TIMEOUT_SECONDS = 30;

    private final ConsoleLogger logger;
    private final List<ForkedProcessTimeoutExtension> extensions;
    private final ExecutorService executor;

    public TimeoutExtensionDispatcher(ConsoleLogger logger) {
        this(logger, loadExtensions(logger));
    }

    TimeoutExtensionDispatcher(ConsoleLogger logger, List<ForkedProcessTimeoutExtension> extensions) {
        this.logger = logger;
        this.extensions = Collections.unmodifiableList(new ArrayList<>(extensions));
        this.executor = this.extensions.isEmpty() ? null : Executors.newCachedThreadPool(daemonThreadFactory());
    }

    /**
     * @return {@code true} when at least one extension is registered
     */
    public boolean hasExtensions() {
        return !extensions.isEmpty();
    }

    /**
     * Synchronously invokes {@link ForkedProcessTimeoutExtension#onTimeoutDetected}
     * on every registered extension. Should be called immediately after
     * Surefire detects the timeout and <em>before</em> the kill is sent.
     */
    public void fireTimeoutDetected(final ForkedProcessTimeoutContext context) {
        if (extensions.isEmpty()) {
            return;
        }
        for (final ForkedProcessTimeoutExtension extension : extensions) {
            invokeWithTimeout(extension, "onTimeoutDetected", () -> {
                extension.onTimeoutDetected(context);
                return null;
            });
        }
    }

    /**
     * Synchronously invokes {@link ForkedProcessTimeoutExtension#onForkExited}
     * on every registered extension, after the forked JVM exited.
     */
    public void fireForkExited(final ForkedProcessTimeoutContext context, final RunResult result) {
        if (extensions.isEmpty()) {
            return;
        }
        for (final ForkedProcessTimeoutExtension extension : extensions) {
            invokeWithTimeout(extension, "onForkExited", () -> {
                extension.onForkExited(context, result);
                return null;
            });
        }
    }

    /**
     * Shuts down the internal executor. Safe to call multiple times.
     */
    public void close() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    private void invokeWithTimeout(ForkedProcessTimeoutExtension extension, String callback, Callable<Void> task) {
        Future<Void> future = executor.submit(task);
        try {
            future.get(PER_CALLBACK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.warning("Timeout extension " + extension.getClass().getName() + "#" + callback + " exceeded "
                    + PER_CALLBACK_TIMEOUT_SECONDS + "s and was cancelled");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            logger.warning(
                    "Timeout extension " + extension.getClass().getName() + "#" + callback + " failed: " + cause);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
        }
    }

    private static List<ForkedProcessTimeoutExtension> loadExtensions(ConsoleLogger logger) {
        List<ForkedProcessTimeoutExtension> result = new ArrayList<>();
        try {
            ServiceLoader<ForkedProcessTimeoutExtension> loader = ServiceLoader.load(
                    ForkedProcessTimeoutExtension.class, ForkedProcessTimeoutExtension.class.getClassLoader());
            for (ForkedProcessTimeoutExtension extension : loader) {
                result.add(extension);
            }
        } catch (ServiceConfigurationError | RuntimeException e) {
            logger.warning("Failed to load ForkedProcessTimeoutExtension implementations: " + e);
        }
        return result;
    }

    private static ThreadFactory daemonThreadFactory() {
        return new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "surefire-timeout-extension-" + counter.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
    }
}
