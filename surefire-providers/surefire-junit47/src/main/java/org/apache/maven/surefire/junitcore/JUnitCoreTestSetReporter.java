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
package org.apache.maven.surefire.junitcore;



import java.util.ResourceBundle;

import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterManager;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

class JUnitCoreTestSetReporter
    extends RunListener
{
    // Constants
    private static ResourceBundle bundle = ResourceBundle.getBundle( Surefire.SUREFIRE_BUNDLE_NAME );


    private ReporterManager reportMgr;

    /**
     * This flag is set after a failure has occurred so that a <code>testSucceeded</code> event is not fired.  This is necessary because JUnit4 always fires a <code>testRunFinished</code> event-- even if there was a failure.
     */
    private boolean failureFlag;

    /**
     * Constructor.
     *
     * @param reportManager the report manager to log testing events to
     */
    JUnitCoreTestSetReporter(ReporterManager reportManager)
    {
        this.reportMgr = reportManager;
    }

    /**
     * Called right before any tests from a specific class are run.
     *
     * @see org.junit.runner.notification.RunListener#testRunStarted(org.junit.runner.Description)
     */
    public void testRunStarted( Description description )
        throws Exception
    {
        String rawString = bundle.getString( "testSetStarting" );
        ReportEntry report = new ReportEntry( description.getClassName(), description.getDisplayName(), rawString );

        this.reportMgr.testSetStarting( report );
    }

    /**
     * Called right after all tests from a specific class are run.
     *
     * @see org.junit.runner.notification.RunListener#testRunFinished(org.junit.runner.Result)
     */
    public void testRunFinished( Result result )
        throws Exception
    {
        String rawString = bundle.getString( "testSetCompletedNormally" );
        ReportEntry report = new ReportEntry( result.getClass().getCanonicalName(), result.getClass().getName(), rawString );
        this.reportMgr.testSetCompleted( report );
        this.reportMgr.reset();
    }

    /**
     * Called when a specific test has been skipped (for whatever reason).
     *
     * @see org.junit.runner.notification.RunListener#testIgnored(org.junit.runner.Description)
     */
    public void testIgnored( Description description )
        throws Exception
    {
        String rawString = bundle.getString( "testSkipped" );
        ReportEntry report = new ReportEntry( description.getTestClass().getCanonicalName(), description.getDisplayName(), rawString );

        this.reportMgr.testSkipped( report );
    }

    /**
     * Called when a specific test has started.
     *
     * @see org.junit.runner.notification.RunListener#testStarted(org.junit.runner.Description)
     */
    public void testStarted( Description description )
        throws Exception
    {
        String rawString = bundle.getString( "testStarting" );
        ReportEntry report = new ReportEntry( description.getTestClass().getCanonicalName(), description.getDisplayName(), rawString );

        this.reportMgr.testStarting( report );

        this.failureFlag = false;
    }

    /**
     * Called when a specific test has failed.
     *
     * @see org.junit.runner.notification.RunListener#testFailure(org.junit.runner.notification.Failure)
     */
    public void testFailure( Failure failure )
        throws Exception
    {
        String rawString = bundle.getString( "executeException" );
        ReportEntry report =
            new ReportEntry( failure.getDescription().getTestClass().getCanonicalName(), failure.getTestHeader(), rawString, new JUnitCoreStackTraceWriter( failure ) );

        if ( failure.getException() instanceof AssertionError )
        {
            this.reportMgr.testFailed( report );
        }
        else
        {
            this.reportMgr.testError( report );
        }

        failureFlag = true;
    }

    /**
     * Called after a specific test has finished.
     *
     * @see org.junit.runner.notification.RunListener#testFinished(org.junit.runner.Description)
     */
    public void testFinished( Description description )
        throws Exception
    {
        if ( failureFlag == false )
        {
            String rawString = bundle.getString( "testSuccessful" );
            ReportEntry report = new ReportEntry( description.getTestClass().getCanonicalName(), description.getDisplayName(), rawString );

            this.reportMgr.testSucceeded( report );
        }
    }
}
