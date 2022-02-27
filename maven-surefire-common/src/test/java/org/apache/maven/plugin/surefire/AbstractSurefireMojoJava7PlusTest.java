package org.apache.maven.plugin.surefire;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.surefire.log.PluginConsoleLogger;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ModularClasspathConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.extensions.ForkNodeFactory;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.util.DefaultScanResult;
import org.apache.maven.surefire.providerapi.ProviderInfo;
import org.apache.maven.surefire.providerapi.ProviderRequirements;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.languages.java.jpms.ResolvePathResult;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsRequest;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult;
import org.codehaus.plexus.languages.java.jpms.ModuleNameSource;
import org.codehaus.plexus.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Index.atIndex;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 * Test for {@link AbstractSurefireMojo}.
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest( { AbstractSurefireMojo.class, ResolvePathsRequest.class } )
@PowerMockIgnore( { "org.jacoco.agent.rt.*", "com.vladium.emma.rt.*" } )
public class AbstractSurefireMojoJava7PlusTest
{
    @Mock
    private ArtifactHandler handler;

    @Mock
    private LocationManager locationManager;

    @Mock
    private JavaModuleDescriptor descriptor;

    @Test
    @SuppressWarnings( "checkstyle:linelength" )
    public void shouldHaveStartupConfigForModularClasspath()
            throws Exception
    {
        AbstractSurefireMojo mojo = spy( new Mojo() );
        doReturn( locationManager )
                .when( mojo, "getLocationManager" );

        when( handler.isAddedToClasspath() ).thenReturn( true );

        VersionRange v1 = createFromVersion( "1" );
        Artifact modular = new DefaultArtifact( "x", "modular", v1, "compile", "jar", "", handler );
        modular.setFile( mockFile( "modular.jar" ) );

        VersionRange v2 = createFromVersion( "1" );
        Artifact nonModular = new DefaultArtifact( "x", "non-modular", v2, "test", "jar", "", handler );
        nonModular.setFile( mockFile( "non-modular.jar" ) );

        VersionRange v3 = createFromVersion( "4.12" );
        Artifact junit = new DefaultArtifact( "junit", "junit", v3, "test", "jar", "", handler );
        junit.setFile( mockFile( "junit.jar" ) );

        VersionRange v4 = createFromVersion( "1.3.0" );
        Artifact hamcrest = new DefaultArtifact( "org.hamcrest", "hamcrest-core", v4, "test", "jar", "", handler );
        hamcrest.setFile( mockFile( "hamcrest.jar" ) );

        File classesDir = mockFile( "classes" );
        File testClassesDir = mockFile( "test-classes" );

        TestClassPath testClasspath =
                new TestClassPath( asList( modular, nonModular, junit, hamcrest ), classesDir, testClassesDir,
                        null );

        doReturn( testClasspath ).when( mojo, "generateTestClasspath" );
        doReturn( 1 ).when( mojo, "getEffectiveForkCount" );
        doReturn( true ).when( mojo, "effectiveIsEnableAssertions" );
        when( mojo.isChildDelegation() ).thenReturn( false );
        when( mojo.getTestClassesDirectory() ).thenReturn( testClassesDir );

        DefaultScanResult scanResult = mock( DefaultScanResult.class );
        when( scanResult.getClasses() ).thenReturn( asList( "org.apache.A", "org.apache.B" ) );

        ClassLoaderConfiguration classLoaderConfiguration = new ClassLoaderConfiguration( false, true );

        VersionRange v5 = createFromVersion( "1" );
        Artifact providerArtifact = new DefaultArtifact( "org.apache.maven.surefire", "surefire-provider",
            v5, "runtime", "jar", "", handler );
        providerArtifact.setFile( mockFile( "surefire-provider.jar" ) );
        Set<Artifact> providerClasspath = singleton( providerArtifact );

        ResolvePathResult moduleInfo = mock( ResolvePathResult.class );
        when( moduleInfo.getModuleDescriptor() ).thenReturn( descriptor );

        @SuppressWarnings( "unchecked" )
        ResolvePathsRequest<String> req = mock( ResolvePathsRequest.class );
        mockStatic( ResolvePathsRequest.class );
        when( ResolvePathsRequest.ofStrings( eq( testClasspath.toClasspath().getClassPath() ) ) ).thenReturn( req );
        when( req.setJdkHome( anyString() ) ).thenReturn( req );
        when( req.setIncludeAllProviders( anyBoolean() ) ).thenReturn( req );
        when( req.setModuleDescriptor( eq( descriptor ) ) ).thenReturn( req );

        when( descriptor.name() ).thenReturn( "abc" );

        @SuppressWarnings( "unchecked" )
        ResolvePathsResult<String> res = mock( ResolvePathsResult.class );
        when( res.getMainModuleDescriptor() ).thenReturn( descriptor );
        when( res.getClasspathElements() ).thenReturn( asList( "non-modular.jar", "junit.jar", "hamcrest.jar" ) );
        Map<String, ModuleNameSource> mod = new LinkedHashMap<>();
        mod.put( "modular.jar", null );
        mod.put( "classes", null );
        when( res.getModulepathElements() ).thenReturn( mod );
        when( res.getPathExceptions() ).thenReturn( singletonMap( "java 1.8", new Exception( "low version" ) ) );
        when( locationManager.resolvePaths( eq( req ) ) ).thenReturn( res );

        Logger logger = mock( Logger.class );
        when( logger.isDebugEnabled() ).thenReturn( true );
        doNothing().when( logger ).debug( anyString() );
        when( mojo.getConsoleLogger() ).thenReturn( new PluginConsoleLogger( logger ) );

        Artifact common = new DefaultArtifact( "org.apache.maven.surefire", "maven-surefire-common", v5, "runtime",
                "jar", "", handler );
        common.setFile( mockFile( "maven-surefire-common.jar" ) );

        Artifact ext = new DefaultArtifact( "org.apache.maven.surefire", "surefire-extensions-api", v5, "runtime",
                "jar", "", handler );
        ext.setFile( mockFile( "surefire-extensions-api.jar" ) );

        Artifact api = new DefaultArtifact( "org.apache.maven.surefire", "surefire-api", v5, "runtime",
                "jar", "", handler );
        api.setFile( mockFile( "surefire-api.jar" ) );

        Artifact loggerApi = new DefaultArtifact( "org.apache.maven.surefire", "surefire-logger-api", v5, "runtime",
                "jar", "", handler );
        loggerApi.setFile( mockFile( "surefire-logger-api.jar" ) );

        Artifact spi = new DefaultArtifact( "org.apache.maven.surefire", "surefire-extensions-spi",
            createFromVersion( "1" ), "runtime", "jar", "", handler );
        spi.setFile( mockFile( "surefire-extensions-spi.jar" ) );

        Artifact booter = new DefaultArtifact( "org.apache.maven.surefire", "surefire-booter",
            createFromVersion( "1" ), "runtime", "jar", "", handler );
        booter.setFile( mockFile( "surefire-booter.jar" ) );

        Artifact utils = new DefaultArtifact( "org.apache.maven.surefire", "surefire-shared-utils",
            createFromVersion( "1" ), "runtime", "jar", "", handler );
        utils.setFile( mockFile( "surefire-shared-utils.jar" ) );

        Map<String, Artifact> artifacts = new HashMap<>();
        artifacts.put( "org.apache.maven.surefire:maven-surefire-common", common );
        artifacts.put( "org.apache.maven.surefire:surefire-extensions-api", ext );
        artifacts.put( "org.apache.maven.surefire:surefire-api", api );
        artifacts.put( "org.apache.maven.surefire:surefire-logger-api", loggerApi );
        artifacts.put( "org.apache.maven.surefire:surefire-extensions-spi", spi );
        artifacts.put( "org.apache.maven.surefire:surefire-booter", booter );
        artifacts.put( "org.apache.maven.surefire:surefire-shared-utils", utils );
        when( mojo.getPluginArtifactMap() ).thenReturn( artifacts );

        ProviderInfo providerInfo = mock( ProviderInfo.class );
        when( providerInfo.getProviderName() ).thenReturn( "org.asf.Provider" );
        when( providerInfo.getProviderClasspath() ).thenReturn( providerClasspath );

        StartupConfiguration conf = invokeMethod( mojo, "newStartupConfigWithModularPath",
            classLoaderConfiguration, providerInfo,
            new ResolvePathResultWrapper( moduleInfo, true ), scanResult, "", testClasspath );

        verify( mojo, times( 1 ) ).effectiveIsEnableAssertions();
        verify( mojo, times( 1 ) ).isChildDelegation();
        verify( mojo, times( 1 ) ).getTestClassesDirectory();
        verify( scanResult, times( 1 ) ).getClasses();
        verifyStatic( ResolvePathsRequest.class, times( 1 ) );
        ResolvePathsRequest.ofStrings( eq( testClasspath.toClasspath().getClassPath() ) );
        verify( req, times( 1 ) ).setModuleDescriptor( eq( descriptor ) );
        verify( res, times( 1 ) ).getClasspathElements();
        verify( res, times( 1 ) ).getModulepathElements();
        verify( locationManager, times( 1 ) ).resolvePaths( eq( req ) );
        ArgumentCaptor<String> argument1 = ArgumentCaptor.forClass( String.class );
        ArgumentCaptor<Exception> argument2 = ArgumentCaptor.forClass( Exception.class );
        verify( logger, times( 1 ) ).warn( argument1.capture(), argument2.capture() );
        assertThat( argument1.getValue() )
                .isEqualTo( "Exception for 'java 1.8'." );
        assertThat( argument2.getValue().getMessage() )
                .isEqualTo( "low version" );
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass( String.class );
        verify( logger, times( 9 ) ).debug( argument.capture() );
        assertThat( argument.getAllValues() )
                .containsExactly( "main module descriptor name: abc",
                        "test classpath:  non-modular.jar  junit.jar  hamcrest.jar",
                        "test modulepath:  modular.jar  classes",
                        "provider classpath:  surefire-provider.jar",
                        "test(compact) classpath:  non-modular.jar  junit.jar  hamcrest.jar",
                        "test(compact) modulepath:  modular.jar  classes",
                        "provider(compact) classpath:  surefire-provider.jar",
                        "in-process classpath:  surefire-provider.jar  maven-surefire-common.jar  surefire-booter.jar  surefire-extensions-api.jar  surefire-api.jar  surefire-extensions-spi.jar  surefire-logger-api.jar  surefire-shared-utils.jar",
                        "in-process(compact) classpath:  surefire-provider.jar  maven-surefire-common.jar  surefire-booter.jar  surefire-extensions-api.jar  surefire-api.jar  surefire-extensions-spi.jar  surefire-logger-api.jar  surefire-shared-utils.jar"
                );

        assertThat( conf ).isNotNull();
        assertThat( conf.isShadefire() ).isFalse();
        assertThat( conf.isProviderMainClass() ).isFalse();
        assertThat( conf.isManifestOnlyJarRequestedAndUsable() ).isFalse();
        assertThat( conf.getClassLoaderConfiguration() ).isSameAs( classLoaderConfiguration );
        assertThat( conf.getProviderClassName() ).isEqualTo( "org.asf.Provider" );
        assertThat( conf.getActualClassName() ).isEqualTo( "org.asf.Provider" );
        assertThat( conf.getClasspathConfiguration() ).isNotNull();
        assertThat( ( Object ) conf.getClasspathConfiguration().getTestClasspath() )
                .isEqualTo( new Classpath( res.getClasspathElements() ) );
        assertThat( ( Object ) conf.getClasspathConfiguration().getProviderClasspath() )
                .isEqualTo( new Classpath( singleton( "surefire-provider.jar" ) ) );
        assertThat( conf.getClasspathConfiguration() ).isInstanceOf( ModularClasspathConfiguration.class );
        ModularClasspathConfiguration mcc = ( ModularClasspathConfiguration ) conf.getClasspathConfiguration();
        assertThat( mcc.getModularClasspath().getModuleNameFromDescriptor() ).isEqualTo( "abc" );
        assertThat( mcc.getModularClasspath().getPackages() ).containsOnly( "org.apache" );
        assertThat( mcc.getModularClasspath().getPatchFile().getAbsolutePath() )
                .isEqualTo( "test-classes" );
        assertThat( mcc.getModularClasspath().getModulePath() )
                .containsExactly( "modular.jar", "classes" );
    }

