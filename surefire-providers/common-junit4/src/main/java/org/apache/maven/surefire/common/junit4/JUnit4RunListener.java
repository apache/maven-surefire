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
package org.apache.maven.surefire.common.junit4;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.report.OutputReportEntry;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.RunMode;
import org.apache.maven.surefire.api.report.SimpleReportEntry;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.TestOutputReceiver;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.internal.ClassMethod;
import org.apache.maven.surefire.report.ClassMethodIndexer;
import org.apache.maven.surefire.report.RunModeSetter;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import static org.apache.maven.surefire.api.report.SimpleReportEntry.assumption;
import static org.apache.maven.surefire.api.report.SimpleReportEntry.ignored;
import static org.apache.maven.surefire.api.report.SimpleReportEntry.withException;
import static org.apache.maven.surefire.common.junit4.JUnit4ProviderUtil.isFailureInsideJUnitItself;
import static org.apache.maven.surefire.common.junit4.JUnit4ProviderUtil.toClassMethod;
import static org.apache.maven.surefire.common.junit4.JUnit4Reflector.getAnnotatedIgnoreValue;

/**
 * RunListener for JUnit4, delegates to our own RunListener
 *
 */
public class JUnit4RunListener extends RunListener implements TestOutputReceiver<OutputReportEntry>, RunModeSetter {
    protected final ClassMethodIndexer classMethodIndexer = new ClassMethodIndexer();
    protected final TestReportListener<TestOutputReportEntry> reporter;
    private volatile RunMode runMode;

    /**
     * This flag is set after a failure has occurred so that a
     * {@link org.apache.maven.surefire.api.report.RunListener#testSucceeded} event is not fired.
     * This is necessary because JUnit4 always fires a
     * {@link org.junit.runner.notification.RunListener#testRunFinished(Result)}
     * event-- even if there was a failure.
     */
    private final ThreadLocal<Boolean> failureFlag = new InheritableThreadLocal<>();

    /**
     * Constructor.
     *
     * @param reporter the reporter to log testing events to
     */
    public JUnit4RunListener(TestReportListener<TestOutputReportEntry> reporter) {
        this.reporter = reporter;
    }

    public final ConsoleLogger getConsoleLogger() {
        return reporter;
    }

    @Override
    public void setRunMode(RunMode runMode) {
        this.runMode = runMode;
    }

    protected final RunMode getRunMode() {
        return runMode;
    }

    // Testrun methods are not invoked when using the runner

    /**
     * Called when a specific test has been skipped (for whatever reason).
     *
     * @see org.junit.runner.notification.RunListener#testIgnored(org.junit.runner.Description)
     */
    @Override
    public void testIgnored(Description description) throws Exception {
        String reason = getAnnotatedIgnoreValue(description);
        ClassMethod classMethod = toClassMethod(description);
        long testRunId = classMethodIndexer.indexClassMethod(classMethod.getClazz(), classMethod.getMethod());
        reporter.testSkipped(
                ignored(runMode, testRunId, classMethod.getClazz(), null, classMethod.getMethod(), null, reason));
    }

    /**
     * Called when a specific test has started.
     *
     * @see org.junit.runner.notification.RunListener#testStarted(org.junit.runner.Description)
     */
    @Override
    public void testStarted(Description description) throws Exception {
        try {
            reporter.testStarting(createReportEntry(description));
        } finally {
            failureFlag.remove();
        }
    }

    /**
     * Called when a specific test has failed.
     *
     * @see org.junit.runner.notification.RunListener#testFailure(org.junit.runner.notification.Failure)
     */
    @Override
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    public void testFailure(Failure failure) throws Exception {
        try {
            StackTraceWriter stackTrace = createStackTraceWriter(failure);
            ClassMethod classMethod = toClassMethod(failure.getDescription());
            long testRunId = classMethodIndexer.indexClassMethod(classMethod.getClazz(), classMethod.getMethod());
            ReportEntry report = withException(
                    runMode, testRunId, classMethod.getClazz(), null, classMethod.getMethod(), null, stackTrace);

            if (failure.getException() instanceof AssertionError) {
                reporter.testFailed(report);
            } else {
                reporter.testError(report);
            }
        } finally {
            failureFlag.set(true);
        }
    }

    public void testAssumptionFailure(Failure failure) {
        try {
            Description desc = failure.getDescription();
            ClassMethod classMethod = toClassMethod(desc);
            long testRunId = classMethodIndexer.indexClassMethod(classMethod.getClazz(), classMethod.getMethod());
            ReportEntry report = assumption(
                    runMode,
                    testRunId,
                    classMethod.getClazz(),
                    null,
                    classMethod.getMethod(),
                    null,
                    failure.getMessage());
            reporter.testAssumptionFailure(report);
        } finally {
            failureFlag.set(true);
        }
    }

    /**
     * Called after a specific test has finished.
     *
     * @see org.junit.runner.notification.RunListener#testFinished(org.junit.runner.Description)
     */
    @Override
    public void testFinished(Description description) throws Exception {
        Boolean failure = failureFlag.get();
        if (failure == null) {
            reporter.testSucceeded(createReportEntry(description));
        }
    }

    /**
     * Delegates to {@link org.apache.maven.surefire.api.report.RunListener#testExecutionSkippedByUser()}.
     */
    public void testExecutionSkippedByUser() {
        reporter.testExecutionSkippedByUser();
    }

    protected StackTraceWriter createStackTraceWriter(Failure failure) {
        return new JUnit4StackTraceWriter(failure);
    }

    protected SimpleReportEntry createReportEntry(Description description) {
        ClassMethod classMethod = toClassMethod(description);
        long testRunId = classMethodIndexer.indexClassMethod(classMethod.getClazz(), classMethod.getMethod());
        return new SimpleReportEntry(runMode, testRunId, classMethod.getClazz(), null, classMethod.getMethod(), null);
    }

    public static void rethrowAnyTestMechanismFailures(Result run) throws TestSetFailedException {
        for (Failure failure : run.getFailures()) {
            if (isFailureInsideJUnitItself(failure.getDescription())) {
                throw new TestSetFailedException(
                        failure.getTestHeader() + " :: " + failure.getMessage(), failure.getException());
            }
        }
    }

    @Override
    public void writeTestOutput(OutputReportEntry reportEntry) {
        Long testRunId = classMethodIndexer.getLocalIndex();
        reporter.writeTestOutput(new TestOutputReportEntry(reportEntry, runMode, testRunId));
    }
}
