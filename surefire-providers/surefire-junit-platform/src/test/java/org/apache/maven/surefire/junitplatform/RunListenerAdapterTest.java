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

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.platform.engine.TestDescriptor.Type.CONTAINER;
import static org.junit.platform.engine.TestDescriptor.Type.TEST;
import static org.junit.platform.engine.TestExecutionResult.aborted;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Optional;

import org.apache.maven.surefire.report.PojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.report.TestSetReportEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.opentest4j.TestSkippedException;

/**
 * Unit tests for {@link RunListenerAdapter}.
 *
 * @since 2.22.0
 */
public class RunListenerAdapterTest
{
    private static final ConfigurationParameters CONFIG_PARAMS = mock( ConfigurationParameters.class );

    private RunListener listener;

    private RunListenerAdapter adapter;

    @Before
    public void setUp()
    {
        listener = mock( RunListener.class );
        adapter = new RunListenerAdapter( listener );
        adapter.testPlanExecutionStarted( TestPlan.from( emptyList() ) );
    }

    @Test
    public void notifiedWithCorrectNamesWhenMethodExecutionStarted()
                    throws Exception
    {
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass( ReportEntry.class );

        TestPlan testPlan = TestPlan.from( singletonList( new EngineDescriptor( newId(), "Luke's Plan" ) ) );
        adapter.testPlanExecutionStarted( testPlan );

        TestIdentifier methodIdentifier =
                identifiersAsParentOnTestPlan( testPlan, newClassDescriptor(), newMethodDescriptor() );

        adapter.executionStarted( methodIdentifier );
        verify( listener ).testStarting( entryCaptor.capture() );

        ReportEntry entry = entryCaptor.getValue();
        assertEquals( MY_TEST_METHOD_NAME, entry.getName() );
        assertEquals( MyTestClass.class.getName(), entry.getSourceName() );
        assertNull( entry.getStackTraceWriter() );
    }

    @Test
    public void notifiedWithCompatibleNameForMethodWithArguments()
                    throws Exception
    {
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass( ReportEntry.class );

        TestPlan testPlan = TestPlan.from( singletonList( new EngineDescriptor( newId(), "Luke's Plan" ) ) );
        adapter.testPlanExecutionStarted( testPlan );

        TestIdentifier methodIdentifier = identifiersAsParentOnTestPlan( testPlan, newClassDescriptor(),
                newMethodDescriptor( String.class ) );

        adapter.executionStarted( methodIdentifier );
        verify( listener ).testStarting( entryCaptor.capture() );

        ReportEntry entry = entryCaptor.getValue();
        assertEquals( MY_TEST_METHOD_NAME + "(String)", entry.getName() );
        assertEquals( MyTestClass.class.getName(), entry.getSourceName() );
        assertNull( entry.getStackTraceWriter() );
    }

    @Test
    public void notifiedEagerlyForTestSetWhenClassExecutionStarted()
                    throws Exception
    {
        EngineDescriptor engine = newEngineDescriptor();
        TestDescriptor parent = newClassDescriptor();
        engine.addChild( parent );
        TestDescriptor child = newMethodDescriptor();
        parent.addChild( child );
        TestPlan plan = TestPlan.from( singletonList( engine ) );

        adapter.testPlanExecutionStarted( plan );
        adapter.executionStarted( TestIdentifier.from( engine ) );
        adapter.executionStarted( TestIdentifier.from( parent ) );
        verify( listener ).testSetStarting( new SimpleReportEntry( MyTestClass.class.getName(), null ) );
        verifyNoMoreInteractions( listener );

        adapter.executionStarted( TestIdentifier.from( child ) );
        verify( listener ).testStarting( new SimpleReportEntry( MyTestClass.class.getName(), MY_TEST_METHOD_NAME ) );
        verifyNoMoreInteractions( listener );

        adapter.executionFinished( TestIdentifier.from( child ), successful() );
        ArgumentCaptor<SimpleReportEntry> report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        verify( listener ).testSucceeded( report.capture() );
        assertThat( report.getValue().getSourceName() )
                .isEqualTo( MyTestClass.class.getName() );
        assertThat( report.getValue().getName() )
                .isEqualTo( MY_TEST_METHOD_NAME );
        assertThat( report.getValue().getElapsed() )
                .isNotNull();
        assertThat( report.getValue().getSystemProperties() )
                .isEmpty();
        verifyNoMoreInteractions( listener );

        adapter.executionFinished( TestIdentifier.from( parent ), successful() );
        report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        verify( listener ).testSetCompleted( report.capture() );
        assertThat( report.getValue().getSourceName() )
                .isEqualTo( MyTestClass.class.getName() );
        assertThat( report.getValue().getName() )
                .isNull();
        assertThat( report.getValue().getElapsed() )
                .isNotNull();
        assertThat( report.getValue().getSystemProperties() )
                .isNotEmpty();
        verifyNoMoreInteractions( listener );

        adapter.executionFinished( TestIdentifier.from( engine ), successful() );
        verifyNoMoreInteractions( listener );
    }

