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
public final class ReflectionUtils
{
    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private ReflectionUtils()
    {
        throw new IllegalStateException( "no instantiable constructor" );
    }

    public static Method getMethod( Object instance, String methodName, Class<?>... parameters )
    {
        return getMethod( instance.getClass(), methodName, parameters );
    }

    public static Method getMethod( Class<?> clazz, String methodName, Class<?>... parameters )
    {
        try
        {
            return clazz.getMethod( methodName, parameters );
        }
        catch ( NoSuchMethodException e )
        {
            throw new RuntimeException( "When finding method " + methodName, e );
        }
    }

    public static Method tryGetMethod( Class<?> clazz, String methodName, Class<?>... parameters )
    {
        try
        {
            return clazz.getMethod( methodName, parameters );
        }
        catch ( NoSuchMethodException e )
        {
            return null;
        }
    }

    public static Object invokeGetter( Object instance, String methodName )
    {
        return invokeGetter( instance.getClass(), instance, methodName );
    }

    public static Object invokeGetter( Class<?> instanceType, Object instance, String methodName )
    {
        Method method = getMethod( instanceType, methodName );
        return invokeMethodWithArray( instance, method );
    }

    public static Constructor getConstructor( Class<?> clazz, Class<?>... arguments )
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

    public static Object newInstance( Constructor constructor, Object... params )
    {
        try
        {
            return constructor.newInstance( params );
        }
        catch ( ReflectiveOperationException e )
        {
            throw new SurefireReflectionException( e );
        }
    }

    public static <T> T instantiate( ClassLoader classLoader, String classname, Class<T> returnType )
    {
        try
        {
            Class<?> clazz = loadClass( classLoader, classname );
            return returnType.cast( clazz.newInstance() );
        }
        catch ( ReflectiveOperationException e )
        {
            throw new SurefireReflectionException( e );
        }
    }

    public static Object instantiateOneArg( ClassLoader classLoader, String className, Class<?> param1Class,
                                            Object param1 )
    {
        try
        {
            Class<?> aClass = loadClass( classLoader, className );
            Constructor constructor = getConstructor( aClass, param1Class );
            return constructor.newInstance( param1 );
        }
        catch ( InvocationTargetException e )
        {
            throw new SurefireReflectionException( e.getTargetException() );
        }
        catch ( ReflectiveOperationException e )
        {
            throw new SurefireReflectionException( e );
        }
    }

    public static Object instantiateTwoArgs( ClassLoader classLoader, String className, Class<?> param1Class,
                                             Object param1, Class param2Class, Object param2 )
    {
        try
        {
            Class<?> aClass = loadClass( classLoader, className );
            Constructor constructor = getConstructor( aClass, param1Class, param2Class );
            return constructor.newInstance( param1, param2 );
        }
        catch ( InvocationTargetException e )
        {
            throw new SurefireReflectionException( e.getTargetException() );
        }
        catch ( ReflectiveOperationException e )
        {
            throw new SurefireReflectionException( e );
        }
    }

    public static void invokeSetter( Object o, String name, Class<?> value1clazz, Object value )
    {
        Method setter = getMethod( o, name, value1clazz );
        invokeSetter( o, setter, value );
    }

    public static Object invokeSetter( Object target, Method method, Object value )
    {
        return invokeMethodWithArray( target, method, value );
    }

    public static Object invokeMethodWithArray( Object target, Method method, Object... args )
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
            throw new SurefireReflectionException( e.getTargetException() );
        }
    }

    public static Object invokeMethodWithArray2( Object target, Method method, Object... args )
        throws InvocationTargetException
    {
        try
        {
            return method.invoke( target, args );
        }
        catch ( IllegalAccessException e )
        {
            throw new SurefireReflectionException( e );
        }
    }

    public static Object instantiateObject( String className, Class[] types, Object[] params, ClassLoader classLoader )
    {
        Class<?> clazz = loadClass( classLoader, className );
        final Constructor constructor = getConstructor( clazz, types );
        return newInstance( constructor, params );
    }

    @SuppressWarnings( "checkstyle:emptyblock" )
    public static Class<?> tryLoadClass( ClassLoader classLoader, String className )
    {
        try
        {
            return classLoader.loadClass( className );
        }
        catch ( NoClassDefFoundError | ClassNotFoundException ignore )
        {
        }
        return null;
    }

    public static Class<?> loadClass( ClassLoader classLoader, String className )
    {
        try
        {
            return classLoader.loadClass( className );
        }
        catch ( NoClassDefFoundError | ClassNotFoundException e )
        {
            throw new SurefireReflectionException( e );
        }
    }

    /**
     * Invoker of public static no-argument method.
     *
     * @param clazz         class on which public static no-argument {@code methodName} is invoked
     * @param methodName    public static no-argument method to be called
     * @param parameterTypes    method parameter types
     * @param parameters    method parameters
     * @return value returned by {@code methodName}
     * @throws RuntimeException if no such method found
     * @throws SurefireReflectionException if the method could not be called or threw an exception.
     * It has original cause Exception.
     */
    public static Object invokeStaticMethod( Class<?> clazz, String methodName,
                                             Class<?>[] parameterTypes, Object[] parameters )
    {
        if ( parameterTypes.length != parameters.length )
        {
            throw new IllegalArgumentException( "arguments length do not match" );
        }
        Method method = getMethod( clazz, methodName, parameterTypes );
        return invokeMethodWithArray( null, method, parameters );
    }

    /**
     * Method chain invoker.
     *
     * @param classesChain        classes to invoke on method chain
     * @param noArgMethodNames    chain of public methods to call
     * @param fallback            returned value if a chain could not be invoked due to an error
     * @return successfully returned value from the last method call; {@code fallback} otherwise
     * @throws IllegalArgumentException if {@code classes} and {@code noArgMethodNames} have different array length
     */
    public static Object invokeMethodChain( Class<?>[] classesChain, String[] noArgMethodNames, Object fallback )
    {
        if ( classesChain.length != noArgMethodNames.length )
        {
            throw new IllegalArgumentException( "arrays must have the same length" );
        }
        Object obj = null;
        try
        {
            for ( int i = 0, len = noArgMethodNames.length; i < len; i++ )
            {
                if ( i == 0 )
                {
                    obj = invokeStaticMethod( classesChain[i], noArgMethodNames[i],
                                                    EMPTY_CLASS_ARRAY, EMPTY_OBJECT_ARRAY );
                }
                else
                {
                    Method method = getMethod( classesChain[i], noArgMethodNames[i] );
                    obj = invokeMethodWithArray( obj, method );
                }
            }
            return obj;
        }
        catch ( RuntimeException e )
        {
            return fallback;
        }
    }
}
