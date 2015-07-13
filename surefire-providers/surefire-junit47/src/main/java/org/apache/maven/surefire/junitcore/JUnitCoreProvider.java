package org.apache.maven.surefire.junitcore;

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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.surefire.common.junit4.JUnit4ProviderUtil;
import org.apache.maven.surefire.common.junit4.JUnit4RunListenerFactory;
import org.apache.maven.surefire.common.junit4.JUnitTestFailureListener;
import org.apache.maven.surefire.common.junit48.FilterFactory;
import org.apache.maven.surefire.common.junit48.JUnit48Reflector;
import org.apache.maven.surefire.common.junit48.JUnit48TestChecker;
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.report.ConsoleOutputCapture;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.ScannerFilter;
import org.apache.maven.surefire.util.TestsToRun;
import org.junit.runner.manipulation.Filter;

/**
 * @author Kristian Rosenvold
 */
@SuppressWarnings( { "UnusedDeclaration" } )
public class JUnitCoreProvider
    extends AbstractProvider
{
    private final ClassLoader testClassLoader;

    private final JUnitCoreParameters jUnitCoreParameters;

    private final ScannerFilter scannerFilter;

    private final List<org.junit.runner.notification.RunListener> customRunListeners;

    private final ProviderParameters providerParameters;

    private final ScanResult scanResult;

    private final int rerunFailingTestsCount;

    private TestsToRun testsToRun;

    private final JUnit48Reflector jUnit48Reflector;

    private final RunOrderCalculator runOrderCalculator;

    private final TestListResolver testResolver;

    public JUnitCoreProvider( ProviderParameters providerParameters )
    {
        this.providerParameters = providerParameters;
        testClassLoader = providerParameters.getTestClassLoader();
        scanResult = providerParameters.getScanResult();
        runOrderCalculator = providerParameters.getRunOrderCalculator();
        jUnitCoreParameters = new JUnitCoreParameters( providerParameters.getProviderProperties() );
        scannerFilter = new JUnit48TestChecker( testClassLoader );
        testResolver = providerParameters.getTestRequest().getTestListResolver();
        rerunFailingTestsCount = providerParameters.getTestRequest().getRerunFailingTestsCount();
        customRunListeners = JUnit4RunListenerFactory.createCustomListeners(
            providerParameters.getProviderProperties().get( "listener" ) );
        jUnit48Reflector = new JUnit48Reflector( testClassLoader );
    }

    public Iterable<Class<?>> getSuites()
    {
        testsToRun = scanClassPath();
        return testsToRun;
    }

    private boolean isSingleThreaded()
    {
        return jUnitCoreParameters.isNoThreading();
    }

    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException
    {
        final ReporterFactory reporterFactory = providerParameters.getReporterFactory();

        final ConsoleLogger consoleLogger = providerParameters.getConsoleLogger();

        Filter filter = jUnit48Reflector.isJUnit48Available() ? createJUnit48Filter() : null;

        if ( testsToRun == null )
        {
            if ( forkTestSet instanceof TestsToRun )
            {
                testsToRun = (TestsToRun) forkTestSet;
            }
            else if ( forkTestSet instanceof Class )
            {
                Class theClass = (Class) forkTestSet;
                testsToRun = TestsToRun.fromClass( theClass );
            }
            else
            {
                testsToRun = scanClassPath();
            }
        }

        customRunListeners.add( 0, getRunListener( reporterFactory, consoleLogger ) );

        // Add test failure listener
        JUnitTestFailureListener testFailureListener = new JUnitTestFailureListener();
        customRunListeners.add( 0, testFailureListener );

        JUnitCoreWrapper.execute( consoleLogger, testsToRun, jUnitCoreParameters, customRunListeners, filter );

        // Rerun failing tests if rerunFailingTestsCount is larger than 0
        if ( rerunFailingTestsCount > 0 )
        {
            for ( int i = 0; i < rerunFailingTestsCount && !testFailureListener.getAllFailures().isEmpty(); i++ )
            {
                Map<Class<?>, Set<String>> failingTests =
                    JUnit4ProviderUtil.generateFailingTests( testFailureListener.getAllFailures(), testClassLoader );
                testFailureListener.reset();
                final FilterFactory filterFactory = new FilterFactory( testClassLoader );
                Filter failingMethodsFilter = filterFactory.createFailingMethodFilter( failingTests );
                JUnitCoreWrapper.execute( consoleLogger, testsToRun, jUnitCoreParameters, customRunListeners,
                                          failingMethodsFilter );
            }
        }
        return reporterFactory.close();
    }

    private org.junit.runner.notification.RunListener getRunListener( ReporterFactory reporterFactory,
                                                                      ConsoleLogger consoleLogger )
        throws TestSetFailedException
    {
        org.junit.runner.notification.RunListener jUnit4RunListener;
        if ( isSingleThreaded() )
        {
            NonConcurrentRunListener rm = new NonConcurrentRunListener( reporterFactory.createReporter() );
            ConsoleOutputCapture.startCapture( rm );
            jUnit4RunListener = rm;
        }
        else
        {
            final Map<String, TestSet> testSetMap = new ConcurrentHashMap<String, TestSet>();

            RunListener listener =
                ConcurrentRunListener.createInstance( testSetMap, reporterFactory,
                                                      isParallelTypes(),
                                                      isParallelMethodsAndTypes(), consoleLogger );
            ConsoleOutputCapture.startCapture( (ConsoleOutputReceiver) listener );

            jUnit4RunListener = new JUnitCoreRunListener( listener, testSetMap );
        }
        return jUnit4RunListener;
    }

    private boolean isParallelMethodsAndTypes()
    {
        return jUnitCoreParameters.isParallelMethods() && isParallelTypes();
    }

    private boolean isParallelTypes()
    {
        return jUnitCoreParameters.isParallelClasses() || jUnitCoreParameters.isParallelSuites();
    }

    private Filter createJUnit48Filter()
    {
        final FilterFactory factory = new FilterFactory( testClassLoader );
        Filter groupFilter = factory.createGroupFilter( providerParameters.getProviderProperties() );
        TestListResolver methodFilter = createMethodFilter();
        boolean onlyGroups = methodFilter == null || methodFilter.isEmpty();
        return onlyGroups ? groupFilter : factory.and( groupFilter, factory.createMethodFilter( methodFilter ) );
    }

    private TestsToRun scanClassPath()
    {
        TestsToRun scanned = scanResult.applyFilter( scannerFilter, testClassLoader );
        return runOrderCalculator.orderTestClasses( scanned );
    }

    private TestListResolver createMethodFilter()
    {
        return testResolver == null ? null : testResolver.createMethodFilters();
    }
}
