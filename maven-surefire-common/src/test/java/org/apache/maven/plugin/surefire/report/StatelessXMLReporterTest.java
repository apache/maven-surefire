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

public class StatelessXMLReporterTest
    extends TestCase
{

    private StatelessXmlReporter reporter = new StatelessXmlReporter( new File( "." ), null, false );

    private ReportEntry reportEntry;

    private String message;

    private TestSetStats stats;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        message = "junit.framework.AssertionFailedError";
        reportEntry = new SimpleReportEntry( this.getClass().getName(), "StatelessXMLReporterTest",
                                             new PojoStackTraceWriter( "", "", new AssertionFailedError() ), 17 );
        stats = new TestSetStats( false, true );
    }

    public void testFileNameWithoutSuffix()
    {
        File reportDir = new File( "." );
        String testName = "org.apache.maven.plugin.surefire.report.StatelessXMLReporterTest";
        reportEntry = new SimpleReportEntry( this.getClass().getName(), testName, 12 );
        WrappedReportEntry testSetReportEntry = new WrappedReportEntry( reportEntry, ReportEntryType.success, 12, null, null );
        stats.testSucceeded( testSetReportEntry );
        reporter.testSetCompleted( testSetReportEntry, stats );

        File expectedReportFile = new File( reportDir, "TEST-" + testName + ".xml" );
        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                    expectedReportFile.exists() );
        //noinspection ResultOfMethodCallIgnored
        expectedReportFile.delete();
    }

}
