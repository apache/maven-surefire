package org.apache.maven.surefire.util;

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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Kristian Rosenvold
 */
public class ReflectionUtils
{
    private static final Class[] noargs = new Class[0];

    private static final Object[] noargsValues = new Object[0];


    public static Method getMethod( Object instance, String methodName, Class[] parameters )
    {
        try
        {
            return instance.getClass().getMethod( methodName, parameters );
        }
        catch ( NoSuchMethodException e )
        {
            throw new RuntimeException( "When finding method " + methodName, e );
        }
    }

    public static Object invokeGetter( Object instance, String methodName )
    {
        final Method method = getMethod( instance, methodName, noargs );
        return invokeMethodWithArray( instance, method, noargsValues );
    }

    public static Constructor getConstructor( Class clazz, Class[] arguments )
    {
        try
        {
            return clazz.getConstructor( arguments );
        }
        catch ( NoSuchMethodException e )
        {
            throw new SurefireReflectionException( e );
        }
    }

    public static Object newInstance( Constructor constructor, Object[] params )
    {
        try
        {
            return constructor.newInstance( params );
        }
        catch ( InvocationTargetException e )
        {
            throw new SurefireReflectionException( e );
        }
        catch ( InstantiationException e )
        {
            throw new SurefireReflectionException( e );
        }
        catch ( IllegalAccessException e )
        {
            throw new SurefireReflectionException( e );
        }
    }

    public static Object instantiate( ClassLoader classLoader, String classname )
    {
        try
        {


            Class clazz = loadClass(  classLoader, classname );
            return clazz.newInstance();
        }
        catch ( InstantiationException e )
        {
            throw new SurefireReflectionException( e );
        }
        catch ( IllegalAccessException e )
        {
            throw new SurefireReflectionException( e );
        }
    }

    public static Object instantiateOneArg( ClassLoader classLoader, String className, Class param1Class,
                                            Object param1 )
    {

        try
        {
            Class aClass = loadClass(classLoader, className );
            Constructor constructor = ReflectionUtils.getConstructor( aClass, new Class[]{ param1Class } );
            return constructor.newInstance( new Object[]{ param1 } );
        }
        catch ( InvocationTargetException e )
        {
            throw new SurefireReflectionException( e );
        }
        catch ( InstantiationException e )
        {
            throw new SurefireReflectionException( e );
        }
        catch ( IllegalAccessException e )
        {
            throw new SurefireReflectionException( e );
        }
    }

    public static Object invokeSetter( Object o, String name, Class value1clazz, Object value )

    {
        final Method setter = getMethod( o, name, new Class[]{ value1clazz } );
        return invokeSetter( o, setter, value );
    }

    public static Object invokeSetter( Object target, Method method, Object value )

    {
        return invokeMethodWithArray( target, method, new Object[]{ value } );
    }

    public static Object invokeMethodWithArray( Object target, Method method, Object[] args )

    {
        try
        {
            return method.invoke( target, args );
        }
        catch ( IllegalAccessException e )
        {
            throw new SurefireReflectionException( e );
        }
        catch ( InvocationTargetException e )
        {
            throw new SurefireReflectionException( e );
        }

    }

    public static Object instantiateObject( String className, Object[] params, ClassLoader classLoader )
    {
        try
        {
            Class clazz = loadClass( classLoader, className );

            Object object;
            if ( params != null )
            {
                Class[] paramTypes = new Class[params.length];

                for ( int j = 0; j < params.length; j++ )
                {
                    if ( params[j] == null )
                    {
                        paramTypes[j] = String.class;
                    }
                    else
                    {
                        paramTypes[j] = params[j].getClass();
                    }
                }


                Constructor constructor = getConstructor( clazz, paramTypes );

                object = newInstance( constructor, params );
            }
            else
            {
                object = clazz.newInstance();
            }
            return object;

        }
        catch ( IllegalAccessException e )
        {
            throw new SurefireReflectionException( e );
        }
        catch ( InstantiationException e )
        {
            throw new SurefireReflectionException( e );
        }
    }

    public static Class tryLoadClass( ClassLoader classLoader, String className )
    {
        try
        {
            return classLoader.loadClass( className );
        }
        catch ( NoClassDefFoundError ignore )
        {
        }
        catch ( ClassNotFoundException ignore )
        {
        }
        return null;
    }

    public static Class loadClass( ClassLoader classLoader, String className )
    {
        try
        {
            return classLoader.loadClass( className );
        }
        catch ( NoClassDefFoundError e )
        {
            throw new SurefireReflectionException( e );
        }
        catch ( ClassNotFoundException e )
        {
            throw new SurefireReflectionException( e );
        }
    }
}
