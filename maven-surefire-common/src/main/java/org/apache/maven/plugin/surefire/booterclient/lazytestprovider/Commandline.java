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
package org.apache.maven.plugin.surefire.booterclient.lazytestprovider;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A {@link org.apache.maven.surefire.shared.utils.cli.Commandline} implementation.
 *
 * @author Andreas Gudian
 */
public class Commandline extends org.apache.maven.surefire.shared.utils.cli.Commandline {
    private final Collection<String> excludedEnvironmentVariables;
    private final Set<String> addedEnvironmentVariables;

    /**
     * for testing purposes only
     */
    public Commandline() {
        this(new String[0]);
    }

    public Commandline(String[] excludedEnvironmentVariables) {
        this.excludedEnvironmentVariables = new ConcurrentLinkedDeque<>();
        this.addedEnvironmentVariables = new HashSet<>();
        Collections.addAll(this.excludedEnvironmentVariables, excludedEnvironmentVariables);
    }

    @Override
    public void addEnvironment(String name, String value) {
        super.addEnvironment(name, value);
        addedEnvironmentVariables.add(name);
    }

    @Override
    public String[] getEnvironmentVariables() {
        String[] envs = super.getEnvironmentVariables();
        Set<String> result = new HashSet<>(Arrays.asList(envs));
        result.addAll(addedEnvironmentVariables);
        return excludedEnvironmentVariables.isEmpty()
                ? result.toArray(new String[0])
                : result.stream()
                        .filter(env -> {
                            String varName =
                                    env != null && env.contains("=") ? env.substring(0, env.indexOf('=')) : env;
                            return !excludedEnvironmentVariables.contains(varName);
                        })
                        .toArray(String[]::new);
    }
}
