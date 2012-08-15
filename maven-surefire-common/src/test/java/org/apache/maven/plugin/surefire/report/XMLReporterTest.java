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

import java.io.File;

import org.apache.maven.surefire.report.PojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public class XMLReporterTest
    extends TestCase
{

    private XMLReporter reporter;

    private ReportEntry reportEntry;

    private String message;

    private TestSetStats stats;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        reporter = new XMLReporter( new File( "." ) );
        message = "junit.framework.AssertionFailedError";
        reportEntry = new SimpleReportEntry( this.getClass().getName(), "XMLReporterTest",
                                             new PojoStackTraceWriter( "", "", new AssertionFailedError() ), 17 );
        stats = new TestSetStats( false );
    }

    /*
     * Test method for 'org.codehaus.surefire.report.XMLReporter.testError(ReportEntry, String, String)'
     */
    public void testTestError()
    {
        reporter.testError( reportEntry, "", "", stats );
        assertResult( reporter, message );
    }

    /*
     * Test method for 'org.codehaus.surefire.report.XMLReporter.testFailed(ReportEntry, String, String)'
     */
    public void testTestFailed()
    {
        reporter.testError( reportEntry, "", "", stats );
        assertResult( reporter, message );
    }

    private void assertResult( XMLReporter reporter, String message )
    {
        Xpp3Dom result = (Xpp3Dom) reporter.getResults().next();
        Xpp3Dom child = result.getChild( "error" );
        assertEquals( message, child.getAttribute( "type" ) );
    }

    /*
     * Test method for 'org.codehaus.surefire.report.XMLReporter.testSetCompleted(ReportEntry report)'
     */
    public void testFileNameWithoutSuffix()
    {
        File reportDir = new File( "." );
        String testName = "org.apache.maven.plugin.surefire.report.XMLReporterTest";
        reportEntry = new SimpleReportEntry( this.getClass().getName(), testName, 12 );
        reporter = new XMLReporter( reportDir, null );
        reporter.testSetCompleted( reportEntry, stats );

        File expectedReportFile = new File( reportDir, "TEST-" + testName + ".xml" );
        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                    expectedReportFile.exists() );
        //noinspection ResultOfMethodCallIgnored
        expectedReportFile.delete();
    }

    /*
     * Test method for 'org.codehaus.surefire.report.XMLReporter.testSetCompleted(ReportEntry report)'
     */
    public void testFileNameWithSuffix()
    {
        File reportDir = new File( "target" );
        String testName = "org.apache.maven.plugin.surefire.report.XMLReporterTest";
        String suffixText = "sampleSuffixText";
        reportEntry = new SimpleReportEntry( this.getClass().getName(), testName );
        reporter = new XMLReporter( reportDir, suffixText );
        reporter.testSetCompleted( reportEntry, stats );

        File expectedReportFile = new File( reportDir, "TEST-" + testName + "-" + suffixText + ".xml" );
        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                    expectedReportFile.exists() );
        //noinspection ResultOfMethodCallIgnored
        expectedReportFile.delete();
    }

    public void testGetReportNameWithParams()
        throws Exception
    {
        String category = "[0] 1\u002C 2\u002C 3 (testSum)(surefire.testcase.JunitParamsTest)";
        ReportEntry reportEntry = new SimpleReportEntry( "fud", category );
        final String reportName = XMLReporter.getReportName( reportEntry );
        assertEquals( "[0] 1, 2, 3 (testSum)", reportName );
    }

    public void testClassNameOnly()
        throws Exception
    {
        String category = "surefire.testcase.JunitParamsTest";
        ReportEntry reportEntry = new SimpleReportEntry( "fud", category );
        final String reportName = XMLReporter.getReportName( reportEntry );
        assertEquals( "surefire.testcase.JunitParamsTest", reportName );
    }

    public void testRegular()
    {
        ReportEntry reportEntry = new SimpleReportEntry( "fud", "testSum(surefire.testcase.NonJunitParamsTest)" );
        final String reportName = XMLReporter.getReportName( reportEntry );
        assertEquals( "testSum", reportName );
    }


}
