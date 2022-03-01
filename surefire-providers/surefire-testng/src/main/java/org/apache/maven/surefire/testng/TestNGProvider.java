package org.apache.maven.surefire.testng;

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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.surefire.api.booter.Command;
import org.apache.maven.surefire.api.cli.CommandLineOption;
import org.apache.maven.surefire.api.provider.AbstractProvider;
import org.apache.maven.surefire.api.provider.CommandChainReader;
import org.apache.maven.surefire.api.provider.CommandListener;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.ReporterConfiguration;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.report.TestOutputReportEntry;
import org.apache.maven.surefire.api.report.TestReportListener;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestRequest;
import org.apache.maven.surefire.api.testset.TestSetFailedException;
import org.apache.maven.surefire.api.util.RunOrderCalculator;
import org.apache.maven.surefire.api.util.ScanResult;
import org.apache.maven.surefire.api.util.TestsToRun;
import org.apache.maven.surefire.testng.utils.FailFastEventsSingleton;

import static org.apache.maven.surefire.api.report.ConsoleOutputCapture.startCapture;
import static org.apache.maven.surefire.api.report.RunMode.NORMAL_RUN;
import static org.apache.maven.surefire.api.testset.TestListResolver.getEmptyTestListResolver;
import static org.apache.maven.surefire.api.testset.TestListResolver.optionallyWildcardFilter;
import static org.apache.maven.surefire.api.util.TestsToRun.fromClass;

/**
 * @author Kristian Rosenvold
 */
