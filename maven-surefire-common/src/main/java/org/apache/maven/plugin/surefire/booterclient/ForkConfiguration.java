package org.apache.maven.plugin.surefire.booterclient;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.surefire.JdkAttributes;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.OutputStreamFlushableCommandline;
import org.apache.maven.surefire.booter.Classpath;
import org.apache.maven.surefire.booter.ForkedBooter;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.SurefireBooterForkException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration for forking tests.
 */
public abstract class ForkConfiguration
{
    static final String DEFAULT_PROVIDER_CLASS = ForkedBooter.class.getName();

    @Nonnull public abstract File getTempDirectory();
    @Nullable protected abstract String getDebugLine();
    @Nonnull protected abstract File getWorkingDirectory();
    @Nonnull protected abstract Properties getModelProperties();
    @Nullable protected abstract String getArgLine();
    @Nonnull protected abstract Map<String, String> getEnvironmentVariables();
    protected abstract boolean isDebug();
    protected abstract int getForkCount();
    protected abstract boolean isReuseForks();
    @Nonnull protected abstract Platform getPluginPlatform();
    @Nonnull protected abstract JdkAttributes getJdkForTests();
    @Nonnull protected abstract Classpath getBooterClasspath();

    /**
     * @param config               The startup configuration
     * @param forkNumber           index of forked JVM, to be the replacement in the argLine
     * @return CommandLine able to flush entire command going to be sent to forked JVM
     * @throws org.apache.maven.surefire.booter.SurefireBooterForkException
     *          when unable to perform the fork
     */
    @Nonnull
    public abstract OutputStreamFlushableCommandline createCommandLine( @Nonnull StartupConfiguration config,
                                                                        int forkNumber )
            throws SurefireBooterForkException;
}
