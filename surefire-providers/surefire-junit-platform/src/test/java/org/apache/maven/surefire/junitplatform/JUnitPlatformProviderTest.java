package org.apache.maven.surefire.junitplatform;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.TESTNG_GROUPS_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.INCLUDE_JUNIT5_ENGINES_PROP;
import static org.apache.maven.surefire.api.booter.ProviderParameterNames.EXCLUDE_JUNIT5_ENGINES_PROP;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.powermock.reflect.Whitebox.getInternalState;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.TestOutputReceiver;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.report.RunListener;
import org.apache.maven.surefire.api.report.SimpleReportEntry;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestRequest;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.RunOrderCalculator;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.platform.launcher.EngineFilter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.launcher.listeners.TestExecutionSummary.Failure;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * Unit tests for {@link JUnitPlatformProvider}.
 *
 * @since 2.22.0
 */
public class JUnitPlatformProviderTest
{
    @Test
    public void shouldFailClassOnBeforeAll()
            throws Exception
    {
        Launcher launcher = LauncherFactory.create();
        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParametersMock(), launcher );
        TestReportListener<TestOutputReportEntry> listener = mock( TestReportListener.class );

        ArgumentCaptor<ReportEntry> testCaptor = ArgumentCaptor.forClass( ReportEntry.class );
        ArgumentCaptor<TestSetReportEntry> testSetCaptor = ArgumentCaptor.forClass( TestSetReportEntry.class );

        RunListenerAdapter adapter = new RunListenerAdapter( listener );
        adapter.setRunMode( NORMAL_RUN );
        launcher.registerTestExecutionListeners( adapter );

        TestsToRun testsToRun = newTestsToRun( FailingBeforeAllJupiterTest.class );
        invokeProvider( provider, testsToRun );
        InOrder inOrder = inOrder( listener );
        inOrder.verify( listener, times( 1 ) ).testSetStarting( testSetCaptor.capture() );
        inOrder.verify( listener, never() ).testStarting( any( ReportEntry.class ) );
        inOrder.verify( listener, times( 1 ) ).testFailed( testCaptor.capture() );
        inOrder.verify( listener, times( 1 ) ).testSetCompleted( testSetCaptor.capture() );

        assertThat( testSetCaptor.getAllValues() )
                .hasSize( 2 );

        assertThat( testSetCaptor.getAllValues().get( 0 ).getSourceName() )
                .isEqualTo( FailingBeforeAllJupiterTest.class.getName() );

        assertThat( testSetCaptor.getAllValues().get( 0 ).getName() )
                .isNull();

        assertThat( testCaptor.getAllValues() )
                .hasSize( 1 );

        assertThat( testCaptor.getValue().getSourceName() )
                .isEqualTo( FailingBeforeAllJupiterTest.class.getName() );

        assertThat( testCaptor.getValue().getName() )
                .isNull();

        assertThat( testSetCaptor.getAllValues().get( 1 ).getSourceName() )
                .isEqualTo( FailingBeforeAllJupiterTest.class.getName() );

