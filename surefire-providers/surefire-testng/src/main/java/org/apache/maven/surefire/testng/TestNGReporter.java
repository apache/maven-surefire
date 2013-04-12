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

import java.util.ResourceBundle;
import org.apache.maven.surefire.report.CategorizedReportEntry;
import org.apache.maven.surefire.report.PojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;

import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.TestNG;

/**
 * Listens for and provides and adaptor layer so that
 * TestNG tests can report their status to the current
 * {@link org.apache.maven.surefire.report.RunListener}.
 *
 * @author jkuhnert
 * @noinspection ThrowableResultOfMethodCallIgnored
 */
public class TestNGReporter
    implements ITestListener, ISuiteListener
{
    public static final String SUREFIRE_BUNDLE_NAME = "org.apache.maven.surefire.surefire";

    private final ResourceBundle bundle = ResourceBundle.getBundle( SUREFIRE_BUNDLE_NAME );

    /**
     * core Surefire reporting
     */
    private final RunListener reporter;

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
    public TestNGReporter( RunListener reportManager )
    {
        this.reporter = reportManager;

        if ( reportManager == null )
        {
            throw new IllegalArgumentException( "ReportManager passed in was null." );
        }

    }

    public void onTestStart( ITestResult result )
    {
        String rawString = bundle.getString( "testStarting" );
        String group = groupString( result.getMethod().getGroups(), result.getTestClass().getName() );
        ReportEntry report =
            new CategorizedReportEntry( getSource( result ), getUserFriendlyTestName( result ), group );
        reporter.testStarting( report );
    }

    private String getSource( ITestResult result )
    {
        return result.getTestClass().getName();
    }

    public void onTestSuccess( ITestResult result )
    {
        ReportEntry report = new SimpleReportEntry( getSource( result ), getUserFriendlyTestName( result ) );
        reporter.testSucceeded( report );
    }

    public void onTestFailure( ITestResult result )
    {
        ReportEntry report = SimpleReportEntry.withException( getSource( result ), getUserFriendlyTestName( result ),
                                                              new PojoStackTraceWriter(
                                                                  result.getTestClass().getRealClass().getName(),
                                                                  result.getMethod().getMethodName(),
                                                                  result.getThrowable() ) );

        reporter.testFailed( report );
    }

    private static String getUserFriendlyTestName( ITestResult result )
    {
        // This is consistent with the JUnit output
        return result.getName() + "(" + result.getTestClass().getName() + ")";
    }

    public void onTestSkipped( ITestResult result )
    {
        ReportEntry report = new SimpleReportEntry( getSource( result ), getUserFriendlyTestName( result ) );

        reporter.testSkipped( report );
    }

    public void onTestFailedButWithinSuccessPercentage( ITestResult result )
    {
        ReportEntry report = SimpleReportEntry.withException( getSource( result ), getUserFriendlyTestName( result ),
                                                              new PojoStackTraceWriter(
                                                                  result.getTestClass().getRealClass().getName(),
                                                                  result.getMethod().getMethodName(),
                                                                  result.getThrowable() ) );

        reporter.testError( report );
    }

    public void onStart( ITestContext context )
    {

    }

    public void onFinish( ITestContext context )
    {

    }


    public void onStart( ISuite suite )
    {

    }

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

}
