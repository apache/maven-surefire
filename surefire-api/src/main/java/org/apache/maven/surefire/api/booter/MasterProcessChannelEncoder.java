package org.apache.maven.surefire.api.booter;

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

import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestSetReportEntry;

/**
 * An abstraction for physical encoder of events.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M5
 */
public interface MasterProcessChannelEncoder
{
    /**
     * @return {@code true} if the encoder's stream has got an error
     */
    boolean checkError();

    /**
     * Called on JVM exit error.
     */
    void onJvmExit();

    /**
     * The test set has started.
     *
     * @param reportEntry     test set report entry
     * @param trimStackTraces {@code true} if stack trace trimming
     */
    void testSetStarting( TestSetReportEntry reportEntry, boolean trimStackTraces );

    /**
     * The test set has finished.
     *
     * @param reportEntry     test set report entry
     * @param trimStackTraces {@code true} if stack trace trimming
     */
    void testSetCompleted( TestSetReportEntry reportEntry, boolean trimStackTraces );

    /**
     * The test has started.
     *
     * @param reportEntry     test set report entry
     * @param trimStackTraces {@code true} if stack trace trimming
     */
    void testStarting( ReportEntry reportEntry, boolean trimStackTraces );

    /**
     * The test has succeeded.
     *
     * @param reportEntry     test set report entry
     * @param trimStackTraces {@code true} if stack trace trimming
     */
    void testSucceeded( ReportEntry reportEntry, boolean trimStackTraces );

    /**
     * The test has failed.
     *
     * @param reportEntry     test set report entry
     * @param trimStackTraces {@code true} if stack trace trimming
     */
    void testFailed( ReportEntry reportEntry, boolean trimStackTraces );

    /**
     * The test is skipped.
     *
     * @param reportEntry     test set report entry
     * @param trimStackTraces {@code true} if stack trace trimming
     */
    void testSkipped( ReportEntry reportEntry, boolean trimStackTraces );

    /**
     * The test error.
     *
     * @param reportEntry     test set report entry
     * @param trimStackTraces {@code true} if stack trace trimming
     */
    void testError( ReportEntry reportEntry, boolean trimStackTraces );

    /**
     * The test assumption failure.
     *
     * @param reportEntry     test set report entry
     * @param trimStackTraces {@code true} if stack trace trimming
     */
    void testAssumptionFailure( ReportEntry reportEntry, boolean trimStackTraces );

    /**
     * Test output, a line or characters.
     *
     * @param reportEntry std/out or std/err context
     */
    void testOutput( TestOutputReportEntry reportEntry );

    /**
     * Info log.
     *
     * @param msg message of info logger
     */
    void consoleInfoLog( String msg );

    /**
     * Error log.
     *
     * @param msg message of error logger
     */
    void consoleErrorLog( String msg );

    /**
     * Error log.
     *
     * @param t exception
     */
    void consoleErrorLog( Throwable t );

    /**
     * Error log.
     *
     * @param msg additional error message
     * @param t   exception
     */
    void consoleErrorLog( String msg, Throwable t );

    /**
     * Error log.
     *
     * @param stackTraceWriter printable stack trace
     * @param trimStackTraces  {@code true} if selected trimmed stack trace to print into encoder channel/stream
     */
    void consoleErrorLog( StackTraceWriter stackTraceWriter, boolean trimStackTraces );

    /**
     * Debug log.
     *
     * @param msg message of debug logger
     */
    void consoleDebugLog( String msg );

    /**
     * Warning log.
     *
     * @param msg message of warning logger
     */
    void consoleWarningLog( String msg );

    /**
     * Say BYE on exit.
     * ForkBooter will consequently wait for BYE_ACK command which finally drains the (std/in) sink channel.
     */
    void bye();

    /**
     * The provider wants to stop the progress.
     */
    void stopOnNextTest();

    /**
     * The provider acquires a new test set to run.
     */
    void acquireNextTest();

    /**
     * ForkedBooter tear down has failed while waiting for BYE_ACK command.
     *
     * @param stackTraceWriter printable stack trace
     * @param trimStackTraces  {@code true} if selected trimmed stack trace to print into encoder channel/stream
     */
    void sendExitError( StackTraceWriter stackTraceWriter, boolean trimStackTraces );
}
