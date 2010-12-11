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

import org.apache.maven.surefire.report.Reporter;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kristian Rosenvold
 */
public abstract class ConcurrentReportingRunListener
    extends RunListener
{
    protected Map<Class, TestSet> classMethodCounts = new ConcurrentHashMap<Class, TestSet>();

    private final ReporterConfiguration reporterConfiguration;

    private final ThreadLocal<Reporter> reporterManagerThreadLocal = new ThreadLocal<Reporter>();

    protected final boolean reportImmediately;

    private final ConcurrentPrintStream out = new ConcurrentPrintStream( true );

    private final ConcurrentPrintStream err = new ConcurrentPrintStream( false );

    private ReporterFactory reporterFactory;

    public ConcurrentReportingRunListener( ReporterFactory reporterFactory, boolean reportImmediately,
                                           ReporterConfiguration reporterConfiguration )
        throws TestSetFailedException
    {
        this.reportImmediately = reportImmediately;
        this.reporterFactory = reporterFactory;
        this.reporterConfiguration = reporterConfiguration;

        // We must create the first reporterManager here, even though we will never use it.
        // There is some room for improvement here
        this.reporterFactory.createReporter();
        // Important: We must capture System.out/System.err AFTER the  reportManager captures stdout/stderr
        // because we know how to demultiplex correctly. The redirection in reporterManager is basically
        // ignored/unused because we use ConcurrentPrintStream.
        System.setOut( out );
        System.setErr( err );
    }


    protected Reporter getReporterManager()
        throws TestSetFailedException
    {
        Reporter reporterManager = reporterManagerThreadLocal.get();
        if ( reporterManager == null )
        {
            reporterManager = reporterFactory.createReporter();
            reporterManagerThreadLocal.set( reporterManager );
        }
        return reporterManager;
    }

    public static ConcurrentReportingRunListener createInstance( ReporterFactory reporterManagerFactory,
                                                                 ReporterConfiguration reporterConfiguration,
                                                                 boolean parallelClasses, boolean parallelBoth )
        throws TestSetFailedException
    {
        if ( parallelClasses )
        {
            return new ClassesParallelRunListener( reporterManagerFactory, reporterConfiguration );
        }
        return new MethodsParallelRunListener( reporterManagerFactory, reporterConfiguration, !parallelBoth );
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
            testSet.replay( getReporterManager() );
        }
        out.writeTo( reporterConfiguration.getOriginalSystemOut() );
        err.writeTo( reporterConfiguration.getOriginalSystemErr() );
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
        getOrCreateTestMethod( failure.getDescription() ).testFailure( failure );
    }

    private TestMethod getOrCreateTestMethod( Description description )
    {
        TestMethod threadTestMethod = TestMethod.getThreadTestMethod();
        if ( threadTestMethod != null )
        {
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
        testSet.incrementFinishedTests( getReporterManager(), reportImmediately );
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

    public abstract void checkIfTestSetCanBeReported( TestSet testSetForTest )
        throws TestSetFailedException;

    @Override
    public void testFinished( Description description )
        throws Exception
    {
        getTestMethod().testFinished();
        TestSet.getThreadTestSet().incrementFinishedTests( getReporterManager(), reportImmediately );
        detachTestMethodFromThread();
    }


}
