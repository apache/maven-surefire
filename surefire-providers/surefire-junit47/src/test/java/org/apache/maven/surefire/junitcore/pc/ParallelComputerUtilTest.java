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

import org.apache.maven.surefire.junitcore.JUnitCoreParameters;
import org.apache.maven.surefire.report.ConsoleStream;
import org.apache.maven.surefire.report.DefaultDirectConsoleReporter;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.maven.surefire.junitcore.pc.ParallelComputerUtil.*;
import static org.apache.maven.surefire.junitcore.JUnitCoreParameters.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Testing an algorithm in {@link ParallelComputerUtil} which configures
 * allocated thread resources in ParallelComputer by given {@link org.apache.maven.surefire.junitcore.JUnitCoreParameters}.
 *
 * @author Tibor Digana (tibor17)
 * @see ParallelComputerUtil
 * @since 2.16
 */
@RunWith( Theories.class )
public final class ParallelComputerUtilTest
{
    private final ConsoleStream logger = new DefaultDirectConsoleReporter( System.out );

    @DataPoint
    public static final int CPU_1 = 1;

    @DataPoint
    public static final int CPU_4 = 4;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @BeforeClass
    public static void beforeClass()
    {
        ParallelComputerUtil.overrideAvailableProcessors( 1 );
    }

    @AfterClass
    public static void afterClass()
    {
        ParallelComputerUtil.setDefaultAvailableProcessors();
    }

    @Before
    public void beforeTest()
        throws InterruptedException
    {
        System.gc();
        Thread.sleep( 50L );
        assertFalse( Thread.currentThread().isInterrupted() );
    }

    private static Map<String, String> parallel( String parallel )
    {
        return Collections.singletonMap( PARALLEL_KEY, parallel );
    }

    @Test
    public void unknownParallel()
        throws TestSetFailedException
    {
        Map<String, String> properties = new HashMap<>();
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( new JUnitCoreParameters( properties ), null );
    }

