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
 * @see org.apache.maven.surefire.junitcore.JUnitCoreRunListener for details about regular junit run listening
 * @author Kristian Rosenvold
 */
public abstract class ConcurrentRunListener
    implements RunListener, ConsoleOutputReceiver
{
    private final Map<String, TestSet> classMethodCounts;

    private final ThreadLocal<RunListener> reporterManagerThreadLocal;

    private final boolean reportImmediately;

    private final ReporterFactory reporterFactory;

    private final ConsoleLogger consoleLogger;

    ConcurrentRunListener( ReporterFactory reporterFactory, ConsoleLogger consoleLogger, boolean reportImmediately,
                           Map<String, TestSet> classMethodCounts )
        throws TestSetFailedException
    {
        this.reportImmediately = reportImmediately;
        this.reporterFactory = reporterFactory;
        this.classMethodCounts = classMethodCounts;
        this.consoleLogger = consoleLogger;

        this.reporterManagerThreadLocal = new ThreadLocal<RunListener>()
        {
            @Override
            protected RunListener initialValue()
            {
                return ConcurrentRunListener.this.reporterFactory.createReporter();
            }
        };
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
        reporterManagerThreadLocal.remove();
    }

    public void testFailed( ReportEntry failure )
    {
        final TestMethod testMethod = getOrCreateThreadAttachedTestMethod( failure );
        if ( testMethod != null )
        {
            testMethod.testFailure( failure );
            testMethod.detachFromCurrentThread();
        }
    }

    public void testError( ReportEntry failure )
    {
        final TestMethod testMethod = getOrCreateThreadAttachedTestMethod( failure );
        if ( testMethod != null )
        {
            testMethod.testError( failure );
            testMethod.detachFromCurrentThread();
        }
    }

    public void testSkipped( ReportEntry description )
    {
        TestSet testSet = getTestSet( description );
        TestMethod testMethod = testSet.createThreadAttachedTestMethod( description );
        testMethod.testIgnored( description );
        testSet.incrementFinishedTests( getRunListener(), reportImmediately );
        testMethod.detachFromCurrentThread();
    }

    public void testAssumptionFailure( ReportEntry failure )
    {
        final TestMethod testMethod = getOrCreateThreadAttachedTestMethod( failure );
        if ( testMethod != null )
        {
            testMethod.testIgnored( failure );
            testMethod.detachFromCurrentThread();
        }
    }

    public void testStarting( ReportEntry description )
    {
        TestSet testSet = getTestSet( description );
        testSet.createThreadAttachedTestMethod( description );

        checkIfTestSetCanBeReported( testSet );
        testSet.attachToThread();
    }

    public void testSucceeded( ReportEntry report )
    {
        TestMethod testMethod = getTestMethod();
        if ( null != testMethod )
        {
            testMethod.testFinished();
            testMethod.getTestSet().incrementFinishedTests( getRunListener(), reportImmediately );
            testMethod.detachFromCurrentThread();
        }
    }

    private TestMethod getOrCreateThreadAttachedTestMethod( ReportEntry description )
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
            return testSet.createThreadAttachedTestMethod( description );
        }
    }

    protected abstract void checkIfTestSetCanBeReported( TestSet testSetForTest );

    TestMethod getTestMethod()
    {
        return TestMethod.getThreadTestMethod();
    }

    TestSet getTestSet( ReportEntry description )
    {
        return classMethodCounts.get( description.getSourceName() );
    }

    RunListener getRunListener()
    {
        return reporterManagerThreadLocal.get();
    }


    public static ConcurrentRunListener createInstance( Map<String, TestSet> classMethodCounts,
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
            // Not able to assocaite output with any thread. Just dump to console
            consoleLogger.info( new String( buf, off, len ) );
        }
    }

}
