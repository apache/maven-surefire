package org.apache.maven.surefire.extensions.util;

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

import org.apache.maven.surefire.shared.utils.cli.CommandLineException;
import org.apache.maven.surefire.shared.utils.cli.Commandline;

import java.io.Closeable;

import static org.apache.maven.surefire.shared.utils.cli.ShutdownHookUtils.addShutDownHook;
import static org.apache.maven.surefire.shared.utils.cli.ShutdownHookUtils.removeShutdownHook;

/**
 * Programming model with this class:
 * <pre> {@code
 * try ( CommandlineExecutor exec = new CommandlineExecutor( cli, endOfStreamsCountdown );
 *       CommandlineStreams streams = exec.execute() )
 * {
 *     // register exec in the shutdown hook to destroy pending process
 *
 *     // register streams in the shutdown hook to close all three streams
 *
 *     ReadableByteChannel stdOut = streams.getStdOutChannel();
 *     ReadableByteChannel stdErr = streams.getStdErrChannel();
 *     WritableByteChannel stdIn = streams.getStdInChannel();
 *     // lineConsumerThread = new LineConsumerThread( ..., stdErr, ..., endOfStreamsCountdown );
 *     // lineConsumerThread.start();
 *
 *     // stdIn.write( ... );
 *
 *     int exitCode = exec.awaitExit();
 *     // process exitCode
 * }
 * catch ( InterruptedException e )
 * {
 *     lineConsumerThread.disable();
 * }
 * catch ( CommandLineException e )
 * {
 *     // handle the exceptions
 * }
 * } </pre>
 */
public class CommandlineExecutor implements Closeable
{
    private final Commandline cli;
    private final CountdownCloseable endOfStreamsCountdown;
    private Process process;
    private Thread shutdownHook;

    public CommandlineExecutor( Commandline cli, CountdownCloseable endOfStreamsCountdown )
    {
        // now the surefire-extension-api is dependent on CLI without casting generic type T to unrelated object
        // and the user would not use maven-surefire-common nothing but the only surefire-extension-api
        // because maven-surefire-common is used for MOJO plugin and not the user's extensions. The user does not need
        // to see all MOJO impl. Only the surefire-api, surefire-logger-api and surefire-extension-api.
        this.cli = cli;
        this.endOfStreamsCountdown = endOfStreamsCountdown;
    }

    public CommandlineStreams execute() throws CommandLineException
    {
        process = cli.execute();
        shutdownHook = new ProcessHook( process );
        addShutDownHook( shutdownHook );
        return new CommandlineStreams( process );
    }

    public int awaitExit() throws InterruptedException
    {
        try
        {
            return process.waitFor();
        }
        finally
        {
            endOfStreamsCountdown.awaitClosed();
        }
    }

    @Override
    public void close()
    {
        if ( shutdownHook != null )
        {
            shutdownHook.run();
            removeShutdownHook( shutdownHook );
            shutdownHook = null;
        }
    }

    private static class ProcessHook extends Thread
    {
        private final Process process;

        private ProcessHook( Process process )
        {
            super( "cli-shutdown-hook" );
            this.process = process;
            setContextClassLoader( null );
            setDaemon( true );
        }

        /** {@inheritDoc} */
        public void run()
        {
            process.destroy();
        }
    }

}
