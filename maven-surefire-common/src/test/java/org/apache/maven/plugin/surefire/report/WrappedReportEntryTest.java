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
import org.apache.maven.surefire.report.SimpleReportEntry;

import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class WrappedReportEntryTest
    extends TestCase
{

    public void testClassNameOnly()
        throws Exception
    {
        String category = "surefire.testcase.JunitParamsTest";
        WrappedReportEntry wr =
            new WrappedReportEntry( new SimpleReportEntry( "fud", category ), null, 12, null, null );
        final String reportName = wr.getReportName();
        assertEquals( "surefire.testcase.JunitParamsTest", reportName );
    }

    public void testRegular()
    {
        ReportEntry reportEntry = new SimpleReportEntry( "fud", "testSum(surefire.testcase.NonJunitParamsTest)" );
        WrappedReportEntry wr = new WrappedReportEntry( reportEntry, null, 12, null, null );
        final String reportName = wr.getReportName();
        assertEquals( "testSum", reportName );
    }

    public void testGetReportNameWithParams()
        throws Exception
    {
        String category = "[0] 1\u002C 2\u002C 3 (testSum)(surefire.testcase.JunitParamsTest)";
        ReportEntry reportEntry = new SimpleReportEntry( "fud", category );
        WrappedReportEntry wr = new WrappedReportEntry( reportEntry, null, 12, null, null );
        final String reportName = wr.getReportName();
        assertEquals( "[0] 1, 2, 3 (testSum)", reportName );
    }

    public void testElapsed()
        throws Exception
    {
        String category = "[0] 1\u002C 2\u002C 3 (testSum)(surefire.testcase.JunitParamsTest)";
        ReportEntry reportEntry = new SimpleReportEntry( "fud", category );
        WrappedReportEntry wr = new WrappedReportEntry( reportEntry, null, 12, null, null );
        String elapsedTimeSummary = wr.getElapsedTimeSummary();
        assertEquals( "[0] 1, 2, 3 (testSum)(surefire.testcase.JunitParamsTest)  Time elapsed: 0.012 sec",
                      elapsedTimeSummary );
    }


}
