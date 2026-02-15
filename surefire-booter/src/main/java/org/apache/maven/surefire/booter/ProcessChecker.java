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
 * Interface for checking if a process (typically the parent Maven plugin) is still alive.
 * <p>
 * Implementations allow the forked JVM to detect when its parent Maven process
 * has terminated, enabling cleanup and preventing orphaned processes.
 *
 * @since 3.5.5
 */
public interface ProcessChecker {

    /**
     * Creates the appropriate {@link ProcessChecker} implementation for the given parent PID.
     * <p>
     * On Java 9+, uses {@code ProcessHandleChecker} which leverages the {@code ProcessHandle} API.
     * On Java 8, falls back to {@link PpidChecker} which uses native commands.
     *
     * @param ppid the parent process ID as a string, or {@code null}
     * @return a new checker instance, or {@code null} if ppid is {@code null}
     */
    static ProcessChecker of(String ppid) {
        if (ppid == null) {
            return null;
        }
        if (ProcessHandleChecker.isAvailable()) {
            return new ProcessHandleChecker(ppid);
        }
        return new PpidChecker(ppid);
    }

    /**
     * Returns whether the ProcessHandle API is available in the current JVM.
     *
     * @return {@code true} if running on Java 9+ with ProcessHandle available
     */
    static boolean isProcessHandleSupported() {
        return ProcessHandleChecker.isAvailable();
    }

    /**
     * Checks whether this checker can be used to monitor the process.
     * <p>
     * This method must return {@code true} before {@link #isProcessAlive()} can be called.
     * @deprecated with using ProcessHandleChecker on Java 9+, this method will always return {@code true} and can be removed in a future release.
     * @return {@code true} if the checker is operational and can monitor the process
     */
    @Deprecated
    boolean canUse();

    /**
     * Checks if the process is still alive.
     * <p>
     * This method can only be called after {@link #canUse()} has returned {@code true}.
     *
     * @return {@code true} if the process is still running; {@code false} if it has terminated
     *         or if the PID has been reused by a different process
     * @throws IllegalStateException if {@link #canUse()} returns {@code false} or if the checker
     *                               has been stopped
     */
    boolean isProcessAlive();

    /**
     * Stops the checker and releases any resources.
     * <p>
     * After calling this method, {@link #canUse()} will return {@code false}.
     */
    void stop();

    /**
     * Destroys any active commands or subprocesses used by this checker.
     * <p>
     * This is called during shutdown to ensure clean termination.
     */
    void destroyActiveCommands();

    /**
     * Checks if the checker has been stopped.
     *
     * @return {@code true} if {@link #stop()} or {@link #destroyActiveCommands()} has been called
     */
    boolean isStopped();

    /**
     * Returns information about the process being checked.
     *
     * @return the process information, or {@code null} if not yet initialized
     */
    ProcessInfo processInfo();
}
