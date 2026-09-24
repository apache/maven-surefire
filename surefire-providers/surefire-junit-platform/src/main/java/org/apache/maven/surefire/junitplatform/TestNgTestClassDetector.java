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
package org.apache.maven.surefire.junitplatform;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

import org.apache.maven.surefire.api.util.ReflectionUtils;

/**
 * Tells whether a class could be a TestNG test class.
 * <p>
 * The TestNG engine starts a real TestNG suite whenever it is asked to discover tests, because it uses
 * TestNG's dry-run mode to tell test classes from other classes. It does so for every class selector it
 * receives, even for classes which are obviously not TestNG tests, and a started suite notifies every
 * registered {@code ISuiteListener}. Surefire therefore keeps the engine out of the discovery request
 * altogether when none of the classes it is about to hand over could be a TestNG test.
 * <p>
 * The detection mirrors the engine's own {@code IsTestNGTestClass}: a class qualifies when
 * {@code org.testng.annotations.Test} is present on the class hierarchy or directly on one of its methods.
 *
 * @since 3.6.1
 */
final class TestNgTestClassDetector {

    static final String TESTNG_ENGINE_ID = "testng";

    private static final String TESTNG_TEST_ANNOTATION = "org.testng.annotations.Test";

    private final Class<? extends Annotation> testAnnotation;

    TestNgTestClassDetector(Class<? extends Annotation> testAnnotation) {
        this.testAnnotation = testAnnotation;
    }

    /**
     * @param testClassLoader the class loader of the tests, may be {@code null}
     * @return a detector, or {@code null} when TestNG is not on the test class path and the engine can
     *     therefore not contribute anything anyway
     */
    static TestNgTestClassDetector tryCreate(ClassLoader testClassLoader) {
        if (testClassLoader == null) {
            return null;
        }
        Class<?> annotation = ReflectionUtils.tryLoadClass(testClassLoader, TESTNG_TEST_ANNOTATION);
        if (annotation == null || !annotation.isAnnotation()) {
            return null;
        }
        return new TestNgTestClassDetector(annotation.asSubclass(Annotation.class));
    }

    /**
     * @param testClassLoader the class loader of the tests
     * @param classNames the names of every class which is about to be handed over to the platform
     * @return {@code true} when the TestNG engine can safely be excluded from the discovery request
     */
    static boolean canExcludeTestNgEngine(ClassLoader testClassLoader, Collection<String> classNames) {
        TestNgTestClassDetector detector = tryCreate(testClassLoader);
        if (detector == null) {
            return false;
        }
        for (String className : classNames) {
            Class<?> testClass = ReflectionUtils.tryLoadClass(testClassLoader, className);
            if (testClass == null || detector.isTestNgTestClass(testClass)) {
                // Unknown classes are left to the engine, only certainty allows to skip it.
                return false;
            }
        }
        return true;
    }

    boolean isTestNgTestClass(Class<?> candidate) {
        try {
            return isAnnotatedInHierarchy(candidate) || hasMethodWithTestAnnotation(candidate);
        } catch (LinkageError e) {
            // The class cannot be inspected, so it cannot be ruled out either.
            return true;
        }
    }

    private boolean isAnnotatedInHierarchy(Class<?> candidate) {
        for (Class<?> clazz = candidate; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            if (clazz.getAnnotation(testAnnotation) != null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasMethodWithTestAnnotation(Class<?> candidate) {
        for (Class<?> clazz = candidate; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            if (hasDeclaredMethodWithTestAnnotation(clazz)) {
                return true;
            }
            for (Class<?> implemented : clazz.getInterfaces()) {
                if (hasDeclaredMethodWithTestAnnotation(implemented)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasDeclaredMethodWithTestAnnotation(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getAnnotation(testAnnotation) != null) {
                return true;
            }
        }
        return false;
    }
}
