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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RunListener for JUnit4, delegates to our own RunListener
 *
 */
public class JUnit4RunListener
    extends org.junit.runner.notification.RunListener
{
    private static final Pattern PARENS = Pattern.compile( "^" + ".+" //any character
                                                               + "\\(("
                                                               // then an open-paren (start matching a group)
                                                               + "[^\\\\(\\\\)]+" //non-parens
                                                               + ")\\)" + "$" );

    protected final RunListener reporter;

    /**
     * This flag is set after a failure has occurred so that a <code>testSucceeded</code> event is not fired.
     * This is necessary because JUnit4 always fires a <code>testRunFinished</code> event-- even if there was a failure.
     */
    private final ThreadLocal<Boolean> failureFlag = new InheritableThreadLocal<Boolean>();

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
    public void testIgnored( Description description )
        throws Exception
    {
        final String reason = JUnit4Reflector.getAnnotatedIgnoreValue( description );
        final SimpleReportEntry report =
            SimpleReportEntry.ignored( getClassName( description ), description.getDisplayName(), reason );
        reporter.testSkipped( report );
    }

    /**
     * Called when a specific test has started.
     *
     * @see org.junit.runner.notification.RunListener#testStarted(org.junit.runner.Description)
     */
    public void testStarted( Description description )
        throws Exception
    {
        reporter.testStarting( createReportEntry( description ) );
        failureFlag.remove();
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
        String testHeader = failure.getTestHeader();
        if ( isInsaneJunitNullString( testHeader ) )
        {
            testHeader = "Failure when constructing test";
        }
        ReportEntry report = SimpleReportEntry.withException( getClassName( failure.getDescription() ), testHeader,
                                                              createStackTraceWriter( failure ) );

        if ( failure.getException() instanceof AssertionError )
        {
            this.reporter.testFailed( report );
        }
        else
        {
            this.reporter.testError( report );
        }
        failureFlag.set( true );
    }

    protected StackTraceWriter createStackTraceWriter( Failure failure )
    {
        return new JUnit4StackTraceWriter( failure );
    }

    @SuppressWarnings( { "UnusedDeclaration" } )
    public void testAssumptionFailure( Failure failure )
    {
        this.reporter.testAssumptionFailure( createReportEntry( failure.getDescription() ) );
        failureFlag.set( true );
    }


    /**
     * Called after a specific test has finished.
     *
     * @see org.junit.runner.notification.RunListener#testFinished(org.junit.runner.Description)
     */
    public void testFinished( Description description )
        throws Exception
    {
        Boolean failure = failureFlag.get();
        if ( failure == null )
        {
            reporter.testSucceeded( createReportEntry( description ) );
        }
    }

    protected SimpleReportEntry createReportEntry( Description description )
    {
        return new SimpleReportEntry( getClassName( description ), description.getDisplayName() );
    }

    public String getClassName( Description description )
    {
        String name = extractClassName( description );
        if ( name == null || isInsaneJunitNullString( name ) )
        {
            // This can happen upon early failures (class instantiation error etc)
            Description subDescription = description.getChildren().get( 0 );
            if ( subDescription != null )
            {
                name = extractClassName( subDescription );
            }
            if ( name == null )
            {
                name = "Test Instantiation Error";
            }
        }
        return name;
    }

    private boolean isInsaneJunitNullString( String value )
    {
        return "null".equals( value );
    }

    public static String extractClassName( Description description )
    {
        String displayName = description.getDisplayName();
        Matcher m = PARENS.matcher( displayName );
        if ( !m.find() )
        {
            return displayName;
        }
        return m.group( 1 );
    }

    public static String extractMethodName( Description description )
    {
        String displayName = description.getDisplayName();
        int i = displayName.indexOf( "(" );
        if ( i >= 0 )
        {
            return displayName.substring( 0, i );
        }
        return displayName;
    }


    public static void rethrowAnyTestMechanismFailures( Result run )
        throws TestSetFailedException
    {
        if ( run.getFailureCount() > 0 )
        {
            for ( Failure failure : run.getFailures() )
            {
                Description description = failure.getDescription();
                if ( JUnit4ProviderUtil.isFailureInsideJUnitItself( description ) )
                {
                    final Throwable exception = failure.getException();
                    throw new TestSetFailedException( exception );
                }
            }
        }
    }
}
