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

import java.util.Map;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.testset.TestSetFailedException;

/**
 * @author Kristian Rosenvold
 */
public abstract class ConcurrentReporterManager
    implements RunListener, ConsoleOutputReceiver
{
    private final Map<String, TestSet> classMethodCounts;
    // private final ReporterConfiguration reporterConfiguration;

    private final ThreadLocal<RunListener> reporterManagerThreadLocal = new ThreadLocal<RunListener>();

    private final boolean reportImmediately;

    private final ReporterFactory reporterFactory;

    ConcurrentReporterManager( ReporterFactory reporterFactory, boolean reportImmediately,
                               Map<String, TestSet> classMethodCounts )
        throws TestSetFailedException
    {
        this.reportImmediately = reportImmediately;
        this.reporterFactory = reporterFactory;
        this.classMethodCounts = classMethodCounts;

        // We must create the first reporterManager here, even though we will never use it.
        // There is some room for improvement here
        // todo: Find out if needed for 2.8.1
        this.reporterFactory.createReporter();
    }

    public void testSetStarting( ReportEntry description )
    {
    }

    public void testSetCompleted( ReportEntry result )
    {
        final RunListener reporterManager = getReporterManager();
        for ( TestSet testSet : classMethodCounts.values() )
        {
            testSet.replay( reporterManager );
        }
    }

    public void testFailed( ReportEntry failure )
    {
        getOrCreateTestMethod( failure ).testFailure( failure );
    }

    public void testError( ReportEntry failure )
    {
        getOrCreateTestMethod( failure ).testError( failure );
    }

    public void testSkipped( ReportEntry description )
    {
        TestSet testSet = getTestSet( description );
        TestMethod testMethod = getTestSet( description ).createTestMethod( description );
        testMethod.testIgnored( description );
        testSet.incrementFinishedTests( getReporterManager(), reportImmediately );
    }

    public void testAssumptionFailure( ReportEntry failure )
    {
        getOrCreateTestMethod( failure ).testIgnored( failure );
    }

    public void testStarting( ReportEntry description )
    {
        TestSet testSet = getTestSet( description );
        final TestMethod testMethod = testSet.createTestMethod( description );
        testMethod.attachToThread();

        checkIfTestSetCanBeReported( testSet );
        testSet.attachToThread();
    }

    public void testSucceeded( ReportEntry report )
    {
        getTestMethod().testFinished();
        TestSet.getThreadTestSet().incrementFinishedTests( getReporterManager(), reportImmediately );
        detachTestMethodFromThread();
    }

    private TestMethod getOrCreateTestMethod( ReportEntry description )
    {
        TestMethod threadTestMethod = TestMethod.getThreadTestMethod();
        if ( threadTestMethod != null )
        {
            return threadTestMethod;
        }
        TestSet testSet = getTestSet( description );
        return testSet.createTestMethod( description );
    }

    protected abstract void checkIfTestSetCanBeReported( TestSet testSetForTest );

    TestMethod getTestMethod()
    {
        return TestMethod.getThreadTestMethod();
    }

    void detachTestMethodFromThread()
    {
        TestMethod.detachFromCurrentThread();
    }

    TestSet getTestSet( ReportEntry description )
    {
        return classMethodCounts.get( description.getSourceName() );
    }

    RunListener getReporterManager()
    {
        RunListener reporterManager = reporterManagerThreadLocal.get();
        if ( reporterManager == null )
        {
            reporterManager = reporterFactory.createReporter();
            reporterManagerThreadLocal.set( reporterManager );
        }
        return reporterManager;
    }

    public static ConcurrentReporterManager createInstance( Map<String, TestSet> classMethodCounts,
                                                            ReporterFactory reporterManagerFactory, boolean parallelClasses, boolean parallelBoth )
        throws TestSetFailedException
    {
        if ( parallelClasses )
        {
            return new ClassesParallelRunListener( classMethodCounts, reporterManagerFactory );
        }
        return new MethodsParallelRunListener( classMethodCounts, reporterManagerFactory, !parallelBoth );
    }


    public void writeTestOutput( byte[] buf, int off, int len, boolean stdout )
    {
        TestMethod threadTestMethod = TestMethod.getThreadTestMethod();
        if ( threadTestMethod != null )
        {
            final LogicalStream logicalStream = threadTestMethod.getLogicalStream();
            logicalStream.write( stdout, buf, off, len );
        }
        else
        {
            // No test running, just dump the message "somewhere"
            reporterFactory.createConsoleReporter().writeMessage( new String( buf, off, len ) );
        }
    }

}