public class TestNGProvider
    extends AbstractProvider
{
    private final Map<String, String> providerProperties;

    private final ReporterConfiguration reporterConfiguration;

    private final ClassLoader testClassLoader;

    private final ScanResult scanResult;

    private final TestRequest testRequest;

    private final ProviderParameters providerParameters;

    private final RunOrderCalculator runOrderCalculator;

    private final List<CommandLineOption> mainCliOptions;

    private final CommandChainReader commandsReader;

    public TestNGProvider( ProviderParameters bootParams )
    {
        // don't start a thread in CommandReader while we are in in-plugin process
        commandsReader = bootParams.isInsideFork() ? bootParams.getCommandReader() : null;
        providerParameters = bootParams;
        testClassLoader = bootParams.getTestClassLoader();
        runOrderCalculator = bootParams.getRunOrderCalculator();
        providerProperties = bootParams.getProviderProperties();
        testRequest = bootParams.getTestRequest();
        reporterConfiguration = bootParams.getReporterConfiguration();
        scanResult = bootParams.getScanResult();
        mainCliOptions = bootParams.getMainCliOptions();
    }

    @Override
    public RunResult invoke( Object forkTestSet )
        throws TestSetFailedException
    {
        if ( isFailFast() && commandsReader != null )
        {
            registerPleaseStopListener();
        }

        ReporterFactory reporterFactory = providerParameters.getReporterFactory();
        TestReportListener<TestOutputReportEntry> reporter = reporterFactory.createTestReportListener();

        RunResult runResult;
        try
        {
            if ( isTestNGXmlTestSuite( testRequest ) )
            {
                TestNGReporter testNGReporter = createTestNGReporter( reporter );
                testNGReporter.setRunMode( NORMAL_RUN );
                /*
                 * {@link org.apache.maven.surefire.api.report.ConsoleOutputCapture#startCapture(ConsoleOutputReceiver)}
                 * called in prior to initializing variable {@link #testsToRun}
                 */
                startCapture( testNGReporter );

                if ( commandsReader != null )
                {
                    commandsReader.awaitStarted();
                }
                TestNGXmlTestSuite testNGXmlTestSuite = newXmlSuite();
                testNGXmlTestSuite.locateTestSets();
                testNGXmlTestSuite.execute( testNGReporter );
            }
            else
            {
                TestNGReporter testNGReporter = createTestNGReporter( reporter );
                testNGReporter.setRunMode( NORMAL_RUN );
                /*
                 * {@link org.apache.maven.surefire.api.report.ConsoleOutputCapture#startCapture(ConsoleOutputReceiver)}
                 * called in prior to initializing variable {@link #testsToRun}
                 */
                startCapture( testNGReporter );

                final TestsToRun testsToRun;
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

                if ( commandsReader != null )
                {
                    registerShutdownListener( testsToRun );
                    commandsReader.awaitStarted();
                }
                TestNGDirectoryTestSuite suite = newDirectorySuite();
                suite.execute( testsToRun, testNGReporter );
            }
        }
        finally
        {
            runResult = reporterFactory.close();
        }
        return runResult;
    }

    boolean isTestNGXmlTestSuite( TestRequest testSuiteDefinition )
    {
        Collection<File> suiteXmlFiles = testSuiteDefinition.getSuiteXmlFiles();
        return !suiteXmlFiles.isEmpty() && !hasSpecificTests();
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

    private void registerPleaseStopListener()
    {
        commandsReader.addSkipNextTestsListener( new CommandListener()
        {
            @Override
            public void update( Command command )
            {
                FailFastEventsSingleton.getInstance().setSkipOnNextTest();
            }
        } );
    }

    private TestNGDirectoryTestSuite newDirectorySuite()
    {
        return new TestNGDirectoryTestSuite( testRequest.getTestSourceDirectory().toString(), providerProperties,
                                             reporterConfiguration.getReportsDirectory(), getTestFilter(),
                                             mainCliOptions, getSkipAfterFailureCount() );
    }

    private TestNGXmlTestSuite newXmlSuite()
    {
        return new TestNGXmlTestSuite( testRequest.getSuiteXmlFiles(),
                                       testRequest.getTestSourceDirectory().toString(),
                                       providerProperties,
                                       reporterConfiguration.getReportsDirectory(), getSkipAfterFailureCount() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Iterable<Class<?>> getSuites()
    {
        if ( isTestNGXmlTestSuite( testRequest ) )
        {
            try
            {
                return newXmlSuite().locateTestSets();
            }
            catch ( TestSetFailedException e )
            {
                throw new RuntimeException( e );
            }
        }
        else
        {
            return scanClassPath();
        }
    }

    private TestsToRun scanClassPath()
    {
        final TestsToRun scanned = scanResult.applyFilter( null, testClassLoader );
        return runOrderCalculator.orderTestClasses( scanned );
    }

    private boolean hasSpecificTests()
    {
        TestListResolver specificTestPatterns = testRequest.getTestListResolver();
        return !specificTestPatterns.isEmpty() && !specificTestPatterns.isWildcard();
    }

    private TestListResolver getTestFilter()
    {
        TestListResolver filter = optionallyWildcardFilter( testRequest.getTestListResolver() );
        return filter.isWildcard() ? getEmptyTestListResolver() : filter;
    }

    // If we have access to IResultListener, return a ConfigurationAwareTestNGReporter.
    // But don't cause NoClassDefFoundErrors if it isn't available; just return a regular TestNGReporter instead.
    private static TestNGReporter createTestNGReporter( TestReportListener<TestOutputReportEntry> reportManager )
    {
        try
        {
            Class.forName( "org.testng.internal.IResultListener" );
            Class<?> c = Class.forName( "org.apache.maven.surefire.testng.ConfigurationAwareTestNGReporter" );
            Constructor<?> ctor = c.getConstructor( TestReportListener.class );
            return (TestNGReporter) ctor.newInstance( reportManager );
        }
        catch ( InvocationTargetException e )
        {
            throw new RuntimeException( "Bug in ConfigurationAwareTestNGReporter", e.getCause() );
        }
        catch ( ClassNotFoundException e )
        {
            return new TestNGReporter( reportManager );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Bug in ConfigurationAwareTestNGReporter", e );
        }
    }
}
