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

import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.surefire.api.util.SurefireReflectionException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.Filter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.core.LauncherFactory;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TestPlanScannerFilter}.
 *
 * @since 2.22.0
 */
public class TestPlanScannerFilterTest {

    @Test
    public void emptyClassIsNotAccepted() {
        assertFalse(newFilter().accept(EmptyClass.class), "does not accept empty class");
    }

    @Test
    public void classWithNoTestMethodsIsNotAccepted() {
        assertFalse(newFilter().accept(ClassWithMethods.class), "does not accept class with no @Test methods");
    }

    @Test
    public void classWithTestMethodsIsAccepted() {
        assertTrue(newFilter().accept(ClassWithTestMethods.class));
    }

    @Test
    public void classWithNestedTestClassIsAccepted() {
        assertTrue(newFilter().accept(ClassWithNestedTestClass.class));
    }

    @Test
    public void classWithDeeplyNestedTestClassIsAccepted() {
        assertTrue(newFilter().accept(ClassWithDeeplyNestedTestClass.class));
    }

    @Test
    public void classWithTestFactoryIsAccepted() {
        assertTrue(newFilter().accept(ClassWithTestFactory.class));
    }

    @Test
    public void classWithNestedTestFactoryIsAccepted() {
        assertTrue(newFilter().accept(ClassWithNestedTestFactory.class));
    }

    /**
     * The tests of a nested {@code @ParameterizedClass} only show up when the outer class runs, so its
     * discovery has nothing named after the nested class. It must still be accepted (#3548).
     */
    @Test
    public void nestedParameterizedClassIsAccepted() {
        assertTrue(newFilter().accept(ParameterizedOuter.Inner.class));
    }

    private static TestPlanScannerFilter newFilter() {
        return new TestPlanScannerFilter(new LauncherAdapter(LauncherFactory.create(), null), new Filter<?>[0]);
    }

    @Test
    public void discoveryFailureIncludesRootCauseMessage() {
        RuntimeException rootCause = new RuntimeException("listener failed during discovery");
        JUnitException original =
                new JUnitException("engine discovery failed", new RuntimeException("wrapper", rootCause));
        StackTraceElement[] stackTrace = {new StackTraceElement("Test", "method", "Test.java", 10)};
        IllegalStateException suppressed = new IllegalStateException("suppressed");
        original.setStackTrace(stackTrace);
        original.addSuppressed(suppressed);
        Launcher launcher = mock(Launcher.class);
        when(launcher.discover(any())).thenThrow(original);
        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new LauncherAdapter(launcher, null).discover(mock(LauncherDiscoveryRequest.class)));

