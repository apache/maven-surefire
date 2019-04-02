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


import java.util.Map;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;

/**
 * @author Kristian Rosenvold
 */
public class Foo
    implements DirectoryScannerParametersAware, TestRequestAware, ProviderPropertiesAware, ReporterConfigurationAware,
    SurefireClassLoadersAware, TestArtifactInfoAware, RunOrderParametersAware
{
    DirectoryScannerParameters directoryScannerParameters;

    Map<String, String> providerProperties;

    ReporterConfiguration reporterConfiguration;

    ClassLoader surefireClassLoader;

    ClassLoader testClassLoader;

    TestRequest testRequest;

    TestArtifactInfo testArtifactInfo;

    RunOrderParameters runOrderParameters;

    boolean called = false;

    @Override
    public void setDirectoryScannerParameters( DirectoryScannerParameters directoryScanner )
    {
        this.directoryScannerParameters = directoryScanner;
        this.called = true;
    }


    /**
     * @return true if it has been called
     */
    public Boolean isCalled()
    {
        return called;
    }

    @Override
    public void setProviderProperties( Map<String, String> providerProperties )
    {
        this.providerProperties = providerProperties;
        this.called = true;
    }

    @Override
    public void setReporterConfiguration( ReporterConfiguration reporterConfiguration )
    {
        this.reporterConfiguration = reporterConfiguration;
        this.called = true;
    }

    @Override
    public void setClassLoaders( ClassLoader testClassLoader )
    {
        this.testClassLoader = testClassLoader;
        this.surefireClassLoader = surefireClassLoader;
        this.called = true;
    }

    @Override
    public void setTestRequest( TestRequest testRequest1 )
    {
        this.testRequest = testRequest1;
        this.called = true;
    }

    @Override
    public void setTestArtifactInfo( TestArtifactInfo testArtifactInfo )
    {
        this.testArtifactInfo = testArtifactInfo;
        this.called = true;
    }

    @Override
    public void setRunOrderParameters( RunOrderParameters runOrderParameters )
    {
        this.runOrderParameters = runOrderParameters;
        this.called = true;
    }
}