    @Test
    public void displayNamesInClassAndMethods()
    {
        EngineDescriptor engine = newEngineDescriptor();
        TestDescriptor parent = newClassDescriptor( "parent" );
        engine.addChild( parent );
        TestDescriptor child1 = newTestDescriptor( parent.getUniqueId().append( "test", "child1" ), "child1", TEST );
        parent.addChild( child1 );
        TestDescriptor child2 = newTestDescriptor( parent.getUniqueId().append( "test", "child2" ), "child2", TEST );
        parent.addChild( child2 );
        TestPlan plan = TestPlan.from( singletonList( engine ) );

        InOrder inOrder = inOrder( listener );

        adapter.testPlanExecutionStarted( plan );

        adapter.executionStarted( TestIdentifier.from( engine ) );
        adapter.executionStarted( TestIdentifier.from( parent ) );
        ArgumentCaptor<SimpleReportEntry> report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        inOrder.verify( listener ).testSetStarting( report.capture() );
        assertThat( report.getValue().getSourceName() )
                .isEqualTo( "parent" );
        assertThat( report.getValue().getName() )
                .isNull();
        assertThat( report.getValue().getSystemProperties() )
                .isEmpty();
        verifyZeroInteractions( listener );

        adapter.executionStarted( TestIdentifier.from( child1 ) );
        inOrder.verify( listener ).testStarting( new SimpleReportEntry( "parent", "child1" ) );
        verifyNoMoreInteractions( listener );

        adapter.executionFinished( TestIdentifier.from( child1 ), successful() );
        report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        inOrder.verify( listener ).testSucceeded( report.capture() );
        assertThat( report.getValue().getSourceName() )
                .isEqualTo( "parent" );
        assertThat( report.getValue().getName() )
                .isEqualTo( "child1" );
        assertThat( report.getValue().getElapsed() )
                .isNotNull();
        assertThat( report.getValue().getSystemProperties() )
                .isEmpty();
        verifyNoMoreInteractions( listener );

        adapter.executionStarted( TestIdentifier.from( child2 ) );
        inOrder.verify( listener ).testStarting( new SimpleReportEntry( "parent", "child2" ) );
        verifyNoMoreInteractions( listener );

        adapter.executionFinished( TestIdentifier.from( child2 ), successful() );
        report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        inOrder.verify( listener ).testSucceeded( report.capture() );
        assertThat( report.getValue().getSourceName() )
                .isEqualTo( "parent" );
        assertThat( report.getValue().getName() )
                .isEqualTo( "child2" );
        assertThat( report.getValue().getElapsed() )
                .isNotNull();
        assertThat( report.getValue().getSystemProperties() )
                .isEmpty();
        verifyNoMoreInteractions( listener );

        adapter.executionFinished( TestIdentifier.from( parent ), successful() );
        inOrder.verify( listener ).testSetCompleted( report.capture() );
        assertThat( report.getValue().getSourceName() )
                .isEqualTo( "parent" );
        assertThat( report.getValue().getName() )
                .isNull();
        assertThat( report.getValue().getElapsed() )
                .isNotNull();
        assertThat( report.getValue().getSystemProperties() )
                .isNotEmpty();
        verifyNoMoreInteractions( listener );

        adapter.executionFinished( TestIdentifier.from( engine ), successful() );
        verifyNoMoreInteractions( listener );
    }

    @Test
    public void notifiedForUnclassifiedTestIdentifier()
    {
        EngineDescriptor engine = new EngineDescriptor( UniqueId.forEngine( "engine" ), "engine" )
        {
            @Override
            public Type getType()
            {
                return TEST;
            }
        };
        TestPlan plan = TestPlan.from( singletonList( engine ) );

        adapter.testPlanExecutionStarted( plan );
        adapter.executionStarted( TestIdentifier.from( engine ) );
        verify( listener ).testStarting( new SimpleReportEntry( "engine", "engine" ) );
        verifyNoMoreInteractions( listener );

        adapter.executionFinished( TestIdentifier.from( engine ), successful() );
        ArgumentCaptor<SimpleReportEntry> report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        verify( listener ).testSucceeded( report.capture() );
        assertThat( report.getValue().getSourceName() )
                .isEqualTo( "engine" );
        assertThat( report.getValue().getName() )
                .isEqualTo( "engine" );
        assertThat( report.getValue().getElapsed() )
                .isNotNull();
        assertThat( report.getValue().getSystemProperties() )
                .isEmpty();
        verifyNoMoreInteractions( listener );
    }

