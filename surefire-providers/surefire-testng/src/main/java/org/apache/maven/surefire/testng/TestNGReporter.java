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

import org.apache.maven.surefire.api.report.CategorizedReportEntry;
import org.apache.maven.surefire.api.report.OutputReportEntry;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.TestOutputReceiver;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.report.ClassMethodIndexer;
import org.apache.maven.surefire.report.PojoStackTraceWriter;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.RunListener;
import org.apache.maven.surefire.api.report.SimpleReportEntry;

import org.apache.maven.surefire.report.RunModeSetter;
import org.testng.IClass;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;

import static org.apache.maven.surefire.api.report.SimpleReportEntry.ignored;
import static org.apache.maven.surefire.api.report.SimpleReportEntry.withException;

/**
 * Listens for and provides and adaptor layer so that
 * TestNG tests can report their status to the current
 * {@link RunListener}.
 *
 * @author jkuhnert
 */
public class TestNGReporter
    implements TestOutputReceiver<OutputReportEntry>, ITestListener, ISuiteListener, RunModeSetter
{
    private final ClassMethodIndexer classMethodIndexer = new ClassMethodIndexer();
    private final TestReportListener<TestOutputReportEntry> reporter;
    private volatile RunMode runMode;

    /**
     * Constructs a new instance that will listen to
     * test updates from a {@link org.testng.TestNG} class instance.
     * <br>
     * <br>It is assumed that the requisite {@link org.testng.TestNG#addListener(ITestListener)}
     * method call has already associated with this instance <i>before</i> the test
     * suite is run.
     *
     * @param reportManager Instance to report suite status to
     */
    public TestNGReporter( TestReportListener<TestOutputReportEntry> reportManager )
    {
        this.reporter = reportManager;
    }

    protected final RunListener getRunListener()
    {
        return reporter;
    }

    @Override
    public void onTestStart( ITestResult result )
    {
        String className = result.getTestClass().getName();
        String methodName = testName( result );
        long testRunId = classMethodIndexer.indexClassMethod( className, methodName );
        String group = groupString( result.getMethod().getGroups(), className );
        reporter.testStarting( new CategorizedReportEntry( runMode, testRunId, className, methodName, group ) );
    }

    @Override
    public void onTestSuccess( ITestResult result )
    {
        String className = result.getTestClass().getName();
        String methodName = testName( result );
        long testRunId = classMethodIndexer.indexClassMethod( className, methodName );
        ReportEntry report = new SimpleReportEntry( runMode, testRunId, className, null, methodName, null );
        reporter.testSucceeded( report );
    }

    @Override
    public void onTestFailure( ITestResult result )
    {
        IClass clazz = result.getTestClass();
        String className = clazz.getName();
        String methodName = testName( result );
        long testRunId = classMethodIndexer.indexClassMethod( className, methodName );
        StackTraceWriter stackTraceWriter = new PojoStackTraceWriter( clazz.getRealClass().getName(),
            result.getMethod().getMethodName(), result.getThrowable() );
        ReportEntry report = withException( runMode, testRunId, clazz.getName(), null, methodName,
            null, stackTraceWriter );

        reporter.testFailed( report );
    }

    @Override
    public void onTestSkipped( ITestResult result )
    {
        String className = result.getTestClass().getName();
        String methodName = testName( result );
        long testRunId = classMethodIndexer.indexClassMethod( className, methodName );
        //noinspection ThrowableResultOfMethodCallIgnored
        Throwable t = result.getThrowable();
        String reason = t == null ? null : t.getMessage();
        ReportEntry report = ignored( runMode, testRunId, className, null, methodName, null, reason );
        reporter.testSkipped( report );
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage( ITestResult result )
    {
        IClass clazz = result.getTestClass();
        String className = clazz.getName();
        String methodName = testName( result );
        long testRunId = classMethodIndexer.indexClassMethod( className, methodName );
        StackTraceWriter stackTraceWriter = new PojoStackTraceWriter( clazz.getRealClass().getName(),
            result.getMethod().getMethodName(), result.getThrowable() );
        ReportEntry report = withException( runMode, testRunId, className, null, methodName, null, stackTraceWriter );
        reporter.testSucceeded( report );
    }

    @Override
    public void onStart( ITestContext context )
    {

    }

    @Override
    public void onFinish( ITestContext context )
    {

    }


    @Override
    public void onStart( ISuite suite )
    {

    }

    @Override
    public void onFinish( ISuite suite )
    {

    }

    /**
     * Creates a string out of the list of testng groups in the
     * form of <pre>"group1,group2,group3"</pre>.
     *
     * @param groups       The groups being run
     * @param defaultValue The default to use if no groups
     * @return a string describing the groups
     */
    private static String groupString( String[] groups, String defaultValue )
    {
        String retVal;
        if ( groups != null && groups.length > 0 )
        {
            StringBuilder str = new StringBuilder();
            for ( int i = 0; i < groups.length; i++ )
            {
                str.append( groups[i] );
                if ( i + 1 < groups.length )
                {
                    str.append( "," );
                }
            }
            retVal = str.toString();
        }
        else
        {
            retVal = defaultValue;
        }
        return retVal;
    }

    public void onConfigurationFailure( ITestResult result )
    {
        onTestFailure( result );
    }

    public void onConfigurationSkip( ITestResult result )
    {
        onTestSkipped( result );
    }

    public void onConfigurationSuccess( ITestResult result )
    {
        // DGF Don't record configuration successes as separate tests
        //onTestSuccess( result );
    }

    /**
     * Acquire a better representation of the test name that includes parameters and the invocation count, if there are
     * any parameters
     *
     * @param result the test result to extract from
     * @return a descriptive name for the test
     */
    private static String testName( ITestResult result )
    {
        Object[] parameters = result.getParameters();
        String name = result.getName();
        return parameters == null || parameters.length == 0
            ? name : name + Arrays.toString( parameters ) + "(" + result.getMethod().getCurrentInvocationCount() + ")";
    }

    @Override
    public void setRunMode( RunMode runMode )
    {
        this.runMode = runMode;
    }

    @Override
    public void writeTestOutput( OutputReportEntry reportEntry )
    {
        Long testRunId = classMethodIndexer.getLocalIndex();
        reporter.writeTestOutput( new TestOutputReportEntry( reportEntry, runMode, testRunId ) );
    }
}
