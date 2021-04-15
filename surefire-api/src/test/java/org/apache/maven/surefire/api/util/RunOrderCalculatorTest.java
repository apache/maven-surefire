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

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.surefire.api.testset.RunOrderParameters;

import junit.framework.TestCase;

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

    private Set<Class<?>> getClassesToRun()
    {
        Set<Class<?>> classesToRun = new LinkedHashSet<>();
        classesToRun.add( B.class );
        classesToRun.add( A.class );
        return classesToRun;
    }

    static class A
    {

    }

    static class B
    {

    }

    public void testOrderTestMethods()
    {
        RunOrderParameters runOrderParameters = new RunOrderParameters( "testorder" , null );
        System.setProperty( "test", "TestClass#a2d,TestClass#aBc,TestClass#abc,TestClass#a1b" );
        DefaultRunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( runOrderParameters, 1 );
        Comparator<String> testOrderRunOrderComparator = runOrderCalculator.comparatorForTestMethods();
        String[] strArray = { "abc(TestClass)", "a1b(TestClass)", "a2d(TestClass)", "aBc(TestClass)" };
        List<String> actual = Arrays.asList( strArray );
        actual.sort( testOrderRunOrderComparator );
        String[] strArray2 = { "a2d(TestClass)", "aBc(TestClass)", "abc(TestClass)", "a1b(TestClass)" };
        List<String> expected = Arrays.asList( strArray2 );
        assertEquals( actual, expected );
    }

    public void testOrderTestClassesAndMethods()
    {
        RunOrderParameters runOrderParameters = new RunOrderParameters( "testorder" , null );
        System.setProperty( "test", "TestClass1#a2d,TestClass2#aBc,TestClass2#abc,TestClass2#a1b" );
        DefaultRunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( runOrderParameters, 1 );
        Comparator<String> testOrderRunOrderComparator = runOrderCalculator.comparatorForTestMethods();
        String[] strArray = { "abc(TestClass2)", "a1b(TestClass2)", "a2d(TestClass1)", "aBc(TestClass2)" };
        List<String> actual = Arrays.asList( strArray );
        actual.sort( testOrderRunOrderComparator );
        String[] strArray2 = { "a2d(TestClass1)", "aBc(TestClass2)", "abc(TestClass2)", "a1b(TestClass2)" };
        List<String> expected = Arrays.asList( strArray2 );
        assertEquals( actual, expected );
    }

    public void testOrderTestRegexClassesAndMethods()
    {
        RunOrderParameters runOrderParameters = new RunOrderParameters( "testorder" , null );
        System.setProperty( "test", "Amber*Test#a?c,My???Test#test*" );
        DefaultRunOrderCalculator runOrderCalculator = new DefaultRunOrderCalculator( runOrderParameters, 1 );
        Comparator<String> testOrderRunOrderComparator = runOrderCalculator.comparatorForTestMethods();
        String[] strArray = { "abc(AmberGoodTest)", 
                              "testabc(MyabcTest)", 
                              "a2c(AmberBadTest)", 
                              "testefg(MyefgTest)", 
                              "aBc(AmberGoodTest)" };
        List<String> actual = Arrays.asList( strArray );
        actual.sort( testOrderRunOrderComparator );
        assertEquals( runOrderCalculator.getClassAndMethod( actual.get( 0 ) )[0].substring( 0, 5 ), "Amber" );
        assertEquals( runOrderCalculator.getClassAndMethod( actual.get( 1 ) )[0].substring( 0, 5 ), "Amber" );
        assertEquals( runOrderCalculator.getClassAndMethod( actual.get( 2 ) )[0].substring( 0, 5 ), "Amber" );
        assertEquals( runOrderCalculator.getClassAndMethod( actual.get( 3 ) )[0].substring( 0, 2 ), "My" );
        assertEquals( runOrderCalculator.getClassAndMethod( actual.get( 4 ) )[0].substring( 0, 2 ), "My" );
    }
}
