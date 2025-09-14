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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.surefire.providerapi.ProviderInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Testing providerInfo applicable behavior.
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractSurefireMojoProvidersInfoTest {

    @Spy
    private AbstractSurefireMojo mojo;

    @Test
    public void defaultProviderAreAlwaysAvailable() {
        ProviderInfo providerInfo = mojo.new JUnit3ProviderInfo();
        assertThat(providerInfo.isApplicable()).isTrue();
    }

    @Test
    public void dynamicProviderAreAlwaysApplicable() {
        ProviderInfo providerInfo = mojo.new DynamicProviderInfo("test");
        assertThat(providerInfo.isApplicable()).isTrue();
    }

    @Test
    public void testNgProviderApplicable() {
        Artifact testNg = mock(Artifact.class);
        ProviderInfo providerInfo = mojo.new TestNgProviderInfo(testNg);

        assertThat(providerInfo.isApplicable()).isTrue();

        // no interaction with artifact only reference are checked
        verifyNoMoreInteractions(testNg);
    }

    @Test
    public void testNgProviderNotApplicable() {
        ProviderInfo providerInfo = mojo.new TestNgProviderInfo(null);
        assertThat(providerInfo.isApplicable()).isFalse();
    }

    // surefire-junit-platform

    @Test
    public void jUnitPlatformProviderApplicable() {
        Artifact junitPlatform = mock(Artifact.class);
        ProviderInfo providerInfo =
                mojo.new JUnitPlatformProviderInfo(null, junitPlatform, aTestClassPath(), null, null);

        assertThat(providerInfo.isApplicable()).isTrue();

        // no interaction with artifact only reference are checked
        verifyNoMoreInteractions(junitPlatform);
    }

    @Test
    public void jUnitPlatformProviderNotApplicable() {
        ProviderInfo providerInfo = mojo.new JUnitPlatformProviderInfo(null, null, aTestClassPath(), null, null);
        assertThat(providerInfo.isApplicable()).isFalse();
    }

    @Test
    public void jUnitPlatformProviderNotApplicableForPlatformRunner() {
        // not applicable if junit-platform-runner is on classpath
        Artifact junitPlatformRunnerArtifact = mock(Artifact.class);
        ProviderInfo providerInfo =
                mojo.new JUnitPlatformProviderInfo(junitPlatformRunnerArtifact, null, aTestClassPath(), null, null);
        assertThat(providerInfo.isApplicable()).isFalse();

        // no interaction with artifact only reference are checked
        verifyNoMoreInteractions(junitPlatformRunnerArtifact);
    }

    // surefire-junit4

    @Test
    public void jUnit4ProviderNullArtifacts() {
        ProviderInfo providerInfo = mojo.new JUnit4ProviderInfo(null, null);
        assertThat(providerInfo.isApplicable()).isFalse();
    }

    @Test
    public void jUnit4ProviderOnlyJunitDepArtifact() {
        Artifact junitDep = mock(Artifact.class);
        ProviderInfo providerInfo = mojo.new JUnit4ProviderInfo(null, junitDep);

        assertThat(providerInfo.isApplicable()).isTrue();

        // no interaction with artifact only reference are checked
        verifyNoMoreInteractions(junitDep);
    }

    @Test
    public void jUnit4ProviderJunit3WithJDepArtifact() {
        Artifact junit = aArtifact("3.8.1");
        Artifact junitDep = mock(Artifact.class);

        ProviderInfo providerInfo = mojo.new JUnit4ProviderInfo(junit, junitDep);

        // ??? only existing for junitDep in any version is checked
        assertThat(providerInfo.isApplicable()).isTrue();

        // no interaction with artifact only reference are checked
        verifyNoMoreInteractions(junitDep);
    }

    @Test
    public void jUnit4ProviderJunit3AsDependencyArtifact() {
        Artifact junit = aArtifact("3.8.1");
        ProviderInfo providerInfo = mojo.new JUnit4ProviderInfo(junit, null);
        assertThat(providerInfo.isApplicable()).isFalse();
    }

    @Test
    public void jUnit4ProviderJunit45AsDependencyArtifact() {
        Artifact junit = aArtifact("4.5");
        ProviderInfo providerInfo = mojo.new JUnit4ProviderInfo(junit, null);
        assertThat(providerInfo.isApplicable()).isTrue();
    }

    @Test
    public void jUnit4ProviderJunit47AsDependencyArtifact() {
        Artifact junit = aArtifact("4.7");
        ProviderInfo providerInfo = mojo.new JUnit4ProviderInfo(junit, null);
        // ??? it is ok for 4.7 ???
        assertThat(providerInfo.isApplicable()).isTrue();
    }

    private TestClassPath aTestClassPath() {
        return new TestClassPath(null, null, null, null);
    }

    private Artifact aArtifact(String version) {
        return new DefaultArtifact("test", "test", version, "test", "jar", "", null);
    }
}
