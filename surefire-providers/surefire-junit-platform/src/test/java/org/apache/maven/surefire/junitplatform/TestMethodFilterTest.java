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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Optional;

import org.apache.maven.surefire.api.testset.TestListResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.reporting.OutputDirectoryProvider;
import org.junit.platform.engine.support.descriptor.MethodSource;

import static org.apache.maven.surefire.api.testset.TestListResolver.toClassFileName;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TestMethodFilter}.
 *
 * @since 2.22.0
 */
public class TestMethodFilterTest {

    private static final OutputDirectoryProvider OUTPUT_DIRECTORY = mock(OutputDirectoryProvider.class);

    private static final ConfigurationParameters CONFIG_PARAMS = mock(ConfigurationParameters.class);

    private final TestListResolver resolver = mock(TestListResolver.class);

    private final TestMethodFilter filter = new TestMethodFilter(this.resolver);

    @Test
    public void includesBasedOnTestListResolver() throws Exception {
        when(resolver.shouldRun(toClassFileName(TestClass.class), "testMethod")).thenReturn(true);

        FilterResult result = filter.apply(newTestMethodDescriptor());

        assertTrue(result.included());
        assertFalse(result.excluded());
    }

    @Test
    public void excludesBasedOnTestListResolver() throws Exception {
        when(resolver.shouldRun(toClassFileName(TestClass.class), "testMethod")).thenReturn(false);

        FilterResult result = filter.apply(newTestMethodDescriptor());

        assertFalse(result.included());
        assertTrue(result.excluded());
    }

    @Test
    public void includesTestDescriptorWithClassSource() {
        FilterResult result = filter.apply(newClassTestDescriptor());

        assertTrue(result.included());
        assertFalse(result.excluded());
    }

    @Test
    public void includesOneRunOfJUnit4ParameterizedTest() {
        TestMethodFilter indexFilter = new TestMethodFilter(new TestListResolver("*TestClass#testMethod[1]"));

        assertTrue(indexFilter.apply(newVintageDescriptor("testMethod[1]")).included());
        assertTrue(indexFilter.apply(newVintageDescriptor("testMethod[0]")).excluded());
        assertTrue(indexFilter.apply(newVintageDescriptor("testMethod[10]")).excluded());
    }

    @Test
    public void includesOneRunOfJUnit4ParameterizedTestWithCustomName() {
        TestMethodFilter indexFilter = new TestMethodFilter(new TestListResolver("*TestClass#testMethod[2:*]"));

        assertTrue(indexFilter
                .apply(newVintageDescriptor("testMethod[2: fib(2)=1]"))
                .included());
        assertTrue(indexFilter
                .apply(newVintageDescriptor("testMethod[3: fib(3)=2]"))
                .excluded());
    }

    @Test
    public void includesEveryRunOfJUnit4ParameterizedTestByMethodName() {
        TestMethodFilter nameFilter = new TestMethodFilter(new TestListResolver("*TestClass#testMethod"));

        assertTrue(nameFilter.apply(newVintageDescriptor("testMethod[0]")).included());
        assertTrue(nameFilter.apply(newVintageDescriptor("testMethod[1]")).included());
    }

    @Test
    public void excludesOneRunOfJUnit4ParameterizedTest() {
        TestMethodFilter excludeFilter = new TestMethodFilter(new TestListResolver("!*TestClass#testMethod[1]"));

        assertTrue(excludeFilter.apply(newVintageDescriptor("testMethod[0]")).included());
        assertTrue(excludeFilter.apply(newVintageDescriptor("testMethod[1]")).excluded());
    }

    /**
     * Mimics a JUnit 4 parameterized test run on the Vintage engine: the method source has the plain
     * method name, the reporting name has the index.
     */
    private static TestDescriptor newVintageDescriptor(String reportingName) {
        TestDescriptor descriptor = mock(TestDescriptor.class);
        when(descriptor.getSource())
                .thenReturn(Optional.<TestSource>of(MethodSource.from(TestClass.class.getName(), "testMethod")));
        when(descriptor.getLegacyReportingName()).thenReturn(reportingName);
        return descriptor;
    }

    private static TestMethodTestDescriptor newTestMethodDescriptor() throws Exception {
        UniqueId uniqueId = UniqueId.forEngine("method");
        Class<TestClass> testClass = TestClass.class;
        Method testMethod = testClass.getMethod("testMethod");
        return new TestMethodTestDescriptor(
                uniqueId,
                testClass,
                testMethod,
                Collections::emptyList,
                new DefaultJupiterConfiguration(CONFIG_PARAMS, OUTPUT_DIRECTORY));
    }

    private static ClassTestDescriptor newClassTestDescriptor() {
        UniqueId uniqueId = UniqueId.forEngine("class");
        return new ClassTestDescriptor(
                uniqueId, TestClass.class, new DefaultJupiterConfiguration(CONFIG_PARAMS, OUTPUT_DIRECTORY));
    }

    /**
     *
     */
    public static class TestClass {
        public void testMethod() {}
    }
}
