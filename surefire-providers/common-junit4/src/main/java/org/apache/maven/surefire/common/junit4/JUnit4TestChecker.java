package org.apache.maven.surefire.common.junit4;

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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.apache.maven.surefire.api.filter.NonAbstractClassFilter;
import org.apache.maven.surefire.common.junit3.JUnit3TestChecker;
import org.apache.maven.surefire.api.util.ScannerFilter;

import static org.apache.maven.surefire.api.util.ReflectionUtils.tryLoadClass;

/**
 * @author Kristian Rosenvold
 */
public class JUnit4TestChecker
    implements ScannerFilter
{
    private final NonAbstractClassFilter nonAbstractClassFilter;

    private final Class runWith;

    private final JUnit3TestChecker jUnit3TestChecker;


    public JUnit4TestChecker( ClassLoader testClassLoader )
    {
        jUnit3TestChecker = new JUnit3TestChecker( testClassLoader );
        runWith = tryLoadClass( testClassLoader, org.junit.runner.RunWith.class.getName() );
        nonAbstractClassFilter = new NonAbstractClassFilter();
    }

    @Override
    public boolean accept( Class testClass )
    {
        return jUnit3TestChecker.accept( testClass ) || isValidJUnit4Test( testClass );
    }

    @SuppressWarnings( { "unchecked" } )
    private boolean isValidJUnit4Test( Class testClass )
    {
        if ( !nonAbstractClassFilter.accept( testClass ) )
        {
            return false;
        }

        if ( isRunWithPresentInClassLoader() )
        {
            Annotation runWithAnnotation = testClass.getAnnotation( runWith );
            if ( runWithAnnotation != null )
            {
                return true;
            }
        }

        return lookForTestAnnotatedMethods( testClass );
    }

    private boolean lookForTestAnnotatedMethods( Class testClass )
    {
        Class classToCheck = testClass;
        while ( classToCheck != null )
        {
            if ( checkforTestAnnotatedMethod( classToCheck ) )
            {
                return true;
            }
            classToCheck = classToCheck.getSuperclass();
        }
        return false;
    }

    public boolean checkforTestAnnotatedMethod( Class testClass )
    {
        for ( Method lMethod : testClass.getDeclaredMethods() )
        {
            for ( Annotation lAnnotation : lMethod.getAnnotations() )
            {
                if ( org.junit.Test.class.isAssignableFrom( lAnnotation.annotationType() ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isRunWithPresentInClassLoader()
    {
        return runWith != null;
    }
}
