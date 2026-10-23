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

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.report.TestOutputReceiver;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestRequest;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.DefaultRunOrderCalculator;
import org.apache.maven.surefire.api.util.RunOrderCalculator;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherFactory;
import org.mockito.ArgumentCaptor;

import static org.apache.maven.surefire.api.booter.ProviderParameterNames.JUNIT4_ONLY_DETECTED;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.JUNIT_VINTAGE_DETECTED;
import static org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.CLASS_ORDER_DEFAULT;
import static org.apache.maven.surefire.junitplatform.JUnitPlatformProvider.CONFIGURATION_PARAMETERS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Tests how the provider splits the class ordering with Jupiter for {@code runOrder=random}
 * (issue 3544: both sides shuffled with the same seed, which composed the permutation with
 * itself and made some class orders unreachable, two classes always ran alphabetically).
 */
public class JUnitPlatformProviderRunOrderTest {
    @Test
    public void runOrderRandomShufflesTheClassesOnceWhenJupiterSeesThemAll() throws Exception {
        Set<String> firstExecutedClasses = new HashSet<>();
        // Large seeds only: java.util.Random draws the same first nextInt(2) for the small ones,
        // so the shuffles would not vary.
        for (int i = 1; i <= 12; i++) {
            long seed = 10_000_000_007L * i;
            TestReportListener<TestOutputReportEntry> runListener = runListenerMock();
            ProviderParameters providerParameters = providerParametersMock(
                    runListener,
                    JUnitPlatformProviderTest.TestClass2.class,
                    JUnitPlatformProviderTest.TestClass1.class);
            when(providerParameters.getProviderProperties()).thenReturn(runOrderRandom(seed));
            when(providerParameters.getRunOrderCalculator())
                    .thenReturn(new DefaultRunOrderCalculator(new RunOrderParameters("random", null, seed), 1));

            JUnitPlatformProvider provider = new JUnitPlatformProvider(providerParameters, createLauncherSession());

            invokeProvider(provider, null);

            ArgumentCaptor<TestSetReportEntry> captor = ArgumentCaptor.forClass(TestSetReportEntry.class);
            verify(runListener, times(2)).testSetStarting(captor.capture());
            firstExecutedClasses.add(captor.getAllValues().get(0).getSourceName());
        }
        // The run order calculator and Jupiter shuffled with the same seed, which composed the
        // permutation with itself: two classes always ran alphabetically, whatever the seed was.
        assertThat(firstExecutedClasses)
                .containsExactlyInAnyOrder(
                        JUnitPlatformProviderTest.TestClass1.class.getName(),
                        JUnitPlatformProviderTest.TestClass2.class.getName());
    }

    @Test
    public void runOrderRandomDoesNotShuffleAgainWhenJupiterSeesAllClasses() throws Exception {
        ProviderParameters providerParameters = providerParametersMock(
                JUnitPlatformProviderTest.TestClass2.class, JUnitPlatformProviderTest.TestClass1.class);
        when(providerParameters.getProviderProperties()).thenReturn(runOrderRandom(42));
        RunOrderCalculator runOrderCalculator = mock(RunOrderCalculator.class);
        when(runOrderCalculator.orderTestClasses(any()))
                .thenThrow(new AssertionError("the run order calculator would shuffle a second time"));
        when(providerParameters.getRunOrderCalculator()).thenReturn(runOrderCalculator);

        JUnitPlatformProvider provider = new JUnitPlatformProvider(providerParameters, createLauncherSession());

        invokeProvider(provider, null);

        verify(runOrderCalculator, never()).orderTestClasses(any());
    }

