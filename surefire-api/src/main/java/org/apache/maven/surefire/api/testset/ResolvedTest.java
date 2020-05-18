package org.apache.maven.surefire.api.testset;

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

import org.apache.maven.surefire.shared.utils.StringUtils;
import org.apache.maven.surefire.shared.utils.io.MatchPatterns;

import java.util.regex.Pattern;

import static java.io.File.separatorChar;
import static java.util.regex.Pattern.compile;
import static org.apache.maven.surefire.shared.utils.StringUtils.isBlank;
import static org.apache.maven.surefire.shared.utils.io.MatchPatterns.from;
import static org.apache.maven.surefire.shared.utils.io.SelectorUtils.PATTERN_HANDLER_SUFFIX;
import static org.apache.maven.surefire.shared.utils.io.SelectorUtils.REGEX_HANDLER_PREFIX;
import static org.apache.maven.surefire.shared.utils.io.SelectorUtils.matchPath;

/**
 * Single pattern test filter resolved from multi pattern filter -Dtest=MyTest#test,AnotherTest#otherTest.
 * @deprecated will be renamed to ResolvedTestPattern
 */
// will be renamed to ResolvedTestPattern
@Deprecated
public final class ResolvedTest
{
    /**
     * Type of patterns in ResolvedTest constructor.
     */
    public enum Type
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

    private final ClassMatcher classMatcher = new ClassMatcher();

    private final MethodMatcher methodMatcher = new MethodMatcher();

    /**
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     * The pattern %regex[] prefix and suffix does not appear. The regex <i>pattern</i> is always
     * unwrapped by the caller.
     *
     * @param classPattern     test class file pattern
     * @param methodPattern    test method
     * @param isRegex          {@code true} if pattern is regex
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
        methodMatcher.sanityCheck();
    }

    /**
     * The regex {@code pattern} is always unwrapped.
     *
     * @param type class or method
     * @param pattern pattern or regex
     * @param isRegex {@code true} if pattern is regex
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
        methodMatcher.sanityCheck();
    }

    /**
     * Test class file pattern, e.g. org&#47;**&#47;Cat*.class<br>, or null if not any
     * and {@link #hasTestClassPattern()} returns false.
     * Other examples: org&#47;animals&#47;Cat*, org&#47;animals&#47;Ca?.class, %regex[Cat.class|Dog.*]<br>
     * <br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @return class pattern or regex
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
     * Test method, e.g. "realTestMethod".<br>, or null if not any and {@link #hasTestMethodPattern()} returns false.
     * Other examples: test* or testSomethin? or %regex[testOne|testTwo] or %ant[testOne|testTwo]<br>
     * <br>
     * '*' means zero or more characters<br>
     * '?' means one and only one character
     *
     * @return method pattern or regex
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
        return isEmpty() ? "" : description;
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
        return canMatchExclusiveMethods( testClassFile, methodName )
            || canMatchExclusiveClasses( testClassFile, methodName )
            || canMatchExclusiveAll( testClassFile, methodName );
    }

    private boolean canMatchExclusiveMethods( String testClassFile, String methodName )
    {
        return testClassFile == null && methodName != null && classPattern == null && methodPattern != null;
    }

    private boolean canMatchExclusiveClasses( String testClassFile, String methodName )
    {
        return testClassFile != null && methodName == null && classPattern != null && methodPattern == null;
    }

    private boolean canMatchExclusiveAll( String testClassFile, String methodName )
    {
        return testClassFile != null && methodName != null && ( classPattern != null || methodPattern != null );
    }

    /**
     * Prevents {@link #match(String, String)} from throwing NPE in situations when inclusive returns true.
     *
     * @param testClassFile    path to class file
     * @return {@code true} if examined class in null and class pattern exists
     */
    private boolean alwaysInclusiveQuietly( String testClassFile )
    {
        return testClassFile == null && classPattern != null;
    }

    private boolean match( String testClassFile, String methodName )
    {
        return matchClass( testClassFile ) && matchMethod( methodName );
    }

    private boolean matchClass( String testClassFile )
    {
        return classPattern == null || classMatcher.matchTestClassFile( testClassFile );
    }

    private boolean matchMethod( String methodName )
    {
        return methodPattern == null || methodName == null || methodMatcher.matchMethodName( methodName );
    }

    private static String tryBlank( String s )
    {
        if ( s == null )
        {
            return null;
        }
        else
        {
            String trimmed = s.trim();
            return StringUtils.isEmpty( trimmed ) ? null : trimmed;
        }
    }

    private static String reformatClassPattern( String s, boolean isRegex )
    {
        if ( s != null && !isRegex )
        {
            String path = convertToPath( s );
            path = fromFullyQualifiedClass( path );
            if ( path != null && !path.startsWith( WILDCARD_PATH_PREFIX ) )
            {
                path = WILDCARD_PATH_PREFIX + path;
            }
            return path;
        }
        else
        {
            return s;
        }
    }

