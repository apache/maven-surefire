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
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ConsoleOutputCapture;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.ReflectionUtils;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.TestsToRun;

import java.util.Map;

import static org.apache.maven.surefire.util.internal.ObjectUtils.systemProps;

/**
 * @author Kristian Rosenvold
 */
public class JUnit3Provider
    extends AbstractProvider
{
    private final ClassLoader testClassLoader;

    private final PojoAndJUnit3Checker testChecker;

    private final JUnit3TestChecker jUnit3TestChecker;

    private final JUnit3Reflector reflector;

    private final ProviderParameters providerParameters;

    private final RunOrderCalculator runOrderCalculator;

    private final ScanResult scanResult;

    private TestsToRun testsToRun;

    public JUnit3Provider( ProviderParameters booterParameters )
    {
        this.providerParameters = booterParameters;
        testClassLoader = booterParameters.getTestClassLoader();
        scanResult = booterParameters.getScanResult();
        runOrderCalculator = booterParameters.getRunOrderCalculator();
        reflector = new JUnit3Reflector( testClassLoader );
        jUnit3TestChecker = new JUnit3TestChecker( testClassLoader );
        testChecker = new PojoAndJUnit3Checker( jUnit3TestChecker ); // Todo; use reflector
    }

    @Override
    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException
    {
        if ( testsToRun == null )
        {
            if ( forkTestSet instanceof TestsToRun )
            {
                testsToRun = (TestsToRun) forkTestSet;
            }
            else if ( forkTestSet instanceof Class )
            {
                testsToRun = TestsToRun.fromClass( (Class<?>) forkTestSet );
            }
            else
            {
                testsToRun = scanClassPath();
            }
        }

        ReporterFactory reporterFactory = providerParameters.getReporterFactory();
        RunResult runResult;
        try
        {
            final RunListener reporter = reporterFactory.createReporter();
            ConsoleOutputCapture.startCapture( (ConsoleOutputReceiver) reporter );
            final Map<String, String> systemProperties = systemProps();
            final String smClassName = systemProperties.get( "surefire.security.manager" );
            if ( smClassName != null )
            {
                SecurityManager securityManager =
                    ReflectionUtils.instantiate( getClass().getClassLoader(), smClassName, SecurityManager.class );
                System.setSecurityManager( securityManager );
            }

            for ( Class<?> clazz : testsToRun )
            {
                SurefireTestSet surefireTestSet = createTestSet( clazz );
                executeTestSet( surefireTestSet, reporter, testClassLoader, systemProperties );
            }

        }
        finally
        {
            runResult = reporterFactory.close();
        }
        return runResult;
    }

    private SurefireTestSet createTestSet( Class<?> clazz )
        throws TestSetFailedException
    {
        return reflector.isJUnit3Available() && jUnit3TestChecker.accept( clazz )
            ? new JUnitTestSet( clazz, reflector )
            : new PojoTestSet( clazz );
    }

    private void executeTestSet( SurefireTestSet testSet, RunListener reporter, ClassLoader classLoader,
                                 Map<String, String> systemProperties )
        throws TestSetFailedException
    {
        SimpleReportEntry report = new SimpleReportEntry( getClass().getName(), testSet.getName(), systemProperties );

        reporter.testSetStarting( report );

        testSet.execute( reporter, classLoader );

        reporter.testSetCompleted( report );
    }

    private TestsToRun scanClassPath()
    {
        final TestsToRun testsToRun = scanResult.applyFilter( testChecker, testClassLoader );
        return runOrderCalculator.orderTestClasses( testsToRun );
    }

    @Override
    public Iterable<Class<?>> getSuites()
    {
        testsToRun = scanClassPath();
        return testsToRun;
    }
}
