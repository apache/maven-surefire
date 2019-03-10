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
import static org.apache.maven.surefire.booter.ProviderParameterNames.TESTNG_EXCLUDEDGROUPS_PROP;
import static org.apache.maven.surefire.booter.ProviderParameterNames.TESTNG_GROUPS_PROP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.TestsToRun;
import org.fest.assertions.Assertions;
import org.junit.Test;
import org.junit.jupiter.api.Disabled;
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
    public void allDiscoveredTestsAreInvokedForNullArgument()
                    throws Exception
    {
        RunListener runListener = runListenerMock();
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
        Assertions.assertThat( report.getValue().getSourceName() )
                .isEqualTo( TestClass1.class.getName() );
        Assertions.assertThat( report.getValue().getName() )
                .isNull();

        report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        inOrder.verify( runListener )
                .testSetCompleted( report.capture() );
        Assertions.assertThat( report.getValue().getSourceName() )
                .isEqualTo( TestClass1.class.getName() );
        Assertions.assertThat( report.getValue().getName() )
                .isNull();

        report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        inOrder.verify( runListener )
                .testSetStarting( report.capture() );
        Assertions.assertThat( report.getValue().getSourceName() )
                .isEqualTo( TestClass2.class.getName() );
        Assertions.assertThat( report.getValue().getName() )
                .isNull();

        report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        inOrder.verify( runListener )
                .testSetCompleted( report.capture() );
        Assertions.assertThat( report.getValue().getSourceName() )
                .isEqualTo( TestClass2.class.getName() );
        Assertions.assertThat( report.getValue().getName() )
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
        RunListener runListener = runListenerMock();
        JUnitPlatformProvider provider = new JUnitPlatformProvider( providerParametersMock( runListener ), launcher );

        invokeProvider( provider, VerboseTestClass.class );

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass( String.class );
        verify( (ConsoleOutputReceiver) runListener )
                        .writeTestOutput( captor.capture(), eq( true ), eq( true ) );
        verify( (ConsoleOutputReceiver) runListener )
                        .writeTestOutput( captor.capture(), eq( true ), eq( false ) );
        assertThat( captor.getAllValues() )
                .hasSize( 2 )
                .containsExactly( "stdout", "stderr" );
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
                    RunListener runListener, Class<?>... testClasses )
    {
        TestListResolver testListResolver = new TestListResolver( "" );
        return providerParametersMock( runListener, testListResolver, testClasses );
    }

    private static ProviderParameters providerParametersMock(
                    TestListResolver testListResolver, Class<?>... testClasses )
    {
        return providerParametersMock( runListenerMock(), testListResolver, testClasses );
    }

    private static ProviderParameters providerParametersMock(
                    RunListener runListener, TestListResolver testListResolver, Class<?>... testClasses )
    {
        TestsToRun testsToRun = newTestsToRun( testClasses );

        ScanResult scanResult = mock( ScanResult.class );
        when( scanResult.applyFilter( any(), any() ) ).thenReturn( testsToRun );

        RunOrderCalculator runOrderCalculator = mock( RunOrderCalculator.class );
        when( runOrderCalculator.orderTestClasses( any() ) ).thenReturn( testsToRun );

        ReporterFactory reporterFactory = mock( ReporterFactory.class );
        when( reporterFactory.createReporter() ).thenReturn( runListener );

        TestRequest testRequest = mock( TestRequest.class );
        when( testRequest.getTestListResolver() ).thenReturn( testListResolver );

        ProviderParameters providerParameters = mock( ProviderParameters.class );
        when( providerParameters.getScanResult() ).thenReturn( scanResult );
        when( providerParameters.getRunOrderCalculator() ).thenReturn( runOrderCalculator );
        when( providerParameters.getReporterFactory() ).thenReturn( reporterFactory );
        when( providerParameters.getTestRequest() ).thenReturn( testRequest );

        return providerParameters;
    }

    private static RunListener runListenerMock()
    {
        return mock( RunListener.class, withSettings().extraInterfaces( ConsoleOutputReceiver.class ) );
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
        RunListener reporter = providerParameters.getReporterFactory().createReporter();

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
}
