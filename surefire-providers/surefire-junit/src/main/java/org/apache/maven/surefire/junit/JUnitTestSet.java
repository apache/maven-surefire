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
import org.apache.maven.surefire.testset.TestListenerInvocationHandler;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

public final class JUnitTestSet
    extends AbstractTestSet
{
    public static final String TEST_CASE = "junit.framework.TestCase";

    public static final String TEST_RESULT = "junit.framework.TestResult";

    public static final String TEST_LISTENER = "junit.framework.TestListener";

    public static final String TEST = "junit.framework.Test";

    public static final String ADD_LISTENER_METHOD = "addListener";

    public static final String RUN_METHOD = "run";

    public static final String COUNT_TEST_CASES_METHOD = "countTestCases";

    public static final String SETUP_METHOD = "setUp";

    public static final String TEARDOWN_METHOD = "tearDown";

    private static final String TEST_SUITE = "junit.framework.TestSuite";

    private Object testObject;

    private Class[] interfacesImplementedByDynamicProxy;

    private Class testResultClass;

    private Class testClass;

    private Method addListenerMethod;

    private Method countTestCasesMethod;

    private Method runMethod;

    private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public JUnitTestSet( String testClassName, ClassLoader classLoader )
        throws ClassNotFoundException
    {
        testClass = classLoader.loadClass( testClassName );
    }

    public JUnitTestSet( Class testClass )
    {
        if ( testClass == null )
        {
            throw new NullPointerException( "testClass is null" );
        }

        this.testClass = testClass;
    }

    private void processTestClass( ClassLoader loader )
        throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException,
        NoSuchMethodException
    {
        testResultClass = loader.loadClass( TEST_RESULT );

        Class testCaseClass = loader.loadClass( TEST_CASE );

        Class testSuiteClass = loader.loadClass( TEST_SUITE );

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

        if ( testObject == null && testCaseClass.isAssignableFrom( testClass ) )
        {
            Class[] constructorParamTypes = {Class.class};

            Constructor constructor = testSuiteClass.getConstructor( constructorParamTypes );

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
        this.testObject = testObject;

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
            try
            {
                countTestCasesMethod = testClass.getMethod( COUNT_TEST_CASES_METHOD, EMPTY_CLASS_ARRAY );
            }
            catch ( NoSuchMethodException e )
            {
                countTestCasesMethod = null; // for clarity
            }

            try
            {
                runMethod = testClass.getMethod( RUN_METHOD, new Class[]{testResultClass} );
            }
            catch ( NoSuchMethodException e )
            {
                runMethod = null;    // for clarity
            }
        }
    }

    public Class getTestClass()
    {
        return testClass;
    }

    protected Object getTestClassInstance()
    {
        return testObject;
    }

    public void execute( ReporterManager reportManager, ClassLoader loader )
        throws TestSetFailedException
    {
        try
        {
            processTestClass( loader );
        }
        catch ( ClassNotFoundException e )
        {
            throw new TestSetFailedException( "JUnit classes not available", e );
        }
        catch ( IllegalAccessException e )
        {
            throw new TestSetFailedException( "Unknown access exception creating JUnit classes", e );
        }
        catch ( InvocationTargetException e )
        {
            throw new TestSetFailedException( "Unknown invocation exception creating JUnit classes", e );
        }
        catch ( InstantiationException e )
        {
            throw new TestSetFailedException( "Unknown instantiation exception creating JUnit classes", e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new TestSetFailedException( "Class is not a JUnit TestCase", e );
        }

        // TODO: why do we accept runMethod == null? That means it doesn't extend TestCase
        if ( runMethod != null )
        {
            executeJUnit( reportManager, loader );
        }
        else
        {
            super.execute( reportManager, loader );
        }
    }

    private void executeJUnit( ReporterManager reportManager, ClassLoader classLoader )
        throws TestSetFailedException
    {
        try
        {
            Object instanceOfTestResult = testResultClass.newInstance();

            TestListenerInvocationHandler invocationHandler =
                new TestListenerInvocationHandler( reportManager, instanceOfTestResult, classLoader );

            Object testListener =
                Proxy.newProxyInstance( classLoader, interfacesImplementedByDynamicProxy, invocationHandler );

            Object[] addTestListenerParams = {testListener};

            addListenerMethod.invoke( instanceOfTestResult, addTestListenerParams );

            Object[] runParams = {instanceOfTestResult};

            runMethod.invoke( testObject, runParams );
        }
        catch ( IllegalArgumentException e )
        {
            throw new TestSetFailedException( testObject.getClass().getName(), e );
        }
        catch ( InstantiationException e )
        {
            throw new TestSetFailedException( testObject.getClass().getName(), e );
        }
        catch ( IllegalAccessException e )
        {
            throw new TestSetFailedException( testObject.getClass().getName(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new TestSetFailedException( testObject.getClass().getName(), e );
        }
    }

    public int getTestCount()
        throws TestSetFailedException
    {
        try
        {
            int testCount;
            if ( countTestCasesMethod != null )
            {
                Integer integer = (Integer) countTestCasesMethod.invoke( testObject, EMPTY_CLASS_ARRAY );

                testCount = integer.intValue();
            }
            else
            {
                testCount = super.getTestCount();
            }
            return testCount;
        }
        catch ( IllegalAccessException e )
        {
            throw new TestSetFailedException( testObject.getClass().getName(), e );
        }
        catch ( IllegalArgumentException e )
        {
            throw new TestSetFailedException( testObject.getClass().getName(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new TestSetFailedException( testObject.getClass().getName(), e );
        }
    }

    public String getName()
    {
        return testClass.getName();
    }

    private Constructor getTestConstructor( Class testClass )
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
