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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static java.io.File.separator;
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

    /**
     * Covers {@link SystemUtils#pidOf(Process)}.
     */
    @Nested
    class PidOfTest {

        @Test
        void returnsNullForNullProcess() {
            assertThat(SystemUtils.pidOf(null)).isNull();
        }

        @Test
        void returnsOwnPidWhenProcessHasNoChild() throws Exception {
            assumeTrue(JAVA_RECENT.atLeast(JAVA_9), "Process#pid() needs Java 9+");
            Process process = startSleepDirectly();
            try {
                assertThat(SystemUtils.pidOf(process)).isNotNull();
            } finally {
                process.destroyForcibly();
            }
        }

        @Test
        void resolvesTheChildWhenStartedThroughAShellWrapper() throws Exception {
            assumeTrue(JAVA_RECENT.atLeast(JAVA_9), "Process#pid() needs Java 9+");
            assumeTrue(!IS_OS_WINDOWS, "needs /bin/sh");
            // "cd ... && java ..." fork launches observed in MSUREFIRE-3549 have this same shape: the shell
            // Surefire starts stays alive as the parent of the actual forked JVM. "& wait" guarantees the shell
            // stays alive as the parent for as long as the test needs, regardless of a given /bin/sh's tail-call
            // optimization for a single simple command (which would otherwise exec-replace the shell itself).
            Process shell = new ProcessBuilder("/bin/sh", "-c", "sleep 30 & wait").start();
            try {
                long shellOwnPid = rawPid(shell);
                Long resolved = awaitChildOf(shellOwnPid);
                assertThat(resolved)
                        .as("pidOf should resolve to the sleep child, not the sh wrapper")
                        .isNotNull()
                        .isNotEqualTo(shellOwnPid);
                assertThat(SystemUtils.pidOf(shell)).isEqualTo(resolved);
            } finally {
                shell.destroyForcibly();
            }
        }

        private Process startSleepDirectly() throws IOException {
            return IS_OS_WINDOWS
                    ? new ProcessBuilder("ping", "-n", "30", "127.0.0.1").start()
                    : new ProcessBuilder("sleep", "30").start();
        }

        /**
         * @return the raw PID {@code Process#pid()} reports, bypassing {@link SystemUtils#pidOf(Process)}'s own
         *         child resolution, so the test has a ground truth to compare against
         */
        private long rawPid(Process process) throws ReflectiveOperationException {
            return (Long) Process.class.getMethod("pid").invoke(process);
        }

        /**
         * Polls {@code ps --ppid parentPid} until it reports exactly one child, or the timeout elapses.
         */
        private Long awaitChildOf(long parentPid) throws IOException, InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (System.nanoTime() < deadline) {
                String children = childrenOf(parentPid);
                if (!children.isEmpty()) {
                    return Long.parseLong(children.trim().split("\\s+")[0]);
                }
                Thread.sleep(50L);
            }
            return null;
        }

        private String childrenOf(long parentPid) throws IOException, InterruptedException {
            // pgrep -P is portable across the Linux (procps) and macOS (BSD) pgrep implementations;
            // unlike "ps --ppid", which is GNU/procps-only and absent on macOS.
            Process pgrep = new ProcessBuilder("pgrep", "-P", Long.toString(parentPid)).start();
            pgrep.waitFor(5, TimeUnit.SECONDS);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(pgrep.getInputStream()))) {
                return reader.lines().collect(Collectors.joining(" "));
            }
        }
    }
}
