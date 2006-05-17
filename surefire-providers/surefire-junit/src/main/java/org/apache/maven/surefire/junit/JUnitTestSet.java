package org.apache.maven.surefire.junit;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.testset.AbstractTestSet;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

public final class JUnitTestSet
    extends AbstractTestSet
{
    public static final String TEST_CASE = "junit.framework.Test";

    public static final String TEST_RESULT = "junit.framework.TestResult";

    public static final String TEST_LISTENER = "junit.framework.TestListener";

    public static final String TEST = "junit.framework.Test";

    public static final String ADD_LISTENER_METHOD = "addListener";

    public static final String RUN_METHOD = "run";

    public static final String COUNT_TEST_CASES_METHOD = "countTestCases";

    public static final String SETUP_METHOD = "setUp";

    public static final String TEARDOWN_METHOD = "tearDown";

    private static final String TEST_SUITE = "junit.framework.TestSuite";

    private Class[] interfacesImplementedByDynamicProxy;

    private Class testResultClass;

    private Method addListenerMethod;

    private Method countTestCasesMethod;

    private Method runMethod;

    private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public JUnitTestSet( Class testClass )
        throws TestSetFailedException
    {
        super( testClass );

        processTestClass();
    }

    private void processTestClass()
        throws TestSetFailedException
    {
        try
        {
            Class testClass = getTestClass();
            ClassLoader loader = testClass.getClassLoader();

            testResultClass = loader.loadClass( TEST_RESULT );

            Class testListenerInterface = loader.loadClass( TEST_LISTENER );

            Class testInterface = loader.loadClass( TEST );

            // ----------------------------------------------------------------------
            // Strategy for executing JUnit tests
            //
            // o look for the suite method and if that is present execute that method
            //   to get the test object.
            //
            // o look for test classes that are assignable from TestCase
            //
            // o look for test classes that only implement the Test interface
            // ----------------------------------------------------------------------

            interfacesImplementedByDynamicProxy = new Class[1];

            interfacesImplementedByDynamicProxy[0] = testListenerInterface;

            // The interface implemented by the dynamic proxy (TestListener), happens to be
            // the same as the param types of TestResult.addTestListener
            Class[] addListenerParamTypes = interfacesImplementedByDynamicProxy;

            addListenerMethod = testResultClass.getMethod( ADD_LISTENER_METHOD, addListenerParamTypes );

            if ( testInterface.isAssignableFrom( testClass ) )//testObject.getClass() ) )
            {
                countTestCasesMethod = testInterface.getMethod( COUNT_TEST_CASES_METHOD, EMPTY_CLASS_ARRAY );

                runMethod = testInterface.getMethod( RUN_METHOD, new Class[]{testResultClass} );

            }
            else
            {
                countTestCasesMethod = testClass.getMethod( COUNT_TEST_CASES_METHOD, EMPTY_CLASS_ARRAY );

                runMethod = testClass.getMethod( RUN_METHOD, new Class[]{testResultClass} );
            }
        }
        catch ( ClassNotFoundException e )
        {
            throw new TestSetFailedException( "JUnit classes not available", e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new TestSetFailedException( "Class is not a JUnit TestCase", e );
        }
    }

    private static Object constructTestObject( Class testClass )
        throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException,
        ClassNotFoundException
    {
        Object testObject = createInstanceFromSuiteMethod( testClass );

        if ( testObject == null && testClass.getClassLoader().loadClass( TEST_CASE ).isAssignableFrom( testClass ) )
        {
            Class[] constructorParamTypes = {Class.class};

            Constructor constructor =
                testClass.getClassLoader().loadClass( TEST_SUITE ).getConstructor( constructorParamTypes );

            Object[] constructorParams = {testClass};

            testObject = constructor.newInstance( constructorParams );
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
                testObject = testConstructor.newInstance( new Object[]{testClass.getName()} );
            }
        }
        return testObject;
    }

    private static Object createInstanceFromSuiteMethod( Class testClass )
        throws IllegalAccessException, InvocationTargetException
    {
        Object testObject = null;
        try
        {
            Method suiteMethod = testClass.getMethod( "suite", EMPTY_CLASS_ARRAY );

            if ( Modifier.isPublic( suiteMethod.getModifiers() ) && Modifier.isStatic( suiteMethod.getModifiers() ) )
            {
                testObject = suiteMethod.invoke( null, EMPTY_CLASS_ARRAY );
            }
        }
        catch ( NoSuchMethodException e )
        {
            // No suite method
        }
        return testObject;
    }

    public void execute( ReporterManager reportManager, ClassLoader loader )
        throws TestSetFailedException
    {
        Class testClass = getTestClass();

        try
        {
            Object testObject = constructTestObject( testClass );

            Object instanceOfTestResult = testResultClass.newInstance();

            TestListenerInvocationHandler invocationHandler =
                new TestListenerInvocationHandler( reportManager, instanceOfTestResult, loader );

            Object testListener =
                Proxy.newProxyInstance( loader, interfacesImplementedByDynamicProxy, invocationHandler );

            Object[] addTestListenerParams = {testListener};

            addListenerMethod.invoke( instanceOfTestResult, addTestListenerParams );

            Object[] runParams = {instanceOfTestResult};

            runMethod.invoke( testObject, runParams );
        }
        catch ( IllegalArgumentException e )
        {
            throw new TestSetFailedException( testClass.getName(), e );
        }
        catch ( InstantiationException e )
        {
            throw new TestSetFailedException( testClass.getName(), e );
        }
        catch ( IllegalAccessException e )
        {
            throw new TestSetFailedException( testClass.getName(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new TestSetFailedException( testClass.getName(), e.getTargetException() );
        }
        catch ( ClassNotFoundException e )
        {
            throw new TestSetFailedException( "JUnit classes not available", e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new TestSetFailedException( "Class is not a JUnit TestCase", e );
        }
    }

    public int getTestCount()
        throws TestSetFailedException
    {
        Class testClass = getTestClass();
        try
        {
            Object testObject = constructTestObject( testClass );

            Integer integer = (Integer) countTestCasesMethod.invoke( testObject, EMPTY_CLASS_ARRAY );

            return integer.intValue();
        }
        catch ( IllegalAccessException e )
        {
            throw new TestSetFailedException( testClass.getName(), e );
        }
        catch ( IllegalArgumentException e )
        {
            throw new TestSetFailedException( testClass.getName(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new TestSetFailedException( testClass.getName(), e.getTargetException() );
        }
        catch ( InstantiationException e )
        {
            throw new TestSetFailedException( testClass.getName(), e );
        }
        catch ( ClassNotFoundException e )
        {
            throw new TestSetFailedException( "JUnit classes not available", e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new TestSetFailedException( "Class is not a JUnit TestCase", e );
        }
    }

    private static Constructor getTestConstructor( Class testClass )
        throws NoSuchMethodException
    {
        Constructor constructor;
        try
        {
            constructor = testClass.getConstructor( new Class[]{String.class} );
        }
        catch ( NoSuchMethodException e )
        {
            constructor = testClass.getConstructor( EMPTY_CLASS_ARRAY );
        }
        return constructor;
    }
}
