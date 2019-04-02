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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.surefire.AbstractSurefireMojo.JUnitPlatformProviderInfo;
import org.apache.maven.plugin.surefire.log.PluginConsoleLogger;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.artifact.filter.resolve.TransformableFilter;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.suite.RunResult;
import org.codehaus.plexus.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.io.File.separatorChar;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersion;
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 * Test for {@link AbstractSurefireMojo}.
 */
@RunWith( PowerMockRunner.class )
@PrepareForTest( AbstractSurefireMojo.class )
@PowerMockIgnore( { "org.jacoco.agent.rt.*", "com.vladium.emma.rt.*" } )
public class AbstractSurefireMojoTest
{
    @Mock
    private ArtifactHandler handler;

    private final Mojo mojo = new Mojo();

    @Test
    public void shouldRetainInPluginArtifacts() throws Exception
    {
        Artifact provider = new DefaultArtifact( "g", "a", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        Artifact common = new DefaultArtifact( "g", "c", createFromVersionSpec( "1" ), "compile", "jar", "", null );
        Artifact api = new DefaultArtifact( "g", "a", createFromVersionSpec( "1" ), "compile", "jar", "", null );

        Set<Artifact> providerArtifacts = singleton( provider );
        Artifact[] inPluginArtifacts = { common, api };
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
        Artifact[] inPluginArtifacts = { common, api };
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

        assertThat( (Object ) inPluginClasspath )
                .isEqualTo( expectedClasspath );

    }

    @Test
    public void shouldGenerateTestClasspath() throws Exception
    {
        AbstractSurefireMojo mojo = spy( this.mojo );

        when( mojo.getClassesDirectory() ).thenReturn( new File( "target" + separatorChar + "classes" ) );
        when( mojo.getTestClassesDirectory() ).thenReturn( new File( "target" + separatorChar + "test-classes" ) );
        when( mojo.getClasspathDependencyScopeExclude() ).thenReturn( "runtime" );
        when( mojo.getClasspathDependencyExcludes() ).thenReturn( new String[]{ "g3:a3" } );
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
        verify( mojo, times( 1 ) ).getClassesDirectory();
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

        Map<String, Artifact> providerArtifactsMap = new HashMap<>();
        providerArtifactsMap.put( "org.apache.maven.surefire:maven-surefire-common", common );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-extensions-api", ext );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-api", api );
        providerArtifactsMap.put( "org.apache.maven.surefire:surefire-logger-api", loggerApi );

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

        StartupConfiguration conf = invokeMethod( mojo, "newStartupConfigWithClasspath",
                classLoaderConfiguration, providerArtifacts, "org.asf.Provider", testClasspath );

        verify( mojo, times( 1 ) ).effectiveIsEnableAssertions();
        verify( mojo, times( 1 ) ).isChildDelegation();
        verify( mojo, times( 1 ) ).getEffectiveForkCount();
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass( String.class );
        verify( logger, times( 6 ) ).debug( argument.capture() );
        assertThat( argument.getAllValues() )
                .containsExactly( "test classpath:  test-classes  classes  junit.jar  hamcrest.jar",
                "provider classpath:  surefire-provider.jar",
                "test(compact) classpath:  test-classes  classes  junit.jar  hamcrest.jar",
                "provider(compact) classpath:  surefire-provider.jar",
                "in-process classpath:  surefire-provider.jar  maven-surefire-common.jar  surefire-extensions-api.jar  surefire-api.jar  surefire-logger-api.jar",
                "in-process(compact) classpath:  surefire-provider.jar  maven-surefire-common.jar  surefire-extensions-api.jar  surefire-api.jar  surefire-logger-api.jar"
                );

        assertThat( conf.getClassLoaderConfiguration() )
                .isSameAs( classLoaderConfiguration );

        assertThat( ( Object ) conf.getClasspathConfiguration().getTestClasspath() )
                .isEqualTo( testClasspath.toClasspath() );

        Collection<String> files = new ArrayList<>();
        for ( Artifact providerArtifact : providerArtifacts )
        {
            files.add( providerArtifact.getFile().getAbsolutePath() );
        }
        assertThat( ( Object ) conf.getClasspathConfiguration().getProviderClasspath() )
                .isEqualTo( new Classpath( files ) );

        assertThat( ( Object ) conf.getClasspathConfiguration().isClassPathConfig() )
                .isEqualTo( true );

        assertThat( ( Object ) conf.getClasspathConfiguration().isModularPathConfig() )
                .isEqualTo( false );

        assertThat( ( Object ) conf.getClasspathConfiguration().isEnableAssertions() )
                .isEqualTo( true );

        assertThat( conf.getProviderClassName() )
                .isEqualTo( "org.asf.Provider" );
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

        Artifact junitPlatformArtifact = new DefaultArtifact( "org.apache.maven.surefire",
                "surefire-junit-platform", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

        Artifact testClasspathJUnit = new DefaultArtifact( "junit", "junit", createFromVersion( "4.12" ), null, "jar",
                null, mock( ArtifactHandler.class ) );

        Iterable<Artifact> testArtifacts = singleton( testClasspathJUnit );

        File classesDirectory = new File( "target/classes" );

        File testClassesDirectory = new File( "target/test-classes" );

        TestClassPath testClasspathWrapper =
                new TestClassPath( testArtifacts, classesDirectory, testClassesDirectory, null );

        Artifact forkedBooter = new DefaultArtifact( "org.apache.maven.surefire",
                "surefire-booter", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

        mojo.setPluginArtifactMap( singletonMap( "org.apache.maven.surefire:surefire-booter", forkedBooter ) );
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
        final ArtifactResolutionResult junit4ResolutionResult = mock( ArtifactResolutionResult.class );
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
                        else if ( artifact.getGroupId().equals( "junit" )
                                && artifact.getArtifactId().equals( "junit" )
                                && artifact.getVersion().equals( "4.12" ) )
                        {
                            return junit4ResolutionResult;
                        }
                        else
                        {
                            fail();
                            return null;
                        }
                    }
                } );

