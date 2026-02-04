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
 * Interface for checking if the parent process (Maven plugin) is still alive.
 * <p>
 * Implementations allow the forked JVM to detect when its parent Maven process
 * has terminated, enabling cleanup and preventing orphaned processes.
 *
 * @since 3.6.0
 */
public interface ParentProcessChecker {

    /**
     * Checks whether this checker can be used to monitor the parent process.
     * <p>
     * This method must return {@code true} before {@link #isProcessAlive()} can be called.
     *
     * @return {@code true} if the checker is operational and can monitor the parent process
     */
    boolean canUse();

    /**
     * Checks if the parent process is still alive.
     * <p>
     * This method can only be called after {@link #canUse()} has returned {@code true}.
     *
     * @return {@code true} if the parent process is still running; {@code false} if it has terminated
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
}
