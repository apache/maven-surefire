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
package org.apache.maven.surefire.extensions;

import org.apache.maven.surefire.api.suite.RunResult;

/**
 * Extension point invoked when a forked Surefire test JVM exceeds its
 * configured {@code forkedProcessTimeoutInSeconds}.
 * <p>
 * Implementations are discovered via the standard {@link java.util.ServiceLoader}
 * mechanism from the <em>plugin classpath</em>. To register an extension, add a
 * file
 * {@code META-INF/services/org.apache.maven.surefire.extensions.ForkedProcessTimeoutExtension}
 * to your extension JAR and declare it as a {@code <dependency>} of
 * {@code maven-surefire-plugin} (or {@code maven-failsafe-plugin}) in the
 * project POM.
 * <p>
 * The primary motivation is to capture diagnostic information (for example, a
 * {@code jstack} thread dump) about a test JVM that is about to be killed so
 * the root cause of a deadlock or hang can be investigated.
 * <p>
 * <strong>Lifecycle:</strong>
 * <ol>
 *   <li>{@link #onTimeoutDetected(ForkedProcessTimeoutContext)} is invoked
 *   synchronously immediately after Surefire detects the timeout but
 *   <em>before</em> the {@code KILL} shutdown command is sent. The forked JVM
 *   is therefore still alive at this point, which makes utilities such as
 *   {@code jstack} usable.</li>
 *   <li>{@link #onForkExited(ForkedProcessTimeoutContext, RunResult)} is
 *   invoked from the plugin after the forked JVM has actually exited.</li>
 * </ol>
 * Implementations must not throw checked exceptions; any {@link Throwable}
 * raised by a callback is caught by Surefire, logged at warn level, and does
 * not affect the test result.
 * <p>
 * Callbacks run on a Surefire-internal scheduler thread. Implementations
 * should complete quickly and must not block indefinitely; Surefire applies
 * an internal time limit to each callback invocation.
 *
 * @since 3.6.0
 */
public interface ForkedProcessTimeoutExtension {

    /**
     * Invoked when Surefire detects that a forked JVM has exceeded its
     * timeout, before the JVM is killed.
     * <p>
     * At this point the forked JVM is still running; tools that require a
     * live target process (such as {@code jstack}) may be invoked here.
     *
     * @param context diagnostic information about the forked process; never
     *                {@code null}
     * @throws Exception any failure is logged by Surefire and suppressed
     */
    void onTimeoutDetected(ForkedProcessTimeoutContext context) throws Exception;

    /**
     * Invoked after the forked JVM has exited following a timeout.
     * <p>
     * Useful for cleanup, archival of dumps, or notifying external systems.
     * The forked JVM is no longer alive when this method is invoked.
     *
     * @param context   diagnostic information about the forked process; never
     *                  {@code null}
     * @param runResult the final {@link RunResult} for the fork; never
     *                  {@code null}
     * @throws Exception any failure is logged by Surefire and suppressed
     */
    void onForkExited(ForkedProcessTimeoutContext context, RunResult runResult) throws Exception;
}
