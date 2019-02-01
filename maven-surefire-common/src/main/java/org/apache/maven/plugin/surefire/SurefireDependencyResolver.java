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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.artifact.filter.resolve.ScopeFilter;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolverException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.util.Arrays.asList;
import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE;
import static org.apache.maven.artifact.Artifact.SCOPE_COMPILE_PLUS_RUNTIME;
import static org.apache.maven.artifact.Artifact.SCOPE_RUNTIME;
import static org.apache.maven.artifact.ArtifactUtils.artifactMapByVersionlessId;
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec;

/**
 * Does dependency resolution and artifact handling for the surefire plugin.
 *
 * @author Stephen Connolly
 * @author Kristian Rosenvold
 */
final class SurefireDependencyResolver
{
    static final String PROVIDER_GROUP_ID = "org.apache.maven.surefire";

    private static final String[] PROVIDER_CLASSPATH_ORDER = {
            "surefire-junit3",
            "surefire-junit4",
            "surefire-junit47",
            "surefire-testng",
            "surefire-junit-platform",
            "surefire-api",
            "surefire-logger-api",
            "common-java5",
            "common-junit3",
            "common-junit4",
            "common-junit48",
            "common-testng-utils"
    };

    private final RepositorySystem repositorySystem;

    private final ConsoleLogger log;

    private final ArtifactRepository localRepository;

    private final List<ArtifactRepository> pluginRemoteRepositories;

    private final List<ArtifactRepository> projectRemoteRepositories;

    private final String pluginName;

    private final DependencyResolver depencencyResolver;

    SurefireDependencyResolver( RepositorySystem repositorySystem, ConsoleLogger log,
                                ArtifactRepository localRepository,
                                List<ArtifactRepository> pluginRemoteRepositories,
                                List<ArtifactRepository> projectRemoteRepositories, String pluginName,
                                DependencyResolver depencencyResolver )
    {
        this.repositorySystem = repositorySystem;
        this.log = log;
        this.localRepository = localRepository;
        this.pluginRemoteRepositories = pluginRemoteRepositories;
        this.projectRemoteRepositories = projectRemoteRepositories;
        this.pluginName = pluginName;
        this.depencencyResolver = depencencyResolver;
    }

    static boolean isWithinVersionSpec( @Nullable Artifact artifact, @Nonnull String versionSpec )
    {
        if ( artifact == null )
        {
            return false;
        }
        try
        {
            VersionRange range = createFromVersionSpec( versionSpec );
            try
            {
                return range.containsVersion( artifact.getSelectedVersion() );
            }
            catch ( NullPointerException e )
            {
                return range.containsVersion( new DefaultArtifactVersion( artifact.getBaseVersion() ) );
            }
        }
        catch ( InvalidVersionSpecificationException | OverConstrainedVersionException e )
        {
            throw new RuntimeException( "Bug in plugin. Please report with stacktrace" );
        }
    }

    Set<Artifact> resolvePluginDependencies( ProjectBuildingRequest request, Collection<Dependency> pluginDependencies )
            throws MojoExecutionException
    {
        try
        {
            Iterable<ArtifactResult> resolvedPluginDependencies = depencencyResolver.resolveDependencies( request,
                    pluginDependencies, null, ScopeFilter.including( SCOPE_COMPILE, SCOPE_RUNTIME ) );

            Set<Artifact> resolved = new LinkedHashSet<>();
            for ( ArtifactResult resolvedPluginDependency : resolvedPluginDependencies )
            {
                resolved.add( resolvedPluginDependency.getArtifact() );
            }
            return resolved;
        }
        catch ( DependencyResolverException e )
        {
            throw new MojoExecutionException( e.getLocalizedMessage(), e );
        }
    }

    ArtifactResolutionResult resolvePluginArtifact( Artifact artifact )
    {
        return resolveArtifact( artifact, pluginRemoteRepositories );
    }

    ArtifactResolutionResult resolveProjectArtifact( Artifact artifact )
    {
        return resolveArtifact( artifact, projectRemoteRepositories );
    }

    private ArtifactResolutionResult resolveArtifact( Artifact artifact, List<ArtifactRepository> repositories )
    {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                .setArtifact( artifact )
                .setLocalRepository( localRepository )
                .setResolveTransitively( true )
                .setCollectionFilter( new RuntimeArtifactFilter() )
                .setRemoteRepositories( repositories );

        return repositorySystem.resolve( request );
    }

