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
package org.apache.maven.surefire.booter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static java.io.File.separator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.maven.surefire.shared.lang3.JavaVersion.JAVA_9;
import static org.apache.maven.surefire.shared.lang3.JavaVersion.JAVA_RECENT;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_FREE_BSD;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_NET_BSD;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_OPEN_BSD;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

/**
 * Test of {@link SystemUtils}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.20.1
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SystemUtilsTest {
    /**
     *
     */
    @Nested
    class PlainUnitTests {
        @Test
        public void shouldParseProprietaryReleaseFile() throws IOException {
            String classes = new File(".").getCanonicalPath() + separator + "target" + separator + "test-classes";

            File path = new File(classes, "jdk8-IBM" + separator + "bin" + separator + "java");
            assertThat(SystemUtils.isJava9AtLeast(path.getAbsolutePath())).isFalse();

            path = new File(classes, "jdk8-oracle" + separator + "bin" + separator + "java");
            assertThat(SystemUtils.isJava9AtLeast(path.getAbsolutePath())).isFalse();

            path = new File(classes, "jdk9-oracle" + separator + "bin" + separator + "java");
            assertThat(SystemUtils.isJava9AtLeast(path.getAbsolutePath())).isTrue();
        }

        @Test
        public void shouldHaveJavaPath() {
            String javaPath = System.getProperty("java.home") + separator + "bin" + separator + "java";
            assertThat(SystemUtils.endsWithJavaPath(javaPath)).isTrue();
        }

        @Test
        public void shouldNotHaveJavaPath() {
            assertThat(SystemUtils.endsWithJavaPath("/jdk")).isFalse();
        }

        @Test
        public void shouldNotExtractJdkHomeFromJavaExec() {
            File pathToJdk = SystemUtils.toJdkHomeFromJvmExec("/jdk/binx/java");
            assertThat(pathToJdk).isNull();
        }

        @Test
        public void shouldExtractJdkHomeFromJavaExec() {
            File pathToJdk = SystemUtils.toJdkHomeFromJvmExec("/jdk/bin/java");
            assertThat(pathToJdk).isEqualTo(new File("/jdk").getAbsoluteFile());
        }

        @Test
        public void shouldNotExtractJdkHomeFromJreExec() throws IOException {
            String classes = new File(".").getCanonicalPath() + separator + "target" + separator + "test-classes";
            File jdk = new File(classes, "jdk");
            String pathToJreExec = jdk.getAbsolutePath() + separator + "jre" + separator + "binx" + separator + "java";
            File pathToJdk = SystemUtils.toJdkHomeFromJvmExec(pathToJreExec);
            assertThat(pathToJdk).isNull();
        }

        @Test
        public void shouldExtractJdkHomeFromJreExec() throws IOException {
            String classes = new File(".").getCanonicalPath() + separator + "target" + separator + "test-classes";
            File jdk = new File(classes, "jdk");
            String pathToJreExec = jdk.getAbsolutePath() + separator + "jre" + separator + "bin" + separator + "java";
            File pathToJdk = SystemUtils.toJdkHomeFromJvmExec(pathToJreExec);
            assertThat(pathToJdk).isEqualTo(jdk);
        }

        @Test
        public void shouldExtractJdkHomeFromJre() {
            File pathToJdk = SystemUtils.toJdkHomeFromJre("/jdk/jre");
            assertThat(pathToJdk).isEqualTo(new File("/jdk").getAbsoluteFile());
        }

        @Test
        public void shouldExtractJdkHomeFromJdk() {
            File pathToJdk = SystemUtils.toJdkHomeFromJre("/jdk/");
            assertThat(pathToJdk).isEqualTo(new File("/jdk").getAbsoluteFile());
        }

        @Test
        public void shouldExtractJdkHomeFromRealPath() {
            File pathToJdk = SystemUtils.toJdkHomeFromJre();

            if (JAVA_RECENT.atLeast(JAVA_9)) {
                File realJdkHome = new File(System.getProperty("java.home")).getAbsoluteFile();
                assertThat(realJdkHome).isDirectory();
                assertThat(realJdkHome.getName()).isNotEqualTo("jre");
                assertThat(pathToJdk).isEqualTo(realJdkHome);
            } else {
                File realJreHome = new File(System.getProperty("java.home")).getAbsoluteFile();
                assertThat(realJreHome).isDirectory();
                assertThat(realJreHome.getName()).isEqualTo("jre");
                File realJdkHome = realJreHome.getParentFile();
                assertThat(pathToJdk).isEqualTo(realJdkHome);
            }
        }

        @Test
        public void shouldBeJavaVersion() {
            assertThat(SystemUtils.isJava9AtLeast((BigDecimal) null)).isFalse();
            assertThat(SystemUtils.isJava9AtLeast(new BigDecimal("1.8"))).isFalse();
            assertThat(SystemUtils.isJava9AtLeast(new BigDecimal(9))).isTrue();
        }

        @Test
        public void shouldBePlatformClassLoader() {
            ClassLoader cl = SystemUtils.platformClassLoader();
            if (JAVA_RECENT.atLeast(JAVA_9)) {
                assertThat(cl).isNotNull();
            } else {
                assertThat(cl).isNull();
            }
        }

        @Test
        public void shouldNotFindClassLoader() {
            ClassLoader cl = SystemUtils.reflectClassLoader(getClass(), "_getPlatformClassLoader_");
            assertThat(cl).isNull();
        }

        @Test
        public void shouldFindClassLoader() {
            ClassLoader cl = SystemUtils.reflectClassLoader(SystemUtilsTest.class, "getPlatformClassLoader");
            assertThat(cl).isSameAs(ClassLoader.getSystemClassLoader());
        }

        @Test
        public void shouldBePidOnJigsaw() {
            assumeTrue(JAVA_RECENT.atLeast(JAVA_9));

            Long actualPid = SystemUtils.pidOnJava9();
            String expectedPid =
                    ManagementFactory.getRuntimeMXBean().getName().split("@")[0].trim();

            assertThat(actualPid + "").isEqualTo(expectedPid);
        }

        @Test
        public void shouldBePidStatusOnLinux() throws Exception {
            assumeTrue(IS_OS_LINUX);

            Long actualPid = SystemUtils.pidStatusOnLinux();
            String expectedPid =
                    ManagementFactory.getRuntimeMXBean().getName().split("@")[0].trim();

            assertThat(actualPid + "").isEqualTo(expectedPid);
        }

        @Test
        public void shouldBeMockPidStatusOnLinux() throws Exception {
            String root = new File(System.getProperty("user.dir"), "target/test-classes").getAbsolutePath();
            Long actualPid = SystemUtils.pidStatusOnLinux(root);
            assertThat(actualPid).isEqualTo(48982L);
        }

        @Test
        public void shouldBePidStatusOnBSD() throws Exception {
            assumeTrue(IS_OS_FREE_BSD || IS_OS_NET_BSD || IS_OS_OPEN_BSD);

            Long actualPid = SystemUtils.pidStatusOnBSD();
            String expectedPid =
                    ManagementFactory.getRuntimeMXBean().getName().split("@")[0].trim();

            assertThat(actualPid + "").isEqualTo(expectedPid);
        }

        @Test
        public void shouldBeMockPidStatusOnBSD() throws Exception {
            String root = new File(System.getProperty("user.dir"), "target/test-classes").getAbsolutePath();
            Long actualPid = SystemUtils.pidStatusOnBSD(root);
            assertThat(actualPid).isEqualTo(60424L);
        }

        @Test
        public void shouldBePidOnJMX() {
            Long actualPid = SystemUtils.pidOnJMX();
            String expectedPid =
                    ManagementFactory.getRuntimeMXBean().getName().split("@")[0].trim();

            assertThat(actualPid + "").isEqualTo(expectedPid);
        }

        @Test
        public void shouldBePid() {
            Long actualPid = SystemUtils.pid();
            String expectedPid =
                    ManagementFactory.getRuntimeMXBean().getName().split("@")[0].trim();

            assertThat(actualPid + "").isEqualTo(expectedPid);
        }

        @Test
        public void shouldFindJvmStartedByShell() throws Exception {
            assumeTrue(JAVA_RECENT.atLeast(JAVA_9) && !IS_OS_WINDOWS);

            // "; true" stops the shell from turning into java, so java runs as its child
            Process shell = new ProcessBuilder(
                            "/bin/sh",
                            "-c",
                            "'" + java() + "' -cp '" + classPath() + "' '" + Sleeper.class.getName() + "'; true")
                    .start();
            try {
                awaitReady(shell);
                Long shellPid = SystemUtils.pidOf(shell);

                assertThat(SystemUtils.forkedJvmPidOf(shell, "java"))
                        .isNotNull()
                        .isNotEqualTo(shellPid);
                assertThat(SystemUtils.forkedJvmPidOf(shell, "not-a-jvm")).isEqualTo(shellPid);
            } finally {
                stop(shell);
            }
        }

        @Test
        public void shouldKeepPidOfJvmEvenWithJvmChild() throws Exception {
            assumeTrue(JAVA_RECENT.atLeast(JAVA_9));

            Process jvm = new ProcessBuilder(java(), "-cp", classPath(), Sleeper.class.getName(), "with-child").start();
            try {
                awaitReady(jvm);

                assertThat(SystemUtils.forkedJvmPidOf(jvm, "java")).isEqualTo(SystemUtils.pidOf(jvm));
            } finally {
                stop(jvm);
            }
        }

        @Test
        public void shouldFallBackWhenJvmNameUnknown() throws Exception {
            assertThat(SystemUtils.forkedJvmPidOf(null, "java")).isNull();
            assumeTrue(JAVA_RECENT.atLeast(JAVA_9));

            Process jvm = new ProcessBuilder(java(), "-cp", classPath(), Sleeper.class.getName()).start();
            try {
                awaitReady(jvm);

                assertThat(SystemUtils.forkedJvmPidOf(jvm, null)).isEqualTo(SystemUtils.pidOf(jvm));
            } finally {
                stop(jvm);
            }
        }
    }

    /**
     * Tiny JVM for the {@code forkedJvmPidOf} tests: prints "ready" and waits until its stdin closes.
     * With an argument, it first starts another one as its child. Uses only JDK classes, it runs in its own JVM.
     */
    public static final class Sleeper {
        public static void main(String[] args) throws Exception {
            if (args.length > 0) {
                String java = System.getProperty("java.home") + separator + "bin" + separator + "java";
                Process child = new ProcessBuilder(
                                java, "-cp", System.getProperty("java.class.path"), Sleeper.class.getName())
                        .start();
                new BufferedReader(new InputStreamReader(child.getInputStream(), UTF_8)).readLine();
            }
            System.out.println("ready");
            System.out.flush();
            while (System.in.read() != -1) {
                // run until stdin closes
            }
        }
    }

    private static String java() {
        return System.getProperty("java.home") + separator + "bin" + separator + "java";
    }

    private static String classPath() throws URISyntaxException {
        return new File(Sleeper.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI())
                .getPath();
    }

    private static void awaitReady(Process process) throws IOException {
        BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8));
        assertThat(out.readLine()).isEqualTo("ready");
    }

    private static void stop(Process process) throws Exception {
        process.getOutputStream().close();
        if (!process.waitFor(10, SECONDS)) {
            process.destroyForcibly();
        }
    }

    /**
     *
     */
    @Nested
    class MockTest {

        @Test
        public void shouldBeDifferentJdk9() {
            testIsJava9AtLeast(new File(System.getProperty("java.home")));
        }

        @Test
        public void shouldBeSameJdk9() {
            if (!JAVA_RECENT.atLeast(JAVA_9)) {
                testIsJava9AtLeast(new File(System.getProperty("java.home")).getParentFile());
            }
        }

        private void testIsJava9AtLeast(File pathInJdk) {
            File path = new File(pathInJdk, "bin" + separator + "java");

            try (MockedStatic<SystemUtils> mocked = mockStatic(SystemUtils.class)) {
                mocked.when(() -> SystemUtils.isJava9AtLeast(anyString())).thenCallRealMethod();

                mocked.when(() -> SystemUtils.toJdkHomeFromJvmExec(anyString())).thenCallRealMethod();

                mocked.when(() -> SystemUtils.toJdkHomeFromJre()).thenCallRealMethod();

                mocked.when(() -> SystemUtils.toJdkHomeFromJre(anyString())).thenCallRealMethod();

                mocked.when(() -> SystemUtils.isBuiltInJava9AtLeast()).thenCallRealMethod();

                mocked.when(() -> SystemUtils.toJdkVersionFromReleaseFile(any(File.class)))
                        .thenCallRealMethod();

                mocked.when(() -> SystemUtils.isJava9AtLeast(any(BigDecimal.class)))
                        .thenCallRealMethod();

                if (JAVA_RECENT.atLeast(JAVA_9)) {
                    assertThat(SystemUtils.isJava9AtLeast(path.getAbsolutePath()))
                            .isTrue();
                } else {
                    assertThat(SystemUtils.isJava9AtLeast(path.getAbsolutePath()))
                            .isFalse();
                }

                mocked.verify(() -> SystemUtils.toJdkVersionFromReleaseFile(any(File.class)), times(0));

                mocked.verify(() -> SystemUtils.isBuiltInJava9AtLeast(), times(1));
            }
        }
    }

    @SuppressWarnings("unused")
    public static ClassLoader getPlatformClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }
}