        DependencyResolver dependencyResolver = mock( DependencyResolver.class );
        when( dependencyResolver.resolveDependencies( any( ProjectBuildingRequest.class ),
                ArgumentMatchers.<Dependency>anyCollection(), isNull( Collection.class ),
                any( TransformableFilter.class ) ) )
                .thenAnswer( new Answer<Object>()
                {
                    @Override
                    public Object answer( InvocationOnMock invocation )
                    {
                        Collection deps = (Collection) invocation.getArguments()[1];
                        assertThat( deps ).isEmpty();
                        return emptySet();
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

        Artifact junit = new DefaultArtifact( "junit", "junit",
                createFromVersion( "4.12" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact hamcrest = new DefaultArtifact( "org.hamcrest", "hamcrest-core",
                createFromVersion( "1.3" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Set<Artifact> junitArtifacts = new HashSet<>();
        junitArtifacts.add( junit );
        junitArtifacts.add( hamcrest );
        when( junit4ResolutionResult.getArtifacts() )
                .thenReturn( junitArtifacts );

        mojo.setRepositorySystem( repositorySystem );
        mojo.setLogger( mock( Logger.class ) );
        mojo.setDependencyResolver( dependencyResolver );

        invokeMethod( mojo, "setupStuff" );

        MavenSession session = mock( MavenSession.class );
        mojo.setSession( session );
        when( session.getProjectBuildingRequest() )
                .thenReturn( mock( ProjectBuildingRequest.class ) );

        PluginDescriptor pluginDescriptor = mock( PluginDescriptor.class );
        mojo.setPluginDescriptor( pluginDescriptor );
        Plugin p = mock( Plugin.class );
        when( pluginDescriptor.getPlugin() )
                .thenReturn( p );
        when( p.getDependencies() )
                .thenReturn( Collections.<Dependency>emptyList() );

        JUnitPlatformProviderInfo prov =
                mojo.createJUnitPlatformProviderInfo( junitPlatformArtifact, testClasspathWrapper );

        Artifact expectedProvider = new DefaultArtifact( "org.apache.maven.surefire", "surefire-junit-platform",
                surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedCommonJava5 = new DefaultArtifact( "org.apache.maven.surefire", "common-java5",
                surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedLauncher = new DefaultArtifact( "org.junit.platform", "junit-platform-launcher",
                createFromVersion( "1.3.2" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedApiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
                createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedJUnit5Engine = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
                createFromVersion( "1.3.2" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedOpentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
                createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact expectedPlatformCommons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
                createFromVersion( "1.3.2" ), null, "jar", null, mock( ArtifactHandler.class ) );
        assertThat( prov.getProviderClasspath() )
                .hasSize( 7 )
                .containsOnly( expectedProvider, expectedCommonJava5, expectedLauncher, expectedApiguardian,
                        expectedJUnit5Engine, expectedOpentest4j, expectedPlatformCommons );

        assertThat( testClasspathWrapper.getTestDependencies() )
                .hasSize( 1 )
                .includes( entry( "junit:junit", testClasspathJUnit ) );
    }

    @Test
    public void shouldSmartlyResolveJUnit5ProviderWithVintage() throws Exception
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact( new DefaultArtifact( "dummy", "pom", createFromVersion( "1.0.0" ),
                null, "jar", null, mock( ArtifactHandler.class ) ) );
        mojo.setProject( mavenProject );

        final VersionRange surefireVersion = createFromVersion( "1" );

        Artifact junitPlatformArtifact = new DefaultArtifact( "org.apache.maven.surefire",
                "surefire-junit-platform", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathSomeTestArtifact = new DefaultArtifact( "third.party", "artifact",
                createFromVersion( "1.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathVintage = new DefaultArtifact( "org.junit.vintage", "junit-vintage-engine",
                createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathApiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
                createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathPlatformEng = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
                createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Artifact testClasspathJUnit4 = new DefaultArtifact( "junit", "junit",
                createFromVersion( "4.12" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Artifact testClasspathHamcrest = new DefaultArtifact( "org.hamcrest", "hamcrest-core",
                createFromVersion( "1.3" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathOpentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
                createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathCommons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
                createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Iterable<Artifact> testArtifacts = asList( testClasspathSomeTestArtifact, testClasspathVintage,
                testClasspathApiguardian, testClasspathPlatformEng, testClasspathJUnit4, testClasspathHamcrest,
                testClasspathOpentest4j, testClasspathCommons );

        File classesDirectory = new File( "target/classes" );

        File testClassesDirectory = new File( "target/test-classes" );

        TestClassPath testClasspathWrapper =
                new TestClassPath( testArtifacts, classesDirectory, testClassesDirectory, null );

        Artifact forkedBooter = new DefaultArtifact( "org.apache.maven.surefire",
                "surefire-booter", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

        mojo.setPluginArtifactMap( singletonMap( "org.apache.maven.surefire:surefire-booter", forkedBooter ) );
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
        final ArtifactResolutionResult junit4ResolutionResult = mock( ArtifactResolutionResult.class );
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
                                && "junit-platform-engine".equals( resolvable.getArtifactId() )
                                && "1.4.0".equals( resolvable.getVersion() ) )
                        {
                            return createVintageEngineResolutionResult();
                        }
                        else if ( "org.junit.platform".equals( resolvable.getGroupId() )
                                && "junit-platform-launcher".equals( resolvable.getArtifactId() )
                                && "1.4.0".equals( resolvable.getVersion() ) )
                        {
                            return createExpectedJUnitPlatformLauncherResolutionResult();
                        }
                        else
                        {
                            fail();
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

        Artifact junit = new DefaultArtifact( "junit", "junit",
                createFromVersion( "4.12" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact hamcrest = new DefaultArtifact( "org.hamcrest", "hamcrest-core",
                createFromVersion( "1.3" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Set<Artifact> junitArtifacts = new HashSet<>();
        junitArtifacts.add( junit );
        junitArtifacts.add( hamcrest );
        when( junit4ResolutionResult.getArtifacts() )
                .thenReturn( junitArtifacts );

        mojo.setRepositorySystem( repositorySystem );
        mojo.setLogger( mock( Logger.class ) );

        invokeMethod( mojo, "setupStuff" );
        JUnitPlatformProviderInfo prov =
                mojo.createJUnitPlatformProviderInfo( junitPlatformArtifact, testClasspathWrapper );

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
                .includes( entry(  "third.party:artifact", testClasspathSomeTestArtifact ),
                        entry(  "org.junit.vintage:junit-vintage-engine", testClasspathVintage ),
                        entry(  "org.apiguardian:apiguardian-api", testClasspathApiguardian ),
                        entry(  "org.junit.platform:junit-platform-engine", testClasspathPlatformEng ),
                        entry(  "junit:junit", testClasspathJUnit4 ),
                        entry(  "org.hamcrest:hamcrest-core", testClasspathHamcrest ),
                        entry(  "org.opentest4j:opentest4j", testClasspathOpentest4j ),
                        entry( "org.junit.platform:junit-platform-commons", testClasspathCommons ) );
    }

    @Test
    public void shouldSmartlyResolveJUnit5ProviderWithJUnit5Commons() throws Exception
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact( new DefaultArtifact( "dummy", "pom", createFromVersion( "1.0.0" ),
                null, "jar", null, mock( ArtifactHandler.class ) ) );
        mojo.setProject( mavenProject );

        final VersionRange surefireVersion = createFromVersion( "1" );

        Artifact junitPlatformArtifact = new DefaultArtifact( "org.apache.maven.surefire",
                "surefire-junit-platform", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathSomeTestArtifact = new DefaultArtifact( "third.party", "artifact",
                createFromVersion( "1.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathCommons = new DefaultArtifact( "org.junit.platform", "junit-platform-commons",
                createFromVersion( "1.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        final Artifact testClasspathApiguardian = new DefaultArtifact( "org.apiguardian", "apiguardian-api",
                createFromVersion( "1.0.0" ), null, "jar", null, mock( ArtifactHandler.class ) );

        Iterable<Artifact> testArtifacts =
                asList( testClasspathSomeTestArtifact, testClasspathApiguardian, testClasspathCommons );

        File classesDirectory = new File( "target/classes" );

        File testClassesDirectory = new File( "target/test-classes" );

        TestClassPath testClasspathWrapper =
                new TestClassPath( testArtifacts, classesDirectory, testClassesDirectory, null );

        Artifact forkedBooter = new DefaultArtifact( "org.apache.maven.surefire",
                "surefire-booter", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

        mojo.setPluginArtifactMap( singletonMap( "org.apache.maven.surefire:surefire-booter", forkedBooter ) );
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
                        else
                        {
                            fail();
                            return null;
                        }
                    }
                } );

        DependencyResolver dependencyResolver = mock( DependencyResolver.class );
        when( dependencyResolver.resolveDependencies( any( ProjectBuildingRequest.class ),
                ArgumentMatchers.<Dependency>anyCollection(), isNull( Collection.class ),
                any( TransformableFilter.class ) ) )
                .thenAnswer( new Answer<Object>()
                {
                    @Override
                    public Object answer( InvocationOnMock invocation )
                    {
                        Collection deps = (Collection) invocation.getArguments()[1];
                        assertThat( deps ).isEmpty();
                        return emptySet();
                    }
                } );

        mojo.setRepositorySystem( repositorySystem );
        mojo.setLogger( mock( Logger.class ) );
        mojo.setDependencyResolver( dependencyResolver );

        invokeMethod( mojo, "setupStuff" );

        MavenSession session = mock( MavenSession.class );
        mojo.setSession( session );
        when( session.getProjectBuildingRequest() )
                .thenReturn( mock( ProjectBuildingRequest.class ) );

        PluginDescriptor pluginDescriptor = mock( PluginDescriptor.class );
        mojo.setPluginDescriptor( pluginDescriptor );
        Plugin p = mock( Plugin.class );
        when( pluginDescriptor.getPlugin() )
                .thenReturn( p );
        when( p.getDependencies() )
                .thenReturn( Collections.<Dependency>emptyList() );

        JUnitPlatformProviderInfo prov =
                mojo.createJUnitPlatformProviderInfo( junitPlatformArtifact, testClasspathWrapper );
        Set<Artifact> resolvedProviderArtifacts = prov.getProviderClasspath();

        Artifact provider = new DefaultArtifact( "org.apache.maven.surefire", "surefire-junit-platform",
                surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact java5 = new DefaultArtifact( "org.apache.maven.surefire", "common-java5",
                surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact launcher = new DefaultArtifact( "org.junit.platform", "junit-platform-launcher",
                createFromVersion( "1.3.2" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact engine = new DefaultArtifact( "org.junit.platform", "junit-platform-engine",
                createFromVersion( "1.3.2" ), null, "jar", null, mock( ArtifactHandler.class ) );
        Artifact opentest4j = new DefaultArtifact( "org.opentest4j", "opentest4j",
                createFromVersion( "1.1.1" ), null, "jar", null, mock( ArtifactHandler.class ) );
        assertThat( resolvedProviderArtifacts )
                .hasSize( 5 )
                .containsOnly( provider, java5, launcher, engine, opentest4j );

        assertThat( testClasspathWrapper.getTestDependencies() )
                .hasSize( 3 )
                .includes( entry( "third.party:artifact", testClasspathSomeTestArtifact ),
                        entry( "org.junit.platform:junit-platform-commons", testClasspathCommons ),
                        entry( "org.apiguardian:apiguardian-api", testClasspathApiguardian ) );
    }

    @Test
    public void shouldSmartlyResolveJUnit5ProviderWithJUnit5Engine() throws Exception
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact( new DefaultArtifact( "dummy", "pom", createFromVersion( "1.0.0" ),
                null, "jar", null, mock( ArtifactHandler.class ) ) );
        mojo.setProject( mavenProject );

        final VersionRange surefireVersion = createFromVersion( "1" );

        Artifact junitPlatformArtifact = new DefaultArtifact( "org.apache.maven.surefire",
                "surefire-junit-platform", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

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

        Iterable<Artifact> testArtifacts = asList( testClasspathSomeTestArtifact, testClasspathJUnit5,
                testClasspathApiguardian, testClasspathCommons, testClasspathOpentest4j );

        File classesDirectory = new File( "target/classes" );

        File testClassesDirectory = new File( "target/test-classes" );

        TestClassPath testClasspathWrapper =
                new TestClassPath( testArtifacts, classesDirectory, testClassesDirectory, null );

        Artifact forkedBooter = new DefaultArtifact( "org.apache.maven.surefire",
                "surefire-booter", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

        mojo.setPluginArtifactMap( singletonMap( "org.apache.maven.surefire:surefire-booter", forkedBooter ) );
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
                        else if ( resolvable.equals( testClasspathJUnit5 )  )
                        {
                            return createResolutionResult( testClasspathJUnit5, testClasspathApiguardian,
                                    testClasspathCommons, testClasspathOpentest4j );
                        }
                        else if ( "org.junit.platform".equals( resolvable.getGroupId() )
                                && "junit-platform-launcher".equals( resolvable.getArtifactId() )
                                && "1.4.0".equals( resolvable.getVersion() ) )
                        {
                            return createExpectedJUnitPlatformLauncherResolutionResult();
                        }
                        else
                        {
                            fail();
                            return null;
                        }
                    }
                } );

        mojo.setRepositorySystem( repositorySystem );
        mojo.setLogger( mock( Logger.class ) );

        invokeMethod( mojo, "setupStuff" );
        JUnitPlatformProviderInfo prov =
                mojo.createJUnitPlatformProviderInfo( junitPlatformArtifact, testClasspathWrapper );
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
                .includes( entry( "third.party:artifact", testClasspathSomeTestArtifact ),
                        entry( "org.junit.platform:junit-platform-engine", testClasspathJUnit5 ),
                        entry( "org.apiguardian:apiguardian-api", testClasspathApiguardian ),
                        entry( "org.junit.platform:junit-platform-commons", testClasspathCommons ),
                        entry( "org.opentest4j:opentest4j", testClasspathOpentest4j ) );
    }

    @Test
    public void shouldSmartlyResolveJUnit5ProviderWithJupiterApi() throws Exception
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact( new DefaultArtifact( "dummy", "pom", createFromVersion( "1.0.0" ),
                null, "jar", null, mock( ArtifactHandler.class ) ) );
        mojo.setProject( mavenProject );

        final VersionRange surefireVersion = createFromVersion( "1" );

        Artifact junitPlatformArtifact = new DefaultArtifact( "org.apache.maven.surefire",
                "surefire-junit-platform", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

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

        Iterable<Artifact> testArtifacts = asList( testClasspathSomeTestArtifact, testClasspathJupiterApi,
                testClasspathApiguardian, testClasspathCommons, testClasspathOpentest4j );

        File classesDirectory = new File( "target/classes" );

        File testClassesDirectory = new File( "target/test-classes" );

        TestClassPath testClasspathWrapper =
                new TestClassPath( testArtifacts, classesDirectory, testClassesDirectory, null );

        Artifact forkedBooter = new DefaultArtifact( "org.apache.maven.surefire",
                "surefire-booter", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

        mojo.setPluginArtifactMap( singletonMap( "org.apache.maven.surefire:surefire-booter", forkedBooter ) );
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
                        else if ( resolvable.equals( testClasspathJupiterApi )  )
                        {
                            return createResolutionResult( testClasspathJupiterApi, testClasspathApiguardian,
                                    testClasspathCommons, testClasspathOpentest4j );
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
                            fail();
                            return null;
                        }
                    }
                } );

        DependencyResolver dependencyResolver = mock( DependencyResolver.class );
        when( dependencyResolver.resolveDependencies( any( ProjectBuildingRequest.class ),
                ArgumentMatchers.<Dependency>anyCollection(), isNull( Collection.class ),
                any( TransformableFilter.class ) ) )
                .thenAnswer( new Answer<Object>()
                {
                    @Override
                    public Object answer( InvocationOnMock invocation )
                    {
                        Collection deps = (Collection) invocation.getArguments()[1];
                        assertThat( deps ).isEmpty();
                        return emptySet();
                    }
                } );

        mojo.setRepositorySystem( repositorySystem );
        mojo.setLogger( mock( Logger.class ) );
        mojo.setDependencyResolver( dependencyResolver );

        invokeMethod( mojo, "setupStuff" );

        MavenSession session = mock( MavenSession.class );
        mojo.setSession( session );
        when( session.getProjectBuildingRequest() )
                .thenReturn( mock( ProjectBuildingRequest.class ) );

        PluginDescriptor pluginDescriptor = mock( PluginDescriptor.class );
        mojo.setPluginDescriptor( pluginDescriptor );
        Plugin p = mock( Plugin.class );
        when( pluginDescriptor.getPlugin() )
                .thenReturn( p );
        when( p.getDependencies() )
                .thenReturn( Collections.<Dependency>emptyList() );

        JUnitPlatformProviderInfo prov =
                mojo.createJUnitPlatformProviderInfo( junitPlatformArtifact, testClasspathWrapper );
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
                .includes( entry( "third.party:artifact", testClasspathSomeTestArtifact ),
                        entry( "org.junit.jupiter:junit-jupiter-api", testClasspathJupiterApi ),
                        entry( "org.apiguardian:apiguardian-api", testClasspathApiguardian ),
                        entry( "org.junit.platform:junit-platform-commons", testClasspathCommons ),
                        entry( "org.opentest4j:opentest4j", testClasspathOpentest4j ) );
    }

    @Test
    public void shouldSmartlyResolveJUnit5ProviderWithJupiterEngine() throws Exception
    {
        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact( new DefaultArtifact( "dummy", "pom", createFromVersion( "1.0.0" ),
                null, "jar", null, mock( ArtifactHandler.class ) ) );
        mojo.setProject( mavenProject );

        final VersionRange surefireVersion = createFromVersion( "1" );

        Artifact junitPlatformArtifact = new DefaultArtifact( "org.apache.maven.surefire",
                "surefire-junit-platform", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

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

        Iterable<Artifact> testArtifacts = asList( testClasspathSomeTestArtifact, testClasspathJupiterEngine,
                testClasspathPlatformEngine, testClasspathJupiterApi, testClasspathApiguardian, testClasspathCommons,
                testClasspathOpentest4j );

        File classesDirectory = new File( "target/classes" );

        File testClassesDirectory = new File( "target/test-classes" );

        TestClassPath testClasspathWrapper =
                new TestClassPath( testArtifacts, classesDirectory, testClassesDirectory, null );

        Artifact forkedBooter = new DefaultArtifact( "org.apache.maven.surefire",
                "surefire-booter", surefireVersion, null, "jar", null, mock( ArtifactHandler.class ) );

        mojo.setPluginArtifactMap( singletonMap( "org.apache.maven.surefire:surefire-booter", forkedBooter ) );
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
                        else if ( resolvable.equals( testClasspathJupiterApi )  )
                        {
                            return createResolutionResult( testClasspathJupiterApi, testClasspathApiguardian,
                                    testClasspathCommons, testClasspathOpentest4j );
                        }
                        else if ( "org.junit.platform".equals( resolvable.getGroupId() )
                                && "junit-platform-launcher".equals( resolvable.getArtifactId() )
                                && "1.4.0".equals( resolvable.getVersion() ) )
                        {
                            return createExpectedJUnitPlatformLauncherResolutionResult();
                        }
                        else if ( "org.junit.platform".equals( resolvable.getGroupId() )
                                && "junit-platform-engine".equals( resolvable.getArtifactId() )
                                && "1.4.0".equals( resolvable.getVersion() ) )
                        {
                            return createVintageEngineResolutionResult();
                        }
                        else
                        {
                            fail();
                            return null;
                        }
                    }
                } );

        mojo.setRepositorySystem( repositorySystem );
        mojo.setLogger( mock( Logger.class ) );

        invokeMethod( mojo, "setupStuff" );
        JUnitPlatformProviderInfo prov =
                mojo.createJUnitPlatformProviderInfo( junitPlatformArtifact, testClasspathWrapper );
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
                .includes( entry( "third.party:artifact", testClasspathSomeTestArtifact ),
                        entry( "org.junit.jupiter:junit-jupiter-engine", testClasspathJupiterEngine ),
                        entry( "org.junit.platform:junit-platform-engine", testClasspathPlatformEngine ),
                        entry( "org.junit.jupiter:junit-jupiter-api", testClasspathJupiterApi ),
                        entry( "org.apiguardian:apiguardian-api", testClasspathApiguardian ),
                        entry( "org.junit.platform:junit-platform-commons", testClasspathCommons ),
                        entry( "org.opentest4j:opentest4j", testClasspathOpentest4j ) );
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

        Map<String, Artifact> pluginDependencies = new HashMap<>();
        pluginDependencies.put( "org.apache.maven.surefire:maven-surefire-plugin", plugin );
        pluginDependencies.put( "org.apache.maven.surefire:surefire-booter", forkedBooter );
        pluginDependencies.put( "org.junit.jupiter:junit-jupiter-engine", pluginDepJupiterEngine );
        pluginDependencies.put( "org.junit.platform:junit-platform-engine", pluginDepPlatformEngine );
        pluginDependencies.put( "org.junit.jupiter:junit-jupiter-api", pluginDepJupiterApi );
        pluginDependencies.put( "org.apiguardian:apiguardian-api", pluginDepApiguardian );
        pluginDependencies.put( "org.junit.platform:junit-platform-commons", pluginDepCommons );
        pluginDependencies.put( "org.opentest4j:opentest4j", pluginDepOpentest4j );
        mojo.setPluginArtifactMap( pluginDependencies );

        MavenProject mavenProject = new MavenProject();
        mavenProject.setArtifact( new DefaultArtifact( "dummy", "pom", createFromVersion( "1.0.0" ),
                null, "jar", null, mock( ArtifactHandler.class ) ) );
        mojo.setProject( mavenProject );

        Artifact junitPlatformArtifact = new DefaultArtifact( "g", "a",
                createFromVersion( "0" ), null, "jar", null, mock( ArtifactHandler.class ) );

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

        Iterable<Artifact> testArtifacts = asList( testClasspathSomeTestArtifact, testClasspathJupiterApi,
                testClasspathApiguardian, testClasspathCommons, testClasspathOpentest4j );

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
                        else if ( "org.apache.maven.surefire".equals( resolvable.getGroupId() )
                                && "maven-surefire-plugin".equals( resolvable.getArtifactId() )
                                && "1".equals( resolvable.getVersion() ) )
                        {
                            Set<Artifact> pluginItself = new HashSet<>();
                            pluginItself.add( plugin );
                            pluginItself.add( forkedBooter );
                            ArtifactResolutionResult launcherResolutionResult = mock( ArtifactResolutionResult.class );
                            when( launcherResolutionResult.getArtifacts() )
                                    .thenReturn( pluginItself );
                            return launcherResolutionResult;
                        }
                        else
                        {
                            fail();
                            return null;
                        }
                    }
                } );

        DependencyResolver dependencyResolver = mock( DependencyResolver.class );
        when( dependencyResolver.resolveDependencies( any( ProjectBuildingRequest.class ),
                ArgumentMatchers.<Dependency>anyCollection(), isNull( Collection.class ),
                any( TransformableFilter.class ) ) )
                .thenAnswer( new Answer<Object>()
                {
                    @Override
                    public Object answer( InvocationOnMock invocation )
                    {
                        Collection deps = (Collection) invocation.getArguments()[1];
                        assertThat( deps ).hasSize( 1 );
                        Dependency resolvable = (Dependency) deps.iterator().next();
                        if ( "org.junit.jupiter".equals( resolvable.getGroupId() )
                                && "junit-jupiter-engine".equals( resolvable.getArtifactId() )
                                && "5.4.0".equals( resolvable.getVersion() ) )
                        {
                            Set<ArtifactResult> resolvedPluginDeps = new HashSet<>();
                            resolvedPluginDeps.add( toArtifactResult( pluginDepJupiterEngine ) );
                            resolvedPluginDeps.add( toArtifactResult( pluginDepPlatformEngine ) );
                            resolvedPluginDeps.add( toArtifactResult( pluginDepJupiterApi ) );
                            resolvedPluginDeps.add( toArtifactResult( pluginDepApiguardian ) );
                            resolvedPluginDeps.add( toArtifactResult( pluginDepCommons ) );
                            resolvedPluginDeps.add( toArtifactResult( pluginDepOpentest4j ) );
                            return resolvedPluginDeps;
                        }
                        else
                        {
                            fail();
                            return null;
                        }
                    }
                } );

        mojo.setRepositorySystem( repositorySystem );
        mojo.setLogger( mock( Logger.class ) );
        mojo.setDependencyResolver( dependencyResolver );

        invokeMethod( mojo, "setupStuff" );

        JUnitPlatformProviderInfo prov =
                mojo.createJUnitPlatformProviderInfo( junitPlatformArtifact, testClasspathWrapper );

        MavenSession session = mock( MavenSession.class );
        mojo.setSession( session );
        when( session.getProjectBuildingRequest() )
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
                .includes( entry( "third.party:artifact", testClasspathSomeTestArtifact ),
                        entry( "org.junit.jupiter:junit-jupiter-api", testClasspathJupiterApi ),
                        entry( "org.apiguardian:apiguardian-api", testClasspathApiguardian ),
                        entry( "org.junit.platform:junit-platform-commons", testClasspathCommons ),
                        entry( "org.opentest4j:opentest4j", testClasspathOpentest4j ) );
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

    private static ArtifactResolutionResult createVintageEngineResolutionResult()
    {
        ArtifactResolutionResult launcherResolutionResult = mock( ArtifactResolutionResult.class );
        Set<Artifact> resolvedLauncherArtifacts = new HashSet<>();
        resolvedLauncherArtifacts.add( new DefaultArtifact( "org.junit.vintage", "junit-vintage-engine",
                createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) ) );
        resolvedLauncherArtifacts.add( new DefaultArtifact( "org.junit.jupiter", "junit-jupiter-api",
                createFromVersion( "5.4.0" ), null, "jar", null, mock( ArtifactHandler.class ) ) );
        resolvedLauncherArtifacts.add( new DefaultArtifact( "junit", "junit",
                createFromVersion( "4.12" ), null, "jar", null, mock( ArtifactHandler.class ) ) );
        resolvedLauncherArtifacts.add( new DefaultArtifact( "org.hamcrest", "hamcrest-core",
                createFromVersion( "1.3" ), null, "jar", null, mock( ArtifactHandler.class ) ) );
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

    private static ArtifactResolutionResult createResolutionResult( Artifact... artifacts )
    {
        ArtifactResolutionResult testClasspathCommonsResolutionResult = mock( ArtifactResolutionResult.class );
        Set<Artifact> resolvedCommonsArtifacts = new HashSet<>();
        Collections.addAll( resolvedCommonsArtifacts, artifacts );
        when( testClasspathCommonsResolutionResult.getArtifacts() )
                .thenReturn( resolvedCommonsArtifacts );
        return testClasspathCommonsResolutionResult;
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

    public static class Mojo
            extends AbstractSurefireMojo implements SurefireReportParameters
    {
        private JUnitPlatformProviderInfo createJUnitPlatformProviderInfo( Artifact providerArtifact,
                                                                           TestClassPath testClasspathWrapper )
        {
            return new JUnitPlatformProviderInfo( providerArtifact, testClasspathWrapper );
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
            return false;
        }

        @Override
        public void setTestFailureIgnore( boolean testFailureIgnore )
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
        public File getClassesDirectory()
        {
            return null;
        }

        @Override
        public void setClassesDirectory( File classesDirectory )
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
        public Boolean getFailIfNoSpecifiedTests()
        {
            return null;
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
        protected Artifact getMojoArtifact()
        {
            return new DefaultArtifact( "org.apache.maven.surefire", "maven-surefire-plugin", createFromVersion( "1" ),
                    null, "jar", null, mock( ArtifactHandler.class ) );
        }
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
            dependencies.add( toDependency( artifact) );
        }
        return dependencies;
    }

    private static ArtifactResult toArtifactResult( final Artifact artifact )
    {
        class AR implements ArtifactResult
        {

            @Override
            public Artifact getArtifact()
            {
                return artifact;
            }
        }
        return new AR();
    }
}
