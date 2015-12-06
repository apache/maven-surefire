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
import org.apache.maven.shared.utils.io.MatchPatterns;
import org.apache.maven.shared.utils.io.SelectorUtils;

import java.io.File;

/**
 * Single pattern test filter resolved from multi pattern filter -Dtest=MyTest#test,AnotherTest#otherTest.
 */
public final class ResolvedTest
{
    /**
     * Type of patterns in ResolvedTest constructor.
     */
    public static enum Type
    {
        CLASS, METHOD
    }

    private static final String CLASS_FILE_EXTENSION = ".class";

    private static final String JAVA_FILE_EXTENSION = ".java";

    private static final String WILDCARD_PATH_PREFIX = "**/";

    private static final String WILDCARD_FILENAME_POSTFIX = ".*";

    private final String classPattern;

    private final String methodPattern;

    private final boolean isRegexTestClassPattern;

    private final boolean isRegexTestMethodPattern;

    private final String description;

    /**
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     * The pattern %regex[] prefix and suffix does not appear. The regex <code>pattern</code> is always
     * unwrapped by the caller.
     *
     * @param classPattern     test class file pattern
     * @param methodPattern    test method
     * @param isRegex          {@code true} if regex
     */
    public ResolvedTest( String classPattern, String methodPattern, boolean isRegex )
    {
        classPattern = tryBlank( classPattern );
        methodPattern = tryBlank( methodPattern );
        description = description( classPattern, methodPattern, isRegex );

        if ( isRegex && classPattern != null )
        {
            classPattern = wrapRegex( classPattern );
        }

        if ( isRegex && methodPattern != null )
        {
            methodPattern = wrapRegex( methodPattern );
        }

        this.classPattern = reformatClassPattern( classPattern, isRegex );
        this.methodPattern = methodPattern;
        isRegexTestClassPattern = isRegex;
        isRegexTestMethodPattern = isRegex;
    }

    /**
     * The regex <code>pattern</code> is always unwrapped.
     */
    public ResolvedTest( Type type, String pattern, boolean isRegex )
    {
        pattern = tryBlank( pattern );
        final boolean isClass = type == Type.CLASS;
        description = description( isClass ? pattern : null, !isClass ? pattern : null, isRegex );
        if ( isRegex && pattern != null )
        {
            pattern = wrapRegex( pattern );
        }
        classPattern = isClass ? reformatClassPattern( pattern, isRegex ) : null;
        methodPattern = !isClass ? pattern : null;
        isRegexTestClassPattern = isRegex && isClass;
        isRegexTestMethodPattern = isRegex && !isClass;
    }

    /**
     * Test class file pattern, e.g. org&#47;**&#47;Cat*.class<br/>, or null if not any
     * and {@link #hasTestClassPattern()} returns false.
     * Other examples: org&#47;animals&#47;Cat*, org&#47;animals&#47;Ca?.class, %regex[Cat.class|Dog.*]<br/>
     * <br/>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     */
    public String getTestClassPattern()
    {
        return classPattern;
    }

    public boolean hasTestClassPattern()
    {
        return classPattern != null;
    }

    /**
     * Test method, e.g. "realTestMethod".<br/>, or null if not any and {@link #hasTestMethodPattern()} returns false.
     * Other examples: test* or testSomethin? or %regex[testOne|testTwo] or %ant[testOne|testTwo]<br/>
     * <br/>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     */
    public String getTestMethodPattern()
    {
        return methodPattern;
    }

