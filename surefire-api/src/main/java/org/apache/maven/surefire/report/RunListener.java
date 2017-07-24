package org.apache.maven.surefire.report;

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
 * Used by providers to report results.
 * Using this interface integrates the providers together into a common reporting infrastructure.
 * <br>
 * An instance of a reporter is not guaranteed to be thread-safe and concurrent test frameworks
 * must request an instance of a reporter per-thread from the ReporterFactory.
 */
public interface RunListener
{
    /**
     * Indicates the start of a given test-set
     *
     * @param report the report entry describing the testset
     * @throws ReporterException When reporting fails
     */
    void testSetStarting( TestSetReportEntry report );

    /**
     * Indicates end of a given test-set
     *
     * @param report the report entry describing the testset
     * @throws ReporterException When reporting fails
     */
    void testSetCompleted( TestSetReportEntry report );

    /**
     * Event fired when a test is about to start
     *
     * @param report The report entry to log for
     */
    void testStarting( ReportEntry report );

    /**
     * Event fired when a test ended successfully
     *
     * @param report The report entry to log for
     */
    void testSucceeded( ReportEntry report );

    /**
     * Event fired when a test assumption failure was encountered.
     * An assumption failure indicates that the test is not relevant
     *
     * @param report The report entry to log for
     */
    void testAssumptionFailure( ReportEntry report );

    /**
     * Event fired when a test ended with an error (non anticipated problem)
     *
     * @param report The report entry to log for
     */
    void testError( ReportEntry report );

    /**
     * Event fired when a test ended with a failure (anticipated problem)
     *
     * @param report The report entry to log for
     */
    void testFailed( ReportEntry report );

    /**
     * Event fired when a test is skipped
     *
     * @param report The report entry to log for
     */
    void testSkipped( ReportEntry report );

    /**
     * Event fired skipping an execution of remaining test-set in other fork(s); or does nothing if no forks.
     * The method is called by {@link org.apache.maven.surefire.providerapi.SurefireProvider}.<p>
     * (The event is fired after the Nth test failed to signal skipping the rest of test-set.)
     */
    void testExecutionSkippedByUser();
}
