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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.surefire.booter.Command;
import org.apache.maven.surefire.booter.MasterProcessListener;
import org.apache.maven.surefire.booter.MasterProcessReader;
import org.apache.maven.surefire.common.junit4.JUnit4RunListener;
import org.apache.maven.surefire.common.junit4.JUnitTestFailureListener;
import org.apache.maven.surefire.common.junit4.Notifier;
import org.apache.maven.surefire.common.junit48.FilterFactory;
import org.apache.maven.surefire.common.junit48.JUnit48Reflector;
import org.apache.maven.surefire.common.junit48.JUnit48TestChecker;
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.report.ConsoleOutputCapture;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.ScannerFilter;
import org.apache.maven.surefire.util.TestsToRun;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import static org.apache.maven.surefire.booter.MasterProcessCommand.SKIP_SINCE_NEXT_TEST;
import static org.apache.maven.surefire.junitcore.ConcurrentRunListener.createInstance;
import static org.apache.maven.surefire.common.junit4.JUnit4ProviderUtil.generateFailingTests;
import static org.apache.maven.surefire.common.junit4.JUnit4RunListenerFactory.createCustomListeners;
import static java.util.Collections.unmodifiableCollection;

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

    private final Collection<RunListener> customRunListeners;

    private final ProviderParameters providerParameters;

    private final ScanResult scanResult;

    private final int rerunFailingTestsCount;

    private final JUnit48Reflector jUnit48Reflector;

    private final RunOrderCalculator runOrderCalculator;

    private final TestListResolver testResolver;

    private final MasterProcessReader commandsReader;

    private TestsToRun testsToRun;

    public JUnitCoreProvider( ProviderParameters providerParameters )
    {
        commandsReader = providerParameters.isInsideFork()
            ? MasterProcessReader.getReader().setShutdown( providerParameters.getShutdown() )
            : null;
        this.providerParameters = providerParameters;
        testClassLoader = providerParameters.getTestClassLoader();
        scanResult = providerParameters.getScanResult();
        runOrderCalculator = providerParameters.getRunOrderCalculator();
        jUnitCoreParameters = new JUnitCoreParameters( providerParameters.getProviderProperties() );
        scannerFilter = new JUnit48TestChecker( testClassLoader );
        testResolver = providerParameters.getTestRequest().getTestListResolver();
        rerunFailingTestsCount = providerParameters.getTestRequest().getRerunFailingTestsCount();
        String listeners = providerParameters.getProviderProperties().get( "listener" );
        customRunListeners = unmodifiableCollection( createCustomListeners( listeners ) );
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
        if ( isRerunFailingTests() && isFailFast() )
        {
            throw new TestSetFailedException( "don't enable parameters rerunFailingTestsCount, skipAfterFailureCount" );
        }

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

        Notifier notifier =
            new Notifier( createRunListener( reporterFactory, consoleLogger ), getSkipAfterFailureCount() );

        // Add test failure listener
        JUnitTestFailureListener testFailureListener = new JUnitTestFailureListener();
        notifier.addListener( testFailureListener );

        if ( isFailFast() && commandsReader != null )
        {
            registerPleaseStopJunitListener( notifier );
        }

        try
        {
            JUnitCoreWrapper core = new JUnitCoreWrapper( notifier, jUnitCoreParameters, consoleLogger, isFailFast() );

            if ( commandsReader != null )
            {
                commandsReader.awaitStarted();
            }

            core.execute( testsToRun, customRunListeners, filter );

            // Rerun failing tests if rerunFailingTestsCount is larger than 0
            if ( isRerunFailingTests() )
            {
                for ( int i = 0; i < rerunFailingTestsCount && !testFailureListener.getAllFailures().isEmpty(); i++ )
                {
                    List<Failure> failures = testFailureListener.getAllFailures();
                    Map<Class<?>, Set<String>> failingTests = generateFailingTests( failures, testClassLoader );
                    testFailureListener.reset();
                    final FilterFactory filterFactory = new FilterFactory( testClassLoader );
                    Filter failingMethodsFilter = filterFactory.createFailingMethodFilter( failingTests );
                    core.execute( testsToRun, failingMethodsFilter );
                }
            }
            return reporterFactory.close();
        }
        finally
        {
            notifier.removeListeners();
            closeCommandsReader();
        }
    }

    private boolean isRerunFailingTests()
    {
        return rerunFailingTestsCount > 0;
    }

    private boolean isFailFast()
    {
        return providerParameters.getSkipAfterFailureCount() > 0;
    }

    private int getSkipAfterFailureCount()
    {
        return isFailFast() && !isRerunFailingTests() ? providerParameters.getSkipAfterFailureCount() : 0;
    }

    private void closeCommandsReader()
    {
        if ( commandsReader != null )
        {
            commandsReader.stop();
        }
    }

    private MasterProcessListener registerPleaseStopJunitListener( final Notifier stoppable )
    {
        MasterProcessListener listener = new MasterProcessListener()
        {
            public void update( Command command )
            {
                stoppable.pleaseStop();
            }
        };
        commandsReader.addListener( SKIP_SINCE_NEXT_TEST, listener );
        return listener;
    }

    private JUnit4RunListener createRunListener( ReporterFactory reporterFactory, ConsoleLogger consoleLogger )
        throws TestSetFailedException
    {
        if ( isSingleThreaded() )
        {
            NonConcurrentRunListener rm = new NonConcurrentRunListener( reporterFactory.createReporter() );
            ConsoleOutputCapture.startCapture( rm );
            return rm;
        }
        else
        {
            final Map<String, TestSet> testSetMap = new ConcurrentHashMap<String, TestSet>();

            ConcurrentRunListener listener = createInstance( testSetMap, reporterFactory, isParallelTypes(),
                                                             isParallelMethodsAndTypes(), consoleLogger );
            ConsoleOutputCapture.startCapture( listener );

            return new JUnitCoreRunListener( listener, testSetMap );
        }
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
