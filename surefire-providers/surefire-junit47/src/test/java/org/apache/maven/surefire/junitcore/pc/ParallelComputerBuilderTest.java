package org.apache.maven.surefire.junitcore.pc;

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

import net.jcip.annotations.NotThreadSafe;
import org.apache.maven.surefire.report.ConsoleStream;
import org.apache.maven.surefire.report.DefaultDirectConsoleReporter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Stopwatch;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.apache.maven.surefire.junitcore.pc.RangeMatcher.between;
import static org.junit.Assert.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Tibor Digana (tibor17)
 * @since 2.16
 */
public class ParallelComputerBuilderTest
{
    private static final Object class1Lock = new Object();

    private static volatile boolean beforeShutdown;

    private static volatile Runnable shutdownTask;

    private static final ConsoleStream logger = new DefaultDirectConsoleReporter( System.out );

    @Rule
    public final Stopwatch stopwatch = new Stopwatch() {};

    private static void testKeepBeforeAfter( ParallelComputerBuilder builder, Class<?>... classes )
    {
        JUnitCore core = new JUnitCore();
        for ( int round = 0; round < 5; round++ )
        {
            NothingDoingTest1.methods.clear();
            Result result = core.run( builder.buildComputer(), classes );
            assertTrue( result.wasSuccessful() );
            Iterator<String> methods = NothingDoingTest1.methods.iterator();
            for ( Class<?> clazz : classes )
            {
                String a = clazz.getName() + "#a()";
                String b = clazz.getName() + "#b()";
                assertThat( methods.next(), is( "init" ) );
                assertThat( methods.next(), anyOf( is( a ), is( b ) ) );
                assertThat( methods.next(), anyOf( is( a ), is( b ) ) );
                assertThat( methods.next(), is( "deinit" ) );
            }
        }
    }

    @Before
    public void beforeTest() throws InterruptedException {
        Class1.maxConcurrentMethods = 0;
        Class1.concurrentMethods = 0;
        shutdownTask = null;
        NotThreadSafeTest1.t = null;
        NotThreadSafeTest2.t = null;
        NotThreadSafeTest3.t = null;
        NormalTest1.t = null;
        NormalTest2.t = null;
        System.gc();
        Thread.sleep( 50L );
    }

    @Test
    public void parallelMethodsReuseOneOrTwoThreads()
    {
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder( logger );
        parallelComputerBuilder.useOnePool( 4 );

        // One thread because one suite: TestSuite, however the capacity is 5.
        parallelComputerBuilder.parallelSuites( 5 );

        // Two threads because TestSuite has two classes, however the capacity is 5.
        parallelComputerBuilder.parallelClasses( 5 );

        // One or two threads because one threads comes from '#useOnePool(4)'
        // and next thread may be reused from finished class, however the capacity is 3.
        parallelComputerBuilder.parallelMethods( 3 );

        assertFalse( parallelComputerBuilder.isOptimized() );

        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestSuite.class );
        long timeSpent = stopwatch.runtime( MILLISECONDS );

