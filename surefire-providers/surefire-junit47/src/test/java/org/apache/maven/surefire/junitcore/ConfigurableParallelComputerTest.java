package org.apache.maven.surefire.junitcore;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

/**
 * Simple test of ConfigurableParallelComputer.
 *
 * @author Kristian Rosenvold
 */
public class ConfigurableParallelComputerTest
    extends TestCase
{
    private static final int NUMTESTS = 1000;

    // I'm sorry about all the sout's in this test; but if you deadlock when building you will appreciate it.

    @Test
    public void testAnythingYouWantToPlayWith()
        throws Exception
    {
        Result result = new Result();
        Class<?>[] realClasses = new Class[]{ Dummy.class, Dummy2.class };

        DiagnosticRunListener diagnosticRunListener = new DiagnosticRunListener( false, result.createListener() );
        JUnitCore jUnitCore = getJunitCore( diagnosticRunListener );
        ConfigurableParallelComputer computer = new ConfigurableParallelComputer( true, false );
        jUnitCore.run( computer, realClasses );
        computer.close();
        assertEquals( "All tests should succeed, right ?", 5, result.getRunCount() );
    }

    @Test
    public void testOneMethod()
        throws ExecutionException
    {
        JUnitCore jUnitCore = new JUnitCore();
        ConfigurableParallelComputer computer = new ConfigurableParallelComputer( false, true );
        jUnitCore.run( computer, new Class[]{ Dummy.class, Dummy.class, Dummy.class } );
        computer.close();
    }

    @Test
    public void testSerial()
        throws Exception
    {
        Result result = new Result();
        Class<?>[] realClasses = getClassList();
        JUnitCore jUnitCore = getJunitCore( result );
        Computer computer = new Computer();
        timedRun( NUMTESTS, result, realClasses, jUnitCore, computer );
    }


    @Test
    public void testFullTestRunPC()
        throws Exception
    {
        Result result = new Result();
        Class<?>[] realClasses = getClassList();
        JUnitCore jUnitCore = getJunitCore( result );
        Computer computer = new ConfigurableParallelComputer( true, true );
        timedRun( NUMTESTS, result, realClasses, jUnitCore, computer );
    }

    @Test
    public void testWithFailingAssertionCPC()
        throws Exception
    {
        runWithFailingAssertion( new ConfigurableParallelComputer( false, true, 6, true ) );
        runWithFailingAssertion( new ConfigurableParallelComputer( true, false, 12, false ) );
        runWithFailingAssertion( new ConfigurableParallelComputer( true, true, 2, false ) );
    }

    @Test
    public void testWithSlowTestJustAfew()
        throws Exception
    {
        Result result = new Result();
        final Computer computer = new ConfigurableParallelComputer( false, true, 3, false );
        Class<?>[] realClasses = getClassList( SlowTest.class, 5 ); // 300 ms in methods, 600 in classes

        JUnitCore jUnitCore = getJunitCore( result );
        runIt( realClasses, jUnitCore, computer );
    }


    private void runWithFailingAssertion( Computer computer )
        throws ExecutionException
    {
        Result result = new Result();
        Class<?>[] realClasses = getClassList( FailingAssertions.class );
        JUnitCore jUnitCore = getJunitCore( result );
        runIt( realClasses, jUnitCore, computer );
        assertEquals( "No tests should fail, right ?", NUMTESTS, result.getFailures().size() );
        assertEquals( "All tests should succeed, right ?", 0, result.getIgnoreCount() );
        assertEquals( "All tests should succeed, right ?", NUMTESTS * 3, result.getRunCount() );
    }

    @Test
    public void testWithFailure()
        throws Exception
    {
        Computer computer = new ConfigurableParallelComputer( false, true, 4, true );
        Result result = new Result();
        Class<?>[] realClasses = getClassList( Failure.class );
        JUnitCore jUnitCore = getJunitCore( result );
        runIt( realClasses, jUnitCore, computer );
        assertEquals( "No tests should fail, right ?", NUMTESTS, result.getFailures().size() );
        assertEquals( "All tests should succeed, right ?", 0, result.getIgnoreCount() );
        assertEquals( "All tests should succeed, right ?", NUMTESTS * 3, result.getRunCount() );
    }

    @Test
    public void testFixedThreadPool()
        throws Exception
    {
        Result result = new Result();
        Class<?>[] realClasses = getClassList();
        JUnitCore jUnitCore = getJunitCore( result );
        ConfigurableParallelComputer computer = new ConfigurableParallelComputer( false, true, 2, false );
        timedRun( NUMTESTS, result, realClasses, jUnitCore, computer );
    }

    @Test
    public void testClassesUnlimited()
        throws Exception
    {
        Result result = new Result();
        Class<?>[] realClasses = getClassList();
        JUnitCore jUnitCore = getJunitCore( result );
        ConfigurableParallelComputer computer = new ConfigurableParallelComputer( true, false );
        timedRun( NUMTESTS, result, realClasses, jUnitCore, computer );
    }

    @Test
    public void testBothUnlimited()
        throws Exception
    {
        Result result = new Result();
        Class<?>[] realClasses = getClassList();
        DiagnosticRunListener diagnosticRunListener = new DiagnosticRunListener( false, result.createListener() );
        JUnitCore jUnitCore = getJunitCore( diagnosticRunListener );
        ConfigurableParallelComputer computer = new ConfigurableParallelComputer( true, true );
        timedRun( NUMTESTS, result, realClasses, jUnitCore, computer );
    }

    private JUnitCore getJunitCore( Result result )
    {
        RunListener listener = result.createListener();
        JUnitCore jUnitCore = new JUnitCore();
        jUnitCore.addListener( listener );
        return jUnitCore;
    }

    private JUnitCore getJunitCore( RunListener listener )
    {
        JUnitCore jUnitCore = new JUnitCore();
        jUnitCore.addListener( listener );
        return jUnitCore;
    }

    private long runIt( Class<?>[] realClasses, JUnitCore jUnitCore, Computer computer )
        throws ExecutionException
    {
        long start = System.currentTimeMillis();
        jUnitCore.run( computer, realClasses );
        if ( computer instanceof ConfigurableParallelComputer )
        {
            ( (ConfigurableParallelComputer) computer ).close();
        }
        return System.currentTimeMillis() - start;
    }

    private long timedRun( int NUMTESTS, Result result, Class<?>[] realClasses, JUnitCore jUnitCore, Computer computer )
        throws ExecutionException
    {
        long time = runIt( realClasses, jUnitCore, computer );
        assertEquals( "No tests should fail, right ?", 0, result.getFailures().size() );
        assertEquals( "All tests should succeed, right ?", 0, result.getIgnoreCount() );
        assertEquals( "All tests should succeed, right ?", NUMTESTS * 3, result.getRunCount() );
        return time;
    }

    private Class<?>[] getClassList()
    {
        return getClassList( Dummy.class, NUMTESTS );
    }

    private Class<?>[] getClassList( Class<?> testClass )
    {
        return getClassList( testClass, NUMTESTS );
    }

    private Class<?>[] getClassList( Class<?> testClass, int numItems )
    {
        List<Class<?>> realClasses = new ArrayList<>();
        for ( int i = 0; i < numItems; i++ )
        {
            realClasses.add( testClass );
        }
        return realClasses.toArray( new Class[realClasses.size()] );
    }

    static void sleepReallyEvenOnWindows( long ms )
        throws InterruptedException
    {
        long endAt = System.currentTimeMillis() + ms;
        Thread.sleep( ms );
        while ( endAt > System.currentTimeMillis() )
        {
            Thread.sleep( ms / 10 );
            Thread.yield();
        }
    }

    public static class Dummy
    {
        @Test
        public void testNotMuch()
        {
        }

        @Test
        public void testStub1()
        {
            // Add your code here
        }

        @Test
        public void testStub2()
        {
            // Add your code here
        }
    }

    public static class Dummy2
    {
        @Test
        public void testNotMuch()
        {

        }

        @Test
        public void testDummy2()
        {
            // Add your code here
        }
    }

    public static class SlowTest
    {
        final int scaling = 100;

        @Test
        public void testNotMuch()
            throws InterruptedException
        {
            sleepReallyEvenOnWindows( scaling );
        }

        @Test
        public void testNotMuch2()
            throws InterruptedException
        {
            sleepReallyEvenOnWindows( 3 * scaling );
        }

        @Test
        public void testNotMuch3()
            throws InterruptedException
        {
            sleepReallyEvenOnWindows( 2 * scaling );
        }
    }

    public static class FailingAssertions
    {
        @Test
        public void testNotMuch()
        {
        }

        @Test
        public void testNotMuch2()
        {
        }

        @Test
        public void testWithFail()
        {
            fail( "We excpect this" );
        }
    }

    public static class Failure
    {
        @Test
        public void testNotMuch()
        {
        }

        @Test
        public void testNotMuch2()
        {
        }

        @Test
        public void testWithException()
        {
            throw new RuntimeException( "We expect this" );
        }
    }
}