    @Test
    public void notNotifiedWhenEngineExecutionStarted()
    {
        adapter.executionStarted( newEngineIdentifier() );
        verify( listener, never() ).testStarting( any() );
    }

    @Test
    public void notifiedWhenMethodExecutionSkipped()
                    throws Exception
    {
        adapter.executionSkipped( newMethodIdentifier(), "test" );
        verify( listener ).testSkipped( any() );
    }

    @Test
    public void notifiedWithCorrectNamesWhenClassExecutionSkipped()
    {
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass( ReportEntry.class );
        TestPlan testPlan = TestPlan.from( singletonList( new EngineDescriptor( newId(), "Luke's Plan" ) ) );
        adapter.testPlanExecutionStarted( testPlan );

        TestIdentifier classIdentifier =
                        identifiersAsParentOnTestPlan( testPlan, newEngineDescriptor(), newClassDescriptor() );

        adapter.executionSkipped( classIdentifier, "test" );
        verify( listener ).testSkipped( entryCaptor.capture() );

        ReportEntry entry = entryCaptor.getValue();
        assertNull( entry.getName() );
        assertEquals( MyTestClass.class.getTypeName(), entry.getSourceName() );
    }

    @Test
    public void notifiedWhenEngineExecutionSkipped()
    {
        adapter.executionSkipped( newEngineIdentifier(), "test" );
        verify( listener ).testSkipped( any() );
    }

    @Test
    public void notifiedWhenMethodExecutionAborted()
                    throws Exception
    {
        adapter.executionFinished( newMethodIdentifier(), aborted( null ) );
        verify( listener ).testAssumptionFailure( any() );
    }

    @Test
    public void notifiedWhenClassExecutionAborted()
    {
        TestSkippedException t = new TestSkippedException( "skipped" );
        adapter.executionFinished( newClassIdentifier(), aborted( t ) );
        String source = MyTestClass.class.getName();
        StackTraceWriter stw = new PojoStackTraceWriter( source, null, t );
        ArgumentCaptor<SimpleReportEntry> report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        verify( listener ).testSetCompleted( report.capture() );
        assertThat( report.getValue().getSourceName() )
                .isEqualTo( source );
        assertThat( report.getValue().getStackTraceWriter() )
                .isEqualTo( stw );
    }

    @Test
    public void notifiedWhenMethodExecutionFailed()
                    throws Exception
    {
        adapter.executionFinished( newMethodIdentifier(), failed( new AssertionError() ) );
        verify( listener ).testFailed( any() );
    }

    @Test
    public void notifiedWhenMethodExecutionFailedWithError()
                    throws Exception
    {
        adapter.executionFinished( newMethodIdentifier(), failed( new RuntimeException() ) );
        verify( listener ).testError( any() );
    }

    @Test
    public void notifiedWithCorrectNamesWhenClassExecutionFailed()
    {
        ArgumentCaptor<TestSetReportEntry> entryCaptor = ArgumentCaptor.forClass( TestSetReportEntry.class );
        TestPlan testPlan = TestPlan.from( singletonList( new EngineDescriptor( newId(), "Luke's Plan" ) ) );
        adapter.testPlanExecutionStarted( testPlan );

        adapter.executionFinished( identifiersAsParentOnTestPlan( testPlan, newClassDescriptor() ),
                failed( new AssertionError() ) );
        verify( listener ).testSetCompleted( entryCaptor.capture() );

        ReportEntry entry = entryCaptor.getValue();
        assertEquals( MyTestClass.class.getTypeName(), entry.getSourceName() );
        assertNull( entry.getName() );
        assertNotNull( entry.getStackTraceWriter() );
        assertNotNull( entry.getStackTraceWriter().getThrowable() );
        assertThat( entry.getStackTraceWriter().getThrowable().getTarget() )
                .isInstanceOf( AssertionError.class );
    }

    @Test
    public void notifiedWhenMethodExecutionSucceeded()
                    throws Exception
    {
        adapter.executionFinished( newMethodIdentifier(), successful() );
        verify( listener ).testSucceeded( any() );
    }

