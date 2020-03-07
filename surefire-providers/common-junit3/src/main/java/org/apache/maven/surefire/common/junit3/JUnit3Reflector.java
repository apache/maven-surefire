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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.apache.maven.surefire.util.ReflectionUtils;

/**
 * Reflection facade for JUnit3 classes
 *
 */
public final class JUnit3Reflector
{
    private static final String TEST_CASE = "junit.framework.Test";

    private static final String TEST_RESULT = "junit.framework.TestResult";

    private static final String TEST_LISTENER = "junit.framework.TestListener";

    private static final String TEST = "junit.framework.Test";

    private static final String ADD_LISTENER_METHOD = "addListener";

    private static final String RUN_METHOD = "run";

    private static final String TEST_SUITE = "junit.framework.TestSuite";

    private static final Class[] EMPTY_CLASS_ARRAY = { };

    private static final Object[] EMPTY_OBJECT_ARRAY = { };

    private final Class[] interfacesImplementedByDynamicProxy;

    private final Class<?> testResultClass;

    private final Method addListenerMethod;

    private final Method testInterfaceRunMethod;

    private final Class<?> testInterface;

    private final Class<?> testCase;

    private final Constructor testsSuiteConstructor;

    public JUnit3Reflector( ClassLoader testClassLoader )
    {
        testResultClass = ReflectionUtils.tryLoadClass( testClassLoader, TEST_RESULT );
        testCase = ReflectionUtils.tryLoadClass( testClassLoader, TEST_CASE );
        testInterface = ReflectionUtils.tryLoadClass( testClassLoader, TEST );
        interfacesImplementedByDynamicProxy =
            new Class[]{ ReflectionUtils.tryLoadClass( testClassLoader, TEST_LISTENER ) };
        Class<?>[] constructorParamTypes = { Class.class };

        Class<?> testSuite = ReflectionUtils.tryLoadClass( testClassLoader, TEST_SUITE );

        // The interface implemented by the dynamic proxy (TestListener), happens to be
        // the same as the param types of TestResult.addTestListener

        if ( isJUnit3Available() )
        {
            testsSuiteConstructor = ReflectionUtils.getConstructor( testSuite, constructorParamTypes );
            addListenerMethod =
                    tryGetMethod( testResultClass, ADD_LISTENER_METHOD, interfacesImplementedByDynamicProxy );
            testInterfaceRunMethod = getMethod( testInterface, RUN_METHOD, testResultClass );
        }
        else
        {
            testsSuiteConstructor = null;
            addListenerMethod = null;
            testInterfaceRunMethod = null;
        }
    }

    // Switch to reflectionutils when building with 2.7.2
    private static Method tryGetMethod( Class<?> clazz, String methodName, Class<?>... parameters )
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

    private static Method getMethod( Class<?> clazz, String methodName, Class<?>... parameters )
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


    public Object constructTestObject( Class testClass )
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException
    {
        Object testObject = createInstanceFromSuiteMethod( testClass );

        if ( testObject == null && testCase.isAssignableFrom( testClass ) )
        {
            testObject = testsSuiteConstructor.newInstance( testClass );
        }

        if ( testObject == null )
        {
            Constructor testConstructor = getTestConstructor( testClass );

            if ( testConstructor.getParameterTypes().length == 0 )
            {
                testObject = testConstructor.newInstance( EMPTY_OBJECT_ARRAY );
            }
            else
            {
                testObject = testConstructor.newInstance( testClass.getName() );
            }
        }
        return testObject;
    }

    private static Object createInstanceFromSuiteMethod( Class<?> testClass )
        throws IllegalAccessException, InvocationTargetException
    {
        Object testObject = null;
        try
        {
            Method suiteMethod = testClass.getMethod( "suite", EMPTY_CLASS_ARRAY );

            if ( Modifier.isPublic( suiteMethod.getModifiers() ) && Modifier.isStatic( suiteMethod.getModifiers() ) )
            {
                testObject = suiteMethod.invoke( null, EMPTY_OBJECT_ARRAY );
            }
        }
        catch ( NoSuchMethodException e )
        {
            // No suite method
        }
        return testObject;
    }

    private static Constructor getTestConstructor( Class<?> testClass )
        throws NoSuchMethodException
    {
        try
        {
            return testClass.getConstructor( String.class );
        }
        catch ( NoSuchMethodException e )
        {
            return testClass.getConstructor( EMPTY_CLASS_ARRAY );
        }
    }

    public Class[] getInterfacesImplementedByDynamicProxy()
    {
        return interfacesImplementedByDynamicProxy;
    }

    public Class<?> getTestResultClass()
    {
        return testResultClass;
    }

    public Method getAddListenerMethod()
    {
        return addListenerMethod;
    }

    public Method getTestInterfaceRunMethod()
    {
        return testInterfaceRunMethod;
    }

    public Class<?> getTestInterface()
    {
        return testInterface;
    }

    public Method getRunMethod( Class<?> testClass )
    {
        return getMethod( testClass, RUN_METHOD, getTestResultClass() );
    }

    public boolean isJUnit3Available()
    {
        return testResultClass != null;
    }
}
