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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.suite.AbstractDirectoryTestSuite;
import org.apache.maven.surefire.testset.SurefireTestSet;
import org.apache.maven.surefire.testset.TestSetFailedException;

/**
 * Test suite for TestNG based on a directory of Java test classes. Can also execute JUnit tests.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href='mailto:the[dot]mindstorm[at]gmail[dot]com'>Alex Popescu</a>
 */
public class TestNGDirectoryTestSuite
    extends AbstractDirectoryTestSuite
{
    private ArtifactVersion version;

    private String classifier;
    
    private Map options;

    private String testSourceDirectory;
    
    private File reportsDirectory;

    public TestNGDirectoryTestSuite( File basedir, ArrayList includes, ArrayList excludes, String testSourceDirectory,
                                     String artifactVersion, String artifactClassifier, Properties confOptions, File reportsDirectory )
    {
        this( basedir, includes, excludes, testSourceDirectory, new DefaultArtifactVersion( artifactVersion ),
              artifactClassifier, confOptions, reportsDirectory );
    }

    public TestNGDirectoryTestSuite( File basedir, List includes, List excludes, String testSourceDirectory,
                                     ArtifactVersion artifactVersion, String artifactClassifier, Map confOptions, File reportsDirectory )
    {
        super( basedir, includes, excludes );

        this.options = confOptions;
        
        this.testSourceDirectory = testSourceDirectory;
        this.reportsDirectory = reportsDirectory;
        this.version = artifactVersion;
        
        this.classifier = artifactClassifier;

    }

    protected SurefireTestSet createTestSet( Class testClass, ClassLoader classLoader )
    {
        return new TestNGTestSet( testClass );
    }

    // single class test
    public void execute( String testSetName, ReporterManagerFactory reporterManagerFactory, ClassLoader classLoader )
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

        ReporterManager reporterManager = reporterManagerFactory.createReporterManager();
        startTestSuite( reporterManager, this );

        TestNGExecutor.run( new Class[]{testSet.getTestClass()}, this.testSourceDirectory, this.options, this.version,
                            this.classifier, reporterManager, this, reportsDirectory );
        
        finishTestSuite( reporterManager, this );
    }

    public void execute( ReporterManagerFactory reporterManagerFactory, ClassLoader classLoader )
        throws ReporterException, TestSetFailedException
    {
        if ( testSets == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling execute" );
        }

        List testNgTestClasses = new ArrayList();
        List junitTestClasses = new ArrayList();
        for ( Iterator it = testSets.values().iterator(); it.hasNext(); )
        {
            SurefireTestSet testSet = (SurefireTestSet) it.next();
            Class c = testSet.getTestClass();
            if (junit.framework.Test.class.isAssignableFrom( c )) {
                junitTestClasses.add( c );
            } else {
                testNgTestClasses.add( c );
            }
        }
     
        File testNgReportsDirectory = reportsDirectory, junitReportsDirectory = reportsDirectory;
        
        if ( junitTestClasses.size() > 0 && testNgTestClasses.size() > 0 )
        {
            testNgReportsDirectory = new File( reportsDirectory, "testng-native-results");
            junitReportsDirectory = new File( reportsDirectory, "testng-junit-results");
        }

        ReporterManager reporterManager = reporterManagerFactory.createReporterManager();
        startTestSuite( reporterManager, this );
        
        Class[] testClasses = (Class[]) testNgTestClasses.toArray( new Class[0] );

        TestNGExecutor.run( testClasses, this.testSourceDirectory, this.options, this.version, 
                            this.classifier, reporterManager, this, testNgReportsDirectory );
        
        if (junitTestClasses.size() > 0) {
            testClasses = (Class[]) junitTestClasses.toArray( new Class[0] );
            
            Map junitOptions = new HashMap();
            for (Iterator it = this.options.keySet().iterator(); it.hasNext();) {
                Object key = it.next();
                junitOptions.put( key, options.get( key ) );
            }
            
            junitOptions.put( "junit", Boolean.TRUE );
            
            TestNGExecutor.run( testClasses, this.testSourceDirectory, junitOptions, this.version, this.classifier,
                                reporterManager, this, junitReportsDirectory );
        }
        
        finishTestSuite( reporterManager, this );
    }

    public static void startTestSuite( ReporterManager reporterManager, Object suite )
    {
        String rawString = bundle.getString( "testSetStarting" );
        
        ReportEntry report = new ReportEntry( suite.getClass().getName(), getSuiteName(suite), rawString );

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

        ReportEntry report =
            new ReportEntry( suite.getClass().getName(), getSuiteName(suite), rawString );

        reporterManager.testSetCompleted( report );

        reporterManager.reset();
    }
    
    public String getSuiteName() {
        String result = (String) options.get("suitename");
        if (result == null) {
            result = "TestSuite";
        }
        return result;
    }

    private static String getSuiteName(Object suite)
    {
        String result;
        if (suite instanceof TestNGDirectoryTestSuite) {
            return ((TestNGDirectoryTestSuite) suite).getSuiteName();
        } else if (suite instanceof TestNGXmlTestSuite) {
            return ((TestNGXmlTestSuite) suite).getSuiteName();
        }else {
            result = "TestSuite";
        }

        return result;
    }    
}
