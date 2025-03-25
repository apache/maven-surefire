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

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec;
import static org.apache.maven.plugin.surefire.SurefireDependencyResolver.PROVIDER_GROUP_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 *
 */
public class SurefireDependencyResolverTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldNotBeWithinRangeNullArtifact() {
        boolean result = SurefireDependencyResolver.isWithinVersionSpec(null, "[4.7,)");
        assertThat(result).isFalse();
    }

    @Test
    public void shouldNotBeWithinRange() throws InvalidVersionSpecificationException {
        Artifact api = createArtifact("junit", "junit", "4.6");
        boolean result = SurefireDependencyResolver.isWithinVersionSpec(api, "[4.7,)");
        assertThat(result).isFalse();
    }

    @Test
    public void shouldBeWithinRange() throws InvalidVersionSpecificationException {
        Artifact api = createArtifact("junit", "junit", "4.7");
        boolean result = SurefireDependencyResolver.isWithinVersionSpec(api, "[4.7,)");
        assertThat(result).isTrue();
    }

    @Test
    public void shouldBeFarWithinRange() throws InvalidVersionSpecificationException {
        Artifact api = createArtifact("junit", "junit", "4.13");
        boolean result = SurefireDependencyResolver.isWithinVersionSpec(api, "[4.7,)");
        assertThat(result).isTrue();
    }

    @Test
    public void shouldBeFailWithinRange() throws InvalidVersionSpecificationException {
        Artifact api = createArtifact("junit", "junit", "");
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Bug in plugin. Please report with stacktrace");
        SurefireDependencyResolver.isWithinVersionSpec(api, "[4.7,)");
    }

    @Test
    @Ignore("old not executing tests - to review")
    public void testResolveArtifact()
            throws InvalidVersionSpecificationException, MojoExecutionException, DependencyResolutionException {

        Artifact provider = createArtifact("surefire-junit-platform");
        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        RepositorySystemSession session = mock(RepositorySystemSession.class);
        ArgumentCaptor<DependencyRequest> requestCaptor = ArgumentCaptor.forClass(DependencyRequest.class);

        DependencyResult result = new DependencyResult(new DependencyRequest());
        when(repositorySystem.resolveDependencies(eq(session), requestCaptor.capture()))
                .thenReturn(result);

        SurefireDependencyResolver surefireDependencyResolver = new SurefireDependencyResolver(repositorySystem);
        surefireDependencyResolver.resolveArtifacts(session, Collections.emptyList(), provider);

        DependencyRequest value = requestCaptor.getValue();
        assertThat(value).isNotNull();
        org.eclipse.aether.graph.Dependency requestDependency =
                value.getCollectRequest().getRoot();
        assertThat(requestDependency.getArtifact().getGroupId()).isEqualTo(provider.getGroupId());
        assertThat(requestDependency.getArtifact().getArtifactId()).isEqualTo(provider.getArtifactId());
        assertThat(requestDependency.getArtifact().getVersion()).isEqualTo(provider.getVersion());
        assertThat(requestDependency.getArtifact().getExtension()).isEqualTo(provider.getType());
    }

    @Test
    public void testGetProviderClasspath() throws Exception {

        Artifact commonJunit4 = createArtifact("common-junit4");
        Artifact api = createArtifact("surefire-api");
        Artifact provider = createArtifact("surefire-junit-platform");
        Artifact ext = createArtifact("org.apiguardian", "apiguardian-api");
        Artifact logger = createArtifact("surefire-logger-api");

        Set<Artifact> providerArtifacts = new LinkedHashSet<>();
        providerArtifacts.add(commonJunit4);
        providerArtifacts.add(api);
        providerArtifacts.add(provider);
        providerArtifacts.add(ext);
        providerArtifacts.add(logger);

        List<ArtifactResult> artifactResults = providerArtifacts.stream()
                .map(RepositoryUtils::toArtifact)
                .map(a -> new ArtifactResult(new ArtifactRequest()).setArtifact(a))
                .collect(Collectors.toList());

        DependencyResult result = new DependencyResult(new DependencyRequest());
        result.setArtifactResults(artifactResults);

        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        RepositorySystemSession session = mock(RepositorySystemSession.class);

        when(session.getArtifactTypeRegistry()).thenReturn(mock(ArtifactTypeRegistry.class));
        when(repositorySystem.resolveDependencies(eq(session), any())).thenReturn(result);

        SurefireDependencyResolver surefireDependencyResolver = new SurefireDependencyResolver(repositorySystem);
        Set<Artifact> classpath = surefireDependencyResolver.getProviderClasspath(
                session, Collections.emptyList(), "surefire-junit-platform", "1");

        assertThat(classpath).hasSize(5);

        Iterator<Artifact> it = classpath.iterator();

        // result should be ordered
        assertThat(it.next()).isEqualTo(provider);
        assertThat(it.next()).isEqualTo(api);
        assertThat(it.next()).isEqualTo(logger);
        assertThat(it.next()).isEqualTo(commonJunit4);
        assertThat(it.next()).isEqualTo(ext);
    }

    @Test
    public void testGetProviderClasspathShouldPropagateTheResolutionException() throws Exception {

        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        RepositorySystemSession session = mock(RepositorySystemSession.class);
        when(session.getArtifactTypeRegistry()).thenReturn(mock(ArtifactTypeRegistry.class));

        DependencyResolutionException dependencyResolutionException =
                new DependencyResolutionException(new DependencyResult(new DependencyRequest()), new Exception());
        when(repositorySystem.resolveDependencies(eq(session), any())).thenThrow(dependencyResolutionException);

        SurefireDependencyResolver surefireDependencyResolver = new SurefireDependencyResolver(repositorySystem);

        assertThatThrownBy(() -> surefireDependencyResolver.getProviderClasspath(
                        session, Collections.emptyList(), "surefire-junit-platform", "1"))
                .isInstanceOf(MojoExecutionException.class)
                .hasCause(dependencyResolutionException);
    }

    @Test
    public void testResolvePluginDependencies() throws Exception {
        Dependency providerAsDependency = new Dependency();
        providerAsDependency.setGroupId(PROVIDER_GROUP_ID);
        providerAsDependency.setArtifactId("surefire-shadefire");
        providerAsDependency.setVersion("1");

        Artifact providerAsArtifact = createArtifact("surefire-shadefire");

        Plugin plugin = mock(Plugin.class);
        when(plugin.getDependencies()).thenReturn(singletonList(providerAsDependency));

        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        RepositorySystemSession session = mock(RepositorySystemSession.class);
        when(session.getArtifactTypeRegistry()).thenReturn(mock(ArtifactTypeRegistry.class));

        ArtifactResult artifactResult =
                new ArtifactResult(new ArtifactRequest().setArtifact(RepositoryUtils.toArtifact(providerAsArtifact)));
        artifactResult.setArtifact(RepositoryUtils.toArtifact(providerAsArtifact));
        DependencyResult result = new DependencyResult(new DependencyRequest());
        result.setArtifactResults(Collections.singletonList(artifactResult));

        when(repositorySystem.resolveDependencies(eq(session), any())).thenReturn(result);

        Map<String, Artifact> pluginResolvedDependencies =
                singletonMap(PROVIDER_GROUP_ID + ":surefire-shadefire", providerAsArtifact);

        SurefireDependencyResolver surefireDependencyResolver = new SurefireDependencyResolver(repositorySystem);

        Map<String, Artifact> providers = surefireDependencyResolver.resolvePluginDependencies(
                session, Collections.emptyList(), plugin, pluginResolvedDependencies);

        assertThat(providers.values()).hasSize(1).containsOnly(providerAsArtifact);

        verify(repositorySystem).resolveDependencies(eq(session), any());
        verifyNoMoreInteractions(repositorySystem);
    }

    private static Artifact createArtifact(String artifactId) throws InvalidVersionSpecificationException {
        return createArtifact(PROVIDER_GROUP_ID, artifactId);
    }

    private static Artifact createArtifact(String groupId, String artifactId)
            throws InvalidVersionSpecificationException {
        return createArtifact(groupId, artifactId, "1");
    }

    private static Artifact createArtifact(String groupId, String artifactId, String version)
            throws InvalidVersionSpecificationException {
        VersionRange versionSpec = createFromVersionSpec(version);
        DefaultArtifact defaultArtifact = new DefaultArtifact(
                groupId, artifactId, versionSpec, null, "jar", null, new DefaultArtifactHandler("jar"));
        defaultArtifact.setFile(new File(""));
        return defaultArtifact;
    }
}
