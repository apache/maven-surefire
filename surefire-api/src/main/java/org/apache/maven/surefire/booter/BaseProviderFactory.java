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

import java.util.Properties;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ConsoleLogger;
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

/**
 * @author Kristian Rosenvold
 */
public class BaseProviderFactory
    implements DirectoryScannerParametersAware, ReporterConfigurationAware, SurefireClassLoadersAware, TestRequestAware,
    ProviderPropertiesAware, ProviderParameters, TestArtifactInfoAware, RunOrderParametersAware
{

    private Properties providerProperties;

    private DirectoryScannerParameters directoryScannerParameters;

    private ReporterConfiguration reporterConfiguration;

    private RunOrderParameters runOrderParameters;

    private ClassLoader testClassLoader;

    private TestRequest testRequest;

    private TestArtifactInfo testArtifactInfo;

    private static final Integer ROOT_CHANNEL = 0;


    private final ReporterFactory reporterFactory;

    private final boolean insideFork;


    public BaseProviderFactory( ReporterFactory reporterFactory, Boolean insideFork )
    {
        this.reporterFactory = reporterFactory;
        this.insideFork = insideFork;
    }

    public DirectoryScanner getDirectoryScanner()
    {
        if ( directoryScannerParameters == null )
        {
            return null;
        }
        return new DefaultDirectoryScanner( directoryScannerParameters.getTestClassesDirectory(),
                                            directoryScannerParameters.getIncludes(),
                                            directoryScannerParameters.getExcludes(),
                                            directoryScannerParameters.getSpecificTests() );
    }

    public ScanResult getScanResult()
    {
        return DefaultScanResult.from( providerProperties );
    }

    private int getThreadCount()
    {
        final String threadcount = (String) providerProperties.get( ProviderParameterNames.THREADCOUNT_PROP );
        return threadcount == null ? 1 : Math.max( Integer.parseInt( threadcount ), 1 );
    }

    public RunOrderCalculator getRunOrderCalculator()
    {
        if ( directoryScannerParameters == null )
        {
            return null;
        }
        return new DefaultRunOrderCalculator( runOrderParameters, getThreadCount() );
    }

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

    public ConsoleLogger getConsoleLogger()
    {
        if ( insideFork )
        {
            return new ForkingRunListener( reporterConfiguration.getOriginalSystemOut(), ROOT_CHANNEL,
                                           reporterConfiguration.isTrimStackTrace() );
        }
        return new DefaultDirectConsoleReporter( reporterConfiguration.getOriginalSystemOut() );
    }

    public void setTestRequest( TestRequest testRequest )
    {
        this.testRequest = testRequest;
    }

    public DirectoryScannerParameters getDirectoryScannerParameters()
    {
        return directoryScannerParameters;
    }

    public ReporterConfiguration getReporterConfiguration()
    {
        return reporterConfiguration;
    }

    public TestRequest getTestRequest()
    {
        return testRequest;
    }

    public ClassLoader getTestClassLoader()
    {
        return testClassLoader;
    }

    public void setProviderProperties( Properties providerProperties )
    {
        this.providerProperties = providerProperties;
    }

    public Properties getProviderProperties()
    {
        return providerProperties;
    }

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

}
