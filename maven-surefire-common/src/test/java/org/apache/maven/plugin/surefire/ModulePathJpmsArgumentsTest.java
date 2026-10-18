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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor;
import org.codehaus.plexus.languages.java.jpms.ModuleNameSource;
import org.codehaus.plexus.languages.java.jpms.ResolvePathsResult;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModulePathJpmsArgumentsTest {

    @Test
    public void shouldExportFriendOnlyPackagesAndOpenTestPackages() {
        // what a main module requiring org.junit.jupiter.api (JUnit 6.0) gets on its module path
        Map<String, JavaModuleDescriptor> pathElements = new LinkedHashMap<>();
        pathElements.put("classes", newModule("demo").exports("demo").build());
        pathElements.put(
                "junit-jupiter-api.jar",
                newModule("org.junit.jupiter.api")
                        .exports("org.junit.jupiter.api")
                        .exports("org.junit.jupiter.api.util", singleton("org.junit.jupiter.engine"))
                        .build());
        pathElements.put(
                "junit-platform-commons.jar",
                newModule("org.junit.platform.commons")
                        .exports("org.junit.platform.commons")
                        .exports("org.junit.platform.commons.util", friends())
                        .exports("org.junit.platform.commons.logging", friends())
                        .build());
        pathElements.put(
                "opentest4j.jar",
                newModule("org.opentest4j").exports("org.opentest4j").build());
        ResolvePathsResult<String> result = resolved(
                pathElements, "classes", "junit-jupiter-api.jar", "junit-platform-commons.jar", "opentest4j.jar");

        List<String[]> args = ModulePathJpmsArguments.of(result, "demo", asList("demo", "demo.internal"));

        assertThat(args)
                .containsExactly(
                        new String[] {"--add-exports", "org.junit.jupiter.api/org.junit.jupiter.api.util=ALL-UNNAMED"},
                        new String[] {
                            "--add-exports", "org.junit.platform.commons/org.junit.platform.commons.logging=ALL-UNNAMED"
                        },
                        new String[] {
                            "--add-exports", "org.junit.platform.commons/org.junit.platform.commons.util=ALL-UNNAMED"
                        },
                        new String[] {
                            "--add-opens", "demo/demo=org.junit.jupiter.api,org.junit.platform.commons,org.opentest4j"
                        },
                        new String[] {
                            "--add-opens",
                            "demo/demo.internal=org.junit.jupiter.api,org.junit.platform.commons,org.opentest4j"
                        });
    }

    @Test
    public void shouldIgnoreClassPathEntries() {
        Map<String, JavaModuleDescriptor> pathElements = new LinkedHashMap<>();
        pathElements.put("classes", newModule("demo").build());
        pathElements.put(
                "junit-platform-commons.jar",
                newModule("org.junit.platform.commons")
                        .exports("org.junit.platform.commons.util", friends())
                        .build());
        pathElements.put("test-classes", null);
        ResolvePathsResult<String> result = resolved(pathElements, "classes");

        assertThat(ModulePathJpmsArguments.of(result, "demo", singletonList("demo")))
                .isEmpty();
    }

    @Test
    public void shouldOpenTestPackagesToAutomaticModules() {
        Map<String, JavaModuleDescriptor> pathElements = new LinkedHashMap<>();
        pathElements.put("classes", newModule("demo").build());
        pathElements.put(
                "joda-time.jar",
                JavaModuleDescriptor.newAutomaticModule("joda.time").build());
        pathElements.put("missing.jar", null);
        ResolvePathsResult<String> result = resolved(pathElements, "classes", "joda-time.jar", "missing.jar");

        assertThat(ModulePathJpmsArguments.of(result, "demo", singletonList("demo")))
                .containsExactly(new String[] {"--add-opens", "demo/demo=joda.time"});
    }

    @Test
    public void shouldNotOpenWithoutTestPackages() {
        Map<String, JavaModuleDescriptor> pathElements = new LinkedHashMap<>();
        pathElements.put("classes", newModule("demo").build());
        pathElements.put("opentest4j.jar", newModule("org.opentest4j").build());
        ResolvePathsResult<String> result = resolved(pathElements, "classes", "opentest4j.jar");

        assertThat(ModulePathJpmsArguments.of(result, "demo", emptyList())).isEmpty();
    }

    @Test
    public void shouldCopeWithMissingPaths() {
        @SuppressWarnings("unchecked")
        ResolvePathsResult<String> result = mock(ResolvePathsResult.class);
        when(result.getPathElements()).thenReturn(null);
        when(result.getModulepathElements()).thenReturn(null);

        assertThat(ModulePathJpmsArguments.of(result, "demo", singletonList("demo")))
                .isEmpty();
    }

    private static JavaModuleDescriptor.Builder newModule(String name) {
        return JavaModuleDescriptor.newModule(name);
    }

    private static HashSet<String> friends() {
        return new HashSet<>(asList("org.junit.jupiter.engine", "org.junit.platform.launcher"));
    }

    private static ResolvePathsResult<String> resolved(
            Map<String, JavaModuleDescriptor> pathElements, String... modulePath) {
        Map<String, ModuleNameSource> modulePathElements = new LinkedHashMap<>();
        for (String element : modulePath) {
            modulePathElements.put(element, ModuleNameSource.MODULEDESCRIPTOR);
        }
        @SuppressWarnings("unchecked")
        ResolvePathsResult<String> result = mock(ResolvePathsResult.class);
        when(result.getPathElements()).thenReturn(pathElements);
        when(result.getModulepathElements()).thenReturn(modulePathElements);
        return result;
    }
}
