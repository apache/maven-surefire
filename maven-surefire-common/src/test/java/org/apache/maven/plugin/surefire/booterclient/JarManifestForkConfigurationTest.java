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
package org.apache.maven.plugin.surefire.booterclient;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Path;

import org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfiguration.ClasspathElementUri;
import org.apache.maven.plugin.surefire.booterclient.output.InPluginProcessDumpSingleton;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfiguration.escapeUri;
import static org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfiguration.relativize;
import static org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfiguration.toAbsoluteUri;
import static org.apache.maven.plugin.surefire.booterclient.JarManifestForkConfiguration.toClasspathElementUri;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Files.delete;
import static org.assertj.core.util.Files.newTemporaryFolder;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Unit tests for {@link JarManifestForkConfiguration}.
 */
public class JarManifestForkConfigurationTest {
    private static final File TMP = newTemporaryFolder();

    private static File dumpDirectory;

    @BeforeAll
    public static void createSystemTemporaryDir() {
        dumpDirectory = new File(TMP, "dump");
        assertThat(dumpDirectory.mkdir()).isTrue();
    }

    @AfterAll
    public static void deleteSystemTemporaryDir() {
        delete(TMP);
    }

    @Test
    public void relativeClasspathUnixSimple() throws Exception {
        try (MockedStatic<JarManifestForkConfiguration> mocked =
                mockStatic(JarManifestForkConfiguration.class, inv -> inv.callRealMethod())) {
            Path parent = mock(Path.class, "/home/me/prj/target/surefire");
            Path classPathElement = mock(Path.class, "/home/me/.m2/repository/grp/art/1.0/art-1.0.jar");
            Path relativePath = mock(Path.class, "../../../.m2/repository/grp/art/1.0/art-1.0.jar");
            org.mockito.Mockito.when(parent.relativize(classPathElement)).thenReturn(relativePath);
            assertThat(toClasspathElementUri(parent, classPathElement, dumpDirectory, true).uri)
                    .isEqualTo("../../../.m2/repository/grp/art/1.0/art-1.0.jar");
        }
    }

    @Test
    public void relativeClasspathUnixTricky() throws Exception {
        try (MockedStatic<JarManifestForkConfiguration> mocked =
                mockStatic(JarManifestForkConfiguration.class, inv -> inv.callRealMethod())) {
            Path parent = mock(Path.class, "/home/me/prj/target/surefire");
            Path classPathElement = mock(Path.class, "/the Maven repo/grp/art/1.0/art-1.0.jar");
            Path relativePath = mock(Path.class, "../../../../../the Maven repo/grp/art/1.0/art-1.0.jar");
            org.mockito.Mockito.when(parent.relativize(classPathElement)).thenReturn(relativePath);
            assertThat(toClasspathElementUri(parent, classPathElement, dumpDirectory, true).uri)
                    .isEqualTo("../../../../../the%20Maven%20repo/grp/art/1.0/art-1.0.jar");
        }
    }

    @Test
    public void relativeClasspathWindowsSimple() throws Exception {
        try (MockedStatic<JarManifestForkConfiguration> mocked =
                mockStatic(JarManifestForkConfiguration.class, inv -> inv.callRealMethod())) {
            Path parent = mock(Path.class, "C:\\Windows\\Temp\\surefire");
            Path classPathElement = mock(Path.class, "C:\\Users\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar");
            Path relativePath = mock(Path.class, "..\\..\\..\\Users\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar");
            org.mockito.Mockito.when(parent.relativize(classPathElement)).thenReturn(relativePath);
            assertThat(toClasspathElementUri(parent, classPathElement, dumpDirectory, true).uri)
                    .isEqualTo("../../../Users/me/.m2/repository/grp/art/1.0/art-1.0.jar");
        }
    }

    @Test
    public void relativeClasspathWindowsTricky() throws Exception {
        try (MockedStatic<JarManifestForkConfiguration> mocked =
                mockStatic(JarManifestForkConfiguration.class, inv -> inv.callRealMethod())) {
            Path parent = mock(Path.class, "C:\\Windows\\Temp\\surefire");
            Path classPathElement = mock(Path.class, "C:\\Test User\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar");
            Path relativePath =
                    mock(Path.class, "..\\..\\..\\Test User\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar");
            org.mockito.Mockito.when(parent.relativize(classPathElement)).thenReturn(relativePath);
            assertThat(toClasspathElementUri(parent, classPathElement, dumpDirectory, true).uri)
                    .isEqualTo("../../../Test%20User/me/.m2/repository/grp/art/1.0/art-1.0.jar");
        }
    }

    @Test
    public void crossDriveWindows() throws Exception {
        try (MockedStatic<JarManifestForkConfiguration> mocked =
                        mockStatic(JarManifestForkConfiguration.class, inv -> inv.callRealMethod());
                MockedStatic<InPluginProcessDumpSingleton> dumpMocked =
                        mockStatic(InPluginProcessDumpSingleton.class)) {
            dumpMocked
                    .when(InPluginProcessDumpSingleton::getSingleton)
                    .thenReturn(mock(InPluginProcessDumpSingleton.class));
            Path parent = mock(Path.class, "C:\\Windows\\Temp\\surefire");
            Path classPathElement = mock(Path.class, "X:\\Users\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar");
            org.mockito.Mockito.when(classPathElement.toUri()).thenAnswer(new Answer<URI>() {
                @Override
                public URI answer(InvocationOnMock invocation) throws URISyntaxException {
                    String path = invocation.getMock().toString();
                    return new URI("file", "", "/" + path.replace('\\', '/'), null);
                }
            });
            mocked.when(() -> relativize(parent, classPathElement))
                    .thenThrow(new IllegalArgumentException("'other' has different root"));
            assertThat(toClasspathElementUri(parent, classPathElement, dumpDirectory, true).uri)
                    .isEqualTo("file:///X:/Users/me/.m2/repository/grp/art/1.0/art-1.0.jar");
        }
    }

