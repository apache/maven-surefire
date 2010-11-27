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

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.maven.surefire.booter.BooterConfiguration;
import org.apache.maven.surefire.booter.BooterDeserializer;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.providerapi.ProviderConfiguration;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestSuiteDefinition;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Performs roundtrip testing of serialization/deserialization
 *
 * @author Kristian Rosenvold
 */
public class BooterDeserializerTest
    extends TestCase
{

    private final String aTest = "aTest";
    private final String aUserRequestedTest = "aUserRequestedTest";

    public static ClassLoaderConfiguration getForkConfiguration()
        throws IOException
    {
        return new ClassLoaderConfiguration( true, false );
    }

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

        DirectoryScannerParameters directoryScannerParameters = new DirectoryScannerParameters( aDir, includes, excludes, Boolean.TRUE );
        ClassLoaderConfiguration forkConfiguration = getForkConfiguration();
        BooterConfiguration booterConfiguration = getTestBooterConfiguration( forkConfiguration, directoryScannerParameters,
                                                                              new ArrayList(  ));
        BooterConfiguration read = saveAndReload( forkConfiguration, booterConfiguration );


        Assert.assertEquals( aDir, read.getBaseDir() );
        Assert.assertEquals( includes.get( 0 ), read.getIncludes().get( 0 ) );
        Assert.assertEquals( includes.get( 1 ), read.getIncludes().get( 1 ) );
        Assert.assertEquals( excludes.get( 0 ), read.getExcludes().get( 0 ) );
        Assert.assertEquals( excludes.get( 1 ), read.getExcludes().get( 1 ) );

    }

    public void testReports()
        throws IOException
    {
        DirectoryScannerParameters directoryScannerParameters = getDirectoryScannerParameters();
        ClassLoaderConfiguration forkConfiguration = getForkConfiguration();
        List reports = new ArrayList(  );
        reports.add( "abc" );
        reports.add( "cde" );
        reports.add( "efg" );

        BooterConfiguration booterConfiguration = getTestBooterConfiguration( forkConfiguration, directoryScannerParameters, reports );

        booterConfiguration.getReports().add( "abc" );
        booterConfiguration.getReports().add( "cde" );
        booterConfiguration.getReports().add( "efg" );

        BooterConfiguration reloaded = saveAndReload( forkConfiguration, booterConfiguration );

        Assert.assertEquals( "abc", reloaded.getReports().get( 0 ) );
        Assert.assertEquals( "cde", reloaded.getReports().get( 1 ) );
        Assert.assertEquals( "efg", reloaded.getReports().get( 2 ) );
    }

    public void testTestNgArtifact()
        throws IOException
    {
        BooterConfiguration reloaded = getReloladedConfig();

        Assert.assertEquals( "5.0", reloaded.getTestNg().getVersion());
        Assert.assertEquals( "ABC", reloaded.getTestNg().getClassifier());
    }

    public void testTestSuiteDefinition()
        throws IOException
    {
        BooterConfiguration reloaded = getReloladedConfig();

        TestSuiteDefinition testSuiteDefinition = reloaded.getTestSuiteDefinition();
        File[] suiteXmlFiles = testSuiteDefinition.getSuiteXmlFiles();
        File[] expected = getSuiteXmlFiles();
        Assert.assertEquals( expected[0], suiteXmlFiles[0] );
        Assert.assertEquals( expected[1], suiteXmlFiles[1] );
        Assert.assertEquals( aTest, testSuiteDefinition.getTestForFork() );
        Assert.assertEquals( getTEstSourceDirectory(), testSuiteDefinition.getTestSourceDirectory() );
        Assert.assertEquals( aUserRequestedTest, testSuiteDefinition.getRequestedTest() );


    }
    public void testProvider()
        throws IOException
    {
        BooterConfiguration reloaded = getReloladedConfig();

        assertEquals("com.provider", reloaded.getProviderConfiguration().getClassName());

    }

    public void testFailIfNoTests()
        throws IOException
    {
        BooterConfiguration reloaded = getReloladedConfig();
        assertTrue(reloaded.isFailIfNoTests().booleanValue());

    }

    private BooterConfiguration getReloladedConfig()
        throws IOException
    {
        DirectoryScannerParameters directoryScannerParameters = getDirectoryScannerParameters();
        ClassLoaderConfiguration forkConfiguration = getForkConfiguration();
        BooterConfiguration booterConfiguration = getTestBooterConfiguration( forkConfiguration, directoryScannerParameters,
                                                                              new ArrayList(  ) );
        return saveAndReload( forkConfiguration, booterConfiguration );
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

        return new DirectoryScannerParameters( aDir, includes, excludes, Boolean.TRUE );
    }

    private BooterConfiguration saveAndReload( ClassLoaderConfiguration forkConfiguration,
                                               BooterConfiguration booterConfiguration )
        throws IOException
    {
        BooterSerializer booterSerializer = new BooterSerializer();
        Properties props = new Properties();
        booterSerializer.setForkProperties( props, booterConfiguration, forkConfiguration );
        final File propsTest = booterSerializer.writePropertiesFile( "propsTest", props, false, null );
        BooterDeserializer booterDeserializer = new BooterDeserializer();
        return booterDeserializer.deserialize( new FileInputStream( propsTest ) );
    }

    private BooterConfiguration getTestBooterConfiguration( ClassLoaderConfiguration forkConfiguration,
                                                            DirectoryScannerParameters directoryScannerParameters,
                                                            List reports )
        throws IOException
    {
        ClasspathConfiguration classpathConfiguration = new ClasspathConfiguration( true, true );

        ReporterConfiguration reporterConfiguration = new ReporterConfiguration( new File( "." ), Boolean.TRUE );
        TestSuiteDefinition testSuiteDefinition = new TestSuiteDefinition( getSuiteXmlFiles(), aTest,
                                                                           getTEstSourceDirectory(), aUserRequestedTest );
        ProviderConfiguration providerConfiguration = new ProviderConfiguration( "com.provider" );
        return new BooterConfiguration( new Properties(), false, forkConfiguration, classpathConfiguration, false,
                                        reporterConfiguration, new TestArtifactInfo( "5.0", "ABC" ),
                                 testSuiteDefinition, directoryScannerParameters, true, reports, providerConfiguration );
    }

    private File getTEstSourceDirectory()
    {
        return new File( "TestSrc" );
    }

    private File[] getSuiteXmlFiles()
    {
        return new File[]{new File("A1"), new File("A2")};
    }
}
