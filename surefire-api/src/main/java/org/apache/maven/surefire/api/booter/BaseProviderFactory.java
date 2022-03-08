package org.apache.maven.surefire.api.booter;

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

import org.apache.maven.surefire.api.cli.CommandLineOption;
import org.apache.maven.surefire.api.provider.CommandChainReader;
import org.apache.maven.surefire.api.provider.ProviderParameters;
import org.apache.maven.surefire.api.report.ReporterConfiguration;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.testset.TestArtifactInfo;
import org.apache.maven.surefire.api.testset.TestRequest;
import org.apache.maven.surefire.api.util.DefaultDirectoryScanner;
import org.apache.maven.surefire.api.util.DefaultRunOrderCalculator;
import org.apache.maven.surefire.api.util.DefaultScanResult;
import org.apache.maven.surefire.api.util.DirectoryScanner;
import org.apache.maven.surefire.api.util.RunOrderCalculator;
import org.apache.maven.surefire.api.util.ScanResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

/**
 * @author Kristian Rosenvold
 */
public class BaseProviderFactory
    implements ProviderParameters
{
    private final boolean insideFork;

    private ReporterFactory reporterFactory;

    private List<CommandLineOption> mainCliOptions = emptyList();

    private Map<String, String> providerProperties;

    private DirectoryScannerParameters directoryScannerParameters;

    private ReporterConfiguration reporterConfiguration;

    private RunOrderParameters runOrderParameters;

    private ClassLoader testClassLoader;

    private TestRequest testRequest;

    private TestArtifactInfo testArtifactInfo;

    private int skipAfterFailureCount;

    private Integer systemExitTimeout;

    private CommandChainReader commandReader;

    public BaseProviderFactory( boolean insideFork )
    {
        this.insideFork = insideFork;
    }

    @Override
    public CommandChainReader getCommandReader()
    {
        return commandReader;
    }

    public void setCommandReader( CommandChainReader commandReader )
    {
        this.commandReader = commandReader;
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
        return new DefaultRunOrderCalculator( runOrderParameters, getThreadCount() );
    }

    public void setReporterFactory( ReporterFactory reporterFactory )
    {
        this.reporterFactory = reporterFactory;
    }

    @Override
    public ReporterFactory getReporterFactory()
    {
        return reporterFactory;
    }

    public void setDirectoryScannerParameters( DirectoryScannerParameters directoryScannerParameters )
    {
        this.directoryScannerParameters = directoryScannerParameters;
    }

    public void setReporterConfiguration( ReporterConfiguration reporterConfiguration )
    {
        this.reporterConfiguration = reporterConfiguration;
    }

    public void setClassLoaders( ClassLoader testClassLoader )
    {
        this.testClassLoader = testClassLoader;
    }

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

    public void setTestArtifactInfo( TestArtifactInfo testArtifactInfo )
    {
        this.testArtifactInfo = testArtifactInfo;
    }

    public void setRunOrderParameters( RunOrderParameters runOrderParameters )
    {
        this.runOrderParameters = runOrderParameters;
    }

    @Override
    public List<CommandLineOption> getMainCliOptions()
    {
        return mainCliOptions;
    }

    /**
     * CLI options in plugin (main) JVM process.
     *
     * @param mainCliOptions options
     */
    public void setMainCliOptions( List<CommandLineOption> mainCliOptions )
    {
        this.mainCliOptions = mainCliOptions == null ? Collections.<CommandLineOption>emptyList() : mainCliOptions;
    }

    @Override
    public int getSkipAfterFailureCount()
    {
        return skipAfterFailureCount;
    }

    /**
     * See the plugin configuration parameter "skipAfterFailureCount".
     *
     * @param skipAfterFailureCount the value in config parameter "skipAfterFailureCount"
     */
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
    public Integer getSystemExitTimeout()
    {
        return systemExitTimeout;
    }

    public void setSystemExitTimeout( Integer systemExitTimeout )
    {
        this.systemExitTimeout = systemExitTimeout;
    }
}
