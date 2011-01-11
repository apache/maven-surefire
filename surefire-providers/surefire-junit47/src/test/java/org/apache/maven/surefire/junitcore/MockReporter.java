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

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.Reporter;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MockReporter
    implements Reporter
{
    private final List<String> events = new ArrayList<String>();

    public static final String RUN_STARTED = "RUN_STARTED";

    public static final String RUN_COMPLETED = "RUN_COMPLETED";

    public static final String SET_STARTED = "SET_STARTED";

    public static final String SET_COMPLETED = "SET_COMPLETED";

    public static final String TEST_STARTED = "TEST_STARTED";

    public static final String TEST_COMPLETED = "TEST_COMPLETED";

    public static final String TEST_FAILED = "TEST_FAILED";

    public static final String TEST_ERROR = "TEST_ERROR";

    public static final String TEST_SKIPPED = "TEST_SKIPPED";

    private final AtomicInteger testSucceeded = new AtomicInteger();

    private final AtomicInteger testIgnored = new AtomicInteger();

    private final AtomicInteger testFailed = new AtomicInteger();

    private final AtomicInteger testError = new AtomicInteger();

    public MockReporter()
    {
    }

    public MockReporter( ReporterConfiguration reporterConfiguration )
    {

    }

    public void runStarting()
    {
        events.add( RUN_STARTED );
    }

    public void runCompleted()
    {
        events.add( RUN_COMPLETED );
    }

    public void testSetStarting( ReportEntry report )
        throws ReporterException
    {
        events.add( SET_STARTED );
    }

    public void testSetCompleted( ReportEntry report )
        throws ReporterException
    {
        events.add( SET_COMPLETED );
    }

    public void testStarting( ReportEntry report )
    {
        events.add( TEST_STARTED );
    }

    public void testSucceeded( ReportEntry report )
    {
        events.add( TEST_COMPLETED );
        testSucceeded.incrementAndGet();

    }

    public void testError( ReportEntry report, String stdOut, String stdErr )
    {
        events.add( TEST_ERROR );
        testError.incrementAndGet();
    }

    public void testFailed( ReportEntry report, String stdOut, String stdErr )
    {
        events.add( TEST_FAILED );
        testFailed.incrementAndGet();
    }

    public void testSkipped( ReportEntry report )
    {
        events.add( TEST_SKIPPED );
        testIgnored.incrementAndGet();
    }

    public void reset()
    {
    }

    public void writeMessage( String message )
    {
    }

    public void writeFooter( String footer )
    {
    }

    public void writeDetailMessage( String message )
    {
    }

    public List<String> getEvents()
    {
        return events;
    }

    public int getTestSucceeded()
    {
        return testSucceeded.get();
    }

    public int getTestIgnored()
    {
        return testIgnored.get();
    }

    public int getTestFailed()
    {
        return testFailed.get();
    }

    public void writeConsoleMessage( String message )
    {
    }

    public void testError( ReportEntry report )
    {
    }

    public void testFailed( ReportEntry report )
    {
    }

    public void testAssumptionFailure( ReportEntry report )
    {
    }
}
