package org.apache.maven.surefire.battery;

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

import org.apache.maven.surefire.battery.assertion.BatteryAssert;
import org.apache.maven.surefire.report.ReporterManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Main plugin point for running testng tests within the Surefire runtime
 * infrastructure.
 *
 * @author jkuhnert
 */
public class TestNGBattery
    implements Battery
{
    protected List testMethods;

    protected List subBatteryClassNames;

    private Class testClass;

    /**
     * Creates a new test battery that will process the class being
     * passed in to determine the testing configuration.
     *
     * @param testClass
     * @param loader
     */
    public TestNGBattery( Class testClass, ClassLoader loader )
    {
        processTestClass( testClass, loader );
    }

    /**
     * Parses and configures this battery based on the test class
     * being passed in.
     *
     * @param testClass
     * @param loader
     */
    private void processTestClass( Class testClass, ClassLoader loader )
    {
        if ( testClass == null )
        {
            throw new NullPointerException( "testClass is null" );
        }

        if ( loader == null )
        {
            throw new NullPointerException( "classLoader is null" );
        }

        this.testClass = testClass;
    }

    public Class getTestClass()
    {
        return testClass;
    }

    protected void discoverTestMethods()
    {
        if ( testMethods == null )
        {
            testMethods = new ArrayList();

            Method[] methods = testClass.getMethods();

            for ( int i = 0; i < methods.length; ++i )
            {
                Method m = methods[i];

                // TODO: better location
                if ( BatteryAssert.isValidMethod( m ) )
                {
                    String simpleName = m.getName();

                    // TODO: WHY?
                    // name must have 5 or more chars
                    if ( simpleName.length() > 4 )
                    {
                        testMethods.add( m );
                    }
                }
            }
        }
    }

    public void discoverBatteryClassNames()
    {
    }

    public void execute( ReporterManager reportManager )
    {
    }

    public String getBatteryName()
    {
        return testClass.getName();
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

    public int getTestCount()
    {
        discoverTestMethods();

        return testMethods.size();
    }
}
