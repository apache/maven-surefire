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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static org.apache.maven.shared.utils.StringUtils.isBlank;
import static org.apache.maven.shared.utils.StringUtils.isNotBlank;
import static org.apache.maven.shared.utils.StringUtils.split;
import static org.apache.maven.shared.utils.io.SelectorUtils.PATTERN_HANDLER_SUFFIX;
import static org.apache.maven.shared.utils.io.SelectorUtils.REGEX_HANDLER_PREFIX;
import static java.util.Collections.singleton;
import static org.apache.maven.surefire.testset.ResolvedTest.Type.CLASS;
import static org.apache.maven.surefire.testset.ResolvedTest.Type.METHOD;

// TODO In Surefire 3.0 see SUREFIRE-1309 and use normal fully qualified class name regex instead.
/**
 * Resolved multi pattern filter e.g. -Dtest=MyTest#test,!AnotherTest#otherTest into an object model
 * composed of included and excluded tests.<br>
 * The methods {@link #shouldRun(String, String)} are filters easily used in JUnit filter or TestNG.
 * This class is independent of JUnit and TestNG API.<br>
 * It is accessed by Java Reflection API in {@link org.apache.maven.surefire.booter.SurefireReflector}
 * using specific ClassLoader.
 */
public class TestListResolver
    implements GenericTestPattern<ResolvedTest, String, String>
{
    private static final String JAVA_CLASS_FILE_EXTENSION = ".class";

    private static final TestListResolver WILDCARD = new TestListResolver( "*" + JAVA_CLASS_FILE_EXTENSION );

    private static final TestListResolver EMPTY = new TestListResolver( "" );

    private final Set<ResolvedTest> includedPatterns;

    private final Set<ResolvedTest> excludedPatterns;

    private final boolean hasIncludedMethodPatterns;

    private final boolean hasExcludedMethodPatterns;

    public TestListResolver( Collection<String> tests )
    {
        final IncludedExcludedPatterns patterns = new IncludedExcludedPatterns();
        final Set<ResolvedTest> includedFilters = new LinkedHashSet<>( 0 );
        final Set<ResolvedTest> excludedFilters = new LinkedHashSet<>( 0 );

        for ( final String csvTests : tests )
        {
            if ( isNotBlank( csvTests ) )
            {
                for ( String request : split( csvTests, "," ) )
                {
                    request = request.trim();
                    if ( !request.isEmpty() && !request.equals( "!" ) )
                    {
                        resolveTestRequest( request, patterns, includedFilters, excludedFilters );
                    }
                }
            }
        }

        this.includedPatterns = unmodifiableSet( includedFilters );
        this.excludedPatterns = unmodifiableSet( excludedFilters );
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
                              Set<ResolvedTest> includedPatterns, Set<ResolvedTest> excludedPatterns )
    {
        this.includedPatterns = includedPatterns;
        this.excludedPatterns = excludedPatterns;
        this.hasIncludedMethodPatterns = hasIncludedMethodPatterns;
        this.hasExcludedMethodPatterns = hasExcludedMethodPatterns;
    }

    public static TestListResolver newTestListResolver( Set<ResolvedTest> includedPatterns,
                                                        Set<ResolvedTest> excludedPatterns )
    {
        return new TestListResolver( haveMethodPatterns( includedPatterns ), haveMethodPatterns( excludedPatterns ),
                                     includedPatterns, excludedPatterns );
    }

    @Override
    public boolean hasIncludedMethodPatterns()
    {
        return hasIncludedMethodPatterns;
    }

    @Override
    public boolean hasExcludedMethodPatterns()
    {
        return hasExcludedMethodPatterns;
    }

    @Override
    public boolean hasMethodPatterns()
    {
        return hasIncludedMethodPatterns() || hasExcludedMethodPatterns();
    }

    /**
     *
     * @param resolver    filter possibly having method patterns
     * @return {@code resolver} if {@link TestListResolver#hasMethodPatterns() resolver.hasMethodPatterns()}
     * returns {@code true}; Otherwise wildcard filter {@code *.class} is returned.
     */
    public static TestListResolver optionallyWildcardFilter( TestListResolver resolver )
    {
        return resolver.hasMethodPatterns() ? resolver : WILDCARD;
    }

    public static TestListResolver getWildcard()
    {
        return WILDCARD;
    }

    public static TestListResolver getEmptyTestListResolver()
    {
        return EMPTY;
    }

    public final boolean isWildcard()
    {
        return equals( WILDCARD );
    }

    public TestFilter<String, String> and( final TestListResolver another )
    {
        return new TestFilter<String, String>()
        {
            @Override
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
            @Override
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

    /**
     * Returns {@code true} if satisfies {@code testClassFile} and {@code methodName} filter.
     *
     * @param testClassFile format must be e.g. "my/package/MyTest.class" including class extension; or null
     * @param methodName real test-method name; or null
     */
    @Override
    public boolean shouldRun( String testClassFile, String methodName )
    {
        if ( isEmpty() || isBlank( testClassFile ) && isBlank( methodName ) )
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
                    if ( filter.matchAsInclusive( testClassFile, methodName ) )
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
                    if ( filter.matchAsExclusive( testClassFile, methodName ) )
                    {
                        shouldRun = false;
                        break;
                    }
                }
            }
            return shouldRun;
        }
    }

    @Override
    public boolean isEmpty()
    {
        return equals( EMPTY );
    }

    @Override
    public String getPluginParameterTest()
    {
        String aggregatedTest = aggregatedTest( "", getIncludedPatterns() );

        if ( isNotBlank( aggregatedTest ) && !getExcludedPatterns().isEmpty() )
        {
            aggregatedTest += ", ";
        }

        aggregatedTest += aggregatedTest( "!", getExcludedPatterns() );
        return aggregatedTest.length() == 0 ? "" : aggregatedTest;
    }

    @Override
    public Set<ResolvedTest> getIncludedPatterns()
    {
        return includedPatterns;
    }

    @Override
    public Set<ResolvedTest> getExcludedPatterns()
    {
        return excludedPatterns;
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
        return !s.isEmpty() && s.charAt( 0 ) == '!' ? s.substring( 1 ) : s;
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

    private static String aggregatedTest( String testPrefix, Set<ResolvedTest> tests )
    {
        StringBuilder aggregatedTest = new StringBuilder();
        for ( ResolvedTest test : tests )
        {
            String readableTest = test.toString();
            if ( !readableTest.isEmpty() )
            {
                if ( aggregatedTest.length() != 0 )
                {
                    aggregatedTest.append( ", " );
                }
                aggregatedTest.append( testPrefix )
                        .append( readableTest );
            }
        }
        return aggregatedTest.toString();
    }

    private static Collection<String> mergeIncludedAndExcludedTests( Collection<String> included,
                                                                     Collection<String> excluded )
    {
        ArrayList<String> incExc = new ArrayList<>( included );
        incExc.removeAll( Collections.<String>singleton( null ) );
        for ( String exc : excluded )
        {
            if ( exc != null )
            {
                exc = exc.trim();
                if ( !exc.isEmpty() )
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
        int indexOfRegex = pattern.indexOf( REGEX_HANDLER_PREFIX );
        int prefixLength = REGEX_HANDLER_PREFIX.length();
        if ( indexOfRegex != -1 )
        {
            if ( indexOfRegex != 0
                         || !pattern.endsWith( PATTERN_HANDLER_SUFFIX )
                         || !isRegexMinLength( pattern )
                         || pattern.indexOf( REGEX_HANDLER_PREFIX, prefixLength ) != -1 )
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


    static boolean isRegexMinLength( String pattern )
    {
        //todo bug in maven-shared-utils: '+1' should not appear in the condition
        //todo cannot reuse code from SelectorUtils.java because method isRegexPrefixedPattern is in private package.
        return pattern.length() > REGEX_HANDLER_PREFIX.length() + PATTERN_HANDLER_SUFFIX.length() + 1;
    }

    static String[] unwrapRegex( String regex )
    {
        regex = regex.trim();
        int from = REGEX_HANDLER_PREFIX.length();
        int to = regex.length() - PATTERN_HANDLER_SUFFIX.length();
        return unwrap( regex.substring( from, to ) );
    }

    static String[] unwrap( final String request )
    {
        final String[] classAndMethod = { "", "" };
        final int indexOfHash = request.indexOf( '#' );
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
        for ( String method : split( methods, "+" ) )
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
     * Requires trimmed {@code request} been not equal to "!".
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
            final boolean hasClass = !unwrapped[0].isEmpty();
            final boolean hasMethod = !unwrapped[1].isEmpty();
            if ( hasClass && hasMethod )
            {
                test = new ResolvedTest( unwrapped[0], unwrapped[1], true );
            }
            else if ( hasClass )
            {
                test = new ResolvedTest( CLASS, unwrapped[0], true );
            }
            else if ( hasMethod )
            {
                test = new ResolvedTest( METHOD, unwrapped[1], true );
            }
        }
        else
        {
            final int indexOfMethodSeparator = request.indexOf( '#' );
            if ( indexOfMethodSeparator == -1 )
            {
                test = new ResolvedTest( CLASS, request, false );
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

    private static boolean haveMethodPatterns( Set<ResolvedTest> patterns )
    {
        for ( ResolvedTest pattern : patterns )
        {
            if ( pattern.hasTestMethodPattern() )
            {
                return true;
            }
        }
        return false;
    }
}
