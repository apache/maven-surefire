package org.apache.maven.surefire.testng;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterManager;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.TestNG;

/**
 * Listens for and provides and adaptor layer so that
 * TestNG tests can report their status to the current
 * {@link org.apache.maven.surefire.report.ReporterManager}.
 *
 * @author jkuhnert
 */
public class TestNGReporter
    implements ITestListener, ISuiteListener
{

    /**
     * core Surefire reporting
     */
    protected ReporterManager reportManager;

    /**
     * core Surefire instance
     */
    protected Surefire surefire;

    /**
     * Constructs a new instance that will listen to
     * test updates from a {@link TestNG} class instance.
     * <p/>
     * <p/>It is assumed that the requisite {@link TestNG#addListener(ITestListener)}
     * method call has already associated with this instance <i>before</i> the test
     * suite is run.
     *
     * @param reportManager Instance to report suite status to
     * @param surefire      Main instance that provides resources messages,etc.
     */
    public TestNGReporter( ReporterManager reportManager, Surefire surefire )
    {
        this.reportManager = reportManager;
        this.surefire = surefire;

        if ( reportManager == null )
        {
            throw new IllegalArgumentException( "ReportManager passed in was null." );
        }
        if ( surefire == null )
        {
            throw new IllegalArgumentException( "Surefire passed in was null." );
        }
    }

    /* (non-Javadoc)
    * @see org.testng.ITestListener#onTestStart(org.testng.ITestResult)
    */
    public void onTestStart( ITestResult result )
    {
        String rawString = Surefire.getResourceString( "testStarting" );
        String group = groupString( result.getMethod().getGroups(), result.getTestClass().getName() );
        ReportEntry report = new ReportEntry( surefire, result.getTestClass().getName() + "#" +
            result.getMethod().getMethodName(), group, rawString );

        reportManager.testStarting( report );


    }

    /* (non-Javadoc)
     * @see org.testng.ITestListener#onTestSuccess(org.testng.ITestResult)
     */
    public void onTestSuccess( ITestResult result )
    {
        reportManager.testSucceeded( createReport( result, "testSuccessful" ) );
    }

    /* (non-Javadoc)
     * @see org.testng.ITestListener#onTestFailure(org.testng.ITestResult)
     */
    public void onTestFailure( ITestResult result )
    {
        String rawString = Surefire.getResourceString( "executeException" );

        // TODO: almost certainly not correct: result.getMethod().getExtraOutput().getParameterOutput()
        ReportEntry report = new ReportEntry( surefire, result.getName(),
                                              rawString + result.getMethod().getExtraOutput().getParameterOutput(),
                                              result.getThrowable() );

        reportManager.testFailed( report );
    }

    /* (non-Javadoc)
    * @see org.testng.ITestListener#onTestSkipped(org.testng.ITestResult)
    */
    public void onTestSkipped( ITestResult result )
    {
        // TODO: is this correct?
        reportManager.testSucceeded( createReport( result, "testSkipped" ) );
    }

    private ReportEntry createReport( ITestResult result, String key )
    {
        return new ReportEntry( surefire, result.getName(), Surefire.getResourceString( key ) );
    }

    /* (non-Javadoc)
     * @see org.testng.ITestListener#onTestFailedButWithinSuccessPercentage(org.testng.ITestResult)
     */
    public void onTestFailedButWithinSuccessPercentage( ITestResult result )
    {
        String rawString = Surefire.getResourceString( "executeException" );

        // TODO: almost certainly not correct: result.getMethod().getExtraOutput().getParameterOutput()
        ReportEntry report = new ReportEntry( surefire, result.getName(),
                                              rawString + result.getMethod().getExtraOutput(), result.getThrowable() );

        reportManager.testError( report );
    }

    /* (non-Javadoc)
     * @see org.testng.ITestListener#onStart(org.testng.ITestContext)
     */
    public void onStart( ITestContext context )
    {
        String rawString = Surefire.getResourceString( "suiteExecutionStarting" );

        String group = groupString( context.getIncludedGroups(), context.getName() );

        ReportEntry report = new ReportEntry( surefire, context.getName(), group, rawString );

        reportManager.batteryStarting( report );
    }

    /* (non-Javadoc)
    * @see org.testng.ITestListener#onFinish(org.testng.ITestContext)
    */
    public void onFinish( ITestContext context )
    {
        String rawString = Surefire.getResourceString( "suiteCompletedNormally" );

        ReportEntry report =
            new ReportEntry( surefire, context.getName(), groupString( context.getIncludedGroups(), null ), rawString );

        reportManager.batteryCompleted( report );

        reportManager.runCompleted();

        reportManager.dispose();
    }

    public void onFinish( ISuite suite )
    {
    }

    public void onStart( ISuite suite )
    {
    }

    /**
     * Creates a string out of the list of testng groups in the
     * form of <pre>"group1,group2,group3"</pre>.
     *
     * @param groups
     * @param defaultValue
     */
    private static String groupString( String[] groups, String defaultValue )
    {
        String retVal;
        if ( groups != null && groups.length > 0 )
        {
            StringBuffer str = new StringBuffer();
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

}
