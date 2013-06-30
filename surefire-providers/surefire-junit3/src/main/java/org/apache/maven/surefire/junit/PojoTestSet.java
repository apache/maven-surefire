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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.surefire.report.LegacyPojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.testset.TestSetFailedException;

public class PojoTestSet
    implements SurefireTestSet
{

    private static final String TEST_METHOD_PREFIX = "test";

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private final Object testObject;

    private List<Method> testMethods;

    private Method setUpMethod;

    private Method tearDownMethod;

    private final Class testClass;

    public PojoTestSet( Class testClass )
        throws TestSetFailedException
    {
        if ( testClass == null )
        {
            throw new NullPointerException( "testClass is null" );
        }

        this.testClass = testClass;

        try
        {
            testObject = testClass.newInstance();
        }
        catch ( InstantiationException e )
        {
            throw new TestSetFailedException( "Unable to instantiate POJO '" + testClass + "'", e );
        }
        catch ( IllegalAccessException e )
        {
            throw new TestSetFailedException( "Unable to instantiate POJO '" + testClass + "'", e );
        }
    }

    public void execute( RunListener reportManager, ClassLoader loader )
        throws TestSetFailedException
    {
        if ( reportManager == null )
        {
            throw new NullPointerException( "reportManager is null" );
        }

        executeTestMethods( reportManager );
    }

    private void executeTestMethods( RunListener reportManager )
    {
        if ( reportManager == null )
        {
            throw new NullPointerException( "reportManager is null" );
        }

        if ( testMethods == null )
        {
            discoverTestMethods();
        }

        boolean abort = false;

        for ( int i = 0; i < testMethods.size() && !abort; ++i )
        {
            abort = executeTestMethod( testMethods.get( i ), EMPTY_OBJECT_ARRAY, reportManager );
        }
    }

    private boolean executeTestMethod( Method method, Object[] args, RunListener reportManager )
    {
        if ( method == null || args == null || reportManager == null )
        {
            throw new NullPointerException();
        }

        String userFriendlyMethodName = method.getName() + '(';

        if ( args.length != 0 )
        {
            userFriendlyMethodName += "Reporter";
        }

        userFriendlyMethodName += ')';

        ReportEntry report =
            new SimpleReportEntry( testObject.getClass().getName(), getTestName( userFriendlyMethodName ) );

        reportManager.testStarting( report );

        try
        {
            setUpFixture();
        }
        catch ( Throwable e )
        {
            report =
                SimpleReportEntry.withException( testObject.getClass().getName(), getTestName( userFriendlyMethodName ),
                                                 new LegacyPojoStackTraceWriter( testObject.getClass().getName(),
                                                                                 method.getName(), e ) );

            reportManager.testFailed( report );

            // A return value of true indicates to this class's executeTestMethods
            // method that it should abort and not attempt to execute
            // any other test methods. The other caller of this method,
            // TestRerunner.rerun, ignores this return value, because it is
            // only running one test.
            return true;
        }

        // Make sure that tearDownFixture
        try
        {
            method.invoke( testObject, args );

            report = new SimpleReportEntry( testObject.getClass().getName(), getTestName( userFriendlyMethodName ) );

            reportManager.testSucceeded( report );
        }
        catch ( InvocationTargetException ite )
        {
            Throwable t = ite.getTargetException();

            report =
                SimpleReportEntry.withException( testObject.getClass().getName(), getTestName( userFriendlyMethodName ),
                                                 new LegacyPojoStackTraceWriter( testObject.getClass().getName(),
                                                                                 method.getName(), t ) );

            reportManager.testFailed( report );
            // Don't return  here, because tearDownFixture should be called even
            // if the test method throws an exception.
        }
        catch ( Throwable t )
        {
            report =
                SimpleReportEntry.withException( testObject.getClass().getName(), getTestName( userFriendlyMethodName ),
                                                 new LegacyPojoStackTraceWriter( testObject.getClass().getName(),
                                                                                 method.getName(), t ) );

            reportManager.testFailed( report );
            // Don't return  here, because tearDownFixture should be called even
            // if the test method throws an exception.
        }

        try
        {
            tearDownFixture();
        }
        catch ( Throwable t )
        {
            // Treat any exception from tearDownFixture as a failure of the test.
            report =
                SimpleReportEntry.withException( testObject.getClass().getName(), getTestName( userFriendlyMethodName ),
                                                 new LegacyPojoStackTraceWriter( testObject.getClass().getName(),
                                                                                 method.getName(), t ) );

            reportManager.testFailed( report );

            // A return value of true indicates to this class's executeTestMethods
            // method that it should abort and not attempt to execute
            // any other test methods. The other caller of this method,
            // TestRerunner.rerun, ignores this return value, because it is
            // only running one test.
            return true;
        }

        // A return value of false indicates to this class's executeTestMethods
        // method that it should keep plowing ahead and invoke more test methods.
        // The other caller of this method,
        // TestRerunner.rerun, ignores this return value, because it is
        // only running one test.
        return false;
    }

    private String getTestName( String testMethodName )
    {
        if ( testMethodName == null )
        {
            throw new NullPointerException( "testMethodName is null" );
        }

        return getTestClass().getName() + "." + testMethodName;
    }

    private void setUpFixture()
        throws Throwable
    {
        if ( setUpMethod != null )
        {
            setUpMethod.invoke( testObject );
        }
    }

    private void tearDownFixture()
        throws Throwable
    {
        if ( tearDownMethod != null )
        {
            tearDownMethod.invoke( testObject );
        }
    }

    private void discoverTestMethods()
    {
        if ( testMethods == null )
        {
            testMethods = new ArrayList<Method>();

            Method[] methods = getTestClass().getMethods();

            for ( Method m : methods )
            {
                if ( isValidTestMethod( m ) )
                {
                    String simpleName = m.getName();

                    // name must have 5 or more chars
                    if ( simpleName.length() > 4 )
                    {
                        String firstFour = simpleName.substring( 0, 4 );

                        // name must start with "test"
                        if ( firstFour.equals( TEST_METHOD_PREFIX ) )
                        {
                            testMethods.add( m );
                        }
                    }
                }
                else if ( m.getName().equals( "setUp" ) && m.getParameterTypes().length == 0 )
                {
                    setUpMethod = m;
                }
                else if ( m.getName().equals( "tearDown" ) && m.getParameterTypes().length == 0 )
                {
                    tearDownMethod = m;
                }
            }
        }
    }

    private static boolean isValidTestMethod( Method m )
    {
        boolean isInstanceMethod = !Modifier.isStatic( m.getModifiers() );

        boolean returnsVoid = m.getReturnType().equals( void.class );

        boolean hasNoParams = m.getParameterTypes().length == 0;

        return isInstanceMethod && returnsVoid && hasNoParams;
    }

    public String getName()
    {
        return testClass.getName();
    }

    public Class getTestClass()
    {
        return testClass;
    }
}
