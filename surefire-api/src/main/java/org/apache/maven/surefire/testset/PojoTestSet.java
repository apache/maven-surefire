package org.apache.maven.surefire.testset;

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

import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.report.PojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class PojoTestSet
    extends AbstractTestSet
{
    private ResourceBundle bundle = ResourceBundle.getBundle( Surefire.SUREFIRE_BUNDLE_NAME );

    private static final String TEST_METHOD_PREFIX = "test";

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private Object testObject;

    protected List testMethods;

    public PojoTestSet( Class testClass )
        throws TestSetFailedException
    {
        super( testClass );

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

    public void execute( ReporterManager reportManager, ClassLoader loader )
        throws TestSetFailedException
    {
        if ( reportManager == null )
        {
            throw new NullPointerException( "reportManager is null" );
        }

        executeTestMethods( reportManager );
    }

    protected void executeTestMethods( ReporterManager reportManager )
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
            abort = executeTestMethod( (Method) testMethods.get( i ), EMPTY_OBJECT_ARRAY, reportManager );
        }
    }

    /**
     * @noinspection CatchGenericClass,OverlyBroadCatchBlock,MethodWithMultipleReturnPoints
     */
    protected boolean executeTestMethod( Method method, Object[] args, ReporterManager reportManager )
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

        ReportEntry report = new ReportEntry( testObject, getTestName( userFriendlyMethodName ), getName() );

        reportManager.testStarting( report );

        try
        {
            setUpFixture();
        }
        catch ( Exception e )
        {
            // Treat any exception from setUpFixture as a failure of the test.
            String rawString = bundle.getString( "setupFixtureFailed" );

            MessageFormat msgFmt = new MessageFormat( rawString );

            Object[] stringArgs = {method.getName()};

            String stringToPrint = msgFmt.format( stringArgs );

            report = new ReportEntry( testObject, getTestName( userFriendlyMethodName ), stringToPrint,
                                      new PojoStackTraceWriter( testObject.getClass().getName(), method.getName(),
                                                                e ) );

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

            report = new ReportEntry( testObject, getTestName( userFriendlyMethodName ), getName() );

            reportManager.testSucceeded( report );
        }
        catch ( InvocationTargetException ite )
        {
            Throwable t = ite.getTargetException();

            String msg = t.getMessage();

            if ( msg == null )
            {
                msg = t.toString();
            }

            report = new ReportEntry( testObject, getTestName( userFriendlyMethodName ), msg, new PojoStackTraceWriter(
                testObject.getClass().getName(), method.getName(), t ) );

            reportManager.testFailed( report );
            // Don't return  here, because tearDownFixture should be called even
            // if the test method throws an exception.
        }
        catch ( Throwable t )
        {
            String msg = t.getMessage();

            if ( msg == null )
            {
                msg = t.toString();
            }

            report = new ReportEntry( testObject, getTestName( userFriendlyMethodName ), msg, new PojoStackTraceWriter(
                testObject.getClass().getName(), method.getName(), t ) );

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
            String rawString = bundle.getString( "cleanupFixtureFailed" );

            MessageFormat msgFmt = new MessageFormat( rawString );

            Object[] stringArgs = {method.getName()};

            String stringToPrint = msgFmt.format( stringArgs );

            report = new ReportEntry( testObject, getTestName( userFriendlyMethodName ), stringToPrint,
                                      new PojoStackTraceWriter( testObject.getClass().getName(), method.getName(),
                                                                t ) );

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

    public String getTestName( String testMethodName )
    {
        if ( testMethodName == null )
        {
            throw new NullPointerException( "testMethodName is null" );
        }

        return getTestClass().getName() + "." + testMethodName;
    }

    public void setUpFixture()
    {
    }

    public void tearDownFixture()
    {
    }

    public int getTestCount()
        throws TestSetFailedException
    {
        discoverTestMethods();

        return testMethods.size();
    }

    private void discoverTestMethods()
    {
        if ( testMethods == null )
        {
            testMethods = new ArrayList();

            Method[] methods = getTestClass().getMethods();

            for ( int i = 0; i < methods.length; ++i )
            {
                Method m = methods[i];

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
}
