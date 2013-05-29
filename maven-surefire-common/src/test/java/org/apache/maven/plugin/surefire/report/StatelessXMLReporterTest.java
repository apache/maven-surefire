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

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.apache.maven.plugin.surefire.booterclient.output.DeserializedStacktraceWriter;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.shared.utils.xml.Xpp3Dom;
import org.apache.maven.shared.utils.xml.Xpp3DomBuilder;
import org.apache.maven.surefire.report.LegacyPojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.report.StackTraceWriter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@SuppressWarnings( "ResultOfMethodCallIgnored" )
public class StatelessXMLReporterTest
    extends TestCase
{

    private StatelessXmlReporter reporter = new StatelessXmlReporter( new File( "." ), null, false );

    private ReportEntry reportEntry;

    private TestSetStats stats;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        reportEntry = new SimpleReportEntry( this.getClass().getName(), "StatelessXMLReporterTest",
                                             new LegacyPojoStackTraceWriter( "", "", new AssertionFailedError() ), 17 );
        stats = new TestSetStats( false, true );
    }

    public void testFileNameWithoutSuffix()
    {
        File reportDir = new File( "." );
        String testName = "org.apache.maven.plugin.surefire.report.StatelessXMLReporterTest";
        reportEntry = new SimpleReportEntry( this.getClass().getName(), testName, 12 );
        WrappedReportEntry testSetReportEntry =
            new WrappedReportEntry( reportEntry, ReportEntryType.success, 12, null, null );
        stats.testSucceeded( testSetReportEntry );
        reporter.testSetCompleted( testSetReportEntry, stats );

        File expectedReportFile = new File( reportDir, "TEST-" + testName + ".xml" );
        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                    expectedReportFile.exists() );
        //noinspection ResultOfMethodCallIgnored
        expectedReportFile.delete();
    }


    public void testAllFieldsSerialized()
        throws IOException
    {
        File reportDir = new File( "." );
        String testName = "aTestMethod";
        String testName2 = "bTestMethod";
        reportEntry = new SimpleReportEntry( this.getClass().getName(), testName, 12 );
        WrappedReportEntry testSetReportEntry =
            new WrappedReportEntry( reportEntry, ReportEntryType.success, 12, null, null );
        File expectedReportFile = new File( reportDir, "TEST-" + testName + ".xml" );

        stats.testSucceeded( testSetReportEntry );
        StackTraceWriter stackTraceWriter = new DeserializedStacktraceWriter( "A fud msg", "trimmed", "fail at foo" );
        DeferredFileOutputStream s = new DeferredFileOutputStream( 1000000, "fds", "fdx", new File( "" ) );
        s.write( "std-o\u00DCt<null>!".getBytes() );
        DeferredFileOutputStream s1 = new DeferredFileOutputStream( 1000000, "fds", "fdx", new File( "" ) );
        s1.write( "std-\u0115rr?&-&amp;&#163;".getBytes() );
        WrappedReportEntry t2 =
            new WrappedReportEntry( new SimpleReportEntry( Inner.class.getName(), testName2, stackTraceWriter, 13 ),
                                    ReportEntryType.error, 13, s, s1 );

        stats.testSucceeded( t2 );
        StatelessXmlReporter reporter = new StatelessXmlReporter( new File( "." ), null, false );
        reporter.testSetCompleted( testSetReportEntry, stats );

        Xpp3Dom testSuite = Xpp3DomBuilder.build( new FileReader( expectedReportFile ) );
        assertEquals( "testsuite", testSuite.getName() );
        Xpp3Dom properties = testSuite.getChild( "properties" );
        assertEquals( System.getProperties().size(), properties.getChildCount() );
        Xpp3Dom child = properties.getChild( 1 );
        assertFalse( StringUtils.isEmpty( child.getAttribute( "value" ) ) );
        assertFalse( StringUtils.isEmpty( child.getAttribute( "name" ) ) );

        Xpp3Dom[] testcase = testSuite.getChildren( "testcase" );
        Xpp3Dom tca = testcase[0];
        assertEquals( testName, tca.getAttribute( "name" ) ); // Hopefully same order on jdk5
        assertEquals( "0.012", tca.getAttribute( "time" ) );
        assertEquals( this.getClass().getName(), tca.getAttribute( "classname" ) );

        Xpp3Dom tcb = testcase[1];
        assertEquals( testName2, tcb.getAttribute( "name" ) );
        assertEquals( "0.013", tcb.getAttribute( "time" ) );
        assertEquals( Inner.class.getName(), tcb.getAttribute( "classname" ) );
        Xpp3Dom errorNode = tcb.getChild( "error" );
        assertNotNull( errorNode );
        assertEquals( "A fud msg", errorNode.getAttribute( "message" ) );
        assertEquals( "fail at foo", errorNode.getAttribute( "type" ) );
        assertEquals( "std-o\u00DCt<null>!", tcb.getChild( "system-out" ).getValue() );
        assertEquals( "std-\u0115rr?&-&amp;&#163;", tcb.getChild( "system-err" ).getValue() );

        expectedReportFile.delete();
    }

    class Inner
    {

    }

}
