package org.apache.maven.surefire.report;

import java.util.Collection;

import junit.framework.TestCase;

/*
 * Copyright 2002-2009 the original author or authors.
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
 *
 */

public class RunStatisticsTest
    extends TestCase
{
    private static final String Method = "AClass#AMethod";

    private static final String DUMMY_ERROR_SOURCE = Method + " RuntimeException";

    private static final String DUMMY_FAILURE_SOURCE = "dummy failure source";

    private static final String DUMMY_MESSAGE = "foo";

    public void testAddErrorSourceWithThrowableMessage()
    {
        RuntimeException throwable = new RuntimeException( DUMMY_MESSAGE );
        RunStatistics statistics = createRunStatisticsAndAddErrorSourceWithThrowable( throwable );
        assertRunStatisticsHasErrorSource( statistics, DUMMY_ERROR_SOURCE + " " + DUMMY_MESSAGE );
    }

    public void testAddErrorSourceWithoutThrowable()
    {
        RunStatistics statistics = createRunStatisticsAndAddErrorSourceWithThrowable( null );
        assertRunStatisticsHasErrorSource( statistics, Method );
    }

    public void testAddErrorSourceWithThrowableWithoutMessage()
    {
        RuntimeException throwable = new RuntimeException();
        RunStatistics statistics = createRunStatisticsAndAddErrorSourceWithThrowable( throwable );
        assertRunStatisticsHasErrorSource( statistics, DUMMY_ERROR_SOURCE );
    }

    public void testAddFailureSourceWithThrowableMessage()
    {
        RuntimeException throwable = new RuntimeException( DUMMY_MESSAGE );
        RunStatistics statistics = createRunStatisticsAndAddFailureSourceWithThrowable( throwable );
        assertRunStatisticsHasFailureSource( statistics, DUMMY_ERROR_SOURCE + " " + DUMMY_MESSAGE );
    }

    public void testAddFailureSourceWithoutThrowable()
    {
        RunStatistics statistics = createRunStatisticsAndAddFailureSourceWithThrowable( null );
        assertRunStatisticsHasFailureSource( statistics, Method );
    }

    public void testAddFailureSourceWithThrowableWithoutMessage()
    {
        RuntimeException throwable = new RuntimeException();
        RunStatistics statistics = createRunStatisticsAndAddFailureSourceWithThrowable( throwable );
        assertRunStatisticsHasFailureSource( statistics, DUMMY_ERROR_SOURCE );
    }

    private RunStatistics createRunStatisticsAndAddErrorSourceWithThrowable( Throwable throwable )
    {
        StackTraceWriter stackTraceWriter = new LegacyPojoStackTraceWriter( "AClass", "AMethod", throwable );
        RunStatistics statistics = new RunStatistics();
        statistics.addErrorSource( stackTraceWriter );

        return statistics;
    }

    private RunStatistics createRunStatisticsAndAddFailureSourceWithThrowable( Throwable throwable )
    {
        StackTraceWriter stackTraceWriter = new LegacyPojoStackTraceWriter( "AClass", "AMethod", throwable );
        RunStatistics statistics = new RunStatistics();
        statistics.addFailureSource( stackTraceWriter );

        return statistics;
    }

    private void assertRunStatisticsHasErrorSource( RunStatistics statistics, String expectedErrorSource )
    {
        Collection errorSources = statistics.getErrorSources();
        assertNotNull( "No error sources.", errorSources );
        assertEquals( "Wrong number of error sources.", 1, errorSources.size() );
        assertEquals( "Wrong error sources.", expectedErrorSource, errorSources.iterator().next() );
    }

    private void assertRunStatisticsHasFailureSource( RunStatistics statistics, String expectedFailureSource )
    {
        Collection failureSources = statistics.getFailureSources();
        assertNotNull( "No failure sources.", failureSources );
        assertEquals( "Wrong number of failure sources.", 1, failureSources.size() );
        assertEquals( "Wrong failure sources.", expectedFailureSource, failureSources.iterator().next() );
    }
}
