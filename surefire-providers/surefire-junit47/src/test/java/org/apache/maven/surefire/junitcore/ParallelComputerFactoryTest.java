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

import org.apache.maven.surefire.junitcore.pc.ParallelComputer;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.apache.maven.surefire.junitcore.ParallelComputerFactory.*;
import static org.apache.maven.surefire.junitcore.JUnitCoreParameters.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Testing an algorithm in {@link ParallelComputerFactory} which configures
 * allocated thread resources in ParallelComputer by given {@link JUnitCoreParameters}.
 *
 * @author Tibor Digana (tibor17)
 * @see org.apache.maven.surefire.junitcore.ParallelComputerFactory
 * @since 2.16
 */
@RunWith( Theories.class )
public final class ParallelComputerFactoryTest
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

    private static Properties parallel( String parallel )
    {
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, parallel );
        return properties;
    }

    @Test
    public void unknownParallel()
        throws TestSetFailedException
    {
        Properties properties = new Properties();
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( new JUnitCoreParameters( properties ) );
    }

    @Test
    public void unknownThreadCountSuites()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "suites" ) );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params );
    }

    @Test
    public void unknownThreadCountClasses()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "classes" ) );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params );
    }

    @Test
    public void unknownThreadCountMethods()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "methods" ) );
        assertFalse( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params );
    }

    @Test
    public void unknownThreadCountBoth()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "both" ) );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params );
    }

    @Test
    public void unknownThreadCountAll()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "all" ) );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params );
    }

    @Test
    public void unknownThreadCountSuitesAndClasses()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "suitesAndClasses" ) );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params );
    }

    @Test
    public void unknownThreadCountSuitesAndMethods()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "suitesAndMethods" ) );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params );
    }

    @Test
    public void unknownThreadCountClassesAndMethods()
        throws TestSetFailedException
    {
        JUnitCoreParameters params = new JUnitCoreParameters( parallel( "classesAndMethods" ) );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        exception.expect( TestSetFailedException.class );
        resolveConcurrency( params );
    }

    @Theory
    public void useUnlimitedThreadsSuites( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suites" );
        properties.setProperty( USEUNLIMITEDTHREADS_KEY, "true" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 0 ) );

        properties.setProperty( THREADCOUNTSUITES_KEY, "5" );
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void useUnlimitedThreadsClasses( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "classes" );
        properties.setProperty( USEUNLIMITEDTHREADS_KEY, "true" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.methods, is( 0 ) );

        properties.setProperty( THREADCOUNTCLASSES_KEY, "5" );
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 5 * cpu ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void unlimitedThreadsMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "methods" );
        properties.setProperty( USEUNLIMITEDTHREADS_KEY, "true" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );

        properties.setProperty( THREADCOUNTMETHODS_KEY, "5" );
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 5 * cpu ) );
    }

    @Theory
    public void unlimitedThreadsSuitesAndClasses( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suitesAndClasses" );
        properties.setProperty( USEUNLIMITEDTHREADS_KEY, "true" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.classes, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.methods, is( 0 ) );

        properties.setProperty( THREADCOUNTSUITES_KEY, "5" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "15" );
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 15 * cpu ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void unlimitedThreadsSuitesAndMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suitesAndMethods" );
        properties.setProperty( USEUNLIMITEDTHREADS_KEY, "true" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );

        properties.setProperty( THREADCOUNTSUITES_KEY, "5" );
        properties.setProperty( THREADCOUNTMETHODS_KEY, "15" );
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 15 * cpu ) );
    }

    @Theory
    public void unlimitedThreadsClassesAndMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "classesAndMethods" );
        properties.setProperty( USEUNLIMITEDTHREADS_KEY, "true" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );

        properties.setProperty( THREADCOUNTCLASSES_KEY, "5" );
        properties.setProperty( THREADCOUNTMETHODS_KEY, "15" );
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 5 * cpu ) );
        assertThat( concurrency.methods, is( 15 * cpu ) );
    }

    @Theory
    public void unlimitedThreadsAll( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "all" );
        properties.setProperty( USEUNLIMITEDTHREADS_KEY, "true" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.classes, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );

        properties.setProperty( THREADCOUNTSUITES_KEY, "5" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "15" );
        properties.setProperty( THREADCOUNTMETHODS_KEY, "30" );
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 15 * cpu ) );
        assertThat( concurrency.methods, is( 30 * cpu ) );
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
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 0 ) );
        assertThat( concurrency.suites, is( 3 * cpu ) );
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
        Concurrency concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 0 ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 3 * cpu ) );
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
        Concurrency concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 0 ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 3 * cpu ) );
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
        Concurrency concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( (int) ( ( 3d / 2 ) * cpu ) ) );
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
        Concurrency concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( (int) ( ( 3d / 2 ) * cpu ) ) );
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
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( (int) ( ( 3d / 2 ) * cpu ) ) );
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
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( (int) ( ( 3d / 2 ) * cpu ) ) );
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
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 3 * cpu ) );
        assertThat( concurrency.suites, is( cpu ) );
        assertThat( concurrency.classes, is( cpu ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void everyThreadCountSuitesAndClasses( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suitesAndClasses" );
        properties.setProperty( THREADCOUNT_KEY, "3" );
        // % percentage ratio
        properties.setProperty( THREADCOUNTSUITES_KEY, "34" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "66" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
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
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suitesAndMethods" );
        properties.setProperty( THREADCOUNT_KEY, "3" );
        // % percentage ratio
        properties.setProperty( THREADCOUNTSUITES_KEY, "34" );
        properties.setProperty( THREADCOUNTMETHODS_KEY, "66" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
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
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "classesAndMethods" );
        properties.setProperty( THREADCOUNT_KEY, "3" );
        // % percentage ratio
        properties.setProperty( THREADCOUNTCLASSES_KEY, "34" );
        properties.setProperty( THREADCOUNTMETHODS_KEY, "66" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
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
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "all" );
        properties.setProperty( THREADCOUNT_KEY, "3" );
        // % percentage ratio
        properties.setProperty( THREADCOUNTSUITES_KEY, "17" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "34" );
        properties.setProperty( THREADCOUNTMETHODS_KEY, "49" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
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
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suitesAndClasses" );
        properties.setProperty( THREADCOUNT_KEY, "6" );
        properties.setProperty( THREADCOUNTSUITES_KEY, "2" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
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
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suitesAndMethods" );
        properties.setProperty( THREADCOUNT_KEY, "6" );
        properties.setProperty( THREADCOUNTSUITES_KEY, "2" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
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
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "classesAndMethods" );
        properties.setProperty( THREADCOUNT_KEY, "6" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "2" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
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
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "all" );
        properties.setProperty( THREADCOUNT_KEY, "14" );
        properties.setProperty( THREADCOUNTSUITES_KEY, "2" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "4" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 14 * cpu ) );
        assertThat( concurrency.suites, is( 2 * cpu ) );
        assertThat( concurrency.classes, is( 4 * cpu ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void suites( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suites" );
        properties.setProperty( THREADCOUNTSUITES_KEY, "5" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 5 * cpu ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void classes( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "classes" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "5" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 5 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 5 * cpu ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void methods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "methods" );
        properties.setProperty( THREADCOUNTMETHODS_KEY, "5" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 5 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 5 * cpu ) );
    }

    @Theory
    public void suitesAndClasses( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();

        properties.setProperty( PARALLEL_KEY, "suitesAndClasses" );
        properties.setProperty( THREADCOUNTSUITES_KEY, "5" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "15" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 20 * cpu ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 15 * cpu ) );
        assertThat( concurrency.methods, is( 0 ) );

        // Warning: this case works but is not enabled in AbstractSurefireMojo
        // Instead use the 'useUnlimitedThreads' parameter.
        properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suitesAndClasses" );
        properties.setProperty( THREADCOUNTSUITES_KEY, "5" );
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertFalse( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.methods, is( 0 ) );
    }

    @Theory
    public void suitesAndMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();

        properties.setProperty( PARALLEL_KEY, "suitesAndMethods" );
        properties.setProperty( THREADCOUNTSUITES_KEY, "5" );
        properties.setProperty( THREADCOUNTMETHODS_KEY, "15" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 20 * cpu ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( 15 * cpu ) );

        // Warning: this case works but is not enabled in AbstractSurefireMojo
        // Instead use the 'useUnlimitedThreads' parameter.
        properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "suitesAndMethods" );
        properties.setProperty( THREADCOUNTSUITES_KEY, "5" );
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertFalse( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 0 ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void classesAndMethods( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();

        properties.setProperty( PARALLEL_KEY, "classesAndMethods" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "5" );
        properties.setProperty( THREADCOUNTMETHODS_KEY, "15" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 20 * cpu ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 5 * cpu ) );
        assertThat( concurrency.methods, is( 15 * cpu ) );

        // Warning: this case works but is not enabled in AbstractSurefireMojo
        // Instead use the 'useUnlimitedThreads' parameter.
        properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "classesAndMethods" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "5" );
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params );
        assertFalse( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 0 ) );
        assertThat( concurrency.classes, is( 5 * cpu ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Theory
    public void all( int cpu )
        throws TestSetFailedException
    {
        ParallelComputerFactory.overrideAvailableProcessors( cpu );
        Properties properties = new Properties();

        properties.setProperty( PARALLEL_KEY, "all" );
        properties.setProperty( THREADCOUNTSUITES_KEY, "5" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "15" );
        properties.setProperty( THREADCOUNTMETHODS_KEY, "30" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        Concurrency concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( 50 * cpu ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 15 * cpu ) );
        assertThat( concurrency.methods, is( 30 * cpu ) );

        // Warning: these cases work but they are not enabled in AbstractSurefireMojo
        // Instead use the 'useUnlimitedThreads' parameter.
        properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "all" );
        properties.setProperty( THREADCOUNTSUITES_KEY, "5" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "15" );
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( 5 * cpu ) );
        assertThat( concurrency.classes, is( 15 * cpu ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );

        properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "all" );
        properties.setProperty( THREADCOUNTCLASSES_KEY, "15" );
        params = new JUnitCoreParameters( properties );
        concurrency = resolveConcurrency( params );
        assertTrue( params.isParallelSuites() );
        assertTrue( params.isParallelClasses() );
        assertTrue( params.isParallelMethod() );
        assertThat( concurrency.capacity, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.suites, is( Integer.MAX_VALUE ) );
        assertThat( concurrency.classes, is( 15 * cpu ) );
        assertThat( concurrency.methods, is( Integer.MAX_VALUE ) );
    }

    @Test
    public void withoutShutdown()
        throws TestSetFailedException, ExecutionException, InterruptedException
    {
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "methods" );
        properties.setProperty( THREADCOUNTMETHODS_KEY, "2" );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        ParallelComputer pc = createParallelComputer( params );

        Result result = new JUnitCore().run( pc, TestClass.class );
        long timeSpent = runtime.stop();
        long deltaTime = 500L;

        assertTrue( result.wasSuccessful() );
        assertThat( result.getRunCount(), is( 3 ) );
        assertThat( result.getFailureCount(), is( 0 ) );
        assertThat( result.getIgnoreCount(), is( 0 ) );
        assertEquals( 10000L, timeSpent, deltaTime );
    }

    @Test
    public void shutdown()
        throws TestSetFailedException, ExecutionException, InterruptedException
    {
        // The JUnitCore returns after 2.5s.
        // The test-methods in TestClass are NOT interrupted, and return normally after 5s.
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "methods" );
        properties.setProperty( THREADCOUNTMETHODS_KEY, "2" );
        properties.setProperty( PARALLEL_TIMEOUT_KEY, Double.toString( 2.5d ) );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        ParallelComputer pc = createParallelComputer( params );

        new JUnitCore().run( pc, TestClass.class );
        long timeSpent = runtime.stop();
        long deltaTime = 500L;

        assertEquals( 2500L, timeSpent, deltaTime );
        assertTrue( pc.describeElapsedTimeout().contains(
            "The test run has finished abruptly after timeout of 2.5 seconds." ) );
        assertTrue( pc.describeElapsedTimeout().contains( TestClass.class.getName() ) );
    }

    @Test
    public void forcedShutdown()
        throws TestSetFailedException, ExecutionException, InterruptedException
    {
        // The JUnitCore returns after 2.5s, and the test-methods in TestClass are interrupted.
        Properties properties = new Properties();
        properties.setProperty( PARALLEL_KEY, "methods" );
        properties.setProperty( THREADCOUNTMETHODS_KEY, "2" );
        properties.setProperty( PARALLEL_TIMEOUTFORCED_KEY, Double.toString( 2.5d ) );
        JUnitCoreParameters params = new JUnitCoreParameters( properties );
        ParallelComputer pc = createParallelComputer( params );

        new JUnitCore().run( pc, TestClass.class );
        long timeSpent = runtime.stop();
        long deltaTime = 500L;

        assertEquals( 2500L, timeSpent, deltaTime );
        assertTrue( pc.describeElapsedTimeout().contains(
            "The test run has finished abruptly after timeout of 2.5 seconds." ) );
        assertTrue( pc.describeElapsedTimeout().contains( TestClass.class.getName() ) );
    }

    public static class TestClass
    {
        @Test
        public void a()
            throws InterruptedException
        {
            long t1 = System.currentTimeMillis();
            try{
                Thread.sleep( 5000L );
            }
            finally
            {
                System.out.println( getClass().getSimpleName() + "#a() spent " + ( System.currentTimeMillis() - t1 ) );
            }
        }

        @Test
        public void b()
            throws InterruptedException
        {
            long t1 = System.currentTimeMillis();
            try{
                Thread.sleep( 5000L );
            }
            finally
            {
                System.out.println( getClass().getSimpleName() + "#b() spent " + ( System.currentTimeMillis() - t1 ) );
            }
        }

        @Test
        public void c()
            throws InterruptedException
        {
            long t1 = System.currentTimeMillis();
            try{
                Thread.sleep( 5000L );
            }
            finally
            {
                System.out.println( getClass().getSimpleName() + "#c() spent " + ( System.currentTimeMillis() - t1 ) );
            }
        }
    }
}