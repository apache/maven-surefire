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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestRequest;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.PostDiscoveryFilter;

import static java.util.stream.Collectors.toSet;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.EXCLUDEDGROUPS_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.GROUPS_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.JUNIT_VINTAGE_DETECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@code groups} and {@code excludedGroups} with tests from different engines.
 */
public class JUnitPlatformProviderGroupsTest {

    @Test
    public void excludedGroupsDoNotLookUpMethodsOfOtherEngines() {
        // a Spock feature name is not a Java method (#3468)
        TestDescriptor spockFeature = testDescriptor(
                UniqueId.forEngine("spock").append("feature", "addition works"),
                MethodSource.from(CategorizedTestClass.class.getName(), "addition works"));

        assertTrue(isIncluded(provider(true, null, "slow"), spockFeature));
        assertTrue(isIncluded(provider(false, null, "slow"), spockFeature));
    }

    @Test
    public void excludedGroupsExcludeTaggedTestsOfOtherEnginesWhenVintageIsDetected() {
        // tags must still work with JUnit 4 on the classpath (#3503)
        JUnitPlatformProvider provider = provider(true, null, "slow");

        assertFalse(isIncluded(provider, jupiterTest("slowTest", "slow")));
        assertTrue(isIncluded(provider, jupiterTest("fastTest", "fast")));
        assertTrue(isIncluded(provider, jupiterTest("untaggedTest")));
    }

    @Test
    public void groupsIncludeTaggedTestsOfOtherEnginesWhenVintageIsDetected() {
        JUnitPlatformProvider provider = provider(true, "fast", null);

        assertTrue(isIncluded(provider, jupiterTest("fastTest", "fast")));
        assertFalse(isIncluded(provider, jupiterTest("slowTest", "slow")));
        assertFalse(isIncluded(provider, jupiterTest("untaggedTest")));
    }

    @Test
    public void groupsStillMatchJUnit4CategoriesBySimpleName() {
        TestDescriptor slow = vintageTest("slow");
        TestDescriptor fast = vintageTest("fast");

        JUnitPlatformProvider excluding = provider(true, null, "SlowTests");
        assertFalse(isIncluded(excluding, slow));
        assertTrue(isIncluded(excluding, fast));

        JUnitPlatformProvider including = provider(true, "SlowTests", null);
        assertTrue(isIncluded(including, slow));
        assertFalse(isIncluded(including, fast));

        // vintage engine added by the user
        JUnitPlatformProvider userVintage = provider(false, null, "SlowTests");
        assertFalse(isIncluded(userVintage, slow));
        assertTrue(isIncluded(userVintage, fast));
    }