        assertThat( testSetCaptor.getAllValues().get( 1 ).getName() )
                .isNull();
    }

    @Test
    public void shouldErrorClassOnBeforeAll()
            throws Exception
    {
        Launcher launcher = LauncherFactory.create();
        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParametersMock(), launcher );
        TestReportListener<TestOutputReportEntry> listener = mock( TestReportListener.class );

        ArgumentCaptor<ReportEntry> testCaptor = ArgumentCaptor.forClass( ReportEntry.class );
        ArgumentCaptor<TestSetReportEntry> testSetCaptor = ArgumentCaptor.forClass( TestSetReportEntry.class );

        RunListenerAdapter adapter = new RunListenerAdapter( listener );
        adapter.setRunMode( NORMAL_RUN );
        launcher.registerTestExecutionListeners( adapter );

        TestsToRun testsToRun = newTestsToRun( FailingWithErrorBeforeAllJupiterTest.class );
        invokeProvider( provider, testsToRun );
        InOrder inOrder = inOrder( listener );
        inOrder.verify( listener, times( 1 ) ).testSetStarting( testSetCaptor.capture() );
        inOrder.verify( listener, never() ).testStarting( any( ReportEntry.class ) );
        inOrder.verify( listener, times( 1 ) ).testError( testCaptor.capture() );
        inOrder.verify( listener, times( 1 ) ).testSetCompleted( testSetCaptor.capture() );

        assertThat( testSetCaptor.getAllValues() )
                .hasSize( 2 );

        assertThat( testSetCaptor.getAllValues().get( 0 ).getSourceName() )
                .isEqualTo( FailingWithErrorBeforeAllJupiterTest.class.getName() );

        assertThat( testSetCaptor.getAllValues().get( 0 ).getName() )
                .isNull();

        assertThat( testCaptor.getAllValues() )
                .hasSize( 1 );

        assertThat( testCaptor.getValue().getSourceName() )
                .isEqualTo( FailingWithErrorBeforeAllJupiterTest.class.getName() );

        assertThat( testCaptor.getValue().getName() )
                .isNull();

        assertThat( testSetCaptor.getAllValues().get( 1 ).getSourceName() )
                .isEqualTo( FailingWithErrorBeforeAllJupiterTest.class.getName() );

        assertThat( testSetCaptor.getAllValues().get( 1 ).getName() )
                .isNull();
    }

    @Test
    public void getSuitesReturnsScannedClasses()
    {
        ProviderParameters providerParameters = providerParametersMock( TestClass1.class, TestClass2.class );
        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );
        assertThat( provider.getSuites() )
                .containsOnly( TestClass1.class, TestClass2.class );
    }

    @Test
    public void invokeThrowsForWrongForkTestSet()
    {
        ProviderParameters providerParameters = providerParametersMock( Integer.class );
        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertThrows( IllegalArgumentException.class, () -> invokeProvider( provider, "wrong forkTestSet" ) );
    }

    @Test
    public void allGivenTestsToRunAreInvoked()
                    throws Exception
    {
        Launcher launcher = LauncherFactory.create();
        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParametersMock(), launcher );

        TestPlanSummaryListener executionListener = new TestPlanSummaryListener();
        launcher.registerTestExecutionListeners( executionListener );

        TestsToRun testsToRun = newTestsToRun( TestClass1.class, TestClass2.class );
        invokeProvider( provider, testsToRun );

        assertThat( executionListener.summaries ).hasSize( 1 );
        TestExecutionSummary summary = executionListener.summaries.get( 0 );
        assertEquals( TestClass1.TESTS_FOUND + TestClass2.TESTS_FOUND, summary.getTestsFoundCount() );
        assertEquals(
                        TestClass1.TESTS_STARTED + TestClass2.TESTS_STARTED, summary.getTestsStartedCount() );
        assertEquals(
                        TestClass1.TESTS_SKIPPED + TestClass2.TESTS_SKIPPED, summary.getTestsSkippedCount() );
        assertEquals(
                        TestClass1.TESTS_SUCCEEDED + TestClass2.TESTS_SUCCEEDED, summary.getTestsSucceededCount() );
        assertEquals(
                        TestClass1.TESTS_ABORTED + TestClass2.TESTS_ABORTED, summary.getTestsAbortedCount() );
        assertEquals( TestClass1.TESTS_FAILED + TestClass2.TESTS_FAILED, summary.getTestsFailedCount() );
    }

    @Test
    public void singleTestClassIsInvoked()
                    throws Exception
    {
        Launcher launcher = LauncherFactory.create();
        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParametersMock(), launcher );

        TestPlanSummaryListener executionListener = new TestPlanSummaryListener();
        launcher.registerTestExecutionListeners( executionListener );

        invokeProvider( provider, TestClass1.class );

        assertThat( executionListener.summaries ).hasSize( 1 );
        TestExecutionSummary summary = executionListener.summaries.get( 0 );
        assertEquals( TestClass1.TESTS_FOUND, summary.getTestsFoundCount() );
        assertEquals( TestClass1.TESTS_STARTED, summary.getTestsStartedCount() );
        assertEquals( TestClass1.TESTS_SKIPPED, summary.getTestsSkippedCount() );
        assertEquals( TestClass1.TESTS_SUCCEEDED, summary.getTestsSucceededCount() );
        assertEquals( TestClass1.TESTS_ABORTED, summary.getTestsAbortedCount() );
        assertEquals( TestClass1.TESTS_FAILED, summary.getTestsFailedCount() );
    }

    @Test
    public void singleTestClassIsInvokedLazily()
        throws Exception
    {
        Launcher launcher = LauncherFactory.create();
        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParametersMock(), launcher );

        TestPlanSummaryListener executionListener = new TestPlanSummaryListener();
        launcher.registerTestExecutionListeners( executionListener );

        invokeProvider( provider, newTestsToRunLazily( TestClass1.class ) );

        assertThat( executionListener.summaries ).hasSize( 1 );
        TestExecutionSummary summary = executionListener.summaries.get( 0 );
        assertEquals( TestClass1.TESTS_FOUND, summary.getTestsFoundCount() );
        assertEquals( TestClass1.TESTS_STARTED, summary.getTestsStartedCount() );
        assertEquals( TestClass1.TESTS_SKIPPED, summary.getTestsSkippedCount() );
        assertEquals( TestClass1.TESTS_SUCCEEDED, summary.getTestsSucceededCount() );
        assertEquals( TestClass1.TESTS_ABORTED, summary.getTestsAbortedCount() );
        assertEquals( TestClass1.TESTS_FAILED, summary.getTestsFailedCount() );
    }

    @Test
    public void failingTestCaseAfterRerun()
            throws Exception
    {
        Launcher launcher = LauncherFactory.create();
        ProviderParameters parameters = providerParametersMock();
        // Mock the rerun variable
        when( parameters.getTestRequest().getRerunFailingTestsCount() ).thenReturn( 1 );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( parameters, launcher );

        TestPlanSummaryListener executionListener = new TestPlanSummaryListener();
        launcher.registerTestExecutionListeners( executionListener );

        invokeProvider( provider, TestClass2.class );

        assertThat( executionListener.summaries ).hasSize( 2 );
        TestExecutionSummary summary = executionListener.summaries.get( 0 );
        assertEquals( TestClass2.TESTS_FOUND, summary.getTestsFoundCount() );
        assertEquals( TestClass2.TESTS_STARTED, summary.getTestsStartedCount() );
        assertEquals( TestClass2.TESTS_SKIPPED, summary.getTestsSkippedCount() );
        assertEquals( TestClass2.TESTS_SUCCEEDED, summary.getTestsSucceededCount() );
        assertEquals( TestClass2.TESTS_ABORTED, summary.getTestsAbortedCount() );
        assertEquals( TestClass2.TESTS_FAILED, summary.getTestsFailedCount() );

        // Should only be re-running one unit test
        // - And it should only fail
        summary = executionListener.summaries.get( 1 );
        assertEquals( 1, summary.getTestsFoundCount() );
        assertEquals( 1, summary.getTestsStartedCount() );
        assertEquals( 0, summary.getTestsSkippedCount() );
        assertEquals( 0, summary.getTestsSucceededCount() );
        assertEquals( 0, summary.getTestsAbortedCount() );
        assertEquals( 1, summary.getTestsFailedCount() );
    }

    @Test
    public void rerunStillFailing()
                    throws Exception
    {
        Launcher launcher = LauncherFactory.create();
        ProviderParameters parameters = providerParametersMock();
        // Mock the rerun variable
        when( parameters.getTestRequest().getRerunFailingTestsCount() ).thenReturn( 2 );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( parameters, launcher );
        TestPlanSummaryListener executionListener = new TestPlanSummaryListener();
        launcher.registerTestExecutionListeners( executionListener );

        // 3 unit tests:
        // - fail always
        // - fail twice (pass on 2nd rerun)
        // - pass always
        invokeProvider( provider, TestClass4.class );

        assertThat( executionListener.summaries ).hasSize( 3 );
        TestExecutionSummary summary = executionListener.summaries.get( 0 );
        assertEquals( 6, summary.getTestsFoundCount() );
        assertEquals( 5, summary.getTestsStartedCount() );
        assertEquals( 1, summary.getTestsSkippedCount() );
        assertEquals( 1, summary.getTestsSucceededCount() );
        assertEquals( 1, summary.getTestsAbortedCount() );
        assertEquals( 3, summary.getTestsFailedCount() );
        Set<String> failDisplays = new HashSet<>();
        for ( TestExecutionSummary.Failure failure : summary.getFailures() )
        {
            failDisplays.add( failure.getTestIdentifier().getDisplayName() );
        }
        assertEquals( 3, failDisplays.size() );
        assertTrue( failDisplays.contains( "Fails twice" ) );
        assertTrue( failDisplays.contains( "testAlwaysFail()" ) );
        assertTrue( failDisplays.contains( "testAlwaysError()" ) );

        // Should rerun both of the failures
        summary = executionListener.summaries.get( 1 );
        assertEquals( 3, summary.getTestsFoundCount() );
        assertEquals( 3, summary.getTestsStartedCount() );
        assertEquals( 0, summary.getTestsSkippedCount() );
        assertEquals( 0, summary.getTestsSucceededCount() );
        assertEquals( 0, summary.getTestsAbortedCount() );
        assertEquals( 3, summary.getTestsFailedCount() );
        failDisplays.clear();
        for ( TestExecutionSummary.Failure failure : summary.getFailures() )
        {
            failDisplays.add( failure.getTestIdentifier().getDisplayName() );
        }
        assertEquals( 3, failDisplays.size() );
        assertTrue( failDisplays.contains( "Fails twice" ) );
        assertTrue( failDisplays.contains( "testAlwaysFail()" ) );
        assertTrue( failDisplays.contains( "testAlwaysError()" ) );

        // now only one failure should remain
        summary = executionListener.summaries.get( 2 );
        assertEquals( 3, summary.getTestsFoundCount() );
        assertEquals( 3, summary.getTestsStartedCount() );
        assertEquals( 0, summary.getTestsSkippedCount() );
        assertEquals( 1, summary.getTestsSucceededCount() );
        assertEquals( 0, summary.getTestsAbortedCount() );
        assertEquals( 2, summary.getTestsFailedCount() );
        failDisplays.clear();
        for ( TestExecutionSummary.Failure failure : summary.getFailures() )
        {
            failDisplays.add( failure.getTestIdentifier().getDisplayName() );
        }
        assertEquals( 2, failDisplays.size() );
        assertTrue( failDisplays.contains( "testAlwaysFail()" ) );
        assertTrue( failDisplays.contains( "testAlwaysError()" ) );
    }

    @Test
    public void rerunWithSuccess()
            throws Exception
    {
        Launcher launcher = LauncherFactory.create();
        ProviderParameters parameters = providerParametersMock();
        // Mock the rerun variable
        when( parameters.getTestRequest().getRerunFailingTestsCount() ).thenReturn( 2 );
        when( parameters.getProviderProperties() )
                .thenReturn( singletonMap( JUnitPlatformProvider.CONFIGURATION_PARAMETERS,
                        "forkCount = 1\nreuseForks = true" ) );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( parameters, launcher );
        TestPlanSummaryListener executionListener = new TestPlanSummaryListener();
        launcher.registerTestExecutionListeners( executionListener );

        invokeProvider( provider, TestClass5.class );

        assertThat( executionListener.summaries ).hasSize( 3 );
        TestExecutionSummary summary = executionListener.summaries.get( 0 );
        assertEquals( 1, summary.getTestsFoundCount() );
        assertEquals( 1, summary.getTestsStartedCount() );
        assertEquals( 0, summary.getTestsSucceededCount() );
        assertEquals( 1, summary.getTestsFailedCount() );

        summary = executionListener.summaries.get( 1 );
        assertEquals( 1, summary.getTestsFoundCount() );
        assertEquals( 1, summary.getTestsStartedCount() );
        assertEquals( 0, summary.getTestsSucceededCount() );
        assertEquals( 1, summary.getTestsFailedCount() );

        summary = executionListener.summaries.get( 2 );
        assertEquals( 1, summary.getTestsFoundCount() );
        assertEquals( 1, summary.getTestsStartedCount() );
        assertEquals( 1, summary.getTestsSucceededCount() );
        assertEquals( 0, summary.getTestsFailedCount() );
    }

    @Test
    public void runDisplayNameTest() throws Exception
    {
        Launcher launcher = LauncherFactory.create();
        ProviderParameters parameters = providerParametersMock();
        JUnitPlatformProvider provider = new JUnitPlatformProvider( parameters, launcher );

        TestReportListener<TestOutputReportEntry> listener = mock( TestReportListener.class );
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass( ReportEntry.class );
        RunListenerAdapter adapter = new RunListenerAdapter( listener );
        adapter.setRunMode( NORMAL_RUN );

        launcher.registerTestExecutionListeners( adapter );

        invokeProvider( provider, DisplayNameTest.class );

        verify( listener, times( 1 ) ).testStarting( entryCaptor.capture() );
        List<ReportEntry> reportEntries = entryCaptor.getAllValues();

        assertEquals( 1, reportEntries.size() );

        assertEquals( DisplayNameTest.class.getName(), reportEntries.get( 0 ).getSourceName() );
        assertEquals( "<< ✨ >>", reportEntries.get( 0 ).getSourceText() );
        assertEquals( "test1", reportEntries.get( 0 ).getName() );
        assertEquals( "73$71 ✔", reportEntries.get( 0 ).getNameText() );
    }

    @Test
    public void detectErroredParameterized()
        throws Exception
    {
        Launcher launcher = LauncherFactory.create();
        ProviderParameters parameters = providerParametersMock();

        JUnitPlatformProvider provider = new JUnitPlatformProvider( parameters, launcher );

        TestPlanSummaryListener executionListener = new TestPlanSummaryListener();

        TestReportListener<TestOutputReportEntry> listener = mock( TestReportListener.class );
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass( ReportEntry.class );
        RunListenerAdapter adapter = new RunListenerAdapter( listener );
        adapter.setRunMode( NORMAL_RUN );

        launcher.registerTestExecutionListeners( executionListener, adapter );

        invokeProvider( provider, TestClass8.class );

        assertThat( executionListener.summaries ).hasSize( 1 );

        verify( listener, times( 1 ) ).testSetCompleted( any() );
        verify( listener, times( 1 ) ).testError( entryCaptor.capture() );
        List<ReportEntry> reportEntries = entryCaptor.getAllValues();

        assertEquals( TestClass8.class.getName(), reportEntries.get( 0 ).getSourceName() );
        assertNull( reportEntries.get( 0 ).getSourceText() );
        assertEquals( "testParameterizedTestCases", reportEntries.get( 0 ).getName() );
        assertNull( reportEntries.get( 0 ).getNameText() );

        TestExecutionSummary summary = executionListener.summaries.get( 0 );
        assertEquals( 0, summary.getTestsFoundCount() );
        assertEquals( 1, summary.getContainersFailedCount() );
        assertEquals( 0, summary.getTestsStartedCount() );
        assertEquals( 0, summary.getTestsSucceededCount() );
        assertEquals( 0, summary.getTestsFailedCount() );
    }

    @Test
    public void detectFailedParameterized()
            throws Exception
    {
        Launcher launcher = LauncherFactory.create();
        ProviderParameters parameters = providerParametersMock();

        JUnitPlatformProvider provider = new JUnitPlatformProvider( parameters, launcher );

        TestPlanSummaryListener executionListener = new TestPlanSummaryListener();

        TestReportListener<TestOutputReportEntry> listener = mock( TestReportListener.class );
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass( ReportEntry.class );
        RunListenerAdapter adapter = new RunListenerAdapter( listener );
        adapter.setRunMode( NORMAL_RUN );

        launcher.registerTestExecutionListeners( executionListener, adapter );

        invokeProvider( provider, TestClass9.class );

        assertThat( executionListener.summaries ).hasSize( 1 );

        verify( listener, times( 1 ) ).testSetCompleted( any() );
        verify( listener, times( 1 ) ).testFailed( entryCaptor.capture() );
        List<ReportEntry> reportEntries = entryCaptor.getAllValues();

        assertEquals( TestClass9.class.getName(), reportEntries.get( 0 ).getSourceName() );
        assertNull( reportEntries.get( 0 ).getSourceText() );
        assertEquals( "testParameterizedTestCases", reportEntries.get( 0 ).getName() );
        assertNull( reportEntries.get( 0 ).getNameText() );

        TestExecutionSummary summary = executionListener.summaries.get( 0 );
        assertEquals( 0, summary.getTestsFoundCount() );
        assertEquals( 1, summary.getContainersFailedCount() );
        assertEquals( 0, summary.getTestsStartedCount() );
        assertEquals( 0, summary.getTestsSucceededCount() );
        assertEquals( 0, summary.getTestsFailedCount() );
    }

    @Test
    public void rerunParameterized()
            throws Exception
    {
        Launcher launcher = LauncherFactory.create();
        ProviderParameters parameters = providerParametersMock();
        // Mock the rerun variable
        when( parameters.getTestRequest().getRerunFailingTestsCount() ).thenReturn( 2 );
        when( parameters.getProviderProperties() )
                .thenReturn( singletonMap( JUnitPlatformProvider.CONFIGURATION_PARAMETERS,
                        "forkCount = 1\nreuseForks = true" ) );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( parameters, launcher );

        TestPlanSummaryListener executionListener = new TestPlanSummaryListener();

        TestReportListener<TestOutputReportEntry> listener = mock( TestReportListener.class );
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass( ReportEntry.class );
        RunListenerAdapter adapter = new RunListenerAdapter( listener );
        adapter.setRunMode( NORMAL_RUN );

        launcher.registerTestExecutionListeners( executionListener, adapter );

        invokeProvider( provider, TestClass7.class );

        assertThat( executionListener.summaries ).hasSize( 3 );

        verify( listener, times( 4 ) ).testStarting( entryCaptor.capture() );
        List<ReportEntry> reportEntries = entryCaptor.getAllValues();

        assertEquals( TestClass7.class.getName(), reportEntries.get( 0 ).getSourceName() );
        assertNull( reportEntries.get( 0 ).getSourceText() );
        assertEquals( "testParameterizedTestCases(String, boolean)[1]", reportEntries.get( 0 ).getName() );
        assertEquals( "testParameterizedTestCases(String, boolean)[1] Always pass, true",
                       reportEntries.get( 0 ).getNameText() );

        assertEquals( TestClass7.class.getName(), reportEntries.get( 1 ).getSourceName() );
        assertNull( reportEntries.get( 1 ).getSourceText() );
        assertEquals( "testParameterizedTestCases(String, boolean)[2]", reportEntries.get( 1 ).getName() );
        assertEquals( "testParameterizedTestCases(String, boolean)[2] Always fail, false",
                      reportEntries.get( 1 ).getNameText() );

        assertEquals( TestClass7.class.getName(), reportEntries.get( 2 ).getSourceName() );
        assertNull( reportEntries.get( 2 ).getSourceText() );
        assertEquals( "testParameterizedTestCases(String, boolean)[2]", reportEntries.get( 2 ).getName() );
        assertEquals( "testParameterizedTestCases(String, boolean)[2] Always fail, false",
                      reportEntries.get( 2 ).getNameText() );

        assertEquals( TestClass7.class.getName(), reportEntries.get( 3 ).getSourceName() );
        assertNull( reportEntries.get( 3 ).getSourceText() );
        assertEquals( "testParameterizedTestCases(String, boolean)[2]", reportEntries.get( 3 ).getName() );
        assertEquals( "testParameterizedTestCases(String, boolean)[2] Always fail, false",
                      reportEntries.get( 3 ).getNameText() );

        TestExecutionSummary summary = executionListener.summaries.get( 0 );
        assertEquals( 2, summary.getTestsFoundCount() );
        assertEquals( 2, summary.getTestsStartedCount() );
        assertEquals( 1, summary.getTestsSucceededCount() );
        assertEquals( 1, summary.getTestsFailedCount() );

        summary = executionListener.summaries.get( 1 );
        assertEquals( 1, summary.getTestsFoundCount() );
        assertEquals( 1, summary.getTestsStartedCount() );
        assertEquals( 0, summary.getTestsSucceededCount() );
        assertEquals( 1, summary.getTestsFailedCount() );

        summary = executionListener.summaries.get( 2 );
        assertEquals( 1, summary.getTestsFoundCount() );
        assertEquals( 1, summary.getTestsStartedCount() );
        assertEquals( 0, summary.getTestsSucceededCount() );
        assertEquals( 1, summary.getTestsFailedCount() );
    }

    @Test
    public void allDiscoveredTestsAreInvokedForNullArgument()
                    throws Exception
    {
        TestReportListener<TestOutputReportEntry> runListener = runListenerMock();
        ProviderParameters providerParameters =
                        providerParametersMock( runListener, TestClass1.class, TestClass2.class );
        Launcher launcher = LauncherFactory.create();
        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters, launcher );

        TestPlanSummaryListener executionListener = new TestPlanSummaryListener();
        launcher.registerTestExecutionListeners( executionListener );

        invokeProvider( provider, null );

        InOrder inOrder = inOrder( runListener );

        ArgumentCaptor<SimpleReportEntry> report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        inOrder.verify( runListener )
                .testSetStarting( report.capture() );
        assertThat( report.getValue().getSourceName() )
                .isEqualTo( TestClass1.class.getName() );
        assertThat( report.getValue().getName() )
                .isNull();

        report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        inOrder.verify( runListener )
                .testSetCompleted( report.capture() );
        assertThat( report.getValue().getSourceName() )
                .isEqualTo( TestClass1.class.getName() );
        assertThat( report.getValue().getName() )
                .isNull();

        report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        inOrder.verify( runListener )
                .testSetStarting( report.capture() );
        assertThat( report.getValue().getSourceName() )
                .isEqualTo( TestClass2.class.getName() );
        assertThat( report.getValue().getName() )
                .isNull();

        report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        inOrder.verify( runListener )
                .testSetCompleted( report.capture() );
        assertThat( report.getValue().getSourceName() )
                .isEqualTo( TestClass2.class.getName() );
        assertThat( report.getValue().getName() )
                .isNull();

        assertThat( executionListener.summaries ).hasSize( 1 );
        TestExecutionSummary summary = executionListener.summaries.get( 0 );
        assertEquals( TestClass1.TESTS_FOUND + TestClass2.TESTS_FOUND, summary.getTestsFoundCount() );
        assertEquals(
                        TestClass1.TESTS_STARTED + TestClass2.TESTS_STARTED, summary.getTestsStartedCount() );
        assertEquals(
                        TestClass1.TESTS_SKIPPED + TestClass2.TESTS_SKIPPED, summary.getTestsSkippedCount() );
        assertEquals(
                        TestClass1.TESTS_SUCCEEDED + TestClass2.TESTS_SUCCEEDED, summary.getTestsSucceededCount() );
        assertEquals(
                        TestClass1.TESTS_ABORTED + TestClass2.TESTS_ABORTED, summary.getTestsAbortedCount() );
        assertEquals( TestClass1.TESTS_FAILED + TestClass2.TESTS_FAILED, summary.getTestsFailedCount() );
    }

    @Test
    public void outputIsCaptured()
                    throws Exception
    {
        Launcher launcher = LauncherFactory.create();
        TestReportListener<TestOutputReportEntry> runListener = runListenerMock();
        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParametersMock( runListener ), launcher );

        invokeProvider( provider, VerboseTestClass.class );

        ArgumentCaptor<TestOutputReportEntry> captor = ArgumentCaptor.forClass( TestOutputReportEntry.class );
        verify( runListener, times( 2 ) )
                        .writeTestOutput( captor.capture() );
        assertThat( captor.getAllValues().get( 0 ).getLog() )
            .isEqualTo( "stdout" );
        assertThat( captor.getAllValues().get( 1 ).getLog() )
            .isEqualTo( "stderr" );
    }

    @Test
    public void onlyGroupsIsDeclared()
    {
        Map<String, String> properties = singletonMap( TESTNG_GROUPS_PROP, "groupOne, groupTwo" );

        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );
        when( providerParameters.getProviderProperties() ).thenReturn( properties );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertEquals( 1, provider.getFilters().length );
    }

    @Test
    public void onlyExcludeTagsIsDeclared()
    {
        Map<String, String> properties = singletonMap( TESTNG_EXCLUDEDGROUPS_PROP, "tagOne, tagTwo" );

        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );
        when( providerParameters.getProviderProperties() ).thenReturn( properties );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertEquals( 1, provider.getFilters().length );
    }

    @Test
    public void noFiltersAreCreatedIfTagsAreEmpty()
    {
        Map<String, String> properties = singletonMap( TESTNG_GROUPS_PROP, "" );

        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );
        when( providerParameters.getProviderProperties() ).thenReturn( properties );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );
        assertEquals( 0, provider.getFilters().length );
    }

    @Test
    public void filtersWithEmptyTagsAreNotRegistered()
    {
        // Here only tagOne is registered as a valid tag and other tags are ignored as they are empty
        Map<String, String> properties = singletonMap( TESTNG_EXCLUDEDGROUPS_PROP, "tagOne," );

        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );
        when( providerParameters.getProviderProperties() ).thenReturn( properties );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );
        assertEquals( 1, provider.getFilters().length );
    }

    @Test
    public void bothIncludeAndExcludeAreAllowed()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put( TESTNG_GROUPS_PROP, "tagOne, tagTwo" );
        properties.put( TESTNG_EXCLUDEDGROUPS_PROP, "tagThree, tagFour" );

        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );
        when( providerParameters.getProviderProperties() ).thenReturn( properties );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertEquals( 2, provider.getFilters().length );
    }

    @Test
    public void tagExpressionsAreSupportedForIncludeTagsContainingVerticalBar()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put( TESTNG_GROUPS_PROP, "tagOne | tagTwo" );
        properties.put( TESTNG_EXCLUDEDGROUPS_PROP, "tagThree | tagFour" );

        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );
        when( providerParameters.getProviderProperties() ).thenReturn( properties );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertEquals( 2, provider.getFilters().length );
    }

    @Test
    public void tagExpressionsAreSupportedForIncludeTagsContainingAmpersand()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put( TESTNG_GROUPS_PROP, "tagOne & !tagTwo" );
        properties.put( TESTNG_EXCLUDEDGROUPS_PROP, "tagThree & !tagFour" );

        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );
        when( providerParameters.getProviderProperties() ).thenReturn( properties );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertEquals( 2, provider.getFilters().length );
    }

    @Test
    public void onlyIncludeJunit5EnginesIsDeclared()
    {
        Map<String, String> properties = singletonMap( INCLUDE_JUNIT5_ENGINES_PROP, "engine-one, engine-two" );

        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );
        when( providerParameters.getProviderProperties() ).thenReturn( properties );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertThat( provider.getFilters() ).hasSize( 1 );
        assertThat( provider.getFilters()[0] ).isInstanceOf( EngineFilter.class );
    }

    @Test
    public void onlyExcludeJunit5EnginesIsDeclared()
    {
        Map<String, String> properties = singletonMap( EXCLUDE_JUNIT5_ENGINES_PROP, "engine-one, engine-two" );

        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );
        when( providerParameters.getProviderProperties() ).thenReturn( properties );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertThat( provider.getFilters() ).hasSize( 1 );
        assertThat( provider.getFilters()[0] ).isInstanceOf( EngineFilter.class );
    }

    @Test
    public void noFiltersAreCreatedIfIncludeJunit5EnginesIsEmpty()
    {
        Map<String, String> properties = singletonMap( INCLUDE_JUNIT5_ENGINES_PROP, "" );

        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );
        when( providerParameters.getProviderProperties() ).thenReturn( properties );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );
        assertEquals( 0, provider.getFilters().length );

        assertThat( provider.getFilters() ).hasSize( 0 );
    }

    @Test
    public void filtersWithEmptyJunitEngineAreNotRegistered()
    {
        // Here only tagOne is registered as a valid tag and other tags are ignored as they are empty
        Map<String, String> properties = singletonMap( EXCLUDE_JUNIT5_ENGINES_PROP, "engine-one," );

        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );
        when( providerParameters.getProviderProperties() ).thenReturn( properties );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertThat( provider.getFilters() ).hasSize( 1 );
        assertThat( provider.getFilters()[0] ).isInstanceOf( EngineFilter.class );
    }

    @Test
    public void bothIncludeAndExcludeJunit5EnginesAreAllowed()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put( INCLUDE_JUNIT5_ENGINES_PROP, "engine-one, engine-two" );
        properties.put( EXCLUDE_JUNIT5_ENGINES_PROP, "engine-three, engine-four" );

        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );
        when( providerParameters.getProviderProperties() ).thenReturn( properties );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertThat( provider.getFilters() ).hasSize( 2 );
        assertThat( provider.getFilters()[0] ).isInstanceOf( EngineFilter.class );
        assertThat( provider.getFilters()[1] ).isInstanceOf( EngineFilter.class );
    }

    @Test
    public void noFiltersAreCreatedIfNoPropertiesAreDeclared()
    {
        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertEquals( 0, provider.getFilters().length );
    }

    @Test
    public void defaultConfigurationParametersAreEmpty()
    {
        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );
        when( providerParameters.getProviderProperties() ).thenReturn( emptyMap() );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertTrue( provider.getConfigurationParameters().isEmpty() );
    }

    @Test
    public void parsesConfigurationParameters()
    {
        ProviderParameters providerParameters = providerParametersMock( TestClass1.class );
        when( providerParameters.getProviderProperties() )
                        .thenReturn( singletonMap( JUnitPlatformProvider.CONFIGURATION_PARAMETERS,
                                                     "foo = true\nbar 42\rbaz: *\r\nqux: EOF" ) );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertEquals( 4, provider.getConfigurationParameters().size() );
        assertEquals( "true", provider.getConfigurationParameters().get( "foo" ) );
        assertEquals( "42", provider.getConfigurationParameters().get( "bar" ) );
        assertEquals( "*", provider.getConfigurationParameters().get( "baz" ) );
        assertEquals( "EOF", provider.getConfigurationParameters().get( "qux" ) );
    }

    @Test
    public void shouldFilterTestMethod()
    {
        ProviderParameters providerParameters = providerParametersMock();
        TestListResolver testListResolver = new TestListResolver( "**/*Test#test*" );
        assertFalse( testListResolver.isEmpty() );
        assertFalse( testListResolver.isWildcard() );
        TestRequest request = new TestRequest( null, null, testListResolver, 0 );
        when( providerParameters.getTestRequest() ).thenReturn( request );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertThat( provider.getFilters() )
            .hasSize( 1 );

        assertThat( provider.getFilters()[0] )
            .isInstanceOf( TestMethodFilter.class );

        Object expectedTestListResolver = getInternalState( provider.getFilters()[0], "testListResolver" );

        assertThat( expectedTestListResolver )
            .isInstanceOf( TestListResolver.class );

        assertThat( expectedTestListResolver  )
            .isSameAs( testListResolver );
    }

    @Test
    public void shouldNotFilterEmpty()
    {
        ProviderParameters providerParameters = providerParametersMock();
        TestListResolver testListResolver = new TestListResolver( "" );
        assertTrue( testListResolver.isEmpty() );
        assertFalse( testListResolver.isWildcard() );
        TestRequest request = new TestRequest( null, null, testListResolver, 0 );
        when( providerParameters.getTestRequest() ).thenReturn( request );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertThat( provider.getFilters() )
            .isEmpty();
    }

    @Test
    public void shouldNotFilterWildcard()
    {
        ProviderParameters providerParameters = providerParametersMock();
        TestListResolver testListResolver = new TestListResolver( "*.java" );
        assertTrue( testListResolver.isWildcard() );
        assertFalse( testListResolver.isEmpty() );
        TestRequest request = new TestRequest( null, null, testListResolver, 0 );
        when( providerParameters.getTestRequest() ).thenReturn( request );

        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters );

        assertThat( provider.getFilters() )
            .isEmpty();
    }

    @Test
    public void executesSingleTestIncludedByName()
                    throws Exception
    {
        // following is equivalent of adding '-Dtest=TestClass3#prefix1Suffix1'
        // '*' needed because it's a nested class and thus has name prefixed with '$'
        String pattern = "*TestClass3#prefix1Suffix1";

        testExecutionOfMatchingTestMethods( TestClass3.class, pattern, "prefix1Suffix1()" );
    }

    @Test
    public void executesMultipleTestsIncludedByName()
                    throws Exception
    {
        // following is equivalent of adding '-Dtest=TestClass3#prefix1Suffix1+prefix2Suffix1'
        // '*' needed because it's a nested class and thus has name prefixed with '$'
        String pattern = "*TestClass3#prefix1Suffix1+prefix2Suffix1";

        testExecutionOfMatchingTestMethods(
                        TestClass3.class, pattern, "prefix1Suffix1()", "prefix2Suffix1()" );
    }

    @Test
    public void executesMultipleTestsIncludedByNamePattern()
                    throws Exception
    {
        // following is equivalent of adding '-Dtest=TestClass3#prefix1*'
        // '*' needed because it's a nested class and thus has name prefixed with '$'
        String pattern = "*TestClass3#prefix1*";

        testExecutionOfMatchingTestMethods(
                        TestClass3.class, pattern, "prefix1Suffix1()", "prefix1Suffix2()" );
    }

    @Test
    public void executesMultipleTestsIncludedByNamePatternWithQuestionMark()
                    throws Exception
    {
        // following is equivalent of adding '-Dtest=TestClass3#prefix?Suffix2'
        // '*' needed because it's a nested class and thus has name prefixed with '$'
        String pattern = "*TestClass3#prefix?Suffix2";

        testExecutionOfMatchingTestMethods(
                        TestClass3.class, pattern, "prefix1Suffix2()", "prefix2Suffix2()" );
    }

    @Test
    public void doesNotExecuteTestsExcludedByName()
                    throws Exception
    {
        // following is equivalent of adding '-Dtest=!TestClass3#prefix1Suffix2'
        // '*' needed because it's a nested class and thus has name prefixed with '$'
        String pattern = "!*TestClass3#prefix1Suffix2";

        testExecutionOfMatchingTestMethods(
                        TestClass3.class, pattern, "prefix1Suffix1()", "prefix2Suffix1()", "prefix2Suffix2()" );
    }

    @Test
    public void doesNotExecuteTestsExcludedByNamePattern()
                    throws Exception
    {
        // following is equivalent of adding '-Dtest=!TestClass3#prefix2*'
        // '*' needed because it's a nested class and thus has name prefixed with '$'
        String pattern = "!*TestClass3#prefix2*";

        testExecutionOfMatchingTestMethods(
                        TestClass3.class, pattern, "prefix1Suffix1()", "prefix1Suffix2()" );
    }

    private static void testExecutionOfMatchingTestMethods( Class<?> testClass, String pattern,
                                                            String... expectedTestNames )
                    throws Exception
    {
        TestListResolver testListResolver = new TestListResolver( pattern );
        ProviderParameters providerParameters = providerParametersMock( testListResolver, testClass );
        Launcher launcher = LauncherFactory.create();
        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParameters, launcher );

        TestPlanSummaryListener executionListener = new TestPlanSummaryListener();
        launcher.registerTestExecutionListeners( executionListener );

        invokeProvider( provider, null );

        assertEquals( 1, executionListener.summaries.size() );
        TestExecutionSummary summary = executionListener.summaries.get( 0 );
        int expectedCount = expectedTestNames.length;
        assertEquals( expectedCount, summary.getTestsFoundCount() );
        assertEquals( expectedCount, summary.getTestsFailedCount() );
        assertEquals( expectedCount, summary.getFailures().size() );

        assertThat( failedTestDisplayNames( summary ) ).contains( expectedTestNames );
    }

    private static ProviderParameters providerParametersMock( Class<?>... testClasses )
    {
        return providerParametersMock( runListenerMock(), testClasses );
    }

    private static ProviderParameters providerParametersMock(
            TestReportListener<TestOutputReportEntry> runListener, Class<?>... testClasses )
    {
        TestListResolver testListResolver = new TestListResolver( "" );
        return providerParametersMock( runListener, testListResolver, testClasses );
    }

    private static ProviderParameters providerParametersMock(
                    TestListResolver testListResolver, Class<?>... testClasses )
    {
        return providerParametersMock( runListenerMock(), testListResolver, testClasses );
    }

    private static ProviderParameters providerParametersMock( TestReportListener<TestOutputReportEntry> runListener,
                                                              TestListResolver testListResolver,
                                                              Class<?>... testClasses )
    {
        TestsToRun testsToRun = newTestsToRun( testClasses );

        ScanResult scanResult = mock( ScanResult.class );
        when( scanResult.applyFilter( any(), any() ) ).thenReturn( testsToRun );

        RunOrderCalculator runOrderCalculator = mock( RunOrderCalculator.class );
        when( runOrderCalculator.orderTestClasses( any() ) ).thenReturn( testsToRun );

        ReporterFactory reporterFactory = mock( ReporterFactory.class );
        when( reporterFactory.createTestReportListener() ).thenReturn( runListener );

        TestRequest testRequest = mock( TestRequest.class );
        when( testRequest.getTestListResolver() ).thenReturn( testListResolver );

        ProviderParameters providerParameters = mock( ProviderParameters.class );
        when( providerParameters.getScanResult() ).thenReturn( scanResult );
        when( providerParameters.getRunOrderCalculator() ).thenReturn( runOrderCalculator );
        when( providerParameters.getReporterFactory() ).thenReturn( reporterFactory );
        when( providerParameters.getTestRequest() ).thenReturn( testRequest );

        return providerParameters;
    }

    private static TestReportListener<TestOutputReportEntry> runListenerMock()
    {
        return mock( TestReportListener.class, withSettings().extraInterfaces( TestOutputReceiver.class ) );
    }

    private static Set<String> failedTestDisplayNames( TestExecutionSummary summary )
    {
        // @formatter:off
        return summary.getFailures()
                .stream()
                .map( Failure::getTestIdentifier )
                .map( TestIdentifier::getDisplayName )
                .collect( toSet() );
        // @formatter:on
    }

    private static TestsToRun newTestsToRun( Class<?>... testClasses )
    {
        List<Class<?>> classesList = Arrays.asList( testClasses );
        return new TestsToRun( new LinkedHashSet<>( classesList ) );
    }

    private static TestsToRun newTestsToRunLazily( Class<?>... testClasses )
    {
        class LazyTestsToRunFake extends TestsToRun
        {
            LazyTestsToRunFake( Set<Class<?>> locatedClasses )
            {
                super( locatedClasses );
            }

            @Override
            public boolean allowEagerReading()
            {
                return false;
            }
        }
        List<Class<?>> classesList = Arrays.asList( testClasses );
        return new LazyTestsToRunFake( new LinkedHashSet<>( classesList ) );
    }

    private static class TestPlanSummaryListener
                    extends SummaryGeneratingListener
    {
        private final List<TestExecutionSummary> summaries = new ArrayList<>();

        @Override
        public void testPlanExecutionFinished( TestPlan testPlan )
        {
            super.testPlanExecutionFinished( testPlan );
            summaries.add( getSummary() );
        }
    }

    /**
     * Invokes the provider, then restores system out and system error.
     *
     * @see <a href="https://github.com/junit-team/junit5/issues/986">#986</a>
     */
    private static void invokeProvider( JUnitPlatformProvider provider, Object forkTestSet )
                    throws TestSetFailedException
    {
        PrintStream systemOut = System.out;
        PrintStream systemErr = System.err;
        try
        {
            provider.invoke( forkTestSet );
        }
        finally
        {
            System.setOut( systemOut );
            System.setErr( systemErr );
        }
    }

    static class TestClass1
    {

        static final int TESTS_FOUND = 4;

        static final int TESTS_STARTED = 3;

        static final int TESTS_SKIPPED = 1;

        static final int TESTS_SUCCEEDED = 2;

        static final int TESTS_ABORTED = 0;

        static final int TESTS_FAILED = 1;

        @org.junit.jupiter.api.Test
        void test1()
        {
        }

        @org.junit.jupiter.api.Test
        void test2()
        {
        }

        @Disabled
        @org.junit.jupiter.api.Test
        void test3()
        {
        }

        @org.junit.jupiter.api.Test
        void test4()
        {
            throw new RuntimeException();
        }
    }

    static class TestClass2
    {

        static final int TESTS_FOUND = 3;

        static final int TESTS_STARTED = 3;

        static final int TESTS_SKIPPED = 0;

        static final int TESTS_SUCCEEDED = 1;

        static final int TESTS_ABORTED = 1;

        static final int TESTS_FAILED = 1;

        @org.junit.jupiter.api.Test
        void test1()
        {
        }

        @org.junit.jupiter.api.Test
        void test2()
        {
            throw new RuntimeException();
        }

        @org.junit.jupiter.api.Test
        void test3()
        {
            assumeTrue( false );
        }
    }

    static class VerboseTestClass
    {
        @org.junit.jupiter.api.Test
        void test()
        {
            System.out.println( "stdout" );
            System.err.println( "stderr" );
        }
    }

    @Test
    public void usesClassNamesForXmlReport()
                    throws TestSetFailedException
    {
        String[] classNames = { Sub1Tests.class.getName(), Sub2Tests.class.getName() };
        ProviderParameters providerParameters =
                        providerParametersMock( Sub1Tests.class, Sub2Tests.class );

        JUnitPlatformProvider jUnitPlatformProvider = new JUnitPlatformProvider( providerParameters );
        TestsToRun testsToRun = newTestsToRun( Sub1Tests.class, Sub2Tests.class );

        invokeProvider( jUnitPlatformProvider, testsToRun );
        RunListener reporter = providerParameters.getReporterFactory().createTestReportListener();

        ArgumentCaptor<ReportEntry> reportEntryArgumentCaptor =
                        ArgumentCaptor.forClass( ReportEntry.class );
        verify( reporter, times( 2 ) ).testSucceeded( reportEntryArgumentCaptor.capture() );

        List<ReportEntry> allValues = reportEntryArgumentCaptor.getAllValues();
        assertThat( allValues ).extracting( ReportEntry::getSourceName ).containsExactly( classNames );
    }

    static class AbstractTestClass
    {
        @org.junit.jupiter.api.Test
        void test()
        {
        }
    }

    static class Sub1Tests
                    extends AbstractTestClass
    {
    }

    static class Sub2Tests
                    extends AbstractTestClass
    {
    }

    static class TestClass3
    {
        @org.junit.jupiter.api.Test
        void prefix1Suffix1()
        {
            throw new RuntimeException();
        }

        @org.junit.jupiter.api.Test
        void prefix2Suffix1()
        {
            throw new RuntimeException();
        }

        @org.junit.jupiter.api.Test
        void prefix1Suffix2()
        {
            throw new RuntimeException();
        }

        @org.junit.jupiter.api.Test
        void prefix2Suffix2()
        {
            throw new RuntimeException();
        }
    }

    static class TestClass4
    {
        static int count;

        @org.junit.jupiter.api.DisplayName( "Always passes" )
        @org.junit.jupiter.api.Test
        void testPass()
        {
        }

        @org.junit.jupiter.api.Test
        void testAborted()
        {
            assumeFalse( true );
            throw new IllegalStateException( "this exception should never happen" );
        }

        @org.junit.jupiter.api.Test
        void testAlwaysError()
        {
            throw new Error( "some error" );
        }

        @org.junit.jupiter.api.Test
        void testAlwaysFail()
        {
            assertTrue( false );
        }

        @org.junit.jupiter.api.Test
        @org.junit.jupiter.api.Disabled
        void testAlwaysSkipped()
        {
            throw new IllegalStateException( "this test should be never called" );
        }

        @org.junit.jupiter.api.DisplayName( "Fails twice" )
        @org.junit.jupiter.api.Test
        void testFailTwice()
        {
            count += 1;
            assertTrue( count >= 3 );
        }
    }

    static class TestClass5
    {
        static int count;

        @org.junit.jupiter.api.Test
        void testFailTwice1()
        {
            count += 1;
            assertTrue( count >= 3 );
        }
    }

    static class TestClass7
    {
        static List<Object[]> params()
        {
            return Arrays.asList(  new Object[] { "Always pass", true },
                    new Object[] { "Always fail", false }  );
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.MethodSource( "params" )
        void testParameterizedTestCases( String testName, boolean value )
        {
            assertTrue( value );
        }
    }

    static class TestClass8
    {
        static List<Object[]> params()
        {
            throw new RuntimeException();
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.MethodSource( "params" )
        void testParameterizedTestCases()
        {
        }
    }

    static class TestClass9
    {
        static List<Object[]> params()
        {
            assertTrue( false );
            return new ArrayList<>();
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.MethodSource( "params" )
        void testParameterizedTestCases()
        {
        }
    }

    @DisplayName( "<< ✨ >>" )
    static class DisplayNameTest
    {
        @org.junit.jupiter.api.Test
        @DisplayName( "73$71 ✔" )
        void test1()
        {
        }
    }

    static class FailingBeforeAllJupiterTest
    {

        @BeforeAll
        static void oneTimeSetUp()
        {
            fail( "oneTimeSetUp() failed" );
        }

        @org.junit.jupiter.api.Test
        void test()
        {
        }

    }

    static class FailingWithErrorBeforeAllJupiterTest
    {

        @BeforeAll
        static void oneTimeSetUp()
        {
            throw new RuntimeException( "oneTimeSetUp() threw an exception" );
        }

        @org.junit.jupiter.api.Test
        void test()
        {
        }

    }
}
