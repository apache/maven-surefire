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

import org.apache.maven.surefire.report.CategorizedReportEntry;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ConsoleOutputReceiverForCurrentThread;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;

/**
 * Represents the test-state of a single test method that is run.
 * <p/>
 * Notes about thread safety: This instance is serially confined to 1-3 threads (construction, test-run, reporting),
 * without any actual parallel access
 */
class TestMethod
    implements ConsoleOutputReceiver
{
    private final ReportEntry description;

    private final TestSet testSet;

    private final long startTime;

    private volatile long endTime;

    private volatile ReportEntry testFailure;

    private volatile ReportEntry testError;

    private volatile ReportEntry ignored;

    private static final InheritableThreadLocal<TestMethod> TEST_METHOD = new InheritableThreadLocal<TestMethod>();

    private volatile LogicalStream output;

    TestMethod( ReportEntry description, TestSet testSet )
    {
        this.description = description;
        this.testSet = testSet;
        startTime = System.currentTimeMillis();
    }

    void testFinished()
    {
        setEndTime();
    }

    void testIgnored( ReportEntry description )
    {
        ignored = description;
        setEndTime();
    }

    void testFailure( ReportEntry failure )
    {
        this.testFailure = failure;
        setEndTime();
    }

    void testError( ReportEntry failure )
    {
        this.testError = failure;
        setEndTime();
    }

    private void setEndTime()
    {
        this.endTime = System.currentTimeMillis();
    }

    int getElapsed()
    {
        return endTime > 0 ? (int) ( endTime - startTime ) : 0;
    }

    long getStartTime()
    {
        return startTime;
    }

    long getEndTime()
    {
        return endTime;
    }

    void replay( RunListener reporter )
    {

        if ( ignored != null )
        {
            reporter.testSkipped( createReportEntry( ignored ) );
            return;
        }

        ReportEntry descriptionReport = createReportEntry( description );
        reporter.testStarting( descriptionReport );
        if ( output != null )
        {
            output.writeDetails( ( (ConsoleOutputReceiver) reporter ) );
        }

        if ( testFailure != null )
        {
            reporter.testFailed( createReportEntry( testFailure ) );
        }
        else if ( testError != null )
        {
            reporter.testError( createReportEntry( testError ) );
        }
        else
        {
            reporter.testSucceeded( descriptionReport );
        }
    }

    private ReportEntry createReportEntry( ReportEntry reportEntry )
    {
        return new CategorizedReportEntry( reportEntry.getSourceName(), reportEntry.getName(), reportEntry.getGroup(),
                                           reportEntry.getStackTraceWriter(), getElapsed(), reportEntry.getMessage() );
    }

    void attachToThread()
    {
        TEST_METHOD.set( this );
        ConsoleOutputReceiverForCurrentThread.set( this );
    }

    void detachFromCurrentThread()
    {
        TEST_METHOD.remove();
        ConsoleOutputReceiverForCurrentThread.remove();
    }

    static TestMethod getThreadTestMethod()
    {
        return TEST_METHOD.get();
    }

    LogicalStream getLogicalStream()
    {
        if ( output == null )
        {
            output = new LogicalStream();
        }
        return output;
    }

    public void writeTestOutput( byte[] buf, int off, int len, boolean stdout )
    {
        getLogicalStream().write( stdout, buf, off, len );
    }

    public TestSet getTestSet()
    {
        return testSet;
    }
}
