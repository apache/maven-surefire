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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.shared.utils.io.SelectorUtils;
import org.apache.maven.surefire.common.junit4.JUnit4ProviderUtil;
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
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.TestsToRun;
import org.apache.maven.surefire.util.internal.StringUtils;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.RunNotifier;

/**
 * @author Kristian Rosenvold
 */
public class JUnit4Provider
    extends AbstractProvider
{
    private final ClassLoader testClassLoader;

    private final List<org.junit.runner.notification.RunListener> customRunListeners;

    private final JUnit4TestChecker jUnit4TestChecker;

    private final String requestedTestMethod;

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
        customRunListeners = JUnit4RunListenerFactory.
            createCustomListeners( booterParameters.getProviderProperties().getProperty( "listener" ) );
        jUnit4TestChecker = new JUnit4TestChecker( testClassLoader );
        requestedTestMethod = booterParameters.getTestRequest().getRequestedTestMethod();
        rerunFailingTestsCount = booterParameters.getTestRequest().getRerunFailingTestsCount();
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

        if ( testsToRun.allowEagerReading() )
        {
            runNotifier.fireTestRunStarted( createTestsDescription() );
        }

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
            if ( !StringUtils.isBlank( requestedTestMethod ) )
            {
                String actualTestMethod = getMethod( clazz, requestedTestMethod );
                String[] testMethods = StringUtils.split( actualTestMethod, "+" );
                executeWithRerun( clazz, listeners, testMethods );
            }
            else
            {
                executeWithRerun( clazz, listeners, null );
            }
        }
        catch ( Throwable e )
        {
            reporter.testError( SimpleReportEntry.withException( report.getSourceName(), report.getName(),
                                                                 new PojoStackTraceWriter( report.getSourceName(),
                                                                                           report.getName(), e ) ) );
        }
        finally
        {
            reporter.testSetCompleted( report );
        }
    }

    private void executeWithRerun( Class<?> clazz, RunNotifier listeners, String[] testMethods )
    {
        JUnitTestFailureListener failureListener = new JUnitTestFailureListener();
        listeners.addListener( failureListener );

        execute( clazz, listeners, testMethods );

        // Rerun failing tests if rerunFailingTestsCount is larger than 0
        if ( rerunFailingTestsCount > 0 )
        {
            for ( int i = 0; i < rerunFailingTestsCount && !failureListener.getAllFailures().isEmpty(); i++ )
            {
                Set<String> methodsSet = JUnit4ProviderUtil.generateFailingTests( failureListener.getAllFailures() );
                String[] methods = methodsSet.toArray( new String[ methodsSet.size() ] );
                failureListener.reset();
                execute( clazz, listeners, methods );
            }
        }
    }

    private RunNotifier getRunNotifier( org.junit.runner.notification.RunListener main, Result result,
                                        List<org.junit.runner.notification.RunListener> others )
    {
        RunNotifier fNotifier = new RunNotifier();
        fNotifier.addListener( main );
        fNotifier.addListener( result.createListener() );
        for ( org.junit.runner.notification.RunListener listener : others )
        {
            fNotifier.addListener( listener );
        }
        return fNotifier;
    }

    // I am not entirely sure as to why we do this explicit freeing, it's one of those
    // pieces of code that just seem to linger on in here ;)
    private void closeRunNotifier( org.junit.runner.notification.RunListener main,
                                   List<org.junit.runner.notification.RunListener> others )
    {
        RunNotifier fNotifier = new RunNotifier();
        fNotifier.removeListener( main );
        for ( org.junit.runner.notification.RunListener listener : others )
        {
            fNotifier.removeListener( listener );
        }
    }

    public Iterator<?> getSuites()
    {
        testsToRun = scanClassPath();
        return testsToRun.iterator();
    }

    private TestsToRun scanClassPath()
    {
        final TestsToRun scannedClasses = scanResult.applyFilter( jUnit4TestChecker, testClassLoader );
        return runOrderCalculator.orderTestClasses( scannedClasses );
    }

    @SuppressWarnings( "unchecked" )
    private void upgradeCheck()
        throws TestSetFailedException
    {
        if ( isJUnit4UpgradeCheck() )
        {
            List<Class> classesSkippedByValidation =
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

    private static void execute( Class<?> testClass, RunNotifier fNotifier, String[] testMethods )
    {
        if ( testMethods != null )
        {
            for ( final Method method : testClass.getMethods() )
            {
                for ( final String testMethod : testMethods )
                {
                    if ( SelectorUtils.match( testMethod, method.getName() ) )
                    {
                        Request.method( testClass, method.getName() ).getRunner().run( fNotifier );
                    }

                }
            }
        }
        else
        {
            Request.aClass( testClass ).getRunner().run( fNotifier );
        }
    }

    /**
     * this method retrive testMethods from String like
     * "com.xx.ImmutablePairTest#testBasic,com.xx.StopWatchTest#testLang315+testStopWatchSimpleGet" <br>
     * and we need to think about cases that 2 or more method in 1 class. we should choose the correct method
     *
     * @param testClass the testclass
     * @param testMethodStr the test method string
     * @return a string ;)
     */
    private static String getMethod( Class testClass, String testMethodStr )
    {
        final String className = testClass.getName();

        if ( !testMethodStr.contains( "#" ) && !testMethodStr.contains( "," ) )
        {
            return testMethodStr;
        }
        testMethodStr += ","; // for the bellow  split code
        final int beginIndex = testMethodStr.indexOf( className );
        final int endIndex = testMethodStr.indexOf( ",", beginIndex );
        final String classMethodStr =
            testMethodStr.substring( beginIndex, endIndex ); // String like "StopWatchTest#testLang315"

        final int index = classMethodStr.indexOf( '#' );
        return index >= 0 ? classMethodStr.substring( index + 1, classMethodStr.length() ) : null;
    }
}
