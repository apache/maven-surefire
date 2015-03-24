package org.apache.maven.surefire.testset;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class TestListResolverTest
    extends TestCase
{
    public void testRegexSanity1()
    {
        try
        {
            TestListResolver.isRegexPrefixedPattern( "#%regex[]" );
            fail( "#%regex[]" );
        }
        catch ( IllegalArgumentException e )
        {
            // expected in junit 3.x
        }
    }

    public void testRegexSanity2()
    {
        try
        {
            TestListResolver.isRegexPrefixedPattern( "%regex[]#" );
            fail( "%regex[]#" );
        }
        catch ( IllegalArgumentException e )
        {
            // expected in junit 3.x
        }
    }

    public void testRegexSanity3()
    {
        try
        {
            TestListResolver.isRegexPrefixedPattern( "%regex[]%regex[]" );
            fail( "%regex[]%regex[]" );
        }
        catch ( IllegalArgumentException e )
        {
            // expected in junit 3.x
        }
    }

    public void testRemoveExclamationMark()
    {
        String pattern = TestListResolver.removeExclamationMark( "!%regex[]" );
        assertEquals( "%regex[]", pattern );
        pattern = TestListResolver.removeExclamationMark( "%regex[]" );
        assertEquals( "%regex[]", pattern );
    }

    public void testUnwrapped()
    {
        String[] classAndMethod = TestListResolver.unwrap( " MyTest " );
        assertEquals( "MyTest", classAndMethod[0] );
        assertEquals( "", classAndMethod[1] );
        classAndMethod = TestListResolver.unwrap( " # test " );
        assertEquals( "", classAndMethod[0] );
        assertEquals( "test", classAndMethod[1] );
        classAndMethod = TestListResolver.unwrap( " MyTest # test " );
        assertEquals( "MyTest", classAndMethod[0] );
        assertEquals( "test", classAndMethod[1] );
    }

    public void testUnwrappedRegex()
    {
        String[] classAndMethod = TestListResolver.unwrapRegex( "%regex[ .*.MyTest.class ]" );
        assertEquals( ".*.MyTest.class", classAndMethod[0] );
        assertEquals( "", classAndMethod[1] );
        classAndMethod = TestListResolver.unwrapRegex( "%regex[ # myMethod|secondTest ]" );
        assertEquals( "", classAndMethod[0] );
        assertEquals( "myMethod|secondTest", classAndMethod[1] );
        classAndMethod = TestListResolver.unwrapRegex( "%regex[ .*.MyTest.class # myMethod|secondTest ]" );
        assertEquals( ".*.MyTest.class", classAndMethod[0] );
        assertEquals( "myMethod|secondTest", classAndMethod[1] );
    }

    public void testMakeRegex()
    {
        String regex = ResolvedTest.wrapRegex( ".*.MyTest.class" );
        assertEquals( "%regex[.*.MyTest.class]", regex );
    }

    public void testNonRegexClassAndMethod()
    {
        Collection<ResolvedTest> includedFilters = new ArrayList<ResolvedTest>();
        Collection<ResolvedTest> excludedFilters = new ArrayList<ResolvedTest>();
        IncludedExcludedPatterns includedExcludedPatterns = new IncludedExcludedPatterns();
        TestListResolver.nonRegexClassAndMethods( "MyTest", "myTest", false, includedExcludedPatterns, includedFilters,
                                                  excludedFilters );
        assertTrue( includedExcludedPatterns.hasIncludedMethodPatterns );
        assertFalse( includedExcludedPatterns.hasExcludedMethodPatterns );
        assertFalse( includedFilters.isEmpty() );
        assertTrue( excludedFilters.isEmpty() );
        assertEquals( 1, includedFilters.size() );
        ResolvedTest test = includedFilters.iterator().next();
        assertFalse( test.isEmpty() );
        assertFalse( test.isRegexTestClassPattern() );
        assertFalse( test.isRegexTestMethodPattern() );
        assertTrue( test.hasTestClassPattern() );
        assertTrue( test.hasTestMethodPattern() );
        assertEquals( "**/MyTest", test.getTestClassPattern() );
        assertEquals( "myTest", test.getTestMethodPattern() );
        assertTrue( test.shouldRun( "MyTest", "myTest" ) );
        assertFalse( test.shouldRun( "MyTest", "otherTest" ) );
    }

    public void testNonRegexClassAndMethods()
    {
        Collection<ResolvedTest> includedFilters = new ArrayList<ResolvedTest>();
        Collection<ResolvedTest> excludedFilters = new ArrayList<ResolvedTest>();
        IncludedExcludedPatterns includedExcludedPatterns = new IncludedExcludedPatterns();
        TestListResolver.nonRegexClassAndMethods( "MyTest.class", "first*+second*", false, includedExcludedPatterns,
                                                  includedFilters, excludedFilters );
        assertTrue( includedExcludedPatterns.hasIncludedMethodPatterns );
        assertFalse( includedExcludedPatterns.hasExcludedMethodPatterns );
        assertFalse( includedFilters.isEmpty() );
        assertTrue( excludedFilters.isEmpty() );
        assertEquals( 2, includedFilters.size() );
        Iterator<ResolvedTest> tests = includedFilters.iterator();
        ResolvedTest first = tests.next();
        assertFalse( first.isEmpty() );
        assertFalse( first.isRegexTestClassPattern() );
        assertFalse( first.isRegexTestMethodPattern() );
        assertTrue( first.hasTestClassPattern() );
        assertTrue( first.hasTestMethodPattern() );
        assertEquals( "**/MyTest.class", first.getTestClassPattern() );
        assertEquals( "first*", first.getTestMethodPattern() );
        assertTrue( first.shouldRun( "your/pkg/MyTest.class", "firstTest" ) );
        ResolvedTest second = tests.next();
        assertFalse( second.isEmpty() );
        assertFalse( second.isRegexTestClassPattern() );
        assertFalse( second.isRegexTestMethodPattern() );
        assertTrue( second.hasTestClassPattern() );
        assertTrue( second.hasTestMethodPattern() );
        assertEquals( "**/MyTest.class", second.getTestClassPattern() );
        assertEquals( "second*", second.getTestMethodPattern() );
        assertTrue( second.shouldRun( "your/pkg/MyTest.class", "secondTest" ) );
        assertFalse( second.shouldRun( "your/pkg/MyTest.class", "thirdTest" ) );
    }

    public void testNegativeNonRegexClassAndMethod()
    {
        Collection<ResolvedTest> includedFilters = new ArrayList<ResolvedTest>();
        Collection<ResolvedTest> excludedFilters = new ArrayList<ResolvedTest>();
        IncludedExcludedPatterns includedExcludedPatterns = new IncludedExcludedPatterns();
        TestListResolver.nonRegexClassAndMethods( "MyTest", "myTest", true, includedExcludedPatterns, includedFilters,
                                                  excludedFilters );
        assertFalse( includedExcludedPatterns.hasIncludedMethodPatterns );
        assertTrue( includedExcludedPatterns.hasExcludedMethodPatterns );
        assertTrue( includedFilters.isEmpty() );
        assertEquals( 1, excludedFilters.size() );
        ResolvedTest test = excludedFilters.iterator().next();
        assertFalse( test.isEmpty() );
        assertFalse( test.isRegexTestClassPattern() );
        assertFalse( test.isRegexTestMethodPattern() );
        assertTrue( test.hasTestClassPattern() );
        assertTrue( test.hasTestMethodPattern() );
        assertEquals( "**/MyTest", test.getTestClassPattern() );
        assertEquals( "myTest", test.getTestMethodPattern() );
        // ResolvedTest should not care about isExcluded. This attribute is handled by TestListResolver.
        assertTrue( test.shouldRun( "MyTest", "myTest" ) );
        assertFalse( test.shouldRun( "MyTest", "otherTest" ) );
        assertFalse( test.shouldRun( "pkg/OtherTest.class", "myTest" ) );
    }

    public void testResolveTestRequest()
    {
        Collection<ResolvedTest> includedFilters = new ArrayList<ResolvedTest>();
        Collection<ResolvedTest> excludedFilters = new ArrayList<ResolvedTest>();
        IncludedExcludedPatterns includedExcludedPatterns = new IncludedExcludedPatterns();
        TestListResolver.resolveTestRequest( "!%regex[.*.MyTest.class#myTest]", includedExcludedPatterns,
                                             includedFilters, excludedFilters );
        assertFalse( includedExcludedPatterns.hasIncludedMethodPatterns );
        assertTrue( includedExcludedPatterns.hasExcludedMethodPatterns );
        assertTrue( includedFilters.isEmpty() );
        assertFalse( excludedFilters.isEmpty() );
        assertEquals( 1, excludedFilters.size() );
        ResolvedTest test = excludedFilters.iterator().next();
        // ResolvedTest should not care about isExcluded. This attribute is handled by TestListResolver.
        assertTrue( test.shouldRun( "pkg/MyTest.class", "myTest" ) );
        assertFalse( test.shouldRun( "pkg/MyTest.class", "otherTest" ) );
        assertFalse( test.shouldRun( "pkg/OtherTest.class", "myTest" ) );
    }

    public void testShouldNotRunExcludedMethods()
    {
        TestListResolver resolver = new TestListResolver( "!#*Fail*, !%regex[#.*One], !#testSuccessThree" );
        assertTrue( resolver.shouldRun( "pkg/MyTest.class", null ) );
    }

    public void testShouldNotRunIncludedMethods()
    {
        TestListResolver resolver = new TestListResolver( "#*Fail*, %regex[#.*One], #testSuccessThree" );
        assertFalse( resolver.shouldRun( "pkg/MyTest.class", null ) );
    }

    public void testShouldRunAny()
    {
        TestListResolver resolver = new TestListResolver( "" );
        assertTrue( resolver.shouldRun( "pkg/MyTest.class", null ) );

        resolver = new TestListResolver( Collections.<String>emptySet() );
        assertTrue( resolver.shouldRun( "pkg/MyTest.class", null ) );
    }

    public void testClassFilter()
    {
        TestListResolver resolver = new TestListResolver( "#test" );
        assertTrue( resolver.createClassFilters().shouldRun( "pkg/MyTest.class", null ) );

        resolver = new TestListResolver( "!#test" );
        assertTrue( resolver.createClassFilters().shouldRun( "pkg/MyTest.class", null ) );

        resolver = new TestListResolver( "SomeOtherClass" );
        assertFalse( resolver.createClassFilters().shouldRun( "pkg/MyTest.class", null ) );
    }

    public void testBrokenPatternThrowsException()
    {
        Collection<String> included = Collections.emptySet();
        Collection<String> excluded = Arrays.asList( "BasicTest, !**/TestTwo, **/TestThree.java" );
        try
        {
            new TestListResolver( included, excluded );
            fail( "Expected: IllegalArgumentException" );
        } catch ( IllegalArgumentException e )
        {
            // JUnit 3.x style
            assertEquals( "Exclamation mark not expected in 'exclusion': BasicTest, !**/TestTwo, **/TestThree.java",
                          e.getLocalizedMessage() );
        }
    }

    public void testMultipleExcludedClassesOnly()
    {
        Collection<String> included = Collections.emptySet();
        Collection<String> excluded = Arrays.asList( "BasicTest, **/TestTwo, **/TestThree.java" );
        TestListResolver resolver = new TestListResolver( included, excluded );
        assertFalse( resolver.shouldRun( "jiras/surefire745/BasicTest.class", null ) );
        assertFalse( resolver.shouldRun( "jiras/surefire745/TestTwo.class", null ) );
        assertFalse( resolver.shouldRun( "jiras/surefire745/TestThree.class", null ) );
        assertTrue( resolver.shouldRun( "jiras/surefire745/TestFour.class", null ) );
    }

    public void testMultipleExcludedClasses()
    {
        Collection<String> included = Arrays.asList( "**/Test*.java, **/*Test.java, **/*TestCase.java" );
        Collection<String> excluded = Arrays.asList( "BasicTest, **/TestTwo, **/TestThree.java" );
        TestListResolver resolver = new TestListResolver( included, excluded );
        assertFalse( resolver.shouldRun( "jiras/surefire745/BasicTest.class", null ) );
        assertFalse( resolver.shouldRun( "jiras/surefire745/TestTwo.class", null ) );
        assertFalse( resolver.shouldRun( "jiras/surefire745/TestThree.class", null ) );
        assertTrue( resolver.shouldRun( "jiras/surefire745/TestFour.class", null ) );
    }

    public void testMultipleExcludedClassesWithNoSpecificTests()
    {
        Collection<String> included = Arrays.asList( "**/Test*.java, **/*Test.java, **/*TestCase.java" );
        Collection<String> excluded = Arrays.asList( "BasicTest, **/TestTwo, **/TestThree.java" );

        TestListResolver includedAndExcludedTests = new TestListResolver( included, excluded );
        TestListResolver includedExcludedClasses = includedAndExcludedTests.createClassFilters();

        TestListResolver specificTests = new TestListResolver( "" );
        TestListResolver specificClasses = specificTests.createClassFilters();

        TestFilter<String, String> filter = includedExcludedClasses.and( specificClasses );

        assertFalse( filter.shouldRun( "jiras/surefire745/BasicTest.class", null ) );
        assertFalse( filter.shouldRun( "jiras/surefire745/TestTwo.class", null ) );
        assertFalse( filter.shouldRun( "jiras/surefire745/TestThree.class", null ) );
        assertTrue( filter.shouldRun( "jiras/surefire745/TestFour.class", null ) );
    }
}
