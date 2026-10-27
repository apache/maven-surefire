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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.maven.surefire.api.util.ScannerFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * Accepts the scanned classes the JUnit Platform can actually run tests for.
 * <p>
 * All candidate classes are discovered in a <em>single</em> discovery request instead of one request per
 * class. Discovery is not free: engines may execute arbitrary user code while discovering, and the TestNG
 * engine even starts a real TestNG suite because it relies on TestNG's dry-run mode. Discovering once per
 * scanned class multiplied those side effects by the number of test classes.
 * <p>
 * TestNG test classes skip discovery: {@link TestNgTestClassDetector} spots them and they are accepted as
 * is. The other classes are discovered without the TestNG engine, so the only TestNG dry run left is the
 * one done right before the tests run.
 *
 * @since 2.22.0
 */
final class TestPlanScannerFilter implements ScannerFilter {

    private final Set<String> classNamesWithTests;

    /**
     * @param launcher discovers the classes which are not TestNG test classes
     * @param includeAndExcludeFilters the filters for that discovery
     * @param candidateClassNames the scanned class names
     * @param testNgClassNames the TestNG test classes, accepted without discovery
     */
    TestPlanScannerFilter(
            LauncherAdapter launcher,
            Filter<?>[] includeAndExcludeFilters,
            Collection<String> candidateClassNames,
            Collection<String> testNgClassNames) {
        Set<String> otherClassNames = new LinkedHashSet<>(candidateClassNames);
        otherClassNames.removeAll(testNgClassNames);
        Filter<?>[] filters = includeAndExcludeFilters;
        if (!testNgClassNames.isEmpty()) {
            if (!otherClassNames.isEmpty() && !hasEngineOtherThanTestNg()) {
                // Nothing but TestNG could run these, and they are not TestNG tests.
                otherClassNames.clear();
            }
            filters = Arrays.copyOf(includeAndExcludeFilters, includeAndExcludeFilters.length + 1);
            filters[includeAndExcludeFilters.length] =
                    EngineFilter.excludeEngines(TestNgTestClassDetector.TESTNG_ENGINE_ID);
        }
        classNamesWithTests = discoverClassNamesWithTests(launcher, filters, otherClassNames);
        classNamesWithTests.addAll(testNgClassNames);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean accept(Class testClass) {
        return classNamesWithTests.contains(testClass.getName());
    }

    private static Set<String> discoverClassNamesWithTests(
            LauncherAdapter launcher, Filter<?>[] includeAndExcludeFilters, Collection<String> candidateClassNames) {

        Set<String> candidates = new LinkedHashSet<>(candidateClassNames);
        if (candidates.isEmpty()) {
            return new HashSet<>();
        }

        List<DiscoverySelector> selectors = new ArrayList<>(candidates.size());
        for (String className : candidates) {
            selectors.add(selectClass(className));
        }

        LauncherDiscoveryRequest discoveryRequest =
                request().selectors(selectors).filters(includeAndExcludeFilters).build();

        TestPlan testPlan = launcher.discover(discoveryRequest);

        // The launcher prunes every descriptor which neither contains nor may register tests, so a class
        // which is still part of the test plan is a class the platform has tests for.
        Set<String> discovered = new HashSet<>();
        for (TestIdentifier root : testPlan.getRoots()) {
            collectClassNames(testPlan, root, candidates, discovered);
        }
        return discovered;
    }

    /**
     * Finds the engines the same way the launcher does. If TestNG is the only one, leaving it out would make
     * the launcher warn that all engines were excluded.
     */
    private static boolean hasEngineOtherThanTestNg() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        try {
            for (TestEngine engine : ServiceLoader.load(TestEngine.class, classLoader)) {
                if (!TestNgTestClassDetector.TESTNG_ENGINE_ID.equals(engine.getId())) {
                    return true;
                }
            }
            return false;
        } catch (ServiceConfigurationError | LinkageError e) {
            // Leave broken engines to the launcher, like before.
            return true;
        }
    }

    private static void collectClassNames(
            TestPlan testPlan, TestIdentifier identifier, Set<String> candidates, Set<String> collected) {

        Set<TestIdentifier> children = testPlan.getChildren(identifier);
        Optional<String> className = className(identifier);
        if (className.isPresent()) {
            if (children.isEmpty() && identifier.isContainer() && !identifier.isTest()) {
                // A childless container survives pruning only because it may register tests
                // dynamically, e.g. a class annotated with @ParameterizedClass. A nested class whose
                // own tests only materialize under such a container collapses into this same node
                // during discovery and never gets an identifier of its own, so credit every candidate
                // this node could be standing in for, not just its exact class name.
                String nestedPrefix = className.get() + "$";
                for (String candidate : candidates) {
                    if (candidate.equals(className.get()) || candidate.startsWith(nestedPrefix)) {
                        collected.add(candidate);
                    }
                }
            } else if (candidates.contains(className.get())) {
                collected.add(className.get());
            }
        }
        for (TestIdentifier child : children) {
            collectClassNames(testPlan, child, candidates, collected);
        }
    }

    private static Optional<String> className(TestIdentifier identifier) {
        return identifier.getSource().map(TestPlanScannerFilter::className);
    }

    private static String className(TestSource source) {
        if (source instanceof ClassSource) {
            return ((ClassSource) source).getClassName();
        }
        if (source instanceof MethodSource) {
            return ((MethodSource) source).getClassName();
        }
        return null;
    }
}
