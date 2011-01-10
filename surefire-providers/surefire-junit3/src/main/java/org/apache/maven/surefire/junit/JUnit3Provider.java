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

import org.apache.maven.surefire.common.junit3.JUnit3Reflector;
import org.apache.maven.surefire.common.junit3.JUnit3TestChecker;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.Reporter;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.util.TestsToRun;

import java.util.Iterator;

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

    private final PojoAndJUnit3Checker testChecker;

    private final JUnit3TestChecker jUnit3TestChecker;

    private final JUnit3Reflector reflector;


    private TestsToRun testsToRun;

    public JUnit3Provider( ProviderParameters booterParameters )
    {
        this.reporterFactory = booterParameters.getReporterFactory();
        this.testClassLoader = booterParameters.getTestClassLoader();
        this.directoryScanner = booterParameters.getDirectoryScanner();
        this.reflector = new JUnit3Reflector( testClassLoader );
        jUnit3TestChecker = new JUnit3TestChecker( testClassLoader );
        this.testChecker = new PojoAndJUnit3Checker( jUnit3TestChecker ); // Todo; use reflector
    }

    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException
    {
        if ( testsToRun == null )
        {
            testsToRun = forkTestSet == null ? scanClassPath() : TestsToRun.fromClass( (Class) forkTestSet );
        }

        Reporter reporter = reporterFactory.createReporter();

        for ( Iterator iter = testsToRun.iterator(); iter.hasNext(); )
        {
            Class clazz = (Class) iter.next();
            SurefireTestSet surefireTestSet = createTestSet( clazz );
            executeTestSet( surefireTestSet, reporter, testClassLoader );
        }

        return reporterFactory.close();
    }

    private SurefireTestSet createTestSet( Class clazz )
        throws TestSetFailedException
    {
        return reflector.isJUnit3Available() && jUnit3TestChecker.accept( clazz )
            ? new JUnitTestSet( clazz, reflector )
            : (SurefireTestSet) new PojoTestSet( clazz );

    }

    private void executeTestSet( SurefireTestSet testSet, Reporter reporter, ClassLoader classLoader )
        throws ReporterException, TestSetFailedException
    {

        ReportEntry report = new SimpleReportEntry( this.getClass().getName(), testSet.getName() );

        reporter.testSetStarting( report );

        testSet.execute( reporter, classLoader );

        reporter.testSetCompleted( report );
    }

    private TestsToRun scanClassPath()
    {
        return directoryScanner.locateTestClasses( testClassLoader, testChecker );
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
