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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.project.MavenProject;
import org.apache.maven.surefire.providerapi.ProviderInfo;
import org.apache.maven.surefire.providerapi.ProviderRequirements;

class JUnitPlatformProviderInfo implements ProviderInfo {

    private static final String PROVIDER_DEP_GID = "org.junit.platform";
    private static final String PROVIDER_DEP_AID = "junit-platform-launcher";

    private final Artifact junitPlatformRunnerArtifact;
    private final Artifact junitPlatformArtifact;
    private final TestClassPath testClasspath;

    private final Artifact junitArtifact;
    private final Artifact junitDepArtifact;
    private final Artifact booterArtifact;
    private final SurefireDependencyResolver surefireDependencyResolver;
    private final MavenSession session;
    private final MavenProject project;
    private final PluginDescriptor pluginDescriptor;
    private final Map<String, Artifact> pluginArtifactMap;
    private final ConsoleLogger consoleLogger;
    private ProviderInfo.Engine engine = ProviderInfo.Engine.JUNIT_PLATFORM;

    @SuppressWarnings("checkstyle:parameternumber")
    JUnitPlatformProviderInfo(
            Artifact junitPlatformRunnerArtifact,
            Artifact junitPlatformArtifact,
            @Nonnull TestClassPath testClasspath,
            Artifact junitArtifact,
            Artifact junitDepArtifact,
            Artifact booterArtifact,
            SurefireDependencyResolver surefireDependencyResolver,
            MavenSession session,
            MavenProject project,
            PluginDescriptor pluginDescriptor,
            Map<String, Artifact> pluginArtifactMap,
            ConsoleLogger consoleLogger) {
        this.session = session;
        this.project = project;
        this.pluginDescriptor = pluginDescriptor;
        this.pluginArtifactMap = pluginArtifactMap;
        this.consoleLogger = consoleLogger;
        this.junitPlatformRunnerArtifact = junitPlatformRunnerArtifact;
        this.junitPlatformArtifact = junitPlatformArtifact;
        this.testClasspath = testClasspath;
        this.junitArtifact = junitArtifact;
        this.junitDepArtifact = junitDepArtifact;
        this.booterArtifact = booterArtifact;
        // FIXME check this logic in align versions
        // JUnit 4.7+ vintage engine
        // JUnit 4.12+ is required by JUnit 5.3+
        // JUnit 4.13+ is required by JUnit 5.8+
        // JUnit 4.13.2 is required by JUnit 5.9+
        // JUnit 4.13.3 is required by JUnit 5.10+
        // JUnit 4.13.4 is required by JUnit 5.12+
        // https://junit.org/junit5/docs/current/user-guide/#dependency-metadata
        //
        if (junitDepArtifact != null || isAnyJunit4(junitArtifact)) {
            this.engine = ProviderInfo.Engine.JUNIT47;
        }
        this.surefireDependencyResolver = surefireDependencyResolver;
    }

    @Override
    @Nonnull
    public String getProviderName() {
        return "org.apache.maven.surefire.junitplatform.JUnitPlatformProvider";
    }

    protected String getProviderArtifactName() {
        return "surefire-junit-platform";
    }

    @Override
    public boolean isApplicable() {
        return (junitPlatformRunnerArtifact == null && junitPlatformArtifact != null)
                || (junitDepArtifact != null || isAnyJunit4(junitArtifact));
    }

    @Override
    public void addProviderProperties() {
        // TODO check if ok to call in AbstractSurefireMojo.executeAfterPreconditionsChecked()
        //        convertGroupParameters();
        //        convertJunitEngineParameters();
    }

    @Nonnull
    @Override
    public List<String[]> getJpmsArguments(@Nonnull ProviderRequirements forkRequirements) {
        boolean hasTestDescriptor = forkRequirements.isModularPath() && forkRequirements.hasTestModuleDescriptor();
        return hasTestDescriptor ? getJpmsArgs() : Collections.emptyList();
    }

    @Override
    @Nonnull
    public Set<Artifact> getProviderClasspath() throws MojoExecutionException {
        String surefireVersion = booterArtifact.getBaseVersion();
        Map<String, Artifact> providerArtifacts = surefireDependencyResolver.getProviderClasspathAsMap(
                session.getRepositorySession(),
                project.getRemotePluginRepositories(),
                getProviderArtifactName(),
                surefireVersion);
        Map<String, Artifact> testDeps = testClasspath.getTestDependencies();

        Plugin plugin = pluginDescriptor.getPlugin();
        Map<String, Artifact> pluginDeps = surefireDependencyResolver.resolvePluginDependencies(
                session.getRepositorySession(), project.getRemotePluginRepositories(), plugin, pluginArtifactMap);

        if (hasDependencyPlatformEngine(pluginDeps)) {
            providerArtifacts.putAll(pluginDeps);
        } else {
            String engineVersion = null;
            if (hasDependencyJupiterAPI(testDeps) && !testDeps.containsKey("org.junit.jupiter:junit-jupiter-engine")) {
                String engineGroupId = "org.junit.jupiter";
                String engineArtifactId = "junit-jupiter-engine";
                String engineCoordinates = engineGroupId + ":" + engineArtifactId;
                String api = "org.junit.jupiter:junit-jupiter-api";
                engineVersion = testDeps.get(api).getBaseVersion();
                consoleLogger.debug(
                        "Test dependencies contain " + api + ". Resolving " + engineCoordinates + ":" + engineVersion);
                addEngineByApi(engineGroupId, engineArtifactId, engineVersion, providerArtifacts);
            }

            if ((testDeps.containsKey("junit:junit") || testDeps.containsKey("junit:junit-dep"))
                    && !testDeps.containsKey("org.junit.vintage:junit-vintage-engine")) {
                String engineGroupId = "org.junit.vintage";
                String engineArtifactId = "junit-vintage-engine";
                String engineCoordinates = engineGroupId + ":" + engineArtifactId;

                if (engineVersion != null) {
                    consoleLogger.debug(
                            "Test dependencies contain JUnit4. Resolving " + engineCoordinates + ":" + engineVersion);
                    addEngineByApi(engineGroupId, engineArtifactId, engineVersion, providerArtifacts);
                } else {
                    addEngineByApi(engineGroupId, engineArtifactId, "5.12.1", providerArtifacts);
                }
            }
        }

        narrowDependencies(providerArtifacts, testDeps);
        alignProviderVersions(providerArtifacts);

        return new LinkedHashSet<>(providerArtifacts.values());
    }

