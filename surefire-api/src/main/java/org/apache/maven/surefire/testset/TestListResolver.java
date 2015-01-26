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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resolved multi pattern filter e.g. -Dtest=MyTest#test,!AnotherTest#otherTest into an object model
 * composed of included and excluded tests.<br/>
 * The methods {@link #shouldRun(Class, String)} are filters easily used in JUnit filter or TestNG.
 * This class is independent of JUnit and TestNG API.<br/>
 * This class is accessed by Java Reflection API in {@link org.apache.maven.surefire.booter.SurefireReflector}
 * using specific ClassLoader.
 */
public class TestListResolver
{
    private static final String JAVA_CLASS_FILE_EXTENSION = ".class";

    private final String parameterTest;

    private final Set<ResolvedTest> includedFilters;

    private final Set<ResolvedTest> excludedFilters;

    private final Set<String> specificClasses;

    public TestListResolver( String parameterTest )
    {
        Set<ResolvedTest> includedFilters = new LinkedHashSet<ResolvedTest>( 0 );
        Set<ResolvedTest> excludedFilters = new LinkedHashSet<ResolvedTest>( 0 );
        Set<String> specificClasses = new LinkedHashSet<String>( 0 );
        if ( StringUtils.isNotBlank( parameterTest ) )
        {
            final Map<Class<?>, String> classConversion = new IdentityHashMap<Class<?>, String>( 512 );
            for ( String request : StringUtils.split( parameterTest, "," ) )
            {
                request = request.trim();
                if ( request.length() != 0 && !request.equals( "!" ) )
                {
                    final int indexOfMethodSeparator = request.indexOf( '#' );
                    if ( indexOfMethodSeparator == -1 )
                    {
                        String classPattern = removeExclamationMark( request );
                        boolean isExcluded = classPattern.length() < request.length();
                        ResolvedTest test = new ResolvedTest( classPattern, null, classConversion );
                        if ( !test.isEmpty() )
                        {
                            updatedFilters( isExcluded, test, includedFilters, excludedFilters );
                        }
                    }
                    else
                    {
                        String className = request.substring( 0, indexOfMethodSeparator );
                        String methodNames = request.substring( 1 + indexOfMethodSeparator );
                        for ( String methodName : StringUtils.split( methodNames, "+" ) )
                        {
                            String classPattern = removeExclamationMark( className );
                            boolean isExcluded = classPattern.length() < className.length();
                            ResolvedTest test = new ResolvedTest( classPattern, methodName, classConversion );
                            if ( !test.isEmpty() )
                            {
                                updatedFilters( isExcluded, test, includedFilters, excludedFilters );
                            }
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
        }
        this.parameterTest = StringUtils.isBlank( parameterTest ) ? null : parameterTest;
        this.specificClasses = Collections.unmodifiableSet( specificClasses );
        this.includedFilters = Collections.unmodifiableSet( includedFilters );
        this.excludedFilters = Collections.unmodifiableSet( excludedFilters );
    }

    private TestListResolver( String parameterTest, Set<ResolvedTest> includedFilters,
                              Set<ResolvedTest> excludedFilters )
    {
        this.includedFilters = includedFilters;
        this.excludedFilters = excludedFilters;
        this.specificClasses = null;
        this.parameterTest = parameterTest;
    }

    public TestListResolver onlyMethodFilters()
    {
        return new TestListResolver( getPluginParameterTest(), selectMethodFilters( getIncludedFilters() ),
                                     selectMethodFilters( getExcludedFilters() ) );
    }

    public boolean shouldRun( Class<?> testClass, String methodName )
    {
        if ( isEmpty() || testClass == null && methodName == null )
        {
            return true;
        }
        else
        {
            boolean shouldRun = false;

            if ( includedFilters.isEmpty() )
            {
                shouldRun = true;
            }
            else
            {
                for ( ResolvedTest filter : includedFilters )
                {
                    if ( filter.shouldRun( testClass, methodName ) )
                    {
                        shouldRun = true;
                        break;
                    }
                }
            }

            if ( shouldRun )
            {
                for ( ResolvedTest filter : excludedFilters )
                {
                    if ( filter.shouldRun( testClass, methodName ) )
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
        return getIncludedFilters().isEmpty() && getIncludedFilters().isEmpty();
    }

    public String getPluginParameterTest()
    {
        return parameterTest;
    }

    public Set<ResolvedTest> getIncludedFilters()
    {
        return includedFilters;
    }

    public Set<ResolvedTest> getExcludedFilters()
    {
        return excludedFilters;
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

        return getIncludedFilters().equals( that.getIncludedFilters() )
            && getExcludedFilters().equals( that.getExcludedFilters() );

    }

    @Override
    public int hashCode()
    {
        int result = getIncludedFilters().hashCode();
        result = 31 * result + getExcludedFilters().hashCode();
        return result;
    }

    private static String removeExclamationMark( String s )
    {
        return s.length() != 0 && s.charAt( 0 ) == '!' ? s.substring( 1 ) : s;
    }

    private static void updatedFilters( boolean isExcluded, ResolvedTest test, Collection<ResolvedTest> includedFilters,
                                        Collection<ResolvedTest> excludedFilters )
    {
        if ( isExcluded )
        {
            excludedFilters.add( test );
        }
        else
        {
            includedFilters.add( test );
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

    private static Set<ResolvedTest> selectMethodFilters( Collection<ResolvedTest> filters )
    {
        Set<ResolvedTest> selectedFilters = new HashSet<ResolvedTest>( filters.size() );
        for ( ResolvedTest filter : filters )
        {
            if ( filter.getTestMethodPattern() != null )
            {
                selectedFilters.add( filter );
            }
        }
        return selectedFilters;
    }
}
