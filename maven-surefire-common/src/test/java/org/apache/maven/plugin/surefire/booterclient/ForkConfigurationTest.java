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

import javax.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.surefire.JdkAttributes;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.Commandline;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.ModularClasspath;
import org.apache.maven.surefire.booter.ModularClasspathConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;
import org.apache.maven.surefire.extensions.ForkNodeFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.lang.String.join;
import static java.nio.file.Files.readAllBytes;
import static java.util.Collections.singletonList;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.apache.maven.surefire.api.util.internal.StringUtils.NL;
import static org.apache.maven.surefire.booter.Classpath.emptyClasspath;
import static org.apache.maven.surefire.booter.ProcessCheckerType.ALL;
import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class ForkConfigurationTest {
    private static final StartupConfiguration STARTUP_CONFIG = new StartupConfiguration(
            "",
            new ClasspathConfiguration(true, true),
            new ClassLoaderConfiguration(true, true),
            ALL,
            Collections.<String[]>emptyList());

    private static int idx = 0;

    private File basedir;

    @BeforeEach
    public void setupDirectories() throws IOException {
        File target = new File(System.getProperty("user.dir"), "target");
        basedir = new File(target, "SUREFIRE-1136-" + ++idx);
        FileUtils.deleteDirectory(basedir);
        assertTrue(basedir.mkdirs());
    }

    @AfterEach
    public void deleteDirectories() throws IOException {
        FileUtils.deleteDirectory(basedir);
    }

    @Test
    public void testEnv() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("key1", "val1");
        env.put("key2", "val2");
        env.put("key3", "val3");
        String[] exclEnv = {"PATH"};

        String jvm = new File(new File(System.getProperty("java.home"), "bin"), "java").getCanonicalPath();
        Platform platform = new Platform().withJdkExecAttributesForTests(new JdkAttributes(jvm, false));

        ForkConfiguration config =
                new DefaultForkConfiguration(
                        emptyClasspath(),
                        basedir,
                        "",
                        basedir,
                        new Properties(),
                        "",
                        env,
                        exclEnv,
                        false,
                        1,
                        true,
                        platform,
                        new NullConsoleLogger(),
                        mock(ForkNodeFactory.class)) {

                    @Override
                    protected void resolveClasspath(
                            @Nonnull Commandline cli,
                            @Nonnull String booterThatHasMainMethod,
                            @Nonnull StartupConfiguration config,
                            @Nonnull File workingDirectory,
                            @Nonnull File dumpLogDirectory) {}
                };

        List<String[]> providerJpmsArgs = new ArrayList<>();
        providerJpmsArgs.add(new String[] {"arg2", "arg3"});

        File cpElement = getTempClasspathFile();
        List<String> cp = singletonList(cpElement.getAbsolutePath());

        ClasspathConfiguration cpConfig =
                new ClasspathConfiguration(new Classpath(cp), emptyClasspath(), emptyClasspath(), true, true);
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration(true, true);
        StartupConfiguration startup = new StartupConfiguration("cls", cpConfig, clc, ALL, providerJpmsArgs);

        org.apache.maven.surefire.shared.utils.cli.Commandline cli =
                config.createCommandLine(startup, 1, getTempDirectory());

        assertThat(cli.getEnvironmentVariables())
                .contains("key1=val1", "key2=val2", "key3=val3")
                .doesNotContain("PATH=")
                .doesNotHaveDuplicates();
    }

    @Test
    public void testEnvInterpolateForkNumber() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("FORK_ID", "${surefire.forkNumber}");
        String[] exclEnv = {"PATH"};

        String jvm = new File(new File(System.getProperty("java.home"), "bin"), "java").getCanonicalPath();
        Platform platform = new Platform().withJdkExecAttributesForTests(new JdkAttributes(jvm, false));

        ForkConfiguration config =
                new DefaultForkConfiguration(
                        emptyClasspath(),
                        basedir,
                        "",
                        basedir,
                        new Properties(),
                        "",
                        env,
                        exclEnv,
                        false,
                        2,
                        true,
                        platform,
                        new NullConsoleLogger(),
                        mock(ForkNodeFactory.class)) {

                    @Override
                    protected void resolveClasspath(
                            @Nonnull Commandline cli,
                            @Nonnull String booterThatHasMainMethod,
                            @Nonnull StartupConfiguration config,
                            @Nonnull File workingDirectory,
                            @Nonnull File dumpLogDirectory) {}
                };

        List<String[]> providerJpmsArgs = new ArrayList<>();
        providerJpmsArgs.add(new String[] {"arg2", "arg3"});

        File cpElement = getTempClasspathFile();
        List<String> cp = singletonList(cpElement.getAbsolutePath());

        ClasspathConfiguration cpConfig =
                new ClasspathConfiguration(new Classpath(cp), emptyClasspath(), emptyClasspath(), true, true);
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration(true, true);
        StartupConfiguration startup = new StartupConfiguration("cls", cpConfig, clc, ALL, providerJpmsArgs);

        org.apache.maven.surefire.shared.utils.cli.Commandline cliFork1 =
                config.createCommandLine(startup, 1, getTempDirectory());

        assertThat(cliFork1.getEnvironmentVariables())
                .contains("FORK_ID=1")
                .doesNotContain("PATH=")
                .doesNotHaveDuplicates();

        org.apache.maven.surefire.shared.utils.cli.Commandline cliFork2 =
                config.createCommandLine(startup, 2, getTempDirectory());

        assertThat(cliFork2.getEnvironmentVariables())
                .contains("FORK_ID=2")
                .doesNotContain("PATH=")
                .doesNotHaveDuplicates();
    }

    @Test
    public void testCliArgs() throws Exception {
        String jvm = new File(new File(System.getProperty("java.home"), "bin"), "java").getCanonicalPath();
        Platform platform = new Platform().withJdkExecAttributesForTests(new JdkAttributes(jvm, false));

        ModularClasspathForkConfiguration config = new ModularClasspathForkConfiguration(
                emptyClasspath(),
                basedir,
                "",
                basedir,
                new Properties(),
                "arg1",
                Collections.<String, String>emptyMap(),
                new String[0],
                false,
                1,
                true,
                platform,
                new NullConsoleLogger(),
                mock(ForkNodeFactory.class));

        assertThat(config.isDebug()).isFalse();

        List<String[]> providerJpmsArgs = new ArrayList<>();
        providerJpmsArgs.add(new String[] {"arg2", "arg3"});

        ModularClasspath modulepath = new ModularClasspath(
                "test.module", Collections.<String>emptyList(), Collections.<String>emptyList(), null, false);
        ModularClasspathConfiguration cpConfig = new ModularClasspathConfiguration(
                modulepath, emptyClasspath(), emptyClasspath(), emptyClasspath(), false, true);
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration(true, true);
        StartupConfiguration startup = new StartupConfiguration("cls", cpConfig, clc, ALL, providerJpmsArgs);

        org.apache.maven.surefire.shared.utils.cli.Commandline cli =
                config.createCommandLine(startup, 1, getTempDirectory());
        String cliAsString = cli.toString();

        assertThat(cliAsString).contains("arg1");

        // "/path/to/java arg1 @/path/to/argfile"
        int beginOfFileArg = cliAsString.indexOf('@', cliAsString.lastIndexOf("arg1"));
        assertThat(beginOfFileArg).isPositive();
        int endOfFileArg = cliAsString.indexOf(IS_OS_WINDOWS ? '"' : '\'', beginOfFileArg);
        assertThat(endOfFileArg).isPositive();
        Path argFile = Paths.get(cliAsString.substring(beginOfFileArg + 1, endOfFileArg));
        String argFileText = new String(readAllBytes(argFile));
        assertThat(argFileText).contains("arg2").contains("arg3").contains("--add-modules" + NL + "ALL-MODULE-PATH");
    }

    @Test
    public void testDebugLine() throws Exception {
        String jvm = new File(new File(System.getProperty("java.home"), "bin"), "java").getCanonicalPath();
        Platform platform = new Platform().withJdkExecAttributesForTests(new JdkAttributes(jvm, false));

        ConsoleLogger logger = mock(ConsoleLogger.class);
        ForkNodeFactory forkNodeFactory = mock(ForkNodeFactory.class);

        ForkConfiguration config =
                new DefaultForkConfiguration(
                        emptyClasspath(),
                        basedir,
                        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005",
                        basedir,
                        new Properties(),
                        "",
                        Collections.<String, String>emptyMap(),
                        new String[0],
                        true,
                        1,
                        true,
                        platform,
                        logger,
                        forkNodeFactory) {

                    @Override
                    protected void resolveClasspath(
                            @Nonnull Commandline cli,
                            @Nonnull String booterThatHasMainMethod,
                            @Nonnull StartupConfiguration config,
                            @Nonnull File workingDirectory,
                            @Nonnull File dumpLogDirectory) {}
                };

        assertThat(config.isDebug()).isTrue();

        assertThat(config.getDebugLine())
                .isEqualTo("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005");

        assertThat(config.getForkCount()).isEqualTo(1);

        assertThat(config.isReuseForks()).isTrue();

        assertThat(config.getForkNodeFactory()).isSameAs(forkNodeFactory);

        File cpElement = getTempClasspathFile();
        List<String> cp = singletonList(cpElement.getAbsolutePath());

        ClasspathConfiguration cpConfig =
                new ClasspathConfiguration(new Classpath(cp), emptyClasspath(), emptyClasspath(), true, true);
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration(true, true);
        StartupConfiguration startup = new StartupConfiguration(
                "org.apache.maven.surefire.JUnitProvider#main", cpConfig, clc, ALL, Collections.<String[]>emptyList());

        assertThat(startup.isProviderMainClass()).isTrue();

        assertThat(startup.getProviderClassName()).isEqualTo("org.apache.maven.surefire.JUnitProvider#main");

        assertThat(startup.isShadefire()).isFalse();

        org.apache.maven.surefire.shared.utils.cli.Commandline cli =
                config.createCommandLine(startup, 1, getTempDirectory());

        assertThat(cli.toString()).contains("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005");
    }

    @Test
    @SuppressWarnings({"checkstyle:methodname", "checkstyle:magicnumber"})
    public void testCreateCommandLine_UseSystemClassLoaderForkOnce_ShouldConstructManifestOnlyJar()
            throws IOException, SurefireBooterForkException {
        ForkConfiguration config = getForkConfiguration(basedir, null);
        File cpElement = getTempClasspathFile();

        List<String> cp = singletonList(cpElement.getAbsolutePath());
        ClasspathConfiguration cpConfig =
                new ClasspathConfiguration(new Classpath(cp), emptyClasspath(), emptyClasspath(), true, true);
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration(true, true);
        StartupConfiguration startup =
                new StartupConfiguration("", cpConfig, clc, ALL, Collections.<String[]>emptyList());

        org.apache.maven.surefire.shared.utils.cli.Commandline cli =
                config.createCommandLine(startup, 1, getTempDirectory());

        String line = join(" ", cli.getCommandline());
        assertTrue(line.contains("-jar"));
    }

    @Test
    public void testArglineWithNewline() throws IOException, SurefireBooterForkException {
        // SUREFIRE-657
        ForkConfiguration config = getForkConfiguration(basedir, "abc\ndef");
        File cpElement = getTempClasspathFile();

        List<String> cp = singletonList(cpElement.getAbsolutePath());
        ClasspathConfiguration cpConfig =
                new ClasspathConfiguration(new Classpath(cp), emptyClasspath(), emptyClasspath(), true, true);
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration(true, true);
        StartupConfiguration startup =
                new StartupConfiguration("", cpConfig, clc, ALL, Collections.<String[]>emptyList());

        org.apache.maven.surefire.shared.utils.cli.Commandline commandLine =
                config.createCommandLine(startup, 1, getTempDirectory());
        assertThat(commandLine.toString()).contains(IS_OS_WINDOWS ? "abc def" : "'abc' 'def'");
    }

    @Test
    public void testCurrentWorkingDirectoryPropagationIncludingForkNumberExpansion()
            throws IOException, SurefireBooterForkException {
        File cwd = new File(basedir, "fork_${surefire.forkNumber}");

        ClasspathConfiguration cpConfig =
                new ClasspathConfiguration(emptyClasspath(), emptyClasspath(), emptyClasspath(), true, true);
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration(true, true);
        StartupConfiguration startup =
                new StartupConfiguration("", cpConfig, clc, ALL, Collections.<String[]>emptyList());
        ForkConfiguration config = getForkConfiguration(cwd.getCanonicalFile());
        org.apache.maven.surefire.shared.utils.cli.Commandline commandLine =
                config.createCommandLine(startup, 1, getTempDirectory());

        File forkDirectory = new File(basedir, "fork_1");

        String shellWorkDir = commandLine.getShell().getWorkingDirectory().getCanonicalPath();
        assertEquals(shellWorkDir, forkDirectory.getCanonicalPath());
    }

    @Test
    public void testExceptionWhenCurrentDirectoryIsNotRealDirectory() throws IOException {
        File cwd = new File(basedir, "cwd.txt");
        FileUtils.touch(cwd);

        try {
            ForkConfiguration config = getForkConfiguration(cwd.getCanonicalFile());
            config.createCommandLine(STARTUP_CONFIG, 1, getTempDirectory());
        } catch (SurefireBooterForkException e) {
            // To handle issue with ~ expansion on Windows
            String absolutePath = cwd.getCanonicalPath();
            assertEquals("WorkingDirectory " + absolutePath + " exists and is not a directory", e.getMessage());
            return;
        } finally {
            assertTrue(cwd.delete());
        }

        fail();
    }

    @Test
    public void testExceptionWhenCurrentDirectoryCannotBeCreated() throws IOException {
        // NULL is invalid for JDK starting from 1.7.60
        // - https://github.com/openjdk-mirror/jdk/commit/e5389115f3634d25d101e2dcc71f120d4fd9f72f
        // ? character is invalid on Windows, seems to be imposable to create invalid directory using Java on Linux
        File cwd = new File(basedir, "?\u0000InvalidDirectoryName");

        try {
            ForkConfiguration config = getForkConfiguration(cwd.getAbsoluteFile());
            config.createCommandLine(STARTUP_CONFIG, 1, getTempDirectory());
        } catch (SurefireBooterForkException sbfe) {
            assertEquals("Cannot create workingDirectory " + cwd.getAbsolutePath(), sbfe.getMessage());
            return;
        } finally {
            FileUtils.deleteDirectory(cwd);
        }

        if (IS_OS_WINDOWS || isJavaVersionAtLeast7u60()) {
            fail();
        }
    }

    private File getTempClasspathFile() throws IOException {
        File cpElement = new File(basedir, "ForkConfigurationTest." + idx + ".file");
        FileUtils.deleteDirectory(cpElement);
        return cpElement;
    }

    static ForkConfiguration getForkConfiguration(File basedir, String argLine) throws IOException {
        File jvm = new File(new File(System.getProperty("java.home"), "bin"), "java");
        return getForkConfiguration(basedir, argLine, jvm.getAbsolutePath(), new File(".").getCanonicalFile());
    }

    private ForkConfiguration getForkConfiguration(File cwd) throws IOException {
        File jvm = new File(new File(System.getProperty("java.home"), "bin"), "java");
        return getForkConfiguration(basedir, null, jvm.getAbsolutePath(), cwd);
    }

    private static ForkConfiguration getForkConfiguration(File basedir, String argLine, String jvm, File cwd)
            throws IOException {
        Platform platform = new Platform().withJdkExecAttributesForTests(new JdkAttributes(jvm, false));
        File tmpDir = new File(new File(basedir, "target"), "surefire");
        FileUtils.deleteDirectory(tmpDir);
        assertTrue(tmpDir.mkdirs());
        return new JarManifestForkConfiguration(
                emptyClasspath(),
                tmpDir,
                null,
                cwd,
                new Properties(),
                argLine,
                Collections.<String, String>emptyMap(),
                new String[0],
                false,
                1,
                false,
                platform,
                new NullConsoleLogger(),
                mock(ForkNodeFactory.class));
    }

    /**
     * Verifies that a relative {@code additionalClasspathElement} (e.g. {@code ../classes}, which is
     * correct relative to the fork's {@code workingDirectory}) ends up at the right absolute location
     * inside the manifest JAR's {@code Class-Path}, regardless of where the manifest JAR itself is
     * stored.
     *
     * <p>Without the fix, Surefire wrote the raw relative token {@code ../classes} into the manifest,
     * where the JVM resolved it against the manifest-JAR directory rather than against the fork's
     * working directory – silently pointing at the wrong location.
     */
    @Test
    public void testRelativeClasspathElementResolvedAgainstWorkingDirectory()
            throws IOException, SurefireBooterForkException {
        // Layout:
        //   basedir/build-test-dir-1/bin/     <- workingDirectory
        //   basedir/build-test-dir-1/classes/ <- the directory we want on the classpath
        File forkDir = new File(basedir, "build-test-dir-1");
        File workingDir = new File(forkDir, "bin");
        File classesDir = new File(forkDir, "classes");
        assertTrue(workingDir.mkdirs());
        assertTrue(classesDir.mkdirs());

        // Relative element as a user would write in pom.xml <additionalClasspathElement>../classes
        // The JVM resolves -cp entries against its working directory, so ../classes from bin/ == classes/.
        File cpElement = classesDir;
        List<String> cp = singletonList(cpElement.getAbsolutePath());
        ClasspathConfiguration cpConfig =
                new ClasspathConfiguration(new Classpath(cp), emptyClasspath(), emptyClasspath(), true, true);
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration(true, true);
        StartupConfiguration startup =
                new StartupConfiguration("", cpConfig, clc, ALL, Collections.<String[]>emptyList());

        ForkConfiguration config = getForkConfiguration(workingDir.getCanonicalFile());
        org.apache.maven.surefire.shared.utils.cli.Commandline cli =
                config.createCommandLine(startup, 1, getTempDirectory());

        // The command line must use -jar (manifest-only JAR mode)
        String line = join(" ", cli.getCommandline());
        assertThat(line).contains("-jar");

        // Extract the path of the manifest JAR from the command line
        String[] parts = cli.getCommandline();
        String jarPath = null;
        for (int i = 0; i < parts.length - 1; i++) {
            if ("-jar".equals(parts[i])) {
                jarPath = parts[i + 1];
                break;
            }
        }
        assertThat(jarPath).isNotNull();

        // Read the Class-Path from the manifest
        try (JarFile jar = new JarFile(new File(jarPath))) {
            Manifest manifest = jar.getManifest();
            String classPath = manifest.getMainAttributes().getValue("Class-Path");
            assertThat(classPath).isNotNull();

            // The Class-Path entry for classesDir must, when resolved against the manifest JAR's
            // parent directory, yield the canonical path of classesDir.
            File manifestJar = new File(jarPath);
            for (String entry : classPath.split(" ")) {
                if (entry.isEmpty()) {
                    continue;
                }
                // entries are URI-encoded; decode and resolve against manifest-jar parent
                String decoded = java.net.URLDecoder.decode(entry.replace("+", "%2B"), "UTF-8");
                File resolved = new File(manifestJar.getParentFile(), decoded.replace('/', File.separatorChar));
                if (resolved.getCanonicalPath().equals(classesDir.getCanonicalPath())
                        || resolved.getCanonicalPath().equals(classesDir.getCanonicalPath() + File.separator)) {
                    return; // found – test passes
                }
            }
            fail("Class-Path in manifest JAR does not resolve to " + classesDir.getCanonicalPath()
                    + "; actual Class-Path: " + classPath);
        }
    }

    // based on http://stackoverflow.com/questions/2591083/getting-version-of-java-in-runtime
    @SuppressWarnings("checkstyle:magicnumber")
    private static boolean isJavaVersionAtLeast7u60() {
        String[] javaVersionElements =
                System.getProperty("java.runtime.version").split("\\.|_|-b");
        return Integer.parseInt(javaVersionElements[1]) >= 7 && Integer.parseInt(javaVersionElements[3]) >= 60;
    }
}