    private static String convertToPath( String className )
    {
        if ( isBlank( className ) )
        {
            return null;
        }
        else
        {
            if ( className.endsWith( JAVA_FILE_EXTENSION ) )
            {
                className = className.substring( 0, className.length() - JAVA_FILE_EXTENSION.length() )
                                    + CLASS_FILE_EXTENSION;
            }
            return className;
        }
    }

    static String wrapRegex( String unwrapped )
    {
        return REGEX_HANDLER_PREFIX + unwrapped + PATTERN_HANDLER_SUFFIX;
    }

    static String fromFullyQualifiedClass( String cls )
    {
        if ( cls.endsWith( CLASS_FILE_EXTENSION ) )
        {
            String className = cls.substring( 0, cls.length() - CLASS_FILE_EXTENSION.length() );
            return className.replace( '.', '/' ) + CLASS_FILE_EXTENSION;
        }
        else if ( !cls.contains( "/" ) )
        {
            if ( cls.endsWith( WILDCARD_FILENAME_POSTFIX ) )
            {
                String clsName = cls.substring( 0, cls.length() - WILDCARD_FILENAME_POSTFIX.length() );
                return clsName.contains( "." ) ? clsName.replace( '.', '/' ) + WILDCARD_FILENAME_POSTFIX : cls;
            }
            else
            {
                return cls.replace( '.', '/' );
            }
        }
        else
        {
            return cls;
        }
    }

    private final class ClassMatcher
    {
        private volatile MatchPatterns cache;

        boolean matchTestClassFile( String testClassFile )
        {
            return ResolvedTest.this.isRegexTestClassPattern()
                           ? matchClassRegexPatter( testClassFile )
                           : matchClassPatter( testClassFile );
        }

        private MatchPatterns of( String... sources )
        {
            if ( cache == null )
            {
                try
                {
                    checkIllegalCharacters( sources );
                    cache = from( sources );
                }
                catch ( IllegalArgumentException e )
                {
                    throwSanityError( e );
                }
            }
            return cache;
        }

        private boolean matchClassPatter( String testClassFile )
        {
            //@todo We have to use File.separator only because the MatchPatterns is using it internally - cannot override.
            String classPattern = ResolvedTest.this.classPattern;
            if ( separatorChar != '/' )
            {
                testClassFile = testClassFile.replace( '/', separatorChar );
                classPattern = classPattern.replace( '/', separatorChar );
            }

            if ( classPattern.endsWith( WILDCARD_FILENAME_POSTFIX ) || classPattern.endsWith( CLASS_FILE_EXTENSION ) )
            {
                return of( classPattern ).matches( testClassFile, true );
            }
            else
            {
                String[] classPatterns = { classPattern + CLASS_FILE_EXTENSION, classPattern };
                return of( classPatterns ).matches( testClassFile, true );
            }
        }

        private boolean matchClassRegexPatter( String testClassFile )
        {
            String realFile = separatorChar == '/' ? testClassFile : testClassFile.replace( '/', separatorChar );
            return of( classPattern ).matches( realFile, true );
        }
    }

    private final class MethodMatcher
    {
        private volatile Pattern cache;

        boolean matchMethodName( String methodName )
        {
            if ( ResolvedTest.this.isRegexTestMethodPattern() )
            {
                fetchCache();
                return cache.matcher( methodName )
                               .matches();
            }
            else
            {
                return matchPath( ResolvedTest.this.methodPattern, methodName );
            }
        }

        void sanityCheck()
        {
            if ( ResolvedTest.this.isRegexTestMethodPattern() && ResolvedTest.this.hasTestMethodPattern() )
            {
                try
                {
                    checkIllegalCharacters( ResolvedTest.this.methodPattern );
                    fetchCache();
                }
                catch ( IllegalArgumentException e )
                {
                    throwSanityError( e );
                }
            }
        }

        private void fetchCache()
        {
            if ( cache == null )
            {
                int from = REGEX_HANDLER_PREFIX.length();
                int to = ResolvedTest.this.methodPattern.length() - PATTERN_HANDLER_SUFFIX.length();
                String pattern = ResolvedTest.this.methodPattern.substring( from, to );
                cache = compile( pattern );
            }
        }
    }

    private static void checkIllegalCharacters( String... expressions )
    {
        for ( String expression : expressions )
        {
            if ( expression.contains( "#" ) )
            {
                throw new IllegalArgumentException( "Extra '#' in regex: " + expression );
            }
        }
    }

    private static void throwSanityError( IllegalArgumentException e )
    {
        throw new IllegalArgumentException( "%regex[] usage rule violation, valid regex rules:\n"
                                                    + " * <classNameRegex>#<methodNameRegex> - "
                                                    + "where both regex can be individually evaluated as a regex\n"
                                                    + " * you may use at most 1 '#' to in one regex filter. "
                                                    + e.getLocalizedMessage(), e );
    }
}
