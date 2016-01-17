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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.apache.maven.surefire.booter.Classpath;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.util.Collections.singletonList;
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
    private final RepositorySystem repositorySystem;

    private final ArtifactFactory artifactFactory;

    private final Log log;

    private final ArtifactRepository localRepository;

    private final List<ArtifactRepository> remoteRepositories;

    private final String pluginName;

    protected SurefireDependencyResolver( RepositorySystem repositorySystem, ArtifactFactory artifactFactory, Log log,
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

    public ArtifactResolutionResult resolveArtifact( Artifact providerArtifact )
        throws ArtifactNotFoundException, ArtifactResolverException, ArtifactResolutionException
    {
        return resolveArtifact( providerArtifact, null );
    }

    public ArtifactResolutionResult resolveArtifact( Artifact providerArtifact, Artifact excludeArtifact )
        throws ArtifactResolutionException, ArtifactNotFoundException, ArtifactResolverException
    {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                                                    .setArtifact( providerArtifact )
                                                    .setRemoteRepositories( remoteRepositories )
                                                    .setLocalRepository( localRepository );
        if ( excludeArtifact != null )
        {
            String pattern = excludeArtifact.getGroupId() + ":" + excludeArtifact.getArtifactId();
            request.setCollectionFilter( new ExcludesArtifactFilter( singletonList( pattern ) ) );
        }
        return repositorySystem.resolve( request );
    }

    public Classpath getProviderClasspath( String provider, String version )
            throws ArtifactNotFoundException, ArtifactResolutionException, ArtifactResolverException
    {
        return getProviderClasspath( provider, version, null );
    }

    public Classpath getProviderClasspath( String provider, String version, Artifact excludeArtifact )
        throws ArtifactNotFoundException, ArtifactResolutionException, ArtifactResolverException
    {
        Classpath classPath = ClasspathCache.getCachedClassPath( provider );
        if ( classPath == null )
        {
            Artifact providerArtifact =
                    artifactFactory.createDependencyArtifact( "org.apache.maven.surefire", provider,
                                                              createFromVersion( version ), "jar", null, SCOPE_TEST );
            List<String> files = new ArrayList<String>();
            for ( Artifact artifact : resolveArtifact( providerArtifact ).getArtifacts() )
            {
                String artifactPath = artifact.getFile().getAbsolutePath();
                log.debug( "Adding to " + pluginName + " test classpath: " + artifactPath + " Scope: "
                        + artifact.getScope() );
                files.add( artifactPath );
            }
            classPath = new Classpath( files );
            ClasspathCache.setCachedClasspath( provider, classPath );
        }
        return classPath;
    }

    public Classpath addProviderToClasspath( Map<String, Artifact> pluginArtifactMap, Artifact surefireArtifact )
        throws ArtifactResolutionException, ArtifactNotFoundException, ArtifactResolverException
    {
        List<String> files = new ArrayList<String>();
        if ( surefireArtifact != null )
        {
            final ArtifactResolutionResult artifactResolutionResult = resolveArtifact( surefireArtifact );
            for ( Artifact artifact : pluginArtifactMap.values() )
            {
                if ( !artifactResolutionResult.getArtifacts().contains( artifact ) )
                {
                    files.add( artifact.getFile().getAbsolutePath() );
                }
            }
        }
        else
        {
            // Bit of a brute force strategy if not found. Should probably be improved
            for ( Artifact artifact : pluginArtifactMap.values() )
            {
                files.add( artifact.getFile().getPath() );
            }
        }
        return new Classpath( files );
    }

}
