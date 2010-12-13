package org.apache.maven.surefire.junit;

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
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.DefaultReportEntry;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.PojoTestSet;
import org.apache.maven.surefire.testset.SurefireTestSet;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.util.TestsToRun;

import java.util.Iterator;
import java.util.ResourceBundle;

/**
 * @author Kristian Rosenvold
 * @noinspection UnusedDeclaration
 */
public class JUnit3Provider
    implements SurefireProvider
{
    private final ReporterFactory reporterFactory;

    private final ClassLoader testClassLoader;

    private final DirectoryScanner directoryScanner;

    private final JUnit3TestChecker jUnit3TestChecker;

    private TestsToRun testsToRun;

    private static ResourceBundle bundle = ResourceBundle.getBundle( Surefire.SUREFIRE_BUNDLE_NAME );

    public JUnit3Provider( ProviderParameters booterParameters )
    {
        this.reporterFactory = booterParameters.getReporterFactory();
        this.testClassLoader = booterParameters.getTestClassLoader();
        this.directoryScanner = booterParameters.getDirectoryScanner();
        this.jUnit3TestChecker = new JUnit3TestChecker( testClassLoader );

    }

    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException
    {
        if ( testsToRun == null )
        {
            testsToRun = forkTestSet == null ? scanClassPath() : TestsToRun.fromClass( (Class) forkTestSet );
        }

        for ( Iterator iter = testsToRun.iterator(); iter.hasNext();  )
        {
            Class clazz = (Class) iter.next();
            ReporterManager reporter = (ReporterManager) reporterFactory.createReporter();
            SurefireTestSet surefireTestSet = createTestSet(  clazz );
            executeTestSet( surefireTestSet, reporterFactory, testClassLoader);
        }

        return reporterFactory.close();
    }

    private SurefireTestSet createTestSet( Class clazz )
        throws TestSetFailedException
    {
        return jUnit3TestChecker.isJunit3Test( clazz )
            ? new JUnitTestSet( clazz ) :
            (SurefireTestSet) new PojoTestSet( clazz );

    }

    private void executeTestSet( SurefireTestSet testSet, ReporterFactory reporterManagerFactory,
                                 ClassLoader classLoader )
        throws ReporterException, TestSetFailedException
    {

        ReporterManager reporterManager = (ReporterManager) reporterManagerFactory.createReporter();

        String rawString = bundle.getString( "testSetStarting" );

        ReportEntry report = new DefaultReportEntry( this.getClass().getName(), testSet.getName(), rawString );

        reporterManager.testSetStarting( report );

        testSet.execute( reporterManager, classLoader );

        rawString = bundle.getString( "testSetCompletedNormally" );

        report = new DefaultReportEntry( this.getClass().getName(), testSet.getName(), rawString );

        reporterManager.testSetCompleted( report );

        reporterManager.reset();
    }

    private TestsToRun scanClassPath()
    {
        return directoryScanner.locateTestClasses( testClassLoader, jUnit3TestChecker );
    }


    public Iterator getSuites()
    {
        testsToRun = scanClassPath();
        return testsToRun.iterator();
    }

    public Boolean isRunnable()
    {
        return Boolean.TRUE;
    }
}