    @Test
    public void categoryFiltersIgnoreMethodsThatCannotBeFound() {
        TestDescriptor unknown = vintageTest("not a java method");

        assertTrue(isIncluded(provider(true, null, "SlowTests"), unknown));
        assertFalse(isIncluded(provider(true, "SlowTests", null), unknown));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void groupsIncludeTaggedTestsWhenTestNgIsOnTheClasspath(boolean vintageDetected) {
        JUnitPlatformProvider provider = provider(vintageDetected, true, "base", null);

        assertTrue(isIncluded(provider, jupiterTest("baseTest", "base")));
        assertFalse(isIncluded(provider, jupiterTest("otherTest", "other")));
        assertFalse(isIncluded(provider, jupiterTest("untaggedTest")));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void excludedGroupsExcludeTaggedTestsWhenTestNgIsOnTheClasspath(boolean vintageDetected) {
        JUnitPlatformProvider provider = provider(vintageDetected, true, null, "base");

        assertFalse(isIncluded(provider, jupiterTest("baseTest", "base")));
        assertTrue(isIncluded(provider, jupiterTest("otherTest", "other")));
        assertTrue(isIncluded(provider, jupiterTest("untaggedTest")));
    }

    @Test
    public void groupsStillMatchJUnit4CategoriesWhenTestNgIsOnTheClasspath() {
        JUnitPlatformProvider including = provider(true, true, "SlowTests", null);
        assertTrue(isIncluded(including, vintageTest("slow")));
        assertFalse(isIncluded(including, vintageTest("fast")));

        JUnitPlatformProvider excluding = provider(true, true, null, "SlowTests");
        assertFalse(isIncluded(excluding, vintageTest("slow")));
        assertTrue(isIncluded(excluding, vintageTest("fast")));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testNgTestsAreLeftToTestNg(boolean vintageDetected) {
        TestDescriptor slowTestNgTest = testNgTest("slowTest", "slow");

        JUnitPlatformProvider including = provider(vintageDetected, true, "fast", null);
        assertTrue(isIncluded(including, slowTestNgTest));
        assertEquals("fast", including.getConfigurationParameters().get("testng.groups"));

        JUnitPlatformProvider excluding = provider(vintageDetected, true, null, "slow");
        assertTrue(isIncluded(excluding, slowTestNgTest));
        assertEquals("slow", excluding.getConfigurationParameters().get("testng.excludedGroups"));
    }

    @Test
    public void groupsWhichAreNotTagExpressionsOnlyFilterTestNgTests() {
        // fine for TestNG, but not a valid tag expression for JUnit
        JUnitPlatformProvider provider = provider(false, true, "(?!slow).*, fast", null);
        assertTrue(isIncluded(provider, jupiterTest("fastTest", "fast")));
        assertFalse(isIncluded(provider, jupiterTest("slowTest", "slow")));
        assertEquals("(?!slow).*, fast", provider.getConfigurationParameters().get("testng.groups"));

        JUnitPlatformProvider onlyTestNg = provider(true, true, "(?!slow).*", "(?!fast).*");
        assertTrue(isIncluded(onlyTestNg, jupiterTest("slowTest", "slow")));
        assertTrue(isIncluded(onlyTestNg, jupiterTest("untaggedTest")));
    }

    @Test
    public void groupsWhichAreNotTagExpressionsStillFailWithoutTestNg() {
        assertThrows(JUnitException.class, () -> provider(false, false, "(?!slow).*", null));
    }

    @Test
    public void warnsAboutGroupsWhichOnlyTestNgUnderstands() {
        ConsoleLogger logger = mock(ConsoleLogger.class);

        provider(false, true, "(?!slow).*, fast", null).warnAboutTestNgOnlyGroups(logger);

        verify(logger)
                .warning(
                        "These groups are not valid JUnit tag expressions, so they only filter TestNG tests: (?!slow).*");
    }

    @Test
    public void doesNotWarnAboutGroupsWhichAreTagExpressions() {
        ConsoleLogger logger = mock(ConsoleLogger.class);

        // no warning when JUnit can parse the group, even a regular expression
        provider(false, true, "fast, foo\\..*", null).warnAboutTestNgOnlyGroups(logger);

        verify(logger, never()).warning(any());
    }

    private static JUnitPlatformProvider provider(boolean vintageDetected, String groups, String excludedGroups) {
        return provider(vintageDetected, false, groups, excludedGroups);
    }

    private static JUnitPlatformProvider provider(
            boolean vintageDetected, boolean testNgDetected, String groups, String excludedGroups) {
        Map<String, String> properties = new HashMap<>();
        if (vintageDetected) {
            properties.put(JUNIT_VINTAGE_DETECTED, "true");
        }
        if (testNgDetected) {
            properties.put("testng.version", "6.14.3");
        }
        if (groups != null) {
            properties.put(GROUPS_PROP, groups);
        }
        if (excludedGroups != null) {
            properties.put(EXCLUDEDGROUPS_PROP, excludedGroups);
        }
        TestRequest testRequest = mock(TestRequest.class);
        when(testRequest.getTestListResolver()).thenReturn(new TestListResolver(""));
        ProviderParameters parameters = mock(ProviderParameters.class);
        when(parameters.getProviderProperties()).thenReturn(properties);
        when(parameters.getTestRequest()).thenReturn(testRequest);
        return new JUnitPlatformProvider(parameters);
    }

    private static boolean isIncluded(JUnitPlatformProvider provider, TestDescriptor descriptor) {
        return Arrays.stream(provider.getFilters())
                .filter(PostDiscoveryFilter.class::isInstance)
                .map(PostDiscoveryFilter.class::cast)
                .allMatch(filter -> filter.apply(descriptor).included());
    }

    private static TestDescriptor jupiterTest(String name, String... tags) {
        return testDescriptor(
                UniqueId.forEngine("junit-jupiter").append("method", name),
                MethodSource.from(CategorizedTestClass.class.getName(), "fast"),
                tags);
    }

    private static TestDescriptor testNgTest(String name, String... groups) {
        return testDescriptor(
                UniqueId.forEngine("testng").append("method", name),
                MethodSource.from(CategorizedTestClass.class.getName(), "fast"),
                groups);
    }

    private static TestDescriptor vintageTest(String methodName) {
        return testDescriptor(
                UniqueId.forEngine("junit-vintage").append("test", methodName),
                MethodSource.from(CategorizedTestClass.class.getName(), methodName));
    }

    private static TestDescriptor testDescriptor(UniqueId uniqueId, TestSource source, String... tags) {
        return new AbstractTestDescriptor(uniqueId, uniqueId.toString(), source) {
            @Override
            public Type getType() {
                return Type.TEST;
            }

            @Override
            public Set<TestTag> getTags() {
                return Arrays.stream(tags).map(TestTag::create).collect(toSet());
            }
        };
    }

    interface SlowTests {}

    static class CategorizedTestClass {
        @Category(SlowTests.class)
        public void slow() {}

        public void fast() {}
    }
}
