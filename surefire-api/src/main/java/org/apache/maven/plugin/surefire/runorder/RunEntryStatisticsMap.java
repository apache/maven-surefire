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

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.util.internal.ClassMethod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.sort;
import static org.apache.maven.surefire.util.internal.StringUtils.NL;

/**
 * @author Kristian Rosenvold
 */
public final class RunEntryStatisticsMap
{
    private final Map<ClassMethod, RunEntryStatistics> runEntryStatistics;

    private RunEntryStatisticsMap( Map<ClassMethod, RunEntryStatistics> runEntryStatistics )
    {
        this.runEntryStatistics = new ConcurrentHashMap<>( runEntryStatistics );
    }

    public RunEntryStatisticsMap()
    {
        runEntryStatistics = new ConcurrentHashMap<>();
    }

    public static RunEntryStatisticsMap fromFile( File file )
    {
        if ( file.exists() )
        {
            try
            {
                return fromStream( new FileInputStream( file ) );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        }
        else
        {
            return new RunEntryStatisticsMap();
        }
    }

    static RunEntryStatisticsMap fromStream( InputStream fileReader )
    {
        Map<ClassMethod, RunEntryStatistics> result = new HashMap<>();
        try ( Scanner scanner = new Scanner( fileReader, "UTF-8" ) )
        {
            RunEntryStatistics previous = null;
            while ( scanner.hasNextLine() )
            {
                String line = scanner.nextLine();

                if ( line.charAt( 0 ) == ' ' )
                {
                    previous = new RunEntryStatistics( previous.getRunTime(),
                            previous.getSuccessfulBuilds(),
                            previous.getClassMethod().getClazz(),
                            previous.getClassMethod().getMethod() + NL + line.substring( 1 ) );
                }
                else
                {
                    if ( previous != null )
                    {
                        result.put( previous.getClassMethod(), previous );
                    }
                    StringTokenizer tokenizer = new StringTokenizer( line, "," );

                    int methodIndex = 3;

                    String successfulBuildsString = tokenizer.nextToken();
                    int successfulBuilds = parseInt( successfulBuildsString );

                    methodIndex += successfulBuildsString.length();

                    String runTimeString = tokenizer.nextToken();
                    int runTime = parseInt( runTimeString );

                    methodIndex += runTimeString.length();

                    String className = tokenizer.nextToken();

                    methodIndex += className.length();

                    String methodName = line.substring( methodIndex );

                    ClassMethod classMethod = new ClassMethod( className, methodName );
                    previous = new RunEntryStatistics( runTime, successfulBuilds, classMethod );
                }
            }
            if ( previous != null )
            {
                result.put( previous.getClassMethod(), previous );
            }
        }
        return new RunEntryStatisticsMap( result );
    }

    public void serialize( File statsFile )
        throws IOException
    {
        if ( statsFile.isFile() )
        {
            //noinspection ResultOfMethodCallIgnored
            statsFile.delete();
        }
        OutputStream os = new FileOutputStream( statsFile );
        try ( BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( os, UTF_8 ), 64 * 1024 ) )
        {
            List<RunEntryStatistics> items = new ArrayList<>( runEntryStatistics.values() );
            sort( items, new RunCountComparator() );
            for ( Iterator<RunEntryStatistics> it = items.iterator(); it.hasNext(); )
            {
                RunEntryStatistics item = it.next();
                ClassMethod test = item.getClassMethod();
                String line = item.getSuccessfulBuilds() + "," + item.getRunTime() + "," + test.getClazz() + ",";
                writer.write( line );
                boolean wasFirstLine = false;
                for ( Scanner scanner = new Scanner( test.getMethod() ); scanner.hasNextLine(); wasFirstLine = true )
                {
                    String methodLine = scanner.nextLine();
                    if ( wasFirstLine )
                    {
                        writer.write( ' ' );
                    }
                    writer.write( methodLine );
                    if ( scanner.hasNextLine() )
                    {
                        writer.newLine();
                    }
                }
                if ( it.hasNext() )
                {
                    writer.newLine();
                }
            }
        }
    }

    private RunEntryStatistics findOrCreate( ReportEntry reportEntry )
    {
        ClassMethod classMethod = new ClassMethod( reportEntry.getSourceName(), reportEntry.getName() );
        RunEntryStatistics item = runEntryStatistics.get( classMethod );
        return item != null ? item : new RunEntryStatistics( reportEntry.getElapsed( 0 ), 0, classMethod );
    }

