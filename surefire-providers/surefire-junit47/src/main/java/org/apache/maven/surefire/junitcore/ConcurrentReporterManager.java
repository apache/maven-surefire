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
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.testset.TestSetFailedException;

/**
 * Handles responses from concurrent junit
 * <p/>
 * Stuff to remember about JUnit threading:
 * parallel=classes; beforeClass/afterClass, constructor and all tests method run on same thread
 * parallel=methods; beforeClass/afterClass run on main thread, constructor + each test method run on same thread
 * parallel=both; same as parallel=methods
 *
 * @author Kristian Rosenvold
 */
public abstract class ConcurrentReporterManager
    implements RunListener, ConsoleOutputReceiver
{
    private final Map<String, TestSet> classMethodCounts;

    private final ThreadLocal<RunListener> reporterManagerThreadLocal = new ThreadLocal<RunListener>();

    private final boolean reportImmediately;

    private final ReporterFactory reporterFactory;

    private final ConsoleLogger consoleLogger;

    ConcurrentReporterManager( ReporterFactory reporterFactory, ConsoleLogger consoleLogger, boolean reportImmediately,
                               Map<String, TestSet> classMethodCounts )
        throws TestSetFailedException
    {
        this.reportImmediately = reportImmediately;
        this.reporterFactory = reporterFactory;
        this.classMethodCounts = classMethodCounts;
        this.consoleLogger = consoleLogger;
    }

    public void testSetStarting( ReportEntry description )
    {
    }

    public void testSetCompleted( ReportEntry result )
    {
        final RunListener reporterManager = getRunListener();
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
        final TestMethod testMethod = getOrCreateTestMethod( failure );
        if ( testMethod != null )
        {
            testMethod.testError( failure );
        }
    }

    public void testSkipped( ReportEntry description )
    {
        TestSet testSet = getTestSet( description );
        TestMethod testMethod = getTestSet( description ).createTestMethod( description );
        testMethod.testIgnored( description );
        testSet.incrementFinishedTests( getRunListener(), reportImmediately );
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
        TestSet.getThreadTestSet().incrementFinishedTests( getRunListener(), reportImmediately );
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
        if ( testSet == null )
        {
            consoleLogger.info( description.getName() );
            consoleLogger.info( description.getStackTraceWriter().writeTraceToString() );
            return null;
        }
        else
        {
            return testSet.createTestMethod( description );
        }
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
        TestSet testSet = classMethodCounts.get( description.getSourceName() );
        if ( testSet == null )
        {
            testSet = classMethodCounts.get( ClassDemarcatingRunner.getCurrentTestClass() );
        }
        return testSet;
    }

    RunListener getRunListener()
    {
        RunListener runListener = reporterManagerThreadLocal.get();
        if ( runListener == null )
        {
            runListener = reporterFactory.createReporter();
            reporterManagerThreadLocal.set( runListener );
        }
        return runListener;
    }

    public static ConcurrentReporterManager createInstance( Map<String, TestSet> classMethodCounts,
                                                            ReporterFactory reporterManagerFactory,
                                                            boolean parallelClasses, boolean parallelBoth,
                                                            ConsoleLogger consoleLogger )
        throws TestSetFailedException
    {
        if ( parallelClasses )
        {
            return new ClassesParallelRunListener( classMethodCounts, reporterManagerFactory, consoleLogger );
        }
        return new MethodsParallelRunListener( classMethodCounts, reporterManagerFactory, !parallelBoth,
                                               consoleLogger );
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
            String currentTestClassName = ClassDemarcatingRunner.getCurrentTestClass();
            TestSet testSet = currentTestClassName != null ? classMethodCounts.get( currentTestClassName ) : null;
            if ( testSet != null )
            {
                testSet.getClassLevelLogicalStream().write( stdout, buf, off, len );
            }
            else
            {
                // Not able to assocaite output with any thread. Just dump to console
                consoleLogger.info( new String( buf, off, len ) );
            }
        }
    }

}
