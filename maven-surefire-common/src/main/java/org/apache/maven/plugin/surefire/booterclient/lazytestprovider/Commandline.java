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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * The forked JVM's command line: the environment of this JVM without the excluded variables, plus the added ones.
 */
public class Commandline extends org.apache.maven.surefire.extensions.util.Commandline {
    private final Collection<String> excludedEnvironmentVariables;

    /**
     * For testing purposes only.
     */
    public Commandline() {
        this(new String[0]);
    }

    public Commandline(String[] excludedEnvironmentVariables) {
        this.excludedEnvironmentVariables = new ConcurrentLinkedDeque<>();
        Collections.addAll(this.excludedEnvironmentVariables, excludedEnvironmentVariables);
    }

    @Override
    protected Map<String, String> environment() {
        Map<String, String> env = super.environment();
        env.keySet().removeAll(excludedEnvironmentVariables);
        return env;
    }
}
