/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.surefire.api.report;

/**
 * This report entry should be used in {@link TestOutputReceiver#writeTestOutput(OutputReportEntry)}.
 *
 * {@inheritDoc}
 */
public final class TestOutputReportEntry implements OutputReportEntry {
    private final String log;
    private final boolean isStdOut;
    private final boolean newLine;
    private final RunMode runMode;
    private final Long testRunId;
    /**
     * The stack trace of the thread that produced the output.
     * claasName#method;className#method;...
     */
    private String stack;
    /**
     * Wraps the output from the running test-case.
     *
     * @param log stdout/sterr output from running tests
     * @param isStdOut indicates if this is stdout
     * @param newLine print on new line
     * @param runMode the phase of testset
     * @param testRunId unique id of the test run pointing to the test description
     */
    public TestOutputReportEntry(String log, boolean isStdOut, boolean newLine, RunMode runMode, Long testRunId) {
        this.log = log;
        this.isStdOut = isStdOut;
        this.newLine = newLine;
        this.runMode = runMode;
        this.testRunId = testRunId;
        this.stack = String.join(";", StackTraceProvider.getStack());
    }

    /**
     * Wraps the output from the running test-case.
     *
     * @param log stdout/sterr output from running tests
     * @param isStdOut indicates if this is stdout
     * @param newLine print on new line
     */
    private TestOutputReportEntry(String log, boolean isStdOut, boolean newLine) {
        this(log, isStdOut, newLine, null, null);
    }

    public TestOutputReportEntry(OutputReportEntry reportEntry, RunMode runMode, Long testRunId) {
        this(reportEntry.getLog(), reportEntry.isStdOut(), reportEntry.isNewLine(), runMode, testRunId);
    }

    /**
     * Constructor used when receiving output from a forked JVM where the stack trace was captured
     * on the forked side.
     *
     * @param log stdout/stderr output
     * @param isStdOut true if stdout
     * @param newLine true if newline
     * @param runMode the run mode
     * @param testRunId the test run id
     * @param stack the stack trace captured on the forked JVM side
     */
    public TestOutputReportEntry(
            String log, boolean isStdOut, boolean newLine, RunMode runMode, Long testRunId, String stack) {
        this.log = log;
        this.isStdOut = isStdOut;
        this.newLine = newLine;
        this.runMode = runMode;
        this.testRunId = testRunId;
        this.stack = stack;
    }

    @Override
    public String getLog() {
        return log;
    }

    @Override
    public boolean isStdOut() {
        return isStdOut;
    }

    @Override
    public boolean isNewLine() {
        return newLine;
    }

    public RunMode getRunMode() {
        return runMode;
    }

    public Long getTestRunId() {
        return testRunId;
    }

    @Override
    public String getStack() {
        return stack;
    }

    public static OutputReportEntry stdOut(String log) {
        return new TestOutputReportEntry(log, true, false);
    }

    public static TestOutputReportEntry stdOutln(String log) {
        return new TestOutputReportEntry(log, true, true);
    }

    public static TestOutputReportEntry stdErr(String log) {
        return new TestOutputReportEntry(log, false, false);
    }

    public static TestOutputReportEntry stdErrln(String log) {
        return new TestOutputReportEntry(log, false, true);
    }
}
