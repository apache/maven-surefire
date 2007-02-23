package org.apache.maven.surefire;

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

import org.apache.maven.surefire.report.Reporter;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.suite.SurefireTestSuite;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * @author Jason van Zyl
 * @version $Id$
 */
public class Surefire
{
    private ResourceBundle bundle = ResourceBundle.getBundle( SUREFIRE_BUNDLE_NAME );

    public static final String SUREFIRE_BUNDLE_NAME = "org.apache.maven.surefire.surefire";

    public boolean run( List reportDefinitions, Object[] testSuiteDefinition, String testSetName,
                        ClassLoader surefireClassLoader, ClassLoader testsClassLoader )
        throws ReporterException, TestSetFailedException
    {
        return run( reportDefinitions, testSuiteDefinition, testSetName, surefireClassLoader, testsClassLoader, null );
    }

    public boolean run( List reportDefinitions, Object[] testSuiteDefinition, String testSetName,
                        ClassLoader surefireClassLoader, ClassLoader testsClassLoader, Properties results )
        throws ReporterException, TestSetFailedException
    {
        ReporterManager reporterManager =
            new ReporterManager( instantiateReports( reportDefinitions, surefireClassLoader ) );

        if ( results != null )
        {
            reporterManager.initResultsFromProperties( results );
        }

        int totalTests = 0;

        SurefireTestSuite suite =
            createSuiteFromDefinition( testSuiteDefinition, surefireClassLoader, testsClassLoader );

        int testCount = suite.getNumTests();
        if ( testCount > 0 )
        {
            totalTests += testCount;
        }

        reporterManager.runStarting( totalTests );

        if ( totalTests == 0 )
        {
            reporterManager.writeMessage( "There are no tests to run." );
        }
        else
        {
            suite.execute( testSetName, reporterManager, testsClassLoader );
        }

        reporterManager.runCompleted();

        if ( results != null )
        {
            reporterManager.updateResultsProperties( results );
        }

        return reporterManager.getNumErrors() == 0 && reporterManager.getNumFailures() == 0;
    }

    public boolean run( List reportDefinitions, List testSuiteDefinitions, ClassLoader surefireClassLoader,
                        ClassLoader testsClassLoader )
        throws ReporterException, TestSetFailedException
    {
        ReporterManager reporterManager =
            new ReporterManager( instantiateReports( reportDefinitions, surefireClassLoader ) );

        List suites = new ArrayList();

        int totalTests = 0;
        for ( Iterator i = testSuiteDefinitions.iterator(); i.hasNext(); )
        {
            Object[] definition = (Object[]) i.next();

            SurefireTestSuite suite = createSuiteFromDefinition( definition, surefireClassLoader, testsClassLoader );

            int testCount = suite.getNumTests();
            if ( testCount > 0 )
            {
                suites.add( suite );
                totalTests += testCount;
            }
        }

        reporterManager.runStarting( totalTests );

        if ( totalTests == 0 )
        {
            reporterManager.writeMessage( "There are no tests to run." );
        }
        else
        {
            for ( Iterator i = suites.iterator(); i.hasNext(); )
            {
                SurefireTestSuite suite = (SurefireTestSuite) i.next();
                suite.execute( reporterManager, testsClassLoader );
            }
        }

        reporterManager.runCompleted();

        return reporterManager.getNumErrors() == 0 && reporterManager.getNumFailures() == 0;
    }

    private SurefireTestSuite createSuiteFromDefinition( Object[] definition, ClassLoader surefireClassLoader,
                                                         ClassLoader testsClassLoader )
        throws TestSetFailedException
    {
        String suiteClass = (String) definition[0];
        Object[] params = (Object[]) definition[1];

        SurefireTestSuite suite = instantiateSuite( suiteClass, params, surefireClassLoader );

        suite.locateTestSets( testsClassLoader );

        return suite;
    }

    private List instantiateReports( List reportDefinitions, ClassLoader classLoader )
        throws TestSetFailedException
    {
        List reports = new ArrayList();

        for ( Iterator i = reportDefinitions.iterator(); i.hasNext(); )
        {
            Object[] definition = (Object[]) i.next();

            String className = (String) definition[0];
            Object[] params = (Object[]) definition[1];

            Reporter report = instantiateReport( className, params, classLoader );

            reports.add( report );
        }

        return reports;
    }

    private static Reporter instantiateReport( String className, Object[] params, ClassLoader classLoader )
        throws TestSetFailedException
    {
        try
        {
            return (Reporter) instantiateObject( className, params, classLoader );
        }
        catch ( ClassNotFoundException e )
        {
            throw new TestSetFailedException( "Unable to find class to create report '" + className + "'", e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new TestSetFailedException(
                "Unable to find appropriate constructor to create report: " + e.getMessage(), e );
        }
    }

    public static Object instantiateObject( String className, Object[] params, ClassLoader classLoader )
        throws TestSetFailedException, ClassNotFoundException, NoSuchMethodException
    {
        Class clazz = classLoader.loadClass( className );

        Object object;
        try
        {
            if ( params != null )
            {
                Class[] paramTypes = new Class[params.length];

                for ( int j = 0; j < params.length; j++ )
                {
                    if ( params[j] == null )
                    {
                        paramTypes[j] = String.class;
                    }
                    else
                    {
                        paramTypes[j] = params[j].getClass();
                    }
                }

                Constructor constructor = clazz.getConstructor( paramTypes );

                object = constructor.newInstance( params );
            }
            else
            {
                object = clazz.newInstance();
            }
        }
        catch ( IllegalAccessException e )
        {
            throw new TestSetFailedException( "Unable to instantiate object: " + e.getMessage(), e );
        }
        catch ( InvocationTargetException e )
        {
            throw new TestSetFailedException( e.getTargetException().getMessage(), e.getTargetException() );
        }
        catch ( InstantiationException e )
        {
            throw new TestSetFailedException( "Unable to instantiate object: " + e.getMessage(), e );
        }
        return object;
    }

    private static SurefireTestSuite instantiateSuite( String suiteClass, Object[] params, ClassLoader classLoader )
        throws TestSetFailedException
    {
        try
        {
            return (SurefireTestSuite) instantiateObject( suiteClass, params, classLoader );
        }
        catch ( ClassNotFoundException e )
        {
            throw new TestSetFailedException( "Unable to find class to create suite '" + suiteClass + "'", e );
        }
        catch ( NoSuchMethodException e )
        {
            throw new TestSetFailedException(
                "Unable to find appropriate constructor to create suite: " + e.getMessage(), e );
        }
    }

    public String getResourceString( String key )
    {
        return bundle.getString( key );
    }
}
