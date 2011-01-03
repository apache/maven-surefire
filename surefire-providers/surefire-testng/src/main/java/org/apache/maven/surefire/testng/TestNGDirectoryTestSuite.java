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

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.surefire.NonAbstractClassFilter;
import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.report.DefaultReportEntry;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.DefaultDirectoryScanner;
import org.apache.maven.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.util.TestsToRun;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Test suite for TestNG based on a directory of Java test classes. Can also execute JUnit tests.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class TestNGDirectoryTestSuite
    implements TestNgTestSuite
{
    protected static ResourceBundle bundle = ResourceBundle.getBundle( Surefire.SUREFIRE_BUNDLE_NAME );

    private ArtifactVersion version;

    private Map options;

    private String testSourceDirectory;

    private File reportsDirectory;

    protected SortedMap testSets;

    private final DirectoryScanner surefireDirectoryScanner;

    public TestNGDirectoryTestSuite( File basedir, ArrayList includes, ArrayList excludes, String testSourceDirectory,
                                     String artifactVersion, Properties confOptions, File reportsDirectory )
    {
        this( basedir, includes, excludes, testSourceDirectory, new DefaultArtifactVersion( artifactVersion ),
              confOptions, reportsDirectory );
    }

    public TestNGDirectoryTestSuite( File basedir, List includes, List excludes, String testSourceDirectory,
                                     ArtifactVersion artifactVersion, Map confOptions, File reportsDirectory )
    {
        this.surefireDirectoryScanner = new DefaultDirectoryScanner( basedir, includes, excludes,
                                                                     "filesystem" );

        this.options = confOptions;

        this.testSourceDirectory = testSourceDirectory;
        this.reportsDirectory = reportsDirectory;
        this.version = artifactVersion;

    }

    public void execute( TestsToRun testsToRun, ReporterFactory reporterManagerFactory )
        throws ReporterException, TestSetFailedException
    {

        if ( testsToRun.size() == 0 )
        {
            return;
        }

        if ( testsToRun.size() > 1 )
        {
            executeMulti( testsToRun, reporterManagerFactory );
            return;
        }

        ReporterManager reporterManager = (ReporterManager) reporterManagerFactory.createReporter();
        startTestSuite( reporterManager, this );

        TestNGExecutor.run( new Class[]{ (Class) testsToRun.iterator().next() }, this.testSourceDirectory, this.options,
                            this.version, reporterManager, this, reportsDirectory );

        finishTestSuite( reporterManager, this );
    }

    public void executeMulti( TestsToRun testsToRun, ReporterFactory reporterManagerFactory )
        throws ReporterException, TestSetFailedException
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

        List testNgTestClasses = new ArrayList();
        List junitTestClasses = new ArrayList();
        for ( Iterator it = testsToRun.iterator(); it.hasNext(); )
        {
            Class c = (Class) it.next();
            if ( junitTest != null && junitTest.isAssignableFrom( c ) )
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

        ReporterManager reporterManager =
            new SynchronizedReporterManager( (ReporterManager) reporterManagerFactory.createReporter() );
        startTestSuite( reporterManager, this );

        Class[] testClasses = (Class[]) testNgTestClasses.toArray( new Class[testNgTestClasses.size()] );

        TestNGExecutor.run( testClasses, this.testSourceDirectory, this.options, this.version, reporterManager, this, testNgReportsDirectory );

        if ( junitTestClasses.size() > 0 )
        {
            testClasses = (Class[]) junitTestClasses.toArray( new Class[junitTestClasses.size()] );

            Map junitOptions = new HashMap();
            for ( Iterator it = this.options.keySet().iterator(); it.hasNext(); )
            {
                Object key = it.next();
                junitOptions.put( key, options.get( key ) );
            }

            junitOptions.put( "junit", Boolean.TRUE );

            TestNGExecutor.run( testClasses, this.testSourceDirectory, junitOptions, this.version, reporterManager, this, junitReportsDirectory );
        }

        finishTestSuite( reporterManager, this );
    }

    // single class test
    public void execute( String testSetName, ReporterManagerFactory reporterManagerFactory, ClassLoader classLoader )
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

        ReporterManager reporterManager = reporterManagerFactory.createReporterManager();
        startTestSuite( reporterManager, this );

        TestNGExecutor.run( new Class[]{ testSet.getTestClass() }, this.testSourceDirectory, this.options, this.version,
                            reporterManager, this, reportsDirectory );

        finishTestSuite( reporterManager, this );
    }

    public static void startTestSuite( ReporterManager reporterManager, Object suite )
    {
        String rawString = bundle.getString( "testSetStarting" );

        ReportEntry report = new DefaultReportEntry( suite.getClass().getName(), getSuiteName( suite ), rawString );

        try
        {
            reporterManager.testSetStarting( report );
        }
        catch ( ReporterException e )
        {
            // TODO: remove this exception from the report manager
        }
    }

    public static void finishTestSuite( ReporterManager reporterManager, Object suite )
    {
        String rawString = bundle.getString( "testSetCompletedNormally" );

        ReportEntry report = new DefaultReportEntry( suite.getClass().getName(), getSuiteName( suite ), rawString );

        reporterManager.testSetCompleted( report );

        reporterManager.reset();
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

        final TestsToRun testsToRun =
            surefireDirectoryScanner.locateTestClasses( classLoader, new NonAbstractClassFilter() );
        Class[] locatedClasses = testsToRun.getLocatedClasses();

        for ( int i = 0; i < locatedClasses.length; i++ )
        {
            Class testClass = locatedClasses[i];
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
