package org.apache.maven.surefire.junit4;

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
import org.apache.maven.surefire.common.junit4.ClassMethod;
import org.apache.maven.surefire.common.junit4.JUnit4RunListener;
import org.apache.maven.surefire.common.junit4.JUnit4TestChecker;
import org.apache.maven.surefire.common.junit4.JUnitTestFailureListener;
import org.apache.maven.surefire.common.junit4.Notifier;
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.PojoStackTraceWriter;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.TestsToRun;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;

import java.util.Collection;
import java.util.Set;

import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isInterface;
import static org.apache.maven.surefire.booter.CommandReader.getReader;
import static org.apache.maven.surefire.common.junit4.JUnit4ProviderUtil.generateFailingTests;
import static org.apache.maven.surefire.common.junit4.JUnit4Reflector.createDescription;
import static org.apache.maven.surefire.common.junit4.JUnit4Reflector.createIgnored;
import static org.apache.maven.surefire.common.junit4.JUnit4RunListener.rethrowAnyTestMechanismFailures;
import static org.apache.maven.surefire.common.junit4.JUnit4RunListenerFactory.createCustomListeners;
import static org.apache.maven.surefire.common.junit4.Notifier.pureNotifier;
import static org.apache.maven.surefire.report.ConsoleOutputCapture.startCapture;
import static org.apache.maven.surefire.report.SimpleReportEntry.withException;
import static org.apache.maven.surefire.testset.TestListResolver.optionallyWildcardFilter;
import static org.apache.maven.surefire.util.TestsToRun.fromClass;
import static org.apache.maven.surefire.util.internal.ObjectUtils.systemProps;
import static org.junit.runner.Request.aClass;
import static org.junit.runner.Request.method;

/**
 * @author Kristian Rosenvold
 */
