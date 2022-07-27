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
        System.setProperty( "test", "TestClass#a2d,TestClass#aBc,TestClass#abc,TestClass#a1b" );
        RunOrderParameters runOrderParameters = new RunOrderParameters( "testorder" , null,
                                                    null, System.getProperty( "test" ) );
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
        System.setProperty( "test", "TestClass1#a2d,TestClass2#aBc,TestClass2#abc,TestClass2#a1b" );
        RunOrderParameters runOrderParameters = new RunOrderParameters( "testorder" , null,
                                                    null, System.getProperty( "test" ) );
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
        System.setProperty( "test", "Amber*Test#a?c,My???Test#test*" );
        RunOrderParameters runOrderParameters = new RunOrderParameters( "testorder" , null,
                                                    null, System.getProperty( "test" ) );
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

    public void testOrderComparatorTest()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "TestClass1#testa2d," );
        sb.append( "TestClass1#testabc," );
        sb.append( "TestClass1#testa1b," );
        sb.append( "TestClass2#testa1b," );
        sb.append( "TestClass2#testaBc" );
        RunOrderParameters runOrderParameters = new RunOrderParameters( "testorder" , null,
            null, sb.toString() );
        DefaultRunOrderCalculator roc = new DefaultRunOrderCalculator( runOrderParameters, 1 );
        String className = "TestClass1";
        String className2 = "TestClass2";
        assertTrue( ( int ) roc.testOrderComparator( className, className, "testa2d", "testa1b" ) < 0 );
        assertTrue( ( int ) roc.testOrderComparator( className, className, "testa2d", "testabc" ) < 0 );
        assertTrue( ( int ) roc.testOrderComparator( className, className, "testa1b", "testabc" ) > 0 );
        assertTrue( ( int ) roc.testOrderComparator( className, className, "testa2d", "testaBc" ) > 0 );
        assertTrue( ( int ) roc.testOrderComparator( className, className, "testa3d", "testa1b" ) < 0 );
        assertTrue( ( int ) roc.testOrderComparator( className, className2, "testa2d", "testa1b" ) < 0 );
        assertTrue( ( int ) roc.testOrderComparator( className2, className, "testaBc", "testa1b" ) > 0 );
        assertTrue( ( int ) roc.testOrderComparator( className, className2, "testa3d", "testa1b" ) < 0 );
        assertTrue( ( int ) roc.testOrderComparator( className, className2, "testa2d", "testabc" ) > 0 );
        assertTrue( ( int ) roc.testOrderComparator( className, className, "testa2d", "testa2d" ) == 0 );
    }

    public void testRegexMethodOrderComparator()
    {
        StringBuilder orderParamList = new StringBuilder();
        orderParamList.append( "TestClass1#testa?c," );
        orderParamList.append( "TestClass1#testa?b," );
        orderParamList.append( "TestClass2#test?1*," );
        orderParamList.append( "!TestClass1#testa4b," );
        orderParamList.append( "!TestClass2#test11MyTest" );
        RunOrderParameters runOrderParameters = new RunOrderParameters( "testorder" , null,
            null, orderParamList.toString() );
        DefaultRunOrderCalculator roc = new DefaultRunOrderCalculator( runOrderParameters, 1 );
        String className = "TestClass1";
        String className2 = "TestClass2";
        assertTrue( ( int ) roc.testOrderComparator( className, className, "testabc", "testa1b" ) < 0 );
        assertTrue( ( int ) roc.testOrderComparator( className, className, "testaBc", "testa2b" ) < 0 );
        assertTrue( ( int ) roc.testOrderComparator( className, className, "testa1b", "testa3c" ) > 0 );
        assertTrue( ( int ) roc.testOrderComparator( className, className2, "testa1b", "test1123" ) < 0 );
        assertTrue( ( int ) roc.testOrderComparator( className2, className, "testa1b", "testa1b" ) > 0 );
        assertTrue( ( int ) roc.testOrderComparator( className2, className2, "testa1b", "test1123" ) == 0 );
        assertTrue( ( int ) roc.testOrderComparator( className, className, "testa1c", "testa1c" ) == 0 );
    }

    public void testRegexClassOrderComparator()
    {
        StringBuilder orderParamList = new StringBuilder();
        orderParamList.append( "My2*Test.java," );
        orderParamList.append( "???My1*Test," );
        orderParamList.append( "!abcMy1PeaceTest" );
        RunOrderParameters runOrderParameters = new RunOrderParameters( "testorder" , null,
                                                    null, orderParamList.toString() );
        DefaultRunOrderCalculator roc = new DefaultRunOrderCalculator( runOrderParameters, 1 );
        String className = "My2ConnectTest";
        String className2 = "456My1ConnectTest";
        assertTrue( ( int ) roc.testOrderComparator( className, className2, null, null ) < 0 );
        assertTrue( ( int ) roc.testOrderComparator( className2, className, null, null ) > 0 );
        assertTrue( ( int ) roc.testOrderComparator( className, className, null, null ) == 0 );
    }
}
