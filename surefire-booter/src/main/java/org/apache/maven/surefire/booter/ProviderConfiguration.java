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

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.maven.surefire.api.booter.Shutdown;
import org.apache.maven.surefire.api.cli.CommandLineOption;
import org.apache.maven.surefire.api.report.ReporterConfiguration;
import org.apache.maven.surefire.api.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.testset.TestArtifactInfo;
import org.apache.maven.surefire.api.testset.TestRequest;

/**
 * Represents the surefire configuration that passes all the way into the provider
 * classloader and the provider.
 * <br>
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 */
public class ProviderConfiguration
{
    private final DirectoryScannerParameters dirScannerParams;

    private final ReporterConfiguration reporterConfiguration;

    private final TestArtifactInfo testArtifact;

    private final TestRequest testSuiteDefinition;

    private final RunOrderParameters runOrderParameters;

    private final Map<String, String> providerProperties;

    private final TypeEncodedValue forkTestSet;

    private final boolean readTestsFromInStream;

    private final List<CommandLineOption> mainCliOptions;

    private final int skipAfterFailureCount;

    private final Shutdown shutdown;

    private final Integer systemExitTimeout;

    @SuppressWarnings( "checkstyle:parameternumber" )
    public ProviderConfiguration( DirectoryScannerParameters directoryScannerParameters,
                                  RunOrderParameters runOrderParameters,
                                  ReporterConfiguration reporterConfiguration, TestArtifactInfo testArtifact,
                                  TestRequest testSuiteDefinition, Map<String, String> providerProperties,
                                  TypeEncodedValue typeEncodedTestSet, boolean readTestsFromInStream,
                                  List<CommandLineOption> mainCliOptions, int skipAfterFailureCount,
                                  Shutdown shutdown, Integer systemExitTimeout )
    {
        this.runOrderParameters = runOrderParameters;
        this.providerProperties = providerProperties;
        this.reporterConfiguration = reporterConfiguration;
        this.testArtifact = testArtifact;
        this.testSuiteDefinition = testSuiteDefinition;
        this.dirScannerParams = directoryScannerParameters;
        this.forkTestSet = typeEncodedTestSet;
        this.readTestsFromInStream = readTestsFromInStream;
        this.mainCliOptions = mainCliOptions;
        this.skipAfterFailureCount = skipAfterFailureCount;
        this.shutdown = shutdown;
        this.systemExitTimeout = systemExitTimeout;
    }

    public ReporterConfiguration getReporterConfiguration()
    {
        return reporterConfiguration;
    }

    public File getBaseDir()
    {
        return dirScannerParams.getTestClassesDirectory();
    }

    public DirectoryScannerParameters getDirScannerParams()
    {
        return dirScannerParams;
    }

    @Deprecated
    public List getIncludes()
    {
        return dirScannerParams.getIncludes();
    }

    @Deprecated
    public List getExcludes()
    {
        return dirScannerParams.getExcludes();
    }

    public TestArtifactInfo getTestArtifact()
    {
        return testArtifact;
    }

    public TestRequest getTestSuiteDefinition()
    {
        return testSuiteDefinition;
    }

    public Map<String, String> getProviderProperties()
    {
        return providerProperties;
    }

    public TypeEncodedValue getTestForFork()
    {
        return forkTestSet;
    }

    public RunOrderParameters getRunOrderParameters()
    {
        return runOrderParameters;
    }

    public boolean isReadTestsFromInStream()
    {
        return readTestsFromInStream;
    }

    public List<CommandLineOption> getMainCliOptions()
    {
        return mainCliOptions;
    }

    public int getSkipAfterFailureCount()
    {
        return skipAfterFailureCount;
    }

    public Shutdown getShutdown()
    {
        return shutdown;
    }

    public Integer getSystemExitTimeout()
    {
        return systemExitTimeout;
    }

    public long systemExitTimeout( long fallback )
    {
        return systemExitTimeout == null || systemExitTimeout < 0 ? fallback : systemExitTimeout;
    }
}
