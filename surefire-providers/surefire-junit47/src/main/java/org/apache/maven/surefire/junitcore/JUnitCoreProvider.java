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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.maven.surefire.common.junit4.JUnit4RunListenerFactory;
import org.apache.maven.surefire.common.junit48.FilterFactory;
import org.apache.maven.surefire.common.junit48.JUnit48Reflector;
import org.apache.maven.surefire.common.junit48.JUnit48TestChecker;
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ConsoleLogger;
import org.apache.maven.surefire.report.ConsoleOutputCapture;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.ScannerFilter;
import org.apache.maven.surefire.util.TestsToRun;
import org.apache.maven.surefire.util.internal.StringUtils;
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

    private TestsToRun testsToRun;

    private JUnit48Reflector jUnit48Reflector;

    private RunOrderCalculator runOrderCalculator;

    private String requestedTestMethod;

    private final ScanResult scanResult;

    public JUnitCoreProvider( ProviderParameters providerParameters )
    {
        this.providerParameters = providerParameters;
        this.testClassLoader = providerParameters.getTestClassLoader();
        this.scanResult = providerParameters.getScanResult();
        this.runOrderCalculator = providerParameters.getRunOrderCalculator();
        this.jUnitCoreParameters = new JUnitCoreParameters( providerParameters.getProviderProperties() );
        this.scannerFilter = new JUnit48TestChecker( testClassLoader );
        this.requestedTestMethod = providerParameters.getTestRequest().getRequestedTestMethod();

        customRunListeners =
            JUnit4RunListenerFactory.createCustomListeners( providerParameters.getProviderProperties().getProperty( "listener" ) );
        jUnit48Reflector = new JUnit48Reflector( testClassLoader );
    }

    public Boolean isRunnable()
    {
        return Boolean.TRUE;
    }

    public Iterator getSuites()
    {
        testsToRun = scanClassPath();
        return testsToRun.iterator();
    }

    private boolean isSingleThreaded()
    {
        return jUnitCoreParameters.isNoThreading();
    }

    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException
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

        org.junit.runner.notification.RunListener jUnit4RunListener = getRunListener( reporterFactory, consoleLogger );
        customRunListeners.add( 0, jUnit4RunListener );
        JUnitCoreWrapper.execute( testsToRun, jUnitCoreParameters, customRunListeners, filter );
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
        return jUnitCoreParameters.isParallelMethod() && isParallelTypes();
    }

    private boolean isParallelTypes()
    {
        return jUnitCoreParameters.isParallelClasses() || jUnitCoreParameters.isParallelSuites();
    }

    private Filter createJUnit48Filter()
    {
        final FilterFactory filterFactory = new FilterFactory( testClassLoader );
        Filter groupFilter = filterFactory.createGroupFilter( providerParameters.getProviderProperties() );
        return isMethodFilterSpecified() ? filterFactory.and( groupFilter,
                                                              filterFactory.createMethodFilter( requestedTestMethod ) )
                        : groupFilter;
    }

    private TestsToRun scanClassPath()
    {
        final TestsToRun scanned = scanResult.applyFilter( scannerFilter, testClassLoader );
        return runOrderCalculator.orderTestClasses( scanned );
    }

    private boolean isMethodFilterSpecified()
    {
        return !StringUtils.isBlank( requestedTestMethod );
    }
}
