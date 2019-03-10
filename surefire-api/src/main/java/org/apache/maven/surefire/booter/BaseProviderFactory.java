package org.apache.maven.surefire.booter;

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

import org.apache.maven.surefire.cli.CommandLineOption;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ConsoleStream;
import org.apache.maven.surefire.report.DefaultDirectConsoleReporter;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.DefaultDirectoryScanner;
import org.apache.maven.surefire.util.DefaultRunOrderCalculator;
import org.apache.maven.surefire.util.DefaultScanResult;
import org.apache.maven.surefire.util.DirectoryScanner;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

/**
 * @author Kristian Rosenvold
 */
public class BaseProviderFactory
    implements DirectoryScannerParametersAware, ReporterConfigurationAware, SurefireClassLoadersAware, TestRequestAware,
    ProviderPropertiesAware, ProviderParameters, TestArtifactInfoAware, RunOrderParametersAware, MainCliOptionsAware,
    FailFastAware, ShutdownAware
{
    private final ReporterFactory reporterFactory;

    private final boolean insideFork;

    private ForkedChannelEncoder forkedChannelEncoder;

    private List<CommandLineOption> mainCliOptions = emptyList();

    private Map<String, String> providerProperties;

    private DirectoryScannerParameters directoryScannerParameters;

    private ReporterConfiguration reporterConfiguration;

    private RunOrderParameters runOrderParameters;

    private ClassLoader testClassLoader;

    private TestRequest testRequest;

    private TestArtifactInfo testArtifactInfo;

    private int skipAfterFailureCount;

    private Shutdown shutdown;

    private Integer systemExitTimeout;

    public BaseProviderFactory( ReporterFactory reporterFactory, boolean insideFork )
    {
        this.reporterFactory = reporterFactory;
        this.insideFork = insideFork;
    }

    @Override
    @Deprecated
    public DirectoryScanner getDirectoryScanner()
    {
        return directoryScannerParameters == null
                ? null : new DefaultDirectoryScanner( directoryScannerParameters.getTestClassesDirectory(),
                                            directoryScannerParameters.getIncludes(),
                                            directoryScannerParameters.getExcludes(),
                                            directoryScannerParameters.getSpecificTests() );
    }

    @Override
    public ScanResult getScanResult()
    {
        return DefaultScanResult.from( providerProperties );
    }

    private int getThreadCount()
    {
        final String threadcount = providerProperties.get( ProviderParameterNames.THREADCOUNT_PROP );
        return threadcount == null ? 1 : Math.max( Integer.parseInt( threadcount ), 1 );
    }

    @Override
    public RunOrderCalculator getRunOrderCalculator()
    {
        return directoryScannerParameters == null
                ? null : new DefaultRunOrderCalculator( runOrderParameters, getThreadCount() );
    }

    @Override
    public ReporterFactory getReporterFactory()
    {
        return reporterFactory;
    }

    @Override
    public void setDirectoryScannerParameters( DirectoryScannerParameters directoryScannerParameters )
    {
        this.directoryScannerParameters = directoryScannerParameters;
    }

    @Override
    public void setReporterConfiguration( ReporterConfiguration reporterConfiguration )
    {
        this.reporterConfiguration = reporterConfiguration;
    }

    @Override
    public void setClassLoaders( ClassLoader testClassLoader )
    {
        this.testClassLoader = testClassLoader;
    }

    @Override
    public ConsoleStream getConsoleLogger()
    {
        return insideFork ? new ForkingRunListener( forkedChannelEncoder, reporterConfiguration.isTrimStackTrace() )
                       : new DefaultDirectConsoleReporter( reporterConfiguration.getOriginalSystemOut() );
    }

    @Override
    public void setTestRequest( TestRequest testRequest )
    {
        this.testRequest = testRequest;
    }

    @Override
    public DirectoryScannerParameters getDirectoryScannerParameters()
    {
        return directoryScannerParameters;
    }

    @Override
    public ReporterConfiguration getReporterConfiguration()
    {
        return reporterConfiguration;
    }

    @Override
    public TestRequest getTestRequest()
    {
        return testRequest;
    }

    @Override
    public ClassLoader getTestClassLoader()
    {
        return testClassLoader;
    }

    @Override
    public void setProviderProperties( Map<String, String> providerProperties )
    {
        this.providerProperties = providerProperties;
    }

    @Override
    public Map<String, String> getProviderProperties()
    {
        return providerProperties;
    }

    @Override
    public TestArtifactInfo getTestArtifactInfo()
    {
        return testArtifactInfo;
    }

    @Override
    public void setTestArtifactInfo( TestArtifactInfo testArtifactInfo )
    {
        this.testArtifactInfo = testArtifactInfo;
    }

    @Override
    public void setRunOrderParameters( RunOrderParameters runOrderParameters )
    {
        this.runOrderParameters = runOrderParameters;
    }

    @Override
    public List<CommandLineOption> getMainCliOptions()
    {
        return mainCliOptions;
    }

    @Override
    public void setMainCliOptions( List<CommandLineOption> mainCliOptions )
    {
        this.mainCliOptions = mainCliOptions == null ? Collections.<CommandLineOption>emptyList() : mainCliOptions;
    }

    @Override
    public int getSkipAfterFailureCount()
    {
        return skipAfterFailureCount;
    }

    @Override
    public void setSkipAfterFailureCount( int skipAfterFailureCount )
    {
        this.skipAfterFailureCount = skipAfterFailureCount;
    }

    @Override
    public boolean isInsideFork()
    {
        return insideFork;
    }

    @Override
    public Shutdown getShutdown()
    {
        return shutdown;
    }

    @Override
    public void setShutdown( Shutdown shutdown )
    {
        this.shutdown = shutdown;
    }

    @Override
    public Integer getSystemExitTimeout()
    {
        return systemExitTimeout;
    }

    public void setSystemExitTimeout( Integer systemExitTimeout )
    {
        this.systemExitTimeout = systemExitTimeout;
    }

    @Override
    public ForkedChannelEncoder getForkedChannelEncoder()
    {
        return forkedChannelEncoder;
    }

    public void setForkedChannelEncoder( ForkedChannelEncoder forkedChannelEncoder )
    {
        this.forkedChannelEncoder = forkedChannelEncoder;
    }
}
