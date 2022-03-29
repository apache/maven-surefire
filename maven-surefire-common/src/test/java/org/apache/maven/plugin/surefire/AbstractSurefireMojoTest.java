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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.DefaultResolutionErrorHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo.JUnitPlatformProviderInfo;
import org.apache.maven.plugin.surefire.booterclient.Platform;
import org.apache.maven.plugin.surefire.log.PluginConsoleLogger;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.util.DefaultScanResult;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ModularClasspathConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.extensions.ForkNodeFactory;
import org.apache.maven.surefire.providerapi.ProviderInfo;
import org.apache.maven.toolchain.Toolchain;
import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.LocationManager;
import org.codehaus.plexus.languages.java.jpms.ResolvePathRequest;
import org.codehaus.plexus.languages.java.jpms.ResolvePathResult;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsRequest;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult;
import org.codehaus.plexus.logging.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static java.io.File.separatorChar;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.write;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersion;
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec;
import static org.apache.maven.surefire.shared.lang3.JavaVersion.JAVA_9;
import static org.apache.maven.surefire.shared.lang3.JavaVersion.JAVA_RECENT;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.codehaus.plexus.languages.java.jpms.ModuleNameSource.MODULEDESCRIPTOR;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.reflect.Whitebox.invokeMethod;
import static org.powermock.reflect.Whitebox.setInternalState;

