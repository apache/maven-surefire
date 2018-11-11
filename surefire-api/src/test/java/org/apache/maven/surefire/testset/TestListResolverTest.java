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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.addAll;
import static org.apache.maven.surefire.testset.TestListResolver.newTestListResolver;
import static org.apache.maven.surefire.testset.ResolvedTest.Type.CLASS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestListResolverTest
    extends TestCase
{
    private static final String DEFAULT_SUREFIRE_INCLUDED_TEST_PATTERNS =
        "**/Test*.java, **/*Test.java, **/*TestCase.java";

    private static final String DEFAULT_SUREFIRE_EXCLUDED_TEST_PATTERNS = "**/*$*";

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

    public void testMinRegexLength()
    {
        assertFalse( TestListResolver.isRegexMinLength( "%regex[]" ) );
        assertFalse( TestListResolver.isRegexMinLength( "%regex[ ]" ) );
        assertTrue( TestListResolver.isRegexMinLength( "%regex[*Test]" ) );
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
        Collection<ResolvedTest> includedFilters = new ArrayList<>();
        Collection<ResolvedTest> excludedFilters = new ArrayList<>();
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
        assertTrue( test.matchAsInclusive( "MyTest", "myTest" ) );
        assertFalse( test.matchAsInclusive( "MyTest", "otherTest" ) );
    }

    public void testNonRegexClassAndMethods()
    {
        Collection<ResolvedTest> includedFilters = new ArrayList<>();
        Collection<ResolvedTest> excludedFilters = new ArrayList<>();
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
        assertTrue( first.matchAsInclusive( "your/pkg/MyTest.class", "firstTest" ) );
        ResolvedTest second = tests.next();
        assertFalse( second.isEmpty() );
        assertFalse( second.isRegexTestClassPattern() );
        assertFalse( second.isRegexTestMethodPattern() );
        assertTrue( second.hasTestClassPattern() );
        assertTrue( second.hasTestMethodPattern() );
        assertEquals( "**/MyTest.class", second.getTestClassPattern() );
        assertEquals( "second*", second.getTestMethodPattern() );
        assertTrue( second.matchAsInclusive( "your/pkg/MyTest.class", "secondTest" ) );
        assertFalse( second.matchAsInclusive( "your/pkg/MyTest.class", "thirdTest" ) );
    }

    public void testNegativeNonRegexClassAndMethod()
    {
        Collection<ResolvedTest> includedFilters = new ArrayList<>();
        Collection<ResolvedTest> excludedFilters = new ArrayList<>();
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
        assertTrue( test.matchAsInclusive( "MyTest", "myTest" ) );
        assertFalse( test.matchAsInclusive( "MyTest", "otherTest" ) );
        assertFalse( test.matchAsInclusive( "pkg/OtherTest.class", "myTest" ) );
    }

    public void testResolveTestRequest()
    {
        Collection<ResolvedTest> includedFilters = new ArrayList<>();
        Collection<ResolvedTest> excludedFilters = new ArrayList<>();
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
        assertTrue( test.matchAsInclusive( "pkg/MyTest.class", "myTest" ) );
        assertFalse( test.matchAsInclusive( "pkg/MyTest.class", "otherTest" ) );
        assertFalse( test.matchAsInclusive( "pkg/OtherTest.class", "myTest" ) );
    }

    public void testShouldRunTestWithoutMethod()
    {
        new TestListResolver("**/*Test.class, !%regex[.*.MyTest.class#myTest]").shouldRun( "pkg/MyTest.class", null );
    }

    public void testShouldNotRunExcludedMethods()
    {
        TestListResolver resolver = new TestListResolver( "!#*Fail*, !%regex[#.*One], !#testSuccessThree" );
        assertTrue( resolver.shouldRun( "pkg/MyTest.class", null ) );
    }

    public void testShouldRunSuiteWithIncludedMethods()
    {
        TestListResolver resolver = new TestListResolver( "#*Fail*, %regex[#.*One], #testSuccessThree" );
        assertTrue( resolver.shouldRun( "pkg/MyTest.class", null ) );
    }

    public void testShouldRunAny()
    {
        TestListResolver resolver = TestListResolver.getEmptyTestListResolver();
        assertTrue( resolver.shouldRun( "pkg/MyTest.class", null ) );

        resolver = new TestListResolver( Collections.<String>emptySet() );
        assertTrue( resolver.shouldRun( "pkg/MyTest.class", null ) );
    }

    public void testClassFilter()
    {
        TestListResolver resolver = new TestListResolver( "#test" );
        assertTrue( resolver.shouldRun( "pkg/MyTest.class", null ) );

        resolver = new TestListResolver( "!#test" );
        assertTrue( resolver.shouldRun( "pkg/MyTest.class", null ) );

        resolver = new TestListResolver( "SomeOtherClass" );
        assertFalse( resolver.shouldRun( "pkg/MyTest.class", null ) );
    }

    public void testBrokenPatternThrowsException()
    {
        Collection<String> included = emptySet();
        Collection<String> excluded = asList( "BasicTest, !**/TestTwo, **/TestThree.java" );
        try
        {
            new TestListResolver( included, excluded );
            fail( "Expected: IllegalArgumentException" );
        }
        catch ( IllegalArgumentException e )
        {
            // JUnit 3.x style
            assertEquals( "Exclamation mark not expected in 'exclusion': BasicTest, !**/TestTwo, **/TestThree.java",
                          e.getLocalizedMessage() );
        }
    }

    public void testMultipleExcludedClassesOnly()
    {
        Collection<String> included = emptySet();
        Collection<String> excluded = asList( "BasicTest, **/TestTwo, **/TestThree.java" );
        TestListResolver resolver = new TestListResolver( included, excluded );
        assertFalse( resolver.shouldRun( "jiras/surefire745/BasicTest.class", null ) );
        assertFalse( resolver.shouldRun( "jiras/surefire745/TestTwo.class", null ) );
        assertFalse( resolver.shouldRun( "jiras/surefire745/TestThree.class", null ) );
        assertTrue( resolver.shouldRun( "jiras/surefire745/TestFour.class", null ) );
    }

    public void testMultipleExcludedClasses()
    {
        Collection<String> included = singleton( DEFAULT_SUREFIRE_INCLUDED_TEST_PATTERNS );
        Collection<String> excluded = asList( "BasicTest, **/TestTwo, **/TestThree.java" );
        TestListResolver resolver = new TestListResolver( included, excluded );
        assertFalse( resolver.shouldRun( "jiras/surefire745/BasicTest.class", null ) );
        assertFalse( resolver.shouldRun( "jiras/surefire745/TestTwo.class", null ) );
        assertFalse( resolver.shouldRun( "jiras/surefire745/TestThree.class", null ) );
        assertTrue( resolver.shouldRun( "jiras/surefire745/TestFour.class", null ) );
    }

    public void testAndFilters()
    {
        TestListResolver firstFilter = new TestListResolver( "BasicTest, **/TestTwo, **/TestThree.java" );
        TestListResolver secondFilter = new TestListResolver( "*icTest, Test???*" );
        TestFilter<String, String> filter = firstFilter.and( secondFilter );

        assertTrue( filter.shouldRun( "jiras/surefire745/BasicTest.class", null ) );
        assertTrue( filter.shouldRun( "jiras/surefire745/TestTwo.class", null ) );
        assertTrue( filter.shouldRun( "jiras/surefire745/TestThree.class", null ) );
        assertFalse( filter.shouldRun( "jiras/surefire745/TestFour.class", null ) );
    }

    public void testTestListResolverWithoutMethods()
    {
        ResolvedTest inc1 = new ResolvedTest( "A?Test.java", null, false );
        ResolvedTest inc2 = new ResolvedTest( "**/?Test", null, false );
        ResolvedTest exc1 = new ResolvedTest( "AATest", null, false );
        ResolvedTest exc2 = new ResolvedTest( "**/BTest.java", null, false );
        TestListResolver resolver = newTestListResolver( $( inc1, inc2 ), $( exc1, exc2 ) );
        assertThat( resolver.getPluginParameterTest(), is( "A?Test.java, **/?Test, !AATest, !**/BTest.java" ) );
        assertFalse( resolver.isEmpty() );
        assertFalse( resolver.hasIncludedMethodPatterns() );
        assertFalse( resolver.hasExcludedMethodPatterns() );
        assertFalse( resolver.hasMethodPatterns() );
        assertTrue( resolver.shouldRun( "ATest.class", null ) );
        assertFalse( resolver.shouldRun( "AATest.class", null ) );
        assertTrue( resolver.shouldRun( "ABTest.class", null ) );
        assertFalse( resolver.shouldRun( "BTest.class", null ) );
        assertTrue( resolver.shouldRun( "CTest.class", null ) );
        assertFalse( resolver.hasMethodPatterns() );
    }

    public void testTestListResolverWithMethods()
    {
        ResolvedTest inc1 = new ResolvedTest( "A?Test.java", null, false );
        ResolvedTest inc2 = new ResolvedTest( "*?Test", null, false );
        ResolvedTest exc1 = new ResolvedTest( "AATest", null, false );
        ResolvedTest exc2 = new ResolvedTest( "*BTest.java", "failedTest", false );
        TestListResolver resolver = newTestListResolver( $( inc1, inc2 ), $( exc1, exc2 ) );
        assertThat( resolver.getPluginParameterTest(), is( "A?Test.java, *?Test, !AATest, !*BTest.java#failedTest" ) );
        assertFalse( resolver.isEmpty() );
        assertFalse( resolver.hasIncludedMethodPatterns() );
        assertTrue( resolver.hasExcludedMethodPatterns() );
        assertTrue( resolver.hasMethodPatterns() );
        assertTrue( resolver.shouldRun( "ATest.class", null ) );
        assertFalse( resolver.shouldRun( "AATest.class", null ) );
        assertTrue( resolver.shouldRun( "ABTest.class", null ) );
        assertTrue( resolver.shouldRun( "BTest.class", null ) );
        assertFalse( resolver.shouldRun( "BTest.class", "failedTest" ) );
        assertTrue( resolver.shouldRun( "CTest.class", null ) );
        assertFalse( TestListResolver.optionallyWildcardFilter( resolver ).isEmpty() );
    }

    private static Set<ResolvedTest> $( ResolvedTest... patterns )
    {
        Set<ResolvedTest> set = new LinkedHashSet<>();
        addAll( set, patterns );
        return set;
    }

    public void testDefaultPatternsMatching()
    {
        Set<ResolvedTest> inclusions = resolveClass( DEFAULT_SUREFIRE_INCLUDED_TEST_PATTERNS );
        Set<ResolvedTest> exclusions = resolveClass( DEFAULT_SUREFIRE_EXCLUDED_TEST_PATTERNS );
        TestListResolver tlr = newTestListResolver( inclusions, exclusions );
        boolean shouldRun = tlr.shouldRun( "org/apache/maven/surefire/SomeTest.class", null );
        assertTrue( shouldRun );
    }

    public void testDefaultPatternsNotMatching()
    {
        Set<ResolvedTest> inclusions = resolveClass( DEFAULT_SUREFIRE_INCLUDED_TEST_PATTERNS );
        Set<ResolvedTest> exclusions = resolveClass( DEFAULT_SUREFIRE_EXCLUDED_TEST_PATTERNS );
        TestListResolver tlr = newTestListResolver( inclusions, exclusions );
        boolean shouldRun = tlr.shouldRun( "org/apache/maven/surefire/SomeTestNotRunning.class", null );
        assertFalse( shouldRun );
    }

    public void testInclusiveWithDefaultExclusivePattern()
    {
        Set<ResolvedTest> defaultExclusions = resolveClass( DEFAULT_SUREFIRE_EXCLUDED_TEST_PATTERNS );
        boolean runnable = newTestListResolver( resolveClass( "A*Test" ), defaultExclusions )
            .shouldRun( "org/apache/maven/surefire/ARunnableTest.class", null );
        assertTrue( runnable );
    }

    public void testWildcard()
    {
        TestListResolver tlr = TestListResolver.optionallyWildcardFilter( new TestListResolver( (String) null ) );
        assertThat( tlr, is( new TestListResolver( "**/*.class" ) ) );
        assertThat( tlr.isWildcard(), is( true ) );
        assertThat( tlr.isEmpty(), is( false ) );

        tlr = TestListResolver.optionallyWildcardFilter( new TestListResolver( "**/**/MethodLessPattern.class" ) );
        assertThat( tlr, is( new TestListResolver( "**/*.class" ) ) );
        assertThat( tlr.isWildcard(), is( true ) );
        assertThat( tlr.isEmpty(), is( false ) );
    }

    public void testRegexRuleViolationQuotedHashMark()
    {
        try
        {
            new TestListResolver( "%regex[.\\Q#\\E.]" );
            fail( "IllegalArgumentException is expected" );
        }
        catch ( IllegalArgumentException iea )
        {
            // expected
        }
    }

    public void testRegexRuleViolationEnclosedMethodSeparator()
    {
        try
        {
            new TestListResolver( "%regex[(.|.#.)]" );
            fail( "IllegalArgumentException is expected" );
        }
        catch ( IllegalArgumentException iea )
        {
            // expected
        }
    }

    public void testRegexRuleViolationMultipleHashmarkWithClassConstraint()
    {
        try
        {
            new TestListResolver( "%regex[.*#.|#.]" );
            fail( "IllegalArgumentException is expected" );
        }
        catch ( IllegalArgumentException iea )
        {
            // expected
        }
    }

    public void testRegexRuleViolationMultipleHashmarkForMethods()
    {
        try
        {
            new TestListResolver( "%regex[#.|#.]" );
            fail( "IllegalArgumentException is expected" );
        }
        catch ( IllegalArgumentException iea )
        {
            // expected
        }
    }

    public void testRegexRuleViolationInvalidClassPattern()
    {
        try
        {
            new TestListResolver( "%regex[.(.]" )
                    .shouldRun( "x", "x" );
            fail( "IllegalArgumentException is expected" );
        }
        catch ( IllegalArgumentException iea )
        {
            // expected
        }
    }

    public void testRegexRuleViolationInvalidMethodPattern()
    {
        try
        {
            new TestListResolver( "%regex[#.(.]" );
            fail( "IllegalArgumentException is expected" );
        }
        catch ( IllegalArgumentException iea )
        {
            // expected
        }
    }

    private static Set<ResolvedTest> resolveClass( String patterns )
    {
        Set<ResolvedTest> resolved = new HashSet<>();
        for ( String pattern : patterns.split( "," ) )
        {
            resolved.add( new ResolvedTest( CLASS, pattern, false ) );
        }
        return resolved;
    }
}
