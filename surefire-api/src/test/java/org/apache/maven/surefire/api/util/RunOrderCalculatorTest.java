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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.surefire.api.testset.ResolvedTest;
import org.apache.maven.surefire.api.testset.RunOrderParameters;

import junit.framework.TestCase;

import static java.util.Arrays.asList;

/**
 * @author Kristian Rosenvold
 */
public class RunOrderCalculatorTest
    extends TestCase
{

    public void testAlphabeticalOrder()
    {
        getClassesToRun();
        TestsToRun testsToRun = new TestsToRun( getClassesToRun() );
        RunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( RunOrderParameters.alphabetical(),
            1, Collections.emptyList() );
        final TestsToRun orderedTestsToRun = runOrderCalculator.orderTestClasses( testsToRun );
        Iterator<Class<?>> iterator = orderedTestsToRun.iterator();
        assertEquals( A.class, iterator.next() );
        assertEquals( B.class, iterator.next() );
        assertEquals( C.class, iterator.next() );
    }

    public void testTestOrder()
    {
        TestsToRun testsToRun = new TestsToRun( getClassesToRun() );
        ResolvedTest testA = new ResolvedTest( A.class.getName(), null, false );
        ResolvedTest testB = new ResolvedTest( B.class.getName(), null, false );
        ResolvedTest testC = new ResolvedTest( C.class.getName(), null, false );

        List<ResolvedTest> resolvedTests = asList( testB, testC, testA );
        RunOrderParameters runOrderParameters = new RunOrderParameters(
            RunOrder.valueOfMulti( "test" ), null );
        RunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( runOrderParameters,
            1, resolvedTests );
        final TestsToRun orderedTestsToRun = runOrderCalculator.orderTestClasses( testsToRun );
        Iterator<Class<?>> iterator = orderedTestsToRun.iterator();
        assertEquals( B.class, iterator.next() );
        assertEquals( C.class, iterator.next() );
        assertEquals( A.class, iterator.next() );
    }

    private Set<Class<?>> getClassesToRun()
    {
        Set<Class<?>> classesToRun = new LinkedHashSet<>();
        classesToRun.add( B.class );
        classesToRun.add( A.class );
        classesToRun.add( C.class );
        return classesToRun;
    }

    static class A
    {

    }

    static class B
    {

    }

    static class C
    {

    }
}
