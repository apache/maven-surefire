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

import java.util.List;

import junit.framework.TestCase;
import org.apache.maven.surefire.util.internal.ClassMethod;

/**
 * @author Kristian Rosenvold
 */
public class ThreadedExecutionSchedulerTest
    extends TestCase
{

    private final RunEntryStatistics a1 = fromValues( 200, 2, A.class, "at1" );

    private final RunEntryStatistics a2 = fromValues( 300, 2, A.class, "at2" );

    private final RunEntryStatistics b1 = fromValues( 400, 2, B.class, "bt1" );

    private final RunEntryStatistics b2 = fromValues( 300, 2, B.class, "bt2" );

    private final RunEntryStatistics c1 = fromValues( 400, 2, C.class, "ct1" );

    private final RunEntryStatistics c2 = fromValues( 200, 2, C.class, "ct2" );

    private final RunEntryStatistics d1 = fromValues( 401, 2, D.class, "ct2" );

    private final RunEntryStatistics e1 = fromValues( 200, 2, E.class, "ct2" );

    public void testAddTest()
    {
        ThreadedExecutionScheduler threadedExecutionScheduler = new ThreadedExecutionScheduler( 2 );
        addPrioritizedTests( threadedExecutionScheduler );
        final List<Class<?>> result = threadedExecutionScheduler.getResult();
        assertEquals( 5, result.size() );
        assertEquals( B.class, result.get( 0 ) );
        assertEquals( C.class, result.get( 1 ) );
        assertEquals( D.class, result.get( 2 ) );
        assertEquals( A.class, result.get( 3 ) );
        assertEquals( E.class, result.get( 4 ) );
    }

    public void testAddTestJaggedResult()
    {
        ThreadedExecutionScheduler threadedExecutionScheduler = new ThreadedExecutionScheduler( 4 );
        addPrioritizedTests( threadedExecutionScheduler );
        final List result = threadedExecutionScheduler.getResult();
        assertEquals( 5, result.size() );
    }

    private void addPrioritizedTests( ThreadedExecutionScheduler threadedExecutionScheduler )
    {
        threadedExecutionScheduler.addTest( new PrioritizedTest( B.class, createPriority( b1, b2 ) ) );
        threadedExecutionScheduler.addTest( new PrioritizedTest( C.class, createPriority( c1, c2 ) ) );
        threadedExecutionScheduler.addTest( new PrioritizedTest( A.class, createPriority( a1, a2 ) ) );
        threadedExecutionScheduler.addTest( new PrioritizedTest( D.class, createPriority( d1 ) ) );
        threadedExecutionScheduler.addTest( new PrioritizedTest( E.class, createPriority( e1 ) ) );
    }

    private Priority createPriority( RunEntryStatistics runEntryStatistics )
    {
        final Priority priority = new Priority( A.class.getName() );
        priority.addItem( runEntryStatistics );
        return priority;
    }

    private Priority createPriority( RunEntryStatistics runEntryStatistics, RunEntryStatistics runEntryStatistics2 )
    {
        final Priority priority = new Priority( A.class.getName() );
        priority.addItem( runEntryStatistics );
        priority.addItem( runEntryStatistics2 );
        return priority;
    }

    private static RunEntryStatistics fromValues( int runTime, int successfulBuilds, Class clazz, String testName )
    {
        ClassMethod classMethod = new ClassMethod( clazz.getName(), testName );
        return new RunEntryStatistics( runTime, successfulBuilds, classMethod );
    }

    class A
    {
    } // 500 total

    class B
    {
    } // 700 total

    class C
    {
    } // 600  total

    class D
    {
    } // 400 total

    class E
    {
    } // 200 total

}
