package org.apache.maven.surefire.junit4;
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

import org.apache.maven.surefire.testset.TestSetFailedException;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Kristian Rosenvold
 */
public class JUnit4TestChecker
{
    private final Class junitClass;

    private final Class runWith;

    private final Class suite;

    public JUnit4TestChecker( ClassLoader testClassLoader )
    {
        this.junitClass = getJUnitClass( testClassLoader, junit.framework.Test.class.getName() );
        this.suite = getJUnitClass( testClassLoader, org.junit.runners.Suite.class.getName() );
        this.runWith = getJUnitClass( testClassLoader, org.junit.runner.RunWith.class.getName() );
    }

    @SuppressWarnings( { "unchecked" } )
    public boolean isValidJUnit4Test( Class testClass )
        throws TestSetFailedException
    {
        if ( junitClass != null && junitClass.isAssignableFrom( testClass ) )
        {
            return true;
        }

        Annotation runWithAnno = testClass.getAnnotation( runWith );
        if ( runWithAnno != null )
        {
            final Class value = runWithValue( runWithAnno );
            if ( suite.isAssignableFrom( value ) )
            {
                return true;
            }
        }

        Class classToCheck = testClass;
        while (classToCheck != null){
            if ( checkforTestAnnotatedMethod( classToCheck )){
                return true;
            }
            classToCheck = classToCheck.getSuperclass();
        }
        return false;
    }

    private boolean checkforTestAnnotatedMethod( Class testClass )
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

    private Class getJUnitClass( ClassLoader classLoader, String className )
    {
        Class junitClass = null;
        try
        {
            junitClass = classLoader.loadClass( className );
        }
        catch ( NoClassDefFoundError e )
        {
            // ignore this
        }
        catch ( ClassNotFoundException e )
        {
            // ignore this
        }
        return junitClass;
    }

    private Class runWithValue(Object object)
        throws TestSetFailedException
    {
        final Method valueMethod;
        try
        {
            valueMethod = object.getClass().getMethod( "value" );
            return (Class) valueMethod.invoke(  object);
        }
        catch ( NoSuchMethodException e )
        {
            throw new TestSetFailedException( e );
        }
        catch ( InvocationTargetException e )
        {
            throw new TestSetFailedException( e );
        }
        catch ( IllegalAccessException e )
        {
            throw new TestSetFailedException( e );
        }
    }

}
