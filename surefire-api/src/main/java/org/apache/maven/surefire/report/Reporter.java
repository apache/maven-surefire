package org.apache.maven.surefire.report;

import java.util.Collection;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Contract between the different implementations of the Surefire reporters
 *
 * @version $Id$
 */
public interface Reporter
{
    void writeMessage( String message );

    void writeFooter( String footer );

    // The entire run
    void runStarting( int testCount );

    void runCompleted();

    void runStopped();

    void runAborted( ReportEntry report );

    // Test Sets
    void testSetStarting( ReportEntry report )
        throws ReporterException;

    void testSetCompleted( ReportEntry report )
        throws ReporterException;

    void testSetAborted( ReportEntry report );

    // Tests

    /**
     * Event fired when a test is about to start
     *
     * @param report
     */
    void testStarting( ReportEntry report );

    /**
     * Event fired when a test ended successfully
     *
     * @param report
     */
    void testSucceeded( ReportEntry report );

    /**
     * Event fired when a test ended with an error (non anticipated problem)
     *
     * @param report
     * @param stdOut standard output from the test case
     * @param stdErr error output from the test case
     */
    void testError( ReportEntry report, String stdOut, String stdErr );

    /**
     * Event fired when a test ended with a failure (anticipated problem)
     *
     * @param report
     * @param stdOut standard output from the test case
     * @param stdErr error output from the test case
     */
    void testFailed( ReportEntry report, String stdOut, String stdErr );

    void testSkipped( ReportEntry report );

    // Counters
    void reset();

    /**
     * Get the number of errors
     *
     * @return
     */
    int getNumErrors();

    /**
     * Get the number of failures
     *
     * @return
     */
    int getNumFailures();

    /**
     * Get the number of tests
     *
     * @return
     */
    int getNumTests();

    /**
     * Get the number of tests skipped
     *
     * @return
     */
    int getNumSkipped();

    /**
     * Gives the source(s) that causes the error(s).
     *
     * @return The source(s).
     */
    Collection getErrorSources();

    /**
     * Gives the source(s) that causes the failures(s).
     *
     * @return The source(s).
     */
    Collection getFailureSources();
}
