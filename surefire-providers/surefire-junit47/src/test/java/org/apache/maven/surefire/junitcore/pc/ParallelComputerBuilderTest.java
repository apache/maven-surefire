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

import org.apache.maven.surefire.junitcore.Stopwatch;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.apache.maven.surefire.junitcore.pc.RangeMatcher.between;

/**
 * @author Tibor Digana (tibor17)
 * @since 2.16
 */
public class ParallelComputerBuilderTest
{
    private static volatile boolean beforeShutdown;

    private static volatile Runnable shutdownTask;

    @Rule
    public final Stopwatch runtime = new Stopwatch();

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
    public void beforeTest()
    {
        Class1.maxConcurrentMethods = 0;
        Class1.concurrentMethods = 0;
        shutdownTask = null;
    }

    @Test
    public void parallelMethodsReuseOneOrTwoThreads()
    {
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder();
        parallelComputerBuilder.useOnePool( 4 );

        // One thread because one suite: TestSuite, however the capacity is 5.
        parallelComputerBuilder.parallelSuites( 5 );

        // Two threads because TestSuite has two classes, however the capacity is 5.
        parallelComputerBuilder.parallelClasses( 5 );

        // One or two threads because one threads comes from '#useOnePool(4)'
        // and next thread may be reused from finished class, however the capacity is 3.
        parallelComputerBuilder.parallelMethods( 3 );

        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestSuite.class );
        long timeSpent = runtime.stop();

