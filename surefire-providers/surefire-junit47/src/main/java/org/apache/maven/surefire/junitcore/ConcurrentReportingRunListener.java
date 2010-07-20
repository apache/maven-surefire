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

import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kristian Rosenvold
 */
public abstract class ConcurrentReportingRunListener
    extends RunListener
{
    private final PrintStream orgSystemOut = System.out;

    private final PrintStream orgSystemErr = System.err;

    protected Map<Class, TestSet> classMethodCounts = new ConcurrentHashMap<Class, TestSet>();

    protected final ReporterManager reporterManager;

    protected final boolean reportImmediately;

    private final ConcurrentPrintStream out = new ConcurrentPrintStream( true );
    private final ConcurrentPrintStream err = new ConcurrentPrintStream( false );

    public ConcurrentReportingRunListener( ReporterManagerFactory reporterFactory, boolean reportImmediately )
        throws TestSetFailedException
    {
        this.reportImmediately = reportImmediately;
        reporterManager = reporterFactory.createReporterManager();
        // Important: We must capture System.out/System.err AFTER the  reportManager captures stdout/stderr
        // because we know how to demultiplex correctly. The redirection in reporterManager is basically
        // ignored/unused because we use ConcurrentPrintStream.
        System.setOut( out );
        System.setErr( err );
    }

    @Override
    public void testRunStarted( Description description )
        throws Exception
    {
        TestMethod.fillTestCountMap( description, classMethodCounts );
    }

    @Override
    public void testRunFinished( Result result )
        throws Exception
    {
        for ( TestSet testSet : classMethodCounts.values() )
        {
            testSet.replay( reporterManager );
        }
        System.setOut( orgSystemOut );
        System.setErr( orgSystemErr );

        out.writeTo(  orgSystemOut );
        err.writeTo(  orgSystemErr );
    }

    protected TestMethod getTestMethod()
    {
        return TestMethod.getThreadTestMethod();
    }

    protected void detachTestMethodFromThread()
    {
        TestMethod.detachFromCurrentThread();
    }

    protected TestSet getTestSet( Description description )
    {
        return classMethodCounts.get( description.getTestClass() );
    }

    @Override
    public void testFailure( Failure failure )
        throws Exception
    {
        getOrCreateTestMethod(failure.getDescription()).testFailure( failure );
    }

    private TestMethod getOrCreateTestMethod( Description description )
    {
        TestMethod threadTestMethod = TestMethod.getThreadTestMethod();
        if (threadTestMethod != null){
            return threadTestMethod;
        }
        TestSet testSet = getTestSet( description );
        return testSet.createTestMethod( description );
    }

    @Override
    public void testAssumptionFailure( Failure failure )
    {
        TestMethod.getThreadTestMethod().testAssumptionFailure( failure );
    }

    @Override
    public void testIgnored( Description description )
        throws Exception
    {
        TestSet testSet = getTestSet( description );
        TestMethod testMethod = getTestSet( description ).createTestMethod( description );
        testMethod.testIgnored( description );
        testSet.incrementFinishedTests( reporterManager, reportImmediately );
    }

    @Override
    public void testStarted( Description description )
        throws Exception
    {
        TestSet testSet = getTestSet( description );
        testSet.createTestMethod( description ).attachToThread();
        checkIfTestSetCanBeReported( testSet );
        testSet.attachToThread();
    }

    public abstract void checkIfTestSetCanBeReported( TestSet testSetForTest );

    @Override
    public void testFinished( Description description )
        throws Exception
    {
        getTestMethod().testFinished();
        TestSet.getThreadTestSet().incrementFinishedTests( reporterManager, reportImmediately );
        detachTestMethodFromThread();
    }

    public static ConcurrentReportingRunListener createInstance( ReporterManagerFactory reporterManagerFactory,
                                                                 boolean parallelClasses, boolean parallelBoth )
        throws TestSetFailedException
    {
        if ( parallelClasses )
        {
            return new ClassesParallelRunListener( reporterManagerFactory );
        }
        return new MethodsParallelRunListener( reporterManagerFactory, !parallelBoth );
    }

    public static class ClassesParallelRunListener
        extends ConcurrentReportingRunListener
    {
        public ClassesParallelRunListener( ReporterManagerFactory reporterFactory )
            throws TestSetFailedException
        {
            super( reporterFactory, false );
        }

        @Override
        public void checkIfTestSetCanBeReported( TestSet testSetForTest )
        {
            TestSet currentlyAttached = TestSet.getThreadTestSet();
            if ( currentlyAttached != null && currentlyAttached != testSetForTest )
            {
                currentlyAttached.setAllScheduled( reporterManager );
            }
        }
    }

    public static class MethodsParallelRunListener
        extends ConcurrentReportingRunListener
    {
        private volatile TestSet lastStarted;

        private final Object lock = new Object();

        public MethodsParallelRunListener( ReporterManagerFactory reporterFactory, boolean reportImmediately )
            throws TestSetFailedException
        {
            super( reporterFactory, reportImmediately );
        }

        @Override
        public void checkIfTestSetCanBeReported( TestSet testSetForTest )
        {
            synchronized ( lock )
            {
                if ( testSetForTest != lastStarted )
                {
                    if ( lastStarted != null )
                    {
                        lastStarted.setAllScheduled( reporterManager );
                    }
                    lastStarted = testSetForTest;
                }
            }
        }
    }
}
