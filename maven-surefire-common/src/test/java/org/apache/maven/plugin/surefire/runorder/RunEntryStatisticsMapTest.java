package org.apache.maven.plugin.surefire.runorder;

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.maven.surefire.api.runorder.RunEntryStatistics;
import org.apache.maven.surefire.api.runorder.RunEntryStatisticsMap;
import org.apache.maven.surefire.api.report.ReportEntry;
import org.apache.maven.surefire.api.report.SimpleReportEntry;

import junit.framework.TestCase;
import org.apache.maven.surefire.api.util.internal.ClassMethod;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.shared.io.IOUtils.readLines;
import static org.apache.maven.surefire.api.util.internal.StringUtils.NL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.reflect.Whitebox.getInternalState;

/**
 * @author Kristian Rosenvold
 */
public class RunEntryStatisticsMapTest
    extends TestCase
{
    public void testPrioritizedClassRuntime()
    {
        final RunEntryStatisticsMap runEntryStatisticsMap = RunEntryStatisticsMap.fromStream( getStatisticsFile() );
        final List<Class<?>> list = Arrays.asList( A.class, B.class, C.class );
        final List<Class<?>> prioritizedTestsClassRunTime =
            runEntryStatisticsMap.getPrioritizedTestsClassRunTime( list, 2 );
        assertEquals( C.class, prioritizedTestsClassRunTime.get( 0 ) );
        assertEquals( B.class, prioritizedTestsClassRunTime.get( 1 ) );
        assertEquals( A.class, prioritizedTestsClassRunTime.get( 2 ) );
    }

    public void testPrioritizedFailureFirst()
    {
        final RunEntryStatisticsMap runEntryStatisticsMap = RunEntryStatisticsMap.fromStream( getStatisticsFile() );
        final List<Class<?>> list = Arrays.asList( A.class, B.class, NewClass.class, C.class );
        final List<Class<?>> prioritizedTestsClassRunTime =
            runEntryStatisticsMap.getPrioritizedTestsByFailureFirst( list );
        assertEquals( A.class, prioritizedTestsClassRunTime.get( 0 ) );
        assertEquals( NewClass.class, prioritizedTestsClassRunTime.get( 1 ) );
        assertEquals( C.class, prioritizedTestsClassRunTime.get( 2 ) );
        assertEquals( B.class, prioritizedTestsClassRunTime.get( 3 ) );
    }

    private InputStream getStatisticsFile()
    {
        String content = "0,17,org.apache.maven.plugin.surefire.runorder.RunEntryStatisticsMapTest$A,testA\n"
                + "2,42,org.apache.maven.plugin.surefire.runorder.RunEntryStatisticsMapTest$B,testB\n"
                + "1,100,org.apache.maven.plugin.surefire.runorder.RunEntryStatisticsMapTest$C,testC\n";
        return new ByteArrayInputStream( content.getBytes( UTF_8 ) );
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    public void testSerializeClass()
        throws Exception
    {
        File data = File.createTempFile( "surefire-unit", "test" );
        RunEntryStatisticsMap newResults = new RunEntryStatisticsMap();
        ReportEntry reportEntry = new SimpleReportEntry( NORMAL_RUN, 0L,
            "abc", null, null, null, 42 );
        newResults.add( newResults.createNextGeneration( reportEntry ) );
        newResults.serialize( data );
        try ( InputStream io = new FileInputStream( data ) )
        {
            List<String> lines = readLines( io, UTF_8 );

            assertThat( lines )
                .hasSize( 1 );

            assertThat( lines )
                .containsSequence( "1,42,abc," );
        }
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    public void testDeserializeClass()
        throws Exception
    {
        File data = File.createTempFile( "surefire-unit", "test" );
        Files.write( data.toPath(), "1,42,abc".getBytes( UTF_8 ) );
        RunEntryStatisticsMap existingEntries = RunEntryStatisticsMap.fromFile( data );
        Map<?, ?> runEntryStatistics = getInternalState( existingEntries, "runEntryStatistics" );
        assertThat( runEntryStatistics )
            .hasSize( 1 );
        ClassMethod cm = (ClassMethod) runEntryStatistics.keySet().iterator().next();
        assertThat( cm.getClazz() )
            .isEqualTo( "abc" );
        assertThat( cm.getMethod() )
            .isNull();
        RunEntryStatistics statistics = (RunEntryStatistics) runEntryStatistics.values().iterator().next();
        assertThat( statistics.getRunTime() )
            .isEqualTo( 42 );
        assertThat( statistics.getSuccessfulBuilds() )
            .isEqualTo( 1 );
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    public void testSerialize()
        throws Exception
    {
        File data = File.createTempFile( "surefire-unit", "test" );
        RunEntryStatisticsMap existingEntries = RunEntryStatisticsMap.fromFile( data );
        RunEntryStatisticsMap newResults = new RunEntryStatisticsMap();

        ReportEntry reportEntry1 = new SimpleReportEntry( NORMAL_RUN, 0L,
            "abc", null, "method1", null, 42 );
        ReportEntry reportEntry2 = new SimpleReportEntry( NORMAL_RUN, 0L,
            "abc", null, "willFail", null, 17 );
        ReportEntry reportEntry3 = new SimpleReportEntry( NORMAL_RUN, 0L,
            "abc", null, "method3", null, 100 );

        newResults.add( existingEntries.createNextGeneration( reportEntry1 ) );
        newResults.add( existingEntries.createNextGeneration( reportEntry2 ) );
        newResults.add( existingEntries.createNextGeneration( reportEntry3 ) );

        newResults.serialize( data );
        try ( InputStream io = new FileInputStream( data ) )
        {
            List<String> lines = readLines( io, UTF_8 );

            assertThat( lines )
                    .hasSize( 3 );

            assertThat( lines )
                    .containsSequence( "1,17,abc,willFail", "1,42,abc,method1", "1,100,abc,method3" );
        }

        RunEntryStatisticsMap nextRun = RunEntryStatisticsMap.fromFile( data );
        newResults = new RunEntryStatisticsMap();

        ReportEntry newRunReportEntry1 = new SimpleReportEntry( NORMAL_RUN, 0L,
            "abc", null, "method1", null, 52 );
        ReportEntry newRunReportEntry2 = new SimpleReportEntry( NORMAL_RUN, 0L,
            "abc", null, "willFail", null, 27 );
        ReportEntry newRunReportEntry3 = new SimpleReportEntry( NORMAL_RUN, 0L,
            "abc", null, "method3", null, 110 );

        newResults.add( nextRun.createNextGeneration( newRunReportEntry1 ) );
        newResults.add( nextRun.createNextGenerationFailure( newRunReportEntry2 ) );
        newResults.add( nextRun.createNextGeneration( newRunReportEntry3 ) );

        newResults.serialize( data );
        try ( InputStream io = new FileInputStream( data ) )
        {
            List<String> lines = readLines( io, UTF_8 );

            assertThat( lines )
                    .hasSize( 3 );

            assertThat( lines )
                    .containsSequence( "0,27,abc,willFail", "2,52,abc,method1", "2,110,abc,method3" );
        }
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    public void testMultiLineTestMethodName() throws IOException
    {
        File data = File.createTempFile( "surefire-unit", "test" );
        RunEntryStatisticsMap reportEntries = RunEntryStatisticsMap.fromFile( data );
        ReportEntry reportEntry = new SimpleReportEntry( NORMAL_RUN, 0L,
            "abc", null, "line1\nline2" + NL + " line3", null, 42 );
        reportEntries.add( reportEntries.createNextGeneration( reportEntry ) );

        reportEntries.serialize( data );
        try ( InputStream io = new FileInputStream( data ) )
        {
            List<String> lines = readLines( io, UTF_8 );

            assertThat( lines )
                    .hasSize( 3 );

            assertThat( lines )
                    .containsSequence( "1,42,abc,line1", " line2", "  line3" );
        }

        RunEntryStatisticsMap nextRun = RunEntryStatisticsMap.fromFile( data );
        assertThat( data.delete() ).isTrue();
        nextRun.serialize( data );
        try ( InputStream io = new FileInputStream( data ) )
        {
            List<String> lines = readLines( io, UTF_8 );

            assertThat( lines )
                    .hasSize( 3 );

            assertThat( lines )
                    .containsSequence( "1,42,abc,line1", " line2", "  line3" );
        }
    }

    @SuppressWarnings( "checkstyle:magicnumber" )
    public void testCombinedMethodNames() throws IOException
    {
        File data = File.createTempFile( "surefire-unit", "test" );
        RunEntryStatisticsMap reportEntries = RunEntryStatisticsMap.fromFile( data );
        reportEntries.add( reportEntries.createNextGeneration( new SimpleReportEntry( NORMAL_RUN, 0L,
                    "abc", null, "line1\nline2", null, 42 ) ) );
        reportEntries.add( reportEntries.createNextGeneration( new SimpleReportEntry( NORMAL_RUN, 0L,
            "abc", null, "test", null, 10 ) ) );

        reportEntries.serialize( data );
        try ( InputStream io = new FileInputStream( data ) )
        {
            List<String> lines = readLines( io, UTF_8 );

            assertThat( lines )
                    .hasSize( 3 );

            assertThat( lines )
                    .containsSequence( "1,10,abc,test",
                                       "1,42,abc,line1",
                                       " line2" );
        }

        RunEntryStatisticsMap nextRun = RunEntryStatisticsMap.fromFile( data );
        assertThat( data.delete() ).isTrue();
        nextRun.serialize( data );
        try ( InputStream io = new FileInputStream( data ) )
        {
            List<String> lines = readLines( io, UTF_8 );

            assertThat( lines )
                    .hasSize( 3 );

            assertThat( lines )
                    .containsSequence( "1,10,abc,test",
                                       "1,42,abc,line1",
                                       " line2" );
        }
    }

    class A
    {
    }

    class B
    {
    }

    class C
    {
    }

    class NewClass
    {
    }
}