    @Test
    public void unknownThreadCountSuites()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "suites" ) );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params, null );
    }

    @Test
    public void unknownThreadCountClasses()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "classes" ) );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params, null );
    }

    @Test
    public void unknownThreadCountMethods()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "methods" ) );
        assertFalse( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params, null );
    }

    @Test
    public void unknownThreadCountBoth()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "both" ) );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params, null );
    }

    @Test
    public void unknownThreadCountAll()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "all" ) );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params, null );
    }

    @Test
    public void unknownThreadCountSuitesAndClasses()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "suitesAndClasses" ) );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params, null );
    }

    @Test
    public void unknownThreadCountSuitesAndMethods()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "suitesAndMethods" ) );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params, null );
    }

    @Test
    public void unknownThreadCountClassesAndMethods()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "classesAndMethods" ) );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params, null );
    }

    @Theory
    public void useUnlimitedThreadsSuites( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "suites");
        properties.put(USEUNLIMITEDTHREADS_KEY, "true");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 0 ) );

        properties.put(THREADCOUNTSUITES_KEY, "5");
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void useUnlimitedThreadsClasses( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "classes");
        properties.put(USEUNLIMITEDTHREADS_KEY, "true");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.methods, is( 0 ) );

        properties.put(THREADCOUNTCLASSES_KEY, "5");
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 5 * cpu ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void unlimitedThreadsMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "methods");
        properties.put(USEUNLIMITEDTHREADS_KEY, "true");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );

        properties.put(THREADCOUNTMETHODS_KEY, "5");
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 5 * cpu ) );
    }

    @Theory
    public void unlimitedThreadsSuitesAndClasses( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "suitesAndClasses");
        properties.put(USEUNLIMITEDTHREADS_KEY, "true");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.classes, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.methods, is( 0 ) );

        properties.put(THREADCOUNTSUITES_KEY, "5");
        properties.put(THREADCOUNTCLASSES_KEY, "15");
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 15 * cpu ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void unlimitedThreadsSuitesAndMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "suitesAndMethods");
        properties.put(USEUNLIMITEDTHREADS_KEY, "true");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );

        properties.put(THREADCOUNTSUITES_KEY, "5");
        properties.put(THREADCOUNTMETHODS_KEY, "15");
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 15 * cpu ) );
    }

    @Theory
    public void unlimitedThreadsClassesAndMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "classesAndMethods");
        properties.put(USEUNLIMITEDTHREADS_KEY, "true");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );

        properties.put(THREADCOUNTCLASSES_KEY, "5");
        properties.put(THREADCOUNTMETHODS_KEY, "15");
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 5 * cpu ) );
        assertThat( concurrency.methods, is( 15 * cpu ) );
    }

    @Theory
    public void unlimitedThreadsAll( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "all");
        properties.put(USEUNLIMITEDTHREADS_KEY, "true");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.classes, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );

        properties.put(THREADCOUNTSUITES_KEY, "5");
        properties.put(THREADCOUNTCLASSES_KEY, "15");
        properties.put(THREADCOUNTMETHODS_KEY, "30");
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 15 * cpu ) );
        assertThat( concurrency.methods, is( 30 * cpu ) );
    }

    @Theory
    public void threadCountSuites( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "suites");
        properties.put(THREADCOUNT_KEY, "3");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 0 ) );
        assertThat( concurrency.suites, is( 3 * cpu ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void threadCountClasses( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "classes");
        properties.put(THREADCOUNT_KEY, "3");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 0 ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 3 * cpu ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void threadCountMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "methods");
        properties.put(THREADCOUNT_KEY, "3");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 0 ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 3 * cpu ) );
    }

    @Theory
    public void threadCountBoth( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "both");
        properties.put(THREADCOUNT_KEY, "3");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( (int) ( ( 3d / 2 ) * cpu ) ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void threadCountClassesAndMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "classesAndMethods");
        properties.put(THREADCOUNT_KEY, "3");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( (int) ( ( 3d / 2 ) * cpu ) ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void threadCountSuitesAndMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "suitesAndMethods");
        properties.put(THREADCOUNT_KEY, "3");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( (int) ( ( 3d / 2 ) * cpu ) ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void threadCountSuitesAndClasses( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "suitesAndClasses");
        properties.put(THREADCOUNT_KEY, "3");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( (int) ( ( 3d / 2 ) * cpu ) ) );
        assertThat( concurrency.classes, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void threadCountAll( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "all");
        properties.put(THREADCOUNT_KEY, "3");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( cpu ) );
        assertThat( concurrency.classes, is( cpu ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void everyThreadCountSuitesAndClasses( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "suitesAndClasses");
        properties.put(THREADCOUNT_KEY, "3");
        // % percentage ratio
        properties.put(THREADCOUNTSUITES_KEY, "34");
        properties.put(THREADCOUNTCLASSES_KEY, "66");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        int concurrentSuites = (int) ( 0.34d * concurrency.capacity );
        assertThat( concurrency.suites, is( concurrentSuites ) );
        assertThat( concurrency.classes, is( concurrency.capacity - concurrentSuites ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void everyThreadCountSuitesAndMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "suitesAndMethods");
        properties.put(THREADCOUNT_KEY, "3");
        // % percentage ratio
        properties.put(THREADCOUNTSUITES_KEY, "34");
        properties.put(THREADCOUNTMETHODS_KEY, "66");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        int concurrentSuites = (int) ( 0.34d * concurrency.capacity );
        assertThat( concurrency.suites, is( concurrentSuites ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( concurrency.capacity - concurrentSuites ) );
    }

    @Theory
    public void everyThreadCountClassesAndMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "classesAndMethods");
        properties.put(THREADCOUNT_KEY, "3");
        // % percentage ratio
        properties.put(THREADCOUNTCLASSES_KEY, "34");
        properties.put(THREADCOUNTMETHODS_KEY, "66");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        int concurrentClasses = (int) ( 0.34d * concurrency.capacity );
        assertThat( concurrency.classes, is( concurrentClasses ) );
        assertThat( concurrency.methods, is( concurrency.capacity - concurrentClasses ) );
    }

    @Theory
    public void everyThreadCountAll( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "all");
        properties.put(THREADCOUNT_KEY, "3");
        // % percentage ratio
        properties.put(THREADCOUNTSUITES_KEY, "17");
        properties.put(THREADCOUNTCLASSES_KEY, "34");
        properties.put(THREADCOUNTMETHODS_KEY, "49");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        int concurrentSuites = (int) ( 0.17d * concurrency.capacity );
        int concurrentClasses = (int) ( 0.34d * concurrency.capacity );
        assertThat( concurrency.suites, is( concurrentSuites ) );
        assertThat( concurrency.classes, is( concurrentClasses ) );
        assertThat( concurrency.methods, is( concurrency.capacity - concurrentSuites - concurrentClasses ) );
    }

    @Theory
    public void reusableThreadCountSuitesAndClasses( int cpu )
        throws TestSetFailedException
    {
        // 4 * cpu to 5 * cpu threads to run test classes
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "suitesAndClasses");
        properties.put(THREADCOUNT_KEY, "6");
        properties.put(THREADCOUNTSUITES_KEY, "2");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 6 * cpu ) );
        assertThat( concurrency.suites, is( 2 * cpu ) );
        assertThat( concurrency.classes, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void reusableThreadCountSuitesAndMethods( int cpu )
        throws TestSetFailedException
    {
        // 4 * cpu to 5 * cpu threads to run test methods
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "suitesAndMethods");
        properties.put(THREADCOUNT_KEY, "6");
        properties.put(THREADCOUNTSUITES_KEY, "2");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 6 * cpu ) );
        assertThat( concurrency.suites, is( 2 * cpu ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void reusableThreadCountClassesAndMethods( int cpu )
        throws TestSetFailedException
    {
        // 4 * cpu to 5 * cpu threads to run test methods
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "classesAndMethods");
        properties.put(THREADCOUNT_KEY, "6");
        properties.put(THREADCOUNTCLASSES_KEY, "2");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 6 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 2 * cpu ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void reusableThreadCountAll( int cpu )
        throws TestSetFailedException
    {
        // 8 * cpu to 13 * cpu threads to run test methods
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "all");
        properties.put(THREADCOUNT_KEY, "14");
        properties.put(THREADCOUNTSUITES_KEY, "2");
        properties.put(THREADCOUNTCLASSES_KEY, "4");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 14 * cpu ) );
        assertThat( concurrency.suites, is( 2 * cpu ) );
        assertThat( concurrency.classes, is( 4 * cpu ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void suites( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "suites");
        properties.put(THREADCOUNTSUITES_KEY, "5");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 5 * cpu ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void classes( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "classes");
        properties.put(THREADCOUNTCLASSES_KEY, "5");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 5 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 5 * cpu ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void methods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "methods");
        properties.put(THREADCOUNTMETHODS_KEY, "5");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 5 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 5 * cpu ) );
    }

    @Theory
    public void suitesAndClasses( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();

        properties.put(PARALLEL_KEY, "suitesAndClasses");
        properties.put(THREADCOUNTSUITES_KEY, "5");
        properties.put(THREADCOUNTCLASSES_KEY, "15");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 20 * cpu ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 15 * cpu ) );
        assertThat( concurrency.methods, is( 0 ) );

        // Warning: this case works but is not enabled in AbstractSurefireMojo
        // Instead use the 'useUnlimitedThreads' parameter.
        properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "suitesAndClasses");
        properties.put(THREADCOUNTSUITES_KEY, "5");
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void suitesAndMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();

        properties.put(PARALLEL_KEY, "suitesAndMethods");
        properties.put(THREADCOUNTSUITES_KEY, "5");
        properties.put(THREADCOUNTMETHODS_KEY, "15");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 20 * cpu ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 15 * cpu ) );

        // Warning: this case works but is not enabled in AbstractSurefireMojo
        // Instead use the 'useUnlimitedThreads' parameter.
        properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "suitesAndMethods");
        properties.put(THREADCOUNTSUITES_KEY, "5");
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void classesAndMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();

        properties.put(PARALLEL_KEY, "classesAndMethods");
        properties.put(THREADCOUNTCLASSES_KEY, "5");
        properties.put(THREADCOUNTMETHODS_KEY, "15");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 20 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 5 * cpu ) );
        assertThat( concurrency.methods, is( 15 * cpu ) );

        // Warning: this case works but is not enabled in AbstractSurefireMojo
        // Instead use the 'useUnlimitedThreads' parameter.
        properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "classesAndMethods");
        properties.put(THREADCOUNTCLASSES_KEY, "5");
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params, null );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 5 * cpu ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void all( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerUtil.overrideAvailableProcessors( cpu );
        Map<String, String> properties = new HashMap<>();

        properties.put(PARALLEL_KEY, "all");
        properties.put(THREADCOUNTSUITES_KEY, "5");
        properties.put(THREADCOUNTCLASSES_KEY, "15");
        properties.put(THREADCOUNTMETHODS_KEY, "30");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 50 * cpu ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 15 * cpu ) );
        assertThat( concurrency.methods, is( 30 * cpu ) );

        // Warning: these cases work but they are not enabled in AbstractSurefireMojo
        // Instead use the 'useUnlimitedThreads' parameter.
        properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "all");
        properties.put(THREADCOUNTSUITES_KEY, "5");
        properties.put(THREADCOUNTCLASSES_KEY, "15");
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 15 * cpu ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );

        properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "all");
        properties.put(THREADCOUNTCLASSES_KEY, "15");
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params, null );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.classes, is( 15 * cpu ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Test
    public void withoutShutdown()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "methods");
        properties.put(THREADCOUNTMETHODS_KEY, "2");
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        ParallelComputerBuilder pcBuilder = new ParallelComputerBuilder( logger, params );
        ParallelComputer pc = pcBuilder.buildComputer();
        final JUnitCore core = new JUnitCore();
        final long t1 = systemMillis();
        final Result result = core.run( pc, TestClass.class );
        final long t2 = systemMillis();
        long timeSpent = t2 - t1;
        final long deltaTime = 500L;

        assertTrue( result.wasSuccessful() );
        assertThat( result.getRunCount(), is( 3 ) );
        assertThat( result.getFailureCount(), is( 0 ) );
        assertThat( result.getIgnoreCount(), is( 0 ) );
        //assertThat( timeSpent, between (timeSpent - deltaTime, timeSpent + deltaTime + 2000L ) );
        assertEquals( 10000L, timeSpent, deltaTime );
    }

    @Test
    public void shutdown()
        throws TestSetFailedException
    {
        // The JUnitCore returns after 2.5s.
        // The test-methods in TestClass are NOT interrupted, and return normally after 5s.
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "methods");
        properties.put(THREADCOUNTMETHODS_KEY, "2");
        properties.put(PARALLEL_TIMEOUT_KEY, Double.toString( 2.5d ));
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        ParallelComputerBuilder pcBuilder = new ParallelComputerBuilder( logger, params );
        ParallelComputer pc = pcBuilder.buildComputer();
        final JUnitCore core = new JUnitCore();
        final long t1 = systemMillis();
        core.run( pc, TestClass.class );
        final long t2 = systemMillis();
        final long timeSpent = t2 - t1;
        final long deltaTime = 500L;

        assertEquals( 5000L, timeSpent, deltaTime );
        String description = pc.describeElapsedTimeout();
        assertTrue( description.contains( "The test run has finished abruptly after timeout of 2.5 seconds.") );
        assertTrue( description.contains( "These tests were executed in prior to the shutdown operation:\n"
                + TestClass.class.getName() ) );
    }

    @Test
    public void forcedShutdown()
        throws TestSetFailedException
    {
        // The JUnitCore returns after 2.5s, and the test-methods in TestClass are interrupted.
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "methods");
        properties.put(THREADCOUNTMETHODS_KEY, "2");
        properties.put(PARALLEL_TIMEOUTFORCED_KEY, Double.toString( 2.5d ));
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        ParallelComputerBuilder pcBuilder = new ParallelComputerBuilder( logger, params );
        ParallelComputer pc = pcBuilder.buildComputer();
        final JUnitCore core = new JUnitCore();
        final long t1 = systemMillis();
        core.run( pc, TestClass.class );
        final long t2 = systemMillis();
        final long timeSpent = t2 - t1;
        final long deltaTime = 500L;

        assertEquals( 2500L, timeSpent, deltaTime );
        String description = pc.describeElapsedTimeout();
        assertTrue( description.contains( "The test run has finished abruptly after timeout of 2.5 seconds.") );
        assertTrue( description.contains( "These tests were executed in prior to the shutdown operation:\n"
                + TestClass.class.getName() ) );
    }

    @Test
    public void timeoutAndForcedShutdown()
        throws TestSetFailedException
    {
        // The JUnitCore returns after 3.5s and the test-methods in TestClass are timed out after 2.5s.
        // No new test methods are scheduled for execution after 2.5s.
        // Interruption of test methods after 3.5s.
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "methods");
        properties.put(THREADCOUNTMETHODS_KEY, "2");
        properties.put(PARALLEL_TIMEOUT_KEY, Double.toString( 2.5d ));
        properties.put(PARALLEL_TIMEOUTFORCED_KEY, Double.toString( 3.5d ));
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        ParallelComputerBuilder pcBuilder = new ParallelComputerBuilder( logger, params );
        ParallelComputer pc = pcBuilder.buildComputer();
        final JUnitCore core = new JUnitCore();
        final long t1 = systemMillis();
        core.run( pc, TestClass.class );
        final long t2 = systemMillis();
        final long timeSpent = t2 - t1;
        final long deltaTime = 500L;

        assertEquals( 3500L, timeSpent, deltaTime );
        String description = pc.describeElapsedTimeout();
        assertTrue( description.contains( "The test run has finished abruptly after timeout of 2.5 seconds.") );
        assertTrue( description.contains( "These tests were executed in prior to the shutdown operation:\n"
                                              + TestClass.class.getName() ) );
    }

    @Test
    public void forcedTimeoutAndShutdown()
        throws Exception
    {
        // The JUnitCore returns after 3.5s and the test-methods in TestClass are interrupted after 3.5s.
        Map<String, String> properties = new HashMap<>();
        properties.put(PARALLEL_KEY, "methods");
        properties.put(THREADCOUNTMETHODS_KEY, "2");
        properties.put(PARALLEL_TIMEOUTFORCED_KEY, Double.toString( 3.5d ) );
        properties.put(PARALLEL_TIMEOUT_KEY, Double.toString( 4.0d ) );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        ParallelComputerBuilder pcBuilder = new ParallelComputerBuilder( logger, params );
        ParallelComputer pc = pcBuilder.buildComputer();
        final JUnitCore core = new JUnitCore();
        final long t1 = systemMillis();
        core.run( pc, TestClass.class );
        final long t2 = systemMillis();
        final long timeSpent = t2 - t1;
        final long deltaTime = 500L;

        assertEquals( 3500L, timeSpent, deltaTime );
        String description = pc.describeElapsedTimeout();
        assertTrue( description.contains( "The test run has finished abruptly after timeout of 3.5 seconds.") );
        assertTrue( description.contains( "These tests were executed in prior to the shutdown operation:\n"
                                              + TestClass.class.getName() ) );
    }

    public static class TestClass
    {
        @Test
        public void a()
            throws InterruptedException
        {
            long t1 = systemMillis();
            try
            {
                Thread.sleep( 5000L );
            }
            finally
            {
                System.out.println( getClass().getSimpleName() + "#a() spent " + ( systemMillis() - t1 ) );
            }
        }

        @Test
        public void b()
            throws InterruptedException
        {
            long t1 = systemMillis();
            try
            {
                Thread.sleep( 5000L );
            }
            finally
            {
                System.out.println( getClass().getSimpleName() + "#b() spent " + ( systemMillis() - t1 ) );
            }
        }

        @Test
        public void c()
            throws InterruptedException
        {
            long t1 = systemMillis();
            try
            {
                Thread.sleep( 5000L );
            }
            finally
            {
                System.out.println( getClass().getSimpleName() + "#c() spent " + ( systemMillis() - t1 ) );
            }
        }
    }

    private static long systemMillis()
    {
        return TimeUnit.NANOSECONDS.toMillis( System.nanoTime() );
    }
}
