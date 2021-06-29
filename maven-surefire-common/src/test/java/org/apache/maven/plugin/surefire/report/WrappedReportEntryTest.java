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

import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.SimpleReportEntry;

import junit.framework.TestCase;

import static org.apache.maven.plugin.surefire.report.ReportEntryType.ERROR;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.FAILURE;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.SKIPPED;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.SUCCESS;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;

/**
 * @author Kristian Rosenvold
 */
public class WrappedReportEntryTest
    extends TestCase
{
    public void testClassNameOnly()
    {
        String className = "surefire.testcase.JunitParamsTest";
        WrappedReportEntry wr =
            new WrappedReportEntry( new SimpleReportEntry( NORMAL_RUN, 1L, className, null, null, null ),
                SUCCESS, 12, null, null );
        final String reportName = wr.getReportSourceName();
        assertEquals( "surefire.testcase.JunitParamsTest.null", wr.getClassMethodName() );
        assertEquals( "surefire.testcase.JunitParamsTest", reportName );
        assertTrue( wr.isSucceeded() );
        assertFalse( wr.isErrorOrFailure() );
        assertFalse( wr.isSkipped() );
    }

    public void testRegular()
    {
        ReportEntry reportEntry =
            new SimpleReportEntry( NORMAL_RUN, 1L, "surefire.testcase.JunitParamsTest", null, "testSum", null );
        WrappedReportEntry wr = new WrappedReportEntry( reportEntry, null, 12, null, null );
        assertEquals( "surefire.testcase.JunitParamsTest.testSum", wr.getClassMethodName() );
        assertEquals( "surefire.testcase.JunitParamsTest", wr.getReportSourceName() );
        assertEquals( "surefire.testcase.JunitParamsTest", wr.getReportSourceName( "" ) );
        assertEquals( "surefire.testcase.JunitParamsTest(BDD)", wr.getReportSourceName( "BDD" ) );
        assertEquals( "surefire.testcase.JunitParamsTest", wr.getSourceName( "" ) );
        assertEquals( "surefire.testcase.JunitParamsTest(BDD)", wr.getSourceName( "BDD" ) );
        assertEquals( "testSum", wr.getReportName() );
        assertFalse( wr.isSucceeded() );
        assertFalse( wr.isErrorOrFailure() );
        assertFalse( wr.isSkipped() );
        assertTrue( wr.getSystemProperties().isEmpty() );
        assertNull( wr.getGroup() );
        assertEquals( "surefire.testcase.JunitParamsTest", wr.getNameWithGroup() );
    }

    public void testDisplayNames()
    {
        ReportEntry reportEntry = new SimpleReportEntry( NORMAL_RUN, 0L,
            "surefire.testcase.JunitParamsTest", "dn1", "testSum", "dn2", "exception" );
        WrappedReportEntry wr = new WrappedReportEntry( reportEntry, ERROR, 12, null, null );
        assertEquals( "surefire.testcase.JunitParamsTest.testSum", wr.getClassMethodName() );
        assertEquals( "dn1", wr.getReportSourceName() );
        assertEquals( "dn1(BDD)", wr.getReportSourceName( "BDD" ) );
        assertEquals( "surefire.testcase.JunitParamsTest(BDD)", wr.getSourceName( "BDD" ) );
        assertEquals( "dn2", wr.getReportName() );
        assertFalse( wr.isSucceeded() );
        assertTrue( wr.isErrorOrFailure() );
        assertFalse( wr.isSkipped() );
        assertNull( wr.getStackTraceWriter() );
        assertEquals( "surefire.testcase.JunitParamsTest.testSum  Time elapsed: 0.012 s",
                wr.getElapsedTimeSummary() );
        assertEquals( "surefire.testcase.JunitParamsTest.testSum  Time elapsed: 0.012 s  <<< ERROR!",
                wr.getOutput( false ) );
        assertEquals( "exception", wr.getMessage() );
    }

    public void testEqualDisplayNames()
    {
        ReportEntry reportEntry = new SimpleReportEntry( NORMAL_RUN, 1L, "surefire.testcase.JunitParamsTest",
                "surefire.testcase.JunitParamsTest", "testSum", "testSum" );
        WrappedReportEntry wr = new WrappedReportEntry( reportEntry, FAILURE, 12, null, null );
        assertEquals( "surefire.testcase.JunitParamsTest", wr.getReportSourceName() );
        assertEquals( "surefire.testcase.JunitParamsTest(BDD)", wr.getReportSourceName( "BDD" ) );
        assertEquals( "testSum", wr.getReportName() );
        assertFalse( wr.isSucceeded() );
        assertTrue( wr.isErrorOrFailure() );
        assertFalse( wr.isSkipped() );
    }

    public void testGetReportNameWithParams()
    {
        String className = "[0] 1\u002C 2\u002C 3 (testSum)";
        ReportEntry reportEntry = new SimpleReportEntry( NORMAL_RUN, 1L, className, null, null, null );
        WrappedReportEntry wr = new WrappedReportEntry( reportEntry, SKIPPED, 12, null, null );
        final String reportName = wr.getReportSourceName();
        assertEquals( "[0] 1, 2, 3 (testSum)", reportName );
        assertFalse( wr.isSucceeded() );
        assertFalse ( wr.isErrorOrFailure() );
        assertTrue( wr.isSkipped() );
    }

    public void testElapsed()
    {
        String className = "[0] 1\u002C 2\u002C 3 (testSum)";
        ReportEntry reportEntry = new SimpleReportEntry( NORMAL_RUN, 1L, className, null, null, null );
        WrappedReportEntry wr = new WrappedReportEntry( reportEntry, null, 12, null, null );
        String elapsedTimeSummary = wr.getElapsedTimeSummary();
        assertEquals( "[0] 1, 2, 3 (testSum)  Time elapsed: 0.012 s",
                      elapsedTimeSummary );
    }
}
