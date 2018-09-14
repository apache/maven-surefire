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
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.apache.maven.artifact.Artifact.SCOPE_TEST;
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersion;

/**
 * Does dependency resolution and artifact handling for the surefire plugin.
 *
 * @author Stephen Connolly
 * @author Kristian Rosenvold
 */
public class SurefireDependencyResolver
{

    private final ArtifactResolver artifactResolver;

    private final ArtifactFactory artifactFactory;

    private final ConsoleLogger log;

    private final ArtifactRepository localRepository;

    private final List<ArtifactRepository> remoteRepositories;

    private final ArtifactMetadataSource artifactMetadataSource;

    private final String pluginName;

    protected SurefireDependencyResolver( ArtifactResolver artifactResolver, ArtifactFactory artifactFactory,
                                          ConsoleLogger log,
                                          ArtifactRepository localRepository,
                                          List<ArtifactRepository> remoteRepositories,
                                          ArtifactMetadataSource artifactMetadataSource, String pluginName )
    {
        this.artifactResolver = artifactResolver;
        this.artifactFactory = artifactFactory;
        this.log = log;
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
        this.artifactMetadataSource = artifactMetadataSource;
        this.pluginName = pluginName;
    }


    public boolean isWithinVersionSpec( @Nullable Artifact artifact, @Nonnull String versionSpec )
    {
        if ( artifact == null )
        {
            return false;
        }
        try
        {
            VersionRange range = VersionRange.createFromVersionSpec( versionSpec );
            try
            {
                return range.containsVersion( artifact.getSelectedVersion() );
            }
            catch ( NullPointerException e )
            {
                return range.containsVersion( new DefaultArtifactVersion( artifact.getBaseVersion() ) );
            }
        }
        catch ( InvalidVersionSpecificationException e )
        {
            throw new RuntimeException( "Bug in plugin. Please report with stacktrace" );
        }
        catch ( OverConstrainedVersionException e )
        {
            throw new RuntimeException( "Bug in plugin. Please report with stacktrace" );
        }
    }


    private ArtifactResolutionResult resolveArtifact( Artifact filteredArtifact, Artifact providerArtifact )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        ArtifactFilter filter = null;
        if ( filteredArtifact != null )
        {
            filter = new ExcludesArtifactFilter(
                Collections.singletonList( filteredArtifact.getGroupId() + ":" + filteredArtifact.getArtifactId() ) );
        }

        Artifact originatingArtifact = artifactFactory.createBuildArtifact( "dummy", "dummy", "1.0", "jar" );

        return artifactResolver.resolveTransitively( singleton( providerArtifact ), originatingArtifact,
                                                     localRepository, remoteRepositories, artifactMetadataSource,
                                                     filter );
    }

    @Nonnull
    @SuppressWarnings( "unchecked" )
    public Set<Artifact> getProviderClasspath( String provider, String version, Artifact filteredArtifact )
        throws ArtifactNotFoundException, ArtifactResolutionException
    {
        Artifact providerArtifact = artifactFactory.createDependencyArtifact( "org.apache.maven.surefire",
                provider, createFromVersion( version ), "jar", null, SCOPE_TEST );

        ArtifactResolutionResult result = resolveArtifact( filteredArtifact, providerArtifact );

        if ( log.isDebugEnabled() )
        {
            for ( Object o : result.getArtifacts() )
            {
                Artifact artifact = (Artifact) o;
                String artifactPath = artifact.getFile().getAbsolutePath();
                String scope = artifact.getScope();
                log.debug( "Adding to " + pluginName + " test classpath: " + artifactPath + " Scope: " + scope );
            }
        }

        return result.getArtifacts();
    }

    public Set<Artifact> addProviderToClasspath( Map<String, Artifact> pluginArtifactMap, Artifact surefireArtifact )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        Set<Artifact> providerArtifacts = new LinkedHashSet<Artifact>();
        if ( surefireArtifact != null )
        {
            ArtifactResolutionResult artifactResolutionResult = resolveArtifact( null, surefireArtifact );
            for ( Artifact artifact : pluginArtifactMap.values() )
            {
                if ( !artifactResolutionResult.getArtifacts().contains( artifact ) )
                {
                    providerArtifacts.add( artifact );
                }
            }
        }
        else
        {
            // Bit of a brute force strategy if not found. Should probably be improved
            providerArtifacts.addAll( pluginArtifactMap.values() );
        }
        return providerArtifacts;
    }
}
