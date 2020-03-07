package org.apache.maven.surefire.junit;

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.apache.maven.surefire.common.junit3.JUnit3Reflector;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.testset.TestSetFailedException;

/**
 * JUnit3 test set
 *
 */
public final class JUnitTestSet
    implements SurefireTestSet
{
    private final Class testClass;

    private final JUnit3Reflector reflector;

    public JUnitTestSet( Class testClass, JUnit3Reflector reflector )
    {
        if ( testClass == null )
        {
            throw new NullPointerException( "testClass is null" );
        }

        this.testClass = testClass;
        this.reflector = reflector;

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

        // The interface implemented by the dynamic proxy (TestListener), happens to be
        // the same as the param types of TestResult.addTestListener
    }


    @Override
    public void execute( RunListener reporter, ClassLoader loader )
        throws TestSetFailedException
    {
        Class testClass = getTestClass();

        try
        {
            Object testObject = reflector.constructTestObject( testClass );
            final Method runMethod;

            if ( reflector.getTestInterface().isAssignableFrom( testObject.getClass() ) )
            {
                runMethod = reflector.getTestInterfaceRunMethod();
            }
            else
            {
                runMethod = reflector.getRunMethod( testClass );
            }

            Object instanceOfTestResult = reflector.getTestResultClass().newInstance();

            TestListenerInvocationHandler invocationHandler = new TestListenerInvocationHandler( reporter );

            Object testListener =
                Proxy.newProxyInstance( loader, reflector.getInterfacesImplementedByDynamicProxy(), invocationHandler );

            Object[] addTestListenerParams = { testListener };

            reflector.getAddListenerMethod().invoke( instanceOfTestResult, addTestListenerParams );

            Object[] runParams = { instanceOfTestResult };

            runMethod.invoke( testObject, runParams );
        }
        catch ( InvocationTargetException e )
        {
            throw new TestSetFailedException( testClass.getName(), e.getTargetException() );
        }
        catch ( NoSuchMethodException e )
        {
            throw new TestSetFailedException( "Class is not a JUnit TestCase", e );
        }
        catch ( ReflectiveOperationException e )
        {
            throw new TestSetFailedException( testClass.getName(), e );
        }
    }

    @Override
    public String getName()
    {
        return testClass.getName();
    }

    private Class getTestClass()
    {
        return testClass;
    }
}
