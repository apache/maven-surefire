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

import org.apache.maven.surefire.booter.BooterConstants;
import org.apache.maven.surefire.booter.BooterDeserializer;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
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

import junit.framework.TestCase;

/**
 * Performs roundtrip testing of serialization/deserialization of The StartupConfiguration
 *
 * @author Kristian Rosenvold
 */
public class BooterDeserializerStartupConfigurationTest
    extends TestCase
{

    private final String aTest = "aTest";

    private final String aUserRequestedTest = "aUserRequestedTest";

    public void testProvider()
        throws IOException
    {
        assertEquals( "com.provider", getReloadedStartupConfiguration().getProviderClassName() );
    }

    public void testClassPathConfiguration()
        throws IOException
    {
        final ClasspathConfiguration classpathConfiguration =
            getReloadedStartupConfiguration().getClasspathConfiguration();
        Properties props = new Properties();
        classpathConfiguration.setForkProperties( props );
        List testClassPathUrls = classpathConfiguration.getTestClasspath().getClassPath();
        assertEquals( "true", props.get( BooterConstants.ENABLE_ASSERTIONS ) );
        assertEquals( "true", props.get( BooterConstants.CHILD_DELEGATION ) );
        assertEquals( 2, testClassPathUrls.size() );
        assertEquals( "T1", testClassPathUrls.get( 0 ) );
        assertEquals( "T2", testClassPathUrls.get( 1 ) );
        assertEquals( "P1", props.get( BooterConstants.SUREFIRE_CLASSPATHURL + "0" ) );
        assertEquals( "P2", props.get( BooterConstants.SUREFIRE_CLASSPATHURL + "1" ) );
    }

    public void testClassLoaderConfiguration()
        throws IOException
    {
        assertFalse( getReloadedStartupConfiguration().isManifestOnlyJarRequestedAndUsable() );
    }

    public void testClassLoaderConfigurationTrues()
        throws IOException
    {
        final StartupConfiguration testStartupConfiguration =
            getTestStartupConfiguration( getManifestOnlyJarForkConfiguration() );
        boolean current = testStartupConfiguration.isManifestOnlyJarRequestedAndUsable();
        assertEquals( current, saveAndReload( testStartupConfiguration ).isManifestOnlyJarRequestedAndUsable() );
    }


    public static ClassLoaderConfiguration getSystemClassLoaderConfiguration()
        throws IOException
    {
        return new ClassLoaderConfiguration( true, false );
    }

    public static ClassLoaderConfiguration getManifestOnlyJarForkConfiguration()
        throws IOException
    {
        return new ClassLoaderConfiguration( true, true );
    }


    private StartupConfiguration getReloadedStartupConfiguration()
        throws IOException
    {
        ClassLoaderConfiguration classLoaderConfiguration = getSystemClassLoaderConfiguration();
        return saveAndReload( getTestStartupConfiguration( classLoaderConfiguration ) );
    }

    private StartupConfiguration saveAndReload( StartupConfiguration startupConfiguration )
        throws IOException
    {
        final ForkConfiguration forkConfiguration = ForkConfigurationTest.getForkConfiguration();
        Properties props = new Properties();
        BooterSerializer booterSerializer = new BooterSerializer( forkConfiguration, props );
        booterSerializer.serialize( getProviderConfiguration(), startupConfiguration, aTest );
        final File propsTest =
            SystemPropertyManager.writePropertiesFile( props, forkConfiguration.getTempDirectory(), "propsTest", true );
        BooterDeserializer booterDeserializer = new BooterDeserializer( new FileInputStream( propsTest ) );
        return booterDeserializer.getProviderConfiguration();
    }

    private ProviderConfiguration getProviderConfiguration()
        throws IOException
    {

        File cwd = new File( "." );
        DirectoryScannerParameters directoryScannerParameters =
            new DirectoryScannerParameters( cwd, new ArrayList(), new ArrayList(), Boolean.TRUE, "hourly" );
        ReporterConfiguration reporterConfiguration =
            new ReporterConfiguration( new ArrayList(), cwd, Boolean.TRUE, null );
        TestRequest testSuiteDefinition =
            new TestRequest( Arrays.asList( getSuiteXmlFileStrings() ), getTestSourceDirectory(), aUserRequestedTest );
        return new ProviderConfiguration( directoryScannerParameters, true, reporterConfiguration,
                                          new TestArtifactInfo( "5.0", "ABC" ), testSuiteDefinition, new Properties(),
                                          aTest );
    }

    private StartupConfiguration getTestStartupConfiguration( ClassLoaderConfiguration classLoaderConfiguration )
    {
        Classpath testClasspath = new Classpath( Arrays.asList( new String[]{ "T1", "T2" } ) );
        Classpath providerClasspath = new Classpath( Arrays.asList( new String[]{ "P1", "P2" } ) );
        Classpath testFrameworkClasspath = new Classpath();
        ClasspathConfiguration classpathConfiguration =
            new ClasspathConfiguration( testClasspath, providerClasspath, testFrameworkClasspath, true, true );
        return new StartupConfiguration( "com.provider", classpathConfiguration, classLoaderConfiguration, false, false,
                                         false );
    }

    private File getTestSourceDirectory()
    {
        return new File( "TestSrc" );
    }

    private Object[] getSuiteXmlFileStrings()
    {
        return new Object[]{ "A1", "A2" };
    }
}
