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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.DefaultResolutionErrorHandler;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.surefire.SurefireDependencyResolver.RuntimeArtifactFilter;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.repository.RepositorySystem;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec;
import static org.apache.maven.plugin.surefire.SurefireDependencyResolver.PROVIDER_GROUP_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 *
 */
public class SurefireDependencyResolverTest
{
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldNotBeWithinRangeNullArtifact()
    {
        boolean result = SurefireDependencyResolver.isWithinVersionSpec( null, "[4.7,)" );
        assertThat( result )
            .isFalse();
    }

    @Test
    public void shouldNotBeWithinRange() throws InvalidVersionSpecificationException
    {
        Artifact api = createArtifact( "junit", "junit", "4.6" );
        boolean result = SurefireDependencyResolver.isWithinVersionSpec( api, "[4.7,)" );
        assertThat( result )
            .isFalse();
    }

    @Test
    public void shouldBeWithinRange() throws InvalidVersionSpecificationException
    {
        Artifact api = createArtifact( "junit", "junit", "4.7" );
        boolean result = SurefireDependencyResolver.isWithinVersionSpec( api, "[4.7,)" );
        assertThat( result )
            .isTrue();
    }

    @Test
    public void shouldBeFarWithinRange() throws InvalidVersionSpecificationException
    {
        Artifact api = createArtifact( "junit", "junit", "4.13" );
        boolean result = SurefireDependencyResolver.isWithinVersionSpec( api, "[4.7,)" );
        assertThat( result )
            .isTrue();
    }

    @Test
    public void shouldBeFailWithinRange() throws InvalidVersionSpecificationException
    {
        Artifact api = createArtifact( "junit", "junit", "" );
        expectedException.expect( RuntimeException.class );
        expectedException.expectMessage( "Bug in plugin. Please report with stacktrace" );
        SurefireDependencyResolver.isWithinVersionSpec( api, "[4.7,)" );
    }

    @Test
    public void testResolveArtifact() throws InvalidVersionSpecificationException, MojoExecutionException
    {
        final Artifact provider = createArtifact( "surefire-junit-platform" );
        RepositorySystem repositorySystem = mock( RepositorySystem.class );
        final ArtifactResolutionResult expectedResult = mock( ArtifactResolutionResult.class );
        when( repositorySystem.resolve( any( ArtifactResolutionRequest.class ) ) )
            .then( new Answer<ArtifactResolutionResult>()
            {
                @Override
                public ArtifactResolutionResult answer( InvocationOnMock invocation )
                {
                    Object[] args = invocation.getArguments();
                    assertThat( args )
                        .hasSize( 1 );
                    ArtifactResolutionRequest request = (ArtifactResolutionRequest) args[0];
                    assertThat( request.getArtifact() )
                        .isSameAs( provider );
                    assertThat( request.isResolveTransitively() )
                        .isTrue();
                    assertThat( request.getArtifactDependencies() )
                        .isNull();
                    assertThat( request.getRemoteRepositories() )
                        .isNull();
                    assertThat( request.getLocalRepository() )
                        .isNull();
                    assertThat( request.getCollectionFilter() )
                        .isNotNull();
                    assertThat( request.getCollectionFilter() )
                        .isInstanceOf( RuntimeArtifactFilter.class );
                    assertThat( request.getManagedVersionMap() )
                        .isNull();
                    assertThat( request.getMirrors() )
                        .isEmpty();
                    assertThat( request.getProxies() )
                        .isEmpty();
                    assertThat( request.getResolutionFilter() )
                        .isNull();
                    assertThat( request.getServers() )
                        .isEmpty();
                    return expectedResult;
                }
            } );

        SurefireDependencyResolver surefireDependencyResolver =
            new SurefireDependencyResolver( repositorySystem, null, null, null, null,
                new DefaultResolutionErrorHandler(), null, false );

        ArtifactResolutionResult actualResult = surefireDependencyResolver.resolvePluginArtifact( provider );

        assertThat( actualResult )
            .isSameAs( expectedResult );
    }

