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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import org.apache.maven.surefire.booter.BooterDeserializer;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.PropertiesWrapper;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.testset.TestArtifactInfo;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.RunOrder;

import junit.framework.TestCase;

/**
 * Performs roundtrip testing of serialization/deserialization of The StartupConfiguration
 *
 * @author Kristian Rosenvold
 */
public class BooterDeserializerStartupConfigurationTest
    extends TestCase
{
    private final ClasspathConfiguration classpathConfiguration = createClasspathConfiguration();

    public void testProvider()
        throws IOException
    {
        assertEquals( "com.provider", getReloadedStartupConfiguration().getProviderClassName() );
    }

    public void testClassPathConfiguration()
        throws IOException
    {
        ClasspathConfiguration reloadedClasspathConfiguration =
            getReloadedStartupConfiguration().getClasspathConfiguration();
        assertEquals( classpathConfiguration, reloadedClasspathConfiguration );
    }

    private void assertEquals( ClasspathConfiguration expectedConfiguration,
                               ClasspathConfiguration actualConfiguration )
    {
        assertEquals( expectedConfiguration.getTestClasspath().getClassPath(),
                      actualConfiguration.getTestClasspath().getClassPath() );
        assertEquals( expectedConfiguration.isEnableAssertions(), actualConfiguration.isEnableAssertions() );
        assertEquals(  expectedConfiguration.isChildDelegation(), actualConfiguration.isChildDelegation() );
        assertEquals(  expectedConfiguration.getProviderClasspath(), actualConfiguration.getProviderClasspath() );
        assertEquals(  expectedConfiguration.getTestClasspath(), actualConfiguration.getTestClasspath() );
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

    private ClasspathConfiguration createClasspathConfiguration()
    {
        Classpath testClassPath = new Classpath( Arrays.asList( "CP1", "CP2" ) );
        Classpath providerClasspath = new Classpath( Arrays.asList( "SP1", "SP2" ) );
        return new ClasspathConfiguration( testClassPath, providerClasspath, Classpath.emptyClasspath(), true, true );
    }

    public static ClassLoaderConfiguration getSystemClassLoaderConfiguration()
    {
        return new ClassLoaderConfiguration( true, false );
    }

    public static ClassLoaderConfiguration getManifestOnlyJarForkConfiguration()
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
        final ForkConfiguration forkConfiguration = ForkConfigurationTest.getForkConfiguration( null, null );
        PropertiesWrapper props = new PropertiesWrapper( new Properties() );
        BooterSerializer booterSerializer = new BooterSerializer( forkConfiguration );
        String aTest = "aTest";
        final File propsTest =
            booterSerializer.serialize( props, getProviderConfiguration(), startupConfiguration, aTest, false );
        BooterDeserializer booterDeserializer = new BooterDeserializer( new FileInputStream( propsTest ) );
        return booterDeserializer.getProviderConfiguration();
    }

    private ProviderConfiguration getProviderConfiguration()
    {

        File cwd = new File( "." );
        DirectoryScannerParameters directoryScannerParameters =
            new DirectoryScannerParameters( cwd, new ArrayList<String>(), new ArrayList<String>(),
                                            new ArrayList<String>(), Boolean.TRUE, "hourly" );
        ReporterConfiguration reporterConfiguration = new ReporterConfiguration( cwd, Boolean.TRUE );
        String aUserRequestedTest = "aUserRequestedTest";
        String aUserRequestedTestMethod = "aUserRequestedTestMethod";
        TestRequest testSuiteDefinition =
            new TestRequest( Arrays.asList( getSuiteXmlFileStrings() ), getTestSourceDirectory(), aUserRequestedTest,
                             aUserRequestedTestMethod );

        RunOrderParameters runOrderParameters = new RunOrderParameters( RunOrder.DEFAULT, null );
        return new ProviderConfiguration( directoryScannerParameters, runOrderParameters, true, reporterConfiguration,
                                          new TestArtifactInfo( "5.0", "ABC" ), testSuiteDefinition, new Properties(),
                                          BooterDeserializerProviderConfigurationTest.aTestTyped, true,
                                          false, null );
    }

    private StartupConfiguration getTestStartupConfiguration( ClassLoaderConfiguration classLoaderConfiguration )
    {
        return new StartupConfiguration( "com.provider", classpathConfiguration, classLoaderConfiguration, false,
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
