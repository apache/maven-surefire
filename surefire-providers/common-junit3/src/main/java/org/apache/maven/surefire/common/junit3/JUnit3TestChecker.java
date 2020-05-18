package org.apache.maven.surefire.common.junit3;

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

import java.lang.reflect.Method;
import org.apache.maven.surefire.api.filter.NonAbstractClassFilter;
import org.apache.maven.surefire.api.util.ScannerFilter;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static org.apache.maven.surefire.api.util.ReflectionUtils.tryGetMethod;
import static org.apache.maven.surefire.api.util.ReflectionUtils.tryLoadClass;

/**
 * Missing tests ? This class is basically a subset of the JUnit4TestChecker, which is tested
 * to boredom and back. Unfortunately we don't have any common module between these providers,
 * so this stuff is duplicated. We should probably make some modules and just shade the content
 * into the providers.
 *
 * @author Kristian Rosenvold
 */
public class JUnit3TestChecker
    implements ScannerFilter
{
    private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

    private final Class<?> junitClass;

    private final NonAbstractClassFilter nonAbstractClassFilter = new NonAbstractClassFilter();

    public JUnit3TestChecker( ClassLoader testClassLoader )
    {
        junitClass = tryLoadClass( testClassLoader, "junit.framework.Test" );
    }

    @Override
    public boolean accept( Class testClass )
    {
        return nonAbstractClassFilter.accept( testClass ) && isValidJUnit3Test( testClass );
    }

    private boolean isValidJUnit3Test( Class<?> testClass )
    {
        return junitClass != null && ( junitClass.isAssignableFrom( testClass ) || isSuiteOnly( testClass ) );
    }

    private boolean isSuiteOnly( Class testClass )
    {
        final Method suite = tryGetMethod( testClass, "suite", EMPTY_CLASS_ARRAY );
        if ( suite != null )
        {
            final int modifiers = suite.getModifiers();
            if ( isPublic( modifiers ) && isStatic( modifiers ) )
            {
                return junit.framework.Test.class.isAssignableFrom( suite.getReturnType() );
            }
        }
        return false;
    }

}
