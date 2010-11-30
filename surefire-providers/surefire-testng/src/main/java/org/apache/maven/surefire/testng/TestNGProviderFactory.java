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

import org.apache.maven.surefire.providerapi.BaseProviderFactory;
import org.apache.maven.surefire.providerapi.ProviderPropertiesAware;
import org.apache.maven.surefire.providerapi.SurefireProvider;
import org.apache.maven.surefire.providerapi.TestArtifactInfoAware;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;

import java.io.File;
import java.util.Properties;

/**
 * @author Kristian Rosenvold
 * @noinspection UnusedDeclaration
 */
public class TestNGProviderFactory
    extends BaseProviderFactory
    implements ProviderPropertiesAware, TestArtifactInfoAware
{
    Properties providerProperties;

    TestArtifactInfo testArtifactInfo;

    public void setProviderProperties( Properties providerProperties )
    {
        this.providerProperties = providerProperties;
    }

    public void setTestArtifactInfo( TestArtifactInfo testArtifactInfo )
    {
        this.testArtifactInfo = testArtifactInfo;
    }

    public SurefireProvider createProvider()
    {
        final DirectoryScannerParameters directoryScannerParameters = getDirectoryScannerParameters();
        File testClassesDirectory =
            directoryScannerParameters != null ? directoryScannerParameters.getTestClassesDirectory() : null;
        return new TestNGProvider( providerProperties, testArtifactInfo, getReporterConfiguration(),
                                   getReporterManagerFactory(), getTestClassLoader(), getDirectoryScannerParameters(),
                                   getDirectoryScanner(), getTestRequest(), testClassesDirectory );
    }
}
