package org.apache.maven.surefire.util;
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

import junit.framework.TestCase;
import org.apache.maven.surefire.testset.RunOrderParameters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Kristian Rosenvold
 */
public class RunOrderCalculatorTest
        extends TestCase
{

    public void testOrderTestClasses()
    {
        getClassesToRun();
        TestsToRun testsToRun = new TestsToRun( getClassesToRun() );
        RunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( RunOrderParameters.alphabetical(), 1 );
        final TestsToRun testsToRun1 = runOrderCalculator.orderTestClasses( testsToRun );
        assertEquals( A.class, testsToRun1.iterator().next() );

    }

    public void testOrderTestClassesWithRandom()
    {
        getClassesToRun();
        TestsToRun testsToRun = new TestsToRun( getClassesToRun() );
        RunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator(
                randomizedWith( "424242" ), 1
        );
        final TestsToRun testsToRun1 = runOrderCalculator.orderTestClasses( testsToRun );
        Iterator<Class<?>> iter = testsToRun1.iterator();
        assertEquals( B.class, iter.next() );
        assertEquals( A.class, iter.next() );

        TestsToRun testsToRun2 = runOrderCalculator.orderTestClasses( testsToRun );
        iter = testsToRun2.iterator();
        assertEquals( B.class, iter.next() );
        assertEquals( A.class, iter.next() );
    }

    public void testOrderTestClassesWithRandomized()
    {
        getClassesToRun();
        TestsToRun testsToRun = new TestsToRun( getClassesToRun() );
        RunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( randomized(), 1 );
        final TestsToRun testsToRun1 = runOrderCalculator.orderTestClasses( testsToRun );
        String names1 = join( asNamesList( testsToRun1 ) );

        TestsToRun testsToRun2 = runOrderCalculator.orderTestClasses( testsToRun );
        String names2 = join( asNamesList( testsToRun2 ) );

        int times = 100000;
        while ( names1.equals( names2 ) && times > 0 )
        {
            testsToRun2 = runOrderCalculator.orderTestClasses( testsToRun );
            names2 = join( asNamesList( testsToRun2 ) );
            times--;
        }
        assertEquals( 0, times );
    }

    private static String join( List<String> names )
    {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<String> iter = names.iterator();
        if ( iter.hasNext() )
        {
            stringBuilder.append( iter.next() );
        }
        while ( iter.hasNext() )
        {
            stringBuilder.append( "-" ).append( iter.next() );
        }
        return stringBuilder.toString();
    }

    private static List<String> asNamesList( TestsToRun testsToRun )
    {
        List<String> names1 = new ArrayList<String>();
        for ( Class<?> cls : testsToRun )
        {
            names1.add( cls.getSimpleName() );
        }
        return names1;
    }

    private RunOrderParameters randomizedWith( String seed )
    {
        Randomizer randomizer = new Randomizer( seed );
        return new RunOrderParameters(
                new RunOrders( RunOrder.RANDOM ),
                randomizer,
                null
        );
    }

    private RunOrderParameters randomized()
    {
        return new RunOrderParameters(
                new RunOrders( RunOrder.RANDOM ),
                new Randomizer(),
                null
        );
    }

    private Set<Class<?>> getClassesToRun()
    {
        Set<Class<?>> classesToRun = new LinkedHashSet<>();
        classesToRun.add( B.class );
        classesToRun.add( A.class );
        return classesToRun;
    }

    class A
    {

    }

    class B
    {

    }


}