        assertThat( computer.getSuites().size(), is( 1 ) );
        assertThat( computer.getClasses().size(), is( 0 ) );
        assertThat( computer.getNestedClasses().size(), is( 2 ) );
        assertThat( computer.getNestedSuites().size(), is( 0 ) );
        assertFalse( computer.isSplitPool() );
        assertThat( computer.getPoolCapacity(), is( 4 ) );
        assertTrue( result.wasSuccessful() );
        if ( Class1.maxConcurrentMethods == 1 )
        {
            assertThat( timeSpent, between( 1950, 2250 ) );
        }
        else if ( Class1.maxConcurrentMethods == 2 )
        {
            assertThat( timeSpent, between( 1450, 1750 ) );
        }
        else
        {
            fail();
        }
    }

    @Test
    public void suiteAndClassInOnePool()
    {
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder( logger );
        parallelComputerBuilder.useOnePool( 5 );
        parallelComputerBuilder.parallelSuites( 5 );
        parallelComputerBuilder.parallelClasses( 5 );
        parallelComputerBuilder.parallelMethods( 3 );
        assertFalse( parallelComputerBuilder.isOptimized() );

        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestSuite.class, Class1.class );
        long timeSpent = stopwatch.runtime( MILLISECONDS );

        assertThat( computer.getSuites().size(), is( 1 ) );
        assertThat( computer.getClasses().size(), is( 1 ) );
        assertThat( computer.getNestedClasses().size(), is( 2 ) );
        assertThat( computer.getNestedSuites().size(), is( 0 ) );
        assertFalse( computer.isSplitPool() );
        assertThat( computer.getPoolCapacity(), is( 5 ) );
        assertTrue( result.wasSuccessful() );
        assertThat( Class1.maxConcurrentMethods, is( 2 ) );
        assertThat( timeSpent, anyOf( between( 1450, 1750 ), between( 1950, 2250 ), between( 2450, 2750 ) ) );
    }

    @Test
    public void onePoolWithUnlimitedParallelMethods()
    {
        // see ParallelComputerBuilder Javadoc
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder( logger );
        parallelComputerBuilder.useOnePool( 8 );
        parallelComputerBuilder.parallelSuites( 2 );
        parallelComputerBuilder.parallelClasses( 4 );
        parallelComputerBuilder.parallelMethods();
        assertFalse( parallelComputerBuilder.isOptimized() );

        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestSuite.class, Class1.class );
        long timeSpent = stopwatch.runtime( MILLISECONDS );

        assertThat( computer.getSuites().size(), is( 1 ) );
        assertThat( computer.getClasses().size(), is( 1 ) );
        assertThat( computer.getNestedClasses().size(), is( 2 ) );
        assertThat( computer.getNestedSuites().size(), is( 0 ) );
        assertFalse( computer.isSplitPool() );
        assertThat( computer.getPoolCapacity(), is( 8 ) );
        assertTrue( result.wasSuccessful() );
        assertThat( Class1.maxConcurrentMethods, is( 4 ) );
        assertThat( timeSpent, between( 950, 1250 ) );
    }

    @Test
    public void underflowParallelism()
    {
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder( logger );
        parallelComputerBuilder.useOnePool( 3 );

        // One thread because one suite: TestSuite.
        parallelComputerBuilder.parallelSuites( 5 );

        // One thread because of the limitation which is bottleneck.
        parallelComputerBuilder.parallelClasses( 1 );

        // One thread remains from '#useOnePool(3)'.
        parallelComputerBuilder.parallelMethods( 3 );

        assertFalse( parallelComputerBuilder.isOptimized() );

        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestSuite.class );
        long timeSpent = stopwatch.runtime( MILLISECONDS );

        assertThat( computer.getSuites().size(), is( 1 ) );
        assertThat( computer.getClasses().size(), is( 0 ) );
        assertThat( computer.getNestedClasses().size(), is( 2 ) );
        assertThat( computer.getNestedSuites().size(), is( 0 ) );
        assertFalse( computer.isSplitPool() );
        assertThat( computer.getPoolCapacity(), is( 3 ) );
        assertTrue( result.wasSuccessful() );
        assertThat( Class1.maxConcurrentMethods, is( 1 ) );
        assertThat( timeSpent, between( 1950, 2250 ) );
    }

    @Test
    public void separatePoolsWithSuite()
    {
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder( logger );
        parallelComputerBuilder.parallelSuites( 5 );
        parallelComputerBuilder.parallelClasses( 5 );
        parallelComputerBuilder.parallelMethods( 3 );
        assertFalse( parallelComputerBuilder.isOptimized() );

        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestSuite.class );
        long timeSpent = stopwatch.runtime( MILLISECONDS );

        assertThat( computer.getSuites().size(), is( 1 ) );
        assertThat( computer.getClasses().size(), is( 0 ) );
        assertThat( computer.getNestedClasses().size(), is( 2 ) );
        assertThat( computer.getNestedSuites().size(), is( 0 ) );
        assertTrue( computer.isSplitPool() );
        assertThat( computer.getPoolCapacity(), is( ParallelComputerBuilder.TOTAL_POOL_SIZE_UNDEFINED ) );
        assertTrue( result.wasSuccessful() );
        assertThat( Class1.maxConcurrentMethods, is( 3 ) );
        assertThat( timeSpent, between( 950, 1250 ) );
    }

    @Test
    public void separatePoolsWithSuiteAndClass()
    {
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder( logger );
        parallelComputerBuilder.parallelSuites( 5 );
        parallelComputerBuilder.parallelClasses( 5 );
        parallelComputerBuilder.parallelMethods( 3 );
        assertFalse( parallelComputerBuilder.isOptimized() );

        // 6 methods altogether.
        // 2 groups with 3 threads.
        // Each group takes 0.5s.
        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestSuite.class, Class1.class );
        long timeSpent = stopwatch.runtime( MILLISECONDS );

        assertThat( computer.getSuites().size(), is( 1 ) );
        assertThat( computer.getClasses().size(), is( 1 ) );
        assertThat( computer.getNestedClasses().size(), is( 2 ) );
        assertThat( computer.getNestedSuites().size(), is( 0 ) );
        assertTrue( computer.isSplitPool() );
        assertThat( computer.getPoolCapacity(), is( ParallelComputerBuilder.TOTAL_POOL_SIZE_UNDEFINED ) );
        assertTrue( result.wasSuccessful() );
        assertThat( Class1.maxConcurrentMethods, is( 3 ) );
        assertThat( timeSpent, between( 950, 1250 ) );
    }

    @Test
    public void separatePoolsWithSuiteAndSequentialClasses()
    {
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder( logger );
        parallelComputerBuilder.parallelSuites( 5 );
        parallelComputerBuilder.parallelClasses( 1 );
        parallelComputerBuilder.parallelMethods( 3 );
        assertFalse( parallelComputerBuilder.isOptimized() );

        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestSuite.class, Class1.class );
        long timeSpent = stopwatch.runtime( MILLISECONDS );

        assertThat( computer.getSuites().size(), is( 1 ) );
        assertThat( computer.getClasses().size(), is( 1 ) );
        assertThat( computer.getNestedClasses().size(), is( 2 ) );
        assertThat( computer.getNestedSuites().size(), is( 0 ) );
        assertTrue( computer.isSplitPool() );
        assertThat( computer.getPoolCapacity(), is( ParallelComputerBuilder.TOTAL_POOL_SIZE_UNDEFINED ) );
        assertTrue( result.wasSuccessful() );
        assertThat( Class1.maxConcurrentMethods, is( 2 ) );
        assertThat( timeSpent, between( 1450, 1750 ) );
    }

    @Test( timeout = 2000 )
    public void shutdown()
    {
        Result result = new ShutdownTest().run( false );
        long timeSpent = stopwatch.runtime( MILLISECONDS );
        assertTrue( result.wasSuccessful() );
        assertTrue( beforeShutdown );
        assertThat( timeSpent, between( 450, 1250 ) );
    }

    @Test( timeout = 2000 )
    public void shutdownWithInterrupt()
    {
        new ShutdownTest().run( true );
        long timeSpent = stopwatch.runtime( MILLISECONDS );
        assertTrue( beforeShutdown );
        assertThat( timeSpent, between( 450, 1250 ) );
    }

    @Test
    public void nothingParallel()
    {
        JUnitCore core = new JUnitCore();
        ParallelComputerBuilder builder = new ParallelComputerBuilder( logger );
        assertFalse( builder.isOptimized() );

        Result result = core.run( builder.buildComputer(), NothingDoingTest1.class, NothingDoingTest2.class );
        assertTrue( result.wasSuccessful() );

        result = core.run( builder.buildComputer(), NothingDoingTest1.class, NothingDoingSuite.class );
        assertTrue( result.wasSuccessful() );

        builder.useOnePool( 1 );
        assertFalse( builder.isOptimized() );
        result = core.run( builder.buildComputer(), NothingDoingTest1.class, NothingDoingTest2.class );
        assertTrue( result.wasSuccessful() );

        builder.useOnePool( 1 );
        assertFalse( builder.isOptimized() );
        result = core.run( builder.buildComputer(), NothingDoingTest1.class, NothingDoingSuite.class );
        assertTrue( result.wasSuccessful() );

        builder.useOnePool( 2 );
        assertFalse( builder.isOptimized() );
        result = core.run( builder.buildComputer(), NothingDoingTest1.class, NothingDoingSuite.class );
        assertTrue( result.wasSuccessful() );

        Class<?>[] classes = { NothingDoingTest1.class, NothingDoingSuite.class };

        builder.useOnePool( 2 ).parallelSuites( 1 ).parallelClasses( 1 );
        assertFalse( builder.isOptimized() );
        result = core.run( builder.buildComputer(), classes );
        assertTrue( result.wasSuccessful() );

        builder.useOnePool( 2 ).parallelSuites( 1 ).parallelClasses();
        assertFalse( builder.isOptimized() );
        result = core.run( builder.buildComputer(), classes );
        assertTrue( result.wasSuccessful() );

        classes = new Class<?>[]{ NothingDoingSuite.class, NothingDoingSuite.class, NothingDoingTest1.class,
            NothingDoingTest2.class, NothingDoingTest3.class };

        builder.useOnePool( 2 ).parallelSuites( 1 ).parallelClasses( 1 );
        assertFalse( builder.isOptimized() );
        result = core.run( builder.buildComputer(), classes );
        assertTrue( result.wasSuccessful() );

        builder.useOnePool( 2 ).parallelSuites( 1 ).parallelClasses();
        assertFalse( builder.isOptimized() );
        result = core.run( builder.buildComputer(), classes );
        assertTrue( result.wasSuccessful() );
    }

    @Test
    public void keepBeforeAfterOneClass()
    {
        ParallelComputerBuilder builder = new ParallelComputerBuilder( logger );
        builder.parallelMethods();
        assertFalse( builder.isOptimized() );
        testKeepBeforeAfter( builder, NothingDoingTest1.class );
    }

    @Test
    public void keepBeforeAfterTwoClasses()
    {
        ParallelComputerBuilder builder = new ParallelComputerBuilder( logger );
        builder.useOnePool( 5 ).parallelClasses( 1 ).parallelMethods( 2 );
        assertFalse( builder.isOptimized() );
        testKeepBeforeAfter( builder, NothingDoingTest1.class, NothingDoingTest2.class );
    }

    @Test
    public void keepBeforeAfterTwoParallelClasses()
    {
        ParallelComputerBuilder builder = new ParallelComputerBuilder( logger );
        builder.useOnePool( 8 ).parallelClasses( 2 ).parallelMethods( 2 );
        assertFalse( builder.isOptimized() );
        JUnitCore core = new JUnitCore();
        NothingDoingTest1.methods.clear();
        Class<?>[] classes = { NothingDoingTest1.class, NothingDoingTest2.class, NothingDoingTest3.class };
        Result result = core.run( builder.buildComputer(), classes );
        assertTrue( result.wasSuccessful() );
        ArrayList<String> methods = new ArrayList<String>( NothingDoingTest1.methods );
        assertThat( methods.size(), is( 12 ) );
        assertThat( methods.subList( 9, 12 ), is( not( Arrays.asList( "deinit", "deinit", "deinit" ) ) ) );
    }

    @Test
    public void notThreadSafeTest()
    {
        ParallelComputerBuilder builder = new ParallelComputerBuilder( logger )
            .useOnePool( 6 ).optimize( true ).parallelClasses( 3 ).parallelMethods( 3 );
        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) builder.buildComputer();
        Result result = new JUnitCore().run( computer, NotThreadSafeTest1.class, NotThreadSafeTest2.class );
        assertTrue( result.wasSuccessful() );
        assertThat( result.getRunCount(), is( 2 ) );
        assertNotNull( NotThreadSafeTest1.t );
        assertNotNull( NotThreadSafeTest2.t );
        assertSame( NotThreadSafeTest1.t, NotThreadSafeTest2.t );
        assertThat( computer.getNotParallelRunners().size(), is( 2 ) );
        assertTrue( computer.getSuites().isEmpty() );
        assertTrue( computer.getClasses().isEmpty() );
        assertTrue( computer.getNestedClasses().isEmpty() );
        assertTrue( computer.getNestedSuites().isEmpty() );
        assertFalse( computer.isSplitPool() );
        assertThat( computer.getPoolCapacity(), is( 6 ) );
    }

    @Test
    public void mixedThreadSafety()
    {
        ParallelComputerBuilder builder = new ParallelComputerBuilder( logger )
            .useOnePool( 6 ).optimize( true ).parallelClasses( 3 ).parallelMethods( 3 );
        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) builder.buildComputer();
        Result result = new JUnitCore().run( computer, NotThreadSafeTest1.class, NormalTest1.class );
        assertTrue( result.wasSuccessful() );
        assertThat( result.getRunCount(), is( 2 ) );
        assertNotNull( NotThreadSafeTest1.t );
        assertNotNull( NormalTest1.t );
        assertThat( NormalTest1.t.getName(), is( not( "maven-surefire-plugin@NotThreadSafe" ) ) );
        assertNotSame( NotThreadSafeTest1.t, NormalTest1.t );
        assertThat( computer.getNotParallelRunners().size(), is( 1 ) );
        assertTrue( computer.getSuites().isEmpty() );
        assertThat( computer.getClasses().size(), is( 1 ) );
        assertTrue( computer.getNestedClasses().isEmpty() );
        assertTrue( computer.getNestedSuites().isEmpty() );
        assertFalse( computer.isSplitPool() );
        assertThat( computer.getPoolCapacity(), is( 6 ) );
    }

    @Test
    public void notThreadSafeTestsInSuite()
    {
        ParallelComputerBuilder builder = new ParallelComputerBuilder( logger )
            .useOnePool( 5 ).parallelMethods( 3 );
        assertFalse( builder.isOptimized() );
        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) builder.buildComputer();
        Result result = new JUnitCore().run( computer, NotThreadSafeTestSuite.class );
        assertTrue( result.wasSuccessful() );
        assertNotNull( NormalTest1.t );
        assertNotNull( NormalTest2.t );
        assertSame( NormalTest1.t, NormalTest2.t );
        assertThat( NormalTest1.t.getName(), is( "maven-surefire-plugin@NotThreadSafe" ) );
        assertThat( NormalTest2.t.getName(), is( "maven-surefire-plugin@NotThreadSafe" ) );
        assertThat( computer.getNotParallelRunners().size(), is( 1 ) );
        assertTrue( computer.getSuites().isEmpty() );
        assertTrue( computer.getClasses().isEmpty() );
        assertTrue( computer.getNestedClasses().isEmpty() );
        assertTrue( computer.getNestedSuites().isEmpty() );
        assertFalse( computer.isSplitPool() );
        assertThat( computer.getPoolCapacity(), is( 5 ) );
    }

    @Test
    public void mixedThreadSafetyInSuite()
    {
        ParallelComputerBuilder builder = new ParallelComputerBuilder( logger )
            .useOnePool( 10 ).optimize( true ).parallelSuites( 2 ).parallelClasses( 3 ).parallelMethods( 3 );
        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) builder.buildComputer();
        Result result = new JUnitCore().run( computer, MixedSuite.class );
        assertTrue( result.wasSuccessful() );
        assertThat( result.getRunCount(), is( 2 ) );
        assertNotNull( NotThreadSafeTest1.t );
        assertNotNull( NormalTest1.t );
        assertThat( NormalTest1.t.getName(), is( not( "maven-surefire-plugin@NotThreadSafe" ) ) );
        assertNotSame( NotThreadSafeTest1.t, NormalTest1.t );
        assertTrue( computer.getNotParallelRunners().isEmpty() );
        assertThat( computer.getSuites().size(), is( 1 ) );
        assertTrue( computer.getClasses().isEmpty() );
        assertThat( computer.getNestedClasses().size(), is( 1 ) );
        assertTrue( computer.getNestedSuites().isEmpty() );
        assertFalse( computer.isSplitPool() );
        assertThat( computer.getPoolCapacity(), is( 10 ) );
    }

    @Test
    public void inheritanceWithNotThreadSafe()
    {
        ParallelComputerBuilder builder = new ParallelComputerBuilder( logger )
            .useOnePool( 10 ).optimize( true ).parallelSuites( 2 ).parallelClasses( 3 ).parallelMethods( 3 );
        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) builder.buildComputer();
        Result result = new JUnitCore().run( computer, OverMixedSuite.class );
        assertTrue( result.wasSuccessful() );
        assertThat( result.getRunCount(), is( 2 ) );
        assertNotNull( NotThreadSafeTest3.t );
        assertNotNull( NormalTest1.t );
        assertThat( NormalTest1.t.getName(), is( "maven-surefire-plugin@NotThreadSafe" ) );
        assertSame( NotThreadSafeTest3.t, NormalTest1.t );
        assertThat( computer.getNotParallelRunners().size(), is( 1 ) );
        assertTrue( computer.getSuites().isEmpty() );
        assertTrue( computer.getClasses().isEmpty() );
        assertTrue( computer.getNestedClasses().isEmpty() );
        assertTrue( computer.getNestedSuites().isEmpty() );
        assertFalse( computer.isSplitPool() );
        assertThat( computer.getPoolCapacity(), is( 10 ) );
    }

    @Test
    public void beforeAfterThreadChanges()
        throws InterruptedException
    {
        // try to GC dead Thread objects from previous tests
        for ( int i = 0; i < 5; i++ )
        {
            System.gc();
            TimeUnit.MILLISECONDS.sleep( 500 );
        }
        Collection<Thread> expectedThreads = jvmThreads();
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder( logger );
        parallelComputerBuilder.parallelMethods( 3 );
        ParallelComputer computer = parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestWithBeforeAfter.class );
        assertTrue( result.wasSuccessful() );
        // try to GC dead Thread objects
        for ( int i = 0; i < 5 && expectedThreads.size() != jvmThreads().size(); i++ )
        {
            System.gc();
            TimeUnit.MILLISECONDS.sleep( 500 );
        }
        assertThat( jvmThreads(), is( expectedThreads ) );
    }

    private static Collection<Thread> jvmThreads()
    {
        Thread[] t = new Thread[1000];
        Thread.enumerate( t );
        ArrayList<Thread> appThreads = new ArrayList<Thread>( t.length );
        Collections.addAll( appThreads, t );
        appThreads.removeAll( Collections.singleton( (Thread) null ) );
        Collections.sort( appThreads, new Comparator<Thread>()
        {
            @Override
            public int compare( Thread t1, Thread t2 )
            {
                return (int) Math.signum( t1.getId() - t2.getId() );
            }
        } );
        return appThreads;
    }

    private static class ShutdownTest
    {
        Result run( final boolean useInterrupt )
        {
            ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder( logger )
                .useOnePool( 8 )
                .parallelSuites( 2 )
                .parallelClasses( 3 )
                .parallelMethods( 3 );

            assertFalse( parallelComputerBuilder.isOptimized() );

            final ParallelComputerBuilder.PC computer =
                (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
            shutdownTask = new Runnable()
            {
                @Override
                public void run()
                {
                    Collection<Description> startedTests = computer.describeStopped( useInterrupt ).getTriggeredTests();
                    assertThat( startedTests.size(), is( not( 0 ) ) );
                }
            };
            return new JUnitCore().run( computer, TestSuite.class, Class2.class, Class3.class );
        }
    }

    public static class Class1
    {
        static volatile int concurrentMethods = 0;

        static volatile int maxConcurrentMethods = 0;

        @Test
        public void test1()
            throws InterruptedException
        {
            synchronized ( class1Lock )
            {
                ++concurrentMethods;
                class1Lock.wait( 500 );
                maxConcurrentMethods = Math.max( maxConcurrentMethods, concurrentMethods-- );
            }
        }

        @Test
        public void test2()
            throws InterruptedException
        {
            test1();
            Runnable shutdownTask = ParallelComputerBuilderTest.shutdownTask;
            if ( shutdownTask != null )
            {
                beforeShutdown = true;
                shutdownTask.run();
            }
        }
    }

    public static class Class2
        extends Class1
    {
    }

    public static class Class3
        extends Class1
    {
    }

    public static class NothingDoingTest1
    {
        static final Collection<String> methods = new ConcurrentLinkedQueue<String>();

        @BeforeClass
        public static void init()
        {
            methods.add( "init" );
        }

        @AfterClass
        public static void deinit()
        {
            methods.add( "deinit" );
        }

        @Test
        public void a()
            throws InterruptedException
        {
            Thread.sleep( 5 );
            methods.add( getClass().getName() + "#a()" );
        }

        @Test
        public void b()
            throws InterruptedException
        {
            Thread.sleep( 5 );
            methods.add( getClass().getName() + "#b()" );
        }
    }

    public static class NothingDoingTest2
        extends NothingDoingTest1
    {
    }

    public static class NothingDoingTest3
        extends NothingDoingTest1
    {
    }

    @RunWith( Suite.class )
    @Suite.SuiteClasses( { NothingDoingTest1.class, NothingDoingTest2.class } )
    public static class NothingDoingSuite
    {
    }

    @RunWith( Suite.class )
    @Suite.SuiteClasses( { Class2.class, Class1.class } )
    public static class TestSuite
    {
    }

    @NotThreadSafe
    public static class NotThreadSafeTest1
    {
        static volatile Thread t;

        @BeforeClass
        public static void beforeSuite()
        {
            assertThat( Thread.currentThread().getName(), is( not( "maven-surefire-plugin@NotThreadSafe" ) ) );
        }

        @AfterClass
        public static void afterSuite()
        {
            assertThat( Thread.currentThread().getName(), is( not( "maven-surefire-plugin@NotThreadSafe" ) ) );
        }

        @Test
        public void test()
        {
            t = Thread.currentThread();
            assertThat( t.getName(), is( "maven-surefire-plugin@NotThreadSafe" ) );
        }
    }

    @NotThreadSafe
    public static class NotThreadSafeTest2
    {
        static volatile Thread t;

        @BeforeClass
        public static void beforeSuite()
        {
            assertThat( Thread.currentThread().getName(), is( not( "maven-surefire-plugin@NotThreadSafe" ) ) );
        }

        @AfterClass
        public static void afterSuite()
        {
            assertThat( Thread.currentThread().getName(), is( not( "maven-surefire-plugin@NotThreadSafe" ) ) );
        }

        @Test
        public void test()
        {
            t = Thread.currentThread();
            assertThat( t.getName(), is( "maven-surefire-plugin@NotThreadSafe" ) );
        }
    }

    @NotThreadSafe
    public static class NotThreadSafeTest3
    {
        static volatile Thread t;

        @Test
        public void test()
        {
            t = Thread.currentThread();
            assertThat( t.getName(), is( "maven-surefire-plugin@NotThreadSafe" ) );
        }
    }

    @RunWith( Suite.class )
    @Suite.SuiteClasses( { NormalTest1.class, NormalTest2.class } )
    @NotThreadSafe
    public static class NotThreadSafeTestSuite
    {
        @BeforeClass
        public static void beforeSuite()
        {
            assertThat( Thread.currentThread().getName(), is( not( "maven-surefire-plugin@NotThreadSafe" ) ) );
        }

        @AfterClass
        public static void afterSuite()
        {
            assertThat( Thread.currentThread().getName(), is( not( "maven-surefire-plugin@NotThreadSafe" ) ) );
        }
    }

    public static class NormalTest1
    {
        static volatile Thread t;

        @Test
        public void test()
        {
            t = Thread.currentThread();
        }
    }

    public static class NormalTest2
    {
        static volatile Thread t;

        @Test
        public void test()
        {
            t = Thread.currentThread();
        }
    }

    @RunWith( Suite.class )
    @Suite.SuiteClasses( { NotThreadSafeTest1.class, NormalTest1.class } )
    public static class MixedSuite
    {
        @BeforeClass
        public static void beforeSuite()
        {
            assertThat( Thread.currentThread().getName(), is( not( "maven-surefire-plugin@NotThreadSafe" ) ) );
        }

        @AfterClass
        public static void afterSuite()
        {
            assertThat( Thread.currentThread().getName(), is( not( "maven-surefire-plugin@NotThreadSafe" ) ) );
        }
    }

    @RunWith( Suite.class )
    @Suite.SuiteClasses( { NotThreadSafeTest3.class, NormalTest1.class } )
    @NotThreadSafe
    public static class OverMixedSuite
    {
        @BeforeClass
        public static void beforeSuite()
        {
            assertThat( Thread.currentThread().getName(), is( not( "maven-surefire-plugin@NotThreadSafe" ) ) );
        }

        @AfterClass
        public static void afterSuite()
        {
            assertThat( Thread.currentThread().getName(), is( not( "maven-surefire-plugin@NotThreadSafe" ) ) );
        }
    }

    public static class TestWithBeforeAfter
    {
        @BeforeClass
        public static void beforeClass()
            throws InterruptedException
        {
            System.out.println( new Date() + " BEG: beforeClass" );
            TimeUnit.SECONDS.sleep( 1 );
            System.out.println( new Date() + " END: beforeClass" );
        }

        @Before
        public void before()
            throws InterruptedException
        {
            System.out.println( new Date() + " BEG: before" );
            TimeUnit.SECONDS.sleep( 1 );
            System.out.println( new Date() + " END: before" );
        }

        @Test
        public void test()
            throws InterruptedException
        {
            System.out.println( new Date() + " BEG: test" );
            TimeUnit.SECONDS.sleep( 1 );
            System.out.println( new Date() + " END: test" );
        }

        @After
        public void after()
            throws InterruptedException
        {
            System.out.println( new Date() + " BEG: after" );
            TimeUnit.SECONDS.sleep( 1 );
            System.out.println( new Date() + " END: after" );
        }

        @AfterClass
        public static void afterClass()
            throws InterruptedException
        {
            System.out.println( new Date() + " BEG: afterClass" );
            TimeUnit.SECONDS.sleep( 1 );
            System.out.println( new Date() + " END: afterClass" );
        }
    }
}
