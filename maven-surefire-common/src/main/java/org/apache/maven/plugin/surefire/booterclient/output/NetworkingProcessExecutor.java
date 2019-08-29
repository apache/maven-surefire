package org.apache.maven.plugin.surefire.booterclient.output;

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

import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.AbstractCommandReader;
import org.apache.maven.shared.utils.cli.CommandLineCallable;
import org.apache.maven.shared.utils.cli.CommandLineException;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.apache.maven.shared.utils.cli.Commandline;
import org.apache.maven.shared.utils.cli.StreamConsumer;

import javax.annotation.Nonnull;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
final class NetworkingProcessExecutor
        implements ExecutableCommandline
{
    @Nonnull
    @Override
    public CommandLineCallable executeCommandLineAsCallable( @Nonnull Commandline cli,
                                                             @Nonnull AbstractCommandReader commands,
                                                             @Nonnull AbstractEventHandler events,
                                                             StreamConsumer stdOut,
                                                             StreamConsumer stdErr,
                                                             @Nonnull Runnable runAfterProcessTermination )
            throws CommandLineException
    {
        return CommandLineUtils.executeCommandLineAsCallable( cli, null, stdOut, stdErr,
                0, runAfterProcessTermination, ISO_8859_1 );
    }
}
