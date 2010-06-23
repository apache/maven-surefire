package org.apache.maven.surefire.junitcore;
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

import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterManager;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Represents the test-state of a single test method that is run.
 * <p/>
 * Notes about thread safety: This instance is serially confined to 1-3 threads (construction, test-run, reporting),
 * without any actual parallel access
 */
class TestMethod
{
    private final Description description;

    private volatile Failure testFailure;

    private volatile Failure testAssumptionFailure;

    private volatile Description ignored;

    private static final ResourceBundle bundle = ResourceBundle.getBundle( Surefire.SUREFIRE_BUNDLE_NAME );

    private static final InheritableThreadLocal<TestMethod> testMethod = new InheritableThreadLocal<TestMethod>();

    private volatile LogicalStream output;

    public TestMethod( Description description )
    {
        this.description = description;
    }


    public void testFinished()
        throws Exception
    {
    }


    public void testIgnored( Description description )
        throws Exception
    {
        ignored = description;

    }

    public void testFailure( Failure failure )
        throws Exception
    {
        this.testFailure = failure;
    }


    public void testAssumptionFailure( Failure failure )
    {
        this.testAssumptionFailure = failure;
    }

    public void replay( ReporterManager reporter )
        throws Exception
    {

        if ( ignored != null )
        {
            reporter.testSkipped( createReportEntry( "testSkipped" ) );
            return;
        }

        reporter.testStarting( createReportEntry( "testStarting" ) );
        if ( output != null )
        {
            // For some reason, console output is not written to the txt file.
            output.writeToConsole( reporter );
        }
        if ( testFailure != null )
        {
            ReportEntry report = createFailureEntry( testFailure, "executeException" );
            //noinspection ThrowableResultOfMethodCallIgnored
            if ( testFailure.getException() instanceof AssertionError )
            {
                reporter.testFailed( report, getStdout(), getStdErr() );
            }
            else
            {
                reporter.testError( report, getStdout(), getStdErr() );
            }

        }
        else if ( testAssumptionFailure != null )
        {
            // Does nothing...
        }
        else
        {
            reporter.testSucceeded( createReportEntry( "testSuccessful" ) );
        }
    }

    private ReportEntry createReportEntry( String rawString2 )
    {
        String rawString = bundle.getString( rawString2 );
        return new ReportEntry( description.getTestClass().getCanonicalName(), description.getDisplayName(),
                                rawString );
    }

    private ReportEntry createFailureEntry( Failure failure, String rawString2 )
    {
        String rawString = bundle.getString( rawString2 );
        return new ReportEntry( failure.getDescription().getTestClass().getCanonicalName(), failure.getTestHeader(),
                                rawString, new JUnitCoreStackTraceWriter( failure ) );
    }


    public void attachToThread()
    {
        testMethod.set( this );
    }

    public static void detachFromCurrentThread()
    {
        testMethod.remove();
    }

    public static TestMethod getThreadTestMethod()
    {
        return testMethod.get();
    }

    public LogicalStream getLogicalStream()
    {
        if ( output == null )
        {
            output = new LogicalStream();
        }
        return output;
    }


    private String getStdout()
    {
        return output != null ? output.getOutput( true ) : "";
    }

    private String getStdErr()
    {
        return output != null ? output.getOutput( false ) : "";
    }

    public static void fillTestCountMap( Description description, Map<Class, TestSet> methodCount )
    {
        final ArrayList<Description> children = description.getChildren();

        TestSet testSet = new TestSet( description );
        Class<?> itemTestClass = null;
        for ( Description item : children )
        {
            if ( item.isTest() )
            {
                testSet.incrementTestMethodCount();
                if ( itemTestClass == null )
                {
                    itemTestClass = item.getTestClass();
                }
            }
            else if ( item.getChildren().size() > 0 )
            {
                fillTestCountMap( item, methodCount );
            }
        }
        if ( itemTestClass != null )
        {
            methodCount.put( itemTestClass, testSet );
        }
    }

}
