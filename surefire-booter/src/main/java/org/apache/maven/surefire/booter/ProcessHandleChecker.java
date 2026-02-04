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

import java.lang.reflect.Method;

/**
 * Checks if the parent process (Maven plugin) is alive using the ProcessHandle API via reflection.
 * <p>
 * This implementation uses reflection to access the Java 9+ {@code ProcessHandle} API,
 * allowing the class to compile on Java 8 while functioning on Java 9+.
 * <p>
 * The checker detects two scenarios indicating the parent is no longer available:
 * <ol>
 *   <li>The parent process has terminated ({@code ProcessHandle.isAlive()} returns {@code false})</li>
 *   <li>The PID has been reused by the OS for a new process (start time differs from initial)</li>
 * </ol>
 *
 * @since ?
 */
final class ProcessHandleChecker implements ParentProcessChecker {

    // ============ Static reflection metadata ============

    /** Whether ProcessHandle API is available and reflection setup succeeded */
    private static final boolean AVAILABLE;

    // Method references for ProcessHandle
    private static final Method PROCESS_HANDLE_OF; // ProcessHandle.of(long) -> Optional<ProcessHandle>
    private static final Method PROCESS_HANDLE_IS_ALIVE; // ProcessHandle.isAlive() -> boolean
    private static final Method PROCESS_HANDLE_INFO; // ProcessHandle.info() -> ProcessHandle.Info

    // Method references for ProcessHandle.Info
    private static final Method INFO_START_INSTANT; // ProcessHandle.Info.startInstant() -> Optional<Instant>

    // Method references for Optional
    private static final Method OPTIONAL_IS_PRESENT; // Optional.isPresent() -> boolean
    private static final Method OPTIONAL_GET; // Optional.get() -> Object
    private static final Method OPTIONAL_OR_ELSE; // Optional.orElse(Object) -> Object

    static {
        boolean available = false;
        Method processHandleOf = null;
        Method processHandleIsAlive = null;
        Method processHandleInfo = null;
        Method infoStartInstant = null;
        Method optionalIsPresent = null;
        Method optionalGet = null;
        Method optionalOrElse = null;

        try {
            // Load classes
            Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
            Class<?> processHandleInfoClass = Class.forName("java.lang.ProcessHandle$Info");
            Class<?> optionalClass = Class.forName("java.util.Optional");

            // ProcessHandle methods
            processHandleOf = processHandleClass.getMethod("of", long.class);
            processHandleIsAlive = processHandleClass.getMethod("isAlive");
            processHandleInfo = processHandleClass.getMethod("info");

            // ProcessHandle.Info methods
            infoStartInstant = processHandleInfoClass.getMethod("startInstant");

            // Optional methods
            optionalIsPresent = optionalClass.getMethod("isPresent");
            optionalGet = optionalClass.getMethod("get");
            optionalOrElse = optionalClass.getMethod("orElse", Object.class);

            available = true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // ProcessHandle not available (Java 8) or API changed
            // Leave available = false, factory will use PpidChecker
        }

        AVAILABLE = available;
        PROCESS_HANDLE_OF = processHandleOf;
        PROCESS_HANDLE_IS_ALIVE = processHandleIsAlive;
        PROCESS_HANDLE_INFO = processHandleInfo;
        INFO_START_INSTANT = infoStartInstant;
        OPTIONAL_IS_PRESENT = optionalIsPresent;
        OPTIONAL_GET = optionalGet;
        OPTIONAL_OR_ELSE = optionalOrElse;
    }

    // ============ Instance fields ============

    private final long ppid;
    private volatile Object parentProcessHandle; // ProcessHandle (stored as Object)
    private volatile Object initialStartInstant; // Instant (stored as Object)
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

    /**
     * Returns whether the ProcessHandle API is available for use.
     * This is a static check that can be used by the factory.
     *
     * @return true if ProcessHandle API is available (Java 9+)
     */
    static boolean isAvailable() {
        return AVAILABLE;
    }

    @Override
    public boolean canUse() {
        if (!AVAILABLE || stopped) {
            return false;
        }
        if (parentProcessHandle == null) {
            try {
                // ProcessHandle.of(ppid) returns Optional<ProcessHandle>
                Object optionalHandle = PROCESS_HANDLE_OF.invoke(null, ppid);

                // Check if Optional is present
                boolean isPresent = (Boolean) OPTIONAL_IS_PRESENT.invoke(optionalHandle);
                if (isPresent) {
                    // Get the ProcessHandle from Optional
                    parentProcessHandle = OPTIONAL_GET.invoke(optionalHandle);

                    // Get info and start instant
                    // parentProcessHandle.info().startInstant().orElse(null)
                    Object info = PROCESS_HANDLE_INFO.invoke(parentProcessHandle);
                    Object optionalInstant = INFO_START_INSTANT.invoke(info);
                    initialStartInstant = OPTIONAL_OR_ELSE.invoke(optionalInstant, (Object) null);

                    return true;
                }
                return false;
            } catch (Exception e) {
                // Reflection failed - treat as unavailable
                return false;
            }
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

        try {
            // Check if process is still running: parentProcessHandle.isAlive()
            boolean isAlive = (Boolean) PROCESS_HANDLE_IS_ALIVE.invoke(parentProcessHandle);
            if (!isAlive) {
                return false;
            }

            // Verify it's the same process (not a reused PID)
            if (initialStartInstant != null) {
                // parentProcessHandle.info().startInstant()
                Object info = PROCESS_HANDLE_INFO.invoke(parentProcessHandle);
                Object optionalInstant = INFO_START_INSTANT.invoke(info);

                boolean isPresent = (Boolean) OPTIONAL_IS_PRESENT.invoke(optionalInstant);
                if (isPresent) {
                    Object currentStartInstant = OPTIONAL_GET.invoke(optionalInstant);
                    if (!currentStartInstant.equals(initialStartInstant)) {
                        // PID was reused for a different process
                        return false;
                    }
                }
            }

            return true;
        } catch (Exception e) {
            // Reflection failed during runtime - treat as process not alive
            return false;
        }
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