    @Test
    @SuppressWarnings( "checkstyle:linelength" )
    public void shouldHaveStartupConfigForModularClasspathAndTestDescriptor()
        throws Exception
    {
        AbstractSurefireMojo mojo = spy( new Mojo() );
        doReturn( locationManager )
            .when( mojo, "getLocationManager" );

        when( handler.isAddedToClasspath() ).thenReturn( true );

        VersionRange v1 = createFromVersion( "1" );
        Artifact modular = new DefaultArtifact( "x", "modular", v1, "compile", "jar", "", handler );
        modular.setFile( mockFile( "modular.jar" ) );

        VersionRange v2 = createFromVersion( "1" );
        Artifact nonModular = new DefaultArtifact( "x", "non-modular", v2, "test", "jar", "", handler );
        nonModular.setFile( mockFile( "non-modular.jar" ) );

        VersionRange v3 = createFromVersion( "4.12" );
        Artifact junit = new DefaultArtifact( "junit", "junit", v3, "test", "jar", "", handler );
        junit.setFile( mockFile( "junit.jar" ) );

        VersionRange v4 = createFromVersion( "1.3.0" );
        Artifact hamcrest = new DefaultArtifact( "org.hamcrest", "hamcrest-core", v4, "test", "jar", "", handler );
        hamcrest.setFile( mockFile( "hamcrest.jar" ) );

        File classesDir = mockFile( "classes" );
        File testClassesDir = mockFile( "test-classes" );

        TestClassPath testClasspath =
            new TestClassPath( asList( modular, nonModular, junit, hamcrest ), classesDir, testClassesDir,
                null );

        doReturn( testClasspath ).when( mojo, "generateTestClasspath" );
        doReturn( 1 ).when( mojo, "getEffectiveForkCount" );
        doReturn( true ).when( mojo, "effectiveIsEnableAssertions" );
        when( mojo.isChildDelegation() ).thenReturn( false );
        when( mojo.getTestClassesDirectory() ).thenReturn( testClassesDir );

        DefaultScanResult scanResult = mock( DefaultScanResult.class );
        when( scanResult.getClasses() ).thenReturn( asList( "org.apache.A", "org.apache.B" ) );

        ClassLoaderConfiguration classLoaderConfiguration = new ClassLoaderConfiguration( false, true );

        VersionRange v5 = createFromVersion( "1" );
        Artifact providerArtifact = new DefaultArtifact( "org.apache.maven.surefire", "surefire-provider",
            v5, "runtime", "jar", "", handler );
        providerArtifact.setFile( mockFile( "surefire-provider.jar" ) );
        Set<Artifact> providerClasspath = singleton( providerArtifact );

        ResolvePathResult moduleInfo = mock( ResolvePathResult.class );
        when( moduleInfo.getModuleDescriptor() ).thenReturn( descriptor );

        when( descriptor.name() ).thenReturn( "abc" );

        Logger logger = mock( Logger.class );
        when( logger.isDebugEnabled() ).thenReturn( true );
        doNothing().when( logger ).debug( anyString() );
        when( mojo.getConsoleLogger() ).thenReturn( new PluginConsoleLogger( logger ) );

        Artifact common = new DefaultArtifact( "org.apache.maven.surefire", "maven-surefire-common", v5, "runtime",
            "jar", "", handler );
        common.setFile( mockFile( "maven-surefire-common.jar" ) );

        Artifact ext = new DefaultArtifact( "org.apache.maven.surefire", "surefire-extensions-api", v5, "runtime",
            "jar", "", handler );
        ext.setFile( mockFile( "surefire-extensions-api.jar" ) );

        Artifact api = new DefaultArtifact( "org.apache.maven.surefire", "surefire-api", v5, "runtime",
            "jar", "", handler );
        api.setFile( mockFile( "surefire-api.jar" ) );

        Artifact loggerApi = new DefaultArtifact( "org.apache.maven.surefire", "surefire-logger-api", v5, "runtime",
            "jar", "", handler );
        loggerApi.setFile( mockFile( "surefire-logger-api.jar" ) );

        Artifact spi = new DefaultArtifact( "org.apache.maven.surefire", "surefire-extensions-spi",
            createFromVersion( "1" ), "runtime", "jar", "", handler );
        spi.setFile( mockFile( "surefire-extensions-spi.jar" ) );

        Artifact booter = new DefaultArtifact( "org.apache.maven.surefire", "surefire-booter",
            createFromVersion( "1" ), "runtime", "jar", "", handler );
        booter.setFile( mockFile( "surefire-booter.jar" ) );

        Artifact utils = new DefaultArtifact( "org.apache.maven.surefire", "surefire-shared-utils",
            createFromVersion( "1" ), "runtime", "jar", "", handler );
        utils.setFile( mockFile( "surefire-shared-utils.jar" ) );

        Map<String, Artifact> artifacts = new HashMap<>();
        artifacts.put( "org.apache.maven.surefire:maven-surefire-common", common );
        artifacts.put( "org.apache.maven.surefire:surefire-extensions-api", ext );
        artifacts.put( "org.apache.maven.surefire:surefire-api", api );
        artifacts.put( "org.apache.maven.surefire:surefire-logger-api", loggerApi );
        artifacts.put( "org.apache.maven.surefire:surefire-extensions-spi", spi );
        artifacts.put( "org.apache.maven.surefire:surefire-booter", booter );
        artifacts.put( "org.apache.maven.surefire:surefire-shared-utils", utils );
        when( mojo.getPluginArtifactMap() ).thenReturn( artifacts );

        ProviderInfo providerInfo = mock( ProviderInfo.class );
        when( providerInfo.getProviderName() ).thenReturn( "org.asf.Provider" );
        when( providerInfo.getProviderClasspath() ).thenReturn( providerClasspath );

        StartupConfiguration conf = invokeMethod( mojo, "newStartupConfigWithModularPath",
            classLoaderConfiguration, providerInfo,
            new ResolvePathResultWrapper( moduleInfo, false ), scanResult, "", testClasspath );

        verify( mojo, times( 1 ) ).effectiveIsEnableAssertions();
        verify( mojo, times( 1 ) ).isChildDelegation();
        verify( mojo, never() ).getTestClassesDirectory();
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass( String.class );
        verify( logger, times( 9 ) ).debug( argument.capture() );
        assertThat( argument.getAllValues() )
            .containsExactly( "main module descriptor name: abc",
                "test classpath:",
                "test modulepath:  test-classes  classes  modular.jar  non-modular.jar  junit.jar  hamcrest.jar",
                "provider classpath:  surefire-provider.jar",
                "test(compact) classpath:",
                "test(compact) modulepath:  test-classes  classes  modular.jar  non-modular.jar  junit.jar  hamcrest.jar",
                "provider(compact) classpath:  surefire-provider.jar",
                "in-process classpath:  surefire-provider.jar  maven-surefire-common.jar  surefire-booter.jar  surefire-extensions-api.jar  surefire-api.jar  surefire-extensions-spi.jar  surefire-logger-api.jar  surefire-shared-utils.jar",
                "in-process(compact) classpath:  surefire-provider.jar  maven-surefire-common.jar  surefire-booter.jar  surefire-extensions-api.jar  surefire-api.jar  surefire-extensions-spi.jar  surefire-logger-api.jar  surefire-shared-utils.jar"
            );

        assertThat( conf ).isNotNull();
        assertThat( conf.isShadefire() ).isFalse();
        assertThat( conf.isProviderMainClass() ).isFalse();
        assertThat( conf.isManifestOnlyJarRequestedAndUsable() ).isFalse();
        assertThat( conf.getClassLoaderConfiguration() ).isSameAs( classLoaderConfiguration );
        assertThat( conf.getProviderClassName() ).isEqualTo( "org.asf.Provider" );
        assertThat( conf.getActualClassName() ).isEqualTo( "org.asf.Provider" );
        assertThat( conf.getClasspathConfiguration() ).isNotNull();
        assertThat( ( Object ) conf.getClasspathConfiguration().getTestClasspath() )
            .isEqualTo( Classpath.emptyClasspath() );
        assertThat( ( Object ) conf.getClasspathConfiguration().getProviderClasspath() )
            .isEqualTo( new Classpath( singleton( "surefire-provider.jar" ) ) );
        assertThat( conf.getClasspathConfiguration() ).isInstanceOf( ModularClasspathConfiguration.class );
        ModularClasspathConfiguration mcc = ( ModularClasspathConfiguration ) conf.getClasspathConfiguration();
        assertThat( mcc.getModularClasspath().getModuleNameFromDescriptor() ).isEqualTo( "abc" );
        assertThat( mcc.getModularClasspath().getPackages() ).isEmpty();
        assertThat( mcc.getModularClasspath().getPatchFile() )
            .isNull();
        assertThat( mcc.getModularClasspath().getModulePath() )
            .hasSize( 6 )
            .containsSequence( "test-classes", "classes", "modular.jar", "non-modular.jar",
                "junit.jar", "hamcrest.jar" );
    }

