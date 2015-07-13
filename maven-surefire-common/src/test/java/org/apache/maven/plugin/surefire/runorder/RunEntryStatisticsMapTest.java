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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.SimpleReportEntry;

import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class RunEntryStatisticsMapTest
    extends TestCase
{
    public void testPrioritizedClassRuntime()
        throws IOException
    {
        final RunEntryStatisticsMap runEntryStatisticsMap = RunEntryStatisticsMap.fromReader( getStatisticsFile() );
        final List<Class<?>> list = Arrays.<Class<?>>asList( A.class, B.class, C.class );
        final List<Class<?>> prioritizedTestsClassRunTime =
            runEntryStatisticsMap.getPrioritizedTestsClassRunTime( list, 2 );
        assertEquals( C.class, prioritizedTestsClassRunTime.get( 0 ) );
        assertEquals( B.class, prioritizedTestsClassRunTime.get( 1 ) );
        assertEquals( A.class, prioritizedTestsClassRunTime.get( 2 ) );
    }

    public void testPrioritizedFailureFirst()
        throws IOException
    {
        final RunEntryStatisticsMap runEntryStatisticsMap = RunEntryStatisticsMap.fromReader( getStatisticsFile() );
        final List<Class<?>> list = Arrays.<Class<?>>asList( A.class, B.class, NewClass.class, C.class );
        final List<Class<?>> prioritizedTestsClassRunTime =
            runEntryStatisticsMap.getPrioritizedTestsByFailureFirst( list );
        assertEquals( A.class, prioritizedTestsClassRunTime.get( 0 ) );
        assertEquals( NewClass.class, prioritizedTestsClassRunTime.get( 1 ) );
        assertEquals( C.class, prioritizedTestsClassRunTime.get( 2 ) );
        assertEquals( B.class, prioritizedTestsClassRunTime.get( 3 ) );
    }

    private StringReader getStatisticsFile()
    {
        String content = "0,17,testA(org.apache.maven.plugin.surefire.runorder.RunEntryStatisticsMapTest$A)\n" +
            "2,42,testB(org.apache.maven.plugin.surefire.runorder.RunEntryStatisticsMapTest$B)\n" +
            "1,100,testC(org.apache.maven.plugin.surefire.runorder.RunEntryStatisticsMapTest$C)\n";
        return new StringReader( content );
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

        RunEntryStatisticsMap nextRun = RunEntryStatisticsMap.fromFile( data );
        newResults = new RunEntryStatisticsMap();

        newResults.add( existingEntries.createNextGeneration( reportEntry1 ) );
        newResults.add( existingEntries.createNextGenerationFailure( reportEntry2 ) );
        newResults.add( existingEntries.createNextGeneration( reportEntry3 ) );

        newResults.serialize( data );
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