    @Test
    public void notifiedForTestSetWhenClassExecutionSucceeded()
    {
        EngineDescriptor engineDescriptor = newEngineDescriptor();
        TestDescriptor classDescriptor = newClassDescriptor();
        engineDescriptor.addChild( classDescriptor );
        adapter.testPlanExecutionStarted( TestPlan.from( singleton( engineDescriptor ) ) );
        adapter.executionStarted( TestIdentifier.from( classDescriptor ) );

        adapter.executionFinished( TestIdentifier.from( classDescriptor ), successful() );

        verify( listener ).testSetStarting( new SimpleReportEntry( MyTestClass.class.getName(), null ) );

        ArgumentCaptor<SimpleReportEntry> report = ArgumentCaptor.forClass( SimpleReportEntry.class );
        verify( listener ).testSetCompleted( report.capture() );
        assertThat( report.getValue().getSourceName() )
                .isEqualTo( MyTestClass.class.getName() );
        assertThat( report.getValue().getName() )
                .isNull();
        assertThat( report.getValue().getStackTraceWriter() )
                .isNull();
        assertThat( report.getValue().getElapsed() )
                .isNotNull();

        verify( listener, never() ).testSucceeded( any() );
    }

    @Test
    public void notifiedWithParentDisplayNameWhenTestClassUnknown()
    {
        // Set up a test plan
        TestPlan plan = TestPlan.from( singletonList( new EngineDescriptor( newId(), "Luke's Plan" ) ) );
        adapter.testPlanExecutionStarted( plan );

        // Use the test plan to set up child with parent.
        final String parentDisplay = "I am your father";
        TestIdentifier child = newSourcelessChildIdentifierWithParent( plan, parentDisplay, null );
        adapter.executionStarted( child );

        // Check that the adapter has informed Surefire that the test has been invoked,
        // with the parent name as source (since the test case itself had no source).
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass( ReportEntry.class );
        verify( listener ).testStarting( entryCaptor.capture() );
        assertEquals( parentDisplay, entryCaptor.getValue().getSourceName() );
    }

    @Test
    public void stackTraceWriterPresentWhenParentHasSource()
    {
        TestPlan plan = TestPlan.from( singletonList( new EngineDescriptor( newId(), "Some Plan" ) ) );
        adapter.testPlanExecutionStarted( plan );

        TestIdentifier child =
                newSourcelessChildIdentifierWithParent( plan, "Parent", ClassSource.from( MyTestClass.class ) );
        adapter.executionFinished( child, failed( new RuntimeException() ) );
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass( ReportEntry.class );
        verify( listener ).testError( entryCaptor.capture() );
        assertNotNull( entryCaptor.getValue().getStackTraceWriter() );
    }

    @Test
    public void stackTraceWriterDefaultsToTestClass()
    {
        TestPlan plan = TestPlan.from( singletonList( new EngineDescriptor( newId(), "Some Plan" ) ) );
        adapter.testPlanExecutionStarted( plan );

        TestIdentifier child = newSourcelessChildIdentifierWithParent( plan, "Parent", null );
        adapter.executionFinished( child, failed( new RuntimeException( "message" ) ) );
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass( ReportEntry.class );
        verify( listener ).testError( entryCaptor.capture() );
        assertNotNull( entryCaptor.getValue().getStackTraceWriter() );
        assertNotNull( entryCaptor.getValue().getStackTraceWriter().smartTrimmedStackTrace() );
        assertNotNull( entryCaptor.getValue().getStackTraceWriter().writeTraceToString() );
        assertNotNull( entryCaptor.getValue().getStackTraceWriter().writeTrimmedTraceToString() );
    }

    @Test
    public void stackTraceWriterPresentEvenWithoutException()
                    throws Exception
    {
        adapter.executionFinished( newMethodIdentifier(), failed( null ) );
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass( ReportEntry.class );
        verify( listener ).testError( entryCaptor.capture() );
        assertNotNull( entryCaptor.getValue().getStackTraceWriter() );
    }

    @Test
    public void displayNamesIgnoredInReport()
                    throws NoSuchMethodException
    {
        class TestMethodTestDescriptorWithDisplayName extends AbstractTestDescriptor
        {
            private TestMethodTestDescriptorWithDisplayName( UniqueId uniqueId, Class<?> testClass, Method testMethod )
            {
                super( uniqueId, "some display name", MethodSource.from( testClass, testMethod ) );
            }

            @Override
            public Type getType()
            {
                return Type.TEST;
            }
        }

        TestMethodTestDescriptorWithDisplayName descriptor = new TestMethodTestDescriptorWithDisplayName( newId(),
                MyTestClass.class, MyTestClass.class.getDeclaredMethod( "myNamedTestMethod" ) );

        TestIdentifier factoryIdentifier = TestIdentifier.from( descriptor );
        ArgumentCaptor<ReportEntry> entryCaptor = ArgumentCaptor.forClass( ReportEntry.class );

        adapter.executionSkipped( factoryIdentifier, "" );
        verify( listener ).testSkipped( entryCaptor.capture() );

        ReportEntry value = entryCaptor.getValue();

        assertEquals( "some display name", value.getName() );
    }

