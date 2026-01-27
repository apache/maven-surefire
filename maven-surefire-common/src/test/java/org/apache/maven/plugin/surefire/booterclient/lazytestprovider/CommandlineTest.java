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

import org.apache.maven.surefire.shared.utils.cli.CommandLineException;
import org.junit.Test;

import static org.apache.maven.surefire.shared.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class CommandlineTest {
    @Test
    public void shouldGetEnvironmentVariables() {
        Commandline cli = new Commandline();
        String[] env = cli.getEnvironmentVariables();

        assertThat(env).doesNotHaveDuplicates().anyMatch(s -> s != null && s.startsWith("JAVA_HOME"));

        String[] excluded = {"JAVA_HOME"};
        cli = new Commandline(excluded);
        env = cli.getEnvironmentVariables();

        assertThat(env).doesNotHaveDuplicates().allMatch(s -> s != null && !s.startsWith("JAVA_HOME="));
    }

    @Test
    public void shouldExecute() throws CommandLineException {
        Commandline cli = new Commandline();
        cli.getShell().setWorkingDirectory(System.getProperty("user.dir"));
        cli.getShell().setExecutable(IS_OS_WINDOWS ? "dir" : "ls");
        cli.execute();
    }
}
