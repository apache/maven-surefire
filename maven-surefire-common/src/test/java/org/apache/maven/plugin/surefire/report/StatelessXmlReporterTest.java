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

import junit.framework.TestCase;
import org.apache.maven.plugin.surefire.booterclient.output.DeserializedStacktraceWriter;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.SimpleReportEntry;
import org.apache.maven.surefire.api.report.StackTraceWriter;
import org.apache.maven.surefire.shared.utils.xml.Xpp3Dom;
import org.apache.maven.surefire.shared.utils.xml.Xpp3DomBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Deque;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllLines;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.ERROR;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.SKIPPED;
import static org.apache.maven.plugin.surefire.report.ReportEntryType.SUCCESS;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.report.RunMode.RERUN_TEST_AFTER_FAILURE;
import static org.apache.maven.surefire.api.util.internal.ObjectUtils.systemProps;
import static org.apache.maven.surefire.shared.utils.StringUtils.isEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.reflect.Whitebox.getInternalState;
import static org.powermock.reflect.Whitebox.invokeMethod;
import static org.powermock.reflect.Whitebox.setInternalState;
import static org.apache.maven.surefire.api.util.internal.StringUtils.NL;

/**
 *
 */
@SuppressWarnings( { "ResultOfMethodCallIgnored", "checkstyle:magicnumber" } )
public class StatelessXmlReporterTest
        extends TestCase
{
    private static final String XSD =
            "https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report-3.0.xsd";
    private static final String TEST_ONE = "aTestMethod";
    private static final String TEST_TWO = "bTestMethod";
    private static final String TEST_THREE = "cTestMethod";
    private static final AtomicInteger FOLDER_POSTFIX = new AtomicInteger();

    private TestSetStats stats;
    private TestSetStats rerunStats;
    private File expectedReportFile;
    private File reportDir;

    @Override
    protected void setUp()
            throws Exception
    {
        stats = new TestSetStats( false, true );
        rerunStats = new TestSetStats( false, true );

        File basedir = new File( "." );
        File target = new File( basedir.getCanonicalFile(), "target" );
        target.mkdir();
        String reportRelDir = getClass().getSimpleName() + "-" + FOLDER_POSTFIX.incrementAndGet();
        reportDir = new File( target, reportRelDir );
        reportDir.mkdir();
    }

    @Override
    protected void tearDown()
    {
        if ( expectedReportFile != null )
        {
            expectedReportFile.delete();
        }
    }

    public void testFileNameWithoutSuffix()
    {
        StatelessXmlReporter reporter =
                new StatelessXmlReporter( reportDir, null, false, 0,
                        new ConcurrentHashMap<String, Deque<WrappedReportEntry>>(), XSD, "3.0",
                        false, false, false, false );
        reporter.cleanTestHistoryMap();

        ReportEntry reportEntry = new SimpleReportEntry( NORMAL_RUN, 0L,
            getClass().getName(), null, getClass().getName(), null, 12 );
        WrappedReportEntry testSetReportEntry = new WrappedReportEntry( reportEntry, ReportEntryType.SUCCESS,
                12, null, null, systemProps() );
        stats.testSucceeded( testSetReportEntry );
        reporter.testSetCompleted( testSetReportEntry, stats );

        expectedReportFile = new File( reportDir, "TEST-" + getClass().getName() + ".xml" );
        assertTrue( "Report file (" + expectedReportFile.getAbsolutePath() + ") doesn't exist",
                expectedReportFile.exists() );
    }


    public void testAllFieldsSerialized()
            throws IOException
    {
        ReportEntry reportEntry = new SimpleReportEntry( NORMAL_RUN, 0L,
            getClass().getName(), null, TEST_ONE, null, 12 );
        WrappedReportEntry testSetReportEntry =
                new WrappedReportEntry( reportEntry, SUCCESS, 12, null, null, systemProps() );
        expectedReportFile = new File( reportDir, "TEST-" + getClass().getName() + ".xml" );

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

        stdOut.write( stdOutPrefix + "<null>!\u0020\u0000\u001F", false );

        Utf8RecodingDeferredFileOutputStream stdErr = new Utf8RecodingDeferredFileOutputStream( "fds" );

        stdErr.write( stdErrPrefix + "?&-&amp;&#163;\u0020\u0000\u001F", false );
        WrappedReportEntry t2 = new WrappedReportEntry( new SimpleReportEntry( NORMAL_RUN, 0L,
            getClass().getName(), null, TEST_TWO, null, stackTraceWriter, 13 ),
            ReportEntryType.ERROR, 13, stdOut, stdErr );

        stats.testSucceeded( t2 );
        StatelessXmlReporter reporter = new StatelessXmlReporter( reportDir, null, false, 0,
                new ConcurrentHashMap<String, Deque<WrappedReportEntry>>(), XSD, "3.0", false, false, false, false );
        reporter.testSetCompleted( testSetReportEntry, stats );

        FileInputStream fileInputStream = new FileInputStream( expectedReportFile );

        Xpp3Dom testSuite = Xpp3DomBuilder.build( new InputStreamReader( fileInputStream, UTF_8 ) );
        assertEquals( "testsuite", testSuite.getName() );
        Xpp3Dom properties = testSuite.getChild( "properties" );
        assertEquals( System.getProperties().size(), properties.getChildCount() );
        Xpp3Dom child = properties.getChild( 1 );
        assertFalse( isEmpty( child.getAttribute( "value" ) ) );
        assertFalse( isEmpty( child.getAttribute( "name" ) ) );

        Xpp3Dom[] testcase = testSuite.getChildren( "testcase" );
        Xpp3Dom tca = testcase[0];
        assertEquals( TEST_ONE, tca.getAttribute( "name" ) );
        assertEquals( "0.012", tca.getAttribute( "time" ) );
        assertEquals( getClass().getName(), tca.getAttribute( "classname" ) );

        Xpp3Dom tcb = testcase[1];
        assertEquals( TEST_TWO, tcb.getAttribute( "name" ) );
        assertEquals( "0.013", tcb.getAttribute( "time" ) );
        assertEquals( getClass().getName(), tcb.getAttribute( "classname" ) );
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
        WrappedReportEntry testSetReportEntry =
                new WrappedReportEntry( new SimpleReportEntry( NORMAL_RUN, 0L,
                    getClass().getName(), null, TEST_ONE, null, 12 ),
                        ReportEntryType.SUCCESS, 12, null, null, systemProps() );
        expectedReportFile = new File( reportDir, "TEST-" + getClass().getName() + ".xml" );

        stats.testSucceeded( testSetReportEntry );
        StackTraceWriter stackTraceWriterOne = new DeserializedStacktraceWriter( "A fud msg", "trimmed",
                "fail at foo" );
        StackTraceWriter stackTraceWriterTwo =
                new DeserializedStacktraceWriter( "A fud msg two", "trimmed two", "fail at foo two" );

        String firstRunOut = "first run out";
        String firstRunErr = "first run err";
        String secondRunOut = "second run out";
        String secondRunErr = "second run err";

        String cls = getClass().getName();
        WrappedReportEntry testTwoFirstError = new WrappedReportEntry( new SimpleReportEntry( NORMAL_RUN, 0L,
            cls, null, TEST_TWO, null, stackTraceWriterOne, 5 ),
            ReportEntryType.ERROR, 5, createStdOutput( firstRunOut ), createStdOutput( firstRunErr ) );

        WrappedReportEntry testTwoSecondError = new WrappedReportEntry( new SimpleReportEntry(
            RERUN_TEST_AFTER_FAILURE, 1L, cls, null, TEST_TWO, null, stackTraceWriterTwo, 13 ),
            ReportEntryType.ERROR, 13, createStdOutput( secondRunOut ), createStdOutput( secondRunErr ) );

        WrappedReportEntry testThreeFirstRun = new WrappedReportEntry( new SimpleReportEntry( NORMAL_RUN, 2L,
            cls, null, TEST_THREE, null, stackTraceWriterOne, 13 ),
            ReportEntryType.FAILURE, 13, createStdOutput( firstRunOut ), createStdOutput( firstRunErr ) );

        WrappedReportEntry testThreeSecondRun = new WrappedReportEntry( new SimpleReportEntry(
            RERUN_TEST_AFTER_FAILURE, 3L, cls, null, TEST_THREE, null, stackTraceWriterTwo, 2 ),
            ReportEntryType.SUCCESS, 2, createStdOutput( secondRunOut ), createStdOutput( secondRunErr ) );

        stats.testSucceeded( testTwoFirstError );
        stats.testSucceeded( testThreeFirstRun );
        rerunStats.testSucceeded( testTwoSecondError );
        rerunStats.testSucceeded( testThreeSecondRun );

        StatelessXmlReporter reporter =
                new StatelessXmlReporter( reportDir, null, false, 1,
                        new HashMap<String, Deque<WrappedReportEntry>>(), XSD, "3.0", false, false, false, false );

        reporter.testSetCompleted( testSetReportEntry, stats );
        reporter.testSetCompleted( testSetReportEntry, rerunStats );

        FileInputStream fileInputStream = new FileInputStream( expectedReportFile );

        Xpp3Dom testSuite = Xpp3DomBuilder.build( new InputStreamReader( fileInputStream, UTF_8 ) );
        assertEquals( "testsuite", testSuite.getName() );
        assertEquals( "0.012", testSuite.getAttribute( "time" ) );
        Xpp3Dom properties = testSuite.getChild( "properties" );
        assertEquals( System.getProperties().size(), properties.getChildCount() );
        Xpp3Dom child = properties.getChild( 1 );
        assertFalse( isEmpty( child.getAttribute( "value" ) ) );
        assertFalse( isEmpty( child.getAttribute( "name" ) ) );

        Xpp3Dom[] testcase = testSuite.getChildren( "testcase" );
        Xpp3Dom testCaseOne = testcase[0];
        assertEquals( TEST_ONE, testCaseOne.getAttribute( "name" ) );
        assertEquals( "0.012", testCaseOne.getAttribute( "time" ) );
        assertEquals( getClass().getName(), testCaseOne.getAttribute( "classname" ) );

        Xpp3Dom testCaseTwo = testcase[1];
        assertEquals( TEST_TWO, testCaseTwo.getAttribute( "name" ) );
        // Run time for a rerun failing test is the run time of the first run
        assertEquals( "0.005", testCaseTwo.getAttribute( "time" ) );
        assertEquals( getClass().getName(), testCaseTwo.getAttribute( "classname" ) );
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
        assertEquals( getClass().getName(), testCaseThree.getAttribute( "classname" ) );
        Xpp3Dom flakyFailureNode = testCaseThree.getChild( "flakyFailure" );
        assertNotNull( flakyFailureNode );
        assertEquals( firstRunOut, flakyFailureNode.getChild( "system-out" ).getValue() );
        assertEquals( firstRunErr, flakyFailureNode.getChild( "system-err" ).getValue() );
        // system-out and system-err should not be present for flaky failures
        assertNull( testCaseThree.getChild( "system-out" ) );
        assertNull( testCaseThree.getChild( "system-err" ) );
    }

    public void testOutputRerunFlakyAssumption()
        throws IOException
    {
        expectedReportFile = new File( reportDir, "TEST-" + getClass().getName() + ".xml" );

        StackTraceWriter stackTraceWriterOne = new DeserializedStacktraceWriter( "A fud msg", "trimmed",
            "fail at foo" );

        StackTraceWriter stackTraceWriterTwo =
            new DeserializedStacktraceWriter( "A fud msg two", "trimmed two", "fail at foo two" );

        String firstRunOut = "first run out";
        String firstRunErr = "first run err";
        String secondRunOut = "second run out";
        String secondRunErr = "second run err";

        WrappedReportEntry testTwoFirstError =
            new WrappedReportEntry( new SimpleReportEntry( NORMAL_RUN, 1L, getClass().getName(), null, TEST_TWO, null,
                stackTraceWriterOne, 5 ), ERROR, 5, createStdOutput( firstRunOut ),
                createStdOutput( firstRunErr ) );

        stats.testSucceeded( testTwoFirstError );

        WrappedReportEntry testTwoSecondError =
            new WrappedReportEntry( new SimpleReportEntry( RERUN_TEST_AFTER_FAILURE, 1L, getClass().getName(), null,
                TEST_TWO, null,
                stackTraceWriterTwo, 13 ), SKIPPED, 13, createStdOutput( secondRunOut ),
                createStdOutput( secondRunErr ) );

        rerunStats.testSucceeded( testTwoSecondError );

        StatelessXmlReporter reporter =
            new StatelessXmlReporter( reportDir, null, false, 1,
                new HashMap<>(), XSD, "3.0", false, false, false, false );

        WrappedReportEntry testSetReportEntry =
            new WrappedReportEntry( new SimpleReportEntry( RERUN_TEST_AFTER_FAILURE, 1L, getClass().getName(), null,
                null, null,
                stackTraceWriterOne, 5 ), ERROR, 20, createStdOutput( firstRunOut ),
                createStdOutput( firstRunErr ) );

        reporter.testSetCompleted( testSetReportEntry, stats );
        reporter.testSetCompleted( testSetReportEntry, rerunStats );

        FileInputStream fileInputStream = new FileInputStream( expectedReportFile );

        Xpp3Dom testSuite = Xpp3DomBuilder.build( new InputStreamReader( fileInputStream, UTF_8 ) );
        assertEquals( "testsuite", testSuite.getName() );
        assertEquals( "0.02", testSuite.getAttribute( "time" ) );

        Xpp3Dom[] testcase = testSuite.getChildren( "testcase" );
        assertEquals( 1, testcase.length );
        Xpp3Dom testCaseOne = testcase[0];
        assertEquals( getClass().getName(), testCaseOne.getAttribute( "classname" ) );
        assertEquals( TEST_TWO, testCaseOne.getAttribute( "name" ) );
        assertEquals( "0.005", testCaseOne.getAttribute( "time" ) );

        Xpp3Dom[] testCaseElements = testCaseOne.getChildren();
        assertEquals( 3, testCaseElements.length );
        assertEquals( "error", testCaseElements[0].getName() );
        assertEquals( "system-out", testCaseElements[1].getName() );
        assertEquals( "system-err", testCaseElements[2].getName() );
        long linesWithComments = readAllLines( expectedReportFile.toPath(), UTF_8 )
            .stream()
            .filter( line -> line.contains( "<!-- a skipped test execution in re-run phase -->" ) )
            .count();
        assertEquals( 1, linesWithComments );
    }

    public void testNoWritesOnDeferredFile() throws Exception
    {
        Utf8RecodingDeferredFileOutputStream out = new Utf8RecodingDeferredFileOutputStream( "test" );
        out.free();
        out.write( "a", false );
        assertThat( (boolean) getInternalState( out, "isDirty" ) )
            .isFalse();
    }

    public void testLengthOnDeferredFile() throws Exception
    {
        Utf8RecodingDeferredFileOutputStream out = new Utf8RecodingDeferredFileOutputStream( "test" );

        assertThat( out.getByteCount() ).isZero();

        RandomAccessFile storage = mock( RandomAccessFile.class );
        setInternalState( out, "storage", storage );
        when( storage.length() ).thenReturn( 1L );
        assertThat( out.getByteCount() ).isEqualTo( 1 );

        when( storage.length() ).thenThrow( IOException.class );
        assertThat( out.getByteCount() ).isZero();
        out.free();
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    public void testWritesOnDeferredFile() throws Exception
    {
        Utf8RecodingDeferredFileOutputStream out = new Utf8RecodingDeferredFileOutputStream( "test" );
        for ( int i = 0; i < 33_000; i++ )
        {
            out.write( "A", false );
            out.write( "B", true );
        }
        out.write( null, false );
        out.write( null, true );

        assertThat( out.getByteCount() )
            .isEqualTo( 33_000 * ( 1 + 1 + NL.length() ) + 4 + 4 + NL.length() );

        StringBuilder expectedContent = new StringBuilder( 150_000 );
        for ( int i = 0; i < 33_000; i++ )
        {
            expectedContent.append( 'A' ).append( 'B' ).append( NL );
        }
        expectedContent.append( "null" ).append( "null" ).append( NL );
        ByteArrayOutputStream read = new ByteArrayOutputStream( 150_000 );
        out.writeTo( read );
        assertThat( read.toString() )
            .isEqualTo( expectedContent.toString() );

        out.free();
    }

    public void testFreeOnDeferredFile() throws Exception
    {
        Utf8RecodingDeferredFileOutputStream out = new Utf8RecodingDeferredFileOutputStream( "test" );
        setInternalState( out, "cache", ByteBuffer.allocate( 0 ) );
        Path path = mock( Path.class );
        File file = mock( File.class );
        when( path.toFile() ).thenReturn( file );
        setInternalState( out, "file", path );
        RandomAccessFile storage = mock( RandomAccessFile.class );
        doThrow( IOException.class ).when( storage ).close();
        setInternalState( out, "storage", storage );
        out.free();
        assertThat( (boolean) getInternalState( out, "closed" ) ).isTrue();
        verify( file, times( 1 ) ).deleteOnExit();
    }

    public void testCacheOnDeferredFile() throws Exception
    {
        Utf8RecodingDeferredFileOutputStream out = new Utf8RecodingDeferredFileOutputStream( "test" );
        byte[] b1 = invokeMethod( out, "getLargeCache", 1 );
        byte[] b2 = invokeMethod( out, "getLargeCache", 1 );
        assertThat( b1 ).isSameAs( b2 );
        assertThat( b1 ).hasSize( 1 );

        byte[] b3 = invokeMethod( out, "getLargeCache", 2 );
        assertThat( b3 ).isNotSameAs( b1 );
        assertThat( b3 ).hasSize( 2 );

        byte[] b4 = invokeMethod( out, "getLargeCache", 1 );
        assertThat( b4 ).isSameAs( b3 );
        assertThat( b3 ).hasSize( 2 );
    }

    public void testSyncOnDeferredFile() throws Exception
    {
        Utf8RecodingDeferredFileOutputStream out = new Utf8RecodingDeferredFileOutputStream( "test" );
        Buffer cache = ByteBuffer.wrap( new byte[] {1, 2, 3} );
        cache.position( 3 );
        setInternalState( out, "cache", cache );
        assertThat( (boolean) getInternalState( out, "isDirty" ) ).isFalse();
        setInternalState( out, "isDirty", true );
        File file = new File( reportDir, "test" );
        setInternalState( out, "file", file.toPath() );
        RandomAccessFile storage = new RandomAccessFile( file, "rw" );
        setInternalState( out, "storage", storage );
        invokeMethod( out, "sync" );
        assertThat( (boolean) getInternalState( out, "isDirty" ) ).isFalse();
        storage.seek( 0L );
        assertThat( storage.read() ).isEqualTo( 1 );
        assertThat( storage.read() ).isEqualTo( 2 );
        assertThat( storage.read() ).isEqualTo( 3 );
        assertThat( storage.read() ).isEqualTo( -1 );
        assertThat( storage.length() ).isEqualTo( 3L );
        assertThat( cache.position() ).isEqualTo( 0 );
        assertThat( cache.limit() ).isEqualTo( 3 );
        storage.seek( 3L );
        invokeMethod( out, "sync" );
        assertThat( (boolean) getInternalState( out, "isDirty" ) ).isFalse();
        assertThat( storage.length() ).isEqualTo( 3L );
        assertThat( out.getByteCount() ).isEqualTo( 3L );
        assertThat( (boolean) getInternalState( out, "closed" ) ).isFalse();
        out.free();
        assertThat( (boolean) getInternalState( out, "closed" ) ).isTrue();
        assertThat( file ).doesNotExist();
        out.free();
        assertThat( (boolean) getInternalState( out, "closed" ) ).isTrue();
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
        stdOut.write( content, false );
        return stdOut;
    }
}
