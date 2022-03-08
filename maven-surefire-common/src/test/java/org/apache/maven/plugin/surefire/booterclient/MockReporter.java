package org.apache.maven.plugin.surefire.booterclient;

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
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.report.TestSetReportEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Internal tests use only.
 */
public class MockReporter
        implements TestReportListener<TestOutputReportEntry>
{
    private final List<String> events = new ArrayList<>();

    private final List<Object> data = new ArrayList<>();

    public static final String SET_STARTING = "SET_STARTED";

    public static final String SET_COMPLETED = "SET_COMPLETED";

    public static final String TEST_STARTING = "TEST_STARTED";

    public static final String TEST_SUCCEEDED = "TEST_COMPLETED";

    public static final String TEST_FAILED = "TEST_FAILED";

    public static final String TEST_ERROR = "TEST_ERROR";

    public static final String TEST_SKIPPED = "TEST_SKIPPED";

    public static final String TEST_ASSUMPTION_FAIL = "TEST_ASSUMPTION_SKIPPED";

    public static final String CONSOLE_INFO = "CONSOLE_INFO";

    public static final String CONSOLE_WARN = "CONSOLE_WARN";

    public static final String CONSOLE_DEBUG = "CONSOLE_DEBUG";

    public static final String CONSOLE_ERR = "CONSOLE_ERR";

    public static final String STDOUT = "STDOUT";

    public static final String STDERR = "STDERR";

    private final AtomicInteger testSucceeded = new AtomicInteger();

    private final AtomicInteger testIgnored = new AtomicInteger();

    private final AtomicInteger testFailed = new AtomicInteger();

    @Override
    public void testSetStarting( TestSetReportEntry report )
    {
        events.add( SET_STARTING );
        data.add( report );
    }

    @Override
    public void testSetCompleted( TestSetReportEntry report )
    {
        events.add( SET_COMPLETED );
        data.add( report );
    }

    @Override
    public void testStarting( ReportEntry report )
    {
        events.add( TEST_STARTING );
        data.add( report );
    }

    @Override
    public void testSucceeded( ReportEntry report )
    {
        events.add( TEST_SUCCEEDED );
        testSucceeded.incrementAndGet();
        data.add( report );
    }

    @Override
    public void testError( ReportEntry report )
    {
        events.add( TEST_ERROR );
        data.add( report );
        testFailed.incrementAndGet();
    }

    @Override
    public void testFailed( ReportEntry report )
    {
        events.add( TEST_FAILED );
        data.add( report );
        testFailed.incrementAndGet();
    }

    @Override
    public void testSkipped( ReportEntry report )
    {
        events.add( TEST_SKIPPED );
        data.add( report );
        testIgnored.incrementAndGet();
    }

    @Override
    public void testExecutionSkippedByUser()
    {
    }

    public List<String> getEvents()
    {
        return events;
    }

    public List getData()
    {
        return data;
    }

    public String getFirstEvent()
    {
        return events.get( 0 );
    }

    public ReportEntry getFirstData()
    {
        return (ReportEntry) data.get( 0 );
    }

    @Override
    public void testAssumptionFailure( ReportEntry report )
    {
        events.add( TEST_ASSUMPTION_FAIL );
        data.add( report );
        testIgnored.incrementAndGet();
    }

    @Override
    public boolean isDebugEnabled()
    {
        return true;
    }

    @Override
    public void debug( String message )
    {
        events.add( CONSOLE_DEBUG );
        data.add( message );
    }

    @Override
    public boolean isInfoEnabled()
    {
        return true;
    }

    @Override
    public void info( String message )
    {
        events.add( CONSOLE_INFO );
        data.add( message );
    }

    @Override
    public boolean isWarnEnabled()
    {
        return true;
    }

    @Override
    public void warning( String message )
    {
        events.add( CONSOLE_WARN );
        data.add( message );
    }

    @Override
    public boolean isErrorEnabled()
    {
        return true;
    }

    @Override
    public void error( String message )
    {
        events.add( CONSOLE_ERR );
        data.add( message );
    }

    @Override
    public void error( String message, Throwable t )
    {
        error( message );
    }

    @Override
    public void error( Throwable t )
    {
        error( t.getLocalizedMessage() );
    }

    @Override
    public void writeTestOutput( TestOutputReportEntry reportEntry )
    {
        events.add( reportEntry.isStdOut() ? STDOUT : STDERR );
        String output = reportEntry.getLog();
        data.add( reportEntry.isNewLine() ? output + "\n" : output );
    }
}
