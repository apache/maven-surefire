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
import java.util.Collection;
import java.util.List;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.testset.TestRequest;

// CHECKSTYLE_OFF: imports
import static org.apache.maven.surefire.booter.BooterConstants.*;
import static org.apache.maven.surefire.cli.CommandLineOption.*;

/**
 * Knows how to serialize and deserialize the booter configuration.
 * <br>
 * The internal serialization format is through a properties file. The long-term goal of this
 * class is not to expose this implementation information to its clients. This still leaks somewhat,
 * and there are some cases where properties are being accessed as "Properties" instead of
 * more representative domain objects.
 * <br>
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

    /**
     * @return PID of Maven process where plugin is executed; or null if PID could not be determined.
     */
    public Long getPluginPid()
    {
        return properties.getLongProperty( PLUGIN_PID );
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
        final File sourceDirectory = properties.getFileProperty( SOURCE_DIRECTORY );

        final List<String> excludes = properties.getStringList( EXCLUDES_PROPERTY_PREFIX );
        final List<String> includes = properties.getStringList( INCLUDES_PROPERTY_PREFIX );
        final List<String> specificTests = properties.getStringList( SPECIFIC_TEST_PROPERTY_PREFIX );

        final List<String> testSuiteXmlFiles = properties.getStringList( TEST_SUITE_XML_FILES );
        final File testClassesDirectory = properties.getFileProperty( TEST_CLASSES_DIRECTORY );
        final String runOrder = properties.getProperty( RUN_ORDER );
        final String runStatisticsFile = properties.getProperty( RUN_STATISTICS_FILE );

        final int rerunFailingTestsCount = properties.getIntProperty( RERUN_FAILING_TESTS_COUNT );

        DirectoryScannerParameters dirScannerParams =
            new DirectoryScannerParameters( testClassesDirectory, includes, excludes, specificTests,
                                            properties.getBooleanProperty( FAILIFNOTESTS ), runOrder );

        RunOrderParameters runOrderParameters
                = new RunOrderParameters( runOrder, runStatisticsFile == null ? null : new File( runStatisticsFile ) );

        TestArtifactInfo testNg = new TestArtifactInfo( testNgVersion, testArtifactClassifier );
        TestRequest testSuiteDefinition =
            new TestRequest( testSuiteXmlFiles, sourceDirectory, new TestListResolver( requestedTest ),
                             rerunFailingTestsCount );

        ReporterConfiguration reporterConfiguration =
            new ReporterConfiguration( reportsDirectory, properties.getBooleanProperty( ISTRIMSTACKTRACE ) );

        Collection<String> cli = properties.getStringList( MAIN_CLI_OPTIONS );

        int failFastCount = properties.getIntProperty( FAIL_FAST_COUNT );

        Shutdown shutdown = Shutdown.valueOf( properties.getProperty( SHUTDOWN ) );

        String systemExitTimeoutAsString = properties.getProperty( SYSTEM_EXIT_TIMEOUT );
        Integer systemExitTimeout =
                systemExitTimeoutAsString == null ? null : Integer.valueOf( systemExitTimeoutAsString );

        return new ProviderConfiguration( dirScannerParams, runOrderParameters,
                                          properties.getBooleanProperty( FAILIFNOTESTS ), reporterConfiguration, testNg,
                                          testSuiteDefinition, properties.getProperties(), typeEncodedTestForFork,
                                          preferTestsFromInStream, fromStrings( cli ), failFastCount, shutdown,
                                          systemExitTimeout );
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
