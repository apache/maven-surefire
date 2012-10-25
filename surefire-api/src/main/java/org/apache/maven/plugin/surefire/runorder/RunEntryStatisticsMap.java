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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.surefire.report.ReportEntry;

/**
 * @author Kristian Rosenvold
 */
public class RunEntryStatisticsMap
{
    private final Map runEntryStatistics;

    public RunEntryStatisticsMap( Map runEntryStatistics )
    {
        this.runEntryStatistics = Collections.synchronizedMap( runEntryStatistics );
    }

    public RunEntryStatisticsMap()
    {
        this( new HashMap() );
    }

    public static RunEntryStatisticsMap fromFile( File file )
    {
        if ( file.exists() )
        {
            try
            {
                FileReader fileReader = new FileReader( file );
                return fromReader( fileReader );
            }
            catch ( FileNotFoundException e )
            {
                throw new RuntimeException( e );
            }
            catch ( IOException e1 )
            {
                throw new RuntimeException( e1 );
            }

        }
        return new RunEntryStatisticsMap();
    }

    static RunEntryStatisticsMap fromReader( Reader fileReader )
        throws IOException
    {
        Map result = new HashMap();
        BufferedReader bufferedReader = new BufferedReader( fileReader );
        String line = bufferedReader.readLine();
        while ( line != null )
        {
            if ( !line.startsWith( "#" ) )
            {
                final RunEntryStatistics stats = RunEntryStatistics.fromString( line );
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
        PrintWriter printWriter = new PrintWriter( fos );
        List items = new ArrayList( runEntryStatistics.values() );
        Collections.sort( items, new RunCountComparator() );
        RunEntryStatistics item;
        for ( Iterator iter = items.iterator(); iter.hasNext(); )
        {
            item = (RunEntryStatistics) iter.next();
            printWriter.println( item.getAsString() );
        }
        printWriter.close();
    }


    public RunEntryStatistics findOrCreate( ReportEntry reportEntry )
    {
        final RunEntryStatistics item = (RunEntryStatistics) runEntryStatistics.get( reportEntry.getName() );
        return item != null ? item : RunEntryStatistics.fromReportEntry( reportEntry );
    }

    public RunEntryStatistics createNextGeneration( ReportEntry reportEntry )
    {
        final RunEntryStatistics newItem = findOrCreate( reportEntry );
        final Integer elapsed = reportEntry.getElapsed();
        return newItem.nextGeneration( elapsed != null ? elapsed.intValue() : 0 );
    }

    public RunEntryStatistics createNextGenerationFailure( ReportEntry reportEntry )
    {
        final RunEntryStatistics newItem = findOrCreate( reportEntry );
        final Integer elapsed = reportEntry.getElapsed();
        return newItem.nextGenerationFailure( elapsed != null ? elapsed.intValue() : 0 );
    }

    public void add( RunEntryStatistics item )
    {
        runEntryStatistics.put( item.getTestName(), item );
    }

    class RunCountComparator
        implements Comparator
    {
        public int compare( Object o, Object o1 )
        {
            RunEntryStatistics re = (RunEntryStatistics) o;
            RunEntryStatistics re1 = (RunEntryStatistics) o1;
            int runtime = re.getSuccessfulBuilds() - re1.getSuccessfulBuilds();
            if ( runtime == 0 )
            {
                return re.getRunTime() - re1.getRunTime();
            }
            return runtime;
        }
    }

    public List getPrioritizedTestsClassRunTime( List testsToRun, int threadCount )
    {
        final List prioritizedTests = getPrioritizedTests( testsToRun, new TestRuntimeComparator() );
        ThreadedExecutionScheduler threadedExecutionScheduler = new ThreadedExecutionScheduler( threadCount );
        for ( Iterator prioritizedTest = prioritizedTests.iterator(); prioritizedTest.hasNext(); )
        {
            threadedExecutionScheduler.addTest( (PrioritizedTest) prioritizedTest.next() );
        }

        return threadedExecutionScheduler.getResult();

    }

    public List getPrioritizedTestsByFailureFirst( List testsToRun )
    {
        final List prioritizedTests = getPrioritizedTests( testsToRun, new LeastFailureComparator() );
        return transformToClasses( prioritizedTests );
    }


    private List getPrioritizedTests( List testsToRun, Comparator priorityComparator )
    {
        Map classPriorities = getPriorities( priorityComparator );

        List tests = new ArrayList();
        for ( Iterator iter = testsToRun.iterator(); iter.hasNext(); )
        {
            Class clazz = (Class) iter.next();
            Priority pri = (Priority) classPriorities.get( clazz.getName() );
            if ( pri == null )
            {
                pri = Priority.newTestClassPriority( clazz.getName() );
            }
            PrioritizedTest prioritizedTest = new PrioritizedTest( clazz, pri );
            tests.add( prioritizedTest );
        }
        Collections.sort( tests, new PrioritizedTestComparator() );
        return tests;

    }

    private List transformToClasses( List tests )
    {
        List result = new ArrayList();
        for ( int i = 0; i < tests.size(); i++ )
        {
            result.add( ( (PrioritizedTest) tests.get( i ) ).getClazz() );
        }
        return result;
    }

    public Map getPriorities( Comparator priorityComparator )
    {
        Map priorities = new HashMap();
        for ( Iterator iter = runEntryStatistics.keySet().iterator(); iter.hasNext(); )
        {
            String testNames = (String) iter.next();
            String clazzName = extractClassName( testNames );
            Priority priority = (Priority) priorities.get( clazzName );
            if ( priority == null )
            {
                priority = new Priority( clazzName );
                priorities.put( clazzName, priority );
            }

            RunEntryStatistics itemStat = (RunEntryStatistics) runEntryStatistics.get( testNames );
            priority.addItem( itemStat );
        }

        List items = new ArrayList( priorities.values() );
        Collections.sort( items, priorityComparator );
        Map result = new HashMap();
        int i = 0;
        for ( Iterator iter = items.iterator(); iter.hasNext(); )
        {
            Priority pri = (Priority) iter.next();
            pri.setPriority( i++ );
            result.put( pri.getClassName(), pri );
        }
        return result;
    }

    class PrioritizedTestComparator
        implements Comparator
    {
        public int compare( Object o, Object o1 )
        {
            PrioritizedTest re = (PrioritizedTest) o;
            PrioritizedTest re1 = (PrioritizedTest) o1;
            return re.getPriority() - re1.getPriority();
        }
    }

    class TestRuntimeComparator
        implements Comparator
    {
        public int compare( Object o, Object o1 )
        {
            Priority re = (Priority) o;
            Priority re1 = (Priority) o1;
            return re1.getTotalRuntime() - re.getTotalRuntime();
        }
    }

    class LeastFailureComparator
        implements Comparator
    {
        public int compare( Object o, Object o1 )
        {
            Priority re = (Priority) o;
            Priority re1 = (Priority) o1;
            return re.getMinSuccessRate() - re1.getMinSuccessRate();
        }
    }


    private static final Pattern PARENS = Pattern.compile( "^" + "[^\\(\\)]+" //non-parens
                                                               + "\\((" // then an open-paren (start matching a group)
                                                               + "[^\\\\(\\\\)]+" //non-parens
                                                               + ")\\)" + "$" ); // then a close-paren (end group match)

    String extractClassName( String displayName )
    {
        Matcher m = PARENS.matcher( displayName );
        if ( !m.find() )
        {
            return displayName;
        }
        return m.group( 1 );
    }


}
