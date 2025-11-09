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
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.apache.maven.surefire.shared.utils.StringUtils;
import org.apache.maven.surefire.shared.utils.io.SelectorUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.FilterResult;
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
import static org.apache.maven.surefire.api.report.ConsoleOutputCapture.startCapture;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.report.RunMode.RERUN_TEST_AFTER_FAILURE;
import static org.apache.maven.surefire.api.testset.TestListResolver.optionallyWildcardFilter;
import static org.apache.maven.surefire.api.util.TestsToRun.fromClass;
import static org.apache.maven.surefire.api.util.internal.ConcurrencyUtils.runIfZeroCountDown;
import static org.apache.maven.surefire.shared.utils.StringUtils.isBlank;
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

    private final ProviderParameters parameters;

    private final LauncherSessionFactory launcherSessionFactory;

    private final Filter<?>[] filters;

    private final Map<String, String> configurationParameters;

    private final CommandChainReader commandsReader;

    public JUnitPlatformProvider(ProviderParameters parameters) {
        this(parameters, LauncherSessionFactory.DEFAULT);
    }

    JUnitPlatformProvider(ProviderParameters parameters, LauncherSessionFactory launcherSessionFactory) {
        this.parameters = parameters;
        this.launcherSessionFactory = launcherSessionFactory;
        filters = newFilters();
        configurationParameters = newConfigurationParameters();
        parameters.getProviderProperties().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("junit.vintage.execution.parallel"))
                .forEach(entry -> configurationParameters.put(entry.getKey(), entry.getValue()));
        // don't start a thread in CommandReader while we are in in-plugin process
        commandsReader = parameters.isInsideFork() ? parameters.getCommandReader() : null;

        parameters.getProviderProperties().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("testng."))
                .forEach(entry -> configurationParameters.put(entry.getKey(), entry.getValue()));
        // testng compatibility parameters
        String groups = parameters.getProviderProperties().get(GROUPS_PROP);
        if (groups != null) {
            configurationParameters.put("testng.groups", groups);
        }

        //        configurationParameters.put("testng.useDefaultListeners", "true");

        Optional.ofNullable(parameters.getProviderProperties().get("listener"))
                .ifPresent(listener -> configurationParameters.put("testng.listeners", listener));

        Optional.ofNullable(parameters.getProviderProperties().get("reporter"))
                .ifPresent(reporter -> configurationParameters.compute(
                        "testng.listeners", (key, value) -> value == null ? reporter : value + "," + reporter));

        String excludeGroups = parameters.getProviderProperties().get(EXCLUDEDGROUPS_PROP);
        if (excludeGroups != null) {
            configurationParameters.put("testng.excludedGroups", excludeGroups);
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
        // parameters.getProviderProperties().get(CONFIGURATION_PARAMETERS)
        // add this LegacyXmlReportGeneratingListener ?
        //            adapter,
        //            new LegacyXmlReportGeneratingListener(
        //                    new File("target", "junit-platform").toPath(), new PrintWriter(System.out))
        //        };

        TestExecutionListener[] testExecutionListeners = new TestExecutionListener[] {adapter};

        if (testsToRun.allowEagerReading()) {
            List<DiscoverySelector> selectors = new ArrayList<>();
            testsToRun.iterator().forEachRemaining(c -> selectors.add(selectClass(c.getName())));

            LauncherDiscoveryRequestBuilder builder = newRequest().selectors(selectors);
            launcher.execute(builder.build(), testExecutionListeners);
        } else {
            testsToRun.iterator().forEachRemaining(c -> {
                LauncherDiscoveryRequestBuilder builder = newRequest().selectors(selectClass(c.getName()));
                launcher.execute(builder.build(), testExecutionListeners);
            });
        }
    }

    private LauncherDiscoveryRequest buildLauncherDiscoveryRequestForRerunFailures(RunListenerAdapter adapter) {
        LauncherDiscoveryRequestBuilder builder = newRequest();
        // Iterate over recorded failures
        for (TestIdentifier identifier :
                new LinkedHashSet<>(adapter.getFailures().keySet())) {
            builder.selectors(selectUniqueId(identifier.getUniqueId()));
        }
        return builder.build();
    }

    private LauncherDiscoveryRequestBuilder newRequest() {
        return request().filters(filters).configurationParameters(configurationParameters);
    }

    private boolean matchClassName(String className, String pattern) {
        // pattern can be either fully qualified or simple class name or package + simple class name + #method
        int hashIndex = pattern.indexOf('#');
        if (hashIndex != -1) {
            pattern = pattern.substring(0, hashIndex);
        }
        return className.equals(pattern)
                || className.endsWith("." + pattern)
                || SelectorUtils.matchPath(pattern, className);
    }

    private Filter<?>[] newFilters() {
        List<Filter<?>> filters = new ArrayList<>();

        // includeClassNamePatterns support only regex patterns
        Optional<String> includesList =
                Optional.ofNullable(parameters.getProviderProperties().get(ProviderParameterNames.INCLUDES_SCAN_LIST));
        if (includesList.isPresent()) {
            String[] includesRegex = Stream.of(includesList.get().split(","))
                    .filter(s -> s.startsWith("%regex["))
                    .map(s -> StringUtils.replace(s, "%regex[", ""))
                    .map(s -> s.substring(0, s.length() - 1))
                    .toArray(String[]::new);
            if (includesRegex.length > 0) {
                filters.add(ClassNameFilter.includeClassNamePatterns(includesRegex));
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
                ClassNameFilter classNameFilter = new ClassNameFilter() {
                    @Override
                    public FilterResult apply(String clasName) {
                        FilterResult result = includes.stream()
                                .map(pattern -> FilterResult.includedIf(
                                        SelectorUtils.match(pattern, clasName) || matchClassName(clasName, pattern)))
                                .filter(FilterResult::included)
                                .findAny()
                                .orElse(FilterResult.excluded("Not included by any pattern: " + includes));
                        return result;
                    }
                };
                filters.add(classNameFilter);
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
                ClassNameFilter classNameFilter = new ClassNameFilter() {
                    @Override
                    public FilterResult apply(String className) {
                        FilterResult result = excludes.stream()
                                .map(pattern -> {
                                    boolean inclusive = SelectorUtils.match(pattern, className);
                                    return !inclusive
                                            ? FilterResult.included("Not excluded by pattern: " + pattern)
                                            : FilterResult.excluded("Excluded by pattern: " + pattern);
                                })
                                .filter(FilterResult::excluded)
                                .findAny()
                                .orElse(FilterResult.included("Not excluded by any pattern: " + excludes));
                        return result;
                    }
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
                    hasCategoryClass = hasCategoryAnnotation(
                            methodSource.get().getJavaMethod(), categoryClass.orElse(null), categories);
                }
            }

            Optional<ClassSource> classSource = testDescriptor
                    .getSource()
                    .filter(testSource -> testSource instanceof ClassSource)
                    .map(testSource -> (ClassSource) testSource);
            if (classSource.isPresent()) {
                if (categoryClass.isPresent()) {
                    hasCategoryMethod = hasCategoryAnnotation(
                            getClass(classSource.get().getClassName()).get(), categoryClass.orElse(null), categories);
                }
            }

            return hasCategoryClass || hasCategoryMethod
                    ? FilterResult.included("Category found")
                    : FilterResult.excluded("Does not have category annotation");
        };
    }

    private boolean hasCategoryAnnotation(Class<?> clazz, Class<?> categoryClass, List<String> categories) {
        Optional<Annotation> anno = stream(clazz.getAnnotations())
                .filter(annotation -> annotation.annotationType().equals(categoryClass))
                .findFirst();
        if (anno.isPresent()) {
            List<String> catValue = getCategoryValue(of(anno.get()));
            return catValue.stream().anyMatch(categories::contains);
        }
        return false;
    }

    private boolean hasCategoryAnnotation(Method method, Class<?> categoryClass, List<String> categories) {
        Optional<Annotation> anno = stream(method.getAnnotations())
                .filter(annotation -> annotation.annotationType().equals(categoryClass))
                .findFirst();
        if (anno.isPresent()) {
            List<String> catValue = getCategoryValue(of(anno.get()));
            return catValue.stream().anyMatch(categories::contains);
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

    private List<String> getCategoryValue(Optional<Object> instance) {
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
}
