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

import org.junit.Test;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestFactory;
import org.junit.platform.engine.Filter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TestPlanScannerFilter}.
 *
 * @since 2.22.0
 */
public class TestPlanScannerFilterTest {

    @org.junit.jupiter.api.Test
    public void emptyClassIsNotAccepted() {
        assertFalse(newFilter(EmptyClass.class).accept(EmptyClass.class), "does not accept empty class");
    }

    @org.junit.jupiter.api.Test
    public void classWithNoTestMethodsIsNotAccepted() {
        assertFalse(
                newFilter(ClassWithMethods.class).accept(ClassWithMethods.class),
                "does not accept class with no @Test methods");
    }

    @org.junit.jupiter.api.Test
    public void classWithTestMethodsIsAccepted() {
        assertTrue(newFilter(ClassWithTestMethods.class).accept(ClassWithTestMethods.class));
    }

    @org.junit.jupiter.api.Test
    public void classWithNestedTestClassIsAccepted() {
        assertTrue(newFilter(ClassWithNestedTestClass.class).accept(ClassWithNestedTestClass.class));
    }

    @org.junit.jupiter.api.Test
    public void classWithDeeplyNestedTestClassIsAccepted() {
        assertTrue(newFilter(ClassWithDeeplyNestedTestClass.class).accept(ClassWithDeeplyNestedTestClass.class));
    }

    @org.junit.jupiter.api.Test
    public void classWithTestFactoryIsAccepted() {
        assertTrue(newFilter(ClassWithTestFactory.class).accept(ClassWithTestFactory.class));
    }

    @org.junit.jupiter.api.Test
    public void classWithNestedTestFactoryIsAccepted() {
        assertTrue(newFilter(ClassWithNestedTestFactory.class).accept(ClassWithNestedTestFactory.class));
    }

    @org.junit.jupiter.api.Test
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
                        .collect(toList()));

        assertEquals(1, launcher.discoveries, "discovers all candidate classes at once");
        assertFalse(filter.accept(EmptyClass.class));
        assertFalse(filter.accept(ClassWithMethods.class));
        assertTrue(filter.accept(ClassWithTestMethods.class));
        assertTrue(filter.accept(ClassWithNestedTestClass.class));
        assertTrue(filter.accept(ClassWithTestFactory.class));
    }

    @org.junit.jupiter.api.Test
    public void nothingIsDiscoveredWithoutCandidates() {
        CountingLauncher launcher = new CountingLauncher(LauncherFactory.create());
        TestPlanScannerFilter filter =
                new TestPlanScannerFilter(new LauncherAdapter(launcher, null), new Filter<?>[0], emptyList());

        assertEquals(0, launcher.discoveries, "does not discover anything without candidates");
        assertFalse(filter.accept(ClassWithTestMethods.class));
    }

    @org.junit.jupiter.api.Test
    public void classIsNotAcceptedWhenOnlyAnotherCandidateHasTests() {
        assertFalse(newFilter(EmptyClass.class, ClassWithTestMethods.class).accept(EmptyClass.class));
    }

    private static TestPlanScannerFilter newFilter(Class<?>... candidates) {
        List<String> candidateClassNames =
                Stream.of(candidates).map(Class::getName).collect(toList());
        return new TestPlanScannerFilter(
                new LauncherAdapter(LauncherFactory.create(), null), new Filter<?>[0], candidateClassNames);
    }

    private static final class CountingLauncher implements Launcher {

        private final Launcher delegate;

        private int discoveries;

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

    static class EmptyClass {}

    static class ClassWithMethods {

        void method1() {}

        void method2() {}
    }

    static class ClassWithTestMethods {

        @Test
        void test1() {}

        @org.junit.jupiter.api.Test
        public void test2() {}
    }

    static class ClassWithNestedTestClass {

        void method() {}

        @Nested
        class TestClass {

            @org.junit.jupiter.api.Test
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

                    @org.junit.jupiter.api.Test
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