    @Test
    public void testGetProviderClasspath() throws Exception
    {

        Artifact commonJunit4 = createArtifact( "common-junit4" );
        Artifact api = createArtifact( "surefire-api" );
        final Artifact provider = createArtifact( "surefire-junit-platform" );
        Artifact ext = createArtifact( "org.apiguardian", "apiguardian-api" );
        Artifact logger = createArtifact( "surefire-logger-api" );

        Set<Artifact> providerArtifacts = new LinkedHashSet<>();
        providerArtifacts.add( commonJunit4 );
        providerArtifacts.add( api );
        providerArtifacts.add( provider );
        providerArtifacts.add( ext );
        providerArtifacts.add( logger );

        final ArtifactResolutionResult result = mock( ArtifactResolutionResult.class );
        when( result.getArtifacts() )
            .thenReturn( providerArtifacts );

        RepositorySystem repositorySystem = mock( RepositorySystem.class );
        when( repositorySystem.resolve( any( ArtifactResolutionRequest.class ) ) )
            .then( new Answer<ArtifactResolutionResult>()
            {
                @Override
                public ArtifactResolutionResult answer( InvocationOnMock invocation )
                {
                    Object[] args = invocation.getArguments();
                    assertThat( args )
                        .hasSize( 1 );
                    ArtifactResolutionRequest request = (ArtifactResolutionRequest) args[0];
                    assertThat( request.getArtifact() )
                        .isSameAs( provider );
                    assertThat( request.isResolveTransitively() )
                        .isTrue();
                    assertThat( request.getArtifactDependencies() )
                        .isNull();
                    assertThat( request.getRemoteRepositories() )
                        .isNull();
                    assertThat( request.getLocalRepository() )
                        .isNull();
                    assertThat( request.getCollectionFilter() )
                        .isNotNull();
                    assertThat( request.getCollectionFilter() )
                        .isInstanceOf( RuntimeArtifactFilter.class );
                    assertThat( request.getManagedVersionMap() )
                        .isNull();
                    assertThat( request.getMirrors() )
                        .isEmpty();
                    assertThat( request.getProxies() )
                        .isEmpty();
                    assertThat( request.getResolutionFilter() )
                        .isNull();
                    assertThat( request.getServers() )
                        .isEmpty();
                    return result;
                }
            } );
        when( repositorySystem.createDependencyArtifact( any( Dependency.class ) ) )
            .then( new Answer<Artifact>()
            {
                @Override
                public Artifact answer( InvocationOnMock invocation )
                {
                    Object[] args = invocation.getArguments();
                    assertThat( args )
                        .hasSize( 1 );
                    Dependency request = (Dependency) args[0];
                    assertThat( request.getGroupId() )
                        .isEqualTo( provider.getGroupId() );
                    assertThat( request.getArtifactId() )
                        .isEqualTo( provider.getArtifactId() );
                    assertThat( request.getVersion() )
                        .isEqualTo( provider.getVersion() );
                    assertThat( request.getType() )
                        .isEqualTo( provider.getType() );
                    assertThat( request.getScope() )
                        .isNull();
                    return provider;
                }
            } );

        ConsoleLogger log = mock( ConsoleLogger.class );

        SurefireDependencyResolver surefireDependencyResolver =
            new SurefireDependencyResolver( repositorySystem, log, null, null, null,
                new DefaultResolutionErrorHandler(), null, false );

        when( log.isDebugEnabled() )
            .thenReturn( true );

        Set<Artifact> classpath = surefireDependencyResolver.getProviderClasspath( "surefire-junit-platform", "1" );

        assertThat( classpath )
            .hasSize( 5 );

        Iterator<Artifact> it = classpath.iterator();

        // result should be ordered
        assertThat( it.next() )
            .isSameAs( provider );

        assertThat( it.next() )
            .isSameAs( api );

        assertThat( it.next() )
            .isSameAs( logger );

        assertThat( it.next() )
            .isSameAs( commonJunit4 );

        assertThat( it.next() )
            .isSameAs( ext );
    }

