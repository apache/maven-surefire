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
import org.apache.maven.surefire.booter.*;
import org.apache.maven.surefire.cli.CommandLineOption;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.testset.*;
import org.apache.maven.surefire.util.RunOrder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static org.apache.maven.surefire.cli.CommandLineOption.LOGGING_LEVEL_DEBUG;
import static org.apache.maven.surefire.cli.CommandLineOption.REACTOR_FAIL_FAST;
import static org.apache.maven.surefire.cli.CommandLineOption.SHOW_ERRORS;

/**
 * Performs roundtrip testing of serialization/deserialization of the ProviderConfiguration
 *
 * @author Kristian Rosenvold
 */
public class BooterDeserializerProviderConfigurationTest
    extends TestCase
{

    public static final TypeEncodedValue aTestTyped = new TypeEncodedValue( String.class.getName(), "aTest" );

    private static final String aUserRequestedTest = "aUserRequestedTest";

    private static final String aUserRequestedTestMethod = "aUserRequestedTestMethod";

    private static final int rerunFailingTestsCount = 3;

    private final List<CommandLineOption> cli =
        Arrays.asList( LOGGING_LEVEL_DEBUG, SHOW_ERRORS, REACTOR_FAIL_FAST );

    private static ClassLoaderConfiguration getForkConfiguration()
    {
        return new ClassLoaderConfiguration( true, false );
    }

    // ProviderConfiguration methods
    public void testDirectoryScannerParams()
        throws IOException
    {

        File aDir = new File( "." );
        List<String> includes = new ArrayList<String>();
        List<String> excludes = new ArrayList<String>();
        includes.add( "abc" );
        includes.add( "cde" );
        excludes.add( "xx1" );
        excludes.add( "xx2" );

        ClassLoaderConfiguration forkConfiguration = getForkConfiguration();
        final StartupConfiguration testStartupConfiguration = getTestStartupConfiguration( forkConfiguration );
        ProviderConfiguration providerConfiguration = getReloadedProviderConfiguration();
        ProviderConfiguration read = saveAndReload( providerConfiguration, testStartupConfiguration, false );

        Assert.assertEquals( aDir, read.getBaseDir() );
        Assert.assertEquals( includes.get( 0 ), read.getIncludes().get( 0 ) );
        Assert.assertEquals( includes.get( 1 ), read.getIncludes().get( 1 ) );
        Assert.assertEquals( excludes.get( 0 ), read.getExcludes().get( 0 ) );
        Assert.assertEquals( excludes.get( 1 ), read.getExcludes().get( 1 ) );
        assertEquals( cli, providerConfiguration.getMainCliOptions() );
    }

    public void testReporterConfiguration()
        throws IOException
    {
        DirectoryScannerParameters directoryScannerParameters = getDirectoryScannerParametersWithoutSpecificTests();
        ClassLoaderConfiguration forkConfiguration = getForkConfiguration();

        ProviderConfiguration providerConfiguration = getTestProviderConfiguration( directoryScannerParameters, false );

        final StartupConfiguration testProviderConfiguration = getTestStartupConfiguration( forkConfiguration );
        ProviderConfiguration reloaded = saveAndReload( providerConfiguration, testProviderConfiguration, false );

        assertTrue( reloaded.getReporterConfiguration().isTrimStackTrace() );
        assertNotNull( reloaded.getReporterConfiguration().getReportsDirectory() );
        assertEquals( cli, providerConfiguration.getMainCliOptions() );
    }

    public void testTestArtifact()
        throws IOException
    {
        ProviderConfiguration reloaded = getReloadedProviderConfiguration();

        Assert.assertEquals( "5.0", reloaded.getTestArtifact().getVersion() );
        Assert.assertEquals( "ABC", reloaded.getTestArtifact().getClassifier() );
        assertEquals( cli, reloaded.getMainCliOptions() );
    }

    public void testTestRequest()
        throws IOException
    {
        ProviderConfiguration reloaded = getReloadedProviderConfiguration();

        TestRequest testSuiteDefinition = reloaded.getTestSuiteDefinition();
        List<?> suiteXmlFiles = testSuiteDefinition.getSuiteXmlFiles();
        File[] expected = getSuiteXmlFiles();
        Assert.assertEquals( expected[0], suiteXmlFiles.get( 0 ) );
        Assert.assertEquals( expected[1], suiteXmlFiles.get( 1 ) );
        Assert.assertEquals( getTestSourceDirectory(), testSuiteDefinition.getTestSourceDirectory() );
        TestListResolver resolver = testSuiteDefinition.getTestListResolver();
        Assert.assertNotNull( resolver );
        Assert.assertFalse( resolver.isEmpty() );
        Assert.assertEquals( aUserRequestedTest + "#" + aUserRequestedTestMethod, resolver.getPluginParameterTest() );
        Assert.assertFalse( resolver.getIncludedPatterns().isEmpty() );
        Assert.assertTrue( resolver.getExcludedPatterns().isEmpty() );
        Assert.assertEquals( 1, resolver.getIncludedPatterns().size() );
        ResolvedTest filter = resolver.getIncludedPatterns().iterator().next();
        Assert.assertNotNull( filter );
        Assert.assertEquals( "**/" + aUserRequestedTest, filter.getTestClassPattern() );
        Assert.assertEquals( aUserRequestedTestMethod, filter.getTestMethodPattern() );
        Assert.assertEquals( rerunFailingTestsCount, testSuiteDefinition.getRerunFailingTestsCount() );
        assertEquals( cli, reloaded.getMainCliOptions() );
    }

    public void testTestForFork()
        throws IOException
    {
        final ProviderConfiguration reloaded = getReloadedProviderConfiguration();
        Assert.assertEquals( aTestTyped, reloaded.getTestForFork() );
        assertEquals( cli, reloaded.getMainCliOptions() );
    }

    public void testTestForForkWithMultipleFiles()
        throws IOException
    {
        final ProviderConfiguration reloaded = getReloadedProviderConfigurationForReadFromInStream();
        Assert.assertNull( reloaded.getTestForFork() );
        Assert.assertTrue( reloaded.isReadTestsFromInStream() );
        assertEquals( cli, reloaded.getMainCliOptions() );
    }

    public void testFailIfNoTests()
        throws IOException
    {
        ProviderConfiguration reloaded = getReloadedProviderConfiguration();
        assertTrue( reloaded.isFailIfNoTests() );
        assertEquals( cli, reloaded.getMainCliOptions() );
    }

    private ProviderConfiguration getReloadedProviderConfigurationForReadFromInStream()
        throws IOException
    {
        return getReloadedProviderConfiguration( true );
    }

    private ProviderConfiguration getReloadedProviderConfiguration()
        throws IOException
    {
        return getReloadedProviderConfiguration( false );
    }

    private ProviderConfiguration getReloadedProviderConfiguration( boolean readTestsFromInStream )
        throws IOException
    {
        DirectoryScannerParameters directoryScannerParameters = getDirectoryScannerParametersWithoutSpecificTests();
        ClassLoaderConfiguration forkConfiguration = getForkConfiguration();
        ProviderConfiguration booterConfiguration =
            getTestProviderConfiguration( directoryScannerParameters, readTestsFromInStream );
        assertEquals( cli, booterConfiguration.getMainCliOptions() );
        final StartupConfiguration testProviderConfiguration = getTestStartupConfiguration( forkConfiguration );
        return saveAndReload( booterConfiguration, testProviderConfiguration, readTestsFromInStream );
    }

    private DirectoryScannerParameters getDirectoryScannerParametersWithoutSpecificTests()
    {
        File aDir = new File( "." );
        List<String> includes = new ArrayList<String>();
        List<String> excludes = new ArrayList<String>();
        includes.add( "abc" );
        includes.add( "cde" );
        excludes.add( "xx1" );
        excludes.add( "xx2" );

        return new DirectoryScannerParameters( aDir, includes, excludes, Collections.<String>emptyList(), true,
                                               RunOrder.asString( RunOrder.DEFAULT ) );
    }

    private ProviderConfiguration saveAndReload( ProviderConfiguration booterConfiguration,
                                                 StartupConfiguration testProviderConfiguration,
                                                 boolean readTestsFromInStream )
        throws IOException
    {
        final ForkConfiguration forkConfiguration = ForkConfigurationTest.getForkConfiguration( null, null );
        PropertiesWrapper props = new PropertiesWrapper( new HashMap<String, String>() );
        BooterSerializer booterSerializer = new BooterSerializer( forkConfiguration );
        Object test;
        if ( readTestsFromInStream )
        {
            test = null;
        }
        else
        {
            test = "aTest";
        }
        final File propsTest = booterSerializer.serialize( props, booterConfiguration, testProviderConfiguration, test,
                                                           readTestsFromInStream, 51L );
        BooterDeserializer booterDeserializer = new BooterDeserializer( new FileInputStream( propsTest ) );
        assertEquals( 51L, (Object) booterDeserializer.getPluginPid() );
        return booterDeserializer.deserialize();
    }

    private ProviderConfiguration getTestProviderConfiguration( DirectoryScannerParameters directoryScannerParameters,
                                                                boolean readTestsFromInStream )
    {
        File cwd = new File( "." );
        ReporterConfiguration reporterConfiguration = new ReporterConfiguration( cwd, true );
        TestRequest testSuiteDefinition =
            new TestRequest( getSuiteXmlFileStrings(), getTestSourceDirectory(),
                             new TestListResolver( aUserRequestedTest + "#aUserRequestedTestMethod" ),
                             rerunFailingTestsCount );
        RunOrderParameters runOrderParameters = new RunOrderParameters( RunOrder.DEFAULT, null );
        return new ProviderConfiguration( directoryScannerParameters, runOrderParameters, true, reporterConfiguration,
                new TestArtifactInfo( "5.0", "ABC" ), testSuiteDefinition, new HashMap<String, String>(), aTestTyped,
                readTestsFromInStream, cli, 0, Shutdown.DEFAULT, 0 );
    }

    private StartupConfiguration getTestStartupConfiguration( ClassLoaderConfiguration classLoaderConfiguration )
    {
        ClasspathConfiguration classpathConfiguration = new ClasspathConfiguration( true, true );

        return new StartupConfiguration( "com.provider", classpathConfiguration, classLoaderConfiguration, false,
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

    private List<String> getSuiteXmlFileStrings()
    {
        return Arrays.asList( "A1", "A2" );
    }
}
