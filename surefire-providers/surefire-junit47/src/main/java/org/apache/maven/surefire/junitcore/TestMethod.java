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
import org.apache.maven.surefire.report.Reporter;
import org.apache.maven.surefire.report.SimpleReportEntry;
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

    private final long startTime;

    private long endTime;

    private volatile Failure testFailure;

    private volatile Failure testAssumptionFailure;

    private volatile Description ignored;

    private static final InheritableThreadLocal<TestMethod> TEST_METHOD = new InheritableThreadLocal<TestMethod>();

    private volatile LogicalStream output;

    public TestMethod( Description description )
    {
        this.description = description;
        startTime = System.currentTimeMillis();
    }


    public void testFinished()
        throws Exception
    {
        setEndTime();
    }


    public void testIgnored( Description description )
        throws Exception
    {
        ignored = description;
        setEndTime();
    }

    public void testFailure( Failure failure )
        throws Exception
    {
        this.testFailure = failure;
        setEndTime();
    }


    public void testAssumptionFailure( Failure failure )
    {
        this.testAssumptionFailure = failure;
        setEndTime();
    }

    private void setEndTime()
    {
        this.endTime = System.currentTimeMillis();
    }

    public int getElapsed()
    {
        return (int) ( endTime - startTime );
    }


    public void replay( Reporter reporter )
        throws Exception
    {

        if ( ignored != null )
        {
            reporter.testSkipped( createReportEntry() );
            return;
        }

        reporter.testStarting( createReportEntry() );
        if ( output != null )
        {
            output.writeDetails( reporter );
        }
        if ( testFailure != null )
        {
            ReportEntry report = createFailureEntry( testFailure );
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
            reporter.testSucceeded( createReportEntry() );
        }
    }

    private ReportEntry createReportEntry()
    {
        return new SimpleReportEntry( description.getTestClass().getCanonicalName(), description.getDisplayName(),
                                      getElapsed() );
    }

    private ReportEntry createFailureEntry( Failure failure )
    {
        return new SimpleReportEntry( failure.getDescription().getTestClass().getCanonicalName(),
                                      failure.getTestHeader(), new JUnitCoreStackTraceWriter( failure ), getElapsed() );
    }


    public void attachToThread()
    {
        TEST_METHOD.set( this );
    }

    public static void detachFromCurrentThread()
    {
        TEST_METHOD.remove();
    }

    public static TestMethod getThreadTestMethod()
    {
        return TEST_METHOD.get();
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
