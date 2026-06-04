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

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;

/**
 * Information about a forked Surefire test JVM that has reached its configured
 * timeout, passed to {@link ForkedProcessTimeoutExtension} callbacks.
 *
 * @since 3.6.0
 */
public interface ForkedProcessTimeoutContext {

    /**
     * The operating-system PID of the forked JVM, or {@code -1} when the PID
     * cannot be determined (for instance on Java 8 where the
     * {@code ProcessHandle} API is unavailable).
     *
     * @return the forked process PID, or {@code -1} if unknown
     */
    long getPid();

    /**
     * The fork channel id (1-based) assigned by Surefire to this forked JVM.
     *
     * @return the fork number
     */
    int getForkNumber();

    /**
     * The {@code java} executable used to launch the forked JVM, or
     * {@code null} when it cannot be determined.
     *
     * @return the java executable used by the fork, or {@code null}
     */
    File getJavaExecutable();

    /**
     * The Surefire reports directory configured for the current run. Extensions
     * may use this directory to write diagnostic output (thread dumps, etc.).
     *
     * @return the reports directory; never {@code null}
     */
    File getReportsDirectory();

    /**
     * The configured {@code forkedProcessTimeoutInSeconds}.
     *
     * @return the configured timeout in seconds (always greater than 0)
     */
    int getTimeoutSeconds();

    /**
     * Logger that extensions should use for diagnostic output to the Maven
     * console.
     *
     * @return the console logger; never {@code null}
     */
    ConsoleLogger getConsoleLogger();

    /**
     * User-supplied extension configuration, as provided by the
     * {@code forkedProcessTimeoutExtensionContext} Mojo parameter. Extensions
     * may read implementation-specific keys from this map (for instance the
     * built-in jstack extension reads {@code jstack.output.location}).
     * <p>
     * The map is never {@code null} but may be empty. Implementations should
     * treat it as read-only.
     *
     * @return user-supplied extension configuration; never {@code null}
     */
    default Map<String, String> getExtensionContext() {
        return Collections.emptyMap();
    }
}
