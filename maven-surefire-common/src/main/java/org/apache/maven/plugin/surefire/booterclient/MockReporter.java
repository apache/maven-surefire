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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;

/**
 * Internal use only
 */
public class MockReporter
    implements RunListener, ConsoleLogger, ConsoleOutputReceiver
{
    private final List<String> events = new ArrayList<String>();

    private final List<Object> data = new ArrayList<Object>();

    public static final String SET_STARTING = "SET_STARTED";

    public static final String SET_COMPLETED = "SET_COMPLETED";

    public static final String TEST_STARTING = "TEST_STARTED";

    public static final String TEST_SUCCEEDED = "TEST_COMPLETED";

    public static final String TEST_FAILED = "TEST_FAILED";

    public static final String TEST_ERROR = "TEST_ERROR";

    public static final String TEST_SKIPPED = "TEST_SKIPPED";

    public static final String TEST_ASSUMPTION_FAIL = "TEST_ASSUMPTION_SKIPPED";

    public static final String CONSOLE_OUTPUT = "CONSOLE_OUTPUT";

    public static final String STDOUT = "STDOUT";

    public static final String STDERR = "STDERR";

    private final AtomicInteger testSucceeded = new AtomicInteger();

    private final AtomicInteger testIgnored = new AtomicInteger();

    private final AtomicInteger testFailed = new AtomicInteger();

    public MockReporter()
    {
    }

    public void testSetStarting( ReportEntry report )
    {
        events.add( SET_STARTING );
        data.add( report );
    }

    public void testSetCompleted( ReportEntry report )
    {
        events.add( SET_COMPLETED );
        data.add( report );
    }

    public void testStarting( ReportEntry report )
    {
        events.add( TEST_STARTING );
        data.add( report );
    }

    public void testSucceeded( ReportEntry report )
    {
        events.add( TEST_SUCCEEDED );
        testSucceeded.incrementAndGet();
        data.add( report );
    }

    public void testError( ReportEntry report )
    {
        events.add( TEST_ERROR );
        data.add( report );
        testFailed.incrementAndGet();
    }

    public void testFailed( ReportEntry report )
    {
        events.add( TEST_FAILED );
        data.add( report );
        testFailed.incrementAndGet();
    }


    public void testSkipped( ReportEntry report )
    {
        events.add( TEST_SKIPPED );
        data.add( report );
        testIgnored.incrementAndGet();
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


    public void testAssumptionFailure( ReportEntry report )
    {
        events.add( TEST_ASSUMPTION_FAIL );
        data.add( report );
        testIgnored.incrementAndGet();

    }

    public void info( String message )
    {
        events.add( CONSOLE_OUTPUT );
        data.add( message );
    }

    public void writeTestOutput( byte[] buf, int off, int len, boolean stdout )
    {
        events.add( stdout ? STDOUT : STDERR );
        data.add( new String( buf, off, len ) );
    }
}
