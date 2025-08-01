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
package org.apache.maven.surefire.junitplatform;

import java.lang.reflect.Method;

import org.apache.maven.surefire.api.util.ReflectionUtils;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.surefire.junitplatform.CancellationTokenAdapter.CANCELLATION_TOKEN_CLASS;

final class LauncherAdapter {

    static final Class<?> LAUNCHER_EXECUTION_REQUEST_CLASS = loadLauncherExecutionRequestClass();
    static final Method EXECUTE_METHOD_WITH_LAUNCHER_EXECUTION_REQUEST_PARAMETER =
            findExecuteMethodWithLauncherExecutionRequestParameter();

    private final Launcher delegate;
    private final CancellationTokenAdapter cancellationToken;

    LauncherAdapter(Launcher delegate, CancellationTokenAdapter cancellationToken) {
        this.delegate = delegate;
        this.cancellationToken = cancellationToken;
    }

    TestPlan discover(LauncherDiscoveryRequest discoveryRequest) {
        return delegate.discover(discoveryRequest);
    }

    void execute(LauncherDiscoveryRequest discoveryRequest, TestExecutionListener... listeners) {
        if (cancellationToken == null) {
            executeWithoutCancellationToken(discoveryRequest, listeners);
        } else {
            executeWithCancellationToken(discoveryRequest, listeners);
        }
    }

    void executeWithoutCancellationToken(
            LauncherDiscoveryRequest discoveryRequest, TestExecutionListener... listeners) {
        delegate.execute(discoveryRequest, listeners);
    }

    private void executeWithCancellationToken(
            LauncherDiscoveryRequest discoveryRequest, TestExecutionListener... listeners) {

        Method executeMethod = requireNonNull(EXECUTE_METHOD_WITH_LAUNCHER_EXECUTION_REQUEST_PARAMETER);
        Object executionRequest = createExecutionRequest(discoveryRequest, listeners);
        ReflectionUtils.invokeMethodWithArray(delegate, executeMethod, executionRequest);
    }

    private static Method findExecuteMethodWithLauncherExecutionRequestParameter() {
        if (LAUNCHER_EXECUTION_REQUEST_CLASS == null) {
            return null;
        }
        return ReflectionUtils.getMethod(Launcher.class, "execute", LAUNCHER_EXECUTION_REQUEST_CLASS);
    }

    private static Class<?> loadLauncherExecutionRequestClass() {
        return ReflectionUtils.tryLoadClass(
                Launcher.class.getClassLoader(), "org.junit.platform.launcher.LauncherExecutionRequest");
    }

    private Object createExecutionRequest(
            LauncherDiscoveryRequest discoveryRequest, TestExecutionListener[] listeners) {

        Object builder = createLauncherExecutionRequestBuilder(discoveryRequest);
        ReflectionUtils.invokeSetter(builder, "listeners", TestExecutionListener[].class, listeners);
        ReflectionUtils.invokeSetter(
                builder, "cancellationToken", CANCELLATION_TOKEN_CLASS, cancellationToken.getDelegate());
        return ReflectionUtils.invokeGetter(builder, "build");
    }

    private static Object createLauncherExecutionRequestBuilder(LauncherDiscoveryRequest discoveryRequest) {
        ClassLoader classLoader = discoveryRequest.getClass().getClassLoader();
        Class<?> builderClass = ReflectionUtils.loadClass(
                classLoader, "org.junit.platform.launcher.core.LauncherExecutionRequestBuilder");
        Class<?>[] parameterTypes = {LauncherDiscoveryRequest.class};
        Object[] parameters = {discoveryRequest};
        return ReflectionUtils.invokeStaticMethod(builderClass, "request", parameterTypes, parameters);
    }
}
