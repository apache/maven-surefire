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

import org.apache.maven.surefire.cli.CommandLineOption;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;

/**
 * Represents the surefire configuration that passes all the way into the provider
 * classloader and the provider.
 * <p/>
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 */
public class ProviderConfiguration
{
    /**
     * @noinspection UnusedDeclaration
     */
    public static final int TESTS_SUCCEEDED_EXIT_CODE = 0;

    private final DirectoryScannerParameters dirScannerParams;

    private final ReporterConfiguration reporterConfiguration;

    private final TestArtifactInfo testArtifact;

    private final TestRequest testSuiteDefinition;

    private final RunOrderParameters runOrderParameters;

    private final Map<String, String> providerProperties;

    private final boolean failIfNoTests;

    private final TypeEncodedValue forkTestSet;

    private final boolean readTestsFromInStream;

    private final List<CommandLineOption> mainCliOptions;

    @SuppressWarnings( "checkstyle:parameternumber" )
    public ProviderConfiguration( DirectoryScannerParameters directoryScannerParameters,
                                  RunOrderParameters runOrderParameters, boolean failIfNoTests,
                                  ReporterConfiguration reporterConfiguration, TestArtifactInfo testArtifact,
                                  TestRequest testSuiteDefinition, Map<String, String> providerProperties,
                                  TypeEncodedValue typeEncodedTestSet, boolean readTestsFromInStream,
                                  List<CommandLineOption> mainCliOptions )
    {
        this.runOrderParameters = runOrderParameters;
        this.providerProperties = providerProperties;
        this.reporterConfiguration = reporterConfiguration;
        this.testArtifact = testArtifact;
        this.testSuiteDefinition = testSuiteDefinition;
        this.dirScannerParams = directoryScannerParameters;
        this.failIfNoTests = failIfNoTests;
        this.forkTestSet = typeEncodedTestSet;
        this.readTestsFromInStream = readTestsFromInStream;
        this.mainCliOptions = mainCliOptions;
    }

    public ReporterConfiguration getReporterConfiguration()
    {
        return reporterConfiguration;
    }


    public boolean isFailIfNoTests()
    {
        return failIfNoTests;
    }

    public File getBaseDir()
    {
        return dirScannerParams.getTestClassesDirectory();
    }


    public DirectoryScannerParameters getDirScannerParams()
    {
        return dirScannerParams;
    }

    public List getIncludes()
    {
        return dirScannerParams.getIncludes();
    }

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
}
