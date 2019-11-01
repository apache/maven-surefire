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
import org.apache.maven.surefire.extensions.ForkedChannelServer;

import javax.annotation.Nonnull;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
final class NetworkingProcessExecutor implements ExecutableCommandline
{
    @Nonnull
    @Override
    public CommandLineCallable executeCommandLineAsCallable(
            @Nonnull Commandline cli,
            @Nonnull AbstractCommandReader commands,
            @Nonnull EventHandler events,
            @Nonnull ForkedChannelServer server, StreamConsumer stdOut, StreamConsumer stdErr,
            @Nonnull Runnable runAfterProcessTermination ) throws CommandLineException
    {
        return new NetworkCommandLineCallable( cli, commands, events, server, stdOut, stdErr,
                runAfterProcessTermination );
    }

    private static class NetworkCommandLineCallable implements CommandLineCallable
    {
        private final Commandline cli;
        private final AbstractCommandReader commands;
        private final EventHandler events;
        private final ForkedChannelServer server;
        private final StreamConsumer stdOut;
        private final StreamConsumer stdErr;
        private final Runnable runAfterProcessTermination;

        NetworkCommandLineCallable(
                @Nonnull Commandline cli,
                @Nonnull AbstractCommandReader commands,
                @Nonnull EventHandler events,
                @Nonnull ForkedChannelServer server, StreamConsumer stdOut, StreamConsumer stdErr,
                @Nonnull Runnable runAfterProcessTermination )
        {
            this.cli = cli;
            this.commands = commands;
            this.events = events;
            this.server = server;
            this.stdOut = stdOut;
            this.stdErr = stdErr;
            this.runAfterProcessTermination = runAfterProcessTermination;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Integer call() throws CommandLineException
        {
            //set up the thread to send commands to the remote process
            final Thread pumper = new Thread( new Runnable()
            {
                @Override
                public void run()
                {
                    while ( !commands.isClosed() )
                    {
                        try
                        {
                            server.send( commands.readNextCommand() );
                        }
                        catch ( IOException e )
                        {
                            e.printStackTrace();
                        }
                    }
                }
            } );
            pumper.start();
            Integer ret = CommandLineUtils.executeCommandLineAsCallable( cli, null, stdOut, stdErr, 0,
                    runAfterProcessTermination, ISO_8859_1 ).call();
            try
            {
                pumper.join();
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
            return ret;
        }
    }
}
