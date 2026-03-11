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
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.Commandline;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.plugin.surefire.util.Relocator;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.extensions.ForkNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DefaultForkConfiguration}.
 *
 * @author Tibor Digana (tibor17)
 * @since 2.21
 */
public class DefaultForkConfigurationTest {
    private Classpath booterClasspath;
    private File tempDirectory;
    private String debugLine;
    private File workingDirectory;
    private Properties modelProperties;
    private String argLine;
    private Map<String, String> environmentVariables;
    private String[] excludedEnvironmentVariables;
    private boolean debug;
    private int forkCount;
    private boolean reuseForks;
    private Platform pluginPlatform;
    private ConsoleLogger log;
    private ForkNodeFactory forkNodeFactory;

    @BeforeEach
    public void setup() {
        booterClasspath = new Classpath(singleton("provider.jar"));
        tempDirectory = new File("target/surefire");
        debugLine = "";
        workingDirectory = new File(".");
        modelProperties = new Properties();
        argLine = null;
        environmentVariables = new HashMap<>();
        excludedEnvironmentVariables = new String[0];
        debug = true;
        forkCount = 2;
        reuseForks = true;
        pluginPlatform = new Platform();
        log = mock(ConsoleLogger.class);
        forkNodeFactory = mock(ForkNodeFactory.class);
    }

    @Test
    public void shouldBeNullArgLine() throws Exception {
        DefaultForkConfiguration config =
                new DefaultForkConfiguration(
                        booterClasspath,
                        tempDirectory,
                        debugLine,
                        workingDirectory,
                        modelProperties,
                        argLine,
                        environmentVariables,
                        excludedEnvironmentVariables,
                        debug,
                        forkCount,
                        reuseForks,
                        pluginPlatform,
                        log,
                        forkNodeFactory) {

                    @Override
                    protected void resolveClasspath(
                            @Nonnull Commandline cli,
                            @Nonnull String booterThatHasMainMethod,
                            @Nonnull StartupConfiguration config,
                            @Nonnull File dumpLogDirectory) {}
                };

        String newArgLine = invokeMethod(config, "newJvmArgLine", new Class[] {int.class}, 2);
        assertThat(newArgLine).isEmpty();
    }

    @Test
    public void shouldBeEmptyArgLine() throws Exception {
        argLine = "";
        DefaultForkConfiguration config =
                new DefaultForkConfiguration(
                        booterClasspath,
                        tempDirectory,
                        debugLine,
                        workingDirectory,
                        modelProperties,
                        argLine,
                        environmentVariables,
                        excludedEnvironmentVariables,
                        debug,
                        forkCount,
                        reuseForks,
                        pluginPlatform,
                        log,
                        forkNodeFactory) {

                    @Override
                    protected void resolveClasspath(
                            @Nonnull Commandline cli,
                            @Nonnull String booterThatHasMainMethod,
                            @Nonnull StartupConfiguration config,
                            @Nonnull File dumpLogDirectory) {}
                };

        String newArgLine = invokeMethod(config, "newJvmArgLine", new Class[] {int.class}, 2);
        assertThat(newArgLine).isEmpty();
    }

    @Test
    public void shouldBeEmptyArgLineInsteadOfNewLines() throws Exception {
        argLine = "\n\r";
        DefaultForkConfiguration config =
                new DefaultForkConfiguration(
                        booterClasspath,
                        tempDirectory,
                        debugLine,
                        workingDirectory,
                        modelProperties,
                        argLine,
                        environmentVariables,
                        excludedEnvironmentVariables,
                        debug,
                        forkCount,
                        reuseForks,
                        pluginPlatform,
                        log,
                        forkNodeFactory) {

                    @Override
                    protected void resolveClasspath(
                            @Nonnull Commandline cli,
                            @Nonnull String booterThatHasMainMethod,
                            @Nonnull StartupConfiguration config,
                            @Nonnull File dumpLogDirectory) {}
                };

        String newArgLine = invokeMethod(config, "newJvmArgLine", new Class[] {int.class}, 2);
        assertThat(newArgLine).isEmpty();
    }

    @Test
    public void shouldBeWithoutEscaping() throws Exception {
        argLine = "-Dfile.encoding=UTF-8";
        DefaultForkConfiguration config =
                new DefaultForkConfiguration(
                        booterClasspath,
                        tempDirectory,
                        debugLine,
                        workingDirectory,
                        modelProperties,
                        argLine,
                        environmentVariables,
                        excludedEnvironmentVariables,
                        debug,
                        forkCount,
                        reuseForks,
                        pluginPlatform,
                        log,
                        forkNodeFactory) {

                    @Override
                    protected void resolveClasspath(
                            @Nonnull Commandline cli,
                            @Nonnull String booterThatHasMainMethod,
                            @Nonnull StartupConfiguration config,
                            @Nonnull File dumpLogDirectory) {}
                };

        String newArgLine = invokeMethod(config, "newJvmArgLine", new Class[] {int.class}, 2);
        assertThat(newArgLine).isEqualTo("-Dfile.encoding=UTF-8");
    }

    @Test
    public void shouldBeWithEscaping() throws Exception {
        modelProperties.put("encoding", "UTF-8");
        argLine = "-Dfile.encoding=@{encoding}";
        DefaultForkConfiguration config =
                new DefaultForkConfiguration(
                        booterClasspath,
                        tempDirectory,
                        debugLine,
                        workingDirectory,
                        modelProperties,
                        argLine,
                        environmentVariables,
                        excludedEnvironmentVariables,
                        debug,
                        forkCount,
                        reuseForks,
                        pluginPlatform,
                        log,
                        forkNodeFactory) {

                    @Override
                    protected void resolveClasspath(
                            @Nonnull Commandline cli,
                            @Nonnull String booterThatHasMainMethod,
                            @Nonnull StartupConfiguration config,
                            @Nonnull File dumpLogDirectory) {}
                };

        String newArgLine = invokeMethod(config, "newJvmArgLine", new Class[] {int.class}, 2);
        assertThat(newArgLine).isEqualTo("-Dfile.encoding=UTF-8");
    }

