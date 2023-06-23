/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugin.surefire;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import static org.apache.maven.artifact.ArtifactUtils.artifactMapByVersionlessId;
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec;

/**
 * Does dependency resolution and artifact handling for the surefire plugin.
 *
 * @author Stephen Connolly
 * @author Kristian Rosenvold
 */
@Named
@Singleton
class SurefireDependencyResolver {

    static final String PROVIDER_GROUP_ID = "org.apache.maven.surefire";

    private static final String[] PROVIDER_CLASSPATH_ORDER = {
        "surefire-junit3",
        "surefire-junit4",
        "surefire-junit47",
        "surefire-testng",
        "surefire-junit-platform",
        "surefire-api",
        "surefire-logger-api",
        "surefire-shared-utils",
        "common-java5",
        "common-junit3",
        "common-junit4",
        "common-junit48",
        "common-testng-utils"
    };

    private final RepositorySystem repositorySystem;

    @Inject
    SurefireDependencyResolver(RepositorySystem repositorySystem) {
        this.repositorySystem = repositorySystem;
    }

    static boolean isWithinVersionSpec(@Nullable Artifact artifact, @Nonnull String versionSpec) {
        if (artifact == null) {
            return false;
        }
        try {
            VersionRange range = createFromVersionSpec(versionSpec);
            try {
                return range.containsVersion(artifact.getSelectedVersion());
            } catch (NullPointerException e) {
                return range.containsVersion(new DefaultArtifactVersion(artifact.getBaseVersion()));
            }
        } catch (InvalidVersionSpecificationException | OverConstrainedVersionException e) {
            throw new RuntimeException("Bug in plugin. Please report with stacktrace");
        }
    }

    Map<String, Artifact> resolvePluginDependencies(
            RepositorySystemSession session,
            List<RemoteRepository> repositories,
            Plugin plugin,
            Map<String, Artifact> pluginResolvedDependencies)
            throws MojoExecutionException {
        Map<String, Artifact> resolved = new LinkedHashMap<>();
        Collection<Dependency> pluginDependencies = plugin.getDependencies();

        for (Dependency dependency : pluginDependencies) {
            Set<Artifact> artifacts = resolveDependencies(
                    session, repositories, RepositoryUtils.toDependency(dependency, session.getArtifactTypeRegistry()));
            for (Artifact artifact : artifacts) {
                String key = artifact.getGroupId() + ":" + artifact.getArtifactId();
                Artifact resolvedPluginDependency = pluginResolvedDependencies.get(key);
                if (resolvedPluginDependency != null) {
                    resolved.put(key, artifact);
                }
            }
        }
        return resolved;
    }

    public Set<Artifact> resolveArtifacts(
            RepositorySystemSession session, List<RemoteRepository> repositories, Artifact artifact)
            throws MojoExecutionException {
        return resolveDependencies(session, repositories, RepositoryUtils.toDependency(artifact, null));
    }

    public Set<Artifact> resolveDependencies(
            RepositorySystemSession session, List<RemoteRepository> repositories, Dependency dependency)
            throws MojoExecutionException {
        return resolveDependencies(
                session, repositories, RepositoryUtils.toDependency(dependency, session.getArtifactTypeRegistry()));
    }

    private Set<Artifact> resolveDependencies(
            RepositorySystemSession session,
            List<RemoteRepository> repositories,
            org.eclipse.aether.graph.Dependency dependency)
            throws MojoExecutionException {

        try {
            List<ArtifactResult> results = resolveDependencies(
                    session, repositories, dependency, DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME));
            return results.stream()
                    .map(ArtifactResult::getArtifact)
                    .map(RepositoryUtils::toArtifact)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

        } catch (DependencyResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private List<ArtifactResult> resolveDependencies(
            RepositorySystemSession session,
            List<RemoteRepository> repositories,
            org.eclipse.aether.graph.Dependency dependency,
            DependencyFilter dependencyFilter)
            throws DependencyResolutionException {

        // use a collect request without a root in order to not resolve optional dependencies
        CollectRequest collectRequest = new CollectRequest(Collections.singletonList(dependency), null, repositories);

        DependencyRequest request = new DependencyRequest();
        request.setCollectRequest(collectRequest);
        request.setFilter(dependencyFilter);

        DependencyResult dependencyResult = repositorySystem.resolveDependencies(session, request);
        return dependencyResult.getArtifactResults();
    }

    @Nonnull
    Set<Artifact> getProviderClasspath(
            RepositorySystemSession session,
            List<RemoteRepository> repositories,
            String providerArtifactId,
            String providerVersion)
            throws MojoExecutionException {
        Dependency provider = toProviderDependency(providerArtifactId, providerVersion);

        org.eclipse.aether.graph.Dependency dependency =
                RepositoryUtils.toDependency(provider, session.getArtifactTypeRegistry());

        Set<Artifact> result = resolveDependencies(session, repositories, dependency);

        return orderProviderArtifacts(result);
    }

    @Nonnull
    Map<String, Artifact> getProviderClasspathAsMap(
            RepositorySystemSession session,
            List<RemoteRepository> repositories,
            String providerArtifactId,
            String providerVersion)
            throws MojoExecutionException {
        return artifactMapByVersionlessId(
                getProviderClasspath(session, repositories, providerArtifactId, providerVersion));
    }

    // FIXME
    // method argument should be unchanged
    // what if providerArtifacts will be unmodifiable
    private static Set<Artifact> orderProviderArtifacts(Set<Artifact> providerArtifacts) {
        Set<Artifact> orderedProviderArtifacts = new LinkedHashSet<>();
        for (String order : PROVIDER_CLASSPATH_ORDER) {
            Iterator<Artifact> providerArtifactsIt = providerArtifacts.iterator();
            while (providerArtifactsIt.hasNext()) {
                Artifact providerArtifact = providerArtifactsIt.next();
                if (providerArtifact.getArtifactId().equals(order)) {
                    orderedProviderArtifacts.add(providerArtifact);
                    providerArtifactsIt.remove();
                }
            }
        }
        orderedProviderArtifacts.addAll(providerArtifacts);
        return orderedProviderArtifacts;
    }

    private static Dependency toProviderDependency(String providerArtifactId, String providerVersion) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(PROVIDER_GROUP_ID);
        dependency.setArtifactId(providerArtifactId);
        dependency.setVersion(providerVersion);
        dependency.setType("jar");
        return dependency;
    }
}