    @Nonnull
    Set<Artifact> getProviderClasspath( String providerArtifactId, String providerVersion )
    {
        Dependency provider = toProviderDependency( providerArtifactId, providerVersion );

        Artifact providerArtifact = repositorySystem.createDependencyArtifact( provider );

        ArtifactResolutionResult result = resolvePluginArtifact( providerArtifact );

        if ( log.isDebugEnabled() )
        {
            for ( Artifact artifact : result.getArtifacts() )
            {
                String artifactPath = artifact.getFile().getAbsolutePath();
                String scope = artifact.getScope();
                log.debug( "Adding to " + pluginName + " test classpath: " + artifactPath + " Scope: " + scope );
            }
        }

        return orderProviderArtifacts( result.getArtifacts() );
    }

    @Nonnull
    Map<String, Artifact> getProviderClasspathAsMap( String providerArtifactId, String providerVersion )
    {
        return artifactMapByVersionlessId( getProviderClasspath( providerArtifactId, providerVersion ) );
    }

    Set<Artifact> addProviderToClasspath( Map<String, Artifact> pluginArtifactMap, Artifact mojoPluginArtifact,
                                          Artifact surefireCommon, Artifact surefireApi, Artifact surefireLoggerApi )
    {
        Set<Artifact> providerArtifacts = new LinkedHashSet<>();
        ArtifactResolutionResult artifactResolutionResult = resolvePluginArtifact( mojoPluginArtifact );
        for ( Artifact artifact : pluginArtifactMap.values() )
        {
            if ( !artifactResolutionResult.getArtifacts().contains( artifact ) )
            {
                providerArtifacts.add( artifact );
                for ( Artifact dependency : resolvePluginArtifact( artifact ).getArtifacts() )
                {
                    String groupId = dependency.getGroupId();
                    String artifactId = dependency.getArtifactId();
                    if ( groupId.equals( surefireCommon.getGroupId() )
                            && artifactId.equals( surefireCommon.getArtifactId() ) )
                    {
                        providerArtifacts.add( surefireCommon );
                    }
                    else if ( groupId.equals( surefireApi.getGroupId() )
                            && artifactId.equals( surefireApi.getArtifactId() ) )
                    {
                        providerArtifacts.add( surefireApi );
                    }
                    else if ( groupId.equals( surefireLoggerApi.getGroupId() )
                            && artifactId.equals( surefireLoggerApi.getArtifactId() ) )
                    {
                        providerArtifacts.add( surefireLoggerApi );
                    }
                }
            }
        }
        return orderProviderArtifacts( providerArtifacts );
    }

    private static Set<Artifact> orderProviderArtifacts( Set<Artifact> providerArtifacts )
    {
        Set<Artifact> orderedProviderArtifacts = new LinkedHashSet<>();
        for ( String order : PROVIDER_CLASSPATH_ORDER )
        {
            Iterator<Artifact> providerArtifactsIt = providerArtifacts.iterator();
            while ( providerArtifactsIt.hasNext() )
            {
                Artifact providerArtifact = providerArtifactsIt.next();
                if ( providerArtifact.getArtifactId().equals( order ) )
                {
                    orderedProviderArtifacts.add( providerArtifact );
                    providerArtifactsIt.remove();
                }
            }
        }
        orderedProviderArtifacts.addAll( providerArtifacts );
        return orderedProviderArtifacts;
    }

    private static Dependency toProviderDependency( String providerArtifactId, String providerVersion )
    {
        Dependency dependency = new Dependency();
        dependency.setGroupId( PROVIDER_GROUP_ID );
        dependency.setArtifactId( providerArtifactId );
        dependency.setVersion( providerVersion );
        dependency.setType( "jar" );
        return dependency;
    }

    static class RuntimeArtifactFilter implements ArtifactFilter
    {
        private static final Collection<String> SCOPES =
                asList( SCOPE_COMPILE, SCOPE_COMPILE_PLUS_RUNTIME, SCOPE_RUNTIME );

        @Override
        public boolean include( Artifact artifact )
        {
            String scope = artifact.getScope();
            return !artifact.isOptional() && ( scope == null || SCOPES.contains( scope ) );
        }
    }
}
