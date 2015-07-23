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

import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.shared.utils.io.SelectorUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Collections.emptySet;

/**
 * Resolved multi pattern filter e.g. -Dtest=MyTest#test,!AnotherTest#otherTest into an object model
 * composed of included and excluded tests.<br/>
 * The methods {@link #shouldRun(String, String)} are filters easily used in JUnit filter or TestNG.
 * This class is independent of JUnit and TestNG API.<br/>
 * It is accessed by Java Reflection API in {@link org.apache.maven.surefire.booter.SurefireReflector}
 * using specific ClassLoader.
 */
public class TestListResolver
    implements GenericTestPattern<TestListResolver, ResolvedTest, String, String>
{
    private static final String JAVA_CLASS_FILE_EXTENSION = ".class";

    private static final Set<ResolvedTest> EMPTY_TEST_PATTERNS = emptySet();

    private static final Set<String> EMPTY_SPECIFIC_TESTS = emptySet();

    private final Set<ResolvedTest> includedPatterns;

    private final Set<ResolvedTest> excludedPatterns;

    private final Set<String> specificClasses;

    private final boolean hasIncludedMethodPatterns;

    private final boolean hasExcludedMethodPatterns;

    public TestListResolver( Collection<String> tests )
    {
        final IncludedExcludedPatterns patterns = new IncludedExcludedPatterns();
        final Set<ResolvedTest> includedFilters = new LinkedHashSet<ResolvedTest>( 0 );
        final Set<ResolvedTest> excludedFilters = new LinkedHashSet<ResolvedTest>( 0 );
        final Set<String> specificClasses = new LinkedHashSet<String>( 0 );

        for ( final String csvTests : tests )
        {
            if ( StringUtils.isNotBlank( csvTests ) )
            {
                for ( String request : StringUtils.split( csvTests, "," ) )
                {
                    request = request.trim();
                    if ( request.length() != 0 && !request.equals( "!" ) )
                    {
                        resolveTestRequest( request, patterns, includedFilters, excludedFilters );
                    }
                }
            }
        }

        for ( ResolvedTest test : includedFilters )
        {
            populateSpecificClasses( specificClasses, test );
        }

        for ( ResolvedTest test : excludedFilters )
        {
            populateSpecificClasses( specificClasses, test );
        }

        this.specificClasses = Collections.unmodifiableSet( specificClasses );
        this.includedPatterns = Collections.unmodifiableSet( includedFilters );
        this.excludedPatterns = Collections.unmodifiableSet( excludedFilters );
        this.hasIncludedMethodPatterns = patterns.hasIncludedMethodPatterns;
        this.hasExcludedMethodPatterns = patterns.hasExcludedMethodPatterns;
    }

    public TestListResolver( String csvTests )
    {
        this( csvTests == null ? Collections.<String>emptySet() : singleton( csvTests ) );
    }

    public TestListResolver( Collection<String> included, Collection<String> excluded )
    {
        this( mergeIncludedAndExcludedTests( included, excluded ) );
    }

    /**
     * Used only in method filter.
     */
    private TestListResolver( boolean hasIncludedMethodPatterns, boolean hasExcludedMethodPatterns,
                              Set<String> specificClasses, Set<ResolvedTest> includedPatterns,
                              Set<ResolvedTest> excludedPatterns )
    {
        this.includedPatterns = includedPatterns;
        this.excludedPatterns = excludedPatterns;
        this.specificClasses = specificClasses;
        this.hasIncludedMethodPatterns = hasIncludedMethodPatterns;
        this.hasExcludedMethodPatterns = hasExcludedMethodPatterns;
    }

    public boolean hasIncludedMethodPatterns()
    {
        return hasIncludedMethodPatterns;
    }

    public boolean hasExcludedMethodPatterns()
    {
        return hasExcludedMethodPatterns;
    }

    public boolean hasMethodPatterns()
    {
        return hasIncludedMethodPatterns() || hasExcludedMethodPatterns();
    }

    /**
     * Method filter.
     */
    public TestListResolver createMethodFilters()
    {
        boolean hasMethodPatterns = hasMethodPatterns();
        Set<ResolvedTest> inc = hasMethodPatterns ? getIncludedPatterns() : EMPTY_TEST_PATTERNS;
        Set<ResolvedTest> exc = hasMethodPatterns ? getExcludedPatterns() : EMPTY_TEST_PATTERNS;
        Set<String> specificClasses = hasMethodPatterns ? getTestSpecificClasses() : EMPTY_SPECIFIC_TESTS;
        return new TestListResolver( hasIncludedMethodPatterns(), hasExcludedMethodPatterns(), specificClasses,
                                     inc, exc );
    }

    public TestListResolver createClassFilters()
    {
        return hasMethodPatterns() ? new TestListResolver( "" ) : this;
    }

    public TestFilter<String, String> and( final TestListResolver another )
    {
        return new TestFilter<String, String>()
        {
            public boolean shouldRun( String testClass, String methodName )
            {
                return TestListResolver.this.shouldRun( testClass, methodName )
                    && another.shouldRun( testClass, methodName );
            }
        };
    }

    public TestFilter<String, String> or( final TestListResolver another )
    {
        return new TestFilter<String, String>()
        {
            public boolean shouldRun( String testClass, String methodName )
            {
                return TestListResolver.this.shouldRun( testClass, methodName )
                    || another.shouldRun( testClass, methodName );
            }
        };
    }

    public boolean shouldRun( Class<?> testClass, String methodName )
    {
        return shouldRun( toClassFileName( testClass ), methodName );
    }

    public boolean shouldRun( String testClassFile, String methodName )
    {
        if ( isEmpty() || StringUtils.isBlank( testClassFile ) && StringUtils.isBlank( methodName ) )
        {
            return true;
        }
        else
        {
            boolean shouldRun = false;

            if ( getIncludedPatterns().isEmpty() )
            {
                shouldRun = true;
            }
            else
            {
                for ( ResolvedTest filter : getIncludedPatterns() )
                {
                    if ( filter.shouldRun( testClassFile, methodName ) )
                    {
                        shouldRun = true;
                        break;
                    }
                }
            }

            if ( shouldRun )
            {
                for ( ResolvedTest filter : getExcludedPatterns() )
                {
                    if ( filter.shouldRun( testClassFile, methodName ) )
                    {
                        shouldRun = false;
                        break;
                    }
                }
            }
            return shouldRun;
        }
    }

    public boolean isEmpty()
    {
        return getIncludedPatterns().isEmpty() && getExcludedPatterns().isEmpty();
    }

    public String getPluginParameterTest()
    {
        String aggregatedTest = aggregatedTest( "", getIncludedPatterns() );
        aggregatedTest += aggregatedTest( "!", getExcludedPatterns() );
        return aggregatedTest.length() == 0 ? null : aggregatedTest;
    }

    public Set<ResolvedTest> getIncludedPatterns()
    {
        return includedPatterns;
    }

    public Set<ResolvedTest> getExcludedPatterns()
    {
        return excludedPatterns;
    }

    public Set<String> getTestSpecificClasses()
    {
        return specificClasses;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        TestListResolver that = (TestListResolver) o;

        return getIncludedPatterns().equals( that.getIncludedPatterns() )
            && getExcludedPatterns().equals( that.getExcludedPatterns() );

    }

    @Override
    public int hashCode()
    {
        int result = getIncludedPatterns().hashCode();
        result = 31 * result + getExcludedPatterns().hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        return getPluginParameterTest();
    }

    public static String toClassFileName( Class<?> test )
    {
        return test == null ? null : toClassFileName( test.getName() );
    }

    public static String toClassFileName( String fullyQualifiedTestClass )
    {
        return fullyQualifiedTestClass == null
            ? null
            : fullyQualifiedTestClass.replace( '.', '/' ) + JAVA_CLASS_FILE_EXTENSION;
    }

    static String removeExclamationMark( String s )
    {
        return s.length() != 0 && s.charAt( 0 ) == '!' ? s.substring( 1 ) : s;
    }

    private static void updatedFilters( boolean isExcluded, ResolvedTest test, IncludedExcludedPatterns patterns,
                                        Collection<ResolvedTest> includedFilters,
                                        Collection<ResolvedTest> excludedFilters )
    {
        if ( isExcluded )
        {
            excludedFilters.add( test );
            patterns.hasExcludedMethodPatterns |= test.hasTestMethodPattern();
        }
        else
        {
            includedFilters.add( test );
            patterns.hasIncludedMethodPatterns |= test.hasTestMethodPattern();
        }
    }

    private static void populateSpecificClasses( Set<String> specificClasses, ResolvedTest test )
    {
        String pattern = test.getTestClassPattern();
        if ( pattern != null )
        {
            if ( !test.isRegexTestClassPattern() && pattern.endsWith( JAVA_CLASS_FILE_EXTENSION ) )
            {
                pattern = pattern.substring( 0, pattern.length() - JAVA_CLASS_FILE_EXTENSION.length() );
            }
            specificClasses.add( pattern );
        }
    }

    private static String aggregatedTest( String testPrefix, Set<ResolvedTest> tests )
    {
        String aggregatedTest = "";
        for ( ResolvedTest test : tests )
        {
            String readableTest = test.toString();
            if ( aggregatedTest.length() != 0 && readableTest != null )
            {
                aggregatedTest += ",";
            }
            aggregatedTest += testPrefix + readableTest;
        }
        return aggregatedTest;
    }

    private static Collection<String> mergeIncludedAndExcludedTests( Collection<String> included,
                                                                     Collection<String> excluded )
    {
        ArrayList<String> incExc = new ArrayList<String>( included );
        incExc.removeAll( Collections.<String>singleton( null ) );
        for ( String exc : excluded )
        {
            if ( exc != null )
            {
                exc = exc.trim();
                if ( exc.length() != 0 )
                {
                    if ( exc.contains( "!" ) )
                    {
                        throw new IllegalArgumentException( "Exclamation mark not expected in 'exclusion': " + exc );
                    }
                    exc = exc.replace( ",", ",!" );
                    if ( !exc.startsWith( "!" ) )
                    {
                        exc = "!" + exc;
                    }
                    incExc.add( exc );
                }
            }
        }
        return incExc;
    }

    static boolean isRegexPrefixedPattern( String pattern )
    {
        int indexOfRegex = pattern.indexOf( SelectorUtils.REGEX_HANDLER_PREFIX );
        int prefixLength = SelectorUtils.REGEX_HANDLER_PREFIX.length();
        if ( indexOfRegex != -1 )
        {
            if ( indexOfRegex != 0
                || !pattern.endsWith( SelectorUtils.PATTERN_HANDLER_SUFFIX )
                || pattern.indexOf( SelectorUtils.REGEX_HANDLER_PREFIX, prefixLength ) != -1 )
            {
                String msg = "Illegal test|includes|excludes regex '%s'. Expected %%regex[class#method] "
                    + "or !%%regex[class#method] " + "with optional class or #method.";
                throw new IllegalArgumentException( String.format( msg, pattern ) );
            }
            return true;
        }
        else
        {
            return false;
        }
    }

    static String[] unwrapRegex( String regex )
    {
        regex = regex.trim();
        int from = SelectorUtils.REGEX_HANDLER_PREFIX.length();
        int to = regex.length() - SelectorUtils.PATTERN_HANDLER_SUFFIX.length();
        return unwrap( regex.substring( from, to ) );
    }

    static String[] unwrap( String request )
    {
        String[] classAndMethod = new String[] { "", "" };
        int indexOfHash = request.indexOf( '#' );
        if ( indexOfHash == -1 )
        {
            classAndMethod[0] = request.trim();
        }
        else
        {
            classAndMethod[0] = request.substring( 0, indexOfHash ).trim();
            classAndMethod[1] = request.substring( 1 + indexOfHash ).trim();
        }
        return classAndMethod;
    }

    static void nonRegexClassAndMethods( String clazz, String methods, boolean isExcluded,
                         IncludedExcludedPatterns patterns,
                         Collection<ResolvedTest> includedFilters, Collection<ResolvedTest> excludedFilters )
    {
        for ( String method : StringUtils.split( methods, "+" ) )
        {
            method = method.trim();
            ResolvedTest test = new ResolvedTest( clazz, method, false );
            if ( !test.isEmpty() )
            {
                updatedFilters( isExcluded, test, patterns, includedFilters, excludedFilters );
            }
        }
    }

    /**
     * Requires trimmed <code>request</code> been not equal to "!".
     */
    static void resolveTestRequest( String request, IncludedExcludedPatterns patterns,
                                    Collection<ResolvedTest> includedFilters, Collection<ResolvedTest> excludedFilters )
    {
        final boolean isExcluded = request.startsWith( "!" );
        ResolvedTest test = null;
        request = removeExclamationMark( request );
        if ( isRegexPrefixedPattern( request ) )
        {
            final String[] unwrapped = unwrapRegex( request );
            final boolean hasClass = unwrapped[0].length() != 0;
            final boolean hasMethod = unwrapped[1].length() != 0;
            if ( hasClass && hasMethod )
            {
                test = new ResolvedTest( unwrapped[0], unwrapped[1], true );
            }
            else if ( hasClass )
            {
                test = new ResolvedTest( ResolvedTest.Type.CLASS, unwrapped[0], true );
            }
            else if ( hasMethod )
            {
                test = new ResolvedTest( ResolvedTest.Type.METHOD, unwrapped[1], true );
            }
        }
        else
        {
            final int indexOfMethodSeparator = request.indexOf( '#' );
            if ( indexOfMethodSeparator == -1 )
            {
                test = new ResolvedTest( ResolvedTest.Type.CLASS, request, false );
            }
            else
            {
                String clazz = request.substring( 0, indexOfMethodSeparator );
                String methods = request.substring( 1 + indexOfMethodSeparator );
                nonRegexClassAndMethods( clazz, methods, isExcluded, patterns, includedFilters, excludedFilters );
            }
        }

        if ( test != null && !test.isEmpty() )
        {
            updatedFilters( isExcluded, test, patterns, includedFilters, excludedFilters );
        }
    }
}
