package org.apache.maven.surefire.common.junit48;

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

import org.apache.maven.surefire.shared.utils.io.MatchPatterns;
import org.apache.maven.surefire.common.junit48.tests.pt.PT;
import org.apache.maven.surefire.api.testset.ResolvedTest;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.manipulation.Filter;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.runner.Description.createSuiteDescription;
import static org.junit.runner.Description.createTestDescription;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
@SuppressWarnings( "checkstyle:constantname" )
public class FilterFactoryTest
{
    /**
     *
     */
    @RunWith( org.junit.runners.Suite.class )
    @org.junit.runners.Suite.SuiteClasses( { FirstClass.class, SecondClass.class } )
    public static class Suite
    {

    }

    /**
     *
     */
    public static class FirstClass
    {
        @Test
        public void testMethod()
        {
            //System.out.println( "FirstClass#testMethod" );
        }

        @Test
        public void secondTestMethod()
        {
            //System.out.println( "FirstClass#secondTestMethod" );
        }

        @Test
        public void otherMethod()
        {
            //System.out.println( "FirstClass#otherMethod" );
        }
    }

    /**
     *
     */
    public static class SecondClass
    {
        @Test
        public void testMethod()
        {
            //System.out.println( "SecondClass#testMethod" );
        }

        @Test
        public void secondTestMethod()
        {
            //System.out.println( "SecondClass#secondTestMethod" );
        }
    }

    /**
     *
     */
    public static class ThirdClass
    {
        @Test
        public void testMethod()
        {
            //System.out.println( "ThirdClass#testMethod" );
        }

        @Test
        public void secondTestMethod()
        {
            //System.out.println( "ThirdClass#secondTestMethod" );
        }
    }

    private static final Description testMethod = createTestDescription( FirstClass.class, "testMethod" );

    private static final Description secondTestMethod = createTestDescription( FirstClass.class, "secondTestMethod" );

    private static final Description otherMethod = createTestDescription( FirstClass.class, "otherMethod" );

    private static final Description testMethodInSecondClass = createTestDescription( SecondClass.class, "testMethod" );

    private static final Description secondTestMethodInSecondClass =
        createTestDescription( SecondClass.class, "secondTestMethod" );

    private static final String firstClassName = FirstClass.class.getName().replace( '.', '/' );

    private static final String secondClassName = SecondClass.class.getName().replace( '.', '/' );

    private static final String firstClassRegex = FirstClass.class.getName().replace( "$", "\\$" );

    private static final String secondClassRegex = SecondClass.class.getName().replace( "$", "\\$" );

    private Filter createMethodFilter( String requestString )
    {
        return new FilterFactory( getClass().getClassLoader() ).createMethodFilter( requestString );
    }

    @Test
    public void testSanity()
    {
        ResolvedTest test = new ResolvedTest( ResolvedTest.Type.CLASS, "  \t \n   ", true );
        assertNull( test.getTestClassPattern() );
        assertNull( test.getTestMethodPattern() );
        assertFalse( test.hasTestClassPattern() );
        assertFalse( test.hasTestMethodPattern() );
        assertTrue( test.isEmpty() );
        assertTrue( test.isRegexTestClassPattern() );
        assertFalse( test.isRegexTestMethodPattern() );
        test = new ResolvedTest( ResolvedTest.Type.METHOD, "   \n  \t   ", true );
        assertNull( test.getTestClassPattern() );
        assertNull( test.getTestMethodPattern() );
        assertFalse( test.hasTestClassPattern() );
        assertFalse( test.hasTestMethodPattern() );
        assertTrue( test.isEmpty() );
        assertFalse( test.isRegexTestClassPattern() );
        assertTrue( test.isRegexTestMethodPattern() );
        test = new ResolvedTest( ResolvedTest.Type.METHOD, "  \n   ", false );
        assertNull( test.getTestClassPattern() );
        assertNull( test.getTestMethodPattern() );
        assertFalse( test.hasTestClassPattern() );
        assertFalse( test.hasTestMethodPattern() );
        assertTrue( test.isEmpty() );
        assertFalse( test.isRegexTestClassPattern() );
        assertFalse( test.isRegexTestMethodPattern() );
        test = new ResolvedTest( "  \n  \t ", "  \n  \t ", false );
        assertNull( test.getTestClassPattern() );
        assertNull( test.getTestMethodPattern() );
        assertFalse( test.hasTestClassPattern() );
        assertFalse( test.hasTestMethodPattern() );
        assertTrue( test.isEmpty() );
        assertFalse( test.isRegexTestClassPattern() );
        assertFalse( test.isRegexTestMethodPattern() );
    }

