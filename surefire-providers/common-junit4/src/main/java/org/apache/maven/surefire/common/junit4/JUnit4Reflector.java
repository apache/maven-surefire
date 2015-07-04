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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.maven.surefire.util.ReflectionUtils;

import org.apache.maven.surefire.util.SurefireReflectionException;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.Request;

/**
 * JUnit4 reflection helper
 *
 */
public final class JUnit4Reflector
{
    private static final Class[] PARAMS = new Class[]{ Class.class };

    private static final Class[] IGNORE_PARAMS = new Class[]{ Ignore.class };

    private JUnit4Reflector()
    {
        throw new IllegalStateException( "not instantiable constructor" );
    }

    public static Ignore getAnnotatedIgnore( Description description )
    {
        Method getAnnotation = ReflectionUtils.tryGetMethod( description.getClass(), "getAnnotation", PARAMS );

        if ( getAnnotation == null )
        {
            return null;
        }

        return (Ignore) ReflectionUtils.invokeMethodWithArray( description, getAnnotation, IGNORE_PARAMS );
    }

    public static String getAnnotatedIgnoreValue( Description description )
    {
        final Ignore ignore = getAnnotatedIgnore( description );
        return ignore != null ? ignore.value() : null;
    }

    public static Request createRequest( Class<?>... classes )
    {
        try
        {
            return (Request) Request.class.getDeclaredMethod( "classes", Class[].class )// Since of JUnit 4.5
                .invoke( null, new Object[]{ classes } );
        }
        catch ( NoSuchMethodException e )
        {
            return Request.classes( null, classes ); // Since of JUnit 4.0
        }
        catch ( InvocationTargetException e )
        {
            throw new SurefireReflectionException( e.getCause() );
        }
        catch ( IllegalAccessException e )
        {
            // probably JUnit 5.x
            throw new SurefireReflectionException( e );
        }
    }

    public static Description createDescription( String description )
    {
        try
        {
            return Description.createSuiteDescription( description );
        }
        catch ( NoSuchMethodError e )
        {
            try
            {
                return (Description) Description.class.getDeclaredMethod( "createSuiteDescription",
                                                                          String.class, Annotation[].class )
                    .invoke( null, description, new Annotation[0] );
            }
            catch ( InvocationTargetException e1 )
            {
                throw new SurefireReflectionException( e1.getCause() );
            }
            catch ( Exception e1 )
            {
                // probably JUnit 5.x
                throw new SurefireReflectionException( e1 );
            }
        }
    }
}
