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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.surefire.api.util.SurefireReflectionException;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.Filter;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
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
        assertFalse(newFilter(EmptyClass.class).accept(EmptyClass.class), "does not accept empty class");
    }

    @Test
    public void classWithNoTestMethodsIsNotAccepted() {
        assertFalse(
                newFilter(ClassWithMethods.class).accept(ClassWithMethods.class),
                "does not accept class with no @Test methods");
    }

    @Test
    public void classWithTestMethodsIsAccepted() {
        assertTrue(newFilter(ClassWithTestMethods.class).accept(ClassWithTestMethods.class));
    }

    @Test
    public void classWithNestedTestClassIsAccepted() {
        assertTrue(newFilter(ClassWithNestedTestClass.class).accept(ClassWithNestedTestClass.class));
    }

    @Test
    public void classWithDeeplyNestedTestClassIsAccepted() {
        assertTrue(newFilter(ClassWithDeeplyNestedTestClass.class).accept(ClassWithDeeplyNestedTestClass.class));
    }

    @Test
    public void classWithTestFactoryIsAccepted() {
        assertTrue(newFilter(ClassWithTestFactory.class).accept(ClassWithTestFactory.class));
    }

    @Test
    public void classWithNestedTestFactoryIsAccepted() {
        assertTrue(newFilter(ClassWithNestedTestFactory.class).accept(ClassWithNestedTestFactory.class));
    }

    @Test
    public void allCandidatesAreDiscoveredInASingleRequest() {
        CountingLauncher launcher = new CountingLauncher(LauncherFactory.create());
        TestPlanScannerFilter filter = new TestPlanScannerFilter(
                new LauncherAdapter(launcher, null),
                new Filter<?>[0],
                Stream.of(
                                EmptyClass.class,
                                ClassWithMethods.class,
                                ClassWithTestMethods.class,
                                ClassWithNestedTestClass.class,
                                ClassWithTestFactory.class)
                        .map(Class::getName)
                        .collect(toList()),
                emptyList());

        assertEquals(1, launcher.discoveries, "discovers all candidate classes at once");
        assertFalse(filter.accept(EmptyClass.class));
        assertFalse(filter.accept(ClassWithMethods.class));
        assertTrue(filter.accept(ClassWithTestMethods.class));
        assertTrue(filter.accept(ClassWithNestedTestClass.class));
        assertTrue(filter.accept(ClassWithTestFactory.class));
    }

    @Test
    public void nothingIsDiscoveredWithoutCandidates() {
        CountingLauncher launcher = new CountingLauncher(LauncherFactory.create());
        TestPlanScannerFilter filter = new TestPlanScannerFilter(
                new LauncherAdapter(launcher, null), new Filter<?>[0], emptyList(), emptyList());

        assertEquals(0, launcher.discoveries, "does not discover anything without candidates");
        assertFalse(filter.accept(ClassWithTestMethods.class));
    }

    @Test
    public void classIsNotAcceptedWhenOnlyAnotherCandidateHasTests() {
        assertFalse(newFilter(EmptyClass.class, ClassWithTestMethods.class).accept(EmptyClass.class));
    }

    @Test
    public void testNgClassesAreAcceptedWithoutAskingTheTestNgEngine() {
        CountingLauncher launcher = new CountingLauncher(LauncherFactory.create());
        TestPlanScannerFilter filter = new TestPlanScannerFilter(
                new LauncherAdapter(launcher, null),
                new Filter<?>[0],
                asList(EmptyClass.class.getName(), ClassWithTestMethods.class.getName(), TestNgClass.class.getName()),
                singletonList(TestNgClass.class.getName()));

        assertEquals(1, launcher.discoveries, "still discovers the other classes");
        assertTrue(excludesTestNgEngine(launcher.lastRequest), "the TestNG engine is left out of the discovery");
        assertTrue(filter.accept(TestNgClass.class));
        assertTrue(filter.accept(ClassWithTestMethods.class));
        assertFalse(filter.accept(EmptyClass.class));
    }

    @Test
    public void testNgEngineIsNotExcludedWithoutTestNgClasses() {
        CountingLauncher launcher = new CountingLauncher(LauncherFactory.create());
        new TestPlanScannerFilter(
                new LauncherAdapter(launcher, null),
                new Filter<?>[0],
                singletonList(ClassWithTestMethods.class.getName()),
                emptyList());

        assertFalse(excludesTestNgEngine(launcher.lastRequest));
    }

    @Test
    public void nothingIsDiscoveredWhenTestNgIsTheOnlyEngine() throws Exception {
        CountingLauncher launcher = new CountingLauncher(LauncherFactory.create());
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        // This class loader sees no engine at all, which looks the same as TestNG being the only one.
        try (URLClassLoader noEngines = new URLClassLoader(new URL[0], null)) {
            thread.setContextClassLoader(noEngines);
            TestPlanScannerFilter filter = new TestPlanScannerFilter(
                    new LauncherAdapter(launcher, null),
                    new Filter<?>[0],
                    asList(EmptyClass.class.getName(), TestNgClass.class.getName()),
                    singletonList(TestNgClass.class.getName()));

            assertEquals(0, launcher.discoveries, "no engine is left to ask");
            assertTrue(filter.accept(TestNgClass.class));
            assertFalse(filter.accept(EmptyClass.class));
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    private static boolean excludesTestNgEngine(LauncherDiscoveryRequest request) {
        for (EngineFilter engineFilter : request.getEngineFilters()) {
            if (!engineFilter.isIncludeFilter()
                    && engineFilter.getEngineIds().contains(TestNgTestClassDetector.TESTNG_ENGINE_ID)) {
                return true;
            }
        }
        return false;
    }

    private static TestPlanScannerFilter newFilter(Class<?>... candidates) {
        List<String> candidateClassNames =
                Stream.of(candidates).map(Class::getName).collect(toList());
        return new TestPlanScannerFilter(
                new LauncherAdapter(LauncherFactory.create(), null),
                new Filter<?>[0],
                candidateClassNames,
                emptyList());
    }

    private static final class CountingLauncher implements Launcher {

        private final Launcher delegate;

        private int discoveries;

        private LauncherDiscoveryRequest lastRequest;

        private CountingLauncher(Launcher delegate) {
            this.delegate = delegate;
        }

        @Override
        public void registerLauncherDiscoveryListeners(LauncherDiscoveryListener... listeners) {
            delegate.registerLauncherDiscoveryListeners(listeners);
        }

        @Override
        public void registerTestExecutionListeners(TestExecutionListener... listeners) {
            delegate.registerTestExecutionListeners(listeners);
        }

        @Override
        public TestPlan discover(LauncherDiscoveryRequest launcherDiscoveryRequest) {
            discoveries++;
            lastRequest = launcherDiscoveryRequest;
            return delegate.discover(launcherDiscoveryRequest);
        }

        @Override
        public void execute(LauncherDiscoveryRequest launcherDiscoveryRequest, TestExecutionListener... listeners) {
            delegate.execute(launcherDiscoveryRequest, listeners);
        }

        @Override
        public void execute(TestPlan testPlan, TestExecutionListener... listeners) {
            delegate.execute(testPlan, listeners);
        }
    }

    @Test
    public void discoveryFailureIncludesRootCauseMessage() {
        RuntimeException rootCause = new RuntimeException("listener failed during discovery");
        Launcher launcher = mock(Launcher.class);
        when(launcher.discover(any()))
                .thenThrow(new JUnitException("engine discovery failed", new RuntimeException("wrapper", rootCause)));
        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new TestPlanScannerFilter(
                        new LauncherAdapter(launcher, null),
                        new Filter<?>[0],
                        singletonList(ClassWithTestMethods.class.getName()),
                        emptyList()));

        assertEquals("engine discovery failed: " + rootCause, failure.getMessage());
        assertSame(rootCause, failure.getCause().getCause().getCause());
    }

    @Test
    public void discoveryFailureDoesNotDuplicateRootCauseMessageAndPreservesType() {
        RuntimeException rootCause = new RuntimeException("listener failed during discovery");
        Launcher launcher = mock(Launcher.class);
        when(launcher.discover(any()))
                .thenThrow(new JUnitException("engine discovery failed: listener failed during discovery", rootCause));

        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new TestPlanScannerFilter(
                        new LauncherAdapter(launcher, null),
                        new Filter<?>[0],
                        singletonList(ClassWithTestMethods.class.getName()),
                        emptyList()));

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
                () -> new TestPlanScannerFilter(
                        new LauncherAdapter(launcher, null),
                        new Filter<?>[0],
                        singletonList(ClassWithTestMethods.class.getName()),
                        emptyList()));

        assertEquals("engine discovery failed: " + rootCause.getClass().getName(), failure.getMessage());
        assertSame(rootCause, failure.getCause().getCause());
    }

    @Test
    public void discoveryFailureIncludesRootCauseTypeWhenMessageIsEmpty() {
        IllegalStateException rootCause = new IllegalStateException("");
        Launcher launcher = mock(Launcher.class);
        when(launcher.discover(any())).thenThrow(new JUnitException("engine discovery failed", rootCause));

        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new TestPlanScannerFilter(
                        new LauncherAdapter(launcher, null),
                        new Filter<?>[0],
                        singletonList(ClassWithTestMethods.class.getName()),
                        emptyList()));

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
                () -> new TestPlanScannerFilter(
                        new LauncherAdapter(launcher, null),
                        new Filter<?>[0],
                        singletonList(ClassWithTestMethods.class.getName()),
                        emptyList()));

        assertEquals("engine discovery failed", failure.getMessage());
    }

    @Test
    public void executionFailureIncludesRootCauseMessage() {
        RuntimeException rootCause = new RuntimeException("listener failed during discovery");
        Launcher launcher = mock(Launcher.class);
        doThrow(new JUnitException("engine discovery failed", rootCause))
                .when(launcher)
                .execute(
                        any(LauncherDiscoveryRequest.class),
                        any(org.junit.platform.launcher.TestExecutionListener[].class));

        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new LauncherAdapter(launcher, null)
                        .executeWithoutCancellationToken(mock(LauncherDiscoveryRequest.class)));

        assertEquals("engine discovery failed: " + rootCause, failure.getMessage());
    }

    @Test
    public void executionFailureIncludesRootCauseMessageWithCancellationToken() {
        assumeTrue(CancellationTokenAdapter.CANCELLATION_TOKEN_CLASS != null);
        RuntimeException rootCause = new RuntimeException("listener failed during discovery");
        Launcher launcher = mock(Launcher.class, invocation -> {
            if ("execute".equals(invocation.getMethod().getName())
                    && invocation.getMethod().getParameterCount() == 1) {
                throw new JUnitException("engine discovery failed", rootCause);
            }
            return null;
        });

        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new LauncherAdapter(launcher, CancellationTokenAdapter.tryCreate())
                        .execute(mock(LauncherDiscoveryRequest.class)));

        assertEquals("engine discovery failed: " + rootCause, failure.getMessage());
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
        RuntimeException rootCause = new RuntimeException("listener failed during discovery");
        JUnitException junitException = new JUnitException("engine discovery failed", rootCause);
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

        assertEquals("engine discovery failed: " + rootCause, failure.getMessage());
    }

    @Test
    public void discoveryFailurePreservesRootCauseTypeWithoutDuplicatingMessage() {
        IllegalStateException rootCause = new IllegalStateException("SUREFIRE-3463");
        Launcher launcher = mock(Launcher.class);
        when(launcher.discover(any()))
                .thenThrow(new JUnitException("engine discovery failed: SUREFIRE-3463", rootCause));

        JUnitException failure = assertThrows(
                JUnitException.class,
                () -> new TestPlanScannerFilter(
                        new LauncherAdapter(launcher, null),
                        new Filter<?>[0],
                        singletonList(ClassWithTestMethods.class.getName()),
                        emptyList()));

        assertEquals("engine discovery failed: SUREFIRE-3463: java.lang.IllegalStateException", failure.getMessage());
    }

    static class EmptyClass {}

    /**
     * Plays a TestNG test class. Being in the TestNG list is enough, no annotation needed.
     */
    static class TestNgClass {}

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
}
