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
import org.apache.maven.surefire.shared.io.FileUtils;
import org.apache.maven.surefire.booter.BooterDeserializer;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.PropertiesWrapper;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.api.booter.Shutdown;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.TypeEncodedValue;
import org.apache.maven.surefire.api.cli.CommandLineOption;
import org.apache.maven.surefire.api.report.ReporterConfiguration;
import org.apache.maven.surefire.api.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.api.testset.ResolvedTest;
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.testset.TestArtifactInfo;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestRequest;
import org.apache.maven.surefire.api.util.RunOrder;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.apache.maven.surefire.booter.ProcessCheckerType.ALL;
import static org.apache.maven.surefire.api.cli.CommandLineOption.LOGGING_LEVEL_DEBUG;
import static org.apache.maven.surefire.api.cli.CommandLineOption.REACTOR_FAIL_FAST;
import static org.apache.maven.surefire.api.cli.CommandLineOption.SHOW_ERRORS;

/**
 * Performs roundtrip testing of serialization/deserialization of the ProviderConfiguration
 *
 * @author Kristian Rosenvold
 */
public class BooterDeserializerProviderConfigurationTest
    extends TestCase
{

    static final TypeEncodedValue TEST_TYPED = new TypeEncodedValue( String.class.getName(), "aTest" );

    private static final String USER_REQUESTED_TEST = "aUserRequestedTest";

    private static final String USER_REQUESTED_TEST_METHOD = "aUserRequestedTestMethod";

    private static final int RERUN_FAILING_TEST_COUNT = 3;

    private static int idx = 0;

    private File basedir;

    private final List<CommandLineOption> cli =
        Arrays.asList( LOGGING_LEVEL_DEBUG, SHOW_ERRORS, REACTOR_FAIL_FAST );

    @Before
    public void setupDirectories() throws IOException
    {
        File target = new File( System.getProperty( "user.dir" ), "target" );
        basedir = new File( target, "BooterDeserializerProviderConfigurationTest-" + ++idx );
        FileUtils.deleteDirectory( basedir );
        assertTrue( basedir.mkdirs() );
    }

    @After
    public void deleteDirectories() throws IOException
    {
        FileUtils.deleteDirectory( basedir );
    }

    private static ClassLoaderConfiguration getForkConfiguration()
    {
        return new ClassLoaderConfiguration( true, false );
    }

    // ProviderConfiguration methods
    public void testDirectoryScannerParams()
        throws IOException
    {

        File aDir = new File( "." );
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
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
        Assert.assertEquals( USER_REQUESTED_TEST + "#" + USER_REQUESTED_TEST_METHOD,
                resolver.getPluginParameterTest() );
        Assert.assertFalse( resolver.getIncludedPatterns().isEmpty() );
        Assert.assertTrue( resolver.getExcludedPatterns().isEmpty() );
        Assert.assertEquals( 1, resolver.getIncludedPatterns().size() );
        ResolvedTest filter = resolver.getIncludedPatterns().iterator().next();
        Assert.assertNotNull( filter );
        Assert.assertEquals( "**/" + USER_REQUESTED_TEST, filter.getTestClassPattern() );
        Assert.assertEquals( USER_REQUESTED_TEST_METHOD, filter.getTestMethodPattern() );
        Assert.assertEquals( RERUN_FAILING_TEST_COUNT, testSuiteDefinition.getRerunFailingTestsCount() );
        assertEquals( cli, reloaded.getMainCliOptions() );
    }

    public void testTestForFork()
        throws IOException
    {
        final ProviderConfiguration reloaded = getReloadedProviderConfiguration();
        Assert.assertEquals( TEST_TYPED, reloaded.getTestForFork() );
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
        List<String> includes = new ArrayList<>();
        List<String> excludes = new ArrayList<>();
        includes.add( "abc" );
        includes.add( "cde" );
        excludes.add( "xx1" );
        excludes.add( "xx2" );

        return new DirectoryScannerParameters( aDir, includes, excludes, Collections.<String>emptyList(),
                                               RunOrder.asString( RunOrder.DEFAULT ) );
    }

    private ProviderConfiguration saveAndReload( ProviderConfiguration booterConfiguration,
                                                 StartupConfiguration testProviderConfiguration,
                                                 boolean readTestsFromInStream )
        throws IOException
    {
        final ForkConfiguration forkConfiguration = ForkConfigurationTest.getForkConfiguration( basedir, null );
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
                                                           readTestsFromInStream, 51L, 1, "pipe://1" );
        BooterDeserializer booterDeserializer = new BooterDeserializer( new FileInputStream( propsTest ) );
        assertEquals( "51", (Object) booterDeserializer.getPluginPid() );
        assertEquals( "pipe://1", booterDeserializer.getConnectionString() );
        return booterDeserializer.deserialize();
    }

    private ProviderConfiguration getTestProviderConfiguration( DirectoryScannerParameters directoryScannerParameters,
                                                                boolean readTestsFromInStream )
    {
        File cwd = new File( "." );
        ReporterConfiguration reporterConfiguration = new ReporterConfiguration( cwd, true );
        TestRequest testSuiteDefinition =
            new TestRequest( getSuiteXmlFileStrings(), getTestSourceDirectory(),
                             new TestListResolver( USER_REQUESTED_TEST + "#aUserRequestedTestMethod" ),
                    RERUN_FAILING_TEST_COUNT );
        RunOrderParameters runOrderParameters = new RunOrderParameters( RunOrder.DEFAULT, null );
        return new ProviderConfiguration( directoryScannerParameters, runOrderParameters, reporterConfiguration,
                new TestArtifactInfo( "5.0", "ABC" ), testSuiteDefinition, new HashMap<String, String>(), TEST_TYPED,
                readTestsFromInStream, cli, 0, Shutdown.DEFAULT, 0 );
    }

    private StartupConfiguration getTestStartupConfiguration( ClassLoaderConfiguration classLoaderConfiguration )
    {
        ClasspathConfiguration classpathConfiguration = new ClasspathConfiguration( true, true );

        return new StartupConfiguration( "com.provider", classpathConfiguration, classLoaderConfiguration, ALL,
            Collections.<String[]>emptyList() );
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
