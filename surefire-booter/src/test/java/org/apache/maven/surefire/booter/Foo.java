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

import org.apache.maven.surefire.api.booter.BaseProviderFactory;
import org.apache.maven.surefire.api.report.ReporterConfiguration;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.testset.TestArtifactInfo;
import org.apache.maven.surefire.api.testset.TestRequest;

/**
 * @author Kristian Rosenvold
 */
public class Foo extends BaseProviderFactory
{
    private Map<String, String> providerProperties;

    private ReporterConfiguration reporterConfiguration;

    private ClassLoader testClassLoader;

    private TestArtifactInfo testArtifactInfo;

    private RunOrderParameters runOrderParameters;

    private boolean called;

    Foo()
    {
        super( false );
    }

    @Override
    public void setDirectoryScannerParameters( DirectoryScannerParameters directoryScanner )
    {
        super.setDirectoryScannerParameters( directoryScanner );
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
        super.setProviderProperties( providerProperties );
        called = true;
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
        this.called = true;
    }

    @Override
    public void setTestRequest( TestRequest testRequest )
    {
        super.setTestRequest( testRequest );
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
        super.setRunOrderParameters( runOrderParameters );
        this.called = true;
    }

    @Override
    public void setReporterFactory( ReporterFactory reporterFactory )
    {
        super.setReporterFactory( reporterFactory );
        called = true;
    }
}
