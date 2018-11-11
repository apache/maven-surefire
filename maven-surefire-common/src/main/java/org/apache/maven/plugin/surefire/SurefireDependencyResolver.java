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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.repository.RepositorySystem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.util.Collections.singletonList;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersion;
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

    private final ArtifactFactory artifactFactory;

    private final ConsoleLogger log;

    private final ArtifactRepository localRepository;

    private final List<ArtifactRepository> remoteRepositories;

    private final String pluginName;

    SurefireDependencyResolver( RepositorySystem repositorySystem, ArtifactFactory artifactFactory, ConsoleLogger log,
                                          ArtifactRepository localRepository,
                                          List<ArtifactRepository> remoteRepositories, String pluginName )
    {
        this.repositorySystem = repositorySystem;
        this.artifactFactory = artifactFactory;
        this.log = log;
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
        this.pluginName = pluginName;
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

    ArtifactResolutionResult resolveArtifact( Artifact providerArtifact )
    {
        return resolveArtifact( providerArtifact, null );
    }

    private ArtifactResolutionResult resolveArtifact( Artifact providerArtifact, @Nullable Artifact excludeArtifact )
    {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                                                    .setArtifact( providerArtifact )
                                                    .setRemoteRepositories( remoteRepositories )
                                                    .setLocalRepository( localRepository )
                                                    .setResolveTransitively( true );
        if ( excludeArtifact != null )
        {
            String pattern = excludeArtifact.getGroupId() + ":" + excludeArtifact.getArtifactId();
            request.setCollectionFilter( new ExcludesArtifactFilter( singletonList( pattern ) ) );
        }
        return repositorySystem.resolve( request );
    }

    @Nonnull
    Set<Artifact> getProviderClasspath( String providerArtifactId, String providerVersion )
    {
        Artifact providerArtifact = artifactFactory.createDependencyArtifact( PROVIDER_GROUP_ID,
                providerArtifactId, createFromVersion( providerVersion ), "jar", null, SCOPE_TEST );

        ArtifactResolutionResult result = resolveArtifact( providerArtifact );

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

    Set<Artifact> addProviderToClasspath( Map<String, Artifact> pluginArtifactMap, Artifact mojoPluginArtifact,
                                          Artifact surefireCommon, Artifact surefireApi, Artifact surefireLoggerApi )
    {
        Set<Artifact> providerArtifacts = new LinkedHashSet<>();
        ArtifactResolutionResult artifactResolutionResult = resolveArtifact( mojoPluginArtifact );
        for ( Artifact artifact : pluginArtifactMap.values() )
        {
            if ( !artifactResolutionResult.getArtifacts().contains( artifact ) )
            {
                providerArtifacts.add( artifact );
                for ( Artifact dependency : resolveArtifact( artifact ).getArtifacts() )
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
}
