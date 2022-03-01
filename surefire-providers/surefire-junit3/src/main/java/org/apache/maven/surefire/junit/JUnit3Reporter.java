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

import org.apache.maven.surefire.api.report.OutputReportEntry;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.RunListener;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.report.TestOutputReceiver;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.report.ClassMethodIndexer;
import org.apache.maven.surefire.report.RunModeSetter;

/**
 * This implementation of {@link RunListener} handles {@link OutputReportEntry} in the
 * {@link TestOutputReceiver output receiver}, downcasting to {@link TestOutputReportEntry}, and
 * delegates the report entry to the {@link TestReportListener}.
 * This object necessarily requires setting the {@link RunMode} in order to behave properly.
 */
final class JUnit3Reporter
    implements RunListener, TestOutputReceiver<OutputReportEntry>, RunModeSetter
{
    private final ClassMethodIndexer classMethodIndexer = new ClassMethodIndexer();
    private final TestReportListener<TestOutputReportEntry> reporter;
    private volatile RunMode runMode;

    JUnit3Reporter( TestReportListener<TestOutputReportEntry> reporter )
    {
        this.reporter = reporter;
    }

    ClassMethodIndexer getClassMethodIndexer()
    {
        return classMethodIndexer;
    }

    @Override
    public void setRunMode( RunMode runMode )
    {
        this.runMode = runMode;
    }

    @Override
    public void testSetStarting( TestSetReportEntry report )
    {
        reporter.testSetStarting( report );
    }

    @Override
    public void testSetCompleted( TestSetReportEntry report )
    {
        reporter.testSetCompleted( report );
    }

    @Override
    public void testStarting( ReportEntry report )
    {
        reporter.testStarting( report );
    }

    @Override
    public void testSucceeded( ReportEntry report )
    {
        reporter.testSucceeded( report );
    }

    @Override
    public void testAssumptionFailure( ReportEntry report )
    {
        reporter.testAssumptionFailure( report );
    }

    @Override
    public void testError( ReportEntry report )
    {
        reporter.testError( report );
    }

    @Override
    public void testFailed( ReportEntry report )
    {
        reporter.testFailed( report );
    }

    @Override
    public void testSkipped( ReportEntry report )
    {
        reporter.testSkipped( report );
    }

    @Override
    public void testExecutionSkippedByUser()
    {
        reporter.testExecutionSkippedByUser();
    }

    @Override
    public void writeTestOutput( OutputReportEntry reportEntry )
    {
        Long testRunId = classMethodIndexer.getLocalIndex();
        reporter.writeTestOutput( new TestOutputReportEntry( reportEntry, runMode, testRunId ) );
    }
}
