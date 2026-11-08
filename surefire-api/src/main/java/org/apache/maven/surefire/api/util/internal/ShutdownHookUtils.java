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
package org.apache.maven.surefire.api.util.internal;

/**
 * Registers and removes JVM shutdown hooks, ignoring the failures that are harmless at that point: the JVM is
 * already shutting down, or a security manager forbids it.
 */
public final class ShutdownHookUtils {

    private ShutdownHookUtils() {}

    public static void addShutDownHook(Thread hook) {
        try {
            Runtime.getRuntime().addShutdownHook(hook);
        } catch (IllegalStateException | SecurityException ignore) {
            // the JVM is shutting down, or hooks are not allowed
        }
    }

    public static void removeShutdownHook(Thread hook) {
        try {
            Runtime.getRuntime().removeShutdownHook(hook);
        } catch (IllegalStateException | SecurityException ignore) {
            // the JVM is shutting down, or hooks are not allowed
        }
    }
}
