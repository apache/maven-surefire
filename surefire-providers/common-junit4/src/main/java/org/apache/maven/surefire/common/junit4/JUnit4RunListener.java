package org.apache.maven.surefire.common.junit4;

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
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.report.StackTraceWriter;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import static org.apache.maven.surefire.common.junit4.JUnit4ProviderUtil.isFailureInsideJUnitItself;
import static org.apache.maven.surefire.common.junit4.JUnit4Reflector.getAnnotatedIgnoreValue;
import static org.apache.maven.surefire.report.SimpleReportEntry.assumption;
import static org.apache.maven.surefire.report.SimpleReportEntry.ignored;
import static org.apache.maven.surefire.report.SimpleReportEntry.withException;
import static org.apache.maven.surefire.util.internal.TestClassMethodNameUtils.extractClassName;
import static org.apache.maven.surefire.util.internal.TestClassMethodNameUtils.extractMethodName;

/**
 * RunListener for JUnit4, delegates to our own RunListener
 *
 */
public class JUnit4RunListener
    extends org.junit.runner.notification.RunListener
{
    protected final RunListener reporter;

    /**
     * This flag is set after a failure has occurred so that a {@link RunListener#testSucceeded} event is not fired.
     * This is necessary because JUnit4 always fires a {@link org.junit.runner.notification.RunListener#testRunFinished}
     * event-- even if there was a failure.
     */
    private final ThreadLocal<Boolean> failureFlag = new InheritableThreadLocal<>();

    /**
     * Constructor.
     *
     * @param reporter the reporter to log testing events to
     */
    public JUnit4RunListener( RunListener reporter )
    {
        this.reporter = reporter;
    }

    // Testrun methods are not invoked when using the runner

    /**
     * Called when a specific test has been skipped (for whatever reason).
     *
     * @see org.junit.runner.notification.RunListener#testIgnored(org.junit.runner.Description)
     */
    @Override
    public void testIgnored( Description description )
        throws Exception
    {
        String reason = getAnnotatedIgnoreValue( description );
        reporter.testSkipped( ignored( getClassName( description ), description.getDisplayName(), reason ) );
    }

    /**
     * Called when a specific test has started.
     *
     * @see org.junit.runner.notification.RunListener#testStarted(org.junit.runner.Description)
     */
    @Override
    public void testStarted( Description description )
        throws Exception
    {
        try
        {
            reporter.testStarting( createReportEntry( description ) );
        }
        finally
        {
            failureFlag.remove();
        }
    }

    /**
     * Called when a specific test has failed.
     *
     * @see org.junit.runner.notification.RunListener#testFailure(org.junit.runner.notification.Failure)
     */
    @Override
    @SuppressWarnings( { "ThrowableResultOfMethodCallIgnored" } )
    public void testFailure( Failure failure )
        throws Exception
    {
        try
        {
            String testHeader = failure.getTestHeader();
            if ( isInsaneJunitNullString( testHeader ) )
            {
                testHeader = "Failure when constructing test";
            }

            String testClassName = getClassName( failure.getDescription() );
            StackTraceWriter stackTrace = createStackTraceWriter( failure );

            ReportEntry report = withException( testClassName, testHeader, stackTrace );

            if ( failure.getException() instanceof AssertionError )
            {
                reporter.testFailed( report );
            }
            else
            {
                reporter.testError( report );
            }
        }
        finally
        {
            failureFlag.set( true );
        }
    }

    @SuppressWarnings( "UnusedDeclaration" )
    public void testAssumptionFailure( Failure failure )
    {
        try
        {
            Description desc = failure.getDescription();
            String test = getClassName( desc );
            reporter.testAssumptionFailure( assumption( test, desc.getDisplayName(), failure.getMessage() ) );
        }
        finally
        {
            failureFlag.set( true );
        }
    }

    /**
     * Called after a specific test has finished.
     *
     * @see org.junit.runner.notification.RunListener#testFinished(org.junit.runner.Description)
     */
    @Override
    public void testFinished( Description description )
        throws Exception
    {
        Boolean failure = failureFlag.get();
        if ( failure == null )
        {
            reporter.testSucceeded( createReportEntry( description ) );
        }
    }

    /**
     * Delegates to {@link RunListener#testExecutionSkippedByUser()}.
     */
    public void testExecutionSkippedByUser()
    {
        reporter.testExecutionSkippedByUser();
    }

    private String getClassName( Description description )
    {
        String name = extractDescriptionClassName( description );
        if ( name == null || isInsaneJunitNullString( name ) )
        {
            // This can happen upon early failures (class instantiation error etc)
            Description subDescription = description.getChildren().get( 0 );
            if ( subDescription != null )
            {
                name = extractDescriptionClassName( subDescription );
            }
            if ( name == null )
            {
                name = "Test Instantiation Error";
            }
        }
        return name;
    }

    protected StackTraceWriter createStackTraceWriter( Failure failure )
    {
        return new JUnit4StackTraceWriter( failure );
    }

    protected SimpleReportEntry createReportEntry( Description description )
    {
        return new SimpleReportEntry( getClassName( description ), description.getDisplayName() );
    }

    protected String extractDescriptionClassName( Description description )
    {
        return extractClassName( description.getDisplayName() );
    }

    protected String extractDescriptionMethodName( Description description )
    {
        return extractMethodName( description.getDisplayName() );
    }

    public static void rethrowAnyTestMechanismFailures( Result run )
        throws TestSetFailedException
    {
        for ( Failure failure : run.getFailures() )
        {
            if ( isFailureInsideJUnitItself( failure.getDescription() ) )
            {
                throw new TestSetFailedException( failure.getTestHeader() + " :: " + failure.getMessage(),
                                                        failure.getException() );
            }
        }
    }

    private static boolean isInsaneJunitNullString( String value )
    {
        return "null".equals( value );
    }
}