    @Test
    public void testAllProviderInfo()
    {
        Mojo mojo = new Mojo();

        ProviderRequirements providerRequirements = new ProviderRequirements( true, false, true );

        ProviderInfo providerInfo = mojo.newDynamicProviderInfo();
        assertThat( providerInfo.getProviderName() )
            .isEqualTo( "custom.Provider" );
        assertThat( providerInfo.getJpmsArguments( providerRequirements ) )
            .isEmpty();

        providerInfo = mojo.newJUnit3ProviderInfo();
        assertThat( providerInfo.getProviderName() )
            .isEqualTo( "org.apache.maven.surefire.junit.JUnit3Provider" );
        assertThat( providerInfo.getJpmsArguments( providerRequirements ) )
            .isEmpty();

        providerInfo = mojo.newJUnit4ProviderInfo();
        assertThat( providerInfo.getProviderName() )
            .isEqualTo( "org.apache.maven.surefire.junit4.JUnit4Provider" );
        assertThat( providerInfo.getJpmsArguments( providerRequirements ) )
            .isEmpty();

        providerInfo = mojo.newJUnit47ProviderInfo();
        assertThat( providerInfo.getProviderName() )
            .isEqualTo( "org.apache.maven.surefire.junitcore.JUnitCoreProvider" );
        assertThat( providerInfo.getJpmsArguments( providerRequirements ) )
            .isEmpty();

        providerInfo = mojo.newTestNgProviderInfo();
        assertThat( providerInfo.getProviderName() )
            .isEqualTo( "org.apache.maven.surefire.testng.TestNGProvider" );
        assertThat( providerInfo.getJpmsArguments( providerRequirements ) )
            .isEmpty();

        providerInfo = mojo.newJUnitPlatformProviderInfo();
        assertThat( providerInfo.getProviderName() )
            .isEqualTo( "org.apache.maven.surefire.junitplatform.JUnitPlatformProvider" );
        List<String[]> args = providerInfo.getJpmsArguments( providerRequirements );
        assertThat( args )
            .isNotEmpty()
            .hasSize( 2 )
            .contains( new String[] {
            "--add-opens", "org.junit.platform.commons/org.junit.platform.commons.util=ALL-UNNAMED"}, atIndex( 0 ) )
            .contains( new String[] {
            "--add-opens", "org.junit.platform.commons/org.junit.platform.commons.logging=ALL-UNNAMED"}, atIndex( 1 ) );
    }

