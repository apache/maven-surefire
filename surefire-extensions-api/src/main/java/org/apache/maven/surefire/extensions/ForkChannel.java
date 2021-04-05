package org.apache.maven.surefire.extensions;

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

import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * It's a session object used only by a particular Thread in ForkStarter
 * and communicates with a dedicated forked JVM. It represents a server.
 * <br>
 * <br>
 * It connects to a remote client by {@link #tryConnectToClient()}, provides a connection string
 * {@link #getForkNodeConnectionString()} needed by the client in forked JVM, binds event handler and command reader.
 * This object is called in one Thread.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M5
 */
public abstract class ForkChannel implements Closeable
{
    @Nonnull
    private final ForkNodeArguments arguments;

    /**
     * @param arguments data necessary for command reader and event handler.
     */
    protected ForkChannel( @Nonnull ForkNodeArguments arguments )
    {
        this.arguments = arguments;
    }

    /**
     * Asynchronously connects to the client.
     *
     * @throws IOException if stream fails
     * @throws InterruptedException if interrupted thread
     */
    public abstract void tryConnectToClient() throws IOException, InterruptedException;

    /**
     * This is server related class, which if binds to a TCP port, determines the connection string for the client.
     *
     * @return a connection string utilized by the client in the fork JVM
     */
    public abstract String getForkNodeConnectionString();

    /**
     * the permits in {@link CountdownCloseable}.
     */
    public abstract int getCountdownCloseablePermits();

    /**
     * Binds command handler to the channel. Starts a Thread streaming out the commands.
     *
     * @param commands command reader, see {@link CommandReader#readNextCommand()}
     * @param stdIn    optional standard input stream of the JVM to write the encoded commands into it
     * @throws IOException if an error in the fork channel
     * @throws InterruptedException channel interrupted
     */
    public abstract void bindCommandReader( @Nonnull CommandReader commands, WritableByteChannel stdIn )
        throws IOException, InterruptedException;

    /**
     * Starts a Thread reading the events.
     *
     * @param eventHandler       event eventHandler
     * @param countdownCloseable count down of the final call of {@link Closeable#close()}
     * @param stdOut             optional standard output stream of the JVM
     * @throws IOException if an error in the fork channel
     * @throws InterruptedException channel interrupted
     */
    public abstract void bindEventHandler( @Nonnull EventHandler<Event> eventHandler,
                                           @Nonnull CountdownCloseable countdownCloseable,
                                           ReadableByteChannel stdOut )
        throws IOException, InterruptedException;

    @Nonnull
    protected ForkNodeArguments getArguments()
    {
        return arguments;
    }

    public abstract void disable();

    @Override
    public abstract void close() throws IOException;
}