    public RunEntryStatistics createNextGeneration( ReportEntry reportEntry )
    {
        RunEntryStatistics newItem = findOrCreate( reportEntry );
        return newItem.nextGeneration( reportEntry.getElapsed( 0 ) );
    }

    public RunEntryStatistics createNextGenerationFailure( ReportEntry reportEntry )
    {
        RunEntryStatistics newItem = findOrCreate( reportEntry );
        return newItem.nextGenerationFailure( reportEntry.getElapsed( 0 ) );
    }

    public void add( RunEntryStatistics item )
    {
        runEntryStatistics.put( item.getClassMethod(), item );
    }

    static final class RunCountComparator
        implements Comparator<RunEntryStatistics>
    {
        @Override
        public int compare( RunEntryStatistics o, RunEntryStatistics o1 )
        {
            int runtime = o.getSuccessfulBuilds() - o1.getSuccessfulBuilds();
            return runtime == 0 ? o.getRunTime() - o1.getRunTime() : runtime;
        }
    }

    public List<Class<?>> getPrioritizedTestsClassRunTime( List<Class<?>> testsToRun, int threadCount )
    {
        List<PrioritizedTest> prioritizedTests = getPrioritizedTests( testsToRun, new TestRuntimeComparator() );
        ThreadedExecutionScheduler threadedExecutionScheduler = new ThreadedExecutionScheduler( threadCount );
        for ( Object prioritizedTest1 : prioritizedTests )
        {
            threadedExecutionScheduler.addTest( (PrioritizedTest) prioritizedTest1 );
        }

        return threadedExecutionScheduler.getResult();
    }

    public List<Class<?>> getPrioritizedTestsByFailureFirst( List<Class<?>> testsToRun )
    {
        List<PrioritizedTest> prioritizedTests = getPrioritizedTests( testsToRun, new LeastFailureComparator() );
        return transformToClasses( prioritizedTests );
    }

    private List<PrioritizedTest> getPrioritizedTests( List<Class<?>> testsToRun,
                                                       Comparator<Priority> priorityComparator )
    {
        Map<String, Priority> classPriorities = getPriorities( priorityComparator );

        List<PrioritizedTest> tests = new ArrayList<>();
        for ( Class<?> clazz : testsToRun )
        {
            Priority pri = classPriorities.get( clazz.getName() );
            if ( pri == null )
            {
                pri = Priority.newTestClassPriority( clazz.getName() );
            }
            PrioritizedTest prioritizedTest = new PrioritizedTest( clazz, pri );
            tests.add( prioritizedTest );
        }
        sort( tests, new PrioritizedTestComparator() );
        return tests;
    }

    private static List<Class<?>> transformToClasses( List<PrioritizedTest> tests )
    {
        List<Class<?>> result = new ArrayList<>();
        for ( PrioritizedTest test : tests )
        {
            result.add( test.getClazz() );
        }
        return result;
    }

    private Map<String, Priority> getPriorities( Comparator<Priority> priorityComparator )
    {
        Map<String, Priority> priorities = new HashMap<>();
        for ( Entry<ClassMethod, RunEntryStatistics> testNames : runEntryStatistics.entrySet() )
        {
            String clazzName = testNames.getKey().getClazz();
            Priority priority = priorities.get( clazzName );
            if ( priority == null )
            {
                priority = new Priority( clazzName );
                priorities.put( clazzName, priority );
            }
            priority.addItem( testNames.getValue() );
        }

        List<Priority> items = new ArrayList<>( priorities.values() );
        sort( items, priorityComparator );
        Map<String, Priority> result = new HashMap<>();
        int i = 0;
        for ( Priority pri : items )
        {
            pri.setPriority( i++ );
            result.put( pri.getClassName(), pri );
        }
        return result;
    }

    static final class PrioritizedTestComparator
        implements Comparator<PrioritizedTest>
    {
        @Override
        public int compare( PrioritizedTest o, PrioritizedTest o1 )
        {
            return o.getPriority() - o1.getPriority();
        }
    }

    static final class TestRuntimeComparator
        implements Comparator<Priority>
    {
        @Override
        public int compare( Priority o, Priority o1 )
        {
            return o1.getTotalRuntime() - o.getTotalRuntime();
        }
    }

    static final class LeastFailureComparator
        implements Comparator<Priority>
    {
        @Override
        public int compare( Priority o, Priority o1 )
        {
            return o.getMinSuccessRate() - o1.getMinSuccessRate();
        }
    }
}
