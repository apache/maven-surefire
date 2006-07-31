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
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.suite.SurefireTestSuite;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.TestNG;

import java.util.ResourceBundle;

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
    private ResourceBundle bundle = ResourceBundle.getBundle( Surefire.SUREFIRE_BUNDLE_NAME );

    /**
     * core Surefire reporting
     */
    protected ReporterManager reportManager;

    private Object source;

    /**
     * Constructs a new instance that will listen to
     * test updates from a {@link TestNG} class instance.
     * <p/>
     * <p/>It is assumed that the requisite {@link TestNG#addListener(ITestListener)}
     * method call has already associated with this instance <i>before</i> the test
     * suite is run.
     *
     * @param reportManager Instance to report suite status to
     */
    public TestNGReporter( ReporterManager reportManager, SurefireTestSuite source )
    {
        this.reportManager = reportManager;

        if ( reportManager == null )
        {
            throw new IllegalArgumentException( "ReportManager passed in was null." );
        }

        this.source = source;
    }

    public void onTestStart( ITestResult result )
    {
        String rawString = bundle.getString( "testStarting" );
        String group = groupString( result.getMethod().getGroups(), result.getTestClass().getName() );
        ReportEntry report = new ReportEntry( source, getUserFriendlyTestName( result ), group, rawString );

        reportManager.testStarting( report );
    }

    public void onTestSuccess( ITestResult result )
    {
        ReportEntry report =
            new ReportEntry( source, getUserFriendlyTestName( result ), bundle.getString( "testSuccessful" ) );
        reportManager.testSucceeded( report );
    }

    public void onTestFailure( ITestResult result )
    {
        String rawString = bundle.getString( "executeException" );

        ReportEntry report = new ReportEntry( source, getUserFriendlyTestName( result ), rawString,
                                              new TestNGStackTraceWriter( result ) );

        reportManager.testFailed( report );
    }

    private static String getUserFriendlyTestName( ITestResult result )
    {
        // This is consistent with the JUnit output
        return result.getName() + "(" + result.getTestClass().getName() + ")";
    }

    public void onTestSkipped( ITestResult result )
    {
        ReportEntry report =
            new ReportEntry( source, getUserFriendlyTestName( result ), bundle.getString( "testSkipped" ) );

        reportManager.testSkipped( report );
    }

    public void onTestFailedButWithinSuccessPercentage( ITestResult result )
    {
        String rawString = bundle.getString( "executeException" );

        ReportEntry report = new ReportEntry( source, getUserFriendlyTestName( result ), rawString,
                                              new TestNGStackTraceWriter( result ) );

        reportManager.testError( report );
    }

    public void onStart( ITestContext context )
    {
        String rawString = bundle.getString( "testSetStarting" );

        String group = groupString( context.getIncludedGroups(), context.getName() );

        ReportEntry report = new ReportEntry( source, context.getName(), group, rawString );

        try
        {
            reportManager.testSetStarting( report );
        }
        catch ( ReporterException e )
        {
            // TODO: remove this exception from the report manager
        }
    }

    public void onFinish( ITestContext context )
    {
        String rawString = bundle.getString( "testSetCompletedNormally" );

        ReportEntry report =
            new ReportEntry( source, context.getName(), groupString( context.getIncludedGroups(), null ), rawString );

        reportManager.testSetCompleted( report );

        reportManager.reset();
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
