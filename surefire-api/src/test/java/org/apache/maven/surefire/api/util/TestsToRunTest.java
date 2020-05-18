package org.apache.maven.surefire.api.util;

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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class TestsToRunTest
    extends TestCase
{
    public void testGetTestSets()
    {
        Set<Class<?>> classes = new LinkedHashSet<>();
        classes.add( T1.class );
        classes.add( T2.class );
        TestsToRun testsToRun = new TestsToRun( classes );
        Iterator<Class<?>> it = testsToRun.iterator();
        assertTrue( it.hasNext() );
        assertEquals( it.next(), T1.class );
        assertTrue( it.hasNext() );
        assertEquals( it.next(), T2.class );
        assertFalse( it.hasNext() );
    }

    public void testContainsAtLeast()
    {
        Set<Class<?>> classes = new LinkedHashSet<>();
        classes.add( T1.class );
        classes.add( T2.class );
        TestsToRun testsToRun = new TestsToRun( classes );
        assertTrue( testsToRun.containsAtLeast( 2 ) );
        assertFalse( testsToRun.containsAtLeast( 3 ) );
    }

    public void testContainsExactly()
    {
        Set<Class<?>> classes = new LinkedHashSet<>();
        classes.add( T1.class );
        classes.add( T2.class );
        TestsToRun testsToRun = new TestsToRun( classes );
        assertFalse( testsToRun.containsExactly( 1 ) );
        assertTrue( testsToRun.containsExactly( 2 ) );
        assertFalse( testsToRun.containsExactly( 3 ) );
    }

    public void testToRunArray()
    {
        Set<Class<?>> classes = new LinkedHashSet<>();
        classes.add( T1.class );
        classes.add( T2.class );
        TestsToRun testsToRun = new TestsToRun( classes );
        Class<?>[] locatedClasses = testsToRun.getLocatedClasses();
        assertEquals( 2, locatedClasses.length );
    }

    public void testGetClassByName()
    {
        Set<Class<?>> classes = new LinkedHashSet<>();
        classes.add( T1.class );
        classes.add( T2.class );
        TestsToRun testsToRun = new TestsToRun( classes );
        assertEquals( T1.class, testsToRun.getClassByName( "org.apache.maven.surefire.api.util.TestsToRunTest$T1" ) );
        assertEquals( T2.class, testsToRun.getClassByName( "org.apache.maven.surefire.api.util.TestsToRunTest$T2" ) );
        assertNull( testsToRun.getClassByName( "org.apache.maven.surefire.util.TestsToRunTest$T3" ) );
    }

    public void testTwoIterators()
    {
        Set<Class<?>> classes = new LinkedHashSet<>();
        classes.add( T1.class );
        classes.add( T2.class );
        TestsToRun testsToRun = new TestsToRun( classes );

        Iterator<Class<?>> it1 = testsToRun.iterator();

        assertEquals( it1.next(), T1.class );
        assertTrue( it1.hasNext() );

        Iterator<Class<?>> it2 = testsToRun.iterated();

        assertEquals( it1.next(), T2.class );
        assertFalse( it1.hasNext() );

        assertEquals( it2.next(), T1.class );
        assertFalse( it1.hasNext() );
    }

    static class T1
    {

    }

    static class T2
    {

    }
}

