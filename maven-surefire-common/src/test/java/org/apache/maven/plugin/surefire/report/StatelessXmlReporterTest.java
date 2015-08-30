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

import org.apache.maven.plugin.surefire.booterclient.output.DeserializedStacktraceWriter;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.shared.utils.xml.Xpp3Dom;
import org.apache.maven.shared.utils.xml.Xpp3DomBuilder;
import org.apache.maven.surefire.report.LegacyPojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.report.StackTraceWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings( "ResultOfMethodCallIgnored" )
public class StatelessXmlReporterTest
    extends TestCase
{

    private StatelessXmlReporter reporter = new StatelessXmlReporter( new File( "." ), null, false, 0, 0,
                    Collections.synchronizedMap( new HashMap<String, Map<String, List<WrappedReportEntry>>>() ) );

    private ReportEntry reportEntry;

    private TestSetStats stats;

    private TestSetStats rerunStats;

    private File expectedReportFile;

    private final static String TEST_ONE = "aTestMethod";
    private final static String TEST_TWO = "bTestMethod";
    private final static String TEST_THREE = "cTestMethod";

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        reportEntry = new SimpleReportEntry( this.getClass().getName(), "StatelessXMLReporterTest",
                                             new LegacyPojoStackTraceWriter( "", "", new AssertionFailedError() ), 17 );
        stats = new TestSetStats( false, true );
        rerunStats = new TestSetStats( false, true );
        reporter.cleanTestHistoryMap();
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        if ( expectedReportFile != null )
        {
            expectedReportFile.delete();
        }
    }

    public void testFileNameWithoutSuffix()
    {
        File reportDir = new File( "." );
        String testName = "org.apache.maven.plugin.surefire.report.StatelessXMLReporterTest";
        reportEntry = new SimpleReportEntry( this.getClass().getName(), testName, 12 );
        WrappedReportEntry testSetReportEntry =
            new WrappedReportEntry( reportEntry, ReportEntryType.SUCCESS, 12, null, null );
        stats.testSucceeded( testSetReportEntry );
        reporter.testSetCompleted( testSetReportEntry, stats );

        expectedReportFile = new File( reportDir, "TEST-" + testName + ".xml" );
        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                    expectedReportFile.exists() );
    }


    public void testAllFieldsSerialized()
        throws IOException
    {
        File reportDir = new File( "." );

        reportEntry = new SimpleReportEntry( this.getClass().getName(), TEST_ONE, 12 );
        WrappedReportEntry testSetReportEntry =
            new WrappedReportEntry( reportEntry, ReportEntryType.SUCCESS, 12, null, null );
        expectedReportFile = new File( reportDir, "TEST-" + TEST_ONE + ".xml" );

        stats.testSucceeded( testSetReportEntry );
        StackTraceWriter stackTraceWriter = new DeserializedStacktraceWriter( "A fud msg", "trimmed", "fail at foo" );
        Utf8RecodingDeferredFileOutputStream stdOut = new Utf8RecodingDeferredFileOutputStream( "fds" );
        String stdOutPrefix;
        String stdErrPrefix;
        if ( defaultCharsetSupportsSpecialChar() )
        {
            stdErrPrefix = "std-\u0115rr";
            stdOutPrefix = "st]]>d-o\u00DCt";
        }
        else
        {
            stdErrPrefix = "std-err";
            stdOutPrefix = "st]]>d-out";
        }

        byte[] stdOutBytes = (stdOutPrefix + "<null>!\u0020\u0000\u001F").getBytes();
        stdOut.write( stdOutBytes, 0, stdOutBytes.length );

        Utf8RecodingDeferredFileOutputStream stdErr = new Utf8RecodingDeferredFileOutputStream( "fds" );


        byte[] stdErrBytes = (stdErrPrefix + "?&-&amp;&#163;\u0020\u0000\u001F").getBytes();
        stdErr.write( stdErrBytes, 0, stdErrBytes.length );
        WrappedReportEntry t2 =
            new WrappedReportEntry( new SimpleReportEntry( Inner.class.getName(), TEST_TWO, stackTraceWriter, 13 ),
                                    ReportEntryType.ERROR, 13, stdOut, stdErr );

        stats.testSucceeded( t2 );
        StatelessXmlReporter reporter = new StatelessXmlReporter( new File( "." ), null, false, 0, 0,
                        Collections.synchronizedMap( new HashMap<String, Map<String, List<WrappedReportEntry>>>() ) );
        reporter.testSetCompleted( testSetReportEntry, stats );

        FileInputStream fileInputStream = new FileInputStream( expectedReportFile );

        Xpp3Dom testSuite = Xpp3DomBuilder.build( new InputStreamReader( fileInputStream, "UTF-8") );
        assertEquals( "testsuite", testSuite.getName() );
        Xpp3Dom properties = testSuite.getChild( "properties" );
        assertEquals( System.getProperties().size(), properties.getChildCount() );
        Xpp3Dom child = properties.getChild( 1 );
        assertFalse( StringUtils.isEmpty( child.getAttribute( "value" ) ) );
        assertFalse( StringUtils.isEmpty( child.getAttribute( "name" ) ) );

        Xpp3Dom[] testcase = testSuite.getChildren( "testcase" );
        Xpp3Dom tca = testcase[0];
        assertEquals( TEST_ONE, tca.getAttribute( "name" ) ); // Hopefully same order on jdk5
        assertEquals( "0.012", tca.getAttribute( "time" ) );
        assertEquals( this.getClass().getName(), tca.getAttribute( "classname" ) );

        Xpp3Dom tcb = testcase[1];
        assertEquals( TEST_TWO, tcb.getAttribute( "name" ) );
        assertEquals( "0.013", tcb.getAttribute( "time" ) );
        assertEquals( Inner.class.getName(), tcb.getAttribute( "classname" ) );
        Xpp3Dom errorNode = tcb.getChild( "error" );
        assertNotNull( errorNode );
        assertEquals( "A fud msg", errorNode.getAttribute( "message" ) );
        assertEquals( "fail at foo", errorNode.getAttribute( "type" ) );
        assertEquals( stdOutPrefix + "<null>! &amp#0;&amp#31;", tcb.getChild( "system-out" ).getValue() );


        assertEquals( stdErrPrefix + "?&-&amp;&#163; &amp#0;&amp#31;", tcb.getChild( "system-err" ).getValue() );
    }

    public void testOutputRerunFlakyFailure()
        throws IOException
    {
        File reportDir = new File( "." );
        reportEntry = new SimpleReportEntry( this.getClass().getName(), TEST_ONE, 12 );

        WrappedReportEntry testSetReportEntry =
            new WrappedReportEntry( reportEntry, ReportEntryType.SUCCESS, 12, null, null );
        expectedReportFile = new File( reportDir, "TEST-" + TEST_ONE + ".xml" );

        stats.testSucceeded( testSetReportEntry );
        StackTraceWriter stackTraceWriterOne = new DeserializedStacktraceWriter( "A fud msg", "trimmed",
                                                                                 "fail at foo" );
        StackTraceWriter stackTraceWriterTwo =
            new DeserializedStacktraceWriter( "A fud msg two", "trimmed two", "fail at foo two" );

        String firstRunOut = "first run out";
        String firstRunErr = "first run err";
        String secondRunOut = "second run out";
        String secondRunErr = "second run err";

        WrappedReportEntry testTwoFirstError =
            new WrappedReportEntry( new SimpleReportEntry( Inner.class.getName(), TEST_TWO, stackTraceWriterOne, 5 ),
                                    ReportEntryType.ERROR, 5, createStdOutput( firstRunOut ),
                                    createStdOutput( firstRunErr ) );

        WrappedReportEntry testTwoSecondError =
            new WrappedReportEntry( new SimpleReportEntry( Inner.class.getName(), TEST_TWO, stackTraceWriterTwo, 13 ),
                                    ReportEntryType.ERROR, 13, createStdOutput( secondRunOut ),
                                    createStdOutput( secondRunErr ) );

        WrappedReportEntry testThreeFirstRun =
            new WrappedReportEntry( new SimpleReportEntry( Inner.class.getName(), TEST_THREE, stackTraceWriterOne, 13 ),
                                    ReportEntryType.FAILURE, 13, createStdOutput( firstRunOut ),
                                    createStdOutput( firstRunErr ) );

        WrappedReportEntry testThreeSecondRun =
            new WrappedReportEntry( new SimpleReportEntry( Inner.class.getName(), TEST_THREE, stackTraceWriterTwo, 2 ),
                                    ReportEntryType.SUCCESS, 2, createStdOutput( secondRunOut ),
                                    createStdOutput( secondRunErr ) );

        stats.testSucceeded( testTwoFirstError );
        stats.testSucceeded( testThreeFirstRun );
        rerunStats.testSucceeded( testTwoSecondError );
        rerunStats.testSucceeded( testThreeSecondRun );

        StatelessXmlReporter reporter =
            new StatelessXmlReporter(
                                      new File( "." ),
                                      null,
                                      false,
                                      1, 0,
                                      new HashMap<String, Map<String, List<WrappedReportEntry>>>() );

        reporter.testSetCompleted( testSetReportEntry, stats );
        reporter.testSetCompleted( testSetReportEntry, rerunStats );

        FileInputStream fileInputStream = new FileInputStream( expectedReportFile );

        Xpp3Dom testSuite = Xpp3DomBuilder.build( new InputStreamReader( fileInputStream, "UTF-8" ) );
        assertEquals( "testsuite", testSuite.getName() );
        // 0.019 = 0.012 + 0.005 +0.002
        assertEquals( "0.019", testSuite.getAttribute( "time" ) );
        Xpp3Dom properties = testSuite.getChild( "properties" );
        assertEquals( System.getProperties().size(), properties.getChildCount() );
        Xpp3Dom child = properties.getChild( 1 );
        assertFalse( StringUtils.isEmpty( child.getAttribute( "value" ) ) );
        assertFalse( StringUtils.isEmpty( child.getAttribute( "name" ) ) );

        Xpp3Dom[] testcase = testSuite.getChildren( "testcase" );
        Xpp3Dom testCaseOne = testcase[0];
        assertEquals( TEST_ONE, testCaseOne.getAttribute( "name" ) );
        assertEquals( "0.012", testCaseOne.getAttribute( "time" ) );
        assertEquals( this.getClass().getName(), testCaseOne.getAttribute( "classname" ) );

        Xpp3Dom testCaseTwo = testcase[1];
        assertEquals( TEST_TWO, testCaseTwo.getAttribute( "name" ) );
        // Run time for a rerun failing test is the run time of the first run
        assertEquals( "0.005", testCaseTwo.getAttribute( "time" ) );
        assertEquals( Inner.class.getName(), testCaseTwo.getAttribute( "classname" ) );
        Xpp3Dom errorNode = testCaseTwo.getChild( "error" );
        Xpp3Dom rerunErrorNode = testCaseTwo.getChild( "rerunError" );
        assertNotNull( errorNode );
        assertNotNull( rerunErrorNode );

        assertEquals( "A fud msg", errorNode.getAttribute( "message" ) );
        assertEquals( "fail at foo", errorNode.getAttribute( "type" ) );

        // Check rerun error node contains all the information
        assertEquals( firstRunOut, testCaseTwo.getChild( "system-out" ).getValue() );
        assertEquals( firstRunErr, testCaseTwo.getChild( "system-err" ).getValue() );
        assertEquals( secondRunOut, rerunErrorNode.getChild( "system-out" ).getValue() );
        assertEquals( secondRunErr, rerunErrorNode.getChild( "system-err" ).getValue() );
        assertEquals( "A fud msg two", rerunErrorNode.getAttribute( "message" ) );
        assertEquals( "fail at foo two", rerunErrorNode.getAttribute( "type" ) );

        // Check flaky failure node
        Xpp3Dom testCaseThree = testcase[2];
        assertEquals( TEST_THREE, testCaseThree.getAttribute( "name" ) );
        // Run time for a flaky test is the run time of the first successful run
        assertEquals( "0.002", testCaseThree.getAttribute( "time" ) );
        assertEquals( Inner.class.getName(), testCaseThree.getAttribute( "classname" ) );
        Xpp3Dom flakyFailureNode = testCaseThree.getChild( "flakyFailure" );
        assertNotNull( flakyFailureNode );
        assertEquals( firstRunOut, flakyFailureNode.getChild( "system-out" ).getValue() );
        assertEquals( firstRunErr, flakyFailureNode.getChild( "system-err" ).getValue() );
        // system-out and system-err should not be present for flaky failures
        assertNull( testCaseThree.getChild( "system-out" ) );
        assertNull( testCaseThree.getChild( "system-err" ) );
    }

    private boolean defaultCharsetSupportsSpecialChar()
    {
        // some charsets are not able to deal with \u0115 on both ways of the conversion
        return "\u0115\u00DC".equals( new String( "\u0115\u00DC".getBytes() ) );
    }

    private Utf8RecodingDeferredFileOutputStream createStdOutput( String content )
        throws IOException
    {
        Utf8RecodingDeferredFileOutputStream stdOut = new Utf8RecodingDeferredFileOutputStream( "fds2" );
        stdOut.write( content.getBytes(), 0, content.length() );
        return stdOut;
    }

    class Inner
    {

    }

}
