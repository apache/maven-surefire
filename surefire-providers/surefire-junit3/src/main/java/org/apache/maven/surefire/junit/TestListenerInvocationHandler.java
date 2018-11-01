package org.apache.maven.surefire.junit;

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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import org.apache.maven.surefire.report.LegacyPojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;

/**
 * Invocation Handler for TestListener proxies to delegate to our {@link RunListener}
 *
 */
public class TestListenerInvocationHandler
    implements InvocationHandler
{
    // The String names of the four methods in interface junit.framework.TestListener
    private static final String START_TEST = "startTest";

    private static final String ADD_FAILURE = "addFailure";

    private static final String ADD_ERROR = "addError";

    private static final String END_TEST = "endTest";

    private final Set<FailedTest> failedTestsSet = new HashSet<>();

    private RunListener reporter;

    private static final Class[] EMPTY_CLASS_ARRAY = { };

    private static final Object[] EMPTY_STRING_ARRAY = { };

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

        @Override
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

        @Override
        public int hashCode()
        {
            return threadOnWhichTestFailed.hashCode();
        }
    }

    public TestListenerInvocationHandler( RunListener reporter )
    {
        if ( reporter == null )
        {
            throw new NullPointerException( "reporter is null" );
        }

        this.reporter = reporter;
    }

    @Override
    public Object invoke( Object proxy, Method method, Object[] args )
        throws Throwable
    {
        String methodName = method.getName();

        switch ( methodName )
        {
            case START_TEST:
                handleStartTest( args );
                break;
            case ADD_ERROR:
                handleAddError( args );
                break;
            case ADD_FAILURE:
                handleAddFailure( args );
                break;
            case END_TEST:
                handleEndTest( args );
                break;
            default:
                break;
        }

        return null;
    }

    // Handler for TestListener.startTest(Test)
    private void handleStartTest( Object[] args )
    {
        ReportEntry report = new SimpleReportEntry( args[0].getClass().getName(), args[0].toString() );

        reporter.testStarting( report );
    }

    // Handler for TestListener.addFailure(Test, Throwable)
    private void handleAddError( Object[] args )
        throws IllegalAccessException, InvocationTargetException
    {
        ReportEntry report = SimpleReportEntry.withException( args[0].getClass().getName(), args[0].toString(),
                                                              getStackTraceWriter( args ) );

        reporter.testError( report );

        failedTestsSet.add( new FailedTest( args[0], Thread.currentThread() ) );
    }

    private LegacyPojoStackTraceWriter getStackTraceWriter( Object[] args )
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

        return new LegacyPojoStackTraceWriter( args[0].getClass().getName(), testName, (Throwable) args[1] );
    }

    private void handleAddFailure( Object[] args )
        throws IllegalAccessException, InvocationTargetException
    {
        ReportEntry report = SimpleReportEntry.withException( args[0].getClass().getName(), args[0].toString(),
                                                              getStackTraceWriter( args ) );

        reporter.testFailed( report );

        failedTestsSet.add( new FailedTest( args[0], Thread.currentThread() ) );
    }

    private void handleEndTest( Object[] args )
    {
        boolean testHadFailed = failedTestsSet.remove( new FailedTest( args[0], Thread.currentThread() ) );

        if ( !testHadFailed )
        {
            ReportEntry report = new SimpleReportEntry( args[0].getClass().getName(), args[0].toString() );

            reporter.testSucceeded( report );
        }
    }
}
