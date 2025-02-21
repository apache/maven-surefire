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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import static java.io.File.separatorChar;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.apache.maven.surefire.booter.SystemUtils.toJdkHomeFromJre;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.reflect.Whitebox.invokeMethod;

/**
 * Test for {@link AbstractSurefireMojo}. jdkToolchain parameter
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AbstractSurefireMojo.class})
@PowerMockIgnore({"org.jacoco.agent.rt.*", "com.vladium.emma.rt.*"})
public class AbstractSurefireMojoToolchainsTest {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    /**
     * Ensure that we use the toolchain found by getToolchain()
     * when the jdkToolchain parameter is set.
     */
    @Test
    public void shouldCallMethodWhenSpecSet() throws Exception {
        AbstractSurefireMojoTest.Mojo mojo = new AbstractSurefireMojoTest.Mojo();
        Toolchain expectedMethod = mock(Toolchain.class);
        MockToolchainManager toolchainManager = new MockToolchainManager(expectedMethod, null);
        mojo.setToolchainManager(toolchainManager);
        mojo.setJdkToolchain(singletonMap("version", "1.8"));
        Toolchain actual = invokeMethod(mojo, "getToolchain");
        assertThat(actual).isSameAs(expectedMethod);
    }

    /**
     * Ensure that we use the toolchain from build context when
     * no jdkToolchain map is configured in mojo parameters.
     * getToolchain() returns the main maven toolchain from the build context
     */
    @Test
    public void shouldFallthroughToBuildContextWhenNoSpecSet() throws Exception {
        AbstractSurefireMojoTest.Mojo mojo = new AbstractSurefireMojoTest.Mojo();
        Toolchain expectedFromContext = mock(Toolchain.class);
        Toolchain expectedFromSpec = mock(Toolchain.class); // ensure it still behaves correctly even if not null
        mojo.setToolchainManager(new MockToolchainManager(expectedFromSpec, expectedFromContext));
        Toolchain actual = invokeMethod(mojo, "getToolchain");
        assertThat(actual).isSameAs(expectedFromContext);
    }

    // TODO Is this still required?
    @Test(expected = MojoFailureException.class)
    @Ignore
    public void shouldThrowToolchain() throws Exception {
        invokeMethod(AbstractSurefireMojo.class, "getToolchain");
    }

    // TODO Is this still required?
    @Test
    @Ignore
    public void shouldGetToolchain() throws Exception {
        Toolchain expected = mock(Toolchain.class);
        Toolchain actual = invokeMethod(AbstractSurefireMojo.class, "getToolchain");

        assertThat(actual).isSameAs(expected);
    }

    /**
     * Ensures that the environmentVariables map for launching a test jvm
     * contains a Toolchain-driven entry when toolchain is set.
     */
    @Test
    public void shouldChangeJavaHomeFromToolchain() throws Exception {
        AbstractSurefireMojoTest.Mojo mojo = new AbstractSurefireMojoTest.Mojo();
        DefaultJavaToolChain toolchain = mock(DefaultJavaToolChain.class);
        when(toolchain.findTool("java")).thenReturn("/path/from/toolchain");
        when(toolchain.getJavaHome()).thenReturn("/some/path");
        mojo.setToolchain(toolchain);

        assertThat(mojo.getEnvironmentVariables()).isEmpty();
        JdkAttributes effectiveJvm = invokeMethod(mojo, "getEffectiveJvm");
        assertThat(mojo.getEnvironmentVariables()).containsEntry("JAVA_HOME", "/some/path");
        assertThat(effectiveJvm.getJvmExecutable().getPath())
                .contains("/path/from/toolchain".replace('/', separatorChar));
    }

    @Test
    public void shouldNotChangeJavaHomeFromToolchainIfAlreadySet() throws Exception {
        AbstractSurefireMojoTest.Mojo mojo = new AbstractSurefireMojoTest.Mojo();
        mojo.setEnvironmentVariables(singletonMap("JAVA_HOME", "/already/set/path"));

        DefaultJavaToolChain toolchain = mock(DefaultJavaToolChain.class);
        when(toolchain.findTool("java")).thenReturn("/path/from/toolchain");
        when(toolchain.getJavaHome()).thenReturn("/some/path");
        mojo.setToolchain(toolchain);

        JdkAttributes effectiveJvm = invokeMethod(mojo, "getEffectiveJvm");
        assertThat(mojo.getEnvironmentVariables()).containsEntry("JAVA_HOME", "/already/set/path");
        assertThat(effectiveJvm.getJvmExecutable().getPath())
                .contains("/path/from/toolchain".replace('/', separatorChar));
    }

    /**
     * Ensures that the environmentVariables map for launching a test jvm
     * contains a jvm parameter-driven entry when jvm is set.
     */
    @Test
    public void shouldChangeJavaHomeFromJvm() throws Exception {
        AbstractSurefireMojoTest.Mojo mojo = new AbstractSurefireMojoTest.Mojo();

        File currentJdkHome = toJdkHomeFromJre();
        String javaExecutablePath = FilenameUtils.concat(currentJdkHome.getAbsolutePath(), "bin/java");

        mojo.setJvm(javaExecutablePath);

        assertThat(mojo.getEnvironmentVariables()).isEmpty();
        JdkAttributes effectiveJvm = invokeMethod(mojo, "getEffectiveJvm");
        assertThat(mojo.getEnvironmentVariables()).containsEntry("JAVA_HOME", currentJdkHome.getAbsolutePath());
        assertThat(effectiveJvm.getJvmExecutable().getPath()).contains(javaExecutablePath);
    }

    /**
     * Ensures that users can manually configure a value for JAVA_HOME
     * and we will not override it
     */
    @Test
    public void shouldNotChangeJavaHomeFromJvmIfAlreadySet() throws Exception {
        AbstractSurefireMojoTest.Mojo mojo = new AbstractSurefireMojoTest.Mojo();
        mojo.setEnvironmentVariables(singletonMap("JAVA_HOME", "/already/set/path"));

        File currentJdkHome = toJdkHomeFromJre();
        String javaExecutablePath = FilenameUtils.concat(currentJdkHome.getAbsolutePath(), "bin/java");

        mojo.setJvm(javaExecutablePath);

        JdkAttributes effectiveJvm = invokeMethod(mojo, "getEffectiveJvm");
        assertThat(mojo.getEnvironmentVariables()).containsEntry("JAVA_HOME", "/already/set/path");
        assertThat(effectiveJvm.getJvmExecutable().getPath()).contains(javaExecutablePath);
    }

    @Test
    public void withoutJvmAndToolchain() throws Exception {
        AbstractSurefireMojoTest.Mojo mojo = new AbstractSurefireMojoTest.Mojo();
        Logger logger = mock(Logger.class);
        mojo.setLogger(logger);
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        JdkAttributes effectiveJvm = invokeMethod(mojo, "getEffectiveJvm");

        assertThat(mojo.getJvm()).isNull();

        assertThat(mojo.getEnvironmentVariables()).isEmpty();

        assertThat(effectiveJvm).isNotNull();

        assertThat(effectiveJvm.getJvmExecutable()).isNotNull();

        Path javaHome = Paths.get(System.getProperty("java.home")).normalize();
        boolean isLocalJvm =
                effectiveJvm.getJvmExecutable().toPath().normalize().startsWith(javaHome);
        assertThat(isLocalJvm).isTrue();

        verify(logger, times(1)).debug(argument.capture());

        assertThat(argument.getValue()).startsWith("Using JVM: " + System.getProperty("java.home"));
    }

    @Test
    public void shouldFailWithWrongJvmExecPath() throws Exception {
        AbstractSurefireMojoTest.Mojo mojo = new AbstractSurefireMojoTest.Mojo();
        mojo.setLogger(mock(Logger.class));
        mojo.setJvm(System.getProperty("user.dir"));

        e.expect(MojoFailureException.class);
        e.expectMessage(startsWith("Given path does not end with java executor"));

        invokeMethod(mojo, "getEffectiveJvm");
    }

    /**
     * Mocks a ToolchainManager
     */
    public static final class MockToolchainManager implements ToolchainManager {

        private final Toolchain specToolchain;
        private final Toolchain buildContextToolchain;

        public MockToolchainManager(Toolchain specToolchain, Toolchain buildContextToolchain) {
            this.specToolchain = specToolchain;
            this.buildContextToolchain = buildContextToolchain;
        }

        @Override
        public Toolchain getToolchainFromBuildContext(String type, MavenSession context) {
            return buildContextToolchain;
        }

        @Override
        public List<Toolchain> getToolchains(MavenSession session, String type, Map<String, String> requirements) {
            return specToolchain == null ? Collections.<Toolchain>emptyList() : singletonList(specToolchain);
        }
    }
}
