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
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.surefire.api.report.LegacyPojoStackTraceWriter;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.RunListener;
import org.apache.maven.surefire.api.report.SimpleReportEntry;
import org.apache.maven.surefire.api.report.StackTraceWriter;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.report.SimpleReportEntry.withException;
import static org.apache.maven.surefire.api.util.internal.TestClassMethodNameUtils.extractClassName;
import static org.apache.maven.surefire.api.util.internal.TestClassMethodNameUtils.extractMethodName;

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

    private final JUnit3Reporter reporter;

    private static final Class<?>[] EMPTY_CLASS_ARRAY = { };

    private static final Object[] EMPTY_STRING_ARRAY = { };

    private static class FailedTest
    {
        private final Object testThatFailed;
        private final Thread threadOnWhichTestFailed;

        FailedTest( Object testThatFailed, Thread threadOnWhichTestFailed )
        {
            this.testThatFailed =
                requireNonNull( testThatFailed, "testThatFailed is null" );

            this.threadOnWhichTestFailed =
                requireNonNull( threadOnWhichTestFailed, "threadOnWhichTestFailed is null" );
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

    public TestListenerInvocationHandler( JUnit3Reporter reporter )
    {
        this.reporter = requireNonNull( reporter, "reporter is null" );
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
        ReportEntry report = createStartEndReportEntry( args );

        reporter.testStarting( report );
    }

    // Handler for TestListener.addFailure(Test, Throwable)
    private void handleAddError( Object[] args )
        throws ReflectiveOperationException
    {
        ReportEntry report = toReportEntryWithException( args );

        reporter.testError( report );

        failedTestsSet.add( new FailedTest( args[0], Thread.currentThread() ) );
    }

    private static LegacyPojoStackTraceWriter toStackTraceWriter( Object[] args )
        throws ReflectiveOperationException
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
        throws ReflectiveOperationException
    {
        ReportEntry report = toReportEntryWithException( args );

        reporter.testFailed( report );

        failedTestsSet.add( new FailedTest( args[0], Thread.currentThread() ) );
    }

    private void handleEndTest( Object[] args )
    {
        boolean testHadFailed = failedTestsSet.remove( new FailedTest( args[0], Thread.currentThread() ) );

        if ( !testHadFailed )
        {
            ReportEntry report = createStartEndReportEntry( args );

            reporter.testSucceeded( report );
        }
    }

    private ReportEntry toReportEntryWithException( Object[] args )
            throws ReflectiveOperationException
    {
        String description = args[0].toString();
        String className = extractClassName( description );
        String methodName = extractMethodName( description );
        StackTraceWriter stackTraceWriter = toStackTraceWriter( args );
        long testRunId = reporter.getClassMethodIndexer().indexClassMethod( className, methodName );
        return withException( NORMAL_RUN, testRunId, className, null, methodName, null, stackTraceWriter );
    }

    private SimpleReportEntry createStartEndReportEntry( Object[] args )
    {
        String description = args[0].toString();
        String className = extractClassName( description );
        String methodName = extractMethodName( description );
        long testRunId = reporter.getClassMethodIndexer().indexClassMethod( className, methodName );
        return new SimpleReportEntry( NORMAL_RUN, testRunId, className, null, methodName, null );
    }
}
