package org.apache.maven.surefire.testng;

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

import junit.framework.TestCase;
import org.apache.maven.surefire.api.report.CategorizedReportEntry;
import org.apache.maven.surefire.api.report.SimpleReportEntry;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.mockito.ArgumentCaptor;
import org.testng.ITestClass;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 * Tests for {@link TestNGReporter}.
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class TestNGReporterTest extends TestCase
{
    public void testParameterizedTestName() throws Exception
    {
        ITestNGMethod method = mock( ITestNGMethod.class );
        when( method.getCurrentInvocationCount() ).thenReturn( 3 );
        ITestResult testResult = mock( ITestResult.class );
        when( testResult.getName() ).thenReturn( "myTest" );
        when( testResult.getParameters() ).thenReturn( new String[] { "val1", "val2" } );
        when( testResult.getMethod() ).thenReturn( method );
        String testName = invokeMethod( TestNGReporter.class, "testName", testResult );
        assertThat( testName )
            .isEqualTo( "myTest[val1, val2](3)" );
    }

    public void testWithoutParameterizedTestName() throws Exception
    {
        ITestNGMethod method = mock( ITestNGMethod.class );
        ITestResult testResult = mock( ITestResult.class );
        when( testResult.getName() ).thenReturn( "myTest" );
        when( testResult.getMethod() ).thenReturn( method );
        String testName = invokeMethod( TestNGReporter.class, "testName", testResult );
        assertThat( testName )
            .isEqualTo( "myTest" );
    }

    public void testOnTestStart()
    {
        ITestClass cls = mock( ITestClass.class );
        when( cls.getName() ).thenReturn( "pkg.MyClass" );

        ITestNGMethod method = mock( ITestNGMethod.class );
        when( method.getCurrentInvocationCount() ).thenReturn( 3 );
        when( method.getGroups() ).thenReturn( new String[0] );

        ITestResult testResult = mock( ITestResult.class );
        when( testResult.getTestClass() ).thenReturn( cls );
        when( testResult.getMethod() ).thenReturn( method );
        when( testResult.getName() ).thenReturn( "myTest" );
        when( testResult.getParameters() ).thenReturn( new String[] { "val1", "val2" } );

        TestReportListener<TestOutputReportEntry> listener = mock( TestReportListener.class );
        TestNGReporter reporter = new TestNGReporter( listener );
        reporter.onTestStart( testResult );

        ArgumentCaptor<CategorizedReportEntry> reportEntry = ArgumentCaptor.forClass( CategorizedReportEntry.class );
        verify( listener ).testStarting( reportEntry.capture() );
        verifyNoMoreInteractions( listener );

        assertThat( reportEntry.getValue().getTestRunId() )
            .isEqualTo( 0x0000000100000001L );

        assertThat( reportEntry.getValue().getSourceName() )
            .isEqualTo( "pkg.MyClass" );

        assertThat( reportEntry.getValue().getName() )
            .isEqualTo( "myTest[val1, val2](3)" );
    }

    public void testOnTestSuccess()
    {
        ITestClass cls = mock( ITestClass.class );
        when( cls.getName() ).thenReturn( "pkg.MyClass" );

        ITestNGMethod method = mock( ITestNGMethod.class );
        when( method.getCurrentInvocationCount() ).thenReturn( 3 );

        ITestResult testResult = mock( ITestResult.class );
        when( testResult.getTestClass() ).thenReturn( cls );
        when( testResult.getMethod() ).thenReturn( method );
        when( testResult.getName() ).thenReturn( "myTest" );
        when( testResult.getParameters() ).thenReturn( new String[] { "val1", "val2" } );

        TestReportListener<TestOutputReportEntry> listener = mock( TestReportListener.class );
        TestNGReporter reporter = new TestNGReporter( listener );
        reporter.onTestSuccess( testResult );

        ArgumentCaptor<SimpleReportEntry> reportEntry = ArgumentCaptor.forClass( SimpleReportEntry.class );
        verify( listener ).testSucceeded( reportEntry.capture() );
        verifyNoMoreInteractions( listener );

        assertThat( reportEntry.getValue().getTestRunId() )
            .isEqualTo( 0x0000000100000001L );

        assertThat( reportEntry.getValue().getSourceName() )
            .isEqualTo( "pkg.MyClass" );

        assertThat( reportEntry.getValue().getName() )
            .isEqualTo( "myTest[val1, val2](3)" );
    }

    public void testOnTestFailure()
    {
        Exception stackTrace = new Exception();

        ITestClass cls = mock( ITestClass.class );
        when( cls.getName() ).thenReturn( getClass().getName() );

        ITestNGMethod method = mock( ITestNGMethod.class );
        when( method.getCurrentInvocationCount() ).thenReturn( 1 );
        when( method.getMethodName() ).thenReturn( "myTest" );

        ITestResult testResult = mock( ITestResult.class );
        when( testResult.getThrowable() ).thenReturn( stackTrace );
        when( cls.getRealClass() ).thenReturn( getClass() );
        when( testResult.getTestClass() ).thenReturn( cls );
        when( testResult.getMethod() ).thenReturn( method );
        when( testResult.getName() ).thenReturn( "myTest" );
        when( testResult.getParameters() ).thenReturn( new String[] { "val1", "val2" } );

        TestReportListener<TestOutputReportEntry> listener = mock( TestReportListener.class );
        TestNGReporter reporter = new TestNGReporter( listener );
        reporter.onTestFailure( testResult );

        ArgumentCaptor<SimpleReportEntry> reportEntry = ArgumentCaptor.forClass( SimpleReportEntry.class );
        verify( listener ).testFailed( reportEntry.capture() );
        verifyNoMoreInteractions( listener );

        assertThat( reportEntry.getValue().getTestRunId() )
            .isEqualTo( 0x0000000100000001L );

        assertThat( reportEntry.getValue().getSourceName() )
            .isEqualTo( getClass().getName() );

        assertThat( reportEntry.getValue().getName() )
            .isEqualTo( "myTest[val1, val2](1)" );

        assertThat( reportEntry.getValue().getStackTraceWriter() )
            .isNotNull();

        assertThat( reportEntry.getValue().getStackTraceWriter().getThrowable().getTarget() )
            .isSameAs( stackTrace );
    }

    public void testOnSkippedTest()
    {
        Exception stackTrace = new Exception( "test skip reason" );

        ITestClass cls = mock( ITestClass.class );
        when( cls.getName() ).thenReturn( getClass().getName() );

        ITestNGMethod method = mock( ITestNGMethod.class );
        when( method.getCurrentInvocationCount() ).thenReturn( 1 );

        ITestResult testResult = mock( ITestResult.class );
        when( testResult.getThrowable() ).thenReturn( stackTrace );
        when( testResult.getTestClass() ).thenReturn( cls );
        when( testResult.getMethod() ).thenReturn( method );
        when( testResult.getName() ).thenReturn( "myTest" );
        when( testResult.getParameters() ).thenReturn( new String[] { "val1", "val2" } );

        TestReportListener<TestOutputReportEntry> listener = mock( TestReportListener.class );
        TestNGReporter reporter = new TestNGReporter( listener );
        reporter.onTestSkipped( testResult );

        ArgumentCaptor<SimpleReportEntry> reportEntry = ArgumentCaptor.forClass( SimpleReportEntry.class );
        verify( listener ).testSkipped( reportEntry.capture() );
        verifyNoMoreInteractions( listener );

        assertThat( reportEntry.getValue().getTestRunId() )
            .isEqualTo( 0x0000000100000001L );

        assertThat( reportEntry.getValue().getSourceName() )
            .isEqualTo( getClass().getName() );

        assertThat( reportEntry.getValue().getName() )
            .isEqualTo( "myTest[val1, val2](1)" );

        assertThat( reportEntry.getValue().getMessage() )
            .isEqualTo( stackTrace.getMessage() );
    }

    public void testOnTestFailedButWithinSuccessPercentage()
    {
        Exception stackTrace = new Exception();

        ITestClass cls = mock( ITestClass.class );
        when( cls.getName() ).thenReturn( getClass().getName() );

        ITestNGMethod method = mock( ITestNGMethod.class );
        when( method.getCurrentInvocationCount() ).thenReturn( 1 );
        when( method.getMethodName() ).thenReturn( "myTest" );

        ITestResult testResult = mock( ITestResult.class );
        when( testResult.getThrowable() ).thenReturn( stackTrace );
        when( cls.getRealClass() ).thenReturn( getClass() );
        when( testResult.getTestClass() ).thenReturn( cls );
        when( testResult.getMethod() ).thenReturn( method );
        when( testResult.getName() ).thenReturn( "myTest" );
        when( testResult.getParameters() ).thenReturn( new String[] { "val1", "val2" } );

        TestReportListener<TestOutputReportEntry> listener = mock( TestReportListener.class );
        TestNGReporter reporter = new TestNGReporter( listener );
        reporter.onTestFailedButWithinSuccessPercentage( testResult );

        ArgumentCaptor<SimpleReportEntry> reportEntry = ArgumentCaptor.forClass( SimpleReportEntry.class );
        verify( listener ).testSucceeded( reportEntry.capture() );
        verifyNoMoreInteractions( listener );

        assertThat( reportEntry.getValue().getTestRunId() )
            .isEqualTo( 0x0000000100000001L );

        assertThat( reportEntry.getValue().getSourceName() )
            .isEqualTo( getClass().getName() );

        assertThat( reportEntry.getValue().getName() )
            .isEqualTo( "myTest[val1, val2](1)" );

        assertThat( reportEntry.getValue().getStackTraceWriter() )
            .isNotNull();

        assertThat( reportEntry.getValue().getStackTraceWriter().getThrowable().getTarget() )
            .isSameAs( stackTrace );
    }
}
