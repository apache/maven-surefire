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

import org.apache.maven.surefire.providerapi.ProviderConfiguration;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.suite.SuiteDefinition;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestSuiteDefinition;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * Represents the surefire configuration that crosses booter forks into other vms and classloaders.
 * <p/>
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 * @version $Id$
 */
public class BooterConfiguration
{
    /**
     * @noinspection UnusedDeclaration
     */
    public static final int TESTS_SUCCEEDED_EXIT_CODE = 0;

    public static final int TESTS_FAILED_EXIT_CODE = 255;

    public static final int NO_TESTS_EXIT_CODE = 254;

    private final ClassLoaderConfiguration classLoaderConfiguration;

    private final ClasspathConfiguration classpathConfiguration;

    private final ProviderConfiguration providerConfiguration;

    private final List reports;


    private final boolean failIfNoTests;

    private final ReporterConfiguration reporterConfiguration;

    private final boolean redirectTestOutputToFile;


    private final boolean isForkRequested;


    private final DirectoryScannerParameters dirScannerParams;

    private final boolean isInForkedVm;

    private final TestArtifactInfo testNg;

    private final TestSuiteDefinition testSuiteDefinition;

    private Properties providerProperties;

    private Properties properties; // todo: Zap out of here !

    public BooterConfiguration( ClassLoaderConfiguration classLoaderConfiguration,
                                ClasspathConfiguration classpathConfiguration, List reports, DirectoryScannerParameters directoryScannerParameterses,
                                boolean failIfNoTests, Properties properties, ReporterConfiguration reporterConfiguration,
                                TestArtifactInfo testArtifactInfo, TestSuiteDefinition testSuiteDefinition,
                                ProviderConfiguration providerConfiguration )
    {
        this.classLoaderConfiguration = classLoaderConfiguration;
        this.classpathConfiguration = classpathConfiguration;
        this.reports = reports;
        this.dirScannerParams = directoryScannerParameterses;
        this.failIfNoTests = failIfNoTests;
        this.redirectTestOutputToFile = false;
        this.reporterConfiguration = reporterConfiguration;
        this.properties = properties; // Todo: Hack hack. This must go
        this.isForkRequested = false;
        this.isInForkedVm = true;
        this.testNg = testArtifactInfo;
        this.testSuiteDefinition = testSuiteDefinition;
        this.providerConfiguration = providerConfiguration;
    }

    public BooterConfiguration( Properties providerProperties, boolean isForkRequested,
                                ClassLoaderConfiguration classLoaderConfiguration,
                                ClasspathConfiguration classpathConfiguration, boolean redirectTestOutputToFile,
                                ReporterConfiguration reporterConfiguration, TestArtifactInfo testNg,
                                TestSuiteDefinition testSuiteDefinition,
                                DirectoryScannerParameters directoryScannerParameters, boolean failIfNoTests,
                                List reports, ProviderConfiguration providerConfiguration )
    {
        this.providerProperties = providerProperties;
        this.classLoaderConfiguration = classLoaderConfiguration;
        this.classpathConfiguration = classpathConfiguration;
        this.reporterConfiguration = reporterConfiguration;
        this.isInForkedVm = false;
        this.isForkRequested = isForkRequested;
        this.redirectTestOutputToFile = redirectTestOutputToFile;
        this.testNg = testNg;
        this.testSuiteDefinition = testSuiteDefinition;
        this.dirScannerParams = directoryScannerParameters;
        this.failIfNoTests = failIfNoTests;
        this.reports = reports;
        this.providerConfiguration = providerConfiguration;
    }


    public ReporterConfiguration getReporterConfiguration()
    {
        return reporterConfiguration;
    }

    public ClasspathConfiguration getClasspathConfiguration()
    {
        return classpathConfiguration;
    }

    public boolean useSystemClassLoader()
    {
        // todo; I am not totally convinced this logic is as simple as it could be
        return classLoaderConfiguration.isUseSystemClassLoader() && ( isInForkedVm || isForkRequested );
    }


    public List getReports()
    {
        return reports;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public boolean isRedirectTestOutputToFile()
    {
        return redirectTestOutputToFile;
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

    public boolean isManifestOnlyJarRequestedAndUsable()
    {
        return classLoaderConfiguration.isManifestOnlyJarRequestedAndUsable();
    }

    public TestArtifactInfo getTestNg()
    {
        return testNg;
    }

    public TestSuiteDefinition getTestSuiteDefinition()
    {
        return testSuiteDefinition;
    }

    public TestSuiteDefinition getTestSuiteDefinition(String testName)
    {
        return new TestSuiteDefinition( testSuiteDefinition.getSuiteXmlFiles(), testName, testSuiteDefinition.getTestSourceDirectory(), testSuiteDefinition.getRequestedTest() );
    }

    public ProviderConfiguration getProviderConfiguration()
    {
        return providerConfiguration;
    }

    public Properties getProviderProperties()
    {
        return providerProperties;
    }
}
