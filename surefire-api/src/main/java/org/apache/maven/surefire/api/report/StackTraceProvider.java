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
package org.apache.maven.surefire.api.report;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for stack trace capture with filtering and truncation to reduce memory consumption.
 * Filters out framework classes and limits to a maximum number of frames.
 *
 * @since 3.6.0
 */
public class StackTraceProvider {

    // 15 frames is enough to capture the test class after surefire framework frames
    // while still providing ~50% memory savings vs unbounded stacks (typically 25-30 frames)
    public static final int DEFAULT_MAX_FRAMES = 15;

    private static volatile int maxFrames = DEFAULT_MAX_FRAMES;

    // Only filter JDK internal classes by default.
    // Framework classes (junit, surefire, etc.) are NOT filtered by default because:
    // 1. Test classes might be in framework packages (e.g., during framework's own tests)
    // 2. The consumer (ConsoleOutputFileReporter) needs to find the test class in the stack
    // Users can add additional prefixes via configuration if needed.
    private static final Set<String> DEFAULT_FRAMEWORK_PREFIXES =
            new HashSet<>(Arrays.asList("java.", "javax.", "sun.", "jdk."));

    private static volatile Set<String> frameworkPrefixes = DEFAULT_FRAMEWORK_PREFIXES;

    /**
     * Configure framework prefixes to filter from stack traces.
     * When specified, this REPLACES the default prefixes (does not add to them).
     * To disable all filtering, pass an empty string.
     * To use defaults, pass null.
     *
     * @param prefixes comma-separated list of package prefixes to filter, or empty to disable filtering
     */
    public static void configure(String prefixes) {
        configure(prefixes, DEFAULT_MAX_FRAMES);
    }

    /**
     * Configure framework prefixes and maximum frame count for stack traces.
     *
     * @param prefixes comma-separated list of package prefixes to filter, or empty to disable filtering, or null
     *                 for defaults
     * @param maxFrameCount maximum number of stack trace frames to capture; 0 or negative disables stack trace capture
     */
    public static void configure(String prefixes, int maxFrameCount) {
        if (prefixes == null) {
            // null means use defaults
            frameworkPrefixes = DEFAULT_FRAMEWORK_PREFIXES;
        } else if (prefixes.trim().isEmpty()) {
            // empty string means no filtering
            frameworkPrefixes = new HashSet<>();
        } else {
            // explicit prefixes replace defaults
            Set<String> customPrefixes = new HashSet<>();
            for (String prefix : prefixes.split(",")) {
                String trimmed = prefix.trim();
                if (!trimmed.isEmpty()) {
                    customPrefixes.add(trimmed);
                }
            }
            frameworkPrefixes = customPrefixes;
        }
        maxFrames = maxFrameCount;
    }

    /**
     * Returns the stack trace as a list of "classname#methodname" strings.
     * Filters out framework classes and limits to {@value #DEFAULT_MAX_FRAMES} frames by default.
     * Returns an empty list if max frames is set to 0 or negative.
     *
     * @return the filtered and truncated stack trace
     */
    static List<String> getStack() {
        if (maxFrames <= 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .filter(e -> !isFrameworkClass(e.getClassName()))
                .limit(maxFrames)
                .map(e -> e.getClassName() + "#" + e.getMethodName())
                .collect(Collectors.toList());
    }

    private static boolean isFrameworkClass(String className) {
        for (String prefix : frameworkPrefixes) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
