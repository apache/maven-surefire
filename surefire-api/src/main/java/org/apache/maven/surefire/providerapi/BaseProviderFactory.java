package org.apache.maven.surefire.providerapi;

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

import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterManagerFactory;
import org.apache.maven.surefire.report.ReporterManagerFactory2;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.DefaultDirectoryScanner;
import org.apache.maven.surefire.util.DirectoryScanner;

import java.util.Properties;

/**
 * @author Kristian Rosenvold
 */
public abstract class BaseProviderFactory
    implements DirectoryScannerParametersAware, ReporterConfigurationAware, SurefireClassLoadersAware, TestRequestAware,
    ProviderFactory, ProviderPropertiesAware
{
    private Properties providerProperties;

    private DirectoryScannerParameters directoryScannerParameters;

    private ReporterConfiguration reporterConfiguration;

    private ClassLoader testClassLoader;

    private ClassLoader surefireClassLoader;

    private TestRequest testRequest;


    protected DirectoryScanner getDirectoryScanner()
    {
        if ( directoryScannerParameters == null )
        {
            return null;
        }
        return new DefaultDirectoryScanner( directoryScannerParameters.getTestClassesDirectory(),
                                            directoryScannerParameters.getIncludes(),
                                            directoryScannerParameters.getExcludes() );
    }

    protected ReporterManagerFactory getReporterManagerFactory()
    {
        ReporterManagerFactory reporterManagerFactory =
            new ReporterManagerFactory2( surefireClassLoader, reporterConfiguration );
        if ( providerProperties != null )
        {
            reporterManagerFactory.getGlobalRunStatistics().initResultsFromProperties( providerProperties );
        }
        return reporterManagerFactory;
    }

    public void setDirectoryScannerParameters( DirectoryScannerParameters directoryScannerParameters )
    {
        this.directoryScannerParameters = directoryScannerParameters;
    }

    public void setReporterConfiguration( ReporterConfiguration reporterConfiguration )
    {
        this.reporterConfiguration = reporterConfiguration;
    }

    public void setClassLoaders( ClassLoader surefireClassLoader, ClassLoader testClassLoader )
    {
        this.surefireClassLoader = surefireClassLoader;
        this.testClassLoader = testClassLoader;
    }

    public void setTestRequest( TestRequest testRequest )
    {
        this.testRequest = testRequest;
    }

    protected DirectoryScannerParameters getDirectoryScannerParameters()
    {
        return directoryScannerParameters;
    }

    protected ReporterConfiguration getReporterConfiguration()
    {
        return reporterConfiguration;
    }

    protected TestRequest getTestRequest()
    {
        return testRequest;
    }

    protected ClassLoader getTestClassLoader()
    {
        return testClassLoader;
    }

    public void setProviderProperties( Properties providerProperties )
    {
        this.providerProperties = providerProperties;
    }
}