    @Test
    public void testGetProviderClasspathShouldPropagateTheResolutionException() throws Exception
    {
        Artifact provider = createArtifact( "surefire-junit-platform" );
        provider.setFile( null );

        Set<Artifact> providerArtifacts = new LinkedHashSet<>();
        providerArtifacts.add( provider );

        ArtifactResolutionResult result = mock( ArtifactResolutionResult.class );
        when( result.getArtifacts() ).thenReturn( providerArtifacts );
        when( result.hasMetadataResolutionExceptions() ).thenReturn( true );
        ArtifactResolutionException resolutionException =
            new ArtifactResolutionException( "failed to resolve", provider );
        when( result.getMetadataResolutionException( 0 ) ).thenReturn( resolutionException );

        RepositorySystem repositorySystem = mock( RepositorySystem.class );
        when( repositorySystem.resolve( any( ArtifactResolutionRequest.class ) ) ).thenReturn( result );
        when( repositorySystem.createDependencyArtifact( any( Dependency.class ) ) ).thenReturn( provider );

        SurefireDependencyResolver surefireDependencyResolver =
            new SurefireDependencyResolver( repositorySystem, mock( ConsoleLogger.class ), null, null, null,
                new DefaultResolutionErrorHandler(), null, false );

        assertThatThrownBy( () -> surefireDependencyResolver.getProviderClasspath( "surefire-junit-platform", "1" ) )
            .isInstanceOf( MojoExecutionException.class )
            .hasCause( resolutionException );
    }

    @Test
    public void testResolvePluginDependencies() throws Exception
    {
        Dependency providerAsDependency = new Dependency();
        providerAsDependency.setGroupId( PROVIDER_GROUP_ID );
        providerAsDependency.setArtifactId( "surefire-shadefire" );
        providerAsDependency.setVersion( "1" );

        Artifact providerAsArtifact = createArtifact( "surefire-shadefire" );

        Plugin plugin = mock( Plugin.class );
        when( plugin.getDependencies() )
            .thenReturn( singletonList( providerAsDependency ) );

        RepositorySystem repositorySystem = mock( RepositorySystem.class );
        when( repositorySystem.createDependencyArtifact( providerAsDependency ) )
            .thenReturn( providerAsArtifact );

        ArtifactResolutionResult resolutionResult = mock( ArtifactResolutionResult.class );
        when( resolutionResult.getArtifacts() )
            .thenReturn( singleton( providerAsArtifact ) );

        ArgumentCaptor<ArtifactResolutionRequest> resolutionRequestCaptor =
            ArgumentCaptor.forClass( ArtifactResolutionRequest.class );
        when( repositorySystem.resolve( resolutionRequestCaptor.capture() ) )
            .thenReturn( resolutionResult );

        Map<String, Artifact> pluginResolvedDependencies =
            singletonMap( PROVIDER_GROUP_ID + ":surefire-shadefire", providerAsArtifact );

        SurefireDependencyResolver surefireDependencyResolver =
            new SurefireDependencyResolver( repositorySystem, null, null, null, null,
                new DefaultResolutionErrorHandler(), null, false );

        Map<String, Artifact> providers =
            surefireDependencyResolver.resolvePluginDependencies( plugin, pluginResolvedDependencies );

        assertThat( providers.values() )
            .hasSize( 1 )
            .containsOnly( providerAsArtifact );

        assertThat( resolutionRequestCaptor.getAllValues() )
            .hasSize( 1 );

        ArtifactResolutionRequest resolutionRequest = resolutionRequestCaptor.getValue();

        assertThat( resolutionRequest.getArtifact() )
            .isSameAs( providerAsArtifact );

        verify( repositorySystem ).createDependencyArtifact( any( Dependency.class ) );
        verify( repositorySystem ).resolve( any( ArtifactResolutionRequest.class ) );
        verifyNoMoreInteractions( repositorySystem );
    }

    private static Artifact createArtifact( String artifactId ) throws InvalidVersionSpecificationException
    {
        return createArtifact( PROVIDER_GROUP_ID, artifactId );
    }

    private static Artifact createArtifact( String groupId, String artifactId )
        throws InvalidVersionSpecificationException
    {
        return createArtifact( groupId, artifactId, "1" );
    }

    private static Artifact createArtifact( String groupId, String artifactId, String version )
        throws InvalidVersionSpecificationException
    {
        VersionRange versionSpec = createFromVersionSpec( version );
        DefaultArtifact defaultArtifact =
            new DefaultArtifact( groupId, artifactId, versionSpec, "compile", "jar", "", null );
        defaultArtifact.setFile( new File( "" ) );
        return defaultArtifact;
    }
}