    private List<String[]> getJpmsArgs() {
        List<String[]> args = new ArrayList<>();

        args.add(
                new String[] {"--add-opens", "org.junit.platform.commons/org.junit.platform.commons.util=ALL-UNNAMED"});

        args.add(
                new String[] {"--add-opens", "org.junit.platform.commons/org.junit.platform.commons.logging=ALL-UNNAMED"
                });

        return args;
    }

    private void addEngineByApi(
            String engineGroupId,
            String engineArtifactId,
            String engineVersion,
            Map<String, Artifact> providerArtifacts)
            throws MojoExecutionException {
        for (Artifact dep : resolve(engineGroupId, engineArtifactId, engineVersion, null, "jar")) {
            String key = dep.getGroupId() + ":" + dep.getArtifactId();
            providerArtifacts.put(key, dep);
        }
    }

    private void narrowDependencies(Map<String, Artifact> providerArtifacts, Map<String, Artifact> testDependencies) {
        providerArtifacts.keySet().removeAll(testDependencies.keySet());
    }

    protected void alignProviderVersions(Map<String, Artifact> providerArtifacts) throws MojoExecutionException {
        if (junitPlatformArtifact == null) {
            return;
        }
        String version = junitPlatformArtifact.getBaseVersion();
        for (Artifact launcherArtifact : resolve(PROVIDER_DEP_GID, PROVIDER_DEP_AID, version, null, "jar")) {
            String key = launcherArtifact.getGroupId() + ":" + launcherArtifact.getArtifactId();
            if (providerArtifacts.containsKey(key)) {
                providerArtifacts.put(key, launcherArtifact);
            }
        }
    }

    private Set<Artifact> resolve(String g, String a, String v, String c, String t) throws MojoExecutionException {
        // FIXME will be different with 3 and testng
        ArtifactHandler handler = junitPlatformArtifact != null
                ? junitPlatformArtifact.getArtifactHandler()
                : junitArtifact.getArtifactHandler();
        Artifact artifact = new DefaultArtifact(g, a, v, null, t, c, handler);
        consoleLogger.debug("Resolving artifact " + g + ":" + a + ":" + v);
        Set<Artifact> r = surefireDependencyResolver.resolveArtifacts(
                session.getRepositorySession(), project.getRemoteProjectRepositories(), artifact);
        consoleLogger.debug("Resolved artifact " + g + ":" + a + ":" + v + " to " + r);
        return r;
    }

    private boolean hasDependencyJupiterAPI(Map<String, Artifact> dependencies) {
        return dependencies.containsKey("org.junit.jupiter:junit-jupiter-api");
    }

    private boolean hasDependencyPlatformEngine(Map<String, Artifact> dependencies) {
        for (Map.Entry<String, Artifact> dependency : dependencies.entrySet()) {
            if (dependency.getKey().equals("org.junit.platform:junit-platform-engine")) {
                return true;
            }
        }

        return false;
    }

    // TODO why an enclosing class on the top do we reall need this shading???
    @SuppressWarnings("checkstyle:parameternumber")
    public static class JUnitPlatformProviderShadefireInfo extends JUnitPlatformProviderInfo {

        JUnitPlatformProviderShadefireInfo(
                Artifact junitPlatformRunnerArtifact,
                Artifact junitPlatformArtifact,
                @Nonnull TestClassPath testClasspath,
                Artifact junitArtifact,
                Artifact junitDepArtifact,
                Artifact booterArtifact,
                SurefireDependencyResolver surefireDependencyResolver,
                MavenSession session,
                MavenProject project,
                PluginDescriptor pluginDescriptor,
                Map<String, Artifact> pluginArtifactMap,
                ConsoleLogger consoleLogger) {
            super(
                    junitPlatformRunnerArtifact,
                    junitPlatformArtifact,
                    testClasspath,
                    junitArtifact,
                    junitDepArtifact,
                    booterArtifact,
                    surefireDependencyResolver,
                    session,
                    project,
                    pluginDescriptor,
                    pluginArtifactMap,
                    consoleLogger);
        }

        @Override
        public boolean isApplicable() {
            // newer discover this provider automatically
            return false;
        }

        @Override
        @Nonnull
        public String getProviderName() {
            return "org.apache.maven.shadefire.surefire.junitplatform.JUnitPlatformProvider";
        }

        @Override
        protected String getProviderArtifactName() {
            return "surefire-shadefire";
        }

        @Override
        protected void alignProviderVersions(Map<String, Artifact> providerArtifacts) throws MojoExecutionException {
            // shadefire is used as booter we can not provide additional dependencies,
            // so we need add a launcher here
            providerArtifacts.put("org.junit.platform:junit-platform-launcher", null);
            super.alignProviderVersions(providerArtifacts);
        }
    }
}
