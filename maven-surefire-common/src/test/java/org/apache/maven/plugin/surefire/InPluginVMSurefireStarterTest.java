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
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import org.apache.maven.plugin.surefire.booterclient.Platform;
import org.apache.maven.plugin.surefire.extensions.SurefireConsoleOutputReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessTestsetInfoReporter;
import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.report.ReporterFactoryOptions;
import org.apache.maven.surefire.api.util.DefaultScanResult;
import org.apache.maven.surefire.api.util.SurefireReflectionException;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.IsolatedClassLoader;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link InPluginVMSurefireStarter}.
 */
class InPluginVMSurefireStarterTest {

    @Test
    void shouldCloseInProcessClassLoaderAfterRun() throws Exception {
        TrackingClassLoader classLoader = new TrackingClassLoader(true);
        InPluginVMSurefireStarter starter = newStarter(classLoader, shutdownPlatform());

        starter.runSuitesInProcess(new DefaultScanResult(Collections.emptyList()));

        assertThat(classLoader.isClosed()).isTrue();
    }

    @Test
    void shouldCloseInProcessClassLoaderEvenWhenProviderSetupFails() {
        // childDelegation=false with no URLs and no parent means every loadClass() call fails,
        // forcing CommonReflector's constructor to throw before the provider is ever invoked
        TrackingClassLoader classLoader = new TrackingClassLoader(false);
        InPluginVMSurefireStarter starter = newStarter(classLoader, new Platform());

        assertThatThrownBy(() -> starter.runSuitesInProcess(new DefaultScanResult(Collections.emptyList())))
                .isInstanceOf(SurefireReflectionException.class);

        assertThat(classLoader.isClosed()).isTrue();
    }

    private static InPluginVMSurefireStarter newStarter(ClassLoader testClassLoader, Platform platform) {
        StartupConfiguration startupConfig = mock(StartupConfiguration.class);
        when(startupConfig.getClasspathConfiguration()).thenReturn(new TrackingClasspathConfiguration(testClassLoader));

        ProviderConfiguration providerConfig = mock(ProviderConfiguration.class);
        when(providerConfig.getProviderProperties()).thenReturn(new HashMap<>());

        return new InPluginVMSurefireStarter(
                startupConfig, providerConfig, startupReportConfiguration(), mock(ConsoleLogger.class), platform);
    }

    private static Platform shutdownPlatform() {
        Platform platform = new Platform();
        platform.setShutdownState();
        return platform;
    }

    private static StartupReportConfiguration startupReportConfiguration() {
        File reportsDirectory = new File(System.getProperty("user.dir"), "target/InPluginVMSurefireStarterTest");
        return new StartupReportConfiguration(
                true,
                true,
                "PLAIN",
                false,
                reportsDirectory,
                false,
                null,
                null,
                false,
                1,
                null,
                null,
                false,
                true,
                true,
                false,
                new SurefireStatelessReporter(),
                new SurefireConsoleOutputReporter(),
                new SurefireStatelessTestsetInfoReporter(),
                new ReporterFactoryOptions());
    }

    private static final class TrackingClassLoader extends IsolatedClassLoader {
        private volatile boolean closed;

        TrackingClassLoader(boolean childDelegation) {
            super(childDelegation ? ClassLoader.getSystemClassLoader() : null, childDelegation, "test");
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        boolean isClosed() {
            return closed;
        }
    }

    private static final class TrackingClasspathConfiguration extends ClasspathConfiguration {
        private final ClassLoader classLoader;

        TrackingClasspathConfiguration(ClassLoader classLoader) {
            super(false, true);
            this.classLoader = classLoader;
        }

        @Override
        public ClassLoader createMergedClassLoader() {
            return classLoader;
        }
    }
}
