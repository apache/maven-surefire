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

import static org.apache.maven.surefire.api.util.ReflectionUtils.invokeMethodWithArray;
import static org.apache.maven.surefire.api.util.ReflectionUtils.tryGetMethod;
import static org.apache.maven.surefire.api.util.ReflectionUtils.tryLoadClass;

/**
 * Checks if a process is alive using the ProcessHandle API via reflection.
 * <p>
 * This implementation uses reflection to access the Java 9+ {@code ProcessHandle} API,
 * allowing the class to compile on Java 8 while functioning on Java 9+.
 * <p>
 * The checker detects two scenarios indicating the process is no longer available:
 * <ol>
 *   <li>The process has terminated ({@code ProcessHandle.isAlive()} returns {@code false})</li>
 *   <li>The PID has been reused by the OS for a new process (start time differs from initial)</li>
 * </ol>
 *
 * @since 3.?
 */
final class ProcessHandleChecker implements ProcessChecker {

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

    // Method reference for Instant
    private static final Method INSTANT_TO_EPOCH_MILLI; // Instant.toEpochMilli() -> long

    static {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // Load classes using ReflectionUtils
        Class<?> processHandleClass = tryLoadClass(classLoader, "java.lang.ProcessHandle");
        Class<?> processHandleInfoClass = tryLoadClass(classLoader, "java.lang.ProcessHandle$Info");
        Class<?> optionalClass = tryLoadClass(classLoader, "java.util.Optional");
        Class<?> instantClass = tryLoadClass(classLoader, "java.time.Instant");

        Method processHandleOf = null;
        Method processHandleIsAlive = null;
        Method processHandleInfo = null;
        Method infoStartInstant = null;
        Method optionalIsPresent = null;
        Method optionalGet = null;
        Method optionalOrElse = null;
        Method instantToEpochMilli = null;

        if (processHandleClass != null && processHandleInfoClass != null && optionalClass != null) {
            // ProcessHandle methods
            processHandleOf = tryGetMethod(processHandleClass, "of", long.class);
            processHandleIsAlive = tryGetMethod(processHandleClass, "isAlive");
            processHandleInfo = tryGetMethod(processHandleClass, "info");

            // ProcessHandle.Info methods
            infoStartInstant = tryGetMethod(processHandleInfoClass, "startInstant");

            // Optional methods
            optionalIsPresent = tryGetMethod(optionalClass, "isPresent");
            optionalGet = tryGetMethod(optionalClass, "get");
            optionalOrElse = tryGetMethod(optionalClass, "orElse", Object.class);

            // Instant methods (for processInfo)
            if (instantClass != null) {
                instantToEpochMilli = tryGetMethod(instantClass, "toEpochMilli");
            }
        }

        // All methods must be available for ProcessHandle API to be usable
        AVAILABLE = processHandleOf != null
                && processHandleIsAlive != null
                && processHandleInfo != null
                && infoStartInstant != null
                && optionalIsPresent != null
                && optionalGet != null
                && optionalOrElse != null;

        PROCESS_HANDLE_OF = processHandleOf;
        PROCESS_HANDLE_IS_ALIVE = processHandleIsAlive;
        PROCESS_HANDLE_INFO = processHandleInfo;
        INFO_START_INSTANT = infoStartInstant;
        OPTIONAL_IS_PRESENT = optionalIsPresent;
        OPTIONAL_GET = optionalGet;
        OPTIONAL_OR_ELSE = optionalOrElse;
        INSTANT_TO_EPOCH_MILLI = instantToEpochMilli;
    }

    private final long pid;
    private volatile Object processHandle; // ProcessHandle (stored as Object)
    private volatile Object initialStartInstant; // Instant (stored as Object)
    private volatile boolean stopped;

    /**
     * Creates a new checker for the given process ID.
     *
     * @param pid the process ID as a string
     * @throws NumberFormatException if pid is not a valid long
     */
    ProcessHandleChecker(@Nonnull String pid) {
        this.pid = Long.parseLong(pid);
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
        if (processHandle == null) {
            try {
                // ProcessHandle.of(pid) returns Optional<ProcessHandle>
                Object optionalHandle = invokeMethodWithArray(null, PROCESS_HANDLE_OF, pid);

                // Check if Optional is present
                boolean isPresent = invokeMethodWithArray(optionalHandle, OPTIONAL_IS_PRESENT);
                if (isPresent) {
                    // Get the ProcessHandle from Optional
                    processHandle = invokeMethodWithArray(optionalHandle, OPTIONAL_GET);

                    // Get info and start instant
                    // processHandle.info().startInstant().orElse(null)
                    Object info = invokeMethodWithArray(processHandle, PROCESS_HANDLE_INFO);
                    Object optionalInstant = invokeMethodWithArray(info, INFO_START_INSTANT);
                    initialStartInstant = invokeMethodWithArray(optionalInstant, OPTIONAL_OR_ELSE, (Object) null);

                    return true;
                }
                return false;
            } catch (RuntimeException e) {
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
            // Check if process is still running: processHandle.isAlive()
            boolean isAlive = invokeMethodWithArray(processHandle, PROCESS_HANDLE_IS_ALIVE);
            if (!isAlive) {
                return false;
            }

            // Verify it's the same process (not a reused PID)
            if (initialStartInstant != null) {
                // processHandle.info().startInstant()
                Object info = invokeMethodWithArray(processHandle, PROCESS_HANDLE_INFO);
                Object optionalInstant = invokeMethodWithArray(info, INFO_START_INSTANT);

                boolean isPresent = invokeMethodWithArray(optionalInstant, OPTIONAL_IS_PRESENT);
                if (isPresent) {
                    Object currentStartInstant = invokeMethodWithArray(optionalInstant, OPTIONAL_GET);
                    if (!currentStartInstant.equals(initialStartInstant)) {
                        // PID was reused for a different process
                        return false;
                    }
                }
            }

            return true;
        } catch (RuntimeException e) {
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
    public ProcessInfo processInfo() {
        if (initialStartInstant == null || INSTANT_TO_EPOCH_MILLI == null) {
            return null;
        }
        try {
            long startTimeMillis = invokeMethodWithArray(initialStartInstant, INSTANT_TO_EPOCH_MILLI);
            return ProcessInfo.processHandleInfo(String.valueOf(pid), startTimeMillis);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        String args = "pid=" + pid + ", stopped=" + stopped + ", hasHandle=" + (processHandle != null);
        if (initialStartInstant != null) {
            args += ", startInstant=" + initialStartInstant;
        }
        return "ProcessHandleChecker{" + args + "}";
    }
}