    @Test
    public void uncWindows() throws Exception {
        assumeTrue(IS_OS_WINDOWS);
        try (MockedStatic<JarManifestForkConfiguration> mocked =
                        mockStatic(JarManifestForkConfiguration.class, inv -> inv.callRealMethod());
                MockedStatic<InPluginProcessDumpSingleton> dumpMocked =
                        mockStatic(InPluginProcessDumpSingleton.class)) {
            dumpMocked
                    .when(InPluginProcessDumpSingleton::getSingleton)
                    .thenReturn(mock(InPluginProcessDumpSingleton.class));
            Path parent = mock(Path.class, "C:\\Windows\\Temp\\surefire");
            Path classPathElement = mock(Path.class, "\\\\server\\grp\\art\\1.0\\art-1.0.jar");
            org.mockito.Mockito.when(classPathElement.toFile()).thenAnswer(new Answer<File>() {
                @Override
                public File answer(InvocationOnMock invocation) {
                    String path = invocation.getMock().toString();
                    return new File(path);
                }
            });
            mocked.when(() -> relativize(parent, classPathElement))
                    .thenThrow(new IllegalArgumentException("'other' has different root"));
            assertThat(toClasspathElementUri(parent, classPathElement, dumpDirectory, true).uri)
                    .isEqualTo("file:////server/grp/art/1.0/art-1.0.jar");
        }
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void shouldEscapeUri() throws Exception {
        assertThat(escapeUri("a", UTF_8)).isEqualTo("a");
        assertThat(escapeUri(" ", UTF_8)).isEqualTo("%20");
        assertThat(escapeUri("%", UTF_8)).isEqualTo("%25");
        assertThat(escapeUri("+", UTF_8)).isEqualTo("%2B");
        assertThat(escapeUri(",", UTF_8)).isEqualTo("%2C");
        assertThat(escapeUri("/", UTF_8)).isEqualTo("/");
        assertThat(escapeUri("7", UTF_8)).isEqualTo("7");
        assertThat(escapeUri(":", UTF_8)).isEqualTo("%3A");
        assertThat(escapeUri("@", UTF_8)).isEqualTo("%40");
        assertThat(escapeUri("A", UTF_8)).isEqualTo("A");
        assertThat(escapeUri("[", UTF_8)).isEqualTo("%5B");
        assertThat(escapeUri("\\", UTF_8)).isEqualTo("/");
        assertThat(escapeUri("]", UTF_8)).isEqualTo("%5D");
        assertThat(escapeUri("`", UTF_8)).isEqualTo("%60");
        assertThat(escapeUri("a", UTF_8)).isEqualTo("a");
        assertThat(escapeUri("{", UTF_8)).isEqualTo("%7B");
        assertThat(escapeUri("" + (char) 0xFF, UTF_8)).isEqualTo("%C3%BF");

        assertThat(escapeUri("..\\..\\..\\Test : User\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar", UTF_8))
                .isEqualTo("../../../Test%20%3A%20User/me/.m2/repository/grp/art/1.0/art-1.0.jar");

        assertThat(escapeUri(
                        "..\\..\\..\\Test : User\\me\\.m2\\repository\\grp\\art\\1.0\\art-1.0.jar",
                        Charset.defaultCharset()))
                .isEqualTo("../../../Test%20%3A%20User/me/.m2/repository/grp/art/1.0/art-1.0.jar");

        assertThat(escapeUri("../../surefire-its/target/junit-pathWithÜmlaut_1/target/surefire", UTF_8))
                .isEqualTo("../../surefire-its/target/junit-pathWith%C3%9Cmlaut_1/target/surefire");

        String source = "../../surefire-its/target/junit-pathWithÜmlaut_1/target/surefire";
        String encoded = escapeUri("../../surefire-its/target/junit-pathWithÜmlaut_1/target/surefire", UTF_8);
        String decoded = URLDecoder.decode(encoded, UTF_8.name());
        assertThat(decoded).isEqualTo(source);
    }

    @Test
    public void shouldRelativizeOnRealPlatform() {
        Path parentDir = new File(TMP, "test-parent-1").toPath();

        Path testDir = new File(TMP, "@1 test with white spaces").toPath();

        String relativeTestDir = relativize(parentDir, testDir);

        assertThat(relativeTestDir).isEqualTo(".." + File.separator + "@1 test with white spaces");
    }

    @Test
    public void shouldMakeAbsoluteUriOnRealPlatform() throws Exception {
        Path testDir = new File(TMP, "@2 test with white spaces").toPath();

        URI testDirUri = new URI(toAbsoluteUri(testDir));

        assertThat(testDirUri.getScheme()).isEqualTo("file");

        assertThat(testDirUri.getRawPath()).isEqualTo(testDir.toUri().getRawPath());
    }

    @Test
    public void shouldMakeRelativeUriOnRealPlatform() throws Exception {
        Path parentDir = new File(TMP, "test-parent-2").toPath();

        Path testDir = new File(TMP, "@3 test with white spaces").toPath();

        ClasspathElementUri testDirUriPath = toClasspathElementUri(parentDir, testDir, dumpDirectory, true);

        assertThat(testDirUriPath.uri).isEqualTo("../%403%20test%20with%20white%20spaces");
    }
}
