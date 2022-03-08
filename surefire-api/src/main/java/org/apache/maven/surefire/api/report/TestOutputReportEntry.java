package org.apache.maven.surefire.api.report;

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

/**
 * This report entry should be used in {@link TestOutputReceiver#writeTestOutput(OutputReportEntry)}.
 *
 * {@inheritDoc}
 */
public final class TestOutputReportEntry implements OutputReportEntry
{
    private final String log;
    private final boolean isStdOut;
    private final boolean newLine;
    private final RunMode runMode;
    private final Long testRunId;

    /**
     * Wraps the output from the running test-case.
     *
     * @param log stdout/sterr output from running tests
     * @param isStdOut Indicates if this is stdout
     * @param newLine print on new line
     * @param runMode the phase of testset
     * @param testRunId unique id of the test run pointing to the test description
     */
    public TestOutputReportEntry( String log, boolean isStdOut, boolean newLine, RunMode runMode, Long testRunId )
    {
        this.log = log;
        this.isStdOut = isStdOut;
        this.newLine = newLine;
        this.runMode = runMode;
        this.testRunId = testRunId;
    }

    /**
     * Wraps the output from the running test-case.
     *
     * @param log stdout/sterr output from running tests
     * @param isStdOut Indicates if this is stdout
     * @param newLine print on new line
     */
    private TestOutputReportEntry( String log, boolean isStdOut, boolean newLine )
    {
        this( log, isStdOut, newLine, null, null );
    }

    public TestOutputReportEntry( OutputReportEntry reportEntry, RunMode runMode, Long testRunId )
    {
        log = reportEntry.getLog();
        isStdOut = reportEntry.isStdOut();
        newLine = reportEntry.isNewLine();
        this.runMode = runMode;
        this.testRunId = testRunId;
    }

    @Override
    public String getLog()
    {
        return log;
    }

    @Override
    public boolean isStdOut()
    {
        return isStdOut;
    }

    @Override
    public boolean isNewLine()
    {
        return newLine;
    }

    public RunMode getRunMode()
    {
        return runMode;
    }

    public Long getTestRunId()
    {
        return testRunId;
    }

    public static OutputReportEntry stdOut( String log )
    {
        return new TestOutputReportEntry( log, true, false );
    }

    public static TestOutputReportEntry stdOutln( String log )
    {
        return new TestOutputReportEntry( log, true, true );
    }

    public static TestOutputReportEntry stdErr( String log )
    {
        return new TestOutputReportEntry( log, false, false );
    }

    public static TestOutputReportEntry stdErrln( String log )
    {
        return new TestOutputReportEntry( log, false, true );
    }
}
