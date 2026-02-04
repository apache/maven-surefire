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

import javax.annotation.Nonnull;

import java.time.Instant;
import java.util.Optional;

/**
 * Checks if the parent process (Maven plugin) is alive using the {@link ProcessHandle} API.
 * <p>
 * This implementation uses the Java 9+ {@link ProcessHandle} API to monitor the parent process,
 * providing a cross-platform solution without spawning external processes like {@code ps} or {@code wmic}.
 * <p>
 * The checker detects two scenarios indicating the parent is no longer available:
 * <ol>
 *   <li>The parent process has terminated ({@link ProcessHandle#isAlive()} returns {@code false})</li>
 *   <li>The PID has been reused by the OS for a new process (start time differs from initial)</li>
 * </ol>
 *
 * @since 3.6.0
 */
final class ProcessHandleChecker implements ParentProcessChecker {

    private final long ppid;
    private volatile ProcessHandle parentProcessHandle;
    private volatile Instant initialStartInstant;
    private volatile boolean stopped;

    /**
     * Creates a new checker for the given parent process ID.
     *
     * @param ppid the parent process ID as a string
     * @throws NumberFormatException if ppid is not a valid long
     */
    ProcessHandleChecker(@Nonnull String ppid) {
        this.ppid = Long.parseLong(ppid);
    }

    @Override
    public boolean canUse() {
        if (stopped) {
            return false;
        }
        if (parentProcessHandle == null) {
            Optional<ProcessHandle> handle = ProcessHandle.of(ppid);
            if (handle.isPresent()) {
                parentProcessHandle = handle.get();
                // Store initial start time to detect PID reuse
                initialStartInstant = parentProcessHandle.info().startInstant().orElse(null);
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation checks both that the process is alive and that it's the same process
     * that was originally identified (by comparing start times to detect PID reuse).
     */
    @Override
    public boolean isProcessAlive() {
        if (!canUse()) {
            throw new IllegalStateException("irrelevant to call isProcessAlive()");
        }

        // Check if process is still running
        if (!parentProcessHandle.isAlive()) {
            return false;
        }

        // Verify it's the same process (not a reused PID)
        if (initialStartInstant != null) {
            Optional<Instant> currentStartInstant = parentProcessHandle.info().startInstant();
            if (currentStartInstant.isPresent() && !currentStartInstant.get().equals(initialStartInstant)) {
                // PID was reused for a different process
                return false;
            }
        }

        return true;
    }

    @Override
    public void destroyActiveCommands() {
        stopped = true;
        // No subprocess to destroy - ProcessHandle doesn't spawn processes
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void stop() {
        stopped = true;
    }

    @Override
    public String toString() {
        String args = "ppid=" + ppid + ", stopped=" + stopped + ", hasHandle=" + (parentProcessHandle != null);
        if (initialStartInstant != null) {
            args += ", startInstant=" + initialStartInstant;
        }
        return "ProcessHandleChecker{" + args + "}";
    }
}
