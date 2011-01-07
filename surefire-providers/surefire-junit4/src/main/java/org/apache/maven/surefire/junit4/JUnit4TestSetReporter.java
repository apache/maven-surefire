package org.apache.maven.surefire.junit4;

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

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JUnit4TestSetReporter
    extends RunListener
{
    // Member Variables
    private Class testSet;

    private ReporterManager reportMgr;

    /**
     * This flag is set after a failure has occurred so that a <code>testSucceeded</code> event is not fired.
     * This is necessary because JUnit4 always fires a <code>testRunFinished</code> event-- even if there was a failure.
     */
    private boolean failureFlag;

    /**
     * Constructor.
     *
     * @param testClass     the specific test set that this will report on as it is
     *                      executed
     * @param reportManager the report manager to log testing events to
     */
    JUnit4TestSetReporter( Class testClass, ReporterManager reportManager )
    {
        this.testSet = testClass;
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
        ReportEntry report = new SimpleReportEntry( testSet.getName(), testSet.getName() );

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
        ReportEntry report = new SimpleReportEntry( testSet.getName(), testSet.getName() );

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
        ReportEntry report = new SimpleReportEntry( extractClassName( description ), description.getDisplayName() );

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
        ReportEntry report = new SimpleReportEntry( extractClassName( description ), description.getDisplayName() );

        this.reportMgr.testStarting( report );

        this.failureFlag = false;
    }

    /**
     * Called when a specific test has failed.
     *
     * @see org.junit.runner.notification.RunListener#testFailure(org.junit.runner.notification.Failure)
     */
    @SuppressWarnings( { "ThrowableResultOfMethodCallIgnored" } )
    public void testFailure( Failure failure )
        throws Exception
    {
        ReportEntry report =
            new SimpleReportEntry( extractClassName( failure.getDescription() ), failure.getTestHeader(),
                                   new JUnit4StackTraceWriter( failure ) );

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
        if ( !failureFlag )
        {
            ReportEntry report = new SimpleReportEntry( extractClassName( description ), description.getDisplayName() );

            this.reportMgr.testSucceeded( report );
        }
    }

    private String extractClassName( Description description )
    {
        String displayName = description.getDisplayName();
        final Pattern PARENS = Pattern.compile( "^" + "[^\\(\\)]+" //non-parens
                                                    + "\\((" // then an open-paren (start matching a group)
                                                    + "[^\\\\(\\\\)]+" //non-parens
                                                    + ")\\)" + "$" ); // then a close-paren (end group match)
        Matcher m = PARENS.matcher( displayName );
        if ( !m.find() )
        {
            return displayName;
        }
        return m.group( 1 );
    }

    public void setTestSet( Class testSet )
    {
        this.testSet = testSet;
    }

    public void setReportMgr( ReporterManager reportMgr )
    {
        this.reportMgr = reportMgr;
    }
}
