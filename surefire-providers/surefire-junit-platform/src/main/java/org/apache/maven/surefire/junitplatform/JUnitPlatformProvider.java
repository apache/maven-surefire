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

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.booter.ProviderParameterNames;
import org.apache.maven.surefire.api.provider.AbstractProvider;
import org.apache.maven.surefire.api.provider.CommandChainReader;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.ReporterException;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.report.Stoppable;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.ReflectionUtils;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.apache.maven.surefire.shared.utils.StringUtils;
import org.apache.maven.surefire.shared.utils.io.SelectorUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

import static java.util.Arrays.stream;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.logging.Level.WARNING;
import static java.util.stream.Collectors.toList;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.EXCLUDEDGROUPS_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.EXCLUDE_JUNIT5_ENGINES_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.GROUPS_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.INCLUDE_JUNIT5_ENGINES_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.JUNIT_VINTAGE_DETECTED;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.RUN_ORDER_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.RUN_ORDER_RANDOM_SEED_PROP;
import static org.apache.maven.surefire.api.report.ConsoleOutputCapture.startCapture;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.report.RunMode.RERUN_TEST_AFTER_FAILURE;
import static org.apache.maven.surefire.api.testset.TestListResolver.optionallyWildcardFilter;
import static org.apache.maven.surefire.api.util.TestsToRun.fromClass;
import static org.apache.maven.surefire.api.util.internal.ConcurrencyUtils.runIfZeroCountDown;
import static org.apache.maven.surefire.shared.utils.StringUtils.isBlank;
import static org.apache.maven.surefire.shared.utils.io.SelectorUtils.match;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectUniqueId;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

/**
 * JUnit 5 Platform Provider.
 *
 * @since 2.22.0
 */
public class JUnitPlatformProvider extends AbstractProvider {
    static final String CONFIGURATION_PARAMETERS = "configurationParameters";

    private static final String JUNIT_RANDOM_SEED = "junit.jupiter.execution.order.random.seed";

    private final ProviderParameters parameters;

    private final LauncherSessionFactory launcherSessionFactory;

    private final Filter<?>[] filters;

    private Map<String, String> configurationParameters = new HashMap<>();

    private final CommandChainReader commandsReader;

    public JUnitPlatformProvider(ProviderParameters parameters) {
        this(parameters, LauncherSessionFactory.DEFAULT);
    }

    JUnitPlatformProvider(ProviderParameters parameters, LauncherSessionFactory launcherSessionFactory) {
        this.parameters = parameters;
        this.launcherSessionFactory = launcherSessionFactory;
        filters = newFilters();
        setupRunOrder();
        parameters.getProviderProperties().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("junit.vintage.execution.parallel"))
                .forEach(entry -> getConfigurationParameters().put(entry.getKey(), entry.getValue()));
        getConfigurationParameters().putAll(newConfigurationParameters());

        // don't start a thread in CommandReader while we are in in-plugin process
        commandsReader = parameters.isInsideFork() ? parameters.getCommandReader() : null;

