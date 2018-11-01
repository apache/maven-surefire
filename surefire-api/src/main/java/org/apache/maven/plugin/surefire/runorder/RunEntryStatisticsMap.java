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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.sort;
import static org.apache.maven.plugin.surefire.runorder.RunEntryStatistics.fromReportEntry;
import static org.apache.maven.plugin.surefire.runorder.RunEntryStatistics.fromString;

/**
 * @author Kristian Rosenvold
 */
public final class RunEntryStatisticsMap
{
    private final Map<String, RunEntryStatistics> runEntryStatistics;

    public RunEntryStatisticsMap( Map<String, RunEntryStatistics> runEntryStatistics )
    {
        this.runEntryStatistics = new ConcurrentHashMap<String, RunEntryStatistics>( runEntryStatistics );
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
                return fromReader( new FileReader( file ) );
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

    static RunEntryStatisticsMap fromReader( Reader fileReader )
        throws IOException
    {
        Map<String, RunEntryStatistics> result = new HashMap<>();
        BufferedReader bufferedReader = new BufferedReader( fileReader );
        String line = bufferedReader.readLine();
        while ( line != null )
        {
            if ( !line.startsWith( "#" ) )
            {
                final RunEntryStatistics stats = fromString( line );
                result.put( stats.getTestName(), stats );
            }
            line = bufferedReader.readLine();
        }
        return new RunEntryStatisticsMap( result );
    }

    public void serialize( File file )
        throws FileNotFoundException
    {
        FileOutputStream fos = new FileOutputStream( file );
        try ( PrintWriter printWriter = new PrintWriter( fos ) )
        {
            List<RunEntryStatistics> items = new ArrayList<>( runEntryStatistics.values() );
            sort( items, new RunCountComparator() );
            for ( RunEntryStatistics item : items )
            {
                printWriter.println( item.toString() );
            }
        }
    }

    public RunEntryStatistics findOrCreate( ReportEntry reportEntry )
    {
        final RunEntryStatistics item = runEntryStatistics.get( reportEntry.getName() );
        return item != null ? item : fromReportEntry( reportEntry );
    }

    public RunEntryStatistics createNextGeneration( ReportEntry reportEntry )
    {
        final RunEntryStatistics newItem = findOrCreate( reportEntry );
        final Integer elapsed = reportEntry.getElapsed();
        return newItem.nextGeneration( elapsed != null ? elapsed : 0 );
    }

    public RunEntryStatistics createNextGenerationFailure( ReportEntry reportEntry )
    {
        final RunEntryStatistics newItem = findOrCreate( reportEntry );
        final Integer elapsed = reportEntry.getElapsed();
        return newItem.nextGenerationFailure( elapsed != null ? elapsed : 0 );
    }

    public void add( RunEntryStatistics item )
    {
        runEntryStatistics.put( item.getTestName(), item );
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
        Map classPriorities = getPriorities( priorityComparator );

        List<PrioritizedTest> tests = new ArrayList<>();
        for ( Class<?> clazz : testsToRun )
        {
            Priority pri = (Priority) classPriorities.get( clazz.getName() );
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

    private List<Class<?>> transformToClasses( List<PrioritizedTest> tests )
    {
        List<Class<?>> result = new ArrayList<>();
        for ( PrioritizedTest test : tests )
        {
            result.add( test.getClazz() );
        }
        return result;
    }

    private Map getPriorities( Comparator<Priority> priorityComparator )
    {
        Map<String, Priority> priorities = new HashMap<>();
        for ( Object o : runEntryStatistics.keySet() )
        {
            String testNames = (String) o;
            String clazzName = extractClassName( testNames );
            Priority priority = priorities.get( clazzName );
            if ( priority == null )
            {
                priority = new Priority( clazzName );
                priorities.put( clazzName, priority );
            }

            RunEntryStatistics itemStat = runEntryStatistics.get( testNames );
            priority.addItem( itemStat );
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


    private static final Pattern PARENS = Pattern.compile( "^" + "[^\\(\\)]+" //non-parens
                                                               + "\\((" // then an open-paren (start matching a group)
                                                               + "[^\\\\(\\\\)]+" //non-parens
                                                               + ")\\)" + "$" ); // then a close-paren (end group match)

    String extractClassName( String displayName )
    {
        Matcher m = PARENS.matcher( displayName );
        return m.find() ? m.group( 1 ) : displayName;
    }
}