    @Test
    public void testNegativeIllegalRegex()
    {
        try
        {
            new TestListResolver( "#%regex[.*.Test.class]" );
        }
        catch ( IllegalArgumentException e )
        {
            // expected in junit 3.x
        }
    }

    @Test
    public void testNegativeIllegalRegex2()
    {
        try
        {
            new TestListResolver( "%regex[.*.Test.class]#" );
        }
        catch ( IllegalArgumentException e )
        {
            // expected in junit 3.x
        }
    }

    @Test
    public void testNegativeEmptyRegex()
    {
        TestListResolver resolver = new TestListResolver( "%regex[   ]" );
        assertTrue( resolver.getExcludedPatterns().isEmpty() );
        assertTrue( resolver.getIncludedPatterns().isEmpty() );
        assertTrue( resolver.isEmpty() );
        assertEquals( 0, resolver.getPluginParameterTest().length() );
        assertFalse( resolver.hasExcludedMethodPatterns() );
        assertFalse( resolver.hasIncludedMethodPatterns() );
        assertFalse( resolver.hasMethodPatterns() );
    }

    @Test
    public void testNegativeEmptyRegexWithHash()
    {
        TestListResolver resolver = new TestListResolver( "%regex[# ]" );
        assertTrue( resolver.getExcludedPatterns().isEmpty() );
        assertTrue( resolver.getIncludedPatterns().isEmpty() );
        assertTrue( resolver.isEmpty() );
        assertEquals( 0, resolver.getPluginParameterTest().length() );
        assertFalse( resolver.hasExcludedMethodPatterns() );
        assertFalse( resolver.hasIncludedMethodPatterns() );
        assertFalse( resolver.hasMethodPatterns() );
    }

    @Test
    public void testNegativeRegexWithEmptyMethod()
    {
        TestListResolver resolver = new TestListResolver( "%regex[.*.Test.class# ]" );
        assertFalse( resolver.isEmpty() );
        assertTrue( resolver.getExcludedPatterns().isEmpty() );
        assertFalse( resolver.getIncludedPatterns().isEmpty() );
        assertEquals( 1, resolver.getIncludedPatterns().size() );
        assertEquals( "%regex[.*.Test.class]", resolver.getPluginParameterTest() );
        assertFalse( resolver.hasExcludedMethodPatterns() );
        assertFalse( resolver.hasIncludedMethodPatterns() );
        assertFalse( resolver.hasMethodPatterns() );
    }

