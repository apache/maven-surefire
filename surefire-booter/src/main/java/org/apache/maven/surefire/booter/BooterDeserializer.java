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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;

// CHECKSTYLE_OFF: imports
import static org.apache.maven.surefire.booter.BooterConstants.*;

/**
 * Knows how to serialize and deserialize the booter configuration.
 * <p/>
 * The internal serialization format is through a properties file. The long-term goal of this
 * class is not to expose this implementation information to its clients. This still leaks somewhat,
 * and there are some cases where properties are being accessed as "Properties" instead of
 * more representative domain objects.
 * <p/>
 *
 * @author Jason van Zyl
 * @author Emmanuel Venisse
 * @author Kristian Rosenvold
 */
public class BooterDeserializer
{


    private final PropertiesWrapper properties;

    public BooterDeserializer( InputStream inputStream )
        throws IOException
    {
        properties = SystemPropertyManager.loadProperties( inputStream );
    }

    public ProviderConfiguration deserialize()
    {
        final File reportsDirectory = new File( properties.getProperty( REPORTSDIRECTORY ) );
        final String testNgVersion = properties.getProperty( TESTARTIFACT_VERSION );
        final String testArtifactClassifier = properties.getProperty( TESTARTIFACT_CLASSIFIER );

        final TypeEncodedValue typeEncodedTestForFork = properties.getTypeEncodedValue( FORKTESTSET );
        final boolean preferTestsFromInStream =
            properties.getBooleanProperty( FORKTESTSET_PREFER_TESTS_FROM_IN_STREAM );

        final String requestedTest = properties.getProperty( REQUESTEDTEST );
        final String requestedTestMethod = properties.getProperty( REQUESTEDTESTMETHOD );
        final File sourceDirectory = properties.getFileProperty( SOURCE_DIRECTORY );

        final List excludesList = properties.getStringList( EXCLUDES_PROPERTY_PREFIX );
        final List includesList = properties.getStringList( INCLUDES_PROPERTY_PREFIX );
        final List specificTestsList = properties.getStringList( SPECIFIC_TEST_PROPERTY_PREFIX );

        final List testSuiteXmlFiles = properties.getStringList( TEST_SUITE_XML_FILES );
        final File testClassesDirectory = properties.getFileProperty( TEST_CLASSES_DIRECTORY );
        final String runOrder = properties.getProperty( RUN_ORDER );
        final String runStatisticsFile = properties.getProperty( RUN_STATISTICS_FILE );

        final int rerunFailingTestsCount = properties.getIntProperty( RERUN_FAILING_TESTS_COUNT );

        final boolean testsFromExternalSource = properties.getBooleanProperty( RUN_TESTS_FROM_EXTERNAL_SOURCE );
        final String externalSourceUrl = properties.getProperty( TESTS_FROM_EXTERNAL_SOURCE_URL );

        DirectoryScannerParameters dirScannerParams =
            new DirectoryScannerParameters( testClassesDirectory, includesList, excludesList, specificTestsList,
                                            properties.getBooleanObjectProperty( FAILIFNOTESTS ), runOrder );

        RunOrderParameters runOrderParameters = new RunOrderParameters( runOrder, runStatisticsFile );

        TestArtifactInfo testNg = new TestArtifactInfo( testNgVersion, testArtifactClassifier );
        TestRequest testSuiteDefinition =
            new TestRequest( testSuiteXmlFiles, sourceDirectory, requestedTest, requestedTestMethod,
                             rerunFailingTestsCount );

        ReporterConfiguration reporterConfiguration =
            new ReporterConfiguration( reportsDirectory, properties.getBooleanObjectProperty( ISTRIMSTACKTRACE ) );

        return new ProviderConfiguration( dirScannerParams, runOrderParameters,
                                          properties.getBooleanProperty( FAILIFNOTESTS ), reporterConfiguration, testNg,
                                          testSuiteDefinition, properties.getProperties(), typeEncodedTestForFork,
                                          preferTestsFromInStream, testsFromExternalSource, externalSourceUrl );
    }

    public StartupConfiguration getProviderConfiguration()
    {
        boolean useSystemClassLoader = properties.getBooleanProperty( USESYSTEMCLASSLOADER );
        boolean useManifestOnlyJar = properties.getBooleanProperty( USEMANIFESTONLYJAR );
        String providerConfiguration = properties.getProperty( PROVIDER_CONFIGURATION );

        ClassLoaderConfiguration classLoaderConfiguration =
            new ClassLoaderConfiguration( useSystemClassLoader, useManifestOnlyJar );

        ClasspathConfiguration classpathConfiguration = new ClasspathConfiguration( properties );

        return StartupConfiguration.inForkedVm( providerConfiguration, classpathConfiguration,
                                                classLoaderConfiguration );
    }
}
