package org.apache.maven.surefire.util.internal;

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

import org.apache.maven.surefire.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static java.lang.Math.pow;

/**
 * JDK 9 support.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
public final class SystemUtils
{
    private static final Pattern JAVA_SPEC_VERSION_PATTERN = Pattern.compile( "(\\d+)(\\.?)(\\d*).*" );
    private static final double JAVA_SPEC_VERSION = javaSpecVersion();

    public SystemUtils()
    {
        throw new IllegalStateException( "no instantiable constructor" );
    }

    public static ClassLoader platformClassLoader()
    {
        if ( JAVA_SPEC_VERSION < 9 )
        {
            return null;
        }

        return reflectClassLoader( ClassLoader.class, "getPlatformClassLoader" );
    }

    public static double javaSpecVersion()
    {
        return extractJavaSpecVersion( System.getProperty( "java.specification.version" ) );
    }

    static ClassLoader reflectClassLoader( Class<?> target, String getterMethodName )
    {
        try
        {
            Method getter = ReflectionUtils.getMethod( target, getterMethodName );
            return (ClassLoader) ReflectionUtils.invokeMethodWithArray( null, getter );
        }
        catch ( RuntimeException e )
        {
            return null;
        }
    }

    static double extractJavaSpecVersion( String property )
    {
        Matcher versionRegexMatcher = JAVA_SPEC_VERSION_PATTERN.matcher( property );
        int groups = versionRegexMatcher.groupCount();
        if ( !versionRegexMatcher.matches() )
        {
            throw new IllegalStateException( "Java Spec Version does not match the pattern "
                                                     + JAVA_SPEC_VERSION_PATTERN
            );
        }

        if ( groups >= 3 )
        {
            String majorVersion = versionRegexMatcher.group( 1 );
            String minorVersion = versionRegexMatcher.group( 3 );
            int major = parseInt( majorVersion );
            double minor = minorVersion.isEmpty() ? 0 : parseInt( minorVersion ) / pow( 10, minorVersion.length() );
            return major + minor;
        }
        else
        {
            return parseInt( versionRegexMatcher.group( 0 ) );
        }
    }
}
