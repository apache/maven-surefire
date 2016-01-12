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

import org.apache.maven.surefire.booter.Command;
import org.apache.maven.surefire.booter.CommandListener;
import org.apache.maven.surefire.booter.CommandReader;
import org.apache.maven.surefire.common.junit4.JUnit4RunListener;
import org.apache.maven.surefire.common.junit4.JUnitTestFailureListener;
import org.apache.maven.surefire.common.junit4.Notifier;
import org.apache.maven.surefire.common.junit48.FilterFactory;
import org.apache.maven.surefire.common.junit48.JUnit48Reflector;
import org.apache.maven.surefire.common.junit48.JUnit48TestChecker;
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ConsoleLogger;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableCollection;
import static org.apache.maven.surefire.booter.CommandReader.getReader;
import static org.apache.maven.surefire.common.junit4.JUnit4ProviderUtil.generateFailingTests;
import static org.apache.maven.surefire.common.junit4.JUnit4RunListenerFactory.createCustomListeners;
import static org.apache.maven.surefire.common.junit4.Notifier.pureNotifier;
import static org.apache.maven.surefire.junitcore.ConcurrentRunListener.createInstance;
import static org.apache.maven.surefire.report.ConsoleOutputCapture.startCapture;
import static org.apache.maven.surefire.testset.TestListResolver.optionallyWildcardFilter;
import static org.apache.maven.surefire.util.TestsToRun.fromClass;

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

    private final CommandReader commandsReader;

    private TestsToRun testsToRun;

    public JUnitCoreProvider( ProviderParameters bootParams )
    {
        // don't start a thread in CommandReader while we are in in-plugin process
        commandsReader = bootParams.isInsideFork() ? getReader().setShutdown( bootParams.getShutdown() ) : null;
        providerParameters = bootParams;
        testClassLoader = bootParams.getTestClassLoader();
        scanResult = bootParams.getScanResult();
        runOrderCalculator = bootParams.getRunOrderCalculator();
        jUnitCoreParameters = new JUnitCoreParameters( bootParams.getProviderProperties() );
        scannerFilter = new JUnit48TestChecker( testClassLoader );
        testResolver = bootParams.getTestRequest().getTestListResolver();
        rerunFailingTestsCount = bootParams.getTestRequest().getRerunFailingTestsCount();
        String listeners = bootParams.getProviderProperties().get( "listener" );
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
        final ReporterFactory reporterFactory = providerParameters.getReporterFactory();

        final RunResult runResult;

        final ConsoleLogger consoleLogger = providerParameters.getConsoleLogger();

        Filter filter = jUnit48Reflector.isJUnit48Available() ? createJUnit48Filter() : null;

        Notifier notifier =
            new Notifier( createRunListener( reporterFactory, consoleLogger ), getSkipAfterFailureCount() );
        // startCapture() called in createRunListener() in prior to setTestsToRun()

        if ( testsToRun == null )
        {
            setTestsToRun( forkTestSet );
        }

        // Add test failure listener
        JUnitTestFailureListener testFailureListener = new JUnitTestFailureListener();
        notifier.addListener( testFailureListener );

        if ( isFailFast() && commandsReader != null )
        {
            registerPleaseStopJUnitListener( notifier );
        }

        try
        {
            JUnitCoreWrapper core = new JUnitCoreWrapper( notifier, jUnitCoreParameters, consoleLogger );

            if ( commandsReader != null )
            {
                registerShutdownListener( testsToRun );
                commandsReader.awaitStarted();
            }

            notifier.asFailFast( isFailFast() );
            core.execute( testsToRun, customRunListeners, filter );
            notifier.asFailFast( false );

            // Rerun failing tests if rerunFailingTestsCount is larger than 0
            if ( isRerunFailingTests() )
            {
                Notifier rerunNotifier = pureNotifier();
                notifier.copyListenersTo( rerunNotifier );
                JUnitCoreWrapper rerunCore = new JUnitCoreWrapper( rerunNotifier, jUnitCoreParameters, consoleLogger );
                for ( int i = 0; i < rerunFailingTestsCount && !testFailureListener.getAllFailures().isEmpty(); i++ )
                {
                    List<Failure> failures = testFailureListener.getAllFailures();
                    Map<Class<?>, Set<String>> failingTests = generateFailingTests( failures, testClassLoader );
                    testFailureListener.reset();
                    FilterFactory filterFactory = new FilterFactory( testClassLoader );
                    Filter failingMethodsFilter = filterFactory.createFailingMethodFilter( failingTests );
                    rerunCore.execute( testsToRun, failingMethodsFilter );
                }
            }
        }
        finally
        {
            runResult = reporterFactory.close();
            notifier.removeListeners();
        }
        return runResult;
    }

    private void setTestsToRun( Object forkTestSet )
        throws TestSetFailedException
    {
        if ( forkTestSet instanceof TestsToRun )
        {
            testsToRun = (TestsToRun) forkTestSet;
        }
        else if ( forkTestSet instanceof Class )
        {
            Class<?> theClass = (Class<?>) forkTestSet;
            testsToRun = fromClass( theClass );
        }
        else
        {
            testsToRun = scanClassPath();
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
        return isFailFast() ? providerParameters.getSkipAfterFailureCount() : 0;
    }

    private void registerShutdownListener( final TestsToRun testsToRun )
    {
        commandsReader.addShutdownListener( new CommandListener()
        {
            public void update( Command command )
            {
                testsToRun.markTestSetFinished();
            }
        } );
    }

    private void registerPleaseStopJUnitListener( final Notifier stoppable )
    {
        commandsReader.addSkipNextTestsListener( new CommandListener()
        {
            public void update( Command command )
            {
                stoppable.pleaseStop();
            }
        } );
    }

    private JUnit4RunListener createRunListener( ReporterFactory reporterFactory, ConsoleLogger consoleLogger )
        throws TestSetFailedException
    {
        if ( isSingleThreaded() )
        {
            NonConcurrentRunListener rm = new NonConcurrentRunListener( reporterFactory.createReporter() );
            startCapture( rm );
            return rm;
        }
        else
        {
            final Map<String, TestSet> testSetMap = new ConcurrentHashMap<String, TestSet>();

            ConcurrentRunListener listener = createInstance( testSetMap, reporterFactory, isParallelTypes(),
                                                             isParallelMethodsAndTypes(), consoleLogger );
            startCapture( listener );

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
        Map<String, String> props = providerParameters.getProviderProperties();
        Filter groupFilter = factory.canCreateGroupFilter( props ) ? factory.createGroupFilter( props ) : null;
        TestListResolver methodFilter = optionallyWildcardFilter( testResolver );
        boolean onlyGroups = methodFilter.isEmpty() || methodFilter.isWildcard();
        if ( onlyGroups )
        {
            return groupFilter;
        }
        else
        {
            Filter jUnitMethodFilter = factory.createMethodFilter( methodFilter );
            return groupFilter == null ? jUnitMethodFilter : factory.and( groupFilter, jUnitMethodFilter );
        }
    }

    private TestsToRun scanClassPath()
    {
        TestsToRun scanned = scanResult.applyFilter( scannerFilter, testClassLoader );
        return runOrderCalculator.orderTestClasses( scanned );
    }
}
