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
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.cli.CommandLineOption;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.RunOrder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.apache.maven.surefire.booter.AbstractPathConfiguration.CHILD_DELEGATION;
import static org.apache.maven.surefire.booter.AbstractPathConfiguration.CLASSPATH;
import static org.apache.maven.surefire.booter.AbstractPathConfiguration.ENABLE_ASSERTIONS;
import static org.apache.maven.surefire.booter.AbstractPathConfiguration.SUREFIRE_CLASSPATH;
import static org.apache.maven.surefire.booter.BooterConstants.EXCLUDES_PROPERTY_PREFIX;
import static org.apache.maven.surefire.booter.BooterConstants.FAIL_FAST_COUNT;
import static org.apache.maven.surefire.booter.BooterConstants.FAILIFNOTESTS;
import static org.apache.maven.surefire.booter.BooterConstants.FORKTESTSET;
import static org.apache.maven.surefire.booter.BooterConstants.FORKTESTSET_PREFER_TESTS_FROM_IN_STREAM;
import static org.apache.maven.surefire.booter.BooterConstants.INCLUDES_PROPERTY_PREFIX;
import static org.apache.maven.surefire.booter.BooterConstants.ISTRIMSTACKTRACE;
import static org.apache.maven.surefire.booter.BooterConstants.MAIN_CLI_OPTIONS;
import static org.apache.maven.surefire.booter.BooterConstants.PLUGIN_PID;
import static org.apache.maven.surefire.booter.BooterConstants.PROVIDER_CONFIGURATION;
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
    File serialize( KeyValueSource sourceProperties, ProviderConfiguration booterConfiguration,
                    StartupConfiguration providerConfiguration, Object testSet, boolean readTestsFromInStream,
                    Long pid )
        throws IOException
    {
        SurefireProperties properties = new SurefireProperties( sourceProperties );

        properties.setProperty( PLUGIN_PID, pid );

        AbstractPathConfiguration cp = providerConfiguration.getClasspathConfiguration();
        properties.setClasspath( CLASSPATH, cp.getTestClasspath() );
        properties.setClasspath( SUREFIRE_CLASSPATH, cp.getProviderClasspath() );
        properties.setProperty( ENABLE_ASSERTIONS, toString( cp.isEnableAssertions() ) );
        properties.setProperty( CHILD_DELEGATION, toString( cp.isChildDelegation() ) );

        TestArtifactInfo testNg = booterConfiguration.getTestArtifact();
        if ( testNg != null )
        {
            properties.setProperty( TESTARTIFACT_VERSION, testNg.getVersion() );
            properties.setNullableProperty( TESTARTIFACT_CLASSIFIER, testNg.getClassifier() );
        }

        properties.setProperty( FORKTESTSET_PREFER_TESTS_FROM_IN_STREAM, readTestsFromInStream );
        properties.setNullableProperty( FORKTESTSET, getTypeEncoded( testSet ) );

        TestRequest testSuiteDefinition = booterConfiguration.getTestSuiteDefinition();
        if ( testSuiteDefinition != null )
        {
            properties.setProperty( SOURCE_DIRECTORY, testSuiteDefinition.getTestSourceDirectory() );
            properties.addList( testSuiteDefinition.getSuiteXmlFiles(), TEST_SUITE_XML_FILES );
            TestListResolver testFilter = testSuiteDefinition.getTestListResolver();
            properties.setProperty( REQUESTEDTEST, testFilter == null ? "" : testFilter.getPluginParameterTest() );
            int rerunFailingTestsCount = testSuiteDefinition.getRerunFailingTestsCount();
            properties.setNullableProperty( RERUN_FAILING_TESTS_COUNT, toString( rerunFailingTestsCount ) );
        }

        DirectoryScannerParameters directoryScannerParameters = booterConfiguration.getDirScannerParams();
        if ( directoryScannerParameters != null )
        {
            properties.setProperty( FAILIFNOTESTS, toString( directoryScannerParameters.isFailIfNoTests() ) );
            properties.addList( directoryScannerParameters.getIncludes(), INCLUDES_PROPERTY_PREFIX );
            properties.addList( directoryScannerParameters.getExcludes(), EXCLUDES_PROPERTY_PREFIX );
            properties.addList( directoryScannerParameters.getSpecificTests(), SPECIFIC_TEST_PROPERTY_PREFIX );
            properties.setProperty( TEST_CLASSES_DIRECTORY, directoryScannerParameters.getTestClassesDirectory() );
        }

        final RunOrderParameters runOrderParameters = booterConfiguration.getRunOrderParameters();
        if ( runOrderParameters != null )
        {
            properties.setProperty( RUN_ORDER, RunOrder.asString( runOrderParameters.getRunOrder() ) );
            properties.setProperty( RUN_STATISTICS_FILE, runOrderParameters.getRunStatisticsFile() );
        }

        ReporterConfiguration reporterConfiguration = booterConfiguration.getReporterConfiguration();
        boolean rep = reporterConfiguration.isTrimStackTrace();
        properties.setProperty( ISTRIMSTACKTRACE, rep );
        properties.setProperty( REPORTSDIRECTORY, reporterConfiguration.getReportsDirectory() );
        ClassLoaderConfiguration classLoaderConfig = providerConfiguration.getClassLoaderConfiguration();
        properties.setProperty( USESYSTEMCLASSLOADER, toString( classLoaderConfig.isUseSystemClassLoader() ) );
        properties.setProperty( USEMANIFESTONLYJAR, toString( classLoaderConfig.isUseManifestOnlyJar() ) );
        properties.setProperty( FAILIFNOTESTS, toString( booterConfiguration.isFailIfNoTests() ) );
        properties.setProperty( PROVIDER_CONFIGURATION, providerConfiguration.getProviderClassName() );
        properties.setProperty( FAIL_FAST_COUNT, toString( booterConfiguration.getSkipAfterFailureCount() ) );
        properties.setProperty( SHUTDOWN, booterConfiguration.getShutdown().name() );
        List<CommandLineOption> mainCliOptions = booterConfiguration.getMainCliOptions();
        if ( mainCliOptions != null )
        {
            properties.addList( mainCliOptions, MAIN_CLI_OPTIONS );
        }
        properties.setNullableProperty( SYSTEM_EXIT_TIMEOUT, toString( booterConfiguration.getSystemExitTimeout() ) );

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
