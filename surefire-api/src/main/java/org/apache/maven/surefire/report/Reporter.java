package org.apache.maven.surefire.report;

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

public interface Reporter
{
    void writeMessage( String message );

    // The entire run
    void runStarting( int testCount );

    void runCompleted();

    void runStopped();

    void runAborted( ReportEntry report );

    // Battery
    void batteryStarting( ReportEntry report )
        throws Exception;

    void batteryCompleted( ReportEntry report );

    void batteryAborted( ReportEntry report );

    // Tests
    void testStarting( ReportEntry report );

    void testSucceeded( ReportEntry report );

    void testError( ReportEntry report, String stdOut, String stdErr );

    void testFailed( ReportEntry report, String stdOut, String stdErr );

    void dispose();

    // Counters
    int getNbErrors();

    int getNbFailures();

    int getNbTests();

    void setReportsDirectory( String reportsDirectory );
}
