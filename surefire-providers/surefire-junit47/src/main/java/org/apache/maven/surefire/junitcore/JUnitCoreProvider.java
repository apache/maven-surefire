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

import org.apache.maven.surefire.common.junit4.JUnit4RunListenerFactory;
import org.apache.maven.surefire.common.junit4.JUnit4TestChecker;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.report.Reporter;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.util.ScannerFilter;
import org.apache.maven.surefire.util.TestsToRun;
import org.junit.runner.notification.RunListener;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Kristian Rosenvold
 */
@SuppressWarnings( { "UnusedDeclaration" } )
public class JUnitCoreProvider
    implements SurefireProvider
{
    private final ReporterFactory reporterFactory;

    private final ClassLoader testClassLoader;

    private final DirectoryScanner directoryScanner;

    private final JUnitCoreParameters jUnitCoreParameters;

    private final ScannerFilter scannerFilter;

    private final List<RunListener> customRunListeners;

    private TestsToRun testsToRun;

    private final ReporterConfiguration reporterConfiguration;

    public JUnitCoreProvider( ProviderParameters booterParameters )
    {
        this.reporterFactory = booterParameters.getReporterFactory();
        reporterConfiguration = booterParameters.getReporterConfiguration();
        this.testClassLoader = booterParameters.getTestClassLoader();
        this.directoryScanner = booterParameters.getDirectoryScanner();
        this.jUnitCoreParameters = new JUnitCoreParameters( booterParameters.getProviderProperties() );
        this.scannerFilter = new JUnit4TestChecker(testClassLoader);
        customRunListeners = JUnit4RunListenerFactory.
            createCustomListeners( booterParameters.getProviderProperties().getProperty( "listener" ) );

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

    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException, ReporterException
    {
        final String message = "Concurrency config is " + jUnitCoreParameters.toString();
        reporterFactory.createReporter().writeMessage( message );

        if ( testsToRun == null )
        {
            testsToRun = forkTestSet == null ? scanClassPath() : TestsToRun.fromClass( (Class) forkTestSet );
        }
        final Map<String, TestSet> testSetMap = new ConcurrentHashMap<String, TestSet>();

        Reporter listener =
            ConcurrentReporterManager.createInstance( testSetMap, this.reporterFactory, this.reporterConfiguration,
                                                           jUnitCoreParameters.isParallelClasses(),
                                                           jUnitCoreParameters.isParallelBoth() );
        RunListener jUnit4RunListener = new JUnitCoreRunListener( listener, testSetMap);
        customRunListeners.add( 0, jUnit4RunListener );

        JUnitCoreWrapper.execute( testsToRun, jUnitCoreParameters, customRunListeners );
        return reporterFactory.close();
    }

    private TestsToRun scanClassPath()
    {
        return directoryScanner.locateTestClasses( testClassLoader, scannerFilter );
    }
}
