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
import org.junit.platform.engine.ExecutionRequest;

class CancellationTokenAdapter {

    static final Class<?> CANCELLATION_TOKEN_CLASS = ReflectionUtils.tryLoadClass(
            ExecutionRequest.class.getClassLoader(), "org.junit.platform.engine.CancellationToken");

    static CancellationTokenAdapter tryCreate() {
        if (CANCELLATION_TOKEN_CLASS == null) {
            return null;
        }
        Method createMethod = ReflectionUtils.getMethod(CANCELLATION_TOKEN_CLASS, "create");
        Object token = ReflectionUtils.invokeMethodWithArray(null, createMethod);
        return new CancellationTokenAdapter(token);
    }

    private final Object delegate;

    private CancellationTokenAdapter(Object delegate) {
        this.delegate = delegate;
    }

    Object getDelegate() {
        return delegate;
    }

    void cancel() {
        Method cancelMethod = ReflectionUtils.getMethod(CANCELLATION_TOKEN_CLASS, "cancel");
        ReflectionUtils.invokeMethodWithArray(delegate, cancelMethod);
    }
}
