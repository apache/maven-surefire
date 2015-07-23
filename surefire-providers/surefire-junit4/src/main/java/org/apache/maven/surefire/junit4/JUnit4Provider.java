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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.maven.surefire.common.junit4.ClassMethod;
import org.apache.maven.surefire.common.junit4.JUnit4ProviderUtil;
import org.apache.maven.surefire.common.junit4.JUnit4Reflector;
import org.apache.maven.surefire.common.junit4.JUnit4RunListener;
import org.apache.maven.surefire.common.junit4.JUnit4RunListenerFactory;
import org.apache.maven.surefire.common.junit4.JUnit4TestChecker;
import org.apache.maven.surefire.common.junit4.JUnitTestFailureListener;
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ConsoleOutputCapture;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.PojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
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

import static org.apache.maven.surefire.testset.TestListResolver.toClassFileName;

/**
 * @author Kristian Rosenvold
 */
public class JUnit4Provider
    extends AbstractProvider
{
    private static final String UNDETERMINED_TESTS_DESCRIPTION = "cannot determine test in forked JVM with surefire";

    private final ClassLoader testClassLoader;

    private final List<org.junit.runner.notification.RunListener> customRunListeners;

    private final JUnit4TestChecker jUnit4TestChecker;

    private final TestListResolver testResolver;

    private final ProviderParameters providerParameters;

    private final RunOrderCalculator runOrderCalculator;

    private final ScanResult scanResult;

    private final int rerunFailingTestsCount;

    private TestsToRun testsToRun;

    public JUnit4Provider( ProviderParameters booterParameters )
    {
        providerParameters = booterParameters;
        testClassLoader = booterParameters.getTestClassLoader();
        scanResult = booterParameters.getScanResult();
        runOrderCalculator = booterParameters.getRunOrderCalculator();
        String listeners = booterParameters.getProviderProperties().get( "listener" );
        customRunListeners = JUnit4RunListenerFactory.createCustomListeners( listeners );
        jUnit4TestChecker = new JUnit4TestChecker( testClassLoader );
        TestRequest testRequest = booterParameters.getTestRequest();
        testResolver = testRequest.getTestListResolver();
        rerunFailingTestsCount = testRequest.getRerunFailingTestsCount();
    }

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
                testsToRun = TestsToRun.fromClass( (Class) forkTestSet );
            }
            else
            {
                testsToRun = scanClassPath();
            }
        }

        upgradeCheck();

        final ReporterFactory reporterFactory = providerParameters.getReporterFactory();

        RunListener reporter = reporterFactory.createReporter();

        ConsoleOutputCapture.startCapture( (ConsoleOutputReceiver) reporter );

        JUnit4RunListener jUnit4TestSetReporter = new JUnit4RunListener( reporter );

        Result result = new Result();
        RunNotifier runNotifier = getRunNotifier( jUnit4TestSetReporter, result, customRunListeners );

        runNotifier.fireTestRunStarted( testsToRun.allowEagerReading()
                                            ? createTestsDescription()
                                            : JUnit4Reflector.createDescription( UNDETERMINED_TESTS_DESCRIPTION ) );

        for ( Class aTestsToRun : testsToRun )
        {
            executeTestSet( aTestsToRun, reporter, runNotifier );
        }

        runNotifier.fireTestRunFinished( result );

        JUnit4RunListener.rethrowAnyTestMechanismFailures( result );

        closeRunNotifier( jUnit4TestSetReporter, customRunListeners );
        return reporterFactory.close();
    }

    private void executeTestSet( Class<?> clazz, RunListener reporter, RunNotifier listeners )
    {
        final ReportEntry report = new SimpleReportEntry( getClass().getName(), clazz.getName() );
        reporter.testSetStarting( report );
        try
        {
            executeWithRerun( clazz, listeners );
        }
        catch ( Throwable e )
        {
            String reportName = report.getName();
            String reportSourceName = report.getSourceName();
            PojoStackTraceWriter stackWriter = new PojoStackTraceWriter( reportSourceName, reportName, e );
            reporter.testError( SimpleReportEntry.withException( reportSourceName, reportName, stackWriter ) );
        }
        finally
        {
            reporter.testSetCompleted( report );
        }
    }

    private void executeWithRerun( Class<?> clazz, RunNotifier listeners ) throws TestSetFailedException
    {
        JUnitTestFailureListener failureListener = new JUnitTestFailureListener();
        listeners.addListener( failureListener );
        boolean hasMethodFilter = testResolver != null && testResolver.hasMethodPatterns();
        execute( clazz, listeners, hasMethodFilter ? new TestResolverFilter() : new NullFilter() );

        // Rerun failing tests if rerunFailingTestsCount is larger than 0
        if ( rerunFailingTestsCount > 0 )
        {
            for ( int i = 0; i < rerunFailingTestsCount && !failureListener.getAllFailures().isEmpty(); i++ )
            {
                Set<ClassMethod> failedTests =
                    JUnit4ProviderUtil.generateFailingTests( failureListener.getAllFailures() );
                failureListener.reset();
                if ( !failedTests.isEmpty() )
                {
                    executeFailedMethod( listeners, failedTests );
                }
            }
        }
    }

    private static RunNotifier getRunNotifier( org.junit.runner.notification.RunListener main, Result result,
                                        List<org.junit.runner.notification.RunListener> others )
    {
        RunNotifier notifier = new RunNotifier();
        notifier.addListener( main );
        notifier.addListener( result.createListener() );
        for ( org.junit.runner.notification.RunListener listener : others )
        {
            notifier.addListener( listener );
        }
        return notifier;
    }

    // I am not entirely sure as to why we do this explicit freeing, it's one of those
    // pieces of code that just seem to linger on in here ;)
    private static void closeRunNotifier( org.junit.runner.notification.RunListener main,
                                   Iterable<org.junit.runner.notification.RunListener> others )
    {
        RunNotifier notifier = new RunNotifier();
        notifier.removeListener( main );
        for ( org.junit.runner.notification.RunListener listener : others )
        {
            notifier.removeListener( listener );
        }
    }

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

    private Description createTestsDescription()
    {
        Collection<Class<?>> classes = new ArrayList<Class<?>>();
        for ( Class<?> clazz : testsToRun )
        {
            classes.add( clazz );
        }
        return JUnit4ProviderUtil.createSuiteDescription( classes );
    }

    private static boolean isJUnit4UpgradeCheck()
    {
        return System.getProperty( "surefire.junit4.upgradecheck" ) != null;
    }

    private static void execute( Class<?> testClass, RunNotifier notifier, Filter filter )
    {
        final int classModifiers = testClass.getModifiers();
        if ( !Modifier.isAbstract( classModifiers ) && !Modifier.isInterface( classModifiers ) )
        {
            Runner runner = Request.aClass( testClass ).filterWith( filter ).getRunner();
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
                Request.method( methodClass, methodName ).getRunner().run( notifier );
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

    private class TestResolverFilter
        extends Filter
    {
        private final TestListResolver methodFilter = JUnit4Provider.this.testResolver.createMethodFilters();

        @Override
        public boolean shouldRun( Description description )
        {
            // class: Java class name; method: 1. "testMethod" or 2. "testMethod[5+whatever]" in @Parameterized
            final ClassMethod cm = JUnit4ProviderUtil.cutTestClassAndMethod( description );
            final boolean isSuite = description.isSuite();
            final boolean isValidTest = description.isTest() && cm.isValid();
            final String clazz = cm.getClazz();
            final String method = cm.getMethod();
            return isSuite || isValidTest && methodFilter.shouldRun( toClassFileName( clazz ), method );
        }

        @Override
        public String describe()
        {
            return methodFilter.toString();
        }
    }

    private final class NullFilter
        extends TestResolverFilter
    {

        @Override
        public boolean shouldRun( Description description )
        {
            return true;
        }

        @Override
        public String describe()
        {
            return "";
        }
    }
}
