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

import java.util.Map;

import org.apache.maven.surefire.api.provider.AbstractProvider;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.report.SimpleReportEntry;
import org.apache.maven.surefire.api.report.TestSetReportEntry;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.RunOrderCalculator;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.apache.maven.surefire.common.junit3.JUnit3Reflector;
import org.apache.maven.surefire.common.junit3.JUnit3TestChecker;

import static org.apache.maven.surefire.api.report.ConsoleOutputCapture.startCapture;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.util.ReflectionUtils.instantiate;
import static org.apache.maven.surefire.api.util.internal.ObjectUtils.isSecurityManagerSupported;
import static org.apache.maven.surefire.api.util.internal.ObjectUtils.systemProps;

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
        ReporterFactory reporterFactory = providerParameters.getReporterFactory();
        JUnit3Reporter reporter = new JUnit3Reporter( reporterFactory.createTestReportListener() );
        reporter.setRunMode( NORMAL_RUN );
        startCapture( reporter );

        final TestsToRun testsToRun;
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

        RunResult runResult;
        try
        {
            Map<String, String> systemProperties = systemProps();
            setSystemManager( System.getProperty( "surefire.security.manager" ) );

            for ( Class<?> clazz : testsToRun )
            {
                SurefireTestSetExecutor surefireTestSetExecutor = createTestSet( clazz, reporter );
                executeTestSet( clazz, surefireTestSetExecutor, reporter, systemProperties );
            }
        }
        finally
        {
            runResult = reporterFactory.close();
        }
        return runResult;
    }

    static void setSystemManager( String smClassName ) throws TestSetFailedException
    {
        if ( smClassName != null )
        {
            if ( !isSecurityManagerSupported() )
            {
                throw new TestSetFailedException( "JDK does not support overriding Security Manager with "
                    + "a value in system property 'surefire.security.manager'." );
            }
            ClassLoader classLoader = JUnit3Provider.class.getClassLoader();
            SecurityManager sm = instantiate( classLoader, smClassName, SecurityManager.class );
            System.setSecurityManager( sm );
        }
    }

    private SurefireTestSetExecutor createTestSet( Class<?> clazz, JUnit3Reporter reporter )
    {
        return reflector.isJUnit3Available() && jUnit3TestChecker.accept( clazz )
            ? new JUnitTestSetExecutor( reflector, reporter )
            : new PojoTestSetExecutor( reporter );
    }

    private void executeTestSet( Class<?> testSet, SurefireTestSetExecutor testSetExecutor, JUnit3Reporter reporter,
                                 Map<String, String> systemProperties )
        throws TestSetFailedException
    {
        String clazz = testSet.getName();
        long testId = reporter.getClassMethodIndexer().indexClass( clazz );

        try
        {
            TestSetReportEntry started = new SimpleReportEntry( NORMAL_RUN, testId, clazz, null, null, null );
            reporter.testSetStarting( started );
            testSetExecutor.execute( testSet, testClassLoader );
        }
        finally
        {
            TestSetReportEntry completed =
                new SimpleReportEntry( NORMAL_RUN, testId, clazz, null, null, null, systemProperties );
            reporter.testSetCompleted( completed );
        }
    }

    private TestsToRun scanClassPath()
    {
        TestsToRun testsToRun = scanResult.applyFilter( testChecker, testClassLoader );
        return runOrderCalculator.orderTestClasses( testsToRun );
    }

    @Override
    public Iterable<Class<?>> getSuites()
    {
        return scanClassPath();
    }
}
