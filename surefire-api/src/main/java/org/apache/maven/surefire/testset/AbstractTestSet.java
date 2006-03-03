package org.apache.maven.surefire.testset;

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

import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * @todo trim it up. Those implementing don't need any of this except for discoverTestMethods, getName and isValidMethod
 * @todo bring back other helpers and put in a separate package
 */
public abstract class AbstractTestSet
    implements SurefireTestSet
{
    private static final String TEST_METHOD_PREFIX = "test";

    protected List testMethods;

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

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

        ReportEntry report = new ReportEntry( this, getTestName( userFriendlyMethodName ), getClass().getName() );

        reportManager.testStarting( report );

        try
        {
            setUpFixture();
        }
        catch ( Exception e )
        {
/* TODO
            // Treat any exception from setUpFixture as a failure of the test.
            String rawString = Surefire.getResourceString( "setupFixtureFailed" );

            MessageFormat msgFmt = new MessageFormat( rawString );

            Object[] stringArgs = {method.getName()};

            String stringToPrint = msgFmt.format( stringArgs );

            report = new ReportEntry( this, getTestName( userFriendlyMethodName ), stringToPrint, e );

            reportManager.testFailed( report );
*/

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
            method.invoke( getTestClassInstance(), args );

            report = new ReportEntry( this, getTestName( userFriendlyMethodName ), this.getClass().getName() );

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

            report = new ReportEntry( this, getTestName( userFriendlyMethodName ), msg, t );

            reportManager.testFailed( report );
            // Don't return  here, because tearDownFixture should be called even
            // if the test method throws an exception.
        }
        catch ( Exception e )
        {
            String msg = e.getMessage();

            if ( msg == null )
            {
                msg = e.toString();
            }

            report = new ReportEntry( this, getTestName( userFriendlyMethodName ), msg, e );

            reportManager.testFailed( report );
            // Don't return  here, because tearDownFixture should be called even
            // if the test method throws an exception.
        }

        try
        {
            tearDownFixture();
        }
        catch ( Exception e )
        {

/* TODO
            // Treat any exception from tearDownFixture as a failure of the test.
            String rawString = Surefire.getResourceString( "cleanupFixtureFailed" );

            MessageFormat msgFmt = new MessageFormat( rawString );

            Object[] stringArgs = {method.getName()};

            String stringToPrint = msgFmt.format( stringArgs );

            report = new ReportEntry( this, getTestName( userFriendlyMethodName ), stringToPrint, e );

            reportManager.testFailed( report );
*/

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

    public String getName()
    {
        return getClass().getName();
    }

    public String getTestName( String testMethodName )
    {
        if ( testMethodName == null )
        {
            throw new NullPointerException( "testMethodName is null" );
        }

        return getClass() + "." + testMethodName;
    }

    public int getTestCount()
        throws TestSetFailedException
    {
        discoverTestMethods();

        return testMethods.size();
    }

    public void setUpFixture()
        throws Exception
    {
    }

    public void tearDownFixture()
        throws Exception
    {
    }

    protected Class getTestClass()
    {
        return getClass();
    }

    protected Object getTestClassInstance()
        throws IllegalAccessException, InstantiationException
    {
        return this;
    }

    protected void discoverTestMethods()
    {
        if ( testMethods == null )
        {
            testMethods = new ArrayList();

            Method[] methods = getTestClass().getMethods();

            for ( int i = 0; i < methods.length; ++i )
            {
                Method m = methods[i];

                if ( isValidMethod( m ) )
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

    // ----------------------------------------------------------------------
    // Batteries
    // ----------------------------------------------------------------------

    public static boolean isValidMethod( Method m )
    {
        boolean isInstanceMethod = !Modifier.isStatic( m.getModifiers() );

        boolean returnsVoid = m.getReturnType().equals( void.class );

        boolean hasNoParams = m.getParameterTypes().length == 0;

        return isInstanceMethod && returnsVoid && hasNoParams;
    }
}
