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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.report.TestSetReportEntry;

import static org.apache.maven.surefire.junitcore.TestMethod.getThreadTestMethod;

/**
 * Handles responses from concurrent junit
 * <br>
 * Stuff to remember about JUnit threading:
 * parallel=classes; beforeClass/afterClass, constructor and all tests method run on same thread
 * parallel=methods; beforeClass/afterClass run on main thread, constructor + each test method run on same thread
 * parallel=both; same as parallel=methods
 *
 * @see org.apache.maven.surefire.junitcore.JUnitCoreRunListener for details about regular junit run listening
 * @author Kristian Rosenvold
 */
abstract class ConcurrentRunListener
    implements TestReportListener
{
    private final Map<String, TestSet> classMethodCounts;

    private final ThreadLocal<TestReportListener> reporterManagerThreadLocal;

    private final boolean reportImmediately;

    private final ConsoleLogger logger;

    ConcurrentRunListener( final ReporterFactory reporterFactory, boolean reportImmediately,
                           Map<String, TestSet> classMethodCounts )
    {
        this.reportImmediately = reportImmediately;
        this.classMethodCounts = classMethodCounts;
        logger = reporterFactory.createTestReportListener();
        reporterManagerThreadLocal = ThreadLocal.withInitial( reporterFactory::createTestReportListener );
    }

    @Override
    public final void testSetStarting( TestSetReportEntry description )
    {
    }

    @Override
    public final void testSetCompleted( TestSetReportEntry result )
    {
        try
        {
            final TestReportListener reporterManager = getRunListener();
            for ( TestSet testSet : classMethodCounts.values() )
            {
                testSet.replay( reporterManager );
            }
        }
        finally
        {
            reporterManagerThreadLocal.remove();
        }
    }

    @Override
    public final void testFailed( ReportEntry failure )
    {
        final TestMethod testMethod = getOrCreateThreadAttachedTestMethod( failure );
        if ( testMethod != null )
        {
            testMethod.testFailure( failure );
            testMethod.detachFromCurrentThread();
        }
    }

    @Override
    public final void testError( ReportEntry failure )
    {
        final TestMethod testMethod = getOrCreateThreadAttachedTestMethod( failure );
        if ( testMethod != null )
        {
            testMethod.testError( failure );
            testMethod.detachFromCurrentThread();
        }
    }

    @Override
    public final void testSkipped( ReportEntry description )
    {
        TestSet testSet = getTestSet( description );
        TestMethod testMethod = testSet.createThreadAttachedTestMethod( description );
        testMethod.testIgnored( description );
        testSet.incrementFinishedTests( getRunListener(), reportImmediately );
        testMethod.detachFromCurrentThread();
    }

    @Override
    public final void testExecutionSkippedByUser()
    {
        // cannot guarantee proper call to all listeners
        getRunListener().testExecutionSkippedByUser();
    }

    public final RunMode markAs( RunMode currentRunMode )
    {
        return reporterManagerThreadLocal.get().markAs( currentRunMode );
    }

    @Override
    public final void testAssumptionFailure( ReportEntry failure )
    {
        final TestMethod testMethod = getOrCreateThreadAttachedTestMethod( failure );
        if ( testMethod != null )
        {
            testMethod.testAssumption( failure );
            testMethod.detachFromCurrentThread();
        }
    }

    @Override
    public final void testStarting( ReportEntry description )
    {
        TestSet testSet = getTestSet( description );
        testSet.createThreadAttachedTestMethod( description );

        checkIfTestSetCanBeReported( testSet );
        testSet.attachToThread();
    }

    @Override
    public final void testSucceeded( ReportEntry report )
    {
        TestMethod testMethod = getThreadTestMethod();
        if ( testMethod != null )
        {
            testMethod.testFinished();
            testMethod.getTestSet().incrementFinishedTests( getRunListener(), reportImmediately );
            testMethod.detachFromCurrentThread();
        }
    }

    private TestMethod getOrCreateThreadAttachedTestMethod( ReportEntry description )
    {
        TestMethod threadTestMethod = getThreadTestMethod();
        if ( threadTestMethod != null )
        {
            return threadTestMethod;
        }
        TestSet testSet = getTestSet( description );
        if ( testSet == null )
        {
            logger.warning( description.getName() );
            StackTraceWriter writer = description.getStackTraceWriter();
            if ( writer != null )
            {
                logger.error( writer.writeTraceToString() );
            }
            return null;
        }
        else
        {
            return testSet.createThreadAttachedTestMethod( description );
        }
    }

    protected abstract void checkIfTestSetCanBeReported( TestSet testSetForTest );

    private TestSet getTestSet( ReportEntry description )
    {
        return classMethodCounts.get( description.getSourceName() );
    }

    final TestReportListener getRunListener()
    {
        return reporterManagerThreadLocal.get();
    }

    public static TestReportListener createInstance( Map<String, TestSet> classMethodCounts,
                                                     ReporterFactory reporterFactory,
                                                     boolean parallelClasses, boolean parallelBoth )
    {
        return parallelClasses
            ? new ClassesParallelRunListener( classMethodCounts, reporterFactory )
            : new MethodsParallelRunListener( classMethodCounts, reporterFactory, !parallelBoth );
    }


    @Override
    public final void writeTestOutput( TestOutputReportEntry reportEntry )
    {
        TestMethod threadTestMethod = getThreadTestMethod();
        if ( threadTestMethod != null )
        {
            LogicalStream logicalStream = threadTestMethod.getLogicalStream();
            logicalStream.write( reportEntry );
        }
        else
        {
            // Not able to associate output with any thread. Just dump to console
            logger.info( reportEntry.getLog() );
        }
    }

    @Override
    public final boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    @Override
    public final void debug( String message )
    {
        logger.debug( message );
    }

    @Override
    public final boolean isInfoEnabled()
    {
        return logger.isInfoEnabled();
    }

    @Override
    public final void info( String message )
    {
        logger.info( message );
    }

    @Override
    public final boolean isWarnEnabled()
    {
        return logger.isWarnEnabled();
    }

    @Override
    public final void warning( String message )
    {
        logger.warning( message );
    }

    @Override
    public final boolean isErrorEnabled()
    {
        return logger.isErrorEnabled();
    }

    @Override
    public final void error( String message )
    {
        logger.error( message );
    }

    @Override
    public final void error( String message, Throwable t )
    {
        logger.error( message, t );
    }

    @Override
    public final void error( Throwable t )
    {
        logger.error( t );
    }
}
