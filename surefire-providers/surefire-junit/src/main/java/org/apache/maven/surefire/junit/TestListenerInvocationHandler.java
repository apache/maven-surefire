package org.apache.maven.surefire.junit;

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

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class TestListenerInvocationHandler
    implements InvocationHandler
{
    // The String names of the four methods in interface junit.framework.TestListener
    private static final String START_TEST = "startTest";

    private static final String ADD_FAILURE = "addFailure";

    private static final String ADD_ERROR = "addError";

    private static final String END_TEST = "endTest";

    private Set failedTestsSet = new HashSet();

    private ReporterManager reportManager;

    private static final Class[] EMPTY_CLASS_ARRAY = new Class[]{};

    private static final String[] EMPTY_STRING_ARRAY = new String[]{};

    private static class FailedTest
    {
        private Object testThatFailed;

        private Thread threadOnWhichTestFailed;

        FailedTest( Object testThatFailed, Thread threadOnWhichTestFailed )
        {
            if ( testThatFailed == null )
            {
                throw new NullPointerException( "testThatFailed is null" );
            }

            if ( threadOnWhichTestFailed == null )
            {
                throw new NullPointerException( "threadOnWhichTestFailed is null" );
            }

            this.testThatFailed = testThatFailed;

            this.threadOnWhichTestFailed = threadOnWhichTestFailed;
        }

        public boolean equals( Object obj )
        {
            boolean retVal = true;

            if ( obj == null || getClass() != obj.getClass() )
            {
                retVal = false;
            }
            else
            {
                FailedTest ft = (FailedTest) obj;

                if ( ft.testThatFailed != testThatFailed )
                {
                    retVal = false;
                }
                else if ( !ft.threadOnWhichTestFailed.equals( threadOnWhichTestFailed ) )
                {
                    retVal = false;
                }
            }

            return retVal;
        }

        public int hashCode()
        {
            return threadOnWhichTestFailed.hashCode();
        }
    }

    public TestListenerInvocationHandler( ReporterManager reportManager, Object instanceOfTestResult,
                                          ClassLoader loader )
    {
        if ( reportManager == null )
        {
            throw new NullPointerException( "reportManager is null" );
        }

        if ( instanceOfTestResult == null )
        {
            throw new NullPointerException( "instanceOfTestResult is null" );
        }

        if ( loader == null )
        {
            throw new NullPointerException( "loader is null" );
        }

        this.reportManager = reportManager;
    }

    public Object invoke( Object proxy, Method method, Object[] args )
        throws Throwable
    {
        String methodName = method.getName();

        if ( methodName.equals( START_TEST ) )
        {
            handleStartTest( args );
        }
        else if ( methodName.equals( ADD_ERROR ) )
        {
            handleAddError( args );
        }
        else if ( methodName.equals( ADD_FAILURE ) )
        {
            handleAddFailure( args );
        }
        else if ( methodName.equals( END_TEST ) )
        {
            handleEndTest( args );
        }

        return null;
    }

    // Handler for TestListener.startTest(Test)
    public void handleStartTest( Object[] args )
    {
        ReportEntry report = new ReportEntry( args[0], args[0].toString(), args[0].getClass().getName() );

        reportManager.testStarting( report );
    }

    // Handler for TestListener.addFailure(Test, Throwable)
    private void handleAddError( Object[] args )
        throws IllegalAccessException, InvocationTargetException
    {
        ReportEntry report =
            new ReportEntry( args[0], args[0].toString(), args[1].toString(), getStackTraceWriter( args ) );

        reportManager.testError( report );

        failedTestsSet.add( new FailedTest( args[0], Thread.currentThread() ) );
    }

    private JUnitStackTraceWriter getStackTraceWriter( Object[] args )
        throws IllegalAccessException, InvocationTargetException
    {
        String testName;

        try
        {
            Method m = args[0].getClass().getMethod( "getName", EMPTY_CLASS_ARRAY );
            testName = (String) m.invoke( args[0], EMPTY_STRING_ARRAY );
        }
        catch ( NoSuchMethodException e )
        {
            testName = "UNKNOWN";
        }

        return new JUnitStackTraceWriter( args[0].getClass().getName(), testName, (Throwable) args[1] );
    }

    private void handleAddFailure( Object[] args )
        throws IllegalAccessException, InvocationTargetException
    {
        ReportEntry report =
            new ReportEntry( args[0], args[0].toString(), args[1].toString(), getStackTraceWriter( args ) );

        reportManager.testFailed( report );

        failedTestsSet.add( new FailedTest( args[0], Thread.currentThread() ) );
    }

    private void handleEndTest( Object[] args )
    {
        boolean testHadFailed = failedTestsSet.remove( new FailedTest( args[0], Thread.currentThread() ) );

        if ( !testHadFailed )
        {
            ReportEntry report = new ReportEntry( args[0], args[0].toString(), args[0].getClass().getName() );

            reportManager.testSucceeded( report );
        }
    }
}