        assertEquals("engine discovery failed: " + rootCause, failure.getMessage());
        assertSame(rootCause, failure.getCause().getCause().getCause());
        assertArrayEquals(original.getStackTrace(), failure.getStackTrace());
        assertSame(suppressed, failure.getSuppressed()[0]);
    }

    @Test
    public void discoveryFailureDoesNotDuplicateRootCauseMessageAndPreservesType() {
        RuntimeException rootCause = new RuntimeException("listener failed during discovery");
        Launcher launcher = mock(Launcher.class);
        when(launcher.discover(any()))
                .thenThrow(new JUnitException("engine discovery failed: listener failed during discovery", rootCause));

        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new TestPlanScannerFilter(new LauncherAdapter(launcher, null), new Filter<?>[0])
                        .accept(ClassWithTestMethods.class));

        assertEquals(
                "engine discovery failed: listener failed during discovery: java.lang.RuntimeException",
                failure.getMessage());
    }

    @Test
    public void discoveryFailureIncludesRootCauseTypeWhenMessageIsNull() {
        IllegalStateException rootCause = new IllegalStateException();
        Launcher launcher = mock(Launcher.class);
        when(launcher.discover(any())).thenThrow(new JUnitException("engine discovery failed", rootCause));

        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new TestPlanScannerFilter(new LauncherAdapter(launcher, null), new Filter<?>[0])
                        .accept(ClassWithTestMethods.class));

        assertEquals("engine discovery failed: " + rootCause.getClass().getName(), failure.getMessage());
        assertSame(rootCause, failure.getCause().getCause());
    }

    @Test
    public void discoveryFailureIncludesClassLoadingExceptionType() {
        ClassNotFoundException rootCause = new ClassNotFoundException("com.acme.MissingTest");
        Launcher launcher = mock(Launcher.class);
        when(launcher.discover(any())).thenThrow(new JUnitException("engine discovery failed", rootCause));

        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new TestPlanScannerFilter(new LauncherAdapter(launcher, null), new Filter<?>[0])
                        .accept(ClassWithTestMethods.class));

        assertEquals("engine discovery failed: " + rootCause, failure.getMessage());
        assertSame(rootCause, failure.getCause().getCause());
    }

    @Test
    public void discoveryFailureIncludesRootCauseTypeWhenMessageIsEmpty() {
        IllegalStateException rootCause = new IllegalStateException("");
        Launcher launcher = mock(Launcher.class);
        when(launcher.discover(any())).thenThrow(new JUnitException("engine discovery failed", rootCause));

        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new TestPlanScannerFilter(new LauncherAdapter(launcher, null), new Filter<?>[0])
                        .accept(ClassWithTestMethods.class));

        assertEquals("engine discovery failed: " + rootCause.getClass().getName(), failure.getMessage());
        assertSame(rootCause, failure.getCause().getCause());
    }

    @Test
    public void discoveryFailureWithoutCauseKeepsOriginalMessage() {
        JUnitException original = new JUnitException("engine discovery failed");
        Launcher launcher = mock(Launcher.class);
        when(launcher.discover(any())).thenThrow(original);

        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new TestPlanScannerFilter(new LauncherAdapter(launcher, null), new Filter<?>[0])
                        .accept(ClassWithTestMethods.class));

        assertEquals("engine discovery failed", failure.getMessage());
    }

    @Test
    public void executionFailureIncludesRootCauseMessage() {
        RuntimeException rootCause = new RuntimeException("listener failed during execution");
        Launcher launcher = mock(Launcher.class);
        doThrow(new JUnitException("engine execution failed", rootCause))
                .when(launcher)
                .execute(any(LauncherDiscoveryRequest.class), any(TestExecutionListener[].class));

        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new LauncherAdapter(launcher, null)
                        .executeWithoutCancellationToken(mock(LauncherDiscoveryRequest.class)));

        assertEquals("engine execution failed: " + rootCause, failure.getMessage());
    }

    @Test
    public void executionFailureIncludesRootCauseMessageWithCancellationToken() {
        assumeTrue(CancellationTokenAdapter.CANCELLATION_TOKEN_CLASS != null);
        RuntimeException rootCause = new RuntimeException("listener failed during execution");
        Launcher launcher = mock(Launcher.class, invocation -> {
            if ("execute".equals(invocation.getMethod().getName())
                    && invocation.getMethod().getParameterCount() == 1) {
                throw new JUnitException("engine execution failed", rootCause);
            }
            return null;
        });

        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new LauncherAdapter(launcher, CancellationTokenAdapter.tryCreate())
                        .execute(mock(LauncherDiscoveryRequest.class)));

        assertEquals("engine execution failed: " + rootCause, failure.getMessage());
    }

    @Test
    public void findsJUnitExceptionNestedInReflectionCause() {
        JUnitException expected = new JUnitException("engine discovery failed", new RuntimeException("root cause"));
        SurefireReflectionException failure =
                new SurefireReflectionException(new IllegalStateException("reflection wrapper", expected));

        assertSame(expected, LauncherAdapter.findJUnitException(failure));
    }

    @Test
    public void executionFailureFindsJUnitExceptionNestedInReflectionCause() {
        assumeTrue(CancellationTokenAdapter.CANCELLATION_TOKEN_CLASS != null);
        RuntimeException rootCause = new RuntimeException("listener failed during execution");
        JUnitException junitException = new JUnitException("engine execution failed", rootCause);
        Launcher launcher = mock(Launcher.class, invocation -> {
            if ("execute".equals(invocation.getMethod().getName())
                    && invocation.getMethod().getParameterCount() == 1) {
                throw new IllegalStateException("reflection wrapper", junitException);
            }
            return null;
        });

        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new LauncherAdapter(launcher, CancellationTokenAdapter.tryCreate())
                        .execute(mock(LauncherDiscoveryRequest.class)));

        assertEquals("engine execution failed: " + rootCause, failure.getMessage());
    }

    @Test
    public void discoveryFailurePreservesRootCauseTypeWithoutDuplicatingMessage() {
        IllegalStateException rootCause = new IllegalStateException("SUREFIRE-3463");
        Launcher launcher = mock(Launcher.class);
        when(launcher.discover(any()))
                .thenThrow(new JUnitException("engine discovery failed: SUREFIRE-3463", rootCause));

        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new TestPlanScannerFilter(new LauncherAdapter(launcher, null), new Filter<?>[0])
                        .accept(ClassWithTestMethods.class));

        assertEquals("engine discovery failed: SUREFIRE-3463: java.lang.IllegalStateException", failure.getMessage());
    }

    static class EmptyClass {}

    static class ClassWithMethods {

        void method1() {}

        void method2() {}
    }

    static class ClassWithTestMethods {

        @org.junit.Test
        void test1() {}

        @Test
        public void test2() {}
    }

    static class ClassWithNestedTestClass {

        void method() {}

        @Nested
        class TestClass {

            @Test
            void test1() {}
        }
    }

    static class ClassWithDeeplyNestedTestClass {

        @Nested
        class Level1 {

            @Nested
            class Level2 {

                @Nested
                class TestClass {

                    @Test
                    void test1() {}
                }
            }
        }
    }

    static class ClassWithTestFactory {

        @TestFactory
        Stream<DynamicTest> tests() {
            return Stream.empty();
        }
    }

    static class ClassWithNestedTestFactory {

        @Nested
        class TestClass {

            @TestFactory
            List<DynamicTest> tests() {
                return emptyList();
            }
        }
    }

    @ParameterizedClass
    @ValueSource(ints = {1, 2})
    static class ParameterizedOuter {

        @Parameter
        int i;

        @Nested
        @ParameterizedClass
        @ValueSource(ints = {1, 2})
        class Inner {

            final int j;

            Inner(int j) {
                this.j = j;
            }

            @Test
            void test() {}
        }
    }
}
