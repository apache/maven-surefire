package org.apache.maven.surefire.battery;

/*
 * Copyright 2001-2005 The Codehaus.
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

import org.apache.maven.surefire.battery.assertion.BatteryAssert;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.Surefire;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBattery
    extends BatteryAssert
    implements Battery
{
    private static final String TEST_METHOD_PREFIX = "test";

    private List testMethods;

    private List subBatteryClassNames;

    public void execute( ReporterManager reportManager )
        throws Exception
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

        Object[] args = new Object[0];

        boolean abort = false;

        for ( int i = 0; ( i < testMethods.size() ) && !abort; ++i )
        {
            abort = executeTestMethod( (Method) testMethods.get( i ), args, reportManager );
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
            // Treat any exception from setUpFixture as a failure of the test.
            String rawString = Surefire.getResources().getString( "setupFixtureFailed" );

            MessageFormat msgFmt = new MessageFormat( rawString );

            Object[] stringArgs = {method.getName()};

            String stringToPrint = msgFmt.format( stringArgs );

            report = new ReportEntry( this, getTestName( userFriendlyMethodName ), stringToPrint, e );

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

            // Treat any exception from tearDownFixture as a failure of the test.
            String rawString = Surefire.getResources().getString( "cleanupFixtureFailed" );

            MessageFormat msgFmt = new MessageFormat( rawString );

            Object[] stringArgs = {method.getName()};

            String stringToPrint = msgFmt.format( stringArgs );

            String msg = e.getMessage();
            if ( msg == null )
            {
                msg = e.toString();
            }

            report = new ReportEntry( this, getTestName( userFriendlyMethodName ), stringToPrint, e );

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

    public String getBatteryName()
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
    {
        return this;
    }

    protected void discoverTestMethods()
    {
        if ( testMethods != null )
        {
            return;
        }

        testMethods = new ArrayList();

        Method[] methods = getTestClass().getMethods();

        for ( int i = 0; i < methods.length; ++i )
        {
            Method m = methods[i];

            Class[] paramTypes = m.getParameterTypes();

            boolean isInstanceMethod = !Modifier.isStatic( m.getModifiers() );

            boolean returnsVoid = m.getReturnType() == void.class;

            boolean hasNoParams = paramTypes.length == 0;

            if ( isInstanceMethod && returnsVoid && hasNoParams )
            {
                String simpleName = m.getName();

                if ( simpleName.length() <= 4 )
                {
                    // name must have 5 or more chars
                    continue;
                }

                String firstFour = simpleName.substring( 0, 4 );

                if ( !firstFour.equals( TEST_METHOD_PREFIX ) )
                {
                    // name must start with "test"
                    continue;
                }

                testMethods.add( m );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Batteries
    // ----------------------------------------------------------------------

    public void discoverBatteryClassNames()
        throws Exception
    {
    }

    public void addSubBatteryClassName( String batteryClassName )
    {
        getSubBatteryClassNames().add( batteryClassName );
    }

    public List getSubBatteryClassNames()
    {
        if ( subBatteryClassNames == null )
        {
            subBatteryClassNames = new ArrayList();
        }

        return subBatteryClassNames;
    }
}
