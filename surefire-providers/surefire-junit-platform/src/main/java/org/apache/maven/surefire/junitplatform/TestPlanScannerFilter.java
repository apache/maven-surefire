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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.maven.surefire.api.util.ScannerFilter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import static java.util.Collections.emptySet;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * Accepts the scanned classes the JUnit Platform can actually run tests for.
 * <p>
 * All candidate classes are discovered in a <em>single</em> discovery request instead of one request per
 * class. Discovery is not free: engines may execute arbitrary user code while discovering, and the TestNG
 * engine even starts a real TestNG suite because it relies on TestNG's dry-run mode. Discovering once per
 * scanned class multiplied those side effects by the number of test classes.
 *
 * @since 2.22.0
 */
final class TestPlanScannerFilter implements ScannerFilter {

    private final Set<String> classNamesWithTests;

    TestPlanScannerFilter(
            LauncherAdapter launcher, Filter<?>[] includeAndExcludeFilters, Collection<String> candidateClassNames) {
        classNamesWithTests = discoverClassNamesWithTests(launcher, includeAndExcludeFilters, candidateClassNames);
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
            return emptySet();
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

    private static void collectClassNames(
            TestPlan testPlan, TestIdentifier identifier, Set<String> candidates, Set<String> collected) {

        Optional<String> className = className(identifier);
        if (className.isPresent() && candidates.contains(className.get())) {
            collected.add(className.get());
        }
        for (TestIdentifier child : testPlan.getChildren(identifier)) {
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