    private static TestIdentifier newMethodIdentifier()
                    throws Exception
    {
        return TestIdentifier.from( newMethodDescriptor() );
    }

    private static TestDescriptor newMethodDescriptor( Class<?>... parameterTypes )
                    throws Exception
    {
        return new TestMethodTestDescriptor(
                        UniqueId.forEngine( "method" ),
                        MyTestClass.class,
                        MyTestClass.class.getDeclaredMethod( MY_TEST_METHOD_NAME, parameterTypes ) );
    }

    private static TestIdentifier newClassIdentifier()
    {
        return TestIdentifier.from( newClassDescriptor() );
    }

    private static TestDescriptor newClassDescriptor( String displayName )
    {
        return new ClassTestDescriptor( UniqueId.root( "class", MyTestClass.class.getName() ),
                c -> displayName, MyTestClass.class, CONFIG_PARAMS ) {};
    }

    private static TestDescriptor newClassDescriptor()
    {
        return new ClassTestDescriptor( UniqueId.root( "class", MyTestClass.class.getName() ),
                MyTestClass.class, CONFIG_PARAMS );
    }

    private static TestIdentifier newSourcelessChildIdentifierWithParent(
                    TestPlan testPlan, String parentDisplay, TestSource parentTestSource )
    {
        // A parent test identifier with a name.
        TestDescriptor parent = mock( TestDescriptor.class );
        when( parent.getUniqueId() ).thenReturn( newId() );
        when( parent.getDisplayName() ).thenReturn( parentDisplay );
        when( parent.getLegacyReportingName() ).thenReturn( parentDisplay );
        when( parent.getSource() ).thenReturn( Optional.ofNullable( parentTestSource ) );
        when( parent.getType() ).thenReturn( CONTAINER );
        TestIdentifier parentId = TestIdentifier.from( parent );

        // The (child) test case that is to be executed as part of a test plan.
        TestDescriptor child = mock( TestDescriptor.class );
        when( child.getUniqueId() ).thenReturn( newId() );
        when( child.getType() ).thenReturn( TEST );
        when( child.getLegacyReportingName() ).thenReturn( "child" );

        // Ensure the child source is null yet that there is a parent -- the special case to be tested.
        when( child.getSource() ).thenReturn( Optional.empty() );
        when( child.getParent() ).thenReturn( Optional.of( parent ) );
        TestIdentifier childId = TestIdentifier.from( child );

        testPlan.add( childId );
        testPlan.add( parentId );

        return childId;
    }

    private static TestIdentifier newEngineIdentifier()
    {
        TestDescriptor testDescriptor = newEngineDescriptor();
        return TestIdentifier.from( testDescriptor );
    }

    private static EngineDescriptor newEngineDescriptor()
    {
        return new EngineDescriptor( UniqueId.forEngine( "engine" ), "engine" );
    }

    private TestDescriptor newTestDescriptor( UniqueId uniqueId, String displayName, Type type )
    {
        return new AbstractTestDescriptor( uniqueId, displayName )
        {
            @Override
            public Type getType()
            {
                return type;
            }
        };
    }

    private static TestIdentifier identifiersAsParentOnTestPlan(
                    TestPlan plan, TestDescriptor parent, TestDescriptor child )
    {
        child.setParent( parent );

        TestIdentifier parentIdentifier = TestIdentifier.from( parent );
        TestIdentifier childIdentifier = TestIdentifier.from( child );

        plan.add( parentIdentifier );
        plan.add( childIdentifier );

        return childIdentifier;
    }

    private static TestIdentifier identifiersAsParentOnTestPlan( TestPlan plan, TestDescriptor root )
    {
        TestIdentifier rootIdentifier = TestIdentifier.from( root );
        plan.add( rootIdentifier );
        return rootIdentifier;
    }

    private static UniqueId newId()
    {
        return UniqueId.forEngine( "engine" );
    }

    private static final String MY_TEST_METHOD_NAME = "myTestMethod";

    private static class MyTestClass
    {
        @org.junit.jupiter.api.Test
        void myTestMethod()
        {
        }

        @org.junit.jupiter.api.Test
        void myTestMethod( String foo )
        {
        }

        @DisplayName( "name" )
        @org.junit.jupiter.api.Test
        void myNamedTestMethod()
        {
        }
    }
}
