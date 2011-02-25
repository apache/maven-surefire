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

import org.apache.maven.surefire.booter.BooterDeserializer;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SystemPropertyManager;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Performs roundtrip testing of serialization/deserialization of the ProviderConfiguration
 *
 * @author Kristian Rosenvold
 */
public class BooterDeserializerProviderConfigurationTest
    extends TestCase
{

    private final String aTest = "aTest";

    private final String aUserRequestedTest = "aUserRequestedTest";
    
    private final String aUserRequestedTestMethod = "aUserRequestedTestMethod";

    public static ClassLoaderConfiguration getForkConfiguration()
        throws IOException
    {
        return new ClassLoaderConfiguration( true, false );
    }

    // ProviderConfiguration methods
    public void testDirectoryScannerParams()
        throws IOException
    {

        File aDir = new File( "." );
        List includes = new ArrayList();
        List excludes = new ArrayList();
        includes.add( "abc" );
        includes.add( "cde" );
        excludes.add( "xx1" );
        excludes.add( "xx2" );

        ClassLoaderConfiguration forkConfiguration = getForkConfiguration();
        final StartupConfiguration testStartupConfiguration = getTestStartupConfiguration( forkConfiguration );
        ProviderConfiguration providerConfiguration = getReloadedProviderConfiguration( new ArrayList() );
        ProviderConfiguration read = saveAndReload( providerConfiguration, testStartupConfiguration );

        Assert.assertEquals( aDir, read.getBaseDir() );
        Assert.assertEquals( includes.get( 0 ), read.getIncludes().get( 0 ) );
        Assert.assertEquals( includes.get( 1 ), read.getIncludes().get( 1 ) );
        Assert.assertEquals( excludes.get( 0 ), read.getExcludes().get( 0 ) );
        Assert.assertEquals( excludes.get( 1 ), read.getExcludes().get( 1 ) );

    }

    public void testReporterConfiguration()
        throws IOException
    {
        DirectoryScannerParameters directoryScannerParameters = getDirectoryScannerParameters();
        ClassLoaderConfiguration forkConfiguration = getForkConfiguration();
        List reports = new ArrayList();
        final String first = "abc";
        final String second = "cde";
        final String third = "efg";
        reports.add( first );
        reports.add( second );
        reports.add( third );

        ProviderConfiguration providerConfiguration =
            getTestProviderConfiguration( directoryScannerParameters, reports );

        final ReporterConfiguration reporterConfiguration = providerConfiguration.getReporterConfiguration();
        reporterConfiguration.getReports().add( first );
        reporterConfiguration.getReports().add( second );
        reporterConfiguration.getReports().add( third );

        final StartupConfiguration testProviderConfiguration = getTestStartupConfiguration( forkConfiguration );
        ProviderConfiguration reloaded = saveAndReload( providerConfiguration, testProviderConfiguration );

        Assert.assertEquals( first, reloaded.getReporterConfiguration().getReports().get( 0 ) );
        Assert.assertEquals( second, reloaded.getReporterConfiguration().getReports().get( 1 ) );
        Assert.assertEquals( third, reloaded.getReporterConfiguration().getReports().get( 2 ) );
    }

    public void testTestArtifact()
        throws IOException
    {
        ProviderConfiguration reloaded = getReloadedProviderConfiguration( new ArrayList() );

        Assert.assertEquals( "5.0", reloaded.getTestArtifact().getVersion() );
        Assert.assertEquals( "ABC", reloaded.getTestArtifact().getClassifier() );
    }

    public void testTestRequest()
        throws IOException
    {
        ProviderConfiguration reloaded = getReloadedProviderConfiguration( new ArrayList() );

        TestRequest testSuiteDefinition = reloaded.getTestSuiteDefinition();
        List suiteXmlFiles = testSuiteDefinition.getSuiteXmlFiles();
        File[] expected = getSuiteXmlFiles();
        Assert.assertEquals( expected[0], suiteXmlFiles.get( 0 ) );
        Assert.assertEquals( expected[1], suiteXmlFiles.get( 1 ) );
        Assert.assertEquals( getTestSourceDirectory(), testSuiteDefinition.getTestSourceDirectory() );
        Assert.assertEquals( aUserRequestedTest, testSuiteDefinition.getRequestedTest() );
    }

    public void testTestForFork()
        throws IOException
    {
        final ProviderConfiguration reloaded = getReloadedProviderConfiguration( new ArrayList() );
        Assert.assertEquals( aTest, reloaded.getTestForForkString() );

    }

    public void testFailIfNoTests()
        throws IOException
    {
        ProviderConfiguration reloaded = getReloadedProviderConfiguration( new ArrayList() );
        assertTrue( reloaded.isFailIfNoTests().booleanValue() );

    }

    private ProviderConfiguration getReloadedProviderConfiguration( ArrayList reports )
        throws IOException
    {
        DirectoryScannerParameters directoryScannerParameters = getDirectoryScannerParameters();
        ClassLoaderConfiguration forkConfiguration = getForkConfiguration();
        ProviderConfiguration booterConfiguration = getTestProviderConfiguration( directoryScannerParameters, reports );
        final StartupConfiguration testProviderConfiguration = getTestStartupConfiguration( forkConfiguration );
        return saveAndReload( booterConfiguration, testProviderConfiguration );
    }

    private DirectoryScannerParameters getDirectoryScannerParameters()
    {
        File aDir = new File( "." );
        List includes = new ArrayList();
        List excludes = new ArrayList();
        includes.add( "abc" );
        includes.add( "cde" );
        excludes.add( "xx1" );
        excludes.add( "xx2" );

        return new DirectoryScannerParameters( aDir, includes, excludes, Boolean.TRUE, null );
    }

    private ProviderConfiguration saveAndReload( ProviderConfiguration booterConfiguration,
                                                 StartupConfiguration testProviderConfiguration )
        throws IOException
    {
        final ForkConfiguration forkConfiguration = ForkConfigurationTest.getForkConfiguration();
        Properties props = new Properties();
        BooterSerializer booterSerializer = new BooterSerializer( forkConfiguration, props );
        booterSerializer.serialize( booterConfiguration, testProviderConfiguration, aTest );
        final File propsTest =
            SystemPropertyManager.writePropertiesFile( props, forkConfiguration.getTempDirectory(), "propsTest", true );
        BooterDeserializer booterDeserializer = new BooterDeserializer( new FileInputStream( propsTest ) );
        return booterDeserializer.deserialize();
    }

    private ProviderConfiguration getTestProviderConfiguration( DirectoryScannerParameters directoryScannerParameters,
                                                                List reports )
        throws IOException
    {

        File cwd = new File( "." );
        ReporterConfiguration reporterConfiguration =
            new ReporterConfiguration( reports, cwd, Boolean.TRUE, null );
        TestRequest testSuiteDefinition =
            new TestRequest( getSuiteXmlFileStrings(), getTestSourceDirectory(), aUserRequestedTest, aUserRequestedTestMethod );
        return new ProviderConfiguration( directoryScannerParameters, true, reporterConfiguration,
                                          new TestArtifactInfo( "5.0", "ABC" ), testSuiteDefinition, new Properties(),
                                          aTest );
    }

    private StartupConfiguration getTestStartupConfiguration( ClassLoaderConfiguration classLoaderConfiguration )
    {
        ClasspathConfiguration classpathConfiguration = new ClasspathConfiguration( true, true );

        return new StartupConfiguration( "com.provider", classpathConfiguration, classLoaderConfiguration, false, false,
                                         false );
    }

    private File getTestSourceDirectory()
    {
        return new File( "TestSrc" );
    }

    private File[] getSuiteXmlFiles()
    {
        return new File[]{ new File( "A1" ), new File( "A2" ) };
    }

    private List getSuiteXmlFileStrings()
    {
        return Arrays.asList( new Object[]{ "A1", "A2" } );
    }
}
