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

import org.apache.maven.surefire.util.SurefireReflectionException;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.Request;

import static org.apache.maven.surefire.util.ReflectionUtils.tryGetMethod;
import static org.apache.maven.surefire.util.ReflectionUtils.invokeMethodWithArray;

/**
 * JUnit4 reflection helper
 *
 */
public final class JUnit4Reflector
{
    private static final Class[] PARAMS = { Class.class };

    private static final Class[] IGNORE_PARAMS = { Ignore.class };

    private static final Class[] PARAMS_WITH_ANNOTATIONS = { String.class, Annotation[].class };

    private JUnit4Reflector()
    {
        throw new IllegalStateException( "not instantiable constructor" );
    }

    public static Ignore getAnnotatedIgnore( Description d )
    {
        Method getAnnotation = tryGetMethod( d.getClass(), "getAnnotation", PARAMS );
        return getAnnotation == null ? null : (Ignore) invokeMethodWithArray( d, getAnnotation, IGNORE_PARAMS );
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
            Method method = tryGetMethod( Description.class, "createSuiteDescription", PARAMS_WITH_ANNOTATIONS );
            // may throw exception probably with JUnit 5.x
            return (Description) invokeMethodWithArray( null, method, description, new Annotation[0] );
        }
    }

    public static Description createDescription( String description, Annotation... annotations )
    {
        Method method = tryGetMethod( Description.class, "createSuiteDescription", PARAMS_WITH_ANNOTATIONS );
        return method == null
            ? Description.createSuiteDescription( description )
            : (Description) invokeMethodWithArray( null, method, description, annotations );
    }

    public static Ignore createIgnored( String value )
    {
        return new IgnoredWithUserError( value );
    }

    private static class IgnoredWithUserError
        implements Annotation, Ignore
    {
        private final String value;

        public IgnoredWithUserError( String value )
        {
            this.value = value;
        }

        public IgnoredWithUserError()
        {
            this( "" );
        }

        @Override
        public String value()
        {
            return value;
        }

        @Override
        public Class<? extends Annotation> annotationType()
        {
            return Ignore.class;
        }

        @Override
        public int hashCode()
        {
            return value == null ? 0 : value.hashCode();
        }

        @Override
        public boolean equals( Object obj )
        {
            return obj instanceof Annotation && obj instanceof Ignore && equalValue( ( Ignore ) obj );
        }

        @Override
        public String toString()
        {
            return String.format( "%s(%s)", Ignore.class, value );
        }

        private boolean equalValue( Ignore ignore )
        {
            if ( ignore == null )
            {
                return false;
            }
            else
            {
                String val = ignore.value();
                return val == null ? value == null : val.equals( value );
            }
        }
    }
}
