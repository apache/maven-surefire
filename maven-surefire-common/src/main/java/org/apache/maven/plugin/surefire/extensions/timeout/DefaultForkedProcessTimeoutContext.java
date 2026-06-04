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
package org.apache.maven.plugin.surefire.extensions.timeout;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.extensions.ForkedProcessTimeoutContext;

/**
 * Default immutable {@link ForkedProcessTimeoutContext} implementation.
 */
public final class DefaultForkedProcessTimeoutContext implements ForkedProcessTimeoutContext {

    private final long pid;
    private final int forkNumber;
    private final File javaExecutable;
    private final File reportsDirectory;
    private final int timeoutSeconds;
    private final ConsoleLogger logger;
    private final Map<String, String> extensionContext;

    public DefaultForkedProcessTimeoutContext(
            long pid,
            int forkNumber,
            File javaExecutable,
            File reportsDirectory,
            int timeoutSeconds,
            ConsoleLogger logger) {
        this(pid, forkNumber, javaExecutable, reportsDirectory, timeoutSeconds, logger, null);
    }

    public DefaultForkedProcessTimeoutContext(
            long pid,
            int forkNumber,
            File javaExecutable,
            File reportsDirectory,
            int timeoutSeconds,
            ConsoleLogger logger,
            Map<String, String> extensionContext) {
        this.pid = pid;
        this.forkNumber = forkNumber;
        this.javaExecutable = javaExecutable;
        this.reportsDirectory = reportsDirectory;
        this.timeoutSeconds = timeoutSeconds;
        this.logger = logger;
        this.extensionContext = extensionContext == null || extensionContext.isEmpty()
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(extensionContext));
    }

    @Override
    public long getPid() {
        return pid;
    }

    @Override
    public int getForkNumber() {
        return forkNumber;
    }

    @Override
    public File getJavaExecutable() {
        return javaExecutable;
    }

    @Override
    public File getReportsDirectory() {
        return reportsDirectory;
    }

    @Override
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public ConsoleLogger getConsoleLogger() {
        return logger;
    }

    @Override
    public Map<String, String> getExtensionContext() {
        return extensionContext;
    }
}
