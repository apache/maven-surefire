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

import org.apache.maven.shared.utils.io.SelectorUtils;

import java.util.Map;

/**
 * Single pattern test filter resolved from multi pattern filter -Dtest=MyTest#test,AnotherTest#otherTest.
 */
public final class ResolvedTest
{
    private static final String CLASS_FILE_EXTENSION = ".class";

    private static final String JAVA_FILE_EXTENSION = ".java";

    private final String classPattern;

    private final String methodPattern;

    private final Map<Class<?>, String> classConversion;

    private final boolean isRegexTestClassPattern;

    private final boolean isRegexTestMethodPattern;

    /**
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     * The %ant[] expression is substituted by %regex[].
     *
     * @param classPattern     test class file pattern
     * @param methodPattern    test method
     * @throws IllegalArgumentException if regex not finished with ']'
     */
    ResolvedTest( String classPattern, String methodPattern, Map<Class<?>, String> classConversion )
    {
        this.classPattern = reformatPattern( classPattern, true );
        this.methodPattern = reformatPattern( methodPattern, false );
        this.classConversion = classConversion;
        isRegexTestClassPattern =
            this.classPattern != null && this.classPattern.startsWith( SelectorUtils.REGEX_HANDLER_PREFIX );
        isRegexTestMethodPattern =
            this.methodPattern != null && this.methodPattern.startsWith( SelectorUtils.REGEX_HANDLER_PREFIX );
    }

    /**
     * Test class file pattern, e.g. org&#47;**&#47;Cat*.class<br/>
     * Other examples: org&#47;animals&#47;Cat*, org&#47;animals&#47;Ca?.class, %regex[Cat.class|Dog.*]<br/>
     * <br/>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     */
    public String getTestClassPattern()
    {
        return classPattern;
    }

    /**
     * Test method, e.g. "realTestMethod".<br/>
     * Other examples: test* or testSomethin? or %regex[testOne|testTwo] or %ant[testOne|testTwo]<br/>
     * <br/>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     */
    public String getTestMethodPattern()
    {
        return methodPattern;
    }

    public boolean isRegexTestClassPattern()
    {
        return isRegexTestClassPattern;
    }

    public boolean isRegexTestMethodPattern()
    {
        return isRegexTestMethodPattern;
    }

    public boolean isEmpty()
    {
        return classPattern == null && methodPattern == null;
    }

    public boolean shouldRun( Class<?> realTestClass, String methodName )
    {
        if ( methodPattern != null )
        {
            if ( methodName != null && !SelectorUtils.matchPath( methodPattern, methodName ) )
            {
                return false;
            }
        }

        if ( classPattern == null )
        {
            return methodName != null;
        }
        else
        {
            String classFile = classFile( realTestClass );
            if ( isRegexTestClassPattern() )
            {
                String pattern = classPattern.indexOf( '$' ) == -1 ? classPattern : classPattern.replace( "$", "\\$" );
                return SelectorUtils.matchPath( pattern, classFile );
            }
            else
            {
                if ( SelectorUtils.matchPath( classPattern, classFile ) )
                {
                    // match class pattern with package
                    return true;
                }
                else
                {
                    int indexOfSimpleName = classFile.lastIndexOf( '/' );
                    return indexOfSimpleName != -1
                        && SelectorUtils.matchPath( classPattern, classFile.substring( 1 + indexOfSimpleName ) );
                }
            }
        }
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

        ResolvedTest that = (ResolvedTest) o;

        return ( classPattern == null ? that.classPattern == null : classPattern.equals( that.classPattern ) )
            && ( methodPattern == null ? that.methodPattern == null : methodPattern.equals( that.methodPattern ) );

    }

    @Override
    public int hashCode()
    {
        int result = classPattern != null ? classPattern.hashCode() : 0;
        result = 31 * result + ( methodPattern != null ? methodPattern.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        return "ResolvedTest{classPattern='" + classPattern + "', methodPattern='" + methodPattern + "'}";
    }

    private String classFile( Class<?> realTestClass )
    {
        String classFile = classConversion.get( realTestClass );
        if ( classFile == null )
        {
            classFile = realTestClass.getName().replace( '.', '/' ) + CLASS_FILE_EXTENSION;
            classConversion.put( realTestClass, classFile );
        }
        return classFile;
    }

    /**
     * {@link Class#getSimpleName()} does not return this format with nested classes FirstClass$NestedTest.
     */
    private static String simpleClassFileName( String classFile )
    {
        int indexOfSimpleName = classFile.lastIndexOf( '/' );
        return indexOfSimpleName == -1 ? classFile : classFile.substring( 1 + indexOfSimpleName );
    }

    private static String tryBlank( String s )
    {
        if ( s == null )
        {
            return null;
        }
        else
        {
            s = s.trim();
            return s.length() == 0 ? null : s;
        }
    }

    private static String reformatPattern( String s, boolean isTestClass )
    {
        s = tryBlank( s );
        if ( s == null )
        {
            return null;
        }
        else if ( s.startsWith( SelectorUtils.REGEX_HANDLER_PREFIX ) )
        {
            if ( !s.endsWith( SelectorUtils.PATTERN_HANDLER_SUFFIX ) )
            {
                throw new IllegalArgumentException( s + " enclosed regex does not finish with ']'" );
            }
        }
        else if ( s.startsWith( SelectorUtils.ANT_HANDLER_PREFIX ) )
        {
            if ( s.endsWith( SelectorUtils.PATTERN_HANDLER_SUFFIX ) )
            {
                s = SelectorUtils.REGEX_HANDLER_PREFIX + s.substring( SelectorUtils.ANT_HANDLER_PREFIX.length() );
            }
            else
            {
                throw new IllegalArgumentException( s + " enclosed regex does not finish with ']'" );
            }
        }
        else if ( isTestClass )
        {
            s = convertToPath( s );
        }
        return s;
    }

    private static String convertToPath( String className )
    {
        if ( className == null || className.trim().length() == 0 )
        {
            return className;
        }
        else
        {
            if ( className.endsWith( JAVA_FILE_EXTENSION ) )
            {
                className = className.substring( 0, className.length() - JAVA_FILE_EXTENSION.length() );
            }
            else if ( className.endsWith( CLASS_FILE_EXTENSION ) )
            {
                className = className.substring( 0, className.length() - CLASS_FILE_EXTENSION.length() );
            }
            className = className.replace( '.', '/' );
            return className + CLASS_FILE_EXTENSION;
        }
    }
}