        parameters.getProviderProperties().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("testng."))
                .forEach(entry -> getConfigurationParameters().put(entry.getKey(), entry.getValue()));
        // testng compatibility parameters
        String groups = parameters.getProviderProperties().get(GROUPS_PROP);
        if (groups != null) {
            getConfigurationParameters().put("testng.groups", groups);
        }

        //        configurationParameters.put("testng.useDefaultListeners", "true");

        Optional.ofNullable(parameters.getProviderProperties().get("listener"))
                .ifPresent(listener -> getConfigurationParameters().put("testng.listeners", listener));

        Optional.ofNullable(parameters.getProviderProperties().get("reporter"))
                .ifPresent(reporter -> getConfigurationParameters()
                        .compute(
                                "testng.listeners", (key, value) -> value == null ? reporter : value + "," + reporter));

        String excludeGroups = parameters.getProviderProperties().get(EXCLUDEDGROUPS_PROP);
        if (excludeGroups != null) {
            getConfigurationParameters().put("testng.excludedGroups", excludeGroups);
        }

        // dataproviderthreadcount
        Optional.ofNullable(parameters.getProviderProperties().get("dataproviderthreadcount"))
                .ifPresent(dataproviderthreadcount ->
                        getConfigurationParameters().put("testng.dataProviderThreadCount", dataproviderthreadcount));
    }

    private void setupRunOrder() {
        String runOrder = parameters.getProviderProperties().get(RUN_ORDER_PROP);
        if (runOrder != null) {
            if (runOrder.equals("random")) {
                getConfigurationParameters()
                        .put("junit.jupiter.testmethod.order.default", "org.junit.jupiter.api.MethodOrderer$Random");
                getConfigurationParameters()
                        .put("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer$Random");
                Optional.ofNullable(parameters.getProviderProperties().get(RUN_ORDER_RANDOM_SEED_PROP))
                        .ifPresent(seed -> getConfigurationParameters().put(JUNIT_RANDOM_SEED, seed));
            } else if (runOrder.equals("alphabetical")) {
                getConfigurationParameters()
                        .put(
                                "junit.jupiter.testmethod.order.default",
                                "org.junit.jupiter.api.MethodOrderer$MethodName");
                getConfigurationParameters()
                        .put("junit.jupiter.testclass.order.default", "org.junit.jupiter.api.ClassOrderer$ClassName");
            } else if (runOrder.equals("reversealphabetical")) {
                getConfigurationParameters()
                        .put(
                                "junit.jupiter.testmethod.order.default",
                                "org.apache.maven.surefire.junitplatform.ReverseOrdering$ReverseMethodOrder");
                getConfigurationParameters()
                        .put(
                                "junit.jupiter.testclass.order.default",
                                "org.apache.maven.surefire.junitplatform.ReverseOrdering$ReverseClassOrder");
            }
        }
    }

    @Override
    public Iterable<Class<?>> getSuites() {
        try (LauncherSessionAdapter launcherSession = launcherSessionFactory.openSession()) {
            return scanClasspath(launcherSession.getLauncher());
        }
    }

    @Override
    public RunResult invoke(Object forkTestSet) throws TestSetFailedException, ReporterException {
        ReporterFactory reporterFactory = parameters.getReporterFactory();
        final RunResult runResult;
        TestReportListener<TestOutputReportEntry> runListener = reporterFactory.createTestReportListener();
        CancellationTokenAdapter cancellationToken = CancellationTokenAdapter.tryCreate();
        Stoppable stoppable = prepareFailFastSupport(cancellationToken, runListener);
        RunListenerAdapter adapter = new RunListenerAdapter(runListener, stoppable);
        adapter.setRunMode(NORMAL_RUN);
        startCapture(adapter);
        setupJunitLogger();

        try (LauncherSessionAdapter launcherSession = launcherSessionFactory.openSession(cancellationToken)) {
            LauncherAdapter launcher = launcherSession.getLauncher();
            if (forkTestSet instanceof TestsToRun) {
                invokeAllTests(launcher, (TestsToRun) forkTestSet, adapter);
            } else if (forkTestSet instanceof Class) {
                invokeAllTests(launcher, fromClass((Class<?>) forkTestSet), adapter);
            } else if (forkTestSet == null) {
                invokeAllTests(launcher, scanClasspath(launcher), adapter);
            } else {
                throw new IllegalArgumentException("Unexpected value of forkTestSet: " + forkTestSet);
            }
        } finally {
            runResult = reporterFactory.close();
        }
        return runResult;
    }

    private static void setupJunitLogger() {
        Logger logger = Logger.getLogger("org.junit");
        if (logger.getLevel() == null) {
            logger.setLevel(WARNING);
        }
    }

    private TestsToRun scanClasspath(LauncherAdapter launcher) {
        TestPlanScannerFilter filter = new TestPlanScannerFilter(launcher, filters);
        ScanResult scanResult = parameters.getScanResult();
        TestsToRun scannedClasses = scanResult.applyFilter(filter, parameters.getTestClassLoader());
        return parameters.getRunOrderCalculator().orderTestClasses(scannedClasses);
    }

    private void invokeAllTests(LauncherAdapter launcher, TestsToRun testsToRun, RunListenerAdapter adapter)
            throws TestSetFailedException {

        if (commandsReader != null) {
            commandsReader.addShutdownListener(__ -> testsToRun.markTestSetFinished());
            commandsReader.awaitStarted();
        }

        execute(launcher, testsToRun, adapter);

        // Rerun failing tests if requested
        int count = parameters.getTestRequest().getRerunFailingTestsCount();
        if (count > 0 && adapter.hasFailingTests()) {
            adapter.setRunMode(RERUN_TEST_AFTER_FAILURE);
            for (int i = 0; i < count; i++) {
                // Replace the "discoveryRequest" so that it only specifies the failing tests
                LauncherDiscoveryRequest discoveryRequest = buildLauncherDiscoveryRequestForRerunFailures(adapter);
                // Reset adapter's recorded failures and invoke the failed tests again
                adapter.reset();
                launcher.executeWithoutCancellationToken(discoveryRequest, adapter);
                // If no tests fail in the rerun, we're done
                if (!adapter.hasFailingTests()) {
                    break;
                }
            }
        }
    }

    private void execute(LauncherAdapter launcher, TestsToRun testsToRun, RunListenerAdapter adapter) {
        List<TestExecutionListener> testExecutionListeners = new ArrayList<>();
        testExecutionListeners.add(adapter);
        testExecutionListeners.addAll(createTestExecutionListeners());

        if (testsToRun.allowEagerReading()) {
            List<DiscoverySelector> selectors = new ArrayList<>();
            testsToRun.iterator().forEachRemaining(c -> selectors.add(selectClass(c.getName())));

            LauncherDiscoveryRequestBuilder builder = newRequest().selectors(selectors);
            launcher.execute(builder.build(), testExecutionListeners.toArray(new TestExecutionListener[0]));
        } else {
            testsToRun.iterator().forEachRemaining(c -> {
                LauncherDiscoveryRequestBuilder builder = newRequest().selectors(selectClass(c.getName()));
                launcher.execute(builder.build(), testExecutionListeners.toArray(new TestExecutionListener[0]));
            });
        }
    }

    /**
     * Instantiates the listeners configured through the {@code listener} provider property.
     * A class name may refer to a JUnit Platform {@link TestExecutionListener}, a JUnit 4
     * {@code org.junit.runner.notification.RunListener}, or a TestNG listener. TestNG listeners
     * are ignored here because they are forwarded to the TestNG engine through the
     * {@code testng.listeners} configuration parameter instead.
     *
     * @return the configured JUnit Platform {@link TestExecutionListener}s, or an empty list
     *     if none were declared
     */
    private List<TestExecutionListener> createTestExecutionListeners() {
        String listeners = parameters.getProviderProperties().get("listener");
        if (listeners == null) {
            return Collections.emptyList();
        }
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        List<Object> runListeners = new ArrayList<>();
        List<TestExecutionListener> testExecutionListeners = new ArrayList<>();
        for (String listener : stream(listeners.split(","))
                .map(String::trim)
                .filter(trimmed -> !trimmed.isEmpty())
                .collect(toList())) {
            TestExecutionListener testExecutionListener = instantiateTestExecutionListener(cl, listener);
            if (testExecutionListener != null) {
                testExecutionListeners.add(testExecutionListener);
            } else {
                Object runListener = instantiateJUnit4RunListener(cl, listener);
                if (runListener != null) {
                    runListeners.add(runListener);
                }
                // otherwise ignored as we may be in a TestNG context
            }
        }
        if (!runListeners.isEmpty()) {
            testExecutionListeners.add(new JUnit4ListenersAdapter(runListeners));
        }
        return testExecutionListeners;
    }

    private TestExecutionListener instantiateTestExecutionListener(ClassLoader cl, String listener) {
        try {
            Class<?> listenerClass = cl.loadClass(listener);
            if (!TestExecutionListener.class.isAssignableFrom(listenerClass)) {
                return null;
            }
            return ReflectionUtils.instantiate(cl, listener, TestExecutionListener.class);
        } catch (ClassNotFoundException e) {
            // the configured class is not on the classpath
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object instantiateJUnit4RunListener(ClassLoader cl, String listener) {
        try {
            Class<?> runListenerClass = cl.loadClass("org.junit.runner.notification.RunListener");
            return ReflectionUtils.instantiate(cl, listener, runListenerClass);
        } catch (ClassCastException | ClassNotFoundException c) {
            // ignored as we may be in not-JUnit 4 context like testng
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    LauncherDiscoveryRequest buildLauncherDiscoveryRequestForRerunFailures(RunListenerAdapter adapter) {
        LauncherDiscoveryRequestBuilder builder = newRequest();
        LinkedHashSet<TestIdentifier> failures =
                new LinkedHashSet<>(adapter.getFailures().keySet());
        LinkedHashSet<UniqueId> failureIds = failures.stream()
                .map(TestIdentifier::getUniqueId)
                .map(UniqueId::parse)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        adapter.setRerunTestIds(failureIds);
        LinkedHashSet<String> classNames = new LinkedHashSet<>();

        for (TestIdentifier identifier : failures) {
            // Runner-backed Vintage tests may expose only a ClassSource and may not
            // support rediscovery from a leaf UniqueId. Rediscover their owning class
            // and retain the failed branch with a post-discovery filter instead.
            Optional<ClassSource> classSource =
                    identifier.getSource().filter(ClassSource.class::isInstance).map(ClassSource.class::cast);
            if (classSource.isPresent()) {
                classNames.add(classSource.get().getClassName());
            } else {
                builder.selectors(selectUniqueId(identifier.getUniqueId()));
            }
        }

        if (!classNames.isEmpty()) {
            classNames.forEach(className -> builder.selectors(selectClass(className)));
            builder.filters((PostDiscoveryFilter) testDescriptor -> {
                UniqueId candidateId = testDescriptor.getUniqueId();
                boolean isFailureOrRelatedContainer = failureIds.stream()
                        .anyMatch(failureId -> candidateId.hasPrefix(failureId) || failureId.hasPrefix(candidateId));
                return FilterResult.includedIf(
                        isFailureOrRelatedContainer,
                        () -> "Failed test or related container",
                        () -> "Not a failed test");
            });
        }
        return builder.build();
    }

    private LauncherDiscoveryRequestBuilder newRequest() {

        return request().filters(filters).configurationParameters(getConfigurationParameters());
    }

    private boolean matchClassName(String className, String pattern) {
        boolean reverse = pattern.startsWith("!");
        if (reverse) {
            pattern = pattern.substring(1);
        }
        // pattern can be either fully qualified or simple class name or package + simple class name + #method
        int hashIndex = pattern.indexOf('#');
        // we receive only -Dtest=#method (weird but possible)
        if (hashIndex == 0) {
            return true;
        }
        if (hashIndex != -1) {
            pattern = pattern.substring(0, hashIndex);
        }

        boolean match = className.endsWith("." + pattern)
                || SelectorUtils.matchPath(pattern, className)
                || matchAntPathPattern(pattern, className, true);

        if (className.contains(".")) {
            String simpleName = className.substring(className.lastIndexOf('.') + 1);
            match = match || SelectorUtils.matchPath(pattern, simpleName);
        }

        if (pattern.contains("/")) {
            String pkgStylePattern = pattern.replace('/', '.');
            match = match || SelectorUtils.matchPath(pkgStylePattern, className);
        }

        boolean testMatch = match
                || className.equals(pattern)
                || className.endsWith("." + pattern)
                || SelectorUtils.matchPath(pattern, className);
        return reverse != testMatch;
    }

    // TODO this could be simplified/optimized
    private Filter<?>[] newFilters() {
        List<Filter<?>> filters = new ArrayList<>();

        // includeClassNamePatterns support only regex patterns
        Optional<String> includesList =
                Optional.ofNullable(parameters.getProviderProperties().get(ProviderParameterNames.INCLUDES_SCAN_LIST));
        Set<String> enclosingClassNames = includesList.isPresent() ? getEnclosingClassNames() : Collections.emptySet();
        if (includesList.isPresent()) {
            String[] includesRegex = Stream.of(includesList.get().split(","))
                    .filter(s -> s.startsWith("%regex["))
                    .map(s -> StringUtils.replace(s, "%regex[", ""))
                    .map(s -> s.substring(0, s.length() - 1))
                    .toArray(String[]::new);
            if (includesRegex.length > 0) {
                filters.add(includeEnclosingClasses(
                        ClassNameFilter.includeClassNamePatterns(includesRegex), enclosingClassNames));
            }
        }

        // excludeClassNamePatterns support only regex patterns
        Optional<String> excludesList =
                Optional.ofNullable(parameters.getProviderProperties().get(ProviderParameterNames.EXCLUDES_SCAN_LIST));
        if (excludesList.isPresent()) {
            String[] excludesRegex = Stream.of(excludesList.get().split(","))
                    .filter(s -> s.startsWith("%regex["))
                    .map(s -> StringUtils.replace(s, "%regex[", ""))
                    .map(s -> s.substring(0, s.length() - 1))
                    .toArray(String[]::new);
            if (excludesRegex.length > 0) {
                filters.add(ClassNameFilter.excludeClassNamePatterns(excludesRegex));
            }
        }

        if (includesList.isPresent()) {
            // usual include/exclude are scanner style patterns
            List<String> includes = Stream.of(includesList.get().split(","))
                    .filter(s -> !s.startsWith("%regex["))
                    .map(pattern -> StringUtils.replace(pattern, ".java", ""))
                    // .map(pattern -> StringUtils.replace(pattern, "/", "."))
                    .collect(toList());
            if (!includes.isEmpty()) {
                // use of CompositeFilter?
                ClassNameFilter classNameFilter = className -> {
                    FilterResult result = includes.stream()
                            .map(pattern -> FilterResult.includedIf(
                                    match(pattern, className) || matchClassName(className, pattern)))
                            .filter(FilterResult::included)
                            .findAny()
                            .orElse(FilterResult.excluded("Not included by any pattern: " + includes));
                    return result;
                };
                filters.add(includeEnclosingClasses(classNameFilter, enclosingClassNames));
            }
        }

        if (excludesList.isPresent()) {

            List<String> excludes = Stream.of(excludesList.get().split(","))
                    .filter(s -> !s.startsWith("%regex["))
                    .map(pattern -> StringUtils.replace(pattern, ".java", ""))
                    // .map(pattern -> StringUtils.replace(pattern, "/", "."))
                    .collect(toList());
            if (!excludes.isEmpty()) {
                // use of CompositeFilter?
                ClassNameFilter classNameFilter = className -> {
                    FilterResult result = excludes.stream()
                            .map(pattern -> {
                                boolean inclusive = match(pattern, className);
                                return !inclusive
                                        ? FilterResult.included("Not excluded by pattern: " + pattern)
                                        : FilterResult.excluded("Excluded by pattern: " + pattern);
                            })
                            .filter(FilterResult::excluded)
                            .findAny()
                            .orElse(FilterResult.included("Not excluded by any pattern: " + excludes));
                    return result;
                };
                filters.add(classNameFilter);
            }
        }

        boolean useTestNG = parameters.getProviderProperties().get("testng.version") != null;

        if (!Boolean.parseBoolean(parameters.getProviderProperties().get(JUNIT_VINTAGE_DETECTED)) && !useTestNG) {
            getPropertiesList(GROUPS_PROP).map(TagFilter::includeTags).ifPresent(filters::add);
            getPropertiesList(EXCLUDEDGROUPS_PROP).map(TagFilter::excludeTags).ifPresent(filters::add);
        } else if (!useTestNG) {
            Optional<Class<?>> categoryClass = getCategoryClass();
            if (categoryClass.isPresent()) {
                getPropertiesList(GROUPS_PROP)
                        .map(strings -> getIncludeCategoryFilter(strings, categoryClass))
                        .ifPresent(filters::add);
            }
        }

        if (!useTestNG) {
            Optional<Class<?>> categoryClass = getCategoryClass();
            if (categoryClass.isPresent()) {
                getPropertiesList(EXCLUDEDGROUPS_PROP)
                        .map(strings -> getExcludeCategoryFilter(strings, categoryClass))
                        .ifPresent(filters::add);
            }
        }
        of(optionallyWildcardFilter(parameters.getTestRequest().getTestListResolver()))
                .filter(f -> !f.isEmpty())
                .filter(f -> !f.isWildcard())
                .map(TestMethodFilter::new)
                .ifPresent(filters::add);

        getPropertiesList(INCLUDE_JUNIT5_ENGINES_PROP)
                .map(EngineFilter::includeEngines)
                .ifPresent(filters::add);

        getPropertiesList(EXCLUDE_JUNIT5_ENGINES_PROP)
                .map(EngineFilter::excludeEngines)
                .ifPresent(filters::add);

        return filters.toArray(new Filter<?>[0]);
    }

    private Set<String> getEnclosingClassNames() {
        Set<String> enclosingClassNames = new LinkedHashSet<>();
        ScanResult scanResult = parameters.getScanResult();
        for (int i = 0; i < scanResult.size(); i++) {
            String className = scanResult.getClassName(i);
            Class<?> testClass;
            try {
                testClass = parameters.getTestClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to create test class '" + className + "'", e);
            }
            for (Class<?> enclosingClass = testClass.getEnclosingClass();
                    enclosingClass != null;
                    enclosingClass = enclosingClass.getEnclosingClass()) {
                enclosingClassNames.add(enclosingClass.getName());
            }
        }
        return enclosingClassNames;
    }

    private static ClassNameFilter includeEnclosingClasses(
            ClassNameFilter classNameFilter, Set<String> enclosingClassNames) {
        return className -> enclosingClassNames.contains(className)
                ? FilterResult.included("Enclosing class of an included test class")
                : classNameFilter.apply(className);
    }

    Filter<?>[] getFilters() {
        return filters;
    }

    PostDiscoveryFilter getIncludeCategoryFilter(List<String> categories, Optional<Class<?>> categoryClass) {

        return testDescriptor -> {
            Optional<MethodSource> methodSource = testDescriptor
                    .getSource()
                    .filter(testSource -> testSource instanceof MethodSource)
                    .map(testSource -> (MethodSource) testSource);
            boolean hasCategoryClass = false, hasCategoryMethod = false;
            if (methodSource.isPresent()) {
                if (categoryClass.isPresent()) {
                    hasCategoryMethod = hasCategoryAnnotationValue(
                                    methodSource.get().getJavaMethod(), categoryClass.orElse(null), categories)
                            || hasCategoryAnnotationValue(
                                    methodSource.get().getJavaClass(), categoryClass.orElse(null), categories);
                }
            }

            Optional<ClassSource> classSource = testDescriptor
                    .getSource()
                    .filter(testSource -> testSource instanceof ClassSource)
                    .map(testSource -> (ClassSource) testSource);
            if (classSource.isPresent()) {
                if (categoryClass.isPresent()) {
                    hasCategoryClass = hasCategoryAnnotationValue(
                            classSource.get().getJavaClass(), categoryClass.orElse(null), categories);
                }
            }

            return hasCategoryClass || hasCategoryMethod
                    ? FilterResult.included("Category found")
                    : FilterResult.excluded("Does not have category annotation");
        };
    }

    PostDiscoveryFilter getExcludeCategoryFilter(List<String> categories, Optional<Class<?>> categoryClass) {

        return testDescriptor -> {
            Optional<MethodSource> methodSource = testDescriptor
                    .getSource()
                    .filter(testSource -> testSource instanceof MethodSource)
                    .map(testSource -> (MethodSource) testSource);
            boolean hasCategoryClass = false, hasCategoryMethod = false;
            if (methodSource.isPresent()) {
                if (categoryClass.isPresent()) {
                    hasCategoryMethod = hasCategoryAnnotationValue(
                                    methodSource.get().getJavaMethod(), categoryClass.orElse(null), categories)
                            || hasCategoryAnnotationValue(
                                    methodSource.get().getJavaClass(), categoryClass.orElse(null), categories);
                }
            }

            Optional<ClassSource> classSource = testDescriptor
                    .getSource()
                    .filter(testSource -> testSource instanceof ClassSource)
                    .map(testSource -> (ClassSource) testSource);
            if (classSource.isPresent()) {
                if (categoryClass.isPresent()) {
                    hasCategoryClass = hasCategoryAnnotationValue(
                            classSource.get().getJavaClass(), categoryClass.orElse(null), categories);
                }
            }

            return hasCategoryClass || hasCategoryMethod
                    ? FilterResult.excluded("Does have exclude category annotation")
                    : FilterResult.included("Does not have category excluded found");
        };
    }

    private boolean hasCategoryAnnotationValue(Class<?> clazz, Class<?> categoryClass, List<String> categories) {
        return hasCategoryAnnotationValue(clazz.getAnnotations(), categoryClass, categories);
    }

    private boolean hasCategoryAnnotationValue(Method method, Class<?> categoryClass, List<String> categories) {
        return hasCategoryAnnotationValue(method.getAnnotations(), categoryClass, categories);
    }

    private boolean hasCategoryAnnotationValue(
            Annotation[] annotations, Class<?> categoryClass, List<String> categories) {
        Optional<Annotation> anno = stream(annotations)
                .filter(annotation -> annotation.annotationType().equals(categoryClass))
                .findFirst();
        if (anno.isPresent()) {
            List<String> catValues = getCategoryValueClassName(of(anno.get()));
            catValues.addAll(getCategoryValueClassSimpleName(of(anno.get())));
            return catValues.stream().anyMatch(categories::contains);
        }
        return false;
    }

    private Optional<Class<?>> getCategoryClass() {
        return getClass("org.junit.experimental.categories.Category");
    }

    private Optional<Class<?>> getClass(String className) {
        Thread currentThread = Thread.currentThread();
        try {
            return Optional.of(currentThread.getContextClassLoader().loadClass(className));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private List<String> getCategoryValueClassName(Optional<Object> instance) {
        Optional<Class<?>> optionalClass = getCategoryClass();
        if (optionalClass.isPresent()) {
            try {
                Class<?>[] classes =
                        (Class<?>[]) optionalClass.get().getMethod("value").invoke(instance.get());
                return stream(classes).map(Class::getName).collect(Collectors.toList());
            } catch (Exception e) {
                // ignore
            }
        }
        return Collections.emptyList();
    }

    private List<String> getCategoryValueClassSimpleName(Optional<Object> instance) {
        Optional<Class<?>> optionalClass = getCategoryClass();
        if (optionalClass.isPresent()) {
            try {
                Class<?>[] classes =
                        (Class<?>[]) optionalClass.get().getMethod("value").invoke(instance.get());
                return stream(classes).map(Class::getSimpleName).collect(Collectors.toList());
            } catch (Exception e) {
                // ignore
            }
        }
        return Collections.emptyList();
    }

    private Map<String, String> newConfigurationParameters() {
        String content = parameters.getProviderProperties().get(CONFIGURATION_PARAMETERS);
        if (content == null) {
            return new HashMap<>();
        }
        try (StringReader reader = new StringReader(content)) {
            Map<String, String> result = new HashMap<>();
            Properties props = new Properties();
            props.load(reader);
            props.stringPropertyNames().forEach(key -> result.put(key, props.getProperty(key)));
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading " + CONFIGURATION_PARAMETERS, e);
        }
    }

    Map<String, String> getConfigurationParameters() {
        return configurationParameters;
    }

    private Optional<List<String>> getPropertiesList(String key) {
        String property = parameters.getProviderProperties().get(key);
        return isBlank(property)
                ? empty()
                : of(stream(property.split("[,]+"))
                        .filter(StringUtils::isNotBlank)
                        .map(String::trim)
                        .collect(toList()));
    }

    private Stoppable prepareFailFastSupport(
            CancellationTokenAdapter cancellationToken, TestReportListener<?> runListener) {
        int skipAfterFailureCount = parameters.getSkipAfterFailureCount();
        if (skipAfterFailureCount > 0) {

            AtomicBoolean loggedFailedAttempt = new AtomicBoolean(false);
            Runnable cancellation =
                    () -> cancelExecution(cancellationToken, runListener, loggedFailedAttempt, skipAfterFailureCount);

            if (commandsReader != null) {
                // Register for signals from other forks
                commandsReader.addSkipNextTestsListener(__ -> cancellation.run());
            }

            AtomicInteger remainingFailures = new AtomicInteger(skipAfterFailureCount);
            return () -> {
                runIfZeroCountDown(cancellation, remainingFailures);
                runListener.testExecutionSkippedByUser();
            };
        }
        return Stoppable.NOOP;
    }

    private static void cancelExecution(
            CancellationTokenAdapter cancellationToken,
            ConsoleLogger consoleLogger,
            AtomicBoolean loggedFailedAttempt,
            int skipAfterFailureCount) {

        if (cancellationToken != null) {
            cancellationToken.cancel();
        } else if (loggedFailedAttempt.compareAndSet(false, true)) {
            consoleLogger.warning(String.format(
                            "An attempt was made to cancel the current test run due to the configured skipAfterFailureCount of %d. ",
                            skipAfterFailureCount)
                    + "However, the version of JUnit Platform on the runtime classpath does not support cancellation. "
                    + "Please update to 6.0.0 or later!");
        }
    }

    private static boolean matchAntPathPattern(String pattern, String str, boolean isCaseSensitive) {
        if (str.startsWith("/") != pattern.startsWith("/")) {
            return false;
        }

        List<String> patDirs = tokenizePath(pattern, "/");
        List<String> strDirs = tokenizePath(str, "/");

        int patIdxStart = 0;
        int patIdxEnd = patDirs.size() - 1;
        int strIdxStart = 0;
        int strIdxEnd = strDirs.size() - 1;

        // up to first '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs.get(patIdxStart);
            if ("**".equals(patDir)) {
                break;
            }
            if (!match(patDir, strDirs.get(strIdxStart), isCaseSensitive)) {
                return false;
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!"**".equals(patDirs.get(i))) {
                    return false;
                }
            }
            return true;
        } else {
            if (patIdxStart > patIdxEnd) {
                // String not exhausted, but pattern is. Failure.
                return false;
            }
        }

        // up to last '**'
        while (patIdxStart <= patIdxEnd && strIdxStart <= strIdxEnd) {
            String patDir = patDirs.get(patIdxEnd);
            if ("**".equals(patDir)) {
                break;
            }
            if (!match(patDir, strDirs.get(strIdxEnd), isCaseSensitive)) {
                return false;
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // String is exhausted
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (!"**".equals(patDirs.get(i))) {
                    return false;
                }
            }
            return true;
        }

        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if ("**".equals(patDirs.get(i))) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // '**/**' situation, so skip one
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subPat = patDirs.get(patIdxStart + j + 1);
                    String subStr = strDirs.get(strIdxStart + i + j);
                    if (!match(subPat, subStr, isCaseSensitive)) {
                        continue strLoop;
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (!"**".equals(patDirs.get(i))) {
                return false;
            }
        }

        return true;
    }

    private static List<String> tokenizePath(String path, String separator) {
        List<String> ret = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(path, separator);
        while (st.hasMoreTokens()) {
            ret.add(st.nextToken());
        }
        return ret;
    }
}