    @Test
    public void runOrderRandomKeepsTheCalculatorForTheSuites() {
        ProviderParameters providerParameters = providerParametersMock(
                JUnitPlatformProviderTest.TestClass2.class, JUnitPlatformProviderTest.TestClass1.class);
        when(providerParameters.getProviderProperties()).thenReturn(runOrderRandom(42));
        RunOrderCalculator runOrderCalculator = mock(RunOrderCalculator.class);
        when(runOrderCalculator.orderTestClasses(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(providerParameters.getRunOrderCalculator()).thenReturn(runOrderCalculator);

        JUnitPlatformProvider provider = new JUnitPlatformProvider(providerParameters, createLauncherSession());

        // The suites are handed over to the fork distribution, which runs the classes one at a
        // time, so the run order calculator keeps ordering them.
        assertThat(provider.getSuites())
                .containsOnly(JUnitPlatformProviderTest.TestClass1.class, JUnitPlatformProviderTest.TestClass2.class);
        verify(runOrderCalculator, times(1)).orderTestClasses(any());
    }

    @Test
    public void runOrderRandomKeepsTheCalculatorForClassesRunningOneAtATime() throws Exception {
        ProviderParameters providerParameters = providerParametersMock(
                JUnitPlatformProviderTest.TestClass2.class, JUnitPlatformProviderTest.TestClass1.class);
        Map<String, String> providerProperties = runOrderRandom(42);
        providerProperties.put(JUNIT4_ONLY_DETECTED, "true");
        when(providerParameters.getProviderProperties()).thenReturn(providerProperties);
        RunOrderCalculator runOrderCalculator = mock(RunOrderCalculator.class);
        when(runOrderCalculator.orderTestClasses(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(providerParameters.getRunOrderCalculator()).thenReturn(runOrderCalculator);

        JUnitPlatformProvider provider = new JUnitPlatformProvider(providerParameters, createLauncherSession());

        invokeProvider(provider, null);

        verify(runOrderCalculator, times(1)).orderTestClasses(any());
    }

    @Test
    public void runOrderRandomKeepsTheCalculatorWhenVintageRunsClassesToo() throws Exception {
        ProviderParameters providerParameters = providerParametersMock(
                JUnitPlatformProviderTest.TestClass2.class, JUnitPlatformProviderTest.TestClass1.class);
        Map<String, String> providerProperties = runOrderRandom(42);
        providerProperties.put(JUNIT_VINTAGE_DETECTED, "true");
        when(providerParameters.getProviderProperties()).thenReturn(providerProperties);
        RunOrderCalculator runOrderCalculator = mock(RunOrderCalculator.class);
        when(runOrderCalculator.orderTestClasses(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(providerParameters.getRunOrderCalculator()).thenReturn(runOrderCalculator);

        JUnitPlatformProvider provider = new JUnitPlatformProvider(providerParameters, createLauncherSession());

        invokeProvider(provider, null);

        verify(runOrderCalculator, times(1)).orderTestClasses(any());
    }

    @Test
    public void runOrderRandomKeepsTheCalculatorForConfiguredClassOrderers() throws Exception {
        ProviderParameters providerParameters = providerParametersMock(
                JUnitPlatformProviderTest.TestClass2.class, JUnitPlatformProviderTest.TestClass1.class);
        Map<String, String> providerProperties = runOrderRandom(42);
        providerProperties.put(
                CONFIGURATION_PARAMETERS, CLASS_ORDER_DEFAULT + "=org.junit.jupiter.api.ClassOrderer$ClassName");
        when(providerParameters.getProviderProperties()).thenReturn(providerProperties);
        RunOrderCalculator runOrderCalculator = mock(RunOrderCalculator.class);
        when(runOrderCalculator.orderTestClasses(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(providerParameters.getRunOrderCalculator()).thenReturn(runOrderCalculator);

        JUnitPlatformProvider provider = new JUnitPlatformProvider(providerParameters, createLauncherSession());

        invokeProvider(provider, null);

        verify(runOrderCalculator, times(1)).orderTestClasses(any());
    }

    private static Map<String, String> runOrderRandom(long seed) {
        Map<String, String> providerProperties = new HashMap<>();
        providerProperties.put("runOrder", "random");
        providerProperties.put("runOrderRandomSeed", String.valueOf(seed));
        return providerProperties;
    }

    private static ProviderParameters providerParametersMock(Class<?>... testClasses) {
        return providerParametersMock(runListenerMock(), testClasses);
    }

    private static ProviderParameters providerParametersMock(
            TestReportListener<TestOutputReportEntry> runListener, Class<?>... testClasses) {
        TestsToRun testsToRun = new TestsToRun(new LinkedHashSet<>(Arrays.asList(testClasses)));

        ScanResult scanResult = mock(ScanResult.class);
        when(scanResult.applyFilter(any(), any())).thenReturn(testsToRun);
        when(scanResult.size()).thenReturn(testClasses.length);
        for (int i = 0; i < testClasses.length; i++) {
            when(scanResult.getClassName(i)).thenReturn(testClasses[i].getName());
        }

        RunOrderCalculator runOrderCalculator = mock(RunOrderCalculator.class);
        when(runOrderCalculator.orderTestClasses(any())).thenReturn(testsToRun);

        ReporterFactory reporterFactory = mock(ReporterFactory.class);
        when(reporterFactory.createTestReportListener()).thenReturn(runListener);

        TestListResolver testListResolver = new TestListResolver("");
        TestRequest testRequest = mock(TestRequest.class);
        when(testRequest.getTestListResolver()).thenReturn(testListResolver);

        ProviderParameters providerParameters = mock(ProviderParameters.class);
        when(providerParameters.getScanResult()).thenReturn(scanResult);
        when(providerParameters.getRunOrderCalculator()).thenReturn(runOrderCalculator);
        when(providerParameters.getReporterFactory()).thenReturn(reporterFactory);
        when(providerParameters.getTestRequest()).thenReturn(testRequest);
        when(providerParameters.getTestClassLoader())
                .thenReturn(JUnitPlatformProviderRunOrderTest.class.getClassLoader());

        return providerParameters;
    }

    private static TestReportListener<TestOutputReportEntry> runListenerMock() {
        return mock(TestReportListener.class, withSettings().extraInterfaces(TestOutputReceiver.class));
    }

    private static LauncherSessionFactory createLauncherSession() {
        LauncherConfig launcherConfig = LauncherConfig.builder().build();
        return cancellationToken -> {
            LauncherSession session = LauncherFactory.openSession(launcherConfig);
            return new LauncherSessionAdapter(session, session.getLauncher(), cancellationToken);
        };
    }

    /**
     * Invokes the provider, then restores system out and system error.
     *
     * @see <a href="https://github.com/junit-team/junit5/issues/986">#986</a>
     */
    private static void invokeProvider(JUnitPlatformProvider provider, Object forkTestSet)
            throws TestSetFailedException {
        PrintStream systemOut = System.out;
        PrintStream systemErr = System.err;
        try {
            provider.invoke(forkTestSet);
        } finally {
            System.setOut(systemOut);
            System.setErr(systemErr);
        }
    }
}