/**
 * Test for {@link AbstractSurefireMojo}.
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest( AbstractSurefireMojo.class )
@PowerMockIgnore( {"org.jacoco.agent.rt.*", "com.vladium.emma.rt.*"} )
public class AbstractSurefireMojoTest
{
    @Rule
    public final ExpectedException e = ExpectedException.none();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private ArtifactHandler handler;

    private final Mojo mojo = new Mojo();

    @Before
    public void setupMojo()
    {
        Artifact mojoArtifact = mojo.getMojoArtifact();

        mojo.setPluginArtifactMap( new LinkedHashMap<String, Artifact>() );
        mojo.getPluginArtifactMap().put( mojoArtifact.getGroupId() + ":" + mojoArtifact.getArtifactId(), mojoArtifact );
        Artifact forkedBooter = new DefaultArtifact( "org.apache.maven.surefire", "surefire-booter",
            mojoArtifact.getVersion(), null, "jar", null, mock( ArtifactHandler.class ) );
        mojo.getPluginArtifactMap().put( "org.apache.maven.surefire:surefire-booter", forkedBooter );

        mojo.setProjectArtifactMap( new LinkedHashMap<String, Artifact>() );

        MavenSession session = mock( MavenSession.class );
        mojo.setSession( session );

        PluginDescriptor pluginDescriptor = mock( PluginDescriptor.class );
        Plugin plugin = new Plugin();
        plugin.setGroupId( mojoArtifact.getGroupId() );
        plugin.setArtifactId( mojoArtifact.getArtifactId() );
        plugin.setVersion( mojoArtifact.getVersion() );
        when( pluginDescriptor.getPlugin() ).thenReturn( plugin );
        mojo.setPluginDescriptor( pluginDescriptor );
        mojo.setResolutionErrorHandler( new DefaultResolutionErrorHandler() );
    }

    @Test
    public void noModuleDescriptorFile() throws Exception
    {
        AbstractSurefireMojo mojo = spy( new Mojo() );
        mojo.setMainBuildPath( tempFolder.newFolder() );
        File testClassesDir = tempFolder.newFolder();
        mojo.setTestClassesDirectory( testClassesDir );
        File jdkHome = new File( System.getProperty( "java.home" ) );
        ResolvePathResultWrapper wrapper = invokeMethod( mojo, "findModuleDescriptor", jdkHome );

        assertThat( wrapper )
            .isNotNull();

        assertThat( wrapper.getResolvePathResult() )
            .isNull();

        assertThat( (boolean) invokeMethod( mojo, "existsModuleDescriptor", wrapper ) )
            .isEqualTo( false );

        when( mojo.useModulePath() ).thenReturn( true );

        File jvmExecutable = new File( jdkHome, IS_OS_WINDOWS ? "bin\\java.exe" : "bin/java" );
        JdkAttributes jdkAttributes = new JdkAttributes( jvmExecutable, jdkHome, true );
        Platform platform = new Platform().withJdkExecAttributesForTests( jdkAttributes );
        assertThat( (boolean) invokeMethod( mojo, "canExecuteProviderWithModularPath", platform, wrapper ) )
            .isEqualTo( false );
    }

    @Test
    public void correctModuleDescriptor() throws Exception
    {
        AbstractSurefireMojo mojo = spy( new Mojo() );
        LocationManager locationManager = mock( LocationManager.class );
        ResolvePathResult result = mock( ResolvePathResult.class );
        when( result.getModuleNameSource() ).thenReturn( MODULEDESCRIPTOR );
        JavaModuleDescriptor descriptor = mock( JavaModuleDescriptor.class );
        when( result.getModuleDescriptor() ).thenReturn( descriptor );
        when( locationManager.resolvePath( any( ResolvePathRequest.class ) ) ).thenReturn( result );
        doReturn( locationManager )
            .when( mojo, "getLocationManager" );
        File classesDir = tempFolder.newFolder();
        mojo.setMainBuildPath( classesDir );
        File testClassesDir = tempFolder.newFolder();
        mojo.setTestClassesDirectory( testClassesDir );
        File descriptorFile = new File( classesDir, "module-info.class" );
        assertThat( descriptorFile.createNewFile() ).isTrue();
        File jdkHome = new File( System.getProperty( "java.home" ) );
        ResolvePathResultWrapper wrapper = invokeMethod( mojo, "findModuleDescriptor", jdkHome );

        assertThat( wrapper )
            .isNotNull();

        assertThat( wrapper.getResolvePathResult() )
            .isSameAs( result );

        assertThat( wrapper.getResolvePathResult().getModuleNameSource() )
            .isSameAs( MODULEDESCRIPTOR );

        assertThat( wrapper.getResolvePathResult().getModuleDescriptor() )
            .isSameAs( descriptor );

        assertThat( (boolean) invokeMethod( mojo, "existsModuleDescriptor", wrapper ) )
            .isEqualTo( true );

        when( mojo.useModulePath() ).thenReturn( true );

        File jvmExecutable = new File( jdkHome, IS_OS_WINDOWS ? "bin\\java.exe" : "bin/java" );
        JdkAttributes jdkAttributes = new JdkAttributes( jvmExecutable, jdkHome, true );
        Platform platform = new Platform().withJdkExecAttributesForTests( jdkAttributes );
        assertThat( (boolean) invokeMethod( mojo, "canExecuteProviderWithModularPath", platform, wrapper ) )
            .isEqualTo( true );

        jdkAttributes = new JdkAttributes( jvmExecutable, jdkHome, false );
        platform = new Platform().withJdkExecAttributesForTests( jdkAttributes );
        assertThat( (boolean) invokeMethod( mojo, "canExecuteProviderWithModularPath", platform, wrapper ) )
            .isEqualTo( false );

        when( mojo.useModulePath() ).thenReturn( false );

        jdkAttributes = new JdkAttributes( jvmExecutable, jdkHome, true );
        platform = new Platform().withJdkExecAttributesForTests( jdkAttributes );
        assertThat( (boolean) invokeMethod( mojo, "canExecuteProviderWithModularPath", platform, wrapper ) )
            .isEqualTo( false );
    }

    @Test
    @SuppressWarnings( "checkstyle:magicnumber" )
    public void corruptedModuleDescriptor() throws Exception
    {
        if ( !JAVA_RECENT.atLeast( JAVA_9 ) )
        {
            return;
        }

        AbstractSurefireMojo mojo = spy( new Mojo() );
        doReturn( new LocationManager() )
            .when( mojo, "getLocationManager" );
        File classesDir = tempFolder.newFolder();
        mojo.setMainBuildPath( classesDir );
        File testClassesDir = tempFolder.newFolder();
        mojo.setTestClassesDirectory( testClassesDir );

        File descriptorFile = new File( classesDir, "module-info.class" );
        assertThat( descriptorFile.createNewFile() ).isTrue();
        write( descriptorFile.toPath(), new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE} );

        File jdkHome = new File( System.getProperty( "java.home" ) );
        ResolvePathResultWrapper wrapper = invokeMethod( mojo, "findModuleDescriptor", jdkHome );

        assertThat( wrapper )
            .isNotNull();

        assertThat( wrapper.getResolvePathResult() )
            .isNull();

        assertThat( (boolean) invokeMethod( mojo, "existsModuleDescriptor", wrapper ) )
            .isEqualTo( false );
    }

    @Test
    public void shouldShowArray() throws Exception
    {
        Logger logger = mock( Logger.class );
        when( logger.isDebugEnabled() ).thenReturn( true );
        doNothing().when( logger ).debug( anyString() );

        AbstractSurefireMojo mojo = spy( this.mojo );

        when( mojo.getConsoleLogger() ).thenReturn( new PluginConsoleLogger( logger ) );

        Object[] array = {"ABC", "XYZ"};
        invokeMethod( mojo, "showArray", array, "prefix" );

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass( String.class );
        verify( logger, times( 2 ) ).debug( argument.capture() );
        assertThat( argument.getAllValues() )
            .containsExactly( "Setting prefix [ABC]", "Setting prefix [XYZ]" );
    }

    @Test
    public void shouldShowMap() throws Exception
    {
        Logger logger = mock( Logger.class );
        when( logger.isDebugEnabled() ).thenReturn( true );
        doNothing().when( logger ).debug( anyString() );

        AbstractSurefireMojo mojo = spy( this.mojo );

        when( mojo.getConsoleLogger() ).thenReturn( new PluginConsoleLogger( logger ) );

        Map<String, String> map = new LinkedHashMap<>();
        map.put( "ABC", "123" );
        map.put( "XYZ", "987" );
        invokeMethod( mojo, "showMap", map, "prefix" );

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass( String.class );
        verify( logger, times( 2 ) ).debug( argument.capture() );
        assertThat( argument.getAllValues() )
            .containsExactly( "Setting prefix [ABC]=[123]", "Setting prefix [XYZ]=[987]" );
    }

    @Test
    public void shouldRetainInPluginArtifacts() throws Exception
    {
        Artifact provider = new DefaultArtifact( "g", "a", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        Artifact common = new DefaultArtifact( "g", "c", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        Artifact api = new DefaultArtifact( "g", "a", createFromVersionSpec( "1" ), "compile", "jar", "", null );

        Set<Artifact> providerArtifacts = singleton( provider );
        Artifact[] inPluginArtifacts = {common, api};
        Set<Artifact> inPluginClasspath = invokeMethod( AbstractSurefireMojo.class,
            "retainInProcArtifactsUnique", providerArtifacts, inPluginArtifacts );

        assertThat( inPluginClasspath )
            .containsOnly( common );
    }

    @Test
    public void shouldRetainInProcArtifactsUnique() throws Exception
    {
        Artifact provider = new DefaultArtifact( "g", "p", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        Artifact common = new DefaultArtifact( "g", "c", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        Artifact api = new DefaultArtifact( "g", "a", createFromVersionSpec( "1" ), "compile", "jar", "", null );

        Set<Artifact> providerArtifacts = singleton( provider );
        Artifact[] inPluginArtifacts = {common, api};
        Set<Artifact> inPluginClasspath = invokeMethod( AbstractSurefireMojo.class,
            "retainInProcArtifactsUnique", providerArtifacts, inPluginArtifacts );

        assertThat( inPluginClasspath )
            .containsOnly( common, api );
    }

    @Test
    public void shouldCreateInProcClasspath() throws Exception
    {
        Artifact provider = new DefaultArtifact( "g", "p", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        provider.setFile( mockFile( "provider.jar" ) );

        Artifact common = new DefaultArtifact( "g", "c", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        common.setFile( mockFile( "maven-surefire-common.jar" ) );

        Artifact api = new DefaultArtifact( "g", "a", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        api.setFile( mockFile( "surefire-api.jar" ) );

        Set<Artifact> newArtifacts = new LinkedHashSet<>();
        newArtifacts.add( common );
        newArtifacts.add( api );

        Classpath providerClasspath = new Classpath( singleton( provider.getFile().getAbsolutePath() ) );

        Classpath inPluginClasspath = invokeMethod( AbstractSurefireMojo.class,
            "createInProcClasspath", providerClasspath, newArtifacts );

        Classpath expectedClasspath =
            new Classpath( asList( provider.getFile().getAbsolutePath(),
                common.getFile().getAbsolutePath(),
                api.getFile().getAbsolutePath() ) );

        assertThat( (Object) inPluginClasspath )
            .isEqualTo( expectedClasspath );

    }

    @Test
    public void shouldGenerateTestClasspath() throws Exception
    {
        AbstractSurefireMojo mojo = spy( this.mojo );

        when( mojo.getMainBuildPath() ).thenReturn( new File( "target" + separatorChar + "classes" ) );
        when( mojo.getTestClassesDirectory() ).thenReturn( new File( "target" + separatorChar + "test-classes" ) );
        when( mojo.getClasspathDependencyScopeExclude() ).thenReturn( "runtime" );
        when( mojo.getClasspathDependencyExcludes() ).thenReturn( new String[] {"g3:a3"} );
        doReturn( mock( Artifact.class ) ).when( mojo, "getTestNgArtifact" );

        Set<Artifact> artifacts = new HashSet<>();

        Artifact a1 = mock( Artifact.class );
        when( a1.getGroupId() ).thenReturn( "g1" );
        when( a1.getArtifactId() ).thenReturn( "a1" );
        when( a1.getVersion() ).thenReturn( "1" );
        when( a1.getScope() ).thenReturn( "runtime" );
        when( a1.getDependencyConflictId() ).thenReturn( "g1:a1:jar" );
        when( a1.getId() ).thenReturn( "g1:a1:jar:1" );
        artifacts.add( a1 );

        ArtifactHandler artifactHandler = mock( ArtifactHandler.class );
        when( artifactHandler.isAddedToClasspath() ).thenReturn( true );

        Artifact a2 = mock( Artifact.class );
        when( a2.getGroupId() ).thenReturn( "g2" );
        when( a2.getArtifactId() ).thenReturn( "a2" );
        when( a2.getVersion() ).thenReturn( "2" );
        when( a2.getScope() ).thenReturn( "test" );
        when( a2.getDependencyConflictId() ).thenReturn( "g2:a2:jar" );
        when( a2.getId() ).thenReturn( "g2:a2:jar:2" );
        when( a2.getFile() ).thenReturn( new File( "a2-2.jar" ) );
        when( a2.getArtifactHandler() ).thenReturn( artifactHandler );
        artifacts.add( a2 );

        Artifact a3 = mock( Artifact.class );
        when( a3.getGroupId() ).thenReturn( "g3" );
        when( a3.getArtifactId() ).thenReturn( "a3" );
        when( a3.getVersion() ).thenReturn( "3" );
        when( a3.getScope() ).thenReturn( "test" );
        when( a3.getDependencyConflictId() ).thenReturn( "g3:a3:jar" );
        when( a3.getId() ).thenReturn( "g3:a3:jar:3" );
        when( a3.getFile() ).thenReturn( new File( "a3-3.jar" ) );
        when( a3.getArtifactHandler() ).thenReturn( artifactHandler );
        artifacts.add( a3 );

        MavenProject project = mock( MavenProject.class );
        when( project.getArtifacts() ).thenReturn( artifacts );
        when( mojo.getProject() ).thenReturn( project );

        TestClassPath cp = invokeMethod( mojo, "generateTestClasspath" );

        verifyPrivate( mojo, times( 1 ) ).invoke( "generateTestClasspath" );
        verify( mojo, times( 1 ) ).getMainBuildPath();
        verify( mojo, times( 1 ) ).getTestClassesDirectory();
        verify( mojo, times( 3 ) ).getClasspathDependencyScopeExclude();
        verify( mojo, times( 2 ) ).getClasspathDependencyExcludes();
        verify( mojo, times( 1 ) ).getAdditionalClasspathElements();

        assertThat( cp.toClasspath().getClassPath() ).hasSize( 3 );
        assertThat( cp.toClasspath().getClassPath().get( 0 ) ).endsWith( "test-classes" );
        assertThat( cp.toClasspath().getClassPath().get( 1 ) ).endsWith( "classes" );
        assertThat( cp.toClasspath().getClassPath().get( 2 ) ).endsWith( "a2-2.jar" );
    }

    @Test
    @SuppressWarnings( "checkstyle:linelength" )
    public void shouldHaveStartupConfigForNonModularClasspath()
        throws Exception
    {
        AbstractSurefireMojo mojo = spy( this.mojo );

        Artifact common = new DefaultArtifact( "org.apache.maven.surefire", "maven-surefire-common",
            createFromVersion( "1" ), "runtime", "jar", "", handler );
        common.setFile( mockFile( "maven-surefire-common.jar" ) );

        Artifact ext = new DefaultArtifact( "org.apache.maven.surefire", "surefire-extensions-api",
            createFromVersion( "1" ), "runtime", "jar", "", handler );
        ext.setFile( mockFile( "surefire-extensions-api.jar" ) );

        Artifact api = new DefaultArtifact( "org.apache.maven.surefire", "surefire-api",
            createFromVersion( "1" ), "runtime", "jar", "", handler );
        api.setFile( mockFile( "surefire-api.jar" ) );

        Artifact loggerApi = new DefaultArtifact( "org.apache.maven.surefire", "surefire-logger-api",
            createFromVersion( "1" ), "runtime", "jar", "", handler );
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

        Map<String, Artifact> providerArtifactsMap = new HashMap<>();
        providerArtifactsMap.put( "org.apache.maven.surefire:maven-surefire-common", common );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-extensions-api", ext );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-api", api );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-logger-api", loggerApi );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-extensions-spi", spi );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-booter", booter );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-shared-utils", utils );

        when( mojo.getPluginArtifactMap() )
            .thenReturn( providerArtifactsMap );

        when( handler.isAddedToClasspath() ).thenReturn( true );

        VersionRange v1 = createFromVersion( "4.12" );
        Artifact junit = new DefaultArtifact( "junit", "junit", v1, "test", "jar", "", handler );
        junit.setFile( mockFile( "junit.jar" ) );

        VersionRange v2 = createFromVersion( "1.3.0" );
        Artifact hamcrest = new DefaultArtifact( "org.hamcrest", "hamcrest-core", v2, "test", "jar", "", handler );
        hamcrest.setFile( mockFile( "hamcrest.jar" ) );

        File classesDir = mockFile( "classes" );
        File testClassesDir = mockFile( "test-classes" );
        TestClassPath testClasspath =
            new TestClassPath( asList( junit, hamcrest ), classesDir, testClassesDir, null );

        doReturn( testClasspath ).when( mojo, "generateTestClasspath" );
        doReturn( 1 ).when( mojo, "getEffectiveForkCount" );
        doReturn( true ).when( mojo, "effectiveIsEnableAssertions" );
        when( mojo.isChildDelegation() ).thenReturn( false );

        ClassLoaderConfiguration classLoaderConfiguration = new ClassLoaderConfiguration( false, true );

        VersionRange v3 = createFromVersion( "1" );
        Artifact provider = new DefaultArtifact( "x", "surefire-provider", v3, "runtime", "jar", "", handler );
        provider.setFile( mockFile( "surefire-provider.jar" ) );
        Set<Artifact> providerArtifacts = singleton( provider );

        Logger logger = mock( Logger.class );
        when( logger.isDebugEnabled() ).thenReturn( true );
        doNothing().when( logger ).debug( anyString() );
        when( mojo.getConsoleLogger() ).thenReturn( new PluginConsoleLogger( logger ) );

        ProviderInfo providerInfo = mock( ProviderInfo.class );
        when( providerInfo.getProviderName() ).thenReturn( "org.asf.Provider" );
        when( providerInfo.getProviderClasspath() ).thenReturn( providerArtifacts );

        StartupConfiguration conf = invokeMethod( mojo, "newStartupConfigWithClasspath",
            classLoaderConfiguration, providerInfo, testClasspath );

        verify( mojo, times( 1 ) ).effectiveIsEnableAssertions();
        verify( mojo, times( 1 ) ).isChildDelegation();
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass( String.class );
        verify( logger, times( 6 ) ).debug( argument.capture() );
        assertThat( argument.getAllValues() )
            .containsExactly( "test classpath:  test-classes  classes  junit.jar  hamcrest.jar",
                "provider classpath:  surefire-provider.jar",
                "test(compact) classpath:  test-classes  classes  junit.jar  hamcrest.jar",
                "provider(compact) classpath:  surefire-provider.jar",
                "in-process classpath:  surefire-provider.jar  maven-surefire-common.jar  surefire-booter.jar  surefire-extensions-api.jar  surefire-api.jar  surefire-extensions-spi.jar  surefire-logger-api.jar  surefire-shared-utils.jar",
                "in-process(compact) classpath:  surefire-provider.jar  maven-surefire-common.jar  surefire-booter.jar  surefire-extensions-api.jar  surefire-api.jar  surefire-extensions-spi.jar  surefire-logger-api.jar  surefire-shared-utils.jar"
            );

        assertThat( conf.getClassLoaderConfiguration() )
            .isSameAs( classLoaderConfiguration );

        assertThat( (Object) conf.getClasspathConfiguration().getTestClasspath() )
            .isEqualTo( testClasspath.toClasspath() );

        Collection<String> files = new ArrayList<>();
        for ( Artifact providerArtifact : providerArtifacts )
        {
            files.add( providerArtifact.getFile().getAbsolutePath() );
        }
        assertThat( (Object) conf.getClasspathConfiguration().getProviderClasspath() )
            .isEqualTo( new Classpath( files ) );

        assertThat( (Object) conf.getClasspathConfiguration().isClassPathConfig() )
            .isEqualTo( true );

        assertThat( (Object) conf.getClasspathConfiguration().isModularPathConfig() )
            .isEqualTo( false );

        assertThat( (Object) conf.getClasspathConfiguration().isEnableAssertions() )
            .isEqualTo( true );

        assertThat( conf.getProviderClassName() )
            .isEqualTo( "org.asf.Provider" );
    }

    @Test
    public void providerClasspathCachingIsNotSharedAcrossMojoInstances() throws Exception
    {
        ProviderInfo providerInfo = mock( ProviderInfo.class );
        when( providerInfo.getProviderName() ).thenReturn( "test-provider" );
        Artifact provider = new DefaultArtifact( "com.example", "provider", createFromVersion( "1" ), "runtime",
            "jar", "", handler );
        provider.setFile( mockFile( "original-test-provider.jar" ) );
        Set<Artifact> providerClasspath = singleton( provider );
        when( providerInfo.getProviderClasspath() ).thenReturn( providerClasspath );

        StartupConfiguration startupConfiguration = startupConfigurationForProvider( providerInfo );
        assertThat( startupConfiguration.getClasspathConfiguration().getProviderClasspath().getClassPath() )
            .containsExactly( "original-test-provider.jar" );

        provider.setFile( mockFile( "modified-test-provider.jar" ) );
        startupConfiguration = startupConfigurationForProvider( providerInfo );
        assertThat( startupConfiguration.getClasspathConfiguration().getProviderClasspath().getClassPath() )
            .containsExactly( "modified-test-provider.jar" );
    }

    private StartupConfiguration startupConfigurationForProvider( ProviderInfo providerInfo ) throws Exception
    {
        AbstractSurefireMojo mojo = spy( new Mojo() );

        Logger logger = mock( Logger.class );
        when( logger.isDebugEnabled() ).thenReturn( true );
        doNothing().when( logger ).debug( anyString() );
        when( mojo.getConsoleLogger() ).thenReturn( new PluginConsoleLogger( logger ) );

        File classesDir = mockFile( "classes" );
        File testClassesDir = mockFile( "test-classes" );
        TestClassPath testClassPath =
            new TestClassPath( new ArrayList<Artifact>(), classesDir, testClassesDir, new String[0] );

        Artifact common = new DefaultArtifact( "org.apache.maven.surefire", "maven-surefire-common",
            createFromVersion( "1" ), "runtime", "jar", "", handler );
        common.setFile( mockFile( "maven-surefire-common.jar" ) );

        Artifact ext = new DefaultArtifact( "org.apache.maven.surefire", "surefire-extensions-api",
            createFromVersion( "1" ), "runtime", "jar", "", handler );
        ext.setFile( mockFile( "surefire-extensions-api.jar" ) );

        Artifact api = new DefaultArtifact( "org.apache.maven.surefire", "surefire-api",
            createFromVersion( "1" ), "runtime", "jar", "", handler );
        api.setFile( mockFile( "surefire-api.jar" ) );

        Artifact loggerApi = new DefaultArtifact( "org.apache.maven.surefire", "surefire-logger-api",
            createFromVersion( "1" ), "runtime", "jar", "", handler );
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

        Map<String, Artifact> providerArtifactsMap = new HashMap<>();
        providerArtifactsMap.put( "org.apache.maven.surefire:maven-surefire-common", common );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-extensions-api", ext );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-api", api );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-logger-api", loggerApi );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-extensions-spi", spi );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-booter", booter );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-shared-utils", utils );

        when( mojo.getPluginArtifactMap() ).thenReturn( providerArtifactsMap );

        doReturn( 1 ).when( mojo, "getEffectiveForkCount" );

        return invokeMethod( mojo, "createStartupConfiguration",
            providerInfo, false, null, null, testClassPath, null, null );
    }

    @Test
    public void shouldCreateStartupConfigWithModularPath() throws Exception
    {
        String baseDir = System.getProperty( "user.dir" );

        Mojo mojo = new Mojo();

        // ### BEGIN
        // we cannot mock private method newStartupConfigWithModularPath() - mocking the data to prevent from errors
        LocationManager locationManager = mock( LocationManager.class );
        ResolvePathsResult resolvePathsResult = mock( ResolvePathsResult.class );
        when( locationManager.resolvePaths( any( ResolvePathsRequest.class ) ) ).thenReturn( resolvePathsResult );
        when( resolvePathsResult.getPathExceptions() ).thenReturn( emptyMap() );
        when( resolvePathsResult.getClasspathElements() ).thenReturn( emptyList() );
        when( resolvePathsResult.getModulepathElements() ).thenReturn( emptyMap() );

        mojo.setLogger( mock( Logger.class ) );
        mojo.setUseModulePath( true );
        setInternalState( mojo, "locationManager", locationManager );

        File jdkHome = new File( System.getProperty( "java.home" ) );
        File jvmExecutable = new File( jdkHome, IS_OS_WINDOWS ? "bin\\java.exe" : "bin/java" );
        JdkAttributes jdkAttributes = new JdkAttributes( jvmExecutable, jdkHome, true );
        Platform platform = new Platform().withJdkExecAttributesForTests( jdkAttributes );

        File classesDirectory = new File( baseDir, "mock-dir" );
        File testClassesDirectory = new File( baseDir, "mock-dir" );
        mojo.setTestClassesDirectory( testClassesDirectory );
        TestClassPath testClassPath = new TestClassPath( Collections.<Artifact>emptySet(),
            classesDirectory, testClassesDirectory, new String[0] );

        ProviderInfo providerInfo = mock( ProviderInfo.class );
        when( providerInfo.getProviderName() ).thenReturn( "provider mock" );
        when( providerInfo.getProviderClasspath() ).thenReturn( Collections.<Artifact>emptySet() );

        DefaultScanResult defaultScanResult = mock( DefaultScanResult.class );
        when( defaultScanResult.getClasses() ).thenReturn( Collections.<String>emptyList() );

        Path pathToModularDescriptor =
            Paths.get( baseDir, "src", "test", "resources", "org", "apache", "maven", "plugin", "surefire" );
        mojo.setMainBuildPath( pathToModularDescriptor.toFile() );

        Map<String, Artifact> artifacts = new HashMap<>();
        Artifact dummyArtifact = mock( Artifact.class );
        when( dummyArtifact.getFile() ).thenReturn( new File( baseDir, "mock-file" ) );
        artifacts.put( "org.apache.maven.surefire:maven-surefire-common", dummyArtifact );
        artifacts.put( "org.apache.maven.surefire:surefire-extensions-api", dummyArtifact );
        artifacts.put( "org.apache.maven.surefire:surefire-api", dummyArtifact );
        artifacts.put( "org.apache.maven.surefire:surefire-logger-api", dummyArtifact );
        artifacts.put( "org.apache.maven.surefire:surefire-extensions-spi", dummyArtifact );
        artifacts.put( "org.apache.maven.surefire:surefire-booter", dummyArtifact );
        artifacts.put( "org.apache.maven.surefire:surefire-shared-utils", dummyArtifact );
        mojo.setPluginArtifactMap( artifacts );

        ResolvePathResult resolvePathResult = mock( ResolvePathResult.class );
        JavaModuleDescriptor desc = mock( JavaModuleDescriptor.class );
        when( desc.name() ).thenReturn( "" );
        when( resolvePathResult.getModuleDescriptor() ).thenReturn( desc );
        ResolvePathResultWrapper wrapper = new ResolvePathResultWrapper( resolvePathResult, true );
        // ### END

        StartupConfiguration actualConfig = invokeMethod( mojo, "createStartupConfiguration",
            new Class[] {ProviderInfo.class, boolean.class, ClassLoaderConfiguration.class, DefaultScanResult.class,
                TestClassPath.class, Platform.class, ResolvePathResultWrapper.class},
            providerInfo, true, mock( ClassLoaderConfiguration.class ), defaultScanResult,
            testClassPath, platform, wrapper );

        assertThat( actualConfig )
            .isNotNull();

        assertThat( actualConfig.getClasspathConfiguration() )
            .isInstanceOf( ModularClasspathConfiguration.class );
    }

    @Test
    public void shouldExistTmpDirectory() throws IOException
    {
        String systemTmpDir = System.getProperty( "java.io.tmpdir" );
        String usrDir = new File( System.getProperty( "user.dir" ) ).getCanonicalPath();

        String tmpDir = "surefireX" + System.currentTimeMillis();

        //noinspection ResultOfMethodCallIgnored
        new File( systemTmpDir, tmpDir ).delete();

        File targetDir = new File( usrDir, "target" );
        //noinspection ResultOfMethodCallIgnored
        new File( targetDir, tmpDir ).delete();

        AbstractSurefireMojo mojo = mock( AbstractSurefireMojo.class );
        Logger logger = mock( Logger.class );
        when( logger.isDebugEnabled() ).thenReturn( false );
        when( logger.isErrorEnabled() ).thenReturn( false );
        doNothing().when( logger ).debug( anyString() );
        doNothing().when( logger ).error( anyString(), any( Throwable.class ) );
        when( mojo.getConsoleLogger() ).thenReturn( new PluginConsoleLogger( logger ) );
        when( mojo.getTempDir() ).thenReturn( tmpDir );
        when( mojo.getProjectBuildDirectory() ).thenReturn( targetDir );
        when( mojo.createSurefireBootDirectoryInTemp() ).thenCallRealMethod();
        when( mojo.createSurefireBootDirectoryInBuild() ).thenCallRealMethod();
        when( mojo.getSurefireTempDir() ).thenCallRealMethod();

        File bootDir = mojo.createSurefireBootDirectoryInTemp();
        assertThat( bootDir ).isNotNull();
        assertThat( bootDir ).isDirectory();

        assertThat( new File( systemTmpDir, bootDir.getName() ) ).isDirectory();
        assertThat( bootDir.getName() )
            .startsWith( tmpDir );

        File buildTmp = mojo.createSurefireBootDirectoryInBuild();
        assertThat( buildTmp ).isNotNull();
        assertThat( buildTmp ).isDirectory();
        assertThat( buildTmp.getParentFile().getCanonicalFile().getParent() ).isEqualTo( usrDir );
        assertThat( buildTmp.getName() ).isEqualTo( tmpDir );

        File tmp = mojo.getSurefireTempDir();
        assertThat( tmp ).isNotNull();
        assertThat( tmp ).isDirectory();
        assertThat( IS_OS_WINDOWS ? new File( systemTmpDir, bootDir.getName() ) : new File( targetDir, tmpDir ) )
            .isDirectory();
    }

    @Test
    public void shouldSmartlyResolveJUnit5ProviderWithJUnit4() throws Exception
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact( new DefaultArtifact( "dummy", "pom", createFromVersion( "1.0.0" ),
            null, "jar", null, mock( ArtifactHandler.class ) ) );
        mojo.setProject( mavenProject );

        final VersionRange surefireVersion = createFromVersion( "1" );

        Artifact testClasspathJUnit = new DefaultArtifact( "junit", "junit",
            createFromVersion( "4.12" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Artifact testClasspathHamcrest = new DefaultArtifact( "org.hamcrest", "hamcrest-core",
            createFromVersion( "1.3" ), null, "jar", null, mock( ArtifactHandler.class ) );

        setProjectDepedenciesToMojo( testClasspathJUnit, testClasspathHamcrest );

        Collection<Artifact> testArtifacts = new ArrayList<>();
        testArtifacts.add( testClasspathJUnit );
        testArtifacts.add( testClasspathHamcrest );

        File classesDirectory = new File( "target/classes" );

        File testClassesDirectory = new File( "target/test-classes" );

        TestClassPath testClasspathWrapper =
            new TestClassPath( testArtifacts, classesDirectory, testClassesDirectory, null );

        mojo.setRemoteRepositories( Collections.<ArtifactRepository>emptyList() );
        mojo.setProjectRemoteRepositories( Collections.<ArtifactRepository>emptyList() );
        RepositorySystem repositorySystem = mock( RepositorySystem.class );

        final Artifact surefireProvider = new DefaultArtifact( "org.apache.maven.surefire",
            "surefire-junit-platform", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact pluginDependency = new DefaultArtifact( "org.junit.vintage", "junit-vintage-engine",
            createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        when( repositorySystem.createDependencyArtifact( any( Dependency.class ) ) ).thenAnswer( new Answer<Artifact>()
        {
            @Override
            public Artifact answer( InvocationOnMock invocation )
            {
                Dependency dependency = (Dependency) invocation.getArguments()[0];
                if ( dependency.getArtifactId().equals( "surefire-junit-platform" ) )
                {
                    return surefireProvider;
                }
                else if ( dependency.getArtifactId().equals( "junit-vintage-engine" ) )
                {
                    return pluginDependency;
                }
                else
                {
                    fail( dependency.getGroupId() + ":" + dependency.getArtifactId() );
                    return null;
                }
            }
        } );

        final ArtifactResolutionResult surefireProviderResolutionResult = mock( ArtifactResolutionResult.class );
        final ArtifactResolutionResult pluginDependencyResolutionResult = mock( ArtifactResolutionResult.class );
        when( repositorySystem.resolve( any( ArtifactResolutionRequest.class ) ) )
            .thenAnswer( new Answer<ArtifactResolutionResult>()
            {
                @Override
                public ArtifactResolutionResult answer( InvocationOnMock invocation )
                {
                    ArtifactResolutionRequest req = (ArtifactResolutionRequest) invocation.getArguments()[0];
                    Artifact artifact = req.getArtifact();
                    if ( artifact == surefireProvider )
                    {
                        return surefireProviderResolutionResult;
                    }
                    else if ( artifact == pluginDependency )
                    {
                        return pluginDependencyResolutionResult;
                    }
                    else if ( "org.junit.platform".equals( artifact.getGroupId() )
                        && "junit-platform-launcher".equals( artifact.getArtifactId() )
                        && "1.4.0".equals( artifact.getVersion() ) )
                    {
                        return createExpectedJUnitPlatformLauncherResolutionResult();
                    }
                    else
                    {
                        fail( artifact.getGroupId() + ":" + artifact.getArtifactId() );
                        return null;
                    }
                }
            } );

        Artifact java5 = new DefaultArtifact( "org.apache.maven.surefire", "common-java5",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact launcher = new DefaultArtifact( "org.junit.platform", "junit-platform-launcher",
            createFromVersion( "1.3.2" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact apiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
            createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact engine = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
            createFromVersion( "1.3.2" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact commons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
            createFromVersion( "1.3.2" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact opentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
            createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Set<Artifact> providerArtifacts = new HashSet<>();
        providerArtifacts.add( surefireProvider );
        providerArtifacts.add( java5 );
        providerArtifacts.add( launcher );
        providerArtifacts.add( apiguardian );
        providerArtifacts.add( engine );
        providerArtifacts.add( commons );
        providerArtifacts.add( opentest4j );

        when( surefireProviderResolutionResult.getArtifacts() )
            .thenReturn( providerArtifacts );

        final Artifact pluginDep1 = new DefaultArtifact( "org.junit.vintage", "junit-vintage-engine",
            createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact pluginDep2 = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
            createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact pluginDep3 = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact pluginDep4 = new DefaultArtifact( "junit", "junit",
            createFromVersion( "4.12" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact pluginDep5 = new DefaultArtifact( "org.hamcrest", "hamcrest-core",
            createFromVersion( "1.3" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact pluginDep6 = new DefaultArtifact( "org.opentest4j", "opentest4j",
            createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact pluginDep7 = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        addPluginDependencies( pluginDep1, pluginDep2, pluginDep3, pluginDep4, pluginDep5, pluginDep6, pluginDep7 );

        mojo.setRepositorySystem( repositorySystem );
        mojo.setLogger( mock( Logger.class ) );

        Set<Artifact> pluginDependencyArtifacts = new HashSet<>();
        pluginDependencyArtifacts.add( pluginDep1 );
        pluginDependencyArtifacts.add( pluginDep2 );
        pluginDependencyArtifacts.add( pluginDep3 );
        pluginDependencyArtifacts.add( pluginDep4 );
        pluginDependencyArtifacts.add( pluginDep5 );
        pluginDependencyArtifacts.add( pluginDep6 );
        pluginDependencyArtifacts.add( pluginDep7 );
        when( pluginDependencyResolutionResult.getArtifacts() ).thenReturn( pluginDependencyArtifacts );

        invokeMethod( mojo, "setupStuff" );

        when( mojo.getSession().getProjectBuildingRequest() )
            .thenReturn( mock( ProjectBuildingRequest.class ) );

        PluginDescriptor pluginDescriptor = mock( PluginDescriptor.class );
        mojo.setPluginDescriptor( pluginDescriptor );
        Plugin p = mock( Plugin.class );
        when( pluginDescriptor.getPlugin() )
            .thenReturn( p );
        when( p.getDependencies() )
            .thenReturn( singletonList( toDependency( pluginDependency ) ) );

        Artifact junitPlatformArtifact = invokeMethod( mojo, "getJUnit5Artifact" );
        assertThat( junitPlatformArtifact.getGroupId() ).isEqualTo( "org.junit.platform" );
        assertThat( junitPlatformArtifact.getArtifactId() ).isEqualTo( "junit-platform-engine" );
        assertThat( junitPlatformArtifact.getVersion() ).isEqualTo( "1.4.0" );

        JUnitPlatformProviderInfo prov =
            mojo.createJUnitPlatformProviderInfo( junitPlatformArtifact, testClasspathWrapper );

        assertThat( prov.isApplicable() )
            .isTrue();

        Artifact expectedProvider = new DefaultArtifact( "org.apache.maven.surefire", "surefire-junit-platform",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedCommonJava5 = new DefaultArtifact( "org.apache.maven.surefire", "common-java5",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedLauncher = new DefaultArtifact( "org.junit.platform", "junit-platform-launcher",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedApiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
            createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedJUnit5Engine = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedOpentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
            createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedPlatformCommons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedEngine = new DefaultArtifact( "org.junit.vintage", "junit-vintage-engine",
            createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        assertThat( prov.getProviderClasspath() )
            .hasSize( 8 )
            .containsOnly( expectedProvider, expectedCommonJava5, expectedLauncher, expectedApiguardian,
                expectedJUnit5Engine, expectedOpentest4j, expectedPlatformCommons, expectedEngine );

        assertThat( testClasspathWrapper.getTestDependencies() )
            .hasSize( 2 )
            .containsEntry( "junit:junit", testClasspathJUnit )
            .containsEntry( "org.hamcrest:hamcrest-core", testClasspathHamcrest );
    }

    @Test
    public void shouldSmartlyResolveJUnit5ProviderWithVintage() throws Exception
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact( new DefaultArtifact( "dummy", "pom", createFromVersion( "1.0.0" ),
            null, "jar", null, mock( ArtifactHandler.class ) ) );
        mojo.setProject( mavenProject );

        VersionRange surefireVersion = createFromVersion( "1" );

        Artifact testClasspathSomeTestArtifact = new DefaultArtifact( "third.party", "artifact",
            createFromVersion( "1.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Artifact testClasspathVintage = new DefaultArtifact( "org.junit.vintage", "junit-vintage-engine",
            createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Artifact testClasspathApiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
            createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Artifact testClasspathPlatformEng = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Artifact testClasspathJUnit4 = new DefaultArtifact( "junit", "junit",
            createFromVersion( "4.12" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Artifact testClasspathHamcrest = new DefaultArtifact( "org.hamcrest", "hamcrest-core",
            createFromVersion( "1.3" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Artifact testClasspathOpentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
            createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Artifact testClasspathCommons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Collection<Artifact> testArtifacts = asList( testClasspathSomeTestArtifact, testClasspathVintage,
            testClasspathApiguardian, testClasspathPlatformEng, testClasspathJUnit4, testClasspathHamcrest,
            testClasspathOpentest4j, testClasspathCommons );

        setProjectDepedenciesToMojo( testArtifacts.toArray( new Artifact[testArtifacts.size()] ) );

        File classesDirectory = new File( "target/classes" );

        File testClassesDirectory = new File( "target/test-classes" );

        TestClassPath testClasspathWrapper =
            new TestClassPath( testArtifacts, classesDirectory, testClassesDirectory, null );

        mojo.setRemoteRepositories( Collections.<ArtifactRepository>emptyList() );
        mojo.setProjectRemoteRepositories( Collections.<ArtifactRepository>emptyList() );
        RepositorySystem repositorySystem = mock( RepositorySystem.class );
        final Artifact surefireProvider = new DefaultArtifact( "org.apache.maven.surefire",
            "surefire-junit-platform", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        when( repositorySystem.createDependencyArtifact( any( Dependency.class ) ) ).thenAnswer( new Answer<Artifact>()
        {
            @Override
            public Artifact answer( InvocationOnMock invocation )
            {
                Dependency provider = (Dependency) invocation.getArguments()[0];
                assertThat( provider.getGroupId() ).isEqualTo( "org.apache.maven.surefire" );
                assertThat( provider.getArtifactId() ).isEqualTo( "surefire-junit-platform" );
                return surefireProvider;
            }
        } );
        final ArtifactResolutionResult surefireProviderResolutionResult = mock( ArtifactResolutionResult.class );
        when( repositorySystem.resolve( any( ArtifactResolutionRequest.class ) ) )
            .thenAnswer( new Answer<ArtifactResolutionResult>()
            {
                @Override
                public ArtifactResolutionResult answer( InvocationOnMock invocation )
                {
                    ArtifactResolutionRequest req = (ArtifactResolutionRequest) invocation.getArguments()[0];
                    Artifact resolvable = req.getArtifact();
                    if ( resolvable == surefireProvider )
                    {
                        return surefireProviderResolutionResult;
                    }
                    else if ( "org.junit.platform".equals( resolvable.getGroupId() )
                        && "junit-platform-launcher".equals( resolvable.getArtifactId() )
                        && "1.4.0".equals( resolvable.getVersion() ) )
                    {
                        return createExpectedJUnitPlatformLauncherResolutionResult();
                    }
                    else
                    {
                        fail( resolvable.getGroupId() + ":" + resolvable.getArtifactId() );
                        return null;
                    }
                }
            } );

        Artifact java5 = new DefaultArtifact( "org.apache.maven.surefire", "common-java5",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact launcher = new DefaultArtifact( "org.junit.platform", "junit-platform-launcher",
            createFromVersion( "1.3.2" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact apiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
            createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact engine = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
            createFromVersion( "1.3.2" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact commons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
            createFromVersion( "1.3.2" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact opentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
            createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Set<Artifact> providerArtifacts = new HashSet<>();
        providerArtifacts.add( surefireProvider );
        providerArtifacts.add( java5 );
        providerArtifacts.add( launcher );
        providerArtifacts.add( apiguardian );
        providerArtifacts.add( engine );
        providerArtifacts.add( commons );
        providerArtifacts.add( opentest4j );
        when( surefireProviderResolutionResult.getArtifacts() )
            .thenReturn( providerArtifacts );

        mojo.setRepositorySystem( repositorySystem );
        mojo.setLogger( mock( Logger.class ) );

        invokeMethod( mojo, "setupStuff" );

        PluginDescriptor pluginDescriptor = mock( PluginDescriptor.class );
        mojo.setPluginDescriptor( pluginDescriptor );
        Plugin p = mock( Plugin.class );
        when( pluginDescriptor.getPlugin() )
            .thenReturn( p );
        when( p.getDependencies() )
            .thenReturn( Collections.<Dependency>emptyList() );

        Artifact junitPlatformArtifact = invokeMethod( mojo, "getJUnit5Artifact" );
        assertThat( junitPlatformArtifact.getGroupId() ).isEqualTo( "org.junit.platform" );
        assertThat( junitPlatformArtifact.getArtifactId() ).isEqualTo( "junit-platform-commons" );
        assertThat( junitPlatformArtifact.getVersion() ).isEqualTo( "1.4.0" );

        JUnitPlatformProviderInfo prov =
            mojo.createJUnitPlatformProviderInfo( junitPlatformArtifact, testClasspathWrapper );

        assertThat( prov.isApplicable() )
            .isTrue();

        Artifact expectedProvider = new DefaultArtifact( "org.apache.maven.surefire", "surefire-junit-platform",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedCommonJava5 = new DefaultArtifact( "org.apache.maven.surefire", "common-java5",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedLauncher = new DefaultArtifact( "org.junit.platform", "junit-platform-launcher",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        assertThat( prov.getProviderClasspath() )
            .hasSize( 3 )
            .containsOnly( expectedProvider, expectedCommonJava5, expectedLauncher );

        assertThat( testClasspathWrapper.getTestDependencies() )
            .hasSize( 8 )
            .containsEntry( "third.party:artifact", testClasspathSomeTestArtifact )
            .containsEntry( "org.junit.vintage:junit-vintage-engine", testClasspathVintage )
            .containsEntry( "org.apiguardian:apiguardian-api", testClasspathApiguardian )
            .containsEntry( "org.junit.platform:junit-platform-engine", testClasspathPlatformEng )
            .containsEntry( "junit:junit", testClasspathJUnit4 )
            .containsEntry( "org.hamcrest:hamcrest-core", testClasspathHamcrest )
            .containsEntry( "org.opentest4j:opentest4j", testClasspathOpentest4j )
            .containsEntry( "org.junit.platform:junit-platform-commons", testClasspathCommons );
    }

    @Test
    public void shouldSmartlyResolveJUnit5ProviderWithJUnit5Commons() throws Exception
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact( new DefaultArtifact( "dummy", "pom", createFromVersion( "1.0.0" ),
            null, "jar", null, mock( ArtifactHandler.class ) ) );
        mojo.setProject( mavenProject );

        final VersionRange surefireVersion = createFromVersion( "1" );

        Artifact testClasspathSomeTestArtifact = new DefaultArtifact( "third.party", "artifact",
            createFromVersion( "1.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Artifact testClasspathCommons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Artifact testClasspathApiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
            createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Collection<Artifact> testArtifacts =
            asList( testClasspathSomeTestArtifact, testClasspathApiguardian, testClasspathCommons );

        setProjectDepedenciesToMojo( testArtifacts.toArray( new Artifact[testArtifacts.size()] ) );

        File classesDirectory = new File( "target/classes" );

        File testClassesDirectory = new File( "target/test-classes" );

        TestClassPath testClasspathWrapper =
            new TestClassPath( testArtifacts, classesDirectory, testClassesDirectory, null );

        mojo.setRemoteRepositories( Collections.<ArtifactRepository>emptyList() );
        mojo.setProjectRemoteRepositories( Collections.<ArtifactRepository>emptyList() );
        RepositorySystem repositorySystem = mock( RepositorySystem.class );
        final Artifact surefireProvider = new DefaultArtifact( "org.apache.maven.surefire",
            "surefire-junit-platform", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        when( repositorySystem.createDependencyArtifact( any( Dependency.class ) ) ).thenAnswer( new Answer<Artifact>()
        {
            @Override
            public Artifact answer( InvocationOnMock invocation )
            {
                Dependency provider = (Dependency) invocation.getArguments()[0];
                assertThat( provider.getGroupId() ).isEqualTo( "org.apache.maven.surefire" );
                assertThat( provider.getArtifactId() ).isEqualTo( "surefire-junit-platform" );
                return surefireProvider;
            }
        } );

        when( repositorySystem.resolve( any( ArtifactResolutionRequest.class ) ) )
            .thenAnswer( new Answer<ArtifactResolutionResult>()
            {
                @Override
                public ArtifactResolutionResult answer( InvocationOnMock invocation )
                {
                    ArtifactResolutionRequest req = (ArtifactResolutionRequest) invocation.getArguments()[0];
                    Artifact resolvable = req.getArtifact();
                    if ( resolvable == surefireProvider )
                    {
                        return createSurefireProviderResolutionResult( surefireVersion );
                    }
                    else if ( "org.junit.platform".equals( resolvable.getGroupId() )
                        && "junit-platform-launcher".equals( resolvable.getArtifactId() )
                        && "1.4.0".equals( resolvable.getVersion() ) )
                    {
                        return createExpectedJUnitPlatformLauncherResolutionResult();
                    }
                    else
                    {
                        fail( resolvable.getGroupId() + ":" + resolvable.getArtifactId() );
                        return null;
                    }
                }
            } );

        mojo.setRepositorySystem( repositorySystem );
        mojo.setLogger( mock( Logger.class ) );

        invokeMethod( mojo, "setupStuff" );

        when( mojo.getSession().getProjectBuildingRequest() )
            .thenReturn( mock( ProjectBuildingRequest.class ) );

        PluginDescriptor pluginDescriptor = mock( PluginDescriptor.class );
        mojo.setPluginDescriptor( pluginDescriptor );
        Plugin p = mock( Plugin.class );
        when( pluginDescriptor.getPlugin() )
            .thenReturn( p );
        when( p.getDependencies() )
            .thenReturn( Collections.<Dependency>emptyList() );

        Artifact junitPlatformArtifact = invokeMethod( mojo, "getJUnit5Artifact" );
        assertThat( junitPlatformArtifact.getGroupId() ).isEqualTo( "org.junit.platform" );
        assertThat( junitPlatformArtifact.getArtifactId() ).isEqualTo( "junit-platform-commons" );
        assertThat( junitPlatformArtifact.getVersion() ).isEqualTo( "1.4.0" );

        JUnitPlatformProviderInfo prov =
            mojo.createJUnitPlatformProviderInfo( junitPlatformArtifact, testClasspathWrapper );

        assertThat( prov.isApplicable() )
            .isTrue();

        Set<Artifact> resolvedProviderArtifacts = prov.getProviderClasspath();

        Artifact provider = new DefaultArtifact( "org.apache.maven.surefire", "surefire-junit-platform",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact java5 = new DefaultArtifact( "org.apache.maven.surefire", "common-java5",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact launcher = new DefaultArtifact( "org.junit.platform", "junit-platform-launcher",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact engine = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact opentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
            createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );
        assertThat( resolvedProviderArtifacts )
            .hasSize( 5 )
            .containsOnly( provider, java5, launcher, engine, opentest4j );

        assertThat( testClasspathWrapper.getTestDependencies() )
            .hasSize( 3 )
            .containsEntry( "third.party:artifact", testClasspathSomeTestArtifact )
            .containsEntry( "org.junit.platform:junit-platform-commons", testClasspathCommons )
            .containsEntry( "org.apiguardian:apiguardian-api", testClasspathApiguardian );
    }

    @Test
    public void shouldSmartlyResolveJUnit5ProviderWithJUnit5Engine() throws Exception
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact( new DefaultArtifact( "dummy", "pom", createFromVersion( "1.0.0" ),
            null, "jar", null, mock( ArtifactHandler.class ) ) );
        mojo.setProject( mavenProject );

        final VersionRange surefireVersion = createFromVersion( "1" );

        final Artifact testClasspathSomeTestArtifact = new DefaultArtifact( "third.party", "artifact",
            createFromVersion( "1.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathJUnit5 = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathApiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
            createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathCommons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathOpentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
            createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Collection<Artifact> testArtifacts = asList( testClasspathSomeTestArtifact, testClasspathJUnit5,
            testClasspathApiguardian, testClasspathCommons, testClasspathOpentest4j );

        setProjectDepedenciesToMojo( testArtifacts.toArray( new Artifact[testArtifacts.size()] ) );

        File classesDirectory = new File( "target/classes" );

        File testClassesDirectory = new File( "target/test-classes" );

        TestClassPath testClasspathWrapper =
            new TestClassPath( testArtifacts, classesDirectory, testClassesDirectory, null );

        mojo.setRemoteRepositories( Collections.<ArtifactRepository>emptyList() );
        mojo.setProjectRemoteRepositories( Collections.<ArtifactRepository>emptyList() );
        RepositorySystem repositorySystem = mock( RepositorySystem.class );
        final Artifact surefireProvider = new DefaultArtifact( "org.apache.maven.surefire",
            "surefire-junit-platform", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        when( repositorySystem.createDependencyArtifact( any( Dependency.class ) ) ).thenAnswer( new Answer<Artifact>()
        {
            @Override
            public Artifact answer( InvocationOnMock invocation )
            {
                Dependency provider = (Dependency) invocation.getArguments()[0];
                assertThat( provider.getGroupId() ).isEqualTo( "org.apache.maven.surefire" );
                assertThat( provider.getArtifactId() ).isEqualTo( "surefire-junit-platform" );
                return surefireProvider;
            }
        } );

        when( repositorySystem.resolve( any( ArtifactResolutionRequest.class ) ) )
            .thenAnswer( new Answer<ArtifactResolutionResult>()
            {
                @Override
                public ArtifactResolutionResult answer( InvocationOnMock invocation )
                {
                    ArtifactResolutionRequest req = (ArtifactResolutionRequest) invocation.getArguments()[0];
                    Artifact resolvable = req.getArtifact();
                    if ( resolvable == surefireProvider )
                    {
                        return createSurefireProviderResolutionResult( surefireVersion );
                    }
                    else if ( "org.junit.platform".equals( resolvable.getGroupId() )
                        && "junit-platform-launcher".equals( resolvable.getArtifactId() )
                        && "1.4.0".equals( resolvable.getVersion() ) )
                    {
                        return createExpectedJUnitPlatformLauncherResolutionResult();
                    }
                    else
                    {
                        fail( resolvable.getGroupId() + ":" + resolvable.getArtifactId() );
                        return null;
                    }
                }
            } );

        mojo.setRepositorySystem( repositorySystem );
        mojo.setLogger( mock( Logger.class ) );

        invokeMethod( mojo, "setupStuff" );

        Artifact junitPlatformArtifact = invokeMethod( mojo, "getJUnit5Artifact" );
        assertThat( junitPlatformArtifact.getGroupId() ).isEqualTo( "org.junit.platform" );
        assertThat( junitPlatformArtifact.getArtifactId() ).isEqualTo( "junit-platform-commons" );
        assertThat( junitPlatformArtifact.getVersion() ).isEqualTo( "1.4.0" );

        JUnitPlatformProviderInfo prov =
            mojo.createJUnitPlatformProviderInfo( junitPlatformArtifact, testClasspathWrapper );

        assertThat( prov.isApplicable() )
            .isTrue();

        Set<Artifact> resolvedProviderArtifacts = prov.getProviderClasspath();

        Artifact java5 = new DefaultArtifact( "org.apache.maven.surefire", "common-java5",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact launcher = new DefaultArtifact( "org.junit.platform", "junit-platform-launcher",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        assertThat( resolvedProviderArtifacts )
            .hasSize( 3 )
            .containsOnly( surefireProvider, java5, launcher );

        assertThat( testClasspathWrapper.getTestDependencies() )
            .hasSize( 5 )
            .containsEntry( "third.party:artifact", testClasspathSomeTestArtifact )
            .containsEntry( "org.junit.platform:junit-platform-engine", testClasspathJUnit5 )
            .containsEntry( "org.apiguardian:apiguardian-api", testClasspathApiguardian )
            .containsEntry( "org.junit.platform:junit-platform-commons", testClasspathCommons )
            .containsEntry( "org.opentest4j:opentest4j", testClasspathOpentest4j );
    }

    @Test
    public void shouldSmartlyResolveJUnit5ProviderWithJupiterApi() throws Exception
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact( new DefaultArtifact( "dummy", "pom", createFromVersion( "1.0.0" ),
            null, "jar", null, mock( ArtifactHandler.class ) ) );
        mojo.setProject( mavenProject );

        final VersionRange surefireVersion = createFromVersion( "1" );

        final Artifact testClasspathSomeTestArtifact = new DefaultArtifact( "third.party", "artifact",
            createFromVersion( "1.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathJupiterApi = new DefaultArtifact( "org.junit.jupiter", "junit-jupiter-api",
            createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathApiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
            createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathCommons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathOpentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
            createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Collection<Artifact> testArtifacts = asList( testClasspathSomeTestArtifact, testClasspathJupiterApi,
            testClasspathApiguardian, testClasspathCommons, testClasspathOpentest4j );

        setProjectDepedenciesToMojo( testArtifacts.toArray( new Artifact[testArtifacts.size()] ) );

        File classesDirectory = new File( "target/classes" );

        File testClassesDirectory = new File( "target/test-classes" );

        TestClassPath testClasspathWrapper =
            new TestClassPath( testArtifacts, classesDirectory, testClassesDirectory, null );

        mojo.setRemoteRepositories( Collections.<ArtifactRepository>emptyList() );
        mojo.setProjectRemoteRepositories( Collections.<ArtifactRepository>emptyList() );
        RepositorySystem repositorySystem = mock( RepositorySystem.class );
        final Artifact surefireProvider = new DefaultArtifact( "org.apache.maven.surefire",
            "surefire-junit-platform", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        when( repositorySystem.createDependencyArtifact( any( Dependency.class ) ) ).thenAnswer( new Answer<Artifact>()
        {
            @Override
            public Artifact answer( InvocationOnMock invocation )
            {
                Dependency provider = (Dependency) invocation.getArguments()[0];
                assertThat( provider.getGroupId() ).isEqualTo( "org.apache.maven.surefire" );
                assertThat( provider.getArtifactId() ).isEqualTo( "surefire-junit-platform" );
                return surefireProvider;
            }
        } );

        when( repositorySystem.resolve( any( ArtifactResolutionRequest.class ) ) )
            .thenAnswer( new Answer<ArtifactResolutionResult>()
            {
                @Override
                public ArtifactResolutionResult answer( InvocationOnMock invocation )
                {
                    ArtifactResolutionRequest req = (ArtifactResolutionRequest) invocation.getArguments()[0];
                    Artifact resolvable = req.getArtifact();
                    if ( resolvable == surefireProvider )
                    {
                        return createSurefireProviderResolutionResult( surefireVersion );
                    }
                    else if ( "org.junit.platform".equals( resolvable.getGroupId() )
                        && "junit-platform-launcher".equals( resolvable.getArtifactId() )
                        && "1.4.0".equals( resolvable.getVersion() ) )
                    {
                        return createExpectedJUnitPlatformLauncherResolutionResult();
                    }
                    else if ( "org.junit.jupiter".equals( resolvable.getGroupId() )
                        && "junit-jupiter-engine".equals( resolvable.getArtifactId() )
                        && "5.4.0".equals( resolvable.getVersion() ) )
                    {
                        return createJupiterEngineResolutionResult();
                    }
                    else
                    {
                        fail( resolvable.getGroupId() + ":" + resolvable.getArtifactId() );
                        return null;
                    }
                }
            } );

        mojo.setRepositorySystem( repositorySystem );
        mojo.setLogger( mock( Logger.class ) );

        invokeMethod( mojo, "setupStuff" );

        when( mojo.getSession().getProjectBuildingRequest() )
            .thenReturn( mock( ProjectBuildingRequest.class ) );

        PluginDescriptor pluginDescriptor = mock( PluginDescriptor.class );
        mojo.setPluginDescriptor( pluginDescriptor );
        Plugin p = mock( Plugin.class );
        when( pluginDescriptor.getPlugin() )
            .thenReturn( p );
        when( p.getDependencies() )
            .thenReturn( Collections.<Dependency>emptyList() );

        Artifact junitPlatformArtifact = invokeMethod( mojo, "getJUnit5Artifact" );
        assertThat( junitPlatformArtifact.getGroupId() ).isEqualTo( "org.junit.platform" );
        assertThat( junitPlatformArtifact.getArtifactId() ).isEqualTo( "junit-platform-commons" );
        assertThat( junitPlatformArtifact.getVersion() ).isEqualTo( "1.4.0" );

        JUnitPlatformProviderInfo prov =
            mojo.createJUnitPlatformProviderInfo( junitPlatformArtifact, testClasspathWrapper );

        assertThat( prov.isApplicable() )
            .isTrue();

        Set<Artifact> resolvedProviderArtifacts = prov.getProviderClasspath();

        Artifact java5 = new DefaultArtifact( "org.apache.maven.surefire", "common-java5",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact launcher = new DefaultArtifact( "org.junit.platform", "junit-platform-launcher",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact jupiterEngine = new DefaultArtifact( "org.junit.jupiter", "junit-jupiter-engine",
            createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact platformEngine = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        assertThat( resolvedProviderArtifacts )
            .hasSize( 5 )
            .containsOnly( surefireProvider, java5, launcher, jupiterEngine, platformEngine );

        assertThat( testClasspathWrapper.getTestDependencies() )
            .hasSize( 5 )
            .containsEntry( "third.party:artifact", testClasspathSomeTestArtifact )
            .containsEntry( "org.junit.jupiter:junit-jupiter-api", testClasspathJupiterApi )
            .containsEntry( "org.apiguardian:apiguardian-api", testClasspathApiguardian )
            .containsEntry( "org.junit.platform:junit-platform-commons", testClasspathCommons )
            .containsEntry( "org.opentest4j:opentest4j", testClasspathOpentest4j );
    }

    @Test
    public void shouldSmartlyResolveJUnit5ProviderWithJupiterEngine() throws Exception
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact( new DefaultArtifact( "dummy", "pom", createFromVersion( "1.0.0" ),
            null, "jar", null, mock( ArtifactHandler.class ) ) );
        mojo.setProject( mavenProject );

        final VersionRange surefireVersion = createFromVersion( "1" );

        final Artifact testClasspathSomeTestArtifact = new DefaultArtifact( "third.party", "artifact",
            createFromVersion( "1.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathJupiterEngine = new DefaultArtifact( "org.junit.jupiter", "junit-jupiter-engine",
            createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathPlatformEngine = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathJupiterApi = new DefaultArtifact( "org.junit.jupiter", "junit-jupiter-api",
            createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathApiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
            createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathCommons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathOpentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
            createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Collection<Artifact> testArtifacts = asList( testClasspathSomeTestArtifact, testClasspathJupiterEngine,
            testClasspathPlatformEngine, testClasspathJupiterApi, testClasspathApiguardian, testClasspathCommons,
            testClasspathOpentest4j );

        setProjectDepedenciesToMojo( testArtifacts.toArray( new Artifact[testArtifacts.size()] ) );

        File classesDirectory = new File( "target/classes" );

        File testClassesDirectory = new File( "target/test-classes" );

        TestClassPath testClasspathWrapper =
            new TestClassPath( testArtifacts, classesDirectory, testClassesDirectory, null );

        mojo.setRemoteRepositories( Collections.<ArtifactRepository>emptyList() );
        mojo.setProjectRemoteRepositories( Collections.<ArtifactRepository>emptyList() );
        RepositorySystem repositorySystem = mock( RepositorySystem.class );
        final Artifact surefireProvider = new DefaultArtifact( "org.apache.maven.surefire",
            "surefire-junit-platform", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        when( repositorySystem.createDependencyArtifact( any( Dependency.class ) ) ).thenAnswer( new Answer<Artifact>()
        {
            @Override
            public Artifact answer( InvocationOnMock invocation )
            {
                Dependency provider = (Dependency) invocation.getArguments()[0];
                assertThat( provider.getGroupId() ).isEqualTo( "org.apache.maven.surefire" );
                assertThat( provider.getArtifactId() ).isEqualTo( "surefire-junit-platform" );
                return surefireProvider;
            }
        } );

        when( repositorySystem.resolve( any( ArtifactResolutionRequest.class ) ) )
            .thenAnswer( new Answer<ArtifactResolutionResult>()
            {
                @Override
                public ArtifactResolutionResult answer( InvocationOnMock invocation )
                {
                    ArtifactResolutionRequest req = (ArtifactResolutionRequest) invocation.getArguments()[0];
                    Artifact resolvable = req.getArtifact();
                    if ( resolvable == surefireProvider )
                    {
                        return createSurefireProviderResolutionResult( surefireVersion );
                    }
                    else if ( "org.junit.platform".equals( resolvable.getGroupId() )
                        && "junit-platform-launcher".equals( resolvable.getArtifactId() )
                        && "1.4.0".equals( resolvable.getVersion() ) )
                    {
                        return createExpectedJUnitPlatformLauncherResolutionResult();
                    }
                    else
                    {
                        fail( resolvable.getGroupId() + ":" + resolvable.getArtifactId() );
                        return null;
                    }
                }
            } );

        mojo.setRepositorySystem( repositorySystem );
        mojo.setLogger( mock( Logger.class ) );

        invokeMethod( mojo, "setupStuff" );

        Artifact junitPlatformArtifact = invokeMethod( mojo, "getJUnit5Artifact" );
        assertThat( junitPlatformArtifact.getGroupId() ).isEqualTo( "org.junit.platform" );
        assertThat( junitPlatformArtifact.getArtifactId() ).isEqualTo( "junit-platform-commons" );
        assertThat( junitPlatformArtifact.getVersion() ).isEqualTo( "1.4.0" );

        JUnitPlatformProviderInfo prov =
            mojo.createJUnitPlatformProviderInfo( junitPlatformArtifact, testClasspathWrapper );

        assertThat( prov.isApplicable() )
            .isTrue();

        Set<Artifact> resolvedProviderArtifacts = prov.getProviderClasspath();

        Artifact java5 = new DefaultArtifact( "org.apache.maven.surefire", "common-java5",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact launcher = new DefaultArtifact( "org.junit.platform", "junit-platform-launcher",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        assertThat( resolvedProviderArtifacts )
            .hasSize( 3 )
            .containsOnly( surefireProvider, java5, launcher );

        assertThat( testClasspathWrapper.getTestDependencies() )
            .hasSize( 7 )
            .containsEntry( "third.party:artifact", testClasspathSomeTestArtifact )
            .containsEntry( "org.junit.jupiter:junit-jupiter-engine", testClasspathJupiterEngine )
            .containsEntry( "org.junit.platform:junit-platform-engine", testClasspathPlatformEngine )
            .containsEntry( "org.junit.jupiter:junit-jupiter-api", testClasspathJupiterApi )
            .containsEntry( "org.apiguardian:apiguardian-api", testClasspathApiguardian )
            .containsEntry( "org.junit.platform:junit-platform-commons", testClasspathCommons )
            .containsEntry( "org.opentest4j:opentest4j", testClasspathOpentest4j );
    }

    @Test
    public void shouldSmartlyResolveJUnit5ProviderWithJupiterEngineInPluginDependencies() throws Exception
    {
        final VersionRange surefireVersion = createFromVersion( "1" );

        final Artifact plugin = new DefaultArtifact( "org.apache.maven.surefire", "maven-surefire-plugin",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact forkedBooter = new DefaultArtifact( "org.apache.maven.surefire",
            "surefire-booter", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact pluginDepJupiterEngine = new DefaultArtifact( "org.junit.jupiter", "junit-jupiter-engine",
            createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact pluginDepPlatformEngine = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact pluginDepJupiterApi = new DefaultArtifact( "org.junit.jupiter", "junit-jupiter-api",
            createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact pluginDepApiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
            createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact pluginDepCommons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact pluginDepOpentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
            createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );

        mojo.getPluginArtifactMap().put( "org.apache.maven.surefire:maven-surefire-plugin", plugin );
        mojo.getPluginArtifactMap().put( "org.apache.maven.surefire:surefire-booter", forkedBooter );
        mojo.getPluginArtifactMap().put( "org.junit.jupiter:junit-jupiter-engine", pluginDepJupiterEngine );
        mojo.getPluginArtifactMap().put( "org.junit.platform:junit-platform-engine", pluginDepPlatformEngine );
        mojo.getPluginArtifactMap().put( "org.junit.jupiter:junit-jupiter-api", pluginDepJupiterApi );
        mojo.getPluginArtifactMap().put( "org.apiguardian:apiguardian-api", pluginDepApiguardian );
        mojo.getPluginArtifactMap().put( "org.junit.platform:junit-platform-commons", pluginDepCommons );
        mojo.getPluginArtifactMap().put( "org.opentest4j:opentest4j", pluginDepOpentest4j );

        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact( new DefaultArtifact( "dummy", "pom", createFromVersion( "1.0.0" ),
            null, "jar", null, mock( ArtifactHandler.class ) ) );
        mojo.setProject( mavenProject );

        final Artifact testClasspathSomeTestArtifact = new DefaultArtifact( "third.party", "artifact",
            createFromVersion( "1.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathJupiterApi = new DefaultArtifact( "org.junit.jupiter", "junit-jupiter-api",
            createFromVersion( "5.3.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathApiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
            createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathCommons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathOpentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
            createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Collection<Artifact> testArtifacts = asList( testClasspathSomeTestArtifact, testClasspathJupiterApi,
            testClasspathApiguardian, testClasspathCommons, testClasspathOpentest4j );

        setProjectDepedenciesToMojo( testArtifacts.toArray( new Artifact[testArtifacts.size()] ) );

        File classesDirectory = new File( "target/classes" );

        File testClassesDirectory = new File( "target/test-classes" );

        TestClassPath testClasspathWrapper =
            new TestClassPath( testArtifacts, classesDirectory, testClassesDirectory, null );

        mojo.setRemoteRepositories( Collections.<ArtifactRepository>emptyList() );
        mojo.setProjectRemoteRepositories( Collections.<ArtifactRepository>emptyList() );
        RepositorySystem repositorySystem = mock( RepositorySystem.class );

        final Artifact surefireProvider = new DefaultArtifact( "org.apache.maven.surefire",
            "surefire-junit-platform", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        when( repositorySystem.createDependencyArtifact( any( Dependency.class ) ) ).thenAnswer( new Answer<Artifact>()
        {
            @Override
            public Artifact answer( InvocationOnMock invocation )
            {
                Dependency dependency = (Dependency) invocation.getArguments()[0];
                if ( dependency.getArtifactId().equals( "surefire-junit-platform" ) )
                {
                    return surefireProvider;
                }
                else if ( dependency.getArtifactId().equals( "junit-jupiter-engine" ) )
                {
                    return pluginDepJupiterEngine;
                }
                else
                {
                    fail( dependency.getGroupId() + ":" + dependency.getArtifactId() );
                    return null;
                }
            }
        } );

        final ArtifactResolutionResult pluginDepJupiterEngineResolutionResult = mock( ArtifactResolutionResult.class );

        when( repositorySystem.resolve( any( ArtifactResolutionRequest.class ) ) )
            .thenAnswer( new Answer<ArtifactResolutionResult>()
            {
                @Override
                public ArtifactResolutionResult answer( InvocationOnMock invocation )
                {
                    ArtifactResolutionRequest req = (ArtifactResolutionRequest) invocation.getArguments()[0];
                    Artifact resolvable = req.getArtifact();
                    if ( resolvable == surefireProvider )
                    {
                        return createSurefireProviderResolutionResult( surefireVersion );
                    }
                    else if ( resolvable == pluginDepJupiterEngine )
                    {
                        return pluginDepJupiterEngineResolutionResult;
                    }
                    else if ( "org.junit.platform".equals( resolvable.getGroupId() )
                        && "junit-platform-launcher".equals( resolvable.getArtifactId() )
                        && "1.4.0".equals( resolvable.getVersion() ) )
                    {
                        return createExpectedJUnitPlatformLauncherResolutionResult();
                    }
                    else
                    {
                        fail( resolvable.getGroupId() + ":" + resolvable.getArtifactId() );
                        return null;
                    }
                }
            } );

        mojo.setRepositorySystem( repositorySystem );
        mojo.setLogger( mock( Logger.class ) );

        Set<Artifact> pluginDepJupiterEngineArtifacts = new HashSet<>();
        pluginDepJupiterEngineArtifacts.add( pluginDepJupiterEngine );
        pluginDepJupiterEngineArtifacts.add( pluginDepPlatformEngine );
        pluginDepJupiterEngineArtifacts.add( pluginDepJupiterApi );
        pluginDepJupiterEngineArtifacts.add( pluginDepApiguardian );
        pluginDepJupiterEngineArtifacts.add( pluginDepCommons );
        pluginDepJupiterEngineArtifacts.add( pluginDepOpentest4j );

        when( pluginDepJupiterEngineResolutionResult.getArtifacts() ).thenReturn( pluginDepJupiterEngineArtifacts );

        invokeMethod( mojo, "setupStuff" );

        Artifact junitPlatformArtifact = invokeMethod( mojo, "getJUnit5Artifact" );
        assertThat( junitPlatformArtifact.getGroupId() ).isEqualTo( "org.junit.platform" );
        assertThat( junitPlatformArtifact.getArtifactId() ).isEqualTo( "junit-platform-engine" );
        assertThat( junitPlatformArtifact.getVersion() ).isEqualTo( "1.4.0" );

        JUnitPlatformProviderInfo prov =
            mojo.createJUnitPlatformProviderInfo( junitPlatformArtifact, testClasspathWrapper );

        assertThat( prov.isApplicable() )
            .isTrue();

        when( mojo.getSession().getProjectBuildingRequest() )
            .thenReturn( mock( ProjectBuildingRequest.class ) );

        PluginDescriptor pluginDescriptor = mock( PluginDescriptor.class );
        mojo.setPluginDescriptor( pluginDescriptor );
        Plugin p = mock( Plugin.class );
        when( pluginDescriptor.getPlugin() )
            .thenReturn( p );
        List<Dependency> directPluginDependencies = toDependencies( pluginDepJupiterEngine );
        when( p.getDependencies() )
            .thenReturn( directPluginDependencies );

        Set<Artifact> resolvedProviderArtifacts = prov.getProviderClasspath();

        Artifact java5 = new DefaultArtifact( "org.apache.maven.surefire", "common-java5",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact launcher = new DefaultArtifact( "org.junit.platform", "junit-platform-launcher",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact jupiterEngine = new DefaultArtifact( "org.junit.jupiter", "junit-jupiter-engine",
            createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact platformEngine = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        assertThat( resolvedProviderArtifacts )
            .hasSize( 5 )
            .containsOnly( surefireProvider, java5, launcher, jupiterEngine, platformEngine );

        assertThat( testClasspathWrapper.getTestDependencies() )
            .hasSize( 5 )
            .containsEntry( "third.party:artifact", testClasspathSomeTestArtifact )
            .containsEntry( "org.junit.jupiter:junit-jupiter-api", testClasspathJupiterApi )
            .containsEntry( "org.apiguardian:apiguardian-api", testClasspathApiguardian )
            .containsEntry( "org.junit.platform:junit-platform-commons", testClasspathCommons )
            .containsEntry( "org.opentest4j:opentest4j", testClasspathOpentest4j );
    }

    @Test
    public void shouldConvertJunitEngineParameters() throws Exception
    {
        Properties properties = new Properties();
        setInternalState( mojo, "properties", properties );

        invokeMethod( mojo, "convertJunitEngineParameters" );
        assertThat( properties ).isEmpty();

        mojo.setIncludeJUnit5Engines( new String[0] );
        mojo.setExcludeJUnit5Engines( new String[0] );
        invokeMethod( mojo, "convertJunitEngineParameters" );
        assertThat( properties ).isEmpty();

        mojo.setIncludeJUnit5Engines( new String[] {"e1", "e2"} );
        invokeMethod( mojo, "convertJunitEngineParameters" );
        assertThat( properties )
            .containsEntry( "includejunit5engines", "e1,e2" );

        mojo.setExcludeJUnit5Engines( new String[] {"e1", "e2"} );
        invokeMethod( mojo, "convertJunitEngineParameters" );
        assertThat( properties )
            .containsEntry( "excludejunit5engines", "e1,e2" );
    }

    private static ArtifactResolutionResult createJUnitPlatformLauncherResolutionResult(
        Artifact junit5Engine, Artifact apiguardian, Artifact commons, Artifact opentest4j )
    {
        ArtifactResolutionResult launcherResolutionResult = mock( ArtifactResolutionResult.class );
        Set<Artifact> resolvedLauncherArtifacts = new HashSet<>();
        Artifact launcher = new DefaultArtifact( "org.junit.platform", "junit-platform-launcher",
            commons.getVersionRange(), null, "jar", null, mock( ArtifactHandler.class ) );
        resolvedLauncherArtifacts.add( launcher );
        resolvedLauncherArtifacts.add( apiguardian );
        resolvedLauncherArtifacts.add( junit5Engine );
        resolvedLauncherArtifacts.add( commons );
        resolvedLauncherArtifacts.add( opentest4j );
        resolvedLauncherArtifacts.remove( null );
        when( launcherResolutionResult.getArtifacts() )
            .thenReturn( resolvedLauncherArtifacts );
        return launcherResolutionResult;
    }

    private static ArtifactResolutionResult createJupiterEngineResolutionResult()
    {
        ArtifactResolutionResult launcherResolutionResult = mock( ArtifactResolutionResult.class );
        Set<Artifact> resolvedLauncherArtifacts = new HashSet<>();
        resolvedLauncherArtifacts.add( new DefaultArtifact( "org.junit.jupiter", "junit-jupiter-engine",
            createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) ) );
        resolvedLauncherArtifacts.add( new DefaultArtifact( "org.junit.jupiter", "junit-jupiter-api",
            createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) ) );
        resolvedLauncherArtifacts.add( new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) ) );
        resolvedLauncherArtifacts.add( new DefaultArtifact( "org.apiguardian", "apiguardian-api",
            createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) ) );
        resolvedLauncherArtifacts.add( new DefaultArtifact( "org.opentest4j", "opentest4j",
            createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) ) );
        resolvedLauncherArtifacts.add( new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) ) );
        when( launcherResolutionResult.getArtifacts() )
            .thenReturn( resolvedLauncherArtifacts );
        return launcherResolutionResult;
    }

    private static ArtifactResolutionResult createExpectedJUnitPlatformLauncherResolutionResult()
    {
        Artifact engine = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact commons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
            createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact apiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
            createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact opentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
            createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );
        return createJUnitPlatformLauncherResolutionResult( engine, apiguardian, commons, opentest4j );
    }

    private static ArtifactResolutionResult createSurefireProviderResolutionResult( VersionRange surefireVersion )
    {
        ArtifactResolutionResult surefirePlatformResolutionResult = mock( ArtifactResolutionResult.class );

        Artifact provider = new DefaultArtifact( "org.apache.maven.surefire", "surefire-junit-platform",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact java5 = new DefaultArtifact( "org.apache.maven.surefire", "common-java5",
            surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact launcher = new DefaultArtifact( "org.junit.platform", "junit-platform-launcher",
            createFromVersion( "1.3.2" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact apiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
            createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact engine = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
            createFromVersion( "1.3.2" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact commons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
            createFromVersion( "1.3.2" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact opentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
            createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Set<Artifact> providerArtifacts = new HashSet<>();
        providerArtifacts.add( provider );
        providerArtifacts.add( java5 );
        providerArtifacts.add( launcher );
        providerArtifacts.add( apiguardian );
        providerArtifacts.add( engine );
        providerArtifacts.add( commons );
        providerArtifacts.add( opentest4j );

        when( surefirePlatformResolutionResult.getArtifacts() )
            .thenReturn( providerArtifacts );
        return surefirePlatformResolutionResult;
    }

    @Test
    public void shouldVerifyConfigParameters() throws Exception
    {
        Mojo mojo = new Mojo()
        {
            @Override
            public File getTestClassesDirectory()
            {
                return new File( System.getProperty( "user.dir" ), "target/test-classes" );
            }

            @Override
            protected String getEnableProcessChecker()
            {
                return "fake";
            }
        };

        e.expect( MojoFailureException.class );
        e.expectMessage( "Unexpected value 'fake' in the configuration parameter 'enableProcessChecker'." );
        mojo.verifyParameters();
    }

    private void setProjectDepedenciesToMojo( Artifact... deps )
    {
        for ( Artifact dep : deps )
        {
            mojo.getProjectArtifactMap()
                .put( dep.getGroupId() + ":" + dep.getArtifactId(), dep );
        }
    }

    private void addPluginDependencies( Artifact... deps )
    {
        for ( Artifact dep : deps )
        {
            mojo.getPluginArtifactMap()
                .put( dep.getGroupId() + ":" + dep.getArtifactId(), dep );
        }
    }

    /**
     *
     */
    public static class Mojo
        extends AbstractSurefireMojo implements SurefireReportParameters
    {
        private File mainBuildPath;
        private File testClassesDirectory;
        private boolean useModulePath;
        private int failOnFlakeCount;
        private String[] includeJUnit5Engines;
        private String[] excludeJUnit5Engines;
        private List<Artifact> projectTestArtifacts;
        private File includesFile;
        private File excludesFile;
        private List<String> includes;
        private List<String> excludes;
        private String test;
        private boolean testFailureIgnore;

        private JUnitPlatformProviderInfo createJUnitPlatformProviderInfo( Artifact junitPlatformArtifact,
                                                                           TestClassPath testClasspathWrapper )
        {
            return new JUnitPlatformProviderInfo( null, junitPlatformArtifact, testClasspathWrapper );
        }

        void setProjectTestArtifacts( List<Artifact> projectTestArtifacts )
        {
            this.projectTestArtifacts = projectTestArtifacts;
        }

        @Override
        List<Artifact> getProjectTestArtifacts()
        {
            return projectTestArtifacts;
        }

        @Override
        protected void logDebugOrCliShowErrors( String s )
        {
            // do nothing
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
        public boolean isTestFailureIgnore()
        {
            return testFailureIgnore;
        }

        @Override
        public void setTestFailureIgnore( boolean testFailureIgnore )
        {
            this.testFailureIgnore = testFailureIgnore;
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
            return testClassesDirectory;
        }

        @Override
        public void setTestClassesDirectory( File testClassesDirectory )
        {
            this.testClassesDirectory = testClassesDirectory;
        }

        @Override
        public File getMainBuildPath()
        {
            return mainBuildPath;
        }

        @Override
        public void setMainBuildPath( File mainBuildPath )
        {
            this.mainBuildPath = mainBuildPath;
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
            return test;
        }

        @Override
        public void setTest( String test )
        {
            this.test = test;
        }

        @Override
        public List<String> getIncludes()
        {
            return includes;
        }

        @Override
        public void setIncludes( List<String> includes )
        {
            this.includes = includes;
        }

        void setIncludesFile( File includesFile )
        {
            this.includesFile = includesFile;
        }

        @Override
        public File getIncludesFile()
        {
            return includesFile;
        }

        @Override
        public List<String> getExcludes()
        {
            return excludes;
        }

        @Override
        public void setExcludes( List<String> excludes )
        {
            this.excludes = excludes;
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

        void setExcludesFile( File excludesFile )
        {
            this.excludesFile = excludesFile;
        }

        @Override
        public File getExcludesFile()
        {
            return excludesFile;
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
            return useModulePath;
        }

        @Override
        protected void setUseModulePath( boolean useModulePath )
        {
            this.useModulePath = useModulePath;
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
            return new DefaultArtifact( "org.apache.maven.surefire", "maven-surefire-plugin", createFromVersion( "1" ),
                null, "jar", null, mock( ArtifactHandler.class ) );
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

        public void setToolchain( Toolchain toolchain )
        {
            setInternalState( this, "toolchain", toolchain );
        }

        public void setJvm( String jvm )
        {
            setInternalState( this, "jvm", jvm );
        }

        @Override
        public int getFailOnFlakeCount()
        {
            return failOnFlakeCount;
        }

        @Override
        public void setFailOnFlakeCount( int failOnFlakeCount )
        {
            this.failOnFlakeCount = failOnFlakeCount;
        }

        @Override
        public String[] getIncludeJUnit5Engines()
        {
            return includeJUnit5Engines;
        }

        @Override
        public void setIncludeJUnit5Engines( String[] includeJUnit5Engines )
        {
            this.includeJUnit5Engines = includeJUnit5Engines;
        }

        @Override
        public String[] getExcludeJUnit5Engines()
        {
            return excludeJUnit5Engines;
        }

        @Override
        public void setExcludeJUnit5Engines( String[] excludeJUnit5Engines )
        {
            this.excludeJUnit5Engines = excludeJUnit5Engines;
        }
    }

    @Test
    public void shouldNotPerformMethodFilteringOnIncludes() throws Exception
    {
        Mojo plugin = new Mojo();

        File includesExcludes = File.createTempFile( "surefire", "-includes" );
        FileUtils.write( includesExcludes, "AnotherTest#method" , UTF_8 );
        plugin.setIncludesFile( includesExcludes );

        List<String> includes = new LinkedList<>();
        includes.add( "AnotherTest#method " );
        plugin.setIncludes( includes );

        VersionRange version = VersionRange.createFromVersion( "1.0" );
        ArtifactHandler handler = new DefaultArtifactHandler();
        Artifact testDeps = new DefaultArtifact( "g", "a", version, "compile", "jar", null, handler );
        File artifactFile = File.createTempFile( "surefire", ".jar" );
        artifactFile.deleteOnExit();
        testDeps.setFile( artifactFile );
        plugin.setProjectTestArtifacts( singletonList( testDeps ) );
        plugin.setDependenciesToScan( new String[] { "g:a" } );

        e.expectMessage( "Method filter prohibited in includes|excludes parameter: AnotherTest#method " );
        plugin.scanDependencies();
    }

    @Test
    public void shouldFilterTestsOnIncludesFile() throws Exception
    {
        Mojo plugin = new Mojo();

        plugin.setLogger( mock( Logger.class ) );

        File includes = File.createTempFile( "surefire", "-includes" );
        FileUtils.write( includes, "AnotherTest#method" , UTF_8 );
        plugin.setIncludesFile( includes );

        VersionRange version = VersionRange.createFromVersion( "1.0" );
        ArtifactHandler handler = new DefaultArtifactHandler();
        Artifact testDeps = new DefaultArtifact( "g", "a", version, "compile", "test-jar", null, handler );
        File artifactFile = File.createTempFile( "surefire", "-classes" );
        String classDir = artifactFile.getCanonicalPath();
        assertThat( artifactFile.delete() ).isTrue();
        File classes = new File( classDir );
        assertThat( classes.mkdir() ).isTrue();
        testDeps.setFile( classes );
        assertThat( new File( classes, "AnotherTest.class" ).createNewFile() )
            .isTrue();
        plugin.setProjectTestArtifacts( singletonList( testDeps ) );
        plugin.setDependenciesToScan( new String[] { "g:a" } );

        DefaultScanResult result = plugin.scanDependencies();
        assertThat ( result.getClasses() ).hasSize( 1 );
        assertThat ( result.getClasses().iterator().next() ).isEqualTo( "AnotherTest" );
    }

    @Test
    public void shouldFilterTestsOnExcludesFile() throws Exception
    {
        Mojo plugin = new Mojo();

        plugin.setLogger( mock( Logger.class ) );

        File excludes = File.createTempFile( "surefire", "-excludes" );
        FileUtils.write( excludes, "AnotherTest" , UTF_8 );
        plugin.setExcludesFile( excludes );

        VersionRange version = VersionRange.createFromVersion( "1.0" );
        ArtifactHandler handler = new DefaultArtifactHandler();
        Artifact testDeps = new DefaultArtifact( "g", "a", version, "compile", "test-jar", null, handler );
        File artifactFile = File.createTempFile( "surefire", "-classes" );
        String classDir = artifactFile.getCanonicalPath();
        assertThat( artifactFile.delete() ).isTrue();
        File classes = new File( classDir );
        assertThat( classes.mkdir() ).isTrue();
        testDeps.setFile( classes );
        assertThat( new File( classes, "AnotherTest.class" ).createNewFile() )
            .isTrue();
        plugin.setProjectTestArtifacts( singletonList( testDeps ) );
        plugin.setDependenciesToScan( new String[] { "g:a" } );

        DefaultScanResult result = plugin.scanDependencies();
        assertThat ( result.getClasses() )
            .isEmpty();
    }

    @Test
    public void shouldFilterTestsOnExcludes() throws Exception
    {
        Mojo plugin = new Mojo();

        plugin.setLogger( mock( Logger.class ) );

        plugin.setExcludes( singletonList( "AnotherTest" ) );

        VersionRange version = VersionRange.createFromVersion( "1.0" );
        ArtifactHandler handler = new DefaultArtifactHandler();
        Artifact testDeps = new DefaultArtifact( "g", "a", version, "compile", "jar", null, handler );
        File artifactFile = File.createTempFile( "surefire", "-classes" );
        String classDir = artifactFile.getCanonicalPath();
        assertThat( artifactFile.delete() ).isTrue();
        File classes = new File( classDir );
        assertThat( classes.mkdir() ).isTrue();
        testDeps.setFile( classes );
        assertThat( new File( classes, "AnotherTest.class" ).createNewFile() )
            .isTrue();
        plugin.setProjectTestArtifacts( singletonList( testDeps ) );
        plugin.setDependenciesToScan( new String[] { "g:a" } );

        DefaultScanResult result = plugin.scanDependencies();
        assertThat ( result.getClasses() )
            .isEmpty();
    }

    @Test
    public void shouldUseOnlySpecificTests() throws Exception
    {
        Mojo plugin = new Mojo();

        plugin.setLogger( mock( Logger.class ) );

        File includes = File.createTempFile( "surefire", "-includes" );
        FileUtils.write( includes, "AnotherTest" , UTF_8 );
        plugin.setIncludesFile( includes );
        plugin.setTest( "DifferentTest" );

        VersionRange version = VersionRange.createFromVersion( "1.0" );
        ArtifactHandler handler = new DefaultArtifactHandler();
        Artifact testDeps = new DefaultArtifact( "g", "a", version, "compile", "test-jar", null, handler );
        File artifactFile = File.createTempFile( "surefire", "-classes" );
        String classDir = artifactFile.getCanonicalPath();
        assertThat( artifactFile.delete() ).isTrue();
        File classes = new File( classDir );
        assertThat( classes.mkdir() ).isTrue();
        testDeps.setFile( classes );
        assertThat( new File( classes, "AnotherTest.class" ).createNewFile() )
            .isTrue();
        plugin.setProjectTestArtifacts( singletonList( testDeps ) );
        plugin.setDependenciesToScan( new String[] { "g:a" } );

        DefaultScanResult result = plugin.scanDependencies();
        assertThat ( result.getClasses() )
            .isEmpty();
    }

    private static File mockFile( String absolutePath )
    {
        File f = mock( File.class );
        when( f.getAbsolutePath() ).thenReturn( absolutePath );
        return f;
    }

    private static Dependency toDependency( Artifact artifact )
    {
        Dependency dependency = new Dependency();
        dependency.setGroupId( artifact.getGroupId() );
        dependency.setArtifactId( artifact.getArtifactId() );
        dependency.setVersion( artifact.getBaseVersion() );
        dependency.setType( "jar" );
        return dependency;
    }

    private static List<Dependency> toDependencies( Artifact... artifacts )
    {
        List<Dependency> dependencies = new ArrayList<>();
        for ( Artifact artifact : artifacts )
        {
            dependencies.add( toDependency( artifact ) );
        }
        return dependencies;
    }
}
