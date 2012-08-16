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
    private static final String DUMMY_ERROR_SOURCE = "dummy error source";

    private static final String DUMMY_FAILURE_SOURCE = "dummy failure source";

    private static final String DUMMY_MESSAGE = "dummy message";

    public void testAddErrorSourceWithThrowableMessage()
    {
        RuntimeException throwable = new RuntimeException( DUMMY_MESSAGE );
        RunStatistics statistics = createRunStatisticsAndAddErrorSourceWithThrowable( throwable );
        assertRunStatisticsHasErrorSource( statistics, DUMMY_ERROR_SOURCE + ": " + DUMMY_MESSAGE );
    }

    public void testAddErrorSourceWithoutStackTraceWriter()
    {
        RunStatistics statistics = new RunStatistics();
        statistics.addErrorSource( DUMMY_ERROR_SOURCE, null );
        assertRunStatisticsHasErrorSource( statistics, DUMMY_ERROR_SOURCE );
    }

    public void testAddErrorSourceWithoutThrowable()
    {
        RunStatistics statistics = createRunStatisticsAndAddErrorSourceWithThrowable( null );
        assertRunStatisticsHasErrorSource( statistics, DUMMY_ERROR_SOURCE );
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
        assertRunStatisticsHasFailureSource( statistics, DUMMY_FAILURE_SOURCE + ": " + DUMMY_MESSAGE );
    }

    public void testAddFailureSourceWithoutStackTraceWriter()
    {
        RunStatistics statistics = new RunStatistics();
        statistics.addFailureSource( DUMMY_FAILURE_SOURCE, null );
        assertRunStatisticsHasFailureSource( statistics, DUMMY_FAILURE_SOURCE );
    }

    public void testAddFailureSourceWithoutThrowable()
    {
        RunStatistics statistics = createRunStatisticsAndAddFailureSourceWithThrowable( null );
        assertRunStatisticsHasFailureSource( statistics, DUMMY_FAILURE_SOURCE );
    }

    public void testAddFailureSourceWithThrowableWithoutMessage()
    {
        RuntimeException throwable = new RuntimeException();
        RunStatistics statistics = createRunStatisticsAndAddFailureSourceWithThrowable( throwable );
        assertRunStatisticsHasFailureSource( statistics, DUMMY_FAILURE_SOURCE );
    }

    private RunStatistics createRunStatisticsAndAddErrorSourceWithThrowable( Throwable throwable )
    {
        StackTraceWriter stackTraceWriter = new PojoStackTraceWriter( null, null, throwable );
        RunStatistics statistics = new RunStatistics();
        statistics.addErrorSource( DUMMY_ERROR_SOURCE, stackTraceWriter );

        return statistics;
    }

    private RunStatistics createRunStatisticsAndAddFailureSourceWithThrowable( Throwable throwable )
    {
        StackTraceWriter stackTraceWriter = new PojoStackTraceWriter( null, null, throwable );
        RunStatistics statistics = new RunStatistics();
        statistics.addFailureSource( DUMMY_FAILURE_SOURCE, stackTraceWriter );

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
