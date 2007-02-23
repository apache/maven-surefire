package org.apache.maven.surefire.testng;

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
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.suite.AbstractDirectoryTestSuite;
import org.apache.maven.surefire.testset.SurefireTestSet;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * Test suite for TestNG based on a directory of Java test classes. Can also execute JUnit tests.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class TestNGDirectoryTestSuite
    extends AbstractDirectoryTestSuite
{
    private String groups;

    private String excludedGroups;

    private boolean parallel;

    private int threadCount;

    private String testSourceDirectory;

    public TestNGDirectoryTestSuite( File basedir, ArrayList includes, ArrayList excludes, String groups,
                                     String excludedGroups, Boolean parallel, Integer threadCount,
                                     String testSourceDirectory )
    {
        super( basedir, includes, excludes );

        this.groups = groups;

        this.excludedGroups = excludedGroups;

        this.parallel = parallel.booleanValue();

        this.threadCount = threadCount.intValue();

        this.testSourceDirectory = testSourceDirectory;

    }

    public Map locateTestSets( ClassLoader classLoader )
        throws TestSetFailedException
    {
        // TODO: fix
        // override classloader. That keeps us all together for now, which makes it work, but could pose problems of
        // classloader separation if the tests use plexus-utils.
        return super.locateTestSets( classLoader );
    }

    protected SurefireTestSet createTestSet( Class testClass, ClassLoader classLoader )
    {
        return new TestNGTestSet( testClass );
    }

    public void execute( String testSetName, ReporterManager reporterManager, ClassLoader classLoader )
        throws ReporterException, TestSetFailedException
    {
        if ( testSets == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling execute" );
        }
        SurefireTestSet testSet = (SurefireTestSet) testSets.get( testSetName );

        if ( testSet == null )
        {
            throw new TestSetFailedException( "Unable to find test set '" + testSetName + "' in suite" );
        }

        XmlSuite suite = new XmlSuite();
        suite.setParallel( parallel );
        suite.setThreadCount( threadCount );

        createXmlTest( suite, testSet );

        executeTestNG( suite, reporterManager, classLoader );
    }

    public void execute( ReporterManager reporterManager, ClassLoader classLoader )
        throws ReporterException, TestSetFailedException
    {
        if ( testSets == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling execute" );
        }

        XmlSuite suite = new XmlSuite();
        suite.setParallel( parallel );
        suite.setThreadCount( threadCount );

        for ( Iterator i = testSets.values().iterator(); i.hasNext(); )
        {
            SurefireTestSet testSet = (SurefireTestSet) i.next();

            createXmlTest( suite, testSet );
        }

        executeTestNG( suite, reporterManager, classLoader );
    }

    private void createXmlTest( XmlSuite suite, SurefireTestSet testSet )
    {
        XmlTest xmlTest = new XmlTest( suite );
        xmlTest.setName( testSet.getName() );
        xmlTest.setXmlClasses( Collections.singletonList( new XmlClass( testSet.getTestClass() ) ) );

        if ( groups != null )
        {
            xmlTest.setIncludedGroups( Arrays.asList( groups.split( "," ) ) );
        }
        if ( excludedGroups != null )
        {
            xmlTest.setExcludedGroups( Arrays.asList( excludedGroups.split( "," ) ) );
        }

        // if ( !TestNGClassFinder.isTestNGClass( testSet.getTestClass(), annotationFinder ) )
        // TODO: this is a bit dodgy, but isTestNGClass wasn't working
        try
        {
            Class junitClass = Class.forName( "junit.framework.Test" );
            Class junitBase = Class.forName( "junit.framework.TestCase" );

            if ( junitClass.isAssignableFrom( testSet.getTestClass() ) ||
                junitBase.isAssignableFrom( testSet.getTestClass() ) )
            {
                xmlTest.setJUnit( true );
            }

        }
        catch ( ClassNotFoundException e )
        {
        }
    }

    private void executeTestNG( XmlSuite suite, ReporterManager reporterManager, ClassLoader classLoader )
    {
        TestNG testNG = new TestNG( false );

        // turn off all TestNG output
        testNG.setVerbose( 0 );

        testNG.setXmlSuites( Collections.singletonList( suite ) );

        testNG.setListenerClasses( new ArrayList() );

        TestNGReporter reporter = new TestNGReporter( reporterManager, this );
        testNG.addListener( (ITestListener) reporter );
        testNG.addListener( (ISuiteListener) reporter );

        // Set source path so testng can find javadoc annotations if not in 1.5 jvm
        if ( testSourceDirectory != null )
        {
            testNG.setSourcePath( testSourceDirectory );
        }

        // workaround for SUREFIRE-49
        // TestNG always creates an output directory, and if not set the name for the directory is "null"
        testNG.setOutputDirectory( System.getProperty( "java.io.tmpdir" ) );

        testNG.runSuitesLocally();
    }
}