    @Test
    public void shouldJoinStrings() throws Exception
    {
        Class<?>[] methodArgTypes = {String[].class};

        Object[] args = { new String[]{"my-engine"} };
        String result = invokeMethod( AbstractSurefireMojo.class, "join", methodArgTypes, args );
        assertThat( result ).isNotNull();
        assertThat( result ).isEqualTo( "my-engine" );

        args = new Object[]{ new String[]{"my-engine1", "my-engine2"} };
        result = invokeMethod( AbstractSurefireMojo.class, "join", methodArgTypes, args );
        assertThat( result ).isNotNull();
        assertThat( result ).isEqualTo( "my-engine1,my-engine2" );

        args = new Object[]{new String[0]};
        result = invokeMethod( AbstractSurefireMojo.class, "join", methodArgTypes, args );
        assertThat( result ).isNotNull();
        assertThat( result ).isEmpty();
    }

    private static File mockFile( String absolutePath )
    {
        File f = mock( File.class );
        when( f.getAbsolutePath() ).thenReturn( absolutePath );
        return f;
    }

    /**
     *
     */
    public static class Mojo
            extends AbstractSurefireMojo
    {
        ProviderInfo newDynamicProviderInfo()
        {
            return new DynamicProviderInfo( "custom.Provider" );
        }

        ProviderInfo newJUnit3ProviderInfo()
        {
            return new JUnit3ProviderInfo();
        }

        ProviderInfo newJUnit4ProviderInfo()
        {
            return new JUnit4ProviderInfo( null, null );
        }

        ProviderInfo newJUnit47ProviderInfo()
        {
            return new JUnitCoreProviderInfo( null, null );
        }

        ProviderInfo newTestNgProviderInfo()
        {
            return new TestNgProviderInfo( null );
        }

        ProviderInfo newJUnitPlatformProviderInfo()
        {
            return new JUnitPlatformProviderInfo( null, null, null );
        }

        @Override
        protected String getPluginName()
        {
            return null;
        }

        @Override
        protected int getRerunFailingTestsCount()
        {
            return 0;
        }

        @Override
        public boolean isSkipTests()
        {
            return false;
        }

        @Override
        public void setSkipTests( boolean skipTests )
        {

        }

        @Override
        public boolean isSkipExec()
        {
            return false;
        }

        @Override
        public void setSkipExec( boolean skipExec )
        {

        }

        @Override
        public boolean isSkip()
        {
            return false;
        }

        @Override
        public void setSkip( boolean skip )
        {

        }

        @Override
        public File getBasedir()
        {
            return null;
        }

        @Override
        public void setBasedir( File basedir )
        {

        }

        @Override
        public File getTestClassesDirectory()
        {
            return null;
        }

        @Override
        public void setTestClassesDirectory( File testClassesDirectory )
        {

        }

        @Override
        public File getMainBuildPath()
        {
            return null;
        }

        @Override
        public void setMainBuildPath( File mainBuildPath )
        {

        }

        @Override
        public File getReportsDirectory()
        {
            return null;
        }

        @Override
        public void setReportsDirectory( File reportsDirectory )
        {

        }

        @Override
        public String getTest()
        {
            return null;
        }

        @Override
        public void setTest( String test )
        {

        }

        @Override
        public List<String> getIncludes()
        {
            return null;
        }

        @Override
        public File getIncludesFile()
        {
            return null;
        }

        @Override
        public void setIncludes( List<String> includes )
        {

        }

        @Override
        public List<String> getExcludes()
        {
            return null;
        }

        @Override
        public void setExcludes( List<String> excludes )
        {

        }

        @Override
        public boolean isPrintSummary()
        {
            return false;
        }

        @Override
        public void setPrintSummary( boolean printSummary )
        {

        }

        @Override
        public String getReportFormat()
        {
            return null;
        }

        @Override
        public void setReportFormat( String reportFormat )
        {

        }

        @Override
        public boolean isUseFile()
        {
            return false;
        }

        @Override
        public void setUseFile( boolean useFile )
        {

        }

        @Override
        public String getDebugForkedProcess()
        {
            return null;
        }

        @Override
        public void setDebugForkedProcess( String debugForkedProcess )
        {

        }

        @Override
        public int getForkedProcessTimeoutInSeconds()
        {
            return 0;
        }

        @Override
        public void setForkedProcessTimeoutInSeconds( int forkedProcessTimeoutInSeconds )
        {

        }

        @Override
        public int getForkedProcessExitTimeoutInSeconds()
        {
            return 0;
        }

        @Override
        public void setForkedProcessExitTimeoutInSeconds( int forkedProcessTerminationTimeoutInSeconds )
        {

        }

        @Override
        public double getParallelTestsTimeoutInSeconds()
        {
            return 0;
        }

        @Override
        public void setParallelTestsTimeoutInSeconds( double parallelTestsTimeoutInSeconds )
        {

        }

        @Override
        public double getParallelTestsTimeoutForcedInSeconds()
        {
            return 0;
        }

        @Override
        public void setParallelTestsTimeoutForcedInSeconds( double parallelTestsTimeoutForcedInSeconds )
        {

        }

        @Override
        public boolean isUseSystemClassLoader()
        {
            return false;
        }

        @Override
        public void setUseSystemClassLoader( boolean useSystemClassLoader )
        {

        }

        @Override
        public boolean isUseManifestOnlyJar()
        {
            return false;
        }

        @Override
        public void setUseManifestOnlyJar( boolean useManifestOnlyJar )
        {

        }

        @Override
        public String getEncoding()
        {
            return null;
        }

        @Override
        public void setEncoding( String encoding )
        {

        }

        @Override
        public boolean getFailIfNoSpecifiedTests()
        {
            return false;
        }

        @Override
        public void setFailIfNoSpecifiedTests( boolean failIfNoSpecifiedTests )
        {

        }

        @Override
        public int getSkipAfterFailureCount()
        {
            return 0;
        }

        @Override
        public String getShutdown()
        {
            return null;
        }

        @Override
        public File getExcludesFile()
        {
            return null;
        }

        @Override
        protected List<File> suiteXmlFiles()
        {
            return null;
        }

        @Override
        protected boolean hasSuiteXmlFiles()
        {
            return false;
        }

        @Override
        protected String[] getExcludedEnvironmentVariables()
        {
            return new String[0];
        }

        @Override
        public File[] getSuiteXmlFiles()
        {
            return new File[0];
        }

        @Override
        public void setSuiteXmlFiles( File[] suiteXmlFiles )
        {

        }

        @Override
        public String getRunOrder()
        {
            return null;
        }

        @Override
        public void setRunOrder( String runOrder )
        {

        }

        @Override
        public Long getRunOrderRandomSeed()
        {
            return null;
        }

        @Override
        public void setRunOrderRandomSeed( Long runOrderRandomSeed )
        {

        }

        @Override
        protected void handleSummary( RunResult summary, Exception firstForkException )
        {

        }

        @Override
        protected boolean isSkipExecution()
        {
            return false;
        }

        @Override
        protected String[] getDefaultIncludes()
        {
            return new String[0];
        }

        @Override
        protected String getReportSchemaLocation()
        {
            return null;
        }

        @Override
        protected boolean useModulePath()
        {
            return false;
        }

        @Override
        protected void setUseModulePath( boolean useModulePath )
        {

        }

        @Override
        protected String getEnableProcessChecker()
        {
            return null;
        }

        @Override
        protected ForkNodeFactory getForkNode()
        {
            return null;
        }

        @Override
        protected Artifact getMojoArtifact()
        {
            return null;
        }

        @Override
        public File getSystemPropertiesFile()
        {
            return null;
        }

        @Override
        public void setSystemPropertiesFile( File systemPropertiesFile )
        {

        }

        @Override
        public String[] getIncludeJUnit5Engines()
        {
            return null;
        }

        @Override
        public void setIncludeJUnit5Engines( String[] includeJUnit5Engines )
        {

        }

        @Override
        public String[] getExcludeJUnit5Engines()
        {
            return null;
        }

        @Override
        public void setExcludeJUnit5Engines( String[] excludeJUnit5Engines )
        {

        }
    }
}
