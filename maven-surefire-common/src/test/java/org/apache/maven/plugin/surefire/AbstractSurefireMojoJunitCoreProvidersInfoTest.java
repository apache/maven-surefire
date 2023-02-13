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

import java.util.Arrays;
import java.util.Collection;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.surefire.providerapi.ProviderInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Testing JUnitCoreProviderInfo applicable behavior.
 */
@RunWith(Parameterized.class)
public class AbstractSurefireMojoJunitCoreProvidersInfoTest {
    private final Artifact junitArtifact;
    private final Artifact junitDepArtifact;
    private final boolean isParallel;
    private final boolean hasGroups;

    private final boolean isApplicable;

    @Parameterized.Parameters(name = "{index}: junit={0}, junitDep={1}, parallel={2}, groups={3} then isApplicable={4}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // junit and junitDep are null
            {null, null, false, false, false},
            {null, null, true, false, false},
            {null, null, false, true, false},
            {null, null, true, true, false},

            // only junit artifact
            // without parallel and groups
            {"4.5", null, false, false, false},
            {"4.7", null, false, false, false},

            // with parallel
            {"4.5", null, true, false, false},
            {"4.7", null, true, false, true},

            // with groups
            {"4.5", null, false, true, false},
            {"4.7", null, false, true, true},

            // only junitDep artifact
            // without parallel and groups
            {null, "4.5", false, false, false},
            {null, "4.7", false, false, false},

            // with parallel
            {null, "4.5", true, false, false},
            {null, "4.7", true, false, true},

            // with groups
            {null, "4.5", false, true, false},
            {null, "4.7", false, true, true}
        });
    }

    public AbstractSurefireMojoJunitCoreProvidersInfoTest(
            String junitVersion, String junitDepVersion, boolean isParallel, boolean hasGroups, boolean isApplicable) {
        this.junitArtifact = junitVersion != null ? aArtifact(junitVersion) : null;
        this.junitDepArtifact = junitDepVersion != null ? aArtifact(junitDepVersion) : null;
        this.isParallel = isParallel;
        this.hasGroups = hasGroups;
        this.isApplicable = isApplicable;
    }

    private Artifact aArtifact(String version) {
        return new DefaultArtifact("test", "test", version, "test", "jar", "", null);
    }

    @Test
    public void test() {
        AbstractSurefireMojo mojo = spy(AbstractSurefireMojo.class);

        when(mojo.isAnyConcurrencySelected()).thenReturn(isParallel);
        when(mojo.isAnyGroupsSelected()).thenReturn(hasGroups);

        ProviderInfo providerInfo = mojo.new JUnitCoreProviderInfo(junitArtifact, junitDepArtifact);

        assertThat(providerInfo.isApplicable()).isEqualTo(isApplicable);
    }
}
