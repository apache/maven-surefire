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
import java.util.Arrays;
import java.util.List;

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.SimpleReportEntry;

import junit.framework.TestCase;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.readLines;
import static org.apache.maven.surefire.util.internal.StringUtils.NL;
import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Kristian Rosenvold
 */
public class RunEntryStatisticsMapTest
    extends TestCase
{
    public void testPrioritizedClassRuntime()
    {
        final RunEntryStatisticsMap runEntryStatisticsMap = RunEntryStatisticsMap.fromStream( getStatisticsFile() );
        final List<Class<?>> list = Arrays.<Class<?>>asList( A.class, B.class, C.class );
        final List<Class<?>> prioritizedTestsClassRunTime =
            runEntryStatisticsMap.getPrioritizedTestsClassRunTime( list, 2 );
        assertEquals( C.class, prioritizedTestsClassRunTime.get( 0 ) );
        assertEquals( B.class, prioritizedTestsClassRunTime.get( 1 ) );
        assertEquals( A.class, prioritizedTestsClassRunTime.get( 2 ) );
    }

    public void testPrioritizedFailureFirst()
    {
        final RunEntryStatisticsMap runEntryStatisticsMap = RunEntryStatisticsMap.fromStream( getStatisticsFile() );
        final List<Class<?>> list = Arrays.<Class<?>>asList( A.class, B.class, NewClass.class, C.class );
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

    public void testSerialize()
        throws Exception
    {
        File data = File.createTempFile( "surefire-unit", "test" );
        RunEntryStatisticsMap existingEntries = RunEntryStatisticsMap.fromFile( data );
        RunEntryStatisticsMap newResults = new RunEntryStatisticsMap();

        ReportEntry reportEntry1 = new SimpleReportEntry( "abc", "method1", 42 );
        ReportEntry reportEntry2 = new SimpleReportEntry( "abc", "willFail", 17 );
        ReportEntry reportEntry3 = new SimpleReportEntry( "abc", "method3", 100 );

        newResults.add( existingEntries.createNextGeneration( reportEntry1 ) );
        newResults.add( existingEntries.createNextGeneration( reportEntry2 ) );
        newResults.add( existingEntries.createNextGeneration( reportEntry3 ) );

        newResults.serialize( data );
        try ( InputStream io = new FileInputStream( data) )
        {
            List<String> lines = readLines( io, UTF_8 );

            assertThat( lines )
                    .hasSize( 3 );

            assertThat( lines )
                    .containsSequence( "1,17,abc,willFail", "1,42,abc,method1", "1,100,abc,method3" );
        }

        RunEntryStatisticsMap nextRun = RunEntryStatisticsMap.fromFile( data );
        newResults = new RunEntryStatisticsMap();

        ReportEntry newRunReportEntry1 = new SimpleReportEntry( "abc", "method1", 52 );
        ReportEntry newRunReportEntry2 = new SimpleReportEntry( "abc", "willFail", 27 );
        ReportEntry newRunReportEntry3 = new SimpleReportEntry( "abc", "method3", 110 );

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

    public void testMultiLineTestMethodName() throws IOException
    {
        File data = File.createTempFile( "surefire-unit", "test" );
        RunEntryStatisticsMap reportEntries = RunEntryStatisticsMap.fromFile( data );
        ReportEntry reportEntry = new SimpleReportEntry( "abc", "line1\nline2" + NL + " line3", 42 );
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

    public void testCombinedMethodNames() throws IOException
    {
        File data = File.createTempFile( "surefire-unit", "test" );
        RunEntryStatisticsMap reportEntries = RunEntryStatisticsMap.fromFile( data );
        reportEntries.add( reportEntries.createNextGeneration( new SimpleReportEntry( "abc", "line1\nline2", 42 ) ) );
        reportEntries.add( reportEntries.createNextGeneration( new SimpleReportEntry( "abc", "test", 10 ) ) );

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