public class JUnit4Provider
    extends AbstractProvider
{
    private static final String UNDETERMINED_TESTS_DESCRIPTION = "cannot determine test in forked JVM with surefire";

    private final ClassLoader testClassLoader;

    private final String customRunListeners;

    private final JUnit4TestChecker jUnit4TestChecker;

    private final TestListResolver testResolver;

    private final ProviderParameters providerParameters;

    private final RunOrderCalculator runOrderCalculator;

    private final ScanResult scanResult;

    private final int rerunFailingTestsCount;

    private final CommandReader commandsReader;

    private TestsToRun testsToRun;

    public JUnit4Provider( ProviderParameters bootParams )
    {
        // don't start a thread in CommandReader while we are in in-plugin process
        commandsReader = bootParams.isInsideFork() ? getReader().setShutdown( bootParams.getShutdown() ) : null;
        providerParameters = bootParams;
        testClassLoader = bootParams.getTestClassLoader();
        scanResult = bootParams.getScanResult();
        runOrderCalculator = bootParams.getRunOrderCalculator();
        customRunListeners = bootParams.getProviderProperties().get( "listener" );
        jUnit4TestChecker = new JUnit4TestChecker( testClassLoader );
        TestRequest testRequest = bootParams.getTestRequest();
        testResolver = testRequest.getTestListResolver();
        rerunFailingTestsCount = testRequest.getRerunFailingTestsCount();
    }

    @Override
    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException
    {
        upgradeCheck();

        ReporterFactory reporterFactory = providerParameters.getReporterFactory();

        RunResult runResult;
        try
        {
            RunListener reporter = reporterFactory.createReporter();

            startCapture( (ConsoleOutputReceiver) reporter );
            // startCapture() called in prior to setTestsToRun()

            if ( testsToRun == null )
            {
                setTestsToRun( forkTestSet );
            }

            Notifier notifier = new Notifier( new JUnit4RunListener( reporter ), getSkipAfterFailureCount() );
            Result result = new Result();
            notifier.addListeners( createCustomListeners( customRunListeners ) )
                .addListener( result.createListener() );

            if ( isFailFast() && commandsReader != null )
            {
                registerPleaseStopJUnitListener( notifier );
            }

            try
            {
                notifier.fireTestRunStarted( testsToRun.allowEagerReading()
                                                 ? createTestsDescription( testsToRun )
                                                 : createDescription( UNDETERMINED_TESTS_DESCRIPTION ) );

                if ( commandsReader != null )
                {
                    registerShutdownListener( testsToRun );
                    commandsReader.awaitStarted();
                }

                for ( Class<?> testToRun : testsToRun )
                {
                    executeTestSet( testToRun, reporter, notifier );
                }
            }
            finally
            {
                notifier.fireTestRunFinished( result );
                notifier.removeListeners();
            }
            rethrowAnyTestMechanismFailures( result );
        }
        finally
        {
            runResult = reporterFactory.close();
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
            testsToRun = fromClass( (Class<?>) forkTestSet );
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
            @Override
            public void update( Command command )
            {
                testsToRun.markTestSetFinished();
            }
        } );
    }

    private void registerPleaseStopJUnitListener( final Notifier notifier )
    {
        commandsReader.addSkipNextTestsListener( new CommandListener()
        {
            @Override
            public void update( Command command )
            {
                notifier.pleaseStop();
            }
        } );
    }

    private void executeTestSet( Class<?> clazz, RunListener reporter, Notifier notifier )
    {
        final SimpleReportEntry report = new SimpleReportEntry( getClass().getName(), clazz.getName(), systemProps() );
        reporter.testSetStarting( report );
        try
        {
            executeWithRerun( clazz, notifier );
        }
        catch ( Throwable e )
        {
            if ( isFailFast() && e instanceof StoppedByUserException )
            {
                String reason = e.getClass().getName();
                Description skippedTest = createDescription( clazz.getName(), createIgnored( reason ) );
                notifier.fireTestIgnored( skippedTest );
            }
            else
            {
                String reportName = report.getName();
                String reportSourceName = report.getSourceName();
                PojoStackTraceWriter stackWriter = new PojoStackTraceWriter( reportSourceName, reportName, e );
                reporter.testError( withException( reportSourceName, reportName, stackWriter ) );
            }
        }
        finally
        {
            reporter.testSetCompleted( report );
        }
    }

    private void executeWithRerun( Class<?> clazz, Notifier notifier )
        throws TestSetFailedException
    {
        JUnitTestFailureListener failureListener = new JUnitTestFailureListener();
        notifier.addListener( failureListener );
        boolean hasMethodFilter = testResolver != null && testResolver.hasMethodPatterns();

        try
        {
            try
            {
                notifier.asFailFast( isFailFast() );
                execute( clazz, notifier, hasMethodFilter ? createMethodFilter() : null );
            }
            finally
            {
                notifier.asFailFast( false );
            }

            // Rerun failing tests if rerunFailingTestsCount is larger than 0
            if ( isRerunFailingTests() )
            {
                Notifier rerunNotifier = pureNotifier();
                notifier.copyListenersTo( rerunNotifier );
                for ( int i = 0; i < rerunFailingTestsCount && !failureListener.getAllFailures().isEmpty(); i++ )
                {
                    Set<ClassMethod> failedTests = generateFailingTests( failureListener.getAllFailures() );
                    failureListener.reset();
                    if ( !failedTests.isEmpty() )
                    {
                        executeFailedMethod( rerunNotifier, failedTests );
                    }
                }
            }
        }
        finally
        {
            notifier.removeListener( failureListener );
        }
    }

    @Override
    public Iterable<Class<?>> getSuites()
    {
        testsToRun = scanClassPath();
        return testsToRun;
    }

    private TestsToRun scanClassPath()
    {
        final TestsToRun scannedClasses = scanResult.applyFilter( jUnit4TestChecker, testClassLoader );
        return runOrderCalculator.orderTestClasses( scannedClasses );
    }

    private void upgradeCheck()
        throws TestSetFailedException
    {
        if ( isJUnit4UpgradeCheck() )
        {
            Collection<Class<?>> classesSkippedByValidation =
                scanResult.getClassesSkippedByValidation( jUnit4TestChecker, testClassLoader );
            if ( !classesSkippedByValidation.isEmpty() )
            {
                StringBuilder reason = new StringBuilder();
                reason.append( "Updated check failed\n" );
                reason.append( "There are tests that would be run with junit4 / surefire 2.6 but not with [2.7,):\n" );
                for ( Class testClass : classesSkippedByValidation )
                {
                    reason.append( "   " );
                    reason.append( testClass.getName() );
                    reason.append( "\n" );
                }
                throw new TestSetFailedException( reason.toString() );
            }
        }
    }

    static Description createTestsDescription( Iterable<Class<?>> classes )
    {
        // "null" string rather than null; otherwise NPE in junit:4.0
        Description description = createDescription( "null" );
        for ( Class<?> clazz : classes )
        {
            description.addChild( createDescription( clazz.getName() ) );
        }
        return description;
    }

    private static boolean isJUnit4UpgradeCheck()
    {
        return System.getProperty( "surefire.junit4.upgradecheck" ) != null;
    }

    private static void execute( Class<?> testClass, Notifier notifier, Filter filter )
    {
        final int classModifiers = testClass.getModifiers();
        if ( !isAbstract( classModifiers ) && !isInterface( classModifiers ) )
        {
            Request request = aClass( testClass );
            if ( filter != null )
            {
                request = request.filterWith( filter );
            }
            Runner runner = request.getRunner();
            if ( countTestsInRunner( runner.getDescription() ) != 0 )
            {
                runner.run( notifier );
            }
        }
    }

    private void executeFailedMethod( RunNotifier notifier, Set<ClassMethod> failedMethods )
        throws TestSetFailedException
    {
        for ( ClassMethod failedMethod : failedMethods )
        {
            try
            {
                Class<?> methodClass = Class.forName( failedMethod.getClazz(), true, testClassLoader );
                String methodName = failedMethod.getMethod();
                method( methodClass, methodName ).getRunner().run( notifier );
            }
            catch ( ClassNotFoundException e )
            {
                throw new TestSetFailedException( "Unable to create test class '" + failedMethod.getClazz() + "'", e );
            }
        }
    }

    /**
     * JUnit error: test count includes one test-class as a suite which has filtered out all children.
     * Then the child test has a description "initializationError0(org.junit.runner.manipulation.Filter)"
     * for JUnit 4.0 or "initializationError(org.junit.runner.manipulation.Filter)" for JUnit 4.12
     * and Description#isTest() returns true, but this description is not a real test
     * and therefore it should not be included in the entire test count.
     */
    private static int countTestsInRunner( Description description )
    {
        if ( description.isSuite() )
        {
            int count = 0;
            for ( Description child : description.getChildren() )
            {
                if ( !hasFilteredOutAllChildren( child ) )
                {
                    count += countTestsInRunner( child );
                }
            }
            return count;
        }
        else if ( description.isTest() )
        {
            return hasFilteredOutAllChildren( description ) ? 0 : 1;
        }
        else
        {
            return 0;
        }
    }

    private static boolean hasFilteredOutAllChildren( Description description )
    {
        String name = description.getDisplayName();
        // JUnit 4.0: initializationError0; JUnit 4.12: initializationError.
        if ( name == null )
        {
            return true;
        }
        else
        {
            name = name.trim();
            return name.startsWith( "initializationError0(org.junit.runner.manipulation.Filter)" )
                           || name.startsWith( "initializationError(org.junit.runner.manipulation.Filter)" );
        }
    }

    private Filter createMethodFilter()
    {
        TestListResolver methodFilter = optionallyWildcardFilter( testResolver );
        return methodFilter.isEmpty() || methodFilter.isWildcard() ? null : new TestResolverFilter( methodFilter );
    }
}
