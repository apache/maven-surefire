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

import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * Represents the surefire configuration that passes all the way into the provider
 * classloader and the provider.
 * <p/>
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 * @version $Id$
 */
public class ProviderConfiguration
{
    /**
     * @noinspection UnusedDeclaration
     */
    public static final int TESTS_SUCCEEDED_EXIT_CODE = 0;

    public static final int TESTS_FAILED_EXIT_CODE = 255;

    public static final int NO_TESTS_EXIT_CODE = 254;

    private final DirectoryScannerParameters dirScannerParams;

    private final ReporterConfiguration reporterConfiguration;

    private final TestArtifactInfo testArtifact;

    private final TestRequest testSuiteDefinition;

    private final Properties providerProperties;

    private final boolean failIfNoTests;

    private final Object forkTestSet;

    public ProviderConfiguration( DirectoryScannerParameters directoryScannerParameters, boolean failIfNoTests,
                                  ReporterConfiguration reporterConfiguration, TestArtifactInfo testArtifact,
                                  TestRequest testSuiteDefinition, Properties providerProperties, Object forkTestSet )
    {
        this.providerProperties = providerProperties;
        this.reporterConfiguration = reporterConfiguration;
        this.testArtifact = testArtifact;
        this.testSuiteDefinition = testSuiteDefinition;
        this.dirScannerParams = directoryScannerParameters;
        this.failIfNoTests = failIfNoTests;
        this.forkTestSet = forkTestSet;
    }


    public ReporterConfiguration getReporterConfiguration()
    {
        return reporterConfiguration;
    }


    public Boolean isFailIfNoTests()
    {
        return ( failIfNoTests ) ? Boolean.TRUE : Boolean.FALSE;
    }

    public File getBaseDir()
    {
        return dirScannerParams.getTestClassesDirectory();
    }


    public DirectoryScannerParameters getDirScannerParams()
    {
        return dirScannerParams;
    }

    public Object[] getDirScannerParamsArray()
    {
        if ( dirScannerParams == null )
        {
            return null;
        }
        return new Object[]{ dirScannerParams.getTestClassesDirectory(), dirScannerParams.getIncludes(),
            dirScannerParams.getExcludes() };
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

    public Properties getProviderProperties()
    {
        return providerProperties;
    }

    public Object getTestForFork()
    {
        return forkTestSet;
    }

    public String getTestForForkString()
    {
        if ( forkTestSet instanceof File )
        {
            return forkTestSet.toString();
        }
        return (String) forkTestSet;
    }

    public boolean isSurefireForkReturnCode( int returnCode )
    {
        return TESTS_SUCCEEDED_EXIT_CODE == returnCode ||
               NO_TESTS_EXIT_CODE == returnCode ||
            TESTS_FAILED_EXIT_CODE == returnCode;

    }


}
