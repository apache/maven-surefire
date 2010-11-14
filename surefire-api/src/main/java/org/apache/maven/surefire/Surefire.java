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

import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.report.RunStatistics;
import org.apache.maven.surefire.suite.SuiteDefinition;
import org.apache.maven.surefire.suite.SurefireTestSuite;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * @author Jason van Zyl
 * @version $Id$
 */
public class Surefire
{

    private static final int SUCCESS = 0;

    private static final int NO_TESTS = 254;

    private static final int FAILURE = 255;

    public static final String SUREFIRE_BUNDLE_NAME = "org.apache.maven.surefire.surefire";

    // DGF backwards compatibility
    public boolean run( List reportDefinitions, Object[] testSuiteDefinition, String testSetName,
                        ClassLoader surefireClassLoader, ClassLoader testsClassLoader )
        throws ReporterException, TestSetFailedException
    {
        return run( reportDefinitions, testSuiteDefinition, testSetName, surefireClassLoader, testsClassLoader, null,
                    Boolean.FALSE ) == 0;
    }

    public int run( List reportDefinitions, Object[] testSuiteDefinition, String testSetName,
                    ClassLoader surefireClassLoader, ClassLoader testsClassLoader, Boolean failIfNoTests )
        throws ReporterException, TestSetFailedException
    {
        return run( reportDefinitions, testSuiteDefinition, testSetName, surefireClassLoader, testsClassLoader, null,
                    failIfNoTests );
    }

    // DGF backwards compatibility
    public boolean run( List reportDefinitions, Object[] testSuiteDefinition, String testSetName,
                        ClassLoader surefireClassLoader, ClassLoader testsClassLoader, Properties results )
        throws ReporterException, TestSetFailedException
    {
        return run( reportDefinitions, testSuiteDefinition, testSetName, surefireClassLoader, testsClassLoader, results,
                    Boolean.FALSE ) == 0;
    }

    public int run( List reportDefinitions, Object[] testSuiteDefinition, String testSetName,
                    ClassLoader surefireClassLoader, ClassLoader testsClassLoader, Properties results,
                    Boolean failIfNoTests )
        throws ReporterException, TestSetFailedException
    {
        ReporterManagerFactory reporterManagerFactory =
            new ReporterManagerFactory( reportDefinitions, surefireClassLoader );

        SuiteDefinition suiteDefinition =
            SuiteDefinition.fromBooterFormat( Collections.singletonList( testSuiteDefinition ) );
        RunStatistics runStatistics = reporterManagerFactory.getGlobalRunStatistics();
        if ( results != null )
        {
            runStatistics.initResultsFromProperties( results );
        }

        int totalTests = 0;

        SurefireTestSuite suite = createSuiteFromDefinition( suiteDefinition, surefireClassLoader, testsClassLoader );

        int testCount = suite.getNumTests();
        if ( testCount > 0 )
        {
            totalTests += testCount;
        }

        if ( totalTests == 0 )
        {
            reporterManagerFactory.createReporterManager().writeMessage( "There are no tests to run." );
        }
        else
        {
            suite.execute( testSetName, reporterManagerFactory, testsClassLoader );
        }

        reporterManagerFactory.close();

        if ( results != null )
        {
            runStatistics.updateResultsProperties( results );
        }

        if ( failIfNoTests.booleanValue() )
        {
            if ( runStatistics.getCompletedCount() == 0 )
            {
                return NO_TESTS;
            }
        }

        return runStatistics.isProblemFree() ? SUCCESS : FAILURE;

    }

    public boolean run( List reportDefinitions, List testSuiteDefinitions, ClassLoader surefireClassLoader,
                        ClassLoader testsClassLoader )
        throws ReporterException, TestSetFailedException
    {
        return run( reportDefinitions, testSuiteDefinitions, surefireClassLoader, testsClassLoader, Boolean.FALSE ) ==
            0;
    }

    public int run( List reportDefinitions, List testSuiteDefinitions, ClassLoader surefireClassLoader,
                    ClassLoader testsClassLoader, Boolean failIfNoTests )
        throws ReporterException, TestSetFailedException
    {
        ReporterManagerFactory reporterManagerFactory =
            new ReporterManagerFactory( reportDefinitions, surefireClassLoader );

        RunStatistics runStatistics = reporterManagerFactory.getGlobalRunStatistics();

        int totalTests = 0;
        SuiteDefinition definition = SuiteDefinition.fromBooterFormat( testSuiteDefinitions );
        SurefireTestSuite suite = createSuiteFromDefinition( definition, surefireClassLoader, testsClassLoader );

        int testCount = suite.getNumTests();
        if ( testCount > 0 )
        {
            totalTests += testCount;
        }

        if ( totalTests == 0 )
        {
            reporterManagerFactory.createReporterManager().writeMessage( "There are no tests to run." );
        }
        else
        {
            suite.execute( reporterManagerFactory, testsClassLoader );
        }

        reporterManagerFactory.close();
        if ( failIfNoTests.booleanValue() )
        {
            if ( runStatistics.getCompletedCount() == 0 )
            {
                return NO_TESTS;
            }
        }

        return runStatistics.isProblemFree() ? SUCCESS : FAILURE;
    }

    private SurefireTestSuite createSuiteFromDefinition( SuiteDefinition definition, ClassLoader surefireClassLoader,
                                                         ClassLoader testsClassLoader )
        throws TestSetFailedException
    {
        SurefireTestSuite suite = definition.newInstance( surefireClassLoader );

        suite.locateTestSets( testsClassLoader );

        // Todo: Make this not ugly OR just leave it here as a transitional feature for a few versions
        // I will move this into the JUnit4DirectoryTestSuite when fixing SUREFIRE-141 RSN
        // SUREFIRE-141 will require loosening the relationship between surefire and the providers,
        // which is basically way too constrained and locked into a design that is only correct for
        // junit3 with the AbstractDirectoryTestSuite.
        // This same constraint is making it hard to put this code in the proper place.
        if ( isJunit4UpgradeCheck() && suite.getClassesSkippedByValidation().size() > 0 &&
            suite.getClass().getName().equals( "org.apache.maven.surefire.junit4.JUnit4DirectoryTestSuite" ) )
        {

            StringBuilder reason = new StringBuilder();
            reason.append( "Updated check failed\n" );
            reason.append( "There are tests that would be run with junit4 / surefire 2.6 but not with [2.7,):\n" );
            for ( Iterator i = suite.getClassesSkippedByValidation().iterator(); i.hasNext(); )
            {
                Class testClass = (Class) i.next();
                reason.append( "   " );
                reason.append( testClass.getCanonicalName() );
                reason.append( "\n" );
            }
            throw new TestSetFailedException( reason.toString() );
        }

        return suite;
    }

    private boolean isJunit4UpgradeCheck()
    {
        final String property = System.getProperty( "surefire.junit4.upgradecheck" );
        return property != null;
    }
}
