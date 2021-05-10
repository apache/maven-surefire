package org.apache.maven.plugin.surefire.booterclient;

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

import org.apache.maven.plugin.surefire.SurefireProperties;
import org.apache.maven.surefire.booter.AbstractPathConfiguration;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.KeyValueSource;
import org.apache.maven.surefire.booter.ProcessCheckerType;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.api.cli.CommandLineOption;
import org.apache.maven.surefire.api.report.ReporterConfiguration;
import org.apache.maven.surefire.api.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.testset.TestArtifactInfo;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestRequest;
import org.apache.maven.surefire.api.util.RunOrder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.apache.maven.plugin.surefire.SurefireHelper.replaceForkThreadsInPath;
import static org.apache.maven.surefire.booter.AbstractPathConfiguration.CHILD_DELEGATION;
import static org.apache.maven.surefire.booter.AbstractPathConfiguration.CLASSPATH;
import static org.apache.maven.surefire.booter.AbstractPathConfiguration.ENABLE_ASSERTIONS;
import static org.apache.maven.surefire.booter.AbstractPathConfiguration.SUREFIRE_CLASSPATH;
import static org.apache.maven.surefire.booter.BooterConstants.EXCLUDES_PROPERTY_PREFIX;
import static org.apache.maven.surefire.booter.BooterConstants.FAIL_FAST_COUNT;
import static org.apache.maven.surefire.booter.BooterConstants.FORKTESTSET;
import static org.apache.maven.surefire.booter.BooterConstants.FORKTESTSET_PREFER_TESTS_FROM_IN_STREAM;
import static org.apache.maven.surefire.booter.BooterConstants.FORK_NUMBER;
import static org.apache.maven.surefire.booter.BooterConstants.INCLUDES_PROPERTY_PREFIX;
import static org.apache.maven.surefire.booter.BooterConstants.ISTRIMSTACKTRACE;
import static org.apache.maven.surefire.booter.BooterConstants.MAIN_CLI_OPTIONS;
import static org.apache.maven.surefire.booter.BooterConstants.PLUGIN_PID;
import static org.apache.maven.surefire.booter.BooterConstants.PROCESS_CHECKER;
import static org.apache.maven.surefire.booter.BooterConstants.PROVIDER_CONFIGURATION;
import static org.apache.maven.surefire.booter.BooterConstants.RUN_ORDER_RANDOM_SEED;
import static org.apache.maven.surefire.booter.BooterConstants.REPORTSDIRECTORY;
import static org.apache.maven.surefire.booter.BooterConstants.REQUESTEDTEST;
import static org.apache.maven.surefire.booter.BooterConstants.RERUN_FAILING_TESTS_COUNT;
import static org.apache.maven.surefire.booter.BooterConstants.RUN_ORDER;
import static org.apache.maven.surefire.booter.BooterConstants.RUN_STATISTICS_FILE;
import static org.apache.maven.surefire.booter.BooterConstants.SHUTDOWN;
import static org.apache.maven.surefire.booter.BooterConstants.SOURCE_DIRECTORY;
import static org.apache.maven.surefire.booter.BooterConstants.SPECIFIC_TEST_PROPERTY_PREFIX;
import static org.apache.maven.surefire.booter.BooterConstants.SYSTEM_EXIT_TIMEOUT;
import static org.apache.maven.surefire.booter.BooterConstants.TEST_CLASSES_DIRECTORY;
import static org.apache.maven.surefire.booter.BooterConstants.TEST_SUITE_XML_FILES;
import static org.apache.maven.surefire.booter.BooterConstants.TESTARTIFACT_CLASSIFIER;
import static org.apache.maven.surefire.booter.BooterConstants.TESTARTIFACT_VERSION;
import static org.apache.maven.surefire.booter.BooterConstants.USEMANIFESTONLYJAR;
import static org.apache.maven.surefire.booter.BooterConstants.USESYSTEMCLASSLOADER;
import static org.apache.maven.surefire.booter.BooterConstants.FORK_NODE_CONNECTION_STRING;
import static org.apache.maven.surefire.booter.SystemPropertyManager.writePropertiesFile;

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
 * @author Brett Porter
 * @author Dan Fabulich
 * @author Kristian Rosenvold
 */
class BooterSerializer
{
    private final ForkConfiguration forkConfiguration;

    BooterSerializer( ForkConfiguration forkConfiguration )
    {
        this.forkConfiguration = forkConfiguration;
    }

