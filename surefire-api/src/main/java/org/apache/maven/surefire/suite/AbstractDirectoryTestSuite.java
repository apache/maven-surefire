package org.apache.maven.surefire.suite;

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
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.testset.SurefireTestSet;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public abstract class AbstractDirectoryTestSuite
    implements SurefireTestSuite
{
    protected ResourceBundle bundle = ResourceBundle.getBundle( Surefire.SUREFIRE_BUNDLE_NAME );

    private static final String FS = System.getProperty( "file.separator" );

    private File basedir;

    private List includes;

    private List excludes;

    protected Map testSets;

    private int totalTests;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    protected AbstractDirectoryTestSuite( File basedir, List includes, List excludes )
    {
        this.basedir = basedir;

        this.includes = new ArrayList( includes );

        this.excludes = new ArrayList( excludes );
    }

    public Map locateTestSets( ClassLoader classLoader )
        throws TestSetFailedException
    {
        if ( testSets != null )
        {
            throw new IllegalStateException( "You can't call locateTestSets twice" );
        }
        testSets = new HashMap();

        String[] tests = collectTests( basedir, includes, excludes );

        for ( int i = 0; i < tests.length; i++ )
        {
            String className = tests[i];

            Class testClass;
            try
            {
                testClass = classLoader.loadClass( className );
            }
            catch ( ClassNotFoundException e )
            {
                throw new TestSetFailedException( "Unable to create test class '" + className + "'", e );
            }

            if ( !Modifier.isAbstract( testClass.getModifiers() ) )
            {
                SurefireTestSet testSet = createTestSet( testClass, classLoader );

                if ( testSets.containsKey( testSet.getName() ) )
                {
                    throw new TestSetFailedException( "Duplicate test set '" + testSet.getName() + "'" );
                }
                testSets.put( testSet.getName(), testSet );

                totalTests += testSet.getTestCount();
            }
        }

        return Collections.unmodifiableMap( testSets );
    }

    protected abstract SurefireTestSet createTestSet( Class testClass, ClassLoader classLoader )
        throws TestSetFailedException;

    public void execute( ReporterManager reporterManager, ClassLoader classLoader )
        throws ReporterException, TestSetFailedException
    {
        if ( testSets == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling execute" );
        }
        for ( Iterator i = testSets.values().iterator(); i.hasNext(); )
        {
            SurefireTestSet testSet = (SurefireTestSet) i.next();

            executeTestSet( testSet, reporterManager, classLoader );
        }
    }

    private void executeTestSet( SurefireTestSet testSet, ReporterManager reporterManager, ClassLoader classLoader )
        throws ReporterException, TestSetFailedException
    {
        String rawString = bundle.getString( "testSetStarting" );

        ReportEntry report = new ReportEntry( this, testSet.getName(), rawString );

        reporterManager.testSetStarting( report );

        testSet.execute( reporterManager, classLoader );

        rawString = bundle.getString( "testSetCompletedNormally" );

        report = new ReportEntry( this, testSet.getName(), rawString );

        reporterManager.testSetCompleted( report );

        reporterManager.reset();
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

        executeTestSet( testSet, reporterManager, classLoader );
    }

    public int getNumTests()
    {
        if ( testSets == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling getNumTestSets" );
        }
        return totalTests;
    }

    public int getNumTestSets()
    {
        if ( testSets == null )
        {
            throw new IllegalStateException( "You must call locateTestSets before calling getNumTestSets" );
        }
        return testSets.size();
    }

    private String[] collectTests( File basedir, List includes, List excludes )
    {
        String[] tests = EMPTY_STRING_ARRAY;
        if ( basedir.exists() )
        {
            DirectoryScanner scanner = new DirectoryScanner();

            scanner.setBasedir( basedir );

            if ( includes != null )
            {
                scanner.setIncludes( processIncludesExcludes( includes ) );
            }

            if ( excludes != null )
            {
                scanner.setExcludes( processIncludesExcludes( excludes ) );
            }

            scanner.scan();

            tests = scanner.getIncludedFiles();
            for ( int i = 0; i < tests.length; i++ )
            {
                String test = tests[i];
                test = test.substring( 0, test.indexOf( "." ) );
                tests[i] = test.replace( FS.charAt( 0 ), '.' );
            }
        }
        return tests;
    }

    private static String[] processIncludesExcludes( List list )
    {
        String[] incs = new String[list.size()];

        for ( int i = 0; i < incs.length; i++ )
        {
            incs[i] = StringUtils.replace( (String) list.get( i ), "java", "class" );

        }
        return incs;
    }
}
