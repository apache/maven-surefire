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

import org.apache.maven.surefire.junitcore.pc.RunnerCounter;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.Properties;

import static org.apache.maven.surefire.junitcore.JUnitCoreParameters.*;
import static org.apache.maven.surefire.junitcore.ParallelComputerFactory.resolveConcurrency;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Testing an algorithm in {@link ParallelComputerFactory} which configures
 * optimized thread resources in ParallelComputer by given {@link JUnitCoreParameters}.
 *
 * @author Tibor Digana (tibor17)
 * @see org.apache.maven.surefire.junitcore.ParallelComputerFactory
 * @since 2.17
 */
@RunWith( Theories.class )
public final class OptimizedParallelComputerTest
{
    @DataPoint
    public static final int CPU_1 = 1;

    @DataPoint
    public static final int CPU_4 = 4;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Rule
    public final Stopwatch runtime = new Stopwatch();

    @BeforeClass
    public static void beforeClass()
    {
        ParallelComputerFactory.overrideAvailableProcessors( 1 );
    }

    @AfterClass
    public static void afterClass()
    {
        ParallelComputerFactory.setDefaultAvailableProcessors();
    }

    @Theory
    public void threadCountSuites( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suites" );
        properties.setProperty( THREADCOUNT_KEY, "3" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        RunnerCounter counter = new RunnerCounter( 5, 10, 20 );
        ParallelComputerFactory.Concurrency concurrency = resolveConcurrency( params, counter );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 0 ) );
        assertThat( concurrency.suites, is( (int) Math.min( 3 * cpu, counter.suites ) ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void threadCountClasses( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "classes" );
        properties.setProperty( THREADCOUNT_KEY, "3" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        RunnerCounter counter = new RunnerCounter( 1, 5, 10 );
        ParallelComputerFactory.Concurrency concurrency = resolveConcurrency( params, counter );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 0 ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( (int) Math.min( 3 * cpu, counter.classes ) ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void threadCountMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "methods" );
        properties.setProperty( THREADCOUNT_KEY, "3" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        RunnerCounter counter = new RunnerCounter( 1, 2, 5 );
        ParallelComputerFactory.Concurrency concurrency = resolveConcurrency( params, counter );
        assertFalse( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 0 ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( (int) Math.min( 3 * cpu, counter.methods ) ) );
    }

    @Theory
    public void threadCountBoth( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "both" );
        properties.setProperty( THREADCOUNT_KEY, "3" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        RunnerCounter counter = new RunnerCounter( 1, 2, 5 );
        ParallelComputerFactory.Concurrency concurrency = resolveConcurrency( params, counter );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( (int) Math.min( ( 3d / 2 ) * cpu, 2 ) ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void threadCountClassesAndMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "classesAndMethods" );
        properties.setProperty( THREADCOUNT_KEY, "3" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        RunnerCounter counter = new RunnerCounter( 1, 2, 5 );
        ParallelComputerFactory.Concurrency concurrency = resolveConcurrency( params, counter );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( (int) Math.min( ( 3d / 2 ) * cpu, 2 ) ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void threadCountSuitesAndMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suitesAndMethods" );
        properties.setProperty( THREADCOUNT_KEY, "3" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        RunnerCounter counter = new RunnerCounter( 2, 3, 5 );
        ParallelComputerFactory.Concurrency concurrency = resolveConcurrency( params, counter );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( (int) Math.min( ( 3d / 2 ) * cpu, 2 ) ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void threadCountSuitesAndClasses( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suitesAndClasses" );
        properties.setProperty( THREADCOUNT_KEY, "3" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        RunnerCounter counter = new RunnerCounter( 2, 5, 20 );
        ParallelComputerFactory.Concurrency concurrency = resolveConcurrency( params, counter );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( (int) Math.min( ( 2d * 3 / 7 ) * cpu, 2 ) ) );
        assertThat( concurrency.classes, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void threadCountAll( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "all" );
        properties.setProperty( THREADCOUNT_KEY, "3" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        RunnerCounter counter = new RunnerCounter( 2, 5, 20 );
        ParallelComputerFactory.Concurrency concurrency = resolveConcurrency( params, counter );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( (int) Math.min( ( 2d * 3 / 11 ) * cpu, 2 ) ) );
        assertThat( concurrency.classes, is( (int) Math.min( ( 5d * 3 / 11 ) * cpu, 5 ) ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void reusableThreadCountSuitesAndClasses( int cpu )
        throws TestSetFailedException
    {
        // 4 * cpu to 5 * cpu threads to run test classes
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suitesAndClasses" );
        properties.setProperty( THREADCOUNT_KEY, "6" );
        properties.setProperty( THREADCOUNTSUITES_KEY, "2" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        RunnerCounter counter = new RunnerCounter( 3, 5, 20 );
        ParallelComputerFactory.Concurrency concurrency = resolveConcurrency( params, counter );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 6 * cpu ) );
        assertThat( concurrency.suites, is( Math.min( 2 * cpu, 3 ) ) );
        assertThat( concurrency.classes, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void reusableThreadCountSuitesAndMethods( int cpu )
        throws TestSetFailedException
    {
        // 4 * cpu to 5 * cpu threads to run test methods
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suitesAndMethods" );
        properties.setProperty( THREADCOUNT_KEY, "6" );
        properties.setProperty( THREADCOUNTSUITES_KEY, "2" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        RunnerCounter counter = new RunnerCounter( 3, 5, 20 );
        ParallelComputerFactory.Concurrency concurrency = resolveConcurrency( params, counter );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 6 * cpu ) );
        assertThat( concurrency.suites, is( Math.min( 2 * cpu, 3 ) ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void reusableThreadCountClassesAndMethods( int cpu )
        throws TestSetFailedException
    {
        // 4 * cpu to 5 * cpu threads to run test methods
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "classesAndMethods" );
        properties.setProperty( THREADCOUNT_KEY, "6" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "2" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        RunnerCounter counter = new RunnerCounter( 3, 5, 20 );
        ParallelComputerFactory.Concurrency concurrency = resolveConcurrency( params, counter );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 6 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( Math.min( 2 * cpu, 5 ) ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void reusableThreadCountAll( int cpu )
        throws TestSetFailedException
    {
        // 8 * cpu to 13 * cpu threads to run test methods
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "all" );
        properties.setProperty( THREADCOUNT_KEY, "14" );
        properties.setProperty( THREADCOUNTSUITES_KEY, "2" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "4" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        RunnerCounter counter = new RunnerCounter( 3, 5, 20 );
        ParallelComputerFactory.Concurrency concurrency = resolveConcurrency( params, counter );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethods() );
        assertThat( concurrency.capacity, is( 14 * cpu ) );
        assertThat( concurrency.suites, is( Math.min( 2 * cpu, 3 ) ) );
        assertThat( concurrency.classes, is( Math.min( 4 * cpu, 5 ) ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }
}