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

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.internal.builders.AllDefaultPossibilitiesBuilder;
import org.junit.runner.Computer;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.runner.Description.createTestDescription;

public class FilterFactoryTest
    extends TestCase
{
    @RunWith( org.junit.runners.Suite.class )
    @org.junit.runners.Suite.SuiteClasses( { FirstClass.class, SecondClass.class } )
    static public class Suite
    {

    }

    static public class FirstClass
    {
        @Test
        public void testMethod()
        {
            System.out.println( "FirstClass#testMethod" );
        }

        @Test
        public void secondTestMethod()
        {
            System.out.println( "FirstClass#secondTestMethod" );
        }

        @Test
        public void otherMethod()
        {
            System.out.println( "FirstClass#otherMethod" );
        }
    }

    static public class SecondClass
    {
        @Test
        public void testMethod()
        {
            System.out.println( "SecondClass#testMethod" );
        }

        @Test
        public void secondTestMethod()
        {
            System.out.println( "SecondClass#secondTestMethod" );
        }
    }

    static public class ThirdClass
    {
        @Test
        public void testMethod()
        {
            System.out.println( "ThirdClass#testMethod" );
        }

        @Test
        public void secondTestMethod()
        {
            System.out.println( "ThirdClass#secondTestMethod" );
        }
    }

    private static final Description testMethod = createTestDescription( FirstClass.class, "testMethod" );

    private static final Description secondTestMethod = createTestDescription( FirstClass.class, "secondTestMethod" );

    private static final Description otherMethod = createTestDescription( FirstClass.class, "otherMethod" );

    private static final Description testMethodInSecondClass = createTestDescription( SecondClass.class, "testMethod" );

    private static final Description secondTestMethodInSecondClass =
        createTestDescription( SecondClass.class, "secondTestMethod" );

    private static final String firstClassName = FirstClass.class.getName();

    private static final String secondClassName = SecondClass.class.getName();

    private Filter createMethodFilter( String requestString )
    {
        return new FilterFactory( getClass().getClassLoader() ).createMethodFilter( requestString );
    }

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

    public void testBackwardsCompatibilityTestParameterClass() {
        Filter filter = createMethodFilter( FirstClass.class.getName() );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testBackwardsCompatibilityTestParameterJavaClass() {
        Filter filter = createMethodFilter( FirstClass.class.getName() + ".java" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testBackwardsCompatibilityTestParameterMethod1() {
        Filter filter = createMethodFilter( FirstClass.class.getName().replace( '.', '/' ) + ".java#testMethod"  );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 1, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testBackwardsCompatibilityTestParameterMethod2() {
        Filter filter = createMethodFilter( FirstClass.class.getName().replace( '.', '/' ) + "#testMethod"  );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 1, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testBackwardsCompatibilityTestParameterMethod3() {
        Filter filter = createMethodFilter( FirstClass.class.getName() + "#testMethod"  );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 1, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithWildcard() {
        Filter filter = createMethodFilter( "%regex[" + FirstClass.class.getName() + ".*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithWildcardClass() {
        Filter filter = createMethodFilter( "%regex[" + FirstClass.class.getName() + ".*.class]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithExactClass() {
        Filter filter = createMethodFilter( "%regex[" + FirstClass.class.getName() + ".class]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithWildcardJavaClassNegativeTest() {
        Filter filter = createMethodFilter( "%regex[" + FirstClass.class.getName().replace( '.', '/' ) + ".*.java]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class ).filterWith( filter ) );
        assertFalse( result.wasSuccessful() );
    }

    public void testRegexWithTwoClasses() {
        Filter filter = createMethodFilter( "%regex[" + FirstClass.class.getName() + ".*"
                                                + "|" + SecondClass.class.getName() + ".*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 5, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithTwoClassesComplement() {
        Filter filter = createMethodFilter( "!%regex[" + FirstClass.class.getName() + ".*"
                                                + "|" + SecondClass.class.getName() + ".*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 2, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithTwoClassesAndOneMethod() {
        Filter filter = createMethodFilter( "%regex[" + FirstClass.class.getName() + ".*"
                                                + "|" + SecondClass.class.getName() + ".*]#otherMethod" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 1, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithTwoClassesAndOneMethodComplement() {
        Filter filter = createMethodFilter( "!%regex[" + FirstClass.class.getName() + ".*"
                                                + "|" + SecondClass.class.getName() + ".*]#otherMethod" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 6, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithTwoClassesAndWildcardMethod() {
        Filter filter = createMethodFilter( "%regex[" + FirstClass.class.getName() + ".*"
                                                + "|" + SecondClass.class.getName() + ".*]#test*" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 2, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithTwoClassesAndWildcardMethodComplement() {
        Filter filter = createMethodFilter( "!%regex[" + FirstClass.class.getName() + ".*"
                                                + "|" + SecondClass.class.getName() + ".*]#test*" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 5, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithTwoClassesAndRegexMethod() {
        Filter filter = createMethodFilter( "%regex[" + FirstClass.class.getName() + ".*"
                                                + "|" + SecondClass.class.getName() + ".*]#%regex[test.*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 2, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithTwoClassesAndRegexMethodComplement() {
        Filter filter = createMethodFilter( "!%regex[" + FirstClass.class.getName() + ".*"
                                                + "|" + SecondClass.class.getName() + ".*]#%regex[test.*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 5, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithTwoClassesAndRegexMethods() {
        Filter filter = createMethodFilter( "%regex[" + FirstClass.class.getName() + ".*"
                                                + "|" + SecondClass.class.getName() + ".*]#%regex[test.*|other.*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithTwoClassesAndRegexMethodsComplement() {
        Filter filter = createMethodFilter( "!%regex[" + FirstClass.class.getName() + ".*"
                                                + "|" + SecondClass.class.getName() + ".*]#%regex[test.*|other.*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 4, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithTwoClassesAndMultipleRegexMethods() {
        Filter filter = createMethodFilter( "%regex[" + FirstClass.class.getName() + ".*"
                                                + "|" + SecondClass.class.getName() + ".*]"
                                                + "#%regex[test.*]+%regex[other.*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testRegexWithTwoClassesAndMultipleRegexMethodsComplement() {
        Filter filter = createMethodFilter( "!%regex[" + FirstClass.class.getName() + ".*"
                                                + "|" + SecondClass.class.getName() + ".*]"
                                                + "#%regex[test.*]+%regex[other.*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 4, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testMultipleRegexClasses() {
        Filter filter = createMethodFilter( "%regex[" + FirstClass.class.getName() + ".*],"
                                                + "%regex[" + SecondClass.class.getName() + ".*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 5, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testMultipleRegexClassesComplement() {
        Filter filter = createMethodFilter( "!%regex[" + FirstClass.class.getName() + ".*],"
                                                + "!%regex[" + SecondClass.class.getName() + ".*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 2, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testMultipleClasses() {
        Filter filter = createMethodFilter( FirstClass.class.getName() + "," + SecondClass.class.getName() );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 5, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testMultipleClassesMethods() {
        Filter filter = createMethodFilter( FirstClass.class.getName() + "#other*,"
                                                + SecondClass.class.getName() + "#*TestMethod" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 2, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testMultipleClassesAndMultipleMethodsWithWildcards() {
        Filter filter = createMethodFilter( FirstClass.class.getName() + "#other*+second*Method,"
                                                + SecondClass.class.getName() + "#*TestMethod" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testMultipleClassesAndMultipleMethodsWithRegex() {
        Filter filter = createMethodFilter( FirstClass.class.getName() + "#%regex[other.*|second.*Method],"
                                                + SecondClass.class.getName() + "#%regex[.*TestMethod]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testMultipleClassesAndMultipleMethodsMix() {
        Filter filter = createMethodFilter( FirstClass.class.getName() + "#%regex[other.*|second.*Method],"
                                                + SecondClass.class.getName() + "#*TestMethod" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testMultipleClassesAndMultipleMethods() {
        Filter filter = createMethodFilter( FirstClass.class.getName() + "#other*+secondTestMethod,"
                                                + SecondClass.class.getName() + "#*TestMethod" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testMultipleClassesComplement() {
        Filter filter = createMethodFilter( "!" + FirstClass.class.getName() + ",!" + SecondClass.class.getName() );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 2, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testMultipleRegexClassesMethods() {
        Filter filter = createMethodFilter( "%regex[" + FirstClass.class.getName() + ".*]#%regex[test.*]+%regex[other.*],"
                                                + "%regex[" + SecondClass.class.getName() + ".*]#%regex[second.*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 3, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testMultipleRegexClassesMethodsComplement() {
        Filter filter = createMethodFilter( "!%regex[" + FirstClass.class.getName() + ".*]#%regex[test.*]+%regex[other.*],"
                                                + "!%regex[" + SecondClass.class.getName() + ".*]#%regex[second.*]" );
        JUnitCore core = new JUnitCore();
        Result result = core.run( Request.classes( FirstClass.class, SecondClass.class, ThirdClass.class )
                                      .filterWith( filter ) );
        assertTrue( result.wasSuccessful() );
        assertEquals( 4, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }

    public void testShouldMatchExactMethodName()
    {
        Filter exactFilter = createMethodFilter( "#testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
    }

    public void testShouldMatchExactMethodNameComplement()
    {
        Filter exactFilter = createMethodFilter( "!#testMethod" );
        assertFalse( "should not run testMethod", exactFilter.shouldRun( testMethod ) );
        assertTrue( "should run other than testMethod", exactFilter.shouldRun( secondTestMethod ) );
        assertFalse( "should not run testMethod", exactFilter.shouldRun( testMethodInSecondClass ) );
        assertTrue( "should run other than testMethod", exactFilter.shouldRun( secondTestMethodInSecondClass ) );
    }

    public void testShouldMatchExactMethodNameWithHash()
    {
        Filter exactFilter = createMethodFilter( "#testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
    }

    public void testShouldNotRunExactMethodWithoutClass()
    {
        Filter exactFilter = createMethodFilter( "#testMethod" );
        assertFalse( "should run containing matching method", exactFilter.shouldRun( secondTestMethod ) );
    }

    public void testShouldNotMatchExactOnOtherMethod()
    {
        Filter exactFilter = createMethodFilter( "#testMethod" );
        assertFalse( "should not run other methods", exactFilter.shouldRun( otherMethod ) );
    }

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

    public void testShouldMatchExactClassAndMethod()
    {
        Filter exactFilter = createMethodFilter( firstClassName + "#testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
    }

    public void testShouldMatchSimpleClassNameWithMethod()
    {
        Filter exactFilter = createMethodFilter( "FilterFactoryTest$FirstClass#testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    public void testShouldMatchClassNameWithWildcardAndMethod()
    {
        Filter exactFilter = createMethodFilter( "*First*#testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    public void testShouldMatchClassNameWithWildcardCompletely()
    {
        Filter exactFilter = createMethodFilter( "First*#testMethod" );
        assertFalse( "other method should not match", exactFilter.shouldRun( testMethod ) );
        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    public void testShouldMatchMultipleMethodsSeparatedByComma()
    {
        Filter exactFilter = createMethodFilter( firstClassName + "#testMethod,#secondTestMethod" );

        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( secondTestMethod ) );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( secondTestMethodInSecondClass ) );

        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    public void testShouldMatchMultipleMethodsInSameClassSeparatedByPlus()
    {
        Filter exactFilter = createMethodFilter( FirstClass.class.getName() + "#testMethod+secondTestMethod" );

        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( secondTestMethod ) );

        assertFalse( "method in another class should not match",
                     exactFilter.shouldRun( secondTestMethodInSecondClass ) );

        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    public void testShouldRunCompleteClassWhenSeparatedByCommaWithoutHash()
    {
        Filter exactFilter = createMethodFilter( firstClassName + "#testMethod," + secondClassName );

        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );

        assertFalse( "other method should not match", exactFilter.shouldRun( secondTestMethod ) );
        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );

        assertTrue( "should run complete second class", exactFilter.shouldRun( testMethodInSecondClass ) );
        assertTrue( "should run complete second class", exactFilter.shouldRun( secondTestMethodInSecondClass ) );
    }

    public void testShouldRunSuitesContainingExactMethodName()
    {
        Description suite = Description.createSuiteDescription( Suite.class );
        suite.addChild( testMethod );
        suite.addChild( secondTestMethod );

        Filter exactFilter = createMethodFilter( "#testMethod" );
        assertTrue( "should run suites containing matching method", exactFilter.shouldRun( suite ) );
    }

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

    /*public void testSuiteAggregator()
        throws InitializationError
    {

        class FilteringRequest
            extends Request
        {
            private Runner filteredRunner;

            public FilteringRequest( Request req, Filter filter )
            {
                try
                {
                    Runner runner = req.getRunner();
                    filter.apply( runner );
                    filteredRunner = runner;
                }
                catch ( NoTestsRemainException e )
                {
                    filteredRunner = null;
                }
            }

            @Override
            public Runner getRunner()
            {
                return filteredRunner;
            }
        }

        Request req = Request.classes( new Computer()
        {
            private final List<Runner> runners = new ArrayList<Runner>();

            @Override
            public Runner getSuite( RunnerBuilder builder, Class<?>[] classes )
                throws InitializationError
            {
                super.getSuite( builder, classes );
                return new org.junit.runners.Suite( (Class<?>) null, runners ) {};
            }

            @Override
            protected Runner getRunner( RunnerBuilder builder, Class<?> testClass )
                throws Throwable
            {
                Runner runner = new org.junit.runners.Suite( (Class<?>) null, Arrays.asList( super.getRunner( builder, testClass ) ) ) {};
                runners.add( runner );
                return runner;
            }
        }, Suite.class );
        Filter filter = createMethodFilter( "FilterFactoryTest$Suite" );
        Request request = new FilteringRequest( req, filter );
        JUnitCore core = new JUnitCore();
        Result result = core.run( request );
        assertTrue( result.wasSuccessful() );
        assertEquals( 2, result.getRunCount() );
        assertEquals( 0, result.getFailureCount() );
        assertEquals( 0, result.getIgnoreCount() );
    }*/

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
}
