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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.maven.surefire.api.util.ReflectionUtils.tryGetMethod;
import static org.apache.maven.surefire.api.util.ReflectionUtils.tryLoadClass;

/**
 * Captures the current thread's stack with {@code java.lang.StackWalker}, the cheaper alternative to
 * {@link Thread#getStackTrace()}.
 * <p>
 * {@code StackWalker} only exists on Java 9+, so we reach it through reflection and let the code still compile and run
 * on Java 8 (callers fall back to {@link Thread#getStackTrace()} when it is not {@link #isAvailable() available}).
 * <p>
 * The win is that {@code StackWalker} is lazy: together with {@link Stream#limit(long)} it stops as soon as it has
 * enough frames instead of building the whole stack, and by default it hides reflection and lambda frames. We ask for
 * an instance with the default options (no {@code RETAIN_CLASS_REFERENCE} &mdash; we only want class and method names)
 * and tell it roughly how many frames to expect so it can size its buffers up front.
 *
 * @since 3.6.0
 */
final class StackWalkerStrategy {

    /** Whether the {@code StackWalker} API is available and reflection setup succeeded. */
    private static final boolean AVAILABLE;

    private static final Method GET_INSTANCE; // StackWalker.getInstance(Set, int) -> StackWalker
    private static final Method WALK; // StackWalker.walk(Function) -> Object
    private static final Method FRAME_CLASS_NAME; // StackWalker.StackFrame.getClassName() -> String
    private static final Method FRAME_METHOD_NAME; // StackWalker.StackFrame.getMethodName() -> String

    static {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        Class<?> stackWalkerClass = tryLoadClass(classLoader, "java.lang.StackWalker");
        // The concrete frame implementation is a non-exported jdk.internal class, so the accessor methods must be
        // resolved on the public StackWalker.StackFrame interface to avoid IllegalAccessException at invoke time.
        Class<?> stackFrameClass = tryLoadClass(classLoader, "java.lang.StackWalker$StackFrame");

        Method getInstance = null;
        Method walk = null;
        Method frameClassName = null;
        Method frameMethodName = null;

        if (stackWalkerClass != null && stackFrameClass != null) {
            getInstance = tryGetMethod(stackWalkerClass, "getInstance", Set.class, int.class);
            walk = tryGetMethod(stackWalkerClass, "walk", Function.class);
            frameClassName = tryGetMethod(stackFrameClass, "getClassName");
            frameMethodName = tryGetMethod(stackFrameClass, "getMethodName");
        }

        AVAILABLE = getInstance != null && walk != null && frameClassName != null && frameMethodName != null;
        GET_INSTANCE = getInstance;
        WALK = walk;
        FRAME_CLASS_NAME = frameClassName;
        FRAME_METHOD_NAME = frameMethodName;
    }

    private StackWalkerStrategy() {
        throw new IllegalStateException("no instantiable constructor");
    }

    /**
     * Returns whether the {@code StackWalker} API is available for use (Java 9+).
     *
     * @return {@code true} if the {@code StackWalker} API is available
     */
    static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * Walks the current thread's stack and returns up to {@code maxFrames} frames as {@code "className#methodName"}
     * strings, excluding frames whose class name matches {@code excludeClass}.
     *
     * @param maxFrames    maximum number of frames to return (after exclusion)
     * @param excludeClass predicate that returns {@code true} for class names to drop from the result
     * @return the filtered, truncated stack, or {@code null} if the {@code StackWalker} call failed and the caller
     *         should fall back to another mechanism
     */
    @SuppressWarnings("unchecked")
    static List<String> walk(int maxFrames, Predicate<String> excludeClass) {
        try {
            Object stackWalker = GET_INSTANCE.invoke(null, Collections.emptySet(), maxFrames);
            Function<Stream<Object>, List<String>> walkFunction = stream -> stream.map(StackWalkerStrategy::toFrameInfo)
                    .filter(frame -> !excludeClass.test(frame.className))
                    .limit(maxFrames)
                    .map(frame -> frame.className + "#" + frame.methodName)
                    .collect(Collectors.toList());
            return (List<String>) WALK.invoke(stackWalker, walkFunction);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static FrameInfo toFrameInfo(Object frame) {
        try {
            String className = (String) FRAME_CLASS_NAME.invoke(frame);
            String methodName = (String) FRAME_METHOD_NAME.invoke(frame);
            return new FrameInfo(className, methodName);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class FrameInfo {
        private final String className;
        private final String methodName;

        private FrameInfo(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }
    }
}