    public boolean hasTestMethodPattern()
    {
        return methodPattern != null;
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

    public boolean matchAsInclusive( String testClassFile, String methodName )
    {
        testClassFile = tryBlank( testClassFile );
        methodName = tryBlank( methodName );

        return isEmpty() || alwaysInclusiveQuietly( testClassFile ) || match( testClassFile, methodName );
    }

    public boolean matchAsExclusive( String testClassFile, String methodName )
    {
        testClassFile = tryBlank( testClassFile );
        methodName = tryBlank( methodName );

        return !isEmpty() && canMatchExclusive( testClassFile, methodName ) && match( testClassFile, methodName );
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
        return isEmpty() ? null : description;
    }

    private static String description( String clazz, String method, boolean isRegex )
    {
        String description;
        if ( clazz == null && method == null )
        {
            description = null;
        }
        else if ( clazz == null )
        {
            description = "#" + method;
        }
        else if ( method == null )
        {
            description = clazz;
        }
        else
        {
            description = clazz + "#" + method;
        }
        return isRegex && description != null ? wrapRegex( description ) : description;
    }

    private boolean canMatchExclusive( String testClassFile, String methodName )
    {
        return testClassFile == null && methodName != null && classPattern == null && methodPattern != null
            || testClassFile != null && methodName == null && classPattern != null && methodPattern == null
            || testClassFile != null && methodName != null && ( classPattern != null || methodPattern != null );
    }

    /**
     * Prevents {@link #match(String, String)} from throwing NPE in situations when inclusive returns true.
     */
    private boolean alwaysInclusiveQuietly( String testClassFile )
    {
        return testClassFile == null && classPattern != null;
    }

    private boolean match( String testClassFile, String methodName )
    {
        return ( classPattern == null || matchTestClassFile( testClassFile ) )
            && ( methodPattern == null || methodName == null || matchMethodName( methodName ) );
    }

    private boolean matchTestClassFile( String testClassFile )
    {
        return isRegexTestClassPattern ? matchClassRegexPatter( testClassFile ) : matchClassPatter( testClassFile );
    }

    private boolean matchMethodName( String methodName )
    {
        return SelectorUtils.matchPath( methodPattern, methodName );
    }

    private boolean matchClassPatter( String testClassFile )
    {
        //@todo We have to use File.separator only because the MatchPatterns is using it internally - cannot override.
        String classPattern = this.classPattern;
        if ( File.separatorChar != '/' )
        {
            testClassFile = testClassFile.replace( '/', File.separatorChar );
            classPattern = classPattern.replace( '/', File.separatorChar );
        }

        if ( classPattern.endsWith( WILDCARD_FILENAME_POSTFIX ) || classPattern.endsWith( CLASS_FILE_EXTENSION ) )
        {
            return MatchPatterns.from( classPattern ).matches( testClassFile, true );
        }
        else
        {
            String[] classPatterns = { classPattern + CLASS_FILE_EXTENSION, classPattern };
            return MatchPatterns.from( classPatterns ).matches( testClassFile, true );
        }
    }

    private boolean matchClassRegexPatter( String testClassFile )
    {
        if ( File.separatorChar != '/' )
        {
            testClassFile = testClassFile.replace( '/', File.separatorChar );
        }
        return MatchPatterns.from( classPattern ).matches( testClassFile, true );
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
            return StringUtils.isEmpty( s ) ? null : s;
        }
    }

    private static String reformatClassPattern( String s, boolean isRegex )
    {
        if ( s != null && !isRegex )
        {
            s = convertToPath( s );
            s = fromFullyQualifiedClass( s );
            if ( s != null && !s.startsWith( WILDCARD_PATH_PREFIX ) )
            {
                s = WILDCARD_PATH_PREFIX + s;
            }
        }
        return s;
    }

    private static String convertToPath( String className )
    {
        if ( StringUtils.isBlank( className ) )
        {
            return null;
        }
        else
        {
            if ( className.endsWith( JAVA_FILE_EXTENSION ) )
            {
                className = className.substring( 0, className.length() - JAVA_FILE_EXTENSION.length() );
                className += CLASS_FILE_EXTENSION;
            }
            return className;
        }
    }

    static String wrapRegex( String unwrapped )
    {
        return SelectorUtils.REGEX_HANDLER_PREFIX + unwrapped + SelectorUtils.PATTERN_HANDLER_SUFFIX;
    }

    static String fromFullyQualifiedClass( String cls )
    {
        if ( cls.endsWith( CLASS_FILE_EXTENSION ) )
        {
            cls = cls.substring( 0, cls.length() - CLASS_FILE_EXTENSION.length() );
            return cls.replace( '.', '/' ) + CLASS_FILE_EXTENSION;
        }
        else
        {
            return cls.replace( '.', '/' );
        }
    }
}
