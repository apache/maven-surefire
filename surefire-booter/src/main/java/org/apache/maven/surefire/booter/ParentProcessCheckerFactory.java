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

/**
 * Factory for creating {@link ParentProcessChecker} instances.
 * <p>
 * Automatically selects the best implementation based on the runtime environment:
 * <ul>
 *   <li>On Java 9+: Uses {@code ProcessHandleChecker} with the {@code ProcessHandle} API</li>
 *   <li>On Java 8: Uses {@link PpidChecker} with native commands ({@code ps}/{@code wmic})</li>
 * </ul>
 *
 * @since 3.6.0
 */
public final class ParentProcessCheckerFactory {

    /**
     * Indicates whether the {@code java.lang.ProcessHandle} API is available (Java 9+).
     */
    private static final boolean PROCESS_HANDLE_AVAILABLE = isProcessHandleAvailable();

    private ParentProcessCheckerFactory() {
        // utility class
    }

    /**
     * Creates the appropriate {@link ParentProcessChecker} implementation for the given parent PID.
     * <p>
     * On Java 9+, uses {@code ProcessHandleChecker} which leverages the {@code ProcessHandle} API.
     * On Java 8, falls back to {@link PpidChecker} which uses native commands.
     *
     * @param ppid the parent process ID as a string, or {@code null}
     * @return a new checker instance, or {@code null} if ppid is {@code null}
     */
    public static ParentProcessChecker of(String ppid) {
        if (ppid == null) {
            return null;
        }
        if (PROCESS_HANDLE_AVAILABLE) {
            // Use reflection to avoid loading ProcessHandleChecker on Java 8
            // (which would fail since it references ProcessHandle)
            try {
                return (ParentProcessChecker) Class.forName("org.apache.maven.surefire.booter.ProcessHandleChecker")
                        .getConstructor(String.class)
                        .newInstance(ppid);
            } catch (ReflectiveOperationException e) {
                // Fall back to PpidChecker if ProcessHandleChecker fails to load
            }
        }
        return new PpidChecker(ppid);
    }

    /**
     * Checks if the {@code java.lang.ProcessHandle} class is available at runtime.
     *
     * @return {@code true} if running on Java 9+ with ProcessHandle available
     */
    private static boolean isProcessHandleAvailable() {
        try {
            Class.forName("java.lang.ProcessHandle");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Returns whether the ProcessHandle API is available in the current JVM.
     *
     * @return {@code true} if running on Java 9+ with ProcessHandle available
     */
    public static boolean isProcessHandleSupported() {
        return PROCESS_HANDLE_AVAILABLE;
    }
}
