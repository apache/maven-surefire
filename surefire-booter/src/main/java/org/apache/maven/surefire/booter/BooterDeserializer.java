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
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
 * @version $Id$
 */
public class BooterDeserializer
    implements BooterConstants
{


    private final PropertiesWrapper properties;

    public BooterDeserializer( InputStream inputStream )
        throws IOException
    {
        properties = SystemPropertyManager.loadProperties( inputStream );
    }

    public ProviderConfiguration deserialize()
        throws IOException
    {

        final File reportsDirectory = new File( properties.getProperty( REPORTSDIRECTORY ) );
        Integer timeout = properties.getIntegerObjectProperty( FORKTIMEOUT );
        final String testNgVersion = properties.getProperty( TESTARTIFACT_VERSION );
        final String testArtifactClassifier = properties.getProperty( TESTARTIFACT_CLASSIFIER );
        final Object testForFork = properties.getTypeDecoded( FORKTESTSET );
        final String requestedTest = properties.getProperty( REQUESTEDTEST );
        final String requestedTestMethod = properties.getProperty( REQUESTEDTESTMETHOD );
        final File sourceDirectory = properties.getFileProperty( SOURCE_DIRECTORY );

        final List reports = properties.getStringList( REPORT_PROPERTY_PREFIX );
        final List excludesList = properties.getStringList( EXCLUDES_PROPERTY_PREFIX );
        final List includesList = properties.getStringList( INCLUDES_PROPERTY_PREFIX );

        final List testSuiteXmlFiles = properties.getStringList( TEST_SUITE_XML_FILES );
        final File testClassesDirectory = properties.getFileProperty( TEST_CLASSES_DIRECTORY );
        final String runOrder = properties.getProperty( RUN_ORDER );

        DirectoryScannerParameters dirScannerParams =
            new DirectoryScannerParameters( testClassesDirectory, includesList, excludesList,
                                            properties.getBooleanObjectProperty( FAILIFNOTESTS ), runOrder );

        TestArtifactInfo testNg = new TestArtifactInfo( testNgVersion, testArtifactClassifier );
        TestRequest testSuiteDefinition = new TestRequest( testSuiteXmlFiles, sourceDirectory, requestedTest, requestedTestMethod );

        ReporterConfiguration reporterConfiguration = new ReporterConfiguration( reports, reportsDirectory,
                                                                                 properties.getBooleanObjectProperty(
                                                                                     ISTRIMSTACKTRACE ), timeout );

        return new ProviderConfiguration( dirScannerParams, properties.getBooleanProperty( FAILIFNOTESTS ),
                                          reporterConfiguration, testNg, testSuiteDefinition,
                                          properties.getProperties(), testForFork );
    }

    public StartupConfiguration getProviderConfiguration()
        throws IOException
    {
        boolean enableAssertions = properties.getBooleanProperty( ENABLE_ASSERTIONS );
        boolean childDelegation = properties.getBooleanProperty( CHILD_DELEGATION );
        boolean useSystemClassLoader = properties.getBooleanProperty( USESYSTEMCLASSLOADER );
        boolean useManifestOnlyJar = properties.getBooleanProperty( USEMANIFESTONLYJAR );
        String providerConfiguration = properties.getProperty( PROVIDER_CONFIGURATION );

        Classpath classpath = Classpath.readFromForkProperties( properties, CLASSPATH_URL );
        Classpath sureFireClasspath = Classpath.readFromForkProperties( properties, SUREFIRE_CLASSPATHURL );

        ClassLoaderConfiguration classLoaderConfiguration =
            new ClassLoaderConfiguration( useSystemClassLoader, useManifestOnlyJar );

        ClasspathConfiguration classpathConfiguration =
            new ClasspathConfiguration( classpath, sureFireClasspath, enableAssertions, childDelegation );

        return StartupConfiguration.inForkedVm( providerConfiguration, classpathConfiguration,
                                                classLoaderConfiguration );
    }
}