    /**
     * Does not modify sourceProperties
     */
    File serialize( KeyValueSource sourceProperties, ProviderConfiguration providerConfiguration,
                    StartupConfiguration startupConfiguration, Object testSet, boolean readTestsFromInStream,
                    Long pid, int forkNumber, String forkNodeConnectionString )
        throws IOException
    {
        SurefireProperties properties = new SurefireProperties( sourceProperties );
        properties.setNullableProperty( FORK_NODE_CONNECTION_STRING, forkNodeConnectionString );
        properties.setProperty( PLUGIN_PID, pid );

        AbstractPathConfiguration cp = startupConfiguration.getClasspathConfiguration();
        properties.setClasspath( CLASSPATH, cp.getTestClasspath() );
        properties.setClasspath( SUREFIRE_CLASSPATH, cp.getProviderClasspath() );
        properties.setProperty( ENABLE_ASSERTIONS, toString( cp.isEnableAssertions() ) );
        properties.setProperty( CHILD_DELEGATION, toString( cp.isChildDelegation() ) );
        ProcessCheckerType processChecker = startupConfiguration.getProcessChecker();
        properties.setNullableProperty( PROCESS_CHECKER, processChecker == null ? null : processChecker.getType() );

        TestArtifactInfo testNg = providerConfiguration.getTestArtifact();
        if ( testNg != null )
        {
            properties.setProperty( TESTARTIFACT_VERSION, testNg.getVersion() );
            properties.setNullableProperty( TESTARTIFACT_CLASSIFIER, testNg.getClassifier() );
        }

        properties.setProperty( FORKTESTSET_PREFER_TESTS_FROM_IN_STREAM, readTestsFromInStream );
        properties.setNullableProperty( FORKTESTSET, getTypeEncoded( testSet ) );

        TestRequest testSuiteDefinition = providerConfiguration.getTestSuiteDefinition();
        if ( testSuiteDefinition != null )
        {
            properties.setProperty( SOURCE_DIRECTORY, testSuiteDefinition.getTestSourceDirectory() );
            if ( testSet instanceof File )
            {
                properties.addList( Collections.singletonList( (File) testSet ), TEST_SUITE_XML_FILES );
            }
            else
            {
                properties.addList( testSuiteDefinition.getSuiteXmlFiles(), TEST_SUITE_XML_FILES );
            }
            TestListResolver testFilter = testSuiteDefinition.getTestListResolver();
            properties.setProperty( REQUESTEDTEST, testFilter == null ? "" : testFilter.getPluginParameterTest() );
            int rerunFailingTestsCount = testSuiteDefinition.getRerunFailingTestsCount();
            properties.setNullableProperty( RERUN_FAILING_TESTS_COUNT, toString( rerunFailingTestsCount ) );
        }

        DirectoryScannerParameters directoryScannerParameters = providerConfiguration.getDirScannerParams();
        if ( directoryScannerParameters != null )
        {
            properties.addList( directoryScannerParameters.getIncludes(), INCLUDES_PROPERTY_PREFIX );
            properties.addList( directoryScannerParameters.getExcludes(), EXCLUDES_PROPERTY_PREFIX );
            properties.addList( directoryScannerParameters.getSpecificTests(), SPECIFIC_TEST_PROPERTY_PREFIX );
            properties.setProperty( TEST_CLASSES_DIRECTORY, directoryScannerParameters.getTestClassesDirectory() );
        }

        final RunOrderParameters runOrderParameters = providerConfiguration.getRunOrderParameters();
        if ( runOrderParameters != null )
        {
            properties.setProperty( RUN_ORDER, RunOrder.asString( runOrderParameters.getRunOrder() ) );
            properties.setProperty( RUN_STATISTICS_FILE, runOrderParameters.getRunStatisticsFile() );
            properties.setProperty( RUN_ORDER_RANDOM_SEED, runOrderParameters.getRunOrderRandomSeed() );
        }

        ReporterConfiguration reporterConfiguration = providerConfiguration.getReporterConfiguration();
        boolean rep = reporterConfiguration.isTrimStackTrace();
        File reportsDirectory = replaceForkThreadsInPath( reporterConfiguration.getReportsDirectory(), forkNumber );
        properties.setProperty( FORK_NUMBER, forkNumber );
        properties.setProperty( ISTRIMSTACKTRACE, rep );
        properties.setProperty( REPORTSDIRECTORY, reportsDirectory );
        ClassLoaderConfiguration classLoaderConfig = startupConfiguration.getClassLoaderConfiguration();
        properties.setProperty( USESYSTEMCLASSLOADER, toString( classLoaderConfig.isUseSystemClassLoader() ) );
        properties.setProperty( USEMANIFESTONLYJAR, toString( classLoaderConfig.isUseManifestOnlyJar() ) );
        properties.setProperty( PROVIDER_CONFIGURATION, startupConfiguration.getProviderClassName() );
        properties.setProperty( FAIL_FAST_COUNT, toString( providerConfiguration.getSkipAfterFailureCount() ) );
        properties.setProperty( SHUTDOWN, providerConfiguration.getShutdown().name() );
        List<CommandLineOption> mainCliOptions = providerConfiguration.getMainCliOptions();
        if ( mainCliOptions != null )
        {
            properties.addList( mainCliOptions, MAIN_CLI_OPTIONS );
        }
        properties.setNullableProperty( SYSTEM_EXIT_TIMEOUT, toString( providerConfiguration.getSystemExitTimeout() ) );

        File surefireTmpDir = forkConfiguration.getTempDirectory();
        boolean debug = forkConfiguration.isDebug();
        return writePropertiesFile( properties, surefireTmpDir, "surefire", debug );
    }

    private static String getTypeEncoded( Object value )
    {
        if ( value == null )
        {
            return null;
        }
        String valueToUse = value instanceof Class ? ( (Class<?>) value ).getName() : value.toString();
        return value.getClass().getName() + "|" + valueToUse;
    }

    private static String toString( Object o )
    {
        return String.valueOf( o );
    }
}
