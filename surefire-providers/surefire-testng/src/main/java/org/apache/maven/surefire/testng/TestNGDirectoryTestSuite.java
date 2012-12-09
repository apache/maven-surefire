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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.surefire.NonAbstractClassFilter;
import org.apache.maven.surefire.report.ConsoleOutputCapture;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.LazyTestsToRun;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.TestsToRun;

/**
 * Test suite for TestNG based on a directory of Java test classes. Can also execute JUnit tests.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class TestNGDirectoryTestSuite
    implements TestNgTestSuite
{
    private final ArtifactVersion version;

    private final Map options;

    private final Map junitOptions;

    private final String testSourceDirectory;

    private final File reportsDirectory;

    private SortedMap testSets;

    private final ScanResult scanResult;

    private final String testMethodPattern;

    private final RunOrderCalculator runOrderCalculator;

    private final Class junitTestClass;

    public TestNGDirectoryTestSuite( String testSourceDirectory, String artifactVersion, Properties confOptions,
                                     File reportsDirectory, String testMethodPattern,
                                     RunOrderCalculator runOrderCalculator, ScanResult scanResult )
    {

        this.runOrderCalculator = runOrderCalculator;

        this.options = confOptions;

        this.testSourceDirectory = testSourceDirectory;
        this.reportsDirectory = reportsDirectory;
        this.scanResult = scanResult;
        this.version = new DefaultArtifactVersion( artifactVersion );
        this.testMethodPattern = testMethodPattern;
        this.junitTestClass = findJUnitTestClass();
        this.junitOptions = createJUnitOptions();
    }

    public void execute( TestsToRun testsToRun, ReporterFactory reporterManagerFactory )
        throws ReporterException, TestSetFailedException
    {

        if ( testsToRun instanceof LazyTestsToRun )
        {
            executeLazy( testsToRun, reporterManagerFactory );
        }
        else if ( containsAtLeast( testsToRun, 2 ) )
        {
            executeMulti( testsToRun, reporterManagerFactory );
        }
        else if ( containsAtLeast( testsToRun, 1 ) )
        {
            Class testClass = (Class) testsToRun.iterator().next();
            executeSingleClass( reporterManagerFactory, testClass );
        }
    }
    
    private boolean containsAtLeast( TestsToRun testsToRun, int atLeast ) {
        Iterator it = testsToRun.iterator();
        for ( int i = 0; i < atLeast; i++ )
        {
            if ( !it.hasNext() )
            {
                return false;
            }

            it.next();
        }
        
        return true;
    }
    
    private void executeSingleClass( ReporterFactory reporterManagerFactory, Class testClass )
        throws TestSetFailedException
    {
        this.options.put( "suitename", testClass.getName() );

        RunListener reporter = reporterManagerFactory.createReporter();
        ConsoleOutputCapture.startCapture( (ConsoleOutputReceiver) reporter );

        startTestSuite( reporter, this );

        final Map optionsToUse = isJUnitTest( testClass ) ? junitOptions : options;

        TestNGExecutor.run( new Class[]{ testClass }, testSourceDirectory, optionsToUse, version, reporter, this,
                            reportsDirectory, testMethodPattern );

        finishTestSuite( reporter, this );
    }

    public void executeLazy( TestsToRun testsToRun, ReporterFactory reporterFactory )
        throws ReporterException, TestSetFailedException
    {

        for ( Iterator testClassIt = testsToRun.iterator(); testClassIt.hasNext(); )
        {
            Class c = (Class) testClassIt.next();
            executeSingleClass( reporterFactory, c );
        }
    }

    private Class findJUnitTestClass()
    {
        Class junitTest;
        try
        {
            junitTest = Class.forName( "junit.framework.Test" );
        }
        catch ( ClassNotFoundException e )
        {
            junitTest = null;
        }
        return junitTest;
    }

    public void executeMulti( TestsToRun testsToRun, ReporterFactory reporterFactory )
        throws ReporterException, TestSetFailedException
    {
        List testNgTestClasses = new ArrayList();
        List junitTestClasses = new ArrayList();
        for ( Iterator it = testsToRun.iterator(); it.hasNext(); )
        {
            Class c = (Class) it.next();
            if ( isJUnitTest( c ) )
            {
                junitTestClasses.add( c );
            }
            else
            {
                testNgTestClasses.add( c );
            }
        }

        File testNgReportsDirectory = reportsDirectory, junitReportsDirectory = reportsDirectory;

        if ( junitTestClasses.size() > 0 && testNgTestClasses.size() > 0 )
        {
            testNgReportsDirectory = new File( reportsDirectory, "testng-native-results" );
            junitReportsDirectory = new File( reportsDirectory, "testng-junit-results" );
        }

        RunListener reporterManager = reporterFactory.createReporter();
        ConsoleOutputCapture.startCapture( (ConsoleOutputReceiver) reporterManager );
        startTestSuite( reporterManager, this );

        Class[] testClasses = (Class[]) testNgTestClasses.toArray( new Class[testNgTestClasses.size()] );

        TestNGExecutor.run( testClasses, this.testSourceDirectory, options, version, reporterManager, this,
                            testNgReportsDirectory, testMethodPattern );

        if ( junitTestClasses.size() > 0 )
        {
            testClasses = (Class[]) junitTestClasses.toArray( new Class[junitTestClasses.size()] );

            TestNGExecutor.run( testClasses, testSourceDirectory, junitOptions, version, reporterManager, this,
                                junitReportsDirectory, testMethodPattern );
        }

        finishTestSuite( reporterManager, this );
    }

    private boolean isJUnitTest( Class c )
    {
        return junitTestClass != null && junitTestClass.isAssignableFrom( c );
    }

    private Map createJUnitOptions()
    {
        Map junitOptions = new HashMap( this.options );
        junitOptions.put( "junit", Boolean.TRUE );
        return junitOptions;
    }

    // single class test
    public void execute( String testSetName, ReporterFactory reporterManagerFactory )
        throws ReporterException, TestSetFailedException
    {
        if ( testSets == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling execute" );
        }
        TestNGTestSet testSet = (TestNGTestSet) testSets.get( testSetName );

        if ( testSet == null )
        {
            throw new TestSetFailedException( "Unable to find test set '" + testSetName + "' in suite" );
        }

        RunListener reporter = reporterManagerFactory.createReporter();
        ConsoleOutputCapture.startCapture( (ConsoleOutputReceiver) reporter );

        startTestSuite( reporter, this );

        TestNGExecutor.run( new Class[]{ testSet.getTestClass() }, this.testSourceDirectory, this.options, this.version,
                            reporter, this, reportsDirectory, testMethodPattern );

        finishTestSuite( reporter, this );
    }

    public static void startTestSuite( RunListener reporter, Object suite )
    {
        ReportEntry report = new SimpleReportEntry( suite.getClass().getName(), getSuiteName( suite ) );

        try
        {
            reporter.testSetStarting( report );
        }
        catch ( ReporterException e )
        {
            // TODO: remove this exception from the report manager
        }
    }

    public static void finishTestSuite( RunListener reporterManager, Object suite )
        throws ReporterException
    {
        ReportEntry report = new SimpleReportEntry( suite.getClass().getName(), getSuiteName( suite ) );

        reporterManager.testSetCompleted( report );
    }

    public String getSuiteName()
    {
        String result = (String) options.get( "suitename" );
        if ( result == null )
        {
            result = "TestSuite";
        }
        return result;
    }

    private static String getSuiteName( Object suite )
    {
        String result;
        if ( suite instanceof TestNGDirectoryTestSuite )
        {
            return ( (TestNGDirectoryTestSuite) suite ).getSuiteName();
        }
        else if ( suite instanceof TestNGXmlTestSuite )
        {
            return ( (TestNGXmlTestSuite) suite ).getSuiteName();
        }
        else
        {
            result = "TestSuite";
        }

        return result;
    }

    public Map locateTestSets( ClassLoader classLoader )
        throws TestSetFailedException
    {
        if ( testSets != null )
        {
            throw new IllegalStateException( "You can't call locateTestSets twice" );
        }
        testSets = new TreeMap();

        final TestsToRun scanned = scanResult.applyFilter( new NonAbstractClassFilter(), classLoader );

        final TestsToRun testsToRun = runOrderCalculator.orderTestClasses( scanned );

        for ( Iterator it = testsToRun.iterator(); it.hasNext(); )
        {
            Class testClass = (Class) it.next();
            TestNGTestSet testSet = new TestNGTestSet( testClass );

            if ( testSets.containsKey( testSet.getName() ) )
            {
                throw new TestSetFailedException( "Duplicate test set '" + testSet.getName() + "'" );
            }
            testSets.put( testSet.getName(), testSet );

        }

        return Collections.unmodifiableSortedMap( testSets );
    }

}