    @Test
    public void testBackwardsCompatibilityNullMethodFilter()
    {
        Filter filter = createMethodFilter( null );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 5, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testBackwardsCompatibilityEmptyMethodFilter()
    {
        Filter filter = createMethodFilter( "" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 5, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testBackwardsCompatibilityBlankMethodFilter()
    {
        Filter filter = createMethodFilter( "    \n" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 5, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testBackwardsCompatibilityTestParameterClass()
    {
        Filter filter = createMethodFilter( firstClassName );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testBackwardsCompatibilityTestParameterJavaClass()
    {
        Filter filter = createMethodFilter( firstClassName + ".java" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testBackwardsCompatibilityTestParameterMethod1()
    {
        Filter filter = createMethodFilter( firstClassName + ".java#testMethod"  );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 1, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testBackwardsCompatibilityTestParameterMethod2()
    {
        Filter filter = createMethodFilter( firstClassName + "#testMethod"  );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 1, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testBackwardsCompatibilityTestParameterMethod3()
    {
        Filter filter = createMethodFilter( firstClassName + "#testMethod"  );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 1, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testRegexWithWildcard()
    {
        Filter filter =
            createMethodFilter( "%regex[" + firstClassRegex + ".*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testRegexWithWildcardClass()
    {
        Filter filter =
            createMethodFilter( "%regex[" + firstClassRegex + ".*.class]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testRegexWithExactClass()
    {
        Filter filter = createMethodFilter( "%regex[" + firstClassRegex + ".class]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testRegexWithWildcardJavaClassNegativeTest()
    {
        Filter filter = createMethodFilter( "%regex[" + firstClassRegex + ".*.class]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testRegexWithTwoClasses()
    {
        Filter filter = createMethodFilter( "%regex[" + firstClassRegex + ".*|" + secondClassRegex + ".*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 5, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testRegexWithTwoClassesComplement()
    {
        Filter filter = createMethodFilter( "!%regex[" + firstClassRegex + ".*|" + secondClassRegex + ".*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 2, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testRegexWithTwoClassesAndOneMethod()
    {
        Filter filter =
            createMethodFilter( "%regex[" + firstClassRegex + ".*|" + secondClassRegex + ".* # otherMethod]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 1, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testRegexWithTwoClassesAndOneMethodComplement()
    {
        Filter filter =
            createMethodFilter( "!%regex[" + firstClassRegex + ".*|" + secondClassRegex + ".*# otherMethod]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 6, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testRegexWithTwoClassesAndWildcardMethod()
    {
        Filter filter = createMethodFilter( "%regex[" + firstClassRegex + ".*|" + secondClassRegex + ".*#test.* ]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 2, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testRegexWithTwoClassesAndRegexMethodComplement()
    {
        Filter filter = createMethodFilter( "!%regex[" + firstClassRegex + ".*|" + secondClassRegex + ".*#test.*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 5, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testRegexWithTwoClassesAndRegexMethods()
    {
        Filter filter =
            createMethodFilter( "%regex[ " + firstClassRegex + ".*|" + secondClassRegex + ".* # test.*|other.* ]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testRegexWithTwoClassesAndRegexMethodsComplement()
    {
        Filter filter =
            createMethodFilter( "!%regex[" + firstClassRegex + ".*|" + secondClassRegex + ".* # test.*|other.* ]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 4, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testMultipleRegexClasses()
    {
        Filter filter =
            createMethodFilter( "%regex[" + firstClassRegex + ".*], %regex[" + secondClassRegex + ".*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 5, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testMultipleRegexClassesComplement()
    {
        Filter filter =
            createMethodFilter( "!%regex[" + firstClassRegex + ".*] , !%regex[" + secondClassRegex + ".*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 2, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testMultipleClasses()
    {
        Filter filter = createMethodFilter( firstClassName + "," + secondClassName );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 5, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testMultipleClassesMethods()
    {
        Filter filter = createMethodFilter( firstClassName + "#other*," + secondClassName + "#*TestMethod" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 2, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testMultipleClassesAndMultipleMethodsWithWildcards()
    {
        Filter filter = createMethodFilter( firstClassName + "#other*+second*Method,"
                                                + secondClassName + "#*TestMethod" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testMultipleClassesAndMultipleMethodsWithRegex()
    {
        Filter filter = createMethodFilter( "%regex[" + firstClassRegex + ".class#other.*|second.*Method],"
                                                + "%regex[" + secondClassRegex + ".class#.*TestMethod]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testMultipleClassesAndMultipleMethodsMix()
    {
        Filter filter = createMethodFilter( "%regex[" + firstClassRegex + ".class # other.*|second.*Method],"
                                                + secondClassName + "#*TestMethod" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testMultipleClassesAndMultipleMethods()
    {
        Filter filter = createMethodFilter( firstClassName + "#other*+secondTestMethod,"
                                                + secondClassName + "#*TestMethod" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testMultipleClassesComplement()
    {
        Filter filter = createMethodFilter( "!" + firstClassName + ",!" + secondClassName );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 2, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testMultipleRegexClassesMethods()
    {
        Filter filter =
            createMethodFilter( "%regex[" + firstClassRegex + ".* # test.*|other.*],"
                                    + "%regex[" + secondClassRegex + ".*#second.*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testMultipleRegexClassesMethodsComplement()
    {
        Filter filter =
            createMethodFilter( "!%regex[" + firstClassRegex + ".* # test.*|other.*],"
                                    + "!%regex[" + secondClassRegex + ".*#second.*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 4, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testShouldMatchExactMethodName()
    {
        Filter exactFilter = createMethodFilter( "#testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
    }

    @Test
    public void testShouldMatchExactMethodNameComplement()
    {
        Filter exactFilter = createMethodFilter( "!#testMethod" );
        assertFalse( "should not run testMethod", exactFilter.shouldRun( testMethod ) );
        assertTrue( "should run other than testMethod", exactFilter.shouldRun( secondTestMethod ) );
        assertFalse( "should not run testMethod", exactFilter.shouldRun( testMethodInSecondClass ) );
        assertTrue( "should run other than testMethod", exactFilter.shouldRun( secondTestMethodInSecondClass ) );

        exactFilter = createMethodFilter( "!FilterFactoryTest$FirstClass#testMethod" );
        assertFalse( "should not run testMethod", exactFilter.shouldRun( testMethod ) );
        assertTrue( "should run other than testMethod", exactFilter.shouldRun( secondTestMethod ) );
        assertTrue( "should not run testMethod", exactFilter.shouldRun( testMethodInSecondClass ) );
        assertTrue( "should run other than testMethod", exactFilter.shouldRun( secondTestMethodInSecondClass ) );

        exactFilter =
            createMethodFilter( "!FilterFactoryTest$FirstClass#testMethod, !FilterFactoryTest$SecondClass#testMethod" );
        assertFalse( "should not run testMethod", exactFilter.shouldRun( testMethod ) );
        assertTrue( "should run other than testMethod", exactFilter.shouldRun( secondTestMethod ) );
        assertFalse( "should not run testMethod", exactFilter.shouldRun( testMethodInSecondClass ) );
        assertTrue( "should run other than testMethod", exactFilter.shouldRun( secondTestMethodInSecondClass ) );
    }

    @Test
    public void testShouldMatchExactMethodNameWithHash()
    {
        final Filter exactFilter = createMethodFilter( "#testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
    }

    @Test
    public void testShouldRunSuiteWithIncludedMethod()
    {
        String sourceFile = "pkg" + File.separator + "XMyTest.class";
        assertTrue( new TestListResolver( "#testMethod" ).shouldRun( sourceFile, null ) );
    }

    @Test
    public void testShouldNotRunDifferentMethods()
    {
        final Filter exactFilter = createMethodFilter( "#testMethod" );
        Description testCase = createSuiteDescription( FirstClass.class );
        testCase.addChild( otherMethod );
        assertFalse( "exact match test case", exactFilter.shouldRun( testCase ) );
    }

    @Test
    public void testShouldNotRunExactMethodWithoutClass()
    {
        Filter exactFilter = createMethodFilter( "#testMethod" );
        assertFalse( "should run containing matching method", exactFilter.shouldRun( secondTestMethod ) );
    }

    @Test
    public void testShouldNotMatchExactOnOtherMethod()
    {
        Filter exactFilter = createMethodFilter( "#testMethod" );
        assertFalse( "should not run other methods", exactFilter.shouldRun( otherMethod ) );
    }

    @Test
    public void testShouldMatchWildCardsInMethodName()
    {
        Filter starAtEnd = createMethodFilter( "#test*" );
        assertTrue( "match ending with star should run", starAtEnd.shouldRun( testMethod ) );

        Filter starAtBeginning = createMethodFilter( "#*Method" );
        assertTrue( "match starting with star should run", starAtBeginning.shouldRun( testMethod ) );

        Filter starInMiddle = createMethodFilter( "#test*thod" );
        assertTrue( "match containing star should run", starInMiddle.shouldRun( testMethod ) );

        Filter questionAtEnd = createMethodFilter( "#testMetho?" );
        assertTrue( "match ending with question mark should run", questionAtEnd.shouldRun( testMethod ) );

        Filter questionAtBeginning = createMethodFilter( "#????Method" );
        assertTrue( "match starting with question mark should run", questionAtBeginning.shouldRun( testMethod ) );

        Filter questionInMiddle = createMethodFilter( "#testM?thod" );
        assertTrue( "match containing question mark should run", questionInMiddle.shouldRun( testMethod ) );

        Filter starAndQuestion = createMethodFilter( "#t?st*thod" );
        assertTrue( "match containing star and question mark should run", starAndQuestion.shouldRun( testMethod ) );
    }

    @Test
    public void testShouldMatchExactClassAndMethod()
    {
        Filter exactFilter = createMethodFilter( firstClassName + "#testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
    }

    @Test
    public void testShouldMatchSimpleClassNameWithMethod()
    {
        Filter exactFilter = createMethodFilter( "FilterFactoryTest$FirstClass#testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    @Test
    public void testShouldMatchNestedClassAsRegexWithMethod()
    {
        Filter exactFilter =
            createMethodFilter( "%regex[.*.common.junit48.FilterFactoryTest\\$FirstClass.class#testMethod]" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    @Test
    public void testShouldMatchNestedCanonicalClassAsRegexWithMethod()
    {
        Filter exactFilter =
            createMethodFilter( "%regex[.*.common.junit48.FilterFactoryTest.FirstClass.class#testMethod]" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    @Test
    public void testShouldMatchClassNameWithWildcardAndMethod()
    {
        Filter exactFilter = createMethodFilter( "*First*#testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    @Test
    public void testShouldMatchClassNameWithWildcardCompletely()
    {
        Filter exactFilter = createMethodFilter( "First*#testMethod" );
        assertFalse( "other method should not match", exactFilter.shouldRun( testMethod ) );
        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    @Test
    public void testShouldMatchMultipleMethodsSeparatedByComma()
    {
        Filter exactFilter = createMethodFilter( firstClassName + "#testMethod,#secondTestMethod" );

        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( secondTestMethod ) );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( secondTestMethodInSecondClass ) );

        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    @Test
    public void testShouldMatchMultipleMethodsInSameClassSeparatedByPlus()
    {
        Filter exactFilter = createMethodFilter( firstClassName + "#testMethod+secondTestMethod" );

        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( secondTestMethod ) );

        assertFalse( "method in another class should not match",
                     exactFilter.shouldRun( secondTestMethodInSecondClass ) );

        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    @Test
    public void testShouldRunCompleteClassWhenSeparatedByCommaWithoutHash()
    {
        Filter exactFilter = createMethodFilter( firstClassName + "#testMethod," + secondClassName );

        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );

        assertFalse( "other method should not match", exactFilter.shouldRun( secondTestMethod ) );
        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );

        assertTrue( "should run complete second class", exactFilter.shouldRun( testMethodInSecondClass ) );
        assertTrue( "should run complete second class", exactFilter.shouldRun( secondTestMethodInSecondClass ) );
    }

    @Test
    public void testShouldRunSuitesContainingExactMethodName()
    {
        Description suite = Description.createSuiteDescription( Suite.class );
        suite.addChild( testMethod );
        suite.addChild( secondTestMethod );

        Filter exactFilter = createMethodFilter( "#testMethod" );
        assertTrue( "should run suites containing matching method", exactFilter.shouldRun( suite ) );
    }

    @Test
    public void testShouldSkipSuitesNotContainingExactMethodName()
    {
        Filter exactFilter = createMethodFilter( "#otherMethod" );
        assertFalse( "should not run method", exactFilter.shouldRun( testMethod ) );
        assertFalse( "should not run method", exactFilter.shouldRun( secondTestMethod ) );
        Description suite = Description.createSuiteDescription( Suite.class );
        suite.addChild( testMethod );
        suite.addChild( secondTestMethod );
        assertFalse( "should not run suites containing no matches", exactFilter.shouldRun( suite ) );
    }

    @Test
    public void testSingleMethodWithJUnitCoreSuite()
    {
        Filter filter = createMethodFilter( "#testMethod" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( Suite.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 2, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testShouldNotRunNonExistingMethodJUnitCoreSuite()
    {
        Filter filter = createMethodFilter( "#nonExisting" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( Suite.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 0, result.getRunCount() ); //running the Suite
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testShouldRunNonExistingMethodJUnitCoreSuite()
    {
        Filter filter = createMethodFilter( "!#nonExisting" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( Suite.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 5, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testClassAndMethodJUnitCoreSuite()
    {
        Filter filter = createMethodFilter( "FilterFactoryTest$FirstClass#testMethod" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( Suite.class, FirstClass.class, SecondClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 2, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testSingleMethodWithJUnitCoreFirstClass()
    {
        Filter filter = createMethodFilter( "#testMethod" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 1, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testWithJUnitCoreFirstClassAndSingleMethod()
    {
        Filter filter = createMethodFilter( "FilterFactoryTest$FirstClass#testMethod" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 1, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testShouldRunSuite()
    {
        TestListResolver filter = new TestListResolver( "Su?te" );
        filter = TestListResolver.optionallyWildcardFilter( filter );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( Suite.class ).filterWith( new MethodFilter( filter ) ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 5, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testShouldRunParameterized()
    {
        TestListResolver filter =
            new TestListResolver( "#testAA[?]+testB?[?], "
                                      + "PT#testC*, "
                                      + "!PT.java#testCY[?],"
                                      + "%regex[.*.tests.pt.PT.class#w.*|x.*T.*]" );
        filter = TestListResolver.optionallyWildcardFilter( filter );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( PT.class ).filterWith( new MethodFilter( filter ) ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 12, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testShouldRunParameterizedWithPlusDelimiter()
    {
        // Running parameterized tests: w12T34, x12T34 and x12T35.
        // Two parameters "x" and "y" in the test case PT.java change the method descriptions to the following ones:
        // w12T34[0], w12T34[1]
        // x12T34[0], x12T34[1]
        // x12T35[0], x12T35[1]
        TestListResolver filter =
            new TestListResolver( "%regex[.*.PT.* # w.*|x(\\d+)T(\\d+)\\[(\\d+)\\]]" );
        filter = TestListResolver.optionallyWildcardFilter( filter );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( PT.class ).filterWith( new MethodFilter( filter ) ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 6, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    @Test
    public void testTestListResolver()
    {
        assertFalse( new TestListResolver( "b/ATest.java" ).shouldRun( "tests/a/ATest.class", null ) );
        assertFalse( new TestListResolver( "b/Test.java" ).shouldRun( "a/ATest.class", null ) );
        assertTrue( new TestListResolver( "ATest.java" ).shouldRun( "tests/a/ATest.class", null ) );
        assertTrue( new TestListResolver( "a/ATest.java" ).shouldRun( "a/ATest.class", null ) );
        assertTrue( new TestListResolver( "**/ATest.java" ).shouldRun( "a/ATest.class", null ) );
        Class<?> testsATest = org.apache.maven.surefire.common.junit48.tests.ATest.class;
        Class<?> aTest = org.apache.maven.surefire.common.junit48.tests.a.ATest.class;
        assertFalse( new TestListResolver( "b/ATest.java" ).shouldRun( testsATest, null ) );
        assertFalse( new TestListResolver( "b/ATest.java" ).shouldRun( aTest, null ) );
        assertTrue( new TestListResolver( "ATest.java" ).shouldRun( testsATest, null ) );
        assertTrue( new TestListResolver( "a/ATest.java" ).shouldRun( aTest, null ) );
        assertTrue( new TestListResolver( "**/ATest.java" ).shouldRun( aTest, null ) );
    }

    @Test
    public void testShouldRunClassOnly()
    {
        Class<?> testsATest = org.apache.maven.surefire.common.junit48.tests.ATest.class;
        TestListResolver resolver = new TestListResolver( "**/ATest.java#testSuccessTwo" );
        assertTrue( resolver.shouldRun( testsATest, null ) );
        resolver = new TestListResolver( "**/BTest.java#testSuccessTwo" );
        assertFalse( resolver.shouldRun( testsATest, null ) );
    }

    @Test
    public void testMatchPatterns()
    {
        String sourceFile = "pkg" + File.separator + "MyTest.class";
        boolean matchPattern =
            MatchPatterns.from( "**" + File.separator + "MyTest.class" ).matches( sourceFile, true );
        assertTrue( matchPattern );

        matchPattern = MatchPatterns.from( "MyTest.class" ).matches( sourceFile, true );
        assertFalse( matchPattern );

        matchPattern = MatchPatterns.from( "MyTest.class" ).matches( "MyTest.class", true );
        assertTrue( matchPattern );

        matchPattern = MatchPatterns.from( "**" + File.separator + "MyTest.class" ).matches( "MyTest.class", true );
        assertTrue( matchPattern );
    }

    @Test
    public void testNegativePatternOnPackageLessClass()
    {
        String sourceFile = "pkg" + File.separator + "XMyTest.class";
        assertFalse( new TestListResolver( "**/MyTest.java" ).shouldRun( sourceFile, null ) );
        assertFalse( new TestListResolver( "MyTest.java" ).shouldRun( sourceFile, null ) );
        assertFalse( new TestListResolver( "MyTest.java" ).shouldRun( "XMyTest.class", null ) );
        assertFalse( new TestListResolver( "**/MyTest.java" ).shouldRun( "XMyTest.class", null ) );
    }

    @Test
    public void testPatternOnPackageLessClass()
    {
        String sourceFile = "pkg" + File.separator + "MyTest.class";
        assertTrue( new TestListResolver( "**/MyTest.java" ).shouldRun( sourceFile, null ) );
        assertTrue( new TestListResolver( "MyTest.java" ).shouldRun( sourceFile, null ) );
        assertTrue( new TestListResolver( "MyTest.java" ).shouldRun( "MyTest.class", null ) );
        assertTrue( new TestListResolver( "**/MyTest.java" ).shouldRun( "MyTest.class", null ) );
    }

    @Test
    public void testIncludesExcludes()
    {
        Collection<String> inc = Arrays.asList( "**/NotIncludedByDefault.java", "**/*Test.java" );
        Collection<String> exc = Collections.singletonList( "**/DontRunTest.*" );
        TestListResolver resolver = new TestListResolver( inc, exc );
        assertFalse( resolver.shouldRun( "org/test/DontRunTest.class", null ) );
        assertTrue( resolver.shouldRun( "org/test/DefaultTest.class", null ) );
        assertTrue( resolver.shouldRun( "org/test/NotIncludedByDefault.class", null ) );
    }

    @Test
    public void testSimple()
    {
        TestListResolver resolver = new TestListResolver( "NotIncludedByDefault" );
        assertTrue( resolver.shouldRun( "org/test/NotIncludedByDefault.class", null ) );
    }

    @Test
    public void testFullyQualifiedClass()
    {
        TestListResolver resolver = new TestListResolver( "my.package.MyTest" );
        assertFalse( resolver.shouldRun( "my/package/AnotherTest.class", null ) );
        assertTrue( resolver.shouldRun( "my/package/MyTest.class", null ) );
    }
}
