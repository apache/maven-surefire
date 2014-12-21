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

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.runner.Description.createTestDescription;

public class FilterFactoryTest
{
    static class Suite
    {

    }

    static class FirstClass
    {

    }

    static class SecondClass {

    }

    private final Description testMethod = createTestDescription( FirstClass.class, "testMethod" );
    private final Description secondTestMethod = createTestDescription( FirstClass.class, "secondTestMethod" );
    private final Description otherMethod = createTestDescription( FirstClass.class, "otherMethod" );
    private final Description testMethodInSecondClass = createTestDescription( SecondClass.class, "testMethod" );
    private final Description secondTestMethodInSecondClass = createTestDescription( SecondClass.class,
            "secondTestMethod" );

    private final String firstClassName = FirstClass.class.getName();
    private final String secondClassName = SecondClass.class.getName();

    private Filter createMethodFilter( String requestString )
    {
        return new FilterFactory( getClass().getClassLoader() ).createMethodFilter( requestString );
    }

    @Test
    public void shouldMatchExactMethodName()
    {
        Filter exactFilter = createMethodFilter( "testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
    }

    @Test
    public void shouldMatchExactMethodNameWithHash()
    {
        Filter exactFilter = createMethodFilter( "#testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
    }

    @Test
    public void shouldNotMatchExactOnOtherMethod()
    {
        Filter exactFilter = createMethodFilter( "testMethod" );
        assertFalse( "should not run other methods", exactFilter.shouldRun( otherMethod ) );
    }

    @Test
    public void shouldMatchWildCardsInMethodName()
    {
        Filter starAtEnd = createMethodFilter( "test*" );
        assertTrue( "match ending with star should run", starAtEnd.shouldRun( testMethod ) );

        Filter starAtBeginning = createMethodFilter( "*Method" );
        assertTrue( "match starting with star should run", starAtBeginning.shouldRun( testMethod ) );

        Filter starInMiddle = createMethodFilter( "test*thod" );
        assertTrue( "match containing star should run", starInMiddle.shouldRun( testMethod ) );

        Filter questionAtEnd = createMethodFilter( "testMetho?" );
        assertTrue( "match ending with question mark should run", questionAtEnd.shouldRun( testMethod ) );

        Filter questionAtBeginning = createMethodFilter( "????Method" );
        assertTrue( "match starting with question mark should run", questionAtBeginning.shouldRun( testMethod ) );

        Filter questionInMiddle = createMethodFilter( "testM?thod" );
        assertTrue( "match containing question mark should run", questionInMiddle.shouldRun( testMethod ) );

        Filter starAndQuestion = createMethodFilter( "t?st*thod" );
        assertTrue( "match containing star and question mark should run", starAndQuestion.shouldRun( testMethod ) );
    }

    @Test
    public void shouldMatchExactClassAndMethod()
    {
        Filter exactFilter = createMethodFilter( firstClassName + "#testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
    }

    @Test
    public void shouldMatchSimpleClassNameWithMethod()
    {
        Filter exactFilter = createMethodFilter( "FilterFactoryTest$FirstClass#testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    @Test
    public void shouldMatchClassNameWithWildcardAndMethod()
    {
        Filter exactFilter = createMethodFilter( "*First*#testMethod" );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    @Test
    public void shouldMatchClassNameWithWildcardCompletely()
    {
        Filter exactFilter = createMethodFilter( "First*#testMethod" );
        assertFalse( "other method should not match", exactFilter.shouldRun( testMethod ) );
        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }


    @Test
    public void shouldMatchMultipleMethodsSeparatedByComman()
    {
        Filter exactFilter = createMethodFilter( firstClassName + "#testMethod,#secondTestMethod" );

        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( secondTestMethod ) );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( secondTestMethodInSecondClass ) );

        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    @Test
    public void shouldMatchMultipleMethodsInSameClassSeparatedByPlus() {
        Filter exactFilter = createMethodFilter( FirstClass.class.getName() + "#testMethod+secondTestMethod" );

        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );
        assertTrue( "exact match on name should run", exactFilter.shouldRun( secondTestMethod ) );

        assertFalse( "method in another class should not match", exactFilter.shouldRun( secondTestMethodInSecondClass ) );

        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );
    }

    @Test
    public void shouldRunCompleteClassWhenSeparatedByCommaWithoutHash() {
        Filter exactFilter = createMethodFilter( firstClassName + "#testMethod," + secondClassName );

        assertTrue( "exact match on name should run", exactFilter.shouldRun( testMethod ) );

        assertFalse( "other method should not match", exactFilter.shouldRun( secondTestMethod ) );
        assertFalse( "other method should not match", exactFilter.shouldRun( otherMethod ) );

        assertTrue( "should run complete second class", exactFilter.shouldRun( testMethodInSecondClass ) );
        assertTrue( "should run complete second class", exactFilter.shouldRun( secondTestMethodInSecondClass ) );
    }

    @Test
    public void shouldRunSuitesContainingExactMethodName()
    {
        Description suite = Description.createSuiteDescription( Suite.class );
        suite.addChild( testMethod );
        suite.addChild( secondTestMethod );

        Filter exactFilter = createMethodFilter( "testMethod" );
        assertTrue( "should run suites containing matching method", exactFilter.shouldRun( suite ) );
    }

    @Test
    public void shouldSkipSuitesNotContainingExactMethodName()
    {
        Description suite = Description.createSuiteDescription( Suite.class );
        suite.addChild( testMethod );
        suite.addChild( secondTestMethod );

        Filter exactFilter = createMethodFilter( "otherMethod" );
        assertFalse( "should not run method", exactFilter.shouldRun( testMethod ) );
        assertFalse( "should not run method", exactFilter.shouldRun( secondTestMethod ) );
        assertFalse( "should not run suites containing no matches", exactFilter.shouldRun( suite ) );
    }

}
