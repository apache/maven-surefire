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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.maven.surefire.common.junit4.JUnit4RunListenerFactory;
import org.apache.maven.surefire.common.junit4.JUnit4TestChecker;
import org.apache.maven.surefire.common.junit48.FilterFactory;
import org.apache.maven.surefire.common.junit48.JUnit48Reflector;
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

import org.junit.runner.Description;
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
        this.scannerFilter = new JUnit4TestChecker( testClassLoader );
        this.requestedTestMethod = providerParameters.getTestRequest().getRequestedTestMethod();

        customRunListeners = JUnit4RunListenerFactory.
            createCustomListeners( providerParameters.getProviderProperties().getProperty( "listener" ) );
        jUnit48Reflector = new JUnit48Reflector( testClassLoader );
    }

    public Boolean isRunnable()
    {
        return Boolean.TRUE;
    }

    public Iterator getSuites()
    {
        final Filter filter = jUnit48Reflector.isJUnit48Available() ? createJUnit48Filter() : null;
        testsToRun = getSuitesAsList( filter );
        return testsToRun.iterator();
    }

    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException
    {
        final String message = "Concurrency config is " + jUnitCoreParameters.toString() + "\n";
        final ReporterFactory reporterFactory = providerParameters.getReporterFactory();

        final ConsoleLogger consoleLogger = providerParameters.getConsoleLogger();
        consoleLogger.info( message );

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
                testsToRun = getSuitesAsList( filter );
            }
        }

        final Map<String, TestSet> testSetMap = new ConcurrentHashMap<String, TestSet>();

        RunListener listener = ConcurrentReporterManager.createInstance( testSetMap, reporterFactory,
                                                                         jUnitCoreParameters.isParallelClasses(),
                                                                         jUnitCoreParameters.isParallelBoth(),
                                                                         consoleLogger );

        ConsoleOutputCapture.startCapture( (ConsoleOutputReceiver) listener );

        org.junit.runner.notification.RunListener jUnit4RunListener =
            new JUnitCoreRunListener( listener, testSetMap );
        customRunListeners.add( 0, jUnit4RunListener );

        JUnitCoreWrapper.execute( testsToRun, jUnitCoreParameters, customRunListeners, filter );
        return reporterFactory.close();
    }

    @SuppressWarnings( "unchecked" )
    private TestsToRun getSuitesAsList( Filter filter )
    {
        List<Class<?>> res = new ArrayList<Class<?>>( 500 );
        TestsToRun max = scanClassPath();
        if ( filter == null )
        {
            return max;
        }

        Iterator<Class<?>> it = max.iterator();
        while ( it.hasNext() )
        {
            Class<?> clazz = it.next();
            if ( canRunClass( filter, clazz ) )
            {
                res.add( clazz );
            }
        }
        return new TestsToRun( res );
    }

    private boolean canRunClass( Filter filter, Class<?> clazz )
    {
        final Description d = Description.createSuiteDescription( clazz );
        if ( filter.shouldRun( d ) )
        {
            // if the class-level check passes, we need to check if any methods are left to run
            for ( Method method : clazz.getMethods() )
            {
                final Description testDescription =
                    Description.createTestDescription( clazz, method.getName(), method.getAnnotations() );
                if ( filter.shouldRun( testDescription ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    private Filter createJUnit48Filter()
    {
        final FilterFactory filterFactory = new FilterFactory( testClassLoader );
        return isMethodFilterSpecified()
            ? filterFactory.createMethodFilter( requestedTestMethod )
            : filterFactory.createGroupFilter( providerParameters.getProviderProperties() );
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