    @Test
    public void shouldBeWhitespaceInsteadOfNewLines() throws Exception {
        argLine = "a\n\rb";
        DefaultForkConfiguration config =
                new DefaultForkConfiguration(
                        booterClasspath,
                        tempDirectory,
                        debugLine,
                        workingDirectory,
                        modelProperties,
                        argLine,
                        environmentVariables,
                        excludedEnvironmentVariables,
                        debug,
                        forkCount,
                        reuseForks,
                        pluginPlatform,
                        log,
                        forkNodeFactory) {

                    @Override
                    protected void resolveClasspath(
                            @Nonnull Commandline cli,
                            @Nonnull String booterThatHasMainMethod,
                            @Nonnull StartupConfiguration config,
                            @Nonnull File dumpLogDirectory) {}
                };

        String newArgLine = invokeMethod(config, "newJvmArgLine", new Class[] {int.class}, 2);
        assertThat(newArgLine).isEqualTo("a  b");
    }

    @Test
    public void shouldEscapeThreadNumber() throws Exception {
        argLine = "-Dthread=${surefire.threadNumber}";
        DefaultForkConfiguration config =
                new DefaultForkConfiguration(
                        booterClasspath,
                        tempDirectory,
                        debugLine,
                        workingDirectory,
                        modelProperties,
                        argLine,
                        environmentVariables,
                        excludedEnvironmentVariables,
                        debug,
                        forkCount,
                        reuseForks,
                        pluginPlatform,
                        log,
                        forkNodeFactory) {

                    @Override
                    protected void resolveClasspath(
                            @Nonnull Commandline cli,
                            @Nonnull String booterThatHasMainMethod,
                            @Nonnull StartupConfiguration config,
                            @Nonnull File dumpLogDirectory) {}
                };

        String newArgLine = invokeMethod(config, "newJvmArgLine", new Class[] {int.class}, 2);
        assertThat(newArgLine).isEqualTo("-Dthread=" + forkCount);
    }

    @Test
    public void shouldEscapeForkNumber() throws Exception {
        argLine = "-Dthread=${surefire.forkNumber}";
        DefaultForkConfiguration config =
                new DefaultForkConfiguration(
                        booterClasspath,
                        tempDirectory,
                        debugLine,
                        workingDirectory,
                        modelProperties,
                        argLine,
                        environmentVariables,
                        excludedEnvironmentVariables,
                        debug,
                        forkCount,
                        reuseForks,
                        pluginPlatform,
                        log,
                        forkNodeFactory) {

                    @Override
                    protected void resolveClasspath(
                            @Nonnull Commandline cli,
                            @Nonnull String booterThatHasMainMethod,
                            @Nonnull StartupConfiguration config,
                            @Nonnull File dumpLogDirectory) {}
                };

        String newArgLine = invokeMethod(config, "newJvmArgLine", new Class[] {int.class}, 2);
        assertThat(newArgLine).isEqualTo("-Dthread=" + forkCount);
    }

    @Test
    public void shouldRelocateBooterClassWhenShadefire() throws Exception {
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration(true, true);
        ClasspathConfiguration cc = new ClasspathConfiguration(true, true);
        StartupConfiguration conf = new StartupConfiguration(
                "org.apache.maven.shadefire.surefire.MyProvider", cc, clc, null, Collections.<String[]>emptyList());
        StartupConfiguration confMock = spy(conf);

        try (MockedStatic<Relocator> relocatorMock = mockStatic(Relocator.class)) {
            relocatorMock.when(() -> Relocator.relocate(anyString())).thenCallRealMethod();

            String cls = invokeMethod(
                    DefaultForkConfiguration.class,
                    "findStartClass",
                    new Class[] {StartupConfiguration.class},
                    confMock);

            verify(confMock, times(1)).isShadefire();

            assertThat(cls).isEqualTo("org.apache.maven.shadefire.surefire.booter.ForkedBooter");
            assertThat(confMock.isShadefire()).isTrue();
        }
    }

    @Test
    public void shouldNotRelocateBooterClass() throws Exception {
        ClassLoaderConfiguration clc = new ClassLoaderConfiguration(true, true);
        ClasspathConfiguration cc = new ClasspathConfiguration(true, true);
        StartupConfiguration conf = new StartupConfiguration(
                "org.apache.maven.surefire.MyProvider", cc, clc, null, Collections.<String[]>emptyList());
        StartupConfiguration confMock = spy(conf);

        try (MockedStatic<Relocator> relocatorMock = mockStatic(Relocator.class)) {
            relocatorMock.when(() -> Relocator.relocate(anyString())).thenCallRealMethod();

            String cls = invokeMethod(
                    DefaultForkConfiguration.class,
                    "findStartClass",
                    new Class[] {StartupConfiguration.class},
                    confMock);

            verify(confMock, times(1)).isShadefire();

            assertThat(cls).isEqualTo("org.apache.maven.surefire.booter.ForkedBooter");
            assertThat(confMock.isShadefire()).isFalse();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T invokeMethod(Object target, String methodName, Class<?>[] paramTypes, Object... args)
            throws Exception {
        Class<?> clazz = target instanceof Class ? (Class<?>) target : target.getClass();
        while (clazz != null) {
            try {
                Method method = clazz.getDeclaredMethod(methodName, paramTypes);
                method.setAccessible(true);
                return (T) method.invoke(target instanceof Class ? null : target, args);
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchMethodException(methodName);
    }
}