        assertThat( computer.suites.size(), is( 1 ) );
        assertThat( computer.classes.size(), is( 0 ) );
        assertThat( computer.nestedClasses.size(), is( 2 ) );
        assertThat( computer.nestedSuites.size(), is( 0 ) );
        assertFalse( computer.splitPool );
        assertThat( computer.poolCapacity, is( 4 ) );
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
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder();
        parallelComputerBuilder.useOnePool( 5 );
        parallelComputerBuilder.parallelSuites( 5 );
        parallelComputerBuilder.parallelClasses( 5 );
        parallelComputerBuilder.parallelMethods( 3 );

        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestSuite.class, Class1.class );
        long timeSpent = runtime.stop();

        assertThat( computer.suites.size(), is( 1 ) );
        assertThat( computer.classes.size(), is( 1 ) );
        assertThat( computer.nestedClasses.size(), is( 2 ) );
        assertThat( computer.nestedSuites.size(), is( 0 ) );
        assertFalse( computer.splitPool );
        assertThat( computer.poolCapacity, is( 5 ) );
        assertTrue( result.wasSuccessful() );
        assertThat( Class1.maxConcurrentMethods, is( 2 ) );
        assertThat( timeSpent, anyOf( between( 1450, 1750 ), between( 1950, 2250 ), between( 2450, 2750 ) ) );
    }

    @Test
    public void onePoolWithUnlimitedParallelMethods()
    {
        // see ParallelComputerBuilder Javadoc
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder();
        parallelComputerBuilder.useOnePool( 8 );
        parallelComputerBuilder.parallelSuites( 2 );
        parallelComputerBuilder.parallelClasses( 4 );
        parallelComputerBuilder.parallelMethods();

        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestSuite.class, Class1.class );
        long timeSpent = runtime.stop();

        assertThat( computer.suites.size(), is( 1 ) );
        assertThat( computer.classes.size(), is( 1 ) );
        assertThat( computer.nestedClasses.size(), is( 2 ) );
        assertThat( computer.nestedSuites.size(), is( 0 ) );
        assertFalse( computer.splitPool );
        assertThat( computer.poolCapacity, is( 8 ) );
        assertTrue( result.wasSuccessful() );
        assertThat( Class1.maxConcurrentMethods, is( 4 ) );
        assertThat( timeSpent, between( 950, 1250 ) );
    }

    @Test
    public void underflowParallelism()
    {
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder();
        parallelComputerBuilder.useOnePool( 3 );

        // One thread because one suite: TestSuite.
        parallelComputerBuilder.parallelSuites( 5 );

        // One thread because of the limitation which is bottleneck.
        parallelComputerBuilder.parallelClasses( 1 );

        // One thread remains from '#useOnePool(3)'.
        parallelComputerBuilder.parallelMethods( 3 );

        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestSuite.class );
        long timeSpent = runtime.stop();

        assertThat( computer.suites.size(), is( 1 ) );
        assertThat( computer.classes.size(), is( 0 ) );
        assertThat( computer.nestedClasses.size(), is( 2 ) );
        assertThat( computer.nestedSuites.size(), is( 0 ) );
        assertFalse( computer.splitPool );
        assertThat( computer.poolCapacity, is( 3 ) );
        assertTrue( result.wasSuccessful() );
        assertThat( Class1.maxConcurrentMethods, is( 1 ) );
        assertThat( timeSpent, between( 1950, 2250 ) );
    }

    @Test
    public void separatePoolsWithSuite()
    {
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder();
        parallelComputerBuilder.parallelSuites( 5 );
        parallelComputerBuilder.parallelClasses( 5 );
        parallelComputerBuilder.parallelMethods( 3 );

        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestSuite.class );
        long timeSpent = runtime.stop();

        assertThat( computer.suites.size(), is( 1 ) );
        assertThat( computer.classes.size(), is( 0 ) );
        assertThat( computer.nestedClasses.size(), is( 2 ) );
        assertThat( computer.nestedSuites.size(), is( 0 ) );
        assertTrue( computer.splitPool );
        assertThat( computer.poolCapacity, is( ParallelComputerBuilder.TOTAL_POOL_SIZE_UNDEFINED ) );
        assertTrue( result.wasSuccessful() );
        assertThat( Class1.maxConcurrentMethods, is( 3 ) );
        assertThat( timeSpent, between( 950, 1250 ) );
    }

    @Test
    public void separatePoolsWithSuiteAndClass()
    {
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder();
        parallelComputerBuilder.parallelSuites( 5 );
        parallelComputerBuilder.parallelClasses( 5 );
        parallelComputerBuilder.parallelMethods( 3 );

        // 6 methods altogether.
        // 2 groups with 3 threads.
        // Each group takes 0.5s.
        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestSuite.class, Class1.class );
        long timeSpent = runtime.stop();

        assertThat( computer.suites.size(), is( 1 ) );
        assertThat( computer.classes.size(), is( 1 ) );
        assertThat( computer.nestedClasses.size(), is( 2 ) );
        assertThat( computer.nestedSuites.size(), is( 0 ) );
        assertTrue( computer.splitPool );
        assertThat( computer.poolCapacity, is( ParallelComputerBuilder.TOTAL_POOL_SIZE_UNDEFINED ) );
        assertTrue( result.wasSuccessful() );
        assertThat( Class1.maxConcurrentMethods, is( 3 ) );
        assertThat( timeSpent, between( 950, 1250 ) );
    }

    @Test
    public void separatePoolsWithSuiteAndSequentialClasses()
    {
        ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder();
        parallelComputerBuilder.parallelSuites( 5 );
        parallelComputerBuilder.parallelClasses( 1 );
        parallelComputerBuilder.parallelMethods( 3 );

        ParallelComputerBuilder.PC computer = (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
        Result result = new JUnitCore().run( computer, TestSuite.class, Class1.class );
        long timeSpent = runtime.stop();

        assertThat( computer.suites.size(), is( 1 ) );
        assertThat( computer.classes.size(), is( 1 ) );
        assertThat( computer.nestedClasses.size(), is( 2 ) );
        assertThat( computer.nestedSuites.size(), is( 0 ) );
        assertTrue( computer.splitPool );
        assertThat( computer.poolCapacity, is( ParallelComputerBuilder.TOTAL_POOL_SIZE_UNDEFINED ) );
        assertTrue( result.wasSuccessful() );
        assertThat( Class1.maxConcurrentMethods, is( 2 ) );
        assertThat( timeSpent, between( 1450, 1750 ) );
    }

    @Test( timeout = 2000 )
    public void shutdown()
    {
        Result result = new ShutdownTest().run( false );
        long timeSpent = runtime.stop();
        assertTrue( result.wasSuccessful() );
        assertTrue( beforeShutdown );
        assertThat( timeSpent, between( 450, 1250 ) );
    }

    @Test( timeout = 2000 )
    public void shutdownWithInterrupt()
    {
        new ShutdownTest().run( true );
        long timeSpent = runtime.stop();
        assertTrue( beforeShutdown );
        assertThat( timeSpent, between( 450, 1250 ) );
    }

    @Test
    public void nothingParallel()
    {
        JUnitCore core = new JUnitCore();
        ParallelComputerBuilder builder = new ParallelComputerBuilder();

        Result result = core.run( builder.buildComputer(), NothingDoingTest1.class, NothingDoingTest2.class );
        assertTrue( result.wasSuccessful() );

        result = core.run( builder.buildComputer(), NothingDoingTest1.class, NothingDoingSuite.class );
        assertTrue( result.wasSuccessful() );

        result = core.run( builder.useOnePool( 1 ).buildComputer(), NothingDoingTest1.class, NothingDoingTest2.class );
        assertTrue( result.wasSuccessful() );

        result = core.run( builder.useOnePool( 1 ).buildComputer(), NothingDoingTest1.class, NothingDoingSuite.class );
        assertTrue( result.wasSuccessful() );

        result = core.run( builder.useOnePool( 2 ).buildComputer(), NothingDoingTest1.class, NothingDoingSuite.class );
        assertTrue( result.wasSuccessful() );

        Class<?>[] classes = { NothingDoingTest1.class, NothingDoingSuite.class };

        result = core.run( builder.useOnePool( 2 ).parallelSuites( 1 ).parallelClasses( 1 ).buildComputer(), classes );
        assertTrue( result.wasSuccessful() );

        result = core.run( builder.useOnePool( 2 ).parallelSuites( 1 ).parallelClasses().buildComputer(), classes );
        assertTrue( result.wasSuccessful() );

        classes = new Class<?>[]{ NothingDoingSuite.class, NothingDoingSuite.class, NothingDoingTest1.class,
            NothingDoingTest2.class, NothingDoingTest3.class };

        result = core.run( builder.useOnePool( 2 ).parallelSuites( 1 ).parallelClasses( 1 ).buildComputer(), classes );
        assertTrue( result.wasSuccessful() );

        result = core.run( builder.useOnePool( 2 ).parallelSuites( 1 ).parallelClasses().buildComputer(), classes );
        assertTrue( result.wasSuccessful() );
    }

    @Test
    public void keepBeforeAfterOneClass()
    {
        ParallelComputerBuilder builder = new ParallelComputerBuilder();
        builder.parallelMethods();
        testKeepBeforeAfter( builder, NothingDoingTest1.class );
    }

    @Test
    public void keepBeforeAfterTwoClasses()
    {
        ParallelComputerBuilder builder = new ParallelComputerBuilder();
        builder.useOnePool( 5 ).parallelClasses( 1 ).parallelMethods( 2 );
        testKeepBeforeAfter( builder, NothingDoingTest1.class, NothingDoingTest2.class );
    }

    @Test
    public void keepBeforeAfterTwoParallelClasses()
    {
        ParallelComputerBuilder builder = new ParallelComputerBuilder();
        builder.useOnePool( 8 ).parallelClasses( 2 ).parallelMethods( 2 );
        JUnitCore core = new JUnitCore();
        NothingDoingTest1.methods.clear();
        Class<?>[] classes = { NothingDoingTest1.class, NothingDoingTest2.class, NothingDoingTest3.class };
        Result result = core.run( builder.buildComputer(), classes );
        assertTrue( result.wasSuccessful() );
        ArrayList<String> methods = new ArrayList<String>( NothingDoingTest1.methods );
        assertThat( methods.size(), is( 12 ) );
        assertThat( methods.subList( 9, 12 ), is( not( Arrays.asList( "deinit", "deinit", "deinit" ) ) ) );
    }

    private static class ShutdownTest
    {
        Result run( final boolean useInterrupt )
        {
            ParallelComputerBuilder parallelComputerBuilder = new ParallelComputerBuilder().useOnePool( 8 );
            parallelComputerBuilder.parallelSuites( 2 );
            parallelComputerBuilder.parallelClasses( 3 );
            parallelComputerBuilder.parallelMethods( 3 );

            final ParallelComputerBuilder.PC computer =
                (ParallelComputerBuilder.PC) parallelComputerBuilder.buildComputer();
            shutdownTask = new Runnable()
            {
                public void run()
                {
                    Collection<org.junit.runner.Description> startedTests = computer.shutdown( useInterrupt );
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
            synchronized ( Class1.class )
            {
                ++concurrentMethods;
                Class1.class.wait( 500 );
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
    public class TestSuite
    {
    }
}
