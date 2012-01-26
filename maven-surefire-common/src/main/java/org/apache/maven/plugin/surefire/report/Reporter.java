package org.apache.maven.plugin.surefire.report;

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

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;

/**
 * Persists reports somewhere
 * <p/>
 * An instance of a reporter is not guaranteed to be thread-safe and concurrent test frameworks
 * must request an instance of a reporter per-thread from the ReporterFactory.
 */
public interface Reporter
{
    /**
     * Indicates the start of a given test-set
     *
     * @param report the report entry describing the testset
     * @throws org.apache.maven.surefire.report.ReporterException
     *          When reporting fails
     */
    void testSetStarting( ReportEntry report )
        throws ReporterException;

    /**
     * Indicates end of a given test-set
     *
     * @param report the report entry describing the testset
     * @throws org.apache.maven.surefire.report.ReporterException
     *          When reporting fails
     */
    void testSetCompleted( ReportEntry report )
        throws ReporterException;

    // Tests

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


    void testSkipped( ReportEntry report );

    /**
     * Event fired when a test ended with an error (non anticipated problem)
     *
     * @param report The report entry to log for
     * @param stdOut standard output from the test case
     * @param stdErr error output from the test case
     */
    void testError( ReportEntry report, String stdOut, String stdErr );

    /**
     * Event fired when a test ended with a failure (anticipated problem)
     *
     * @param report The report entry to log for
     * @param stdOut standard output from the test case
     * @param stdErr error output from the test case
     */
    void testFailed( ReportEntry report, String stdOut, String stdErr );

    /**
     * Writes a message that will be displayed in all free-text format reporters.
     * These messages will be output regardless, as opposed to #writeDetailMessage,
     * which is controlled by reportFormat.
     *
     * @param message The message to write.
     */
    void writeMessage( String message );

    void writeMessage( byte[] b, int off, int len );

    /**
     * Restores the instance of the reporter, making the instance re-usable for a subsequent run in the
     * same thread.
     */
    void reset();
}
