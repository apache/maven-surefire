package org.apache.maven.plugin.surefire.extensions;

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

import org.apache.maven.plugin.surefire.booterclient.output.NativeStdOutStreamConsumer;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.extensions.CloseableDaemonThread;
import org.apache.maven.surefire.extensions.CommandReader;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.extensions.ForkChannel;
import org.apache.maven.surefire.extensions.ForkNodeArguments;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.apache.maven.surefire.extensions.util.LineConsumerThread;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.net.StandardSocketOptions.SO_KEEPALIVE;
import static java.net.StandardSocketOptions.SO_REUSEADDR;
import static java.net.StandardSocketOptions.TCP_NODELAY;
import static java.nio.channels.AsynchronousChannelGroup.withThreadPool;
import static java.nio.channels.AsynchronousServerSocketChannel.open;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.apache.maven.surefire.api.util.internal.Channels.newBufferedChannel;
import static org.apache.maven.surefire.api.util.internal.Channels.newChannel;
import static org.apache.maven.surefire.api.util.internal.Channels.newInputStream;
import static org.apache.maven.surefire.api.util.internal.Channels.newOutputStream;
import static org.apache.maven.surefire.api.util.internal.DaemonThreadFactory.newDaemonThreadFactory;

/**
 * The TCP/IP server accepting only one client connection. The forked JVM connects to the server using the
 * {@link #getForkNodeConnectionString() connection string}.
 * The main purpose of this class is to {@link #connectToClient() conect with tthe client}, bind the
 * {@link #bindCommandReader(CommandReader, WritableByteChannel) command reader} to the internal socket's
 * {@link java.io.InputStream}, and bind the
 * {@link #bindEventHandler(EventHandler, CountdownCloseable, ReadableByteChannel) event handler} writing the event
 * objects to the {@link EventHandler event handler}.
 * <br>
 * The objects {@link WritableByteChannel} and {@link ReadableByteChannel} are forked process streams
 * (standard input and output). Both are ignored in this implementation but they are used in {@link LegacyForkChannel}.
 * <br>
 * The channel is closed after the forked JVM has finished normally or the shutdown hook is executed in the plugin.
 */
final class SurefireForkChannel extends ForkChannel
{
    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool( newDaemonThreadFactory() );

    private final AsynchronousServerSocketChannel server;
    private final String localHost;
    private final int localPort;
    private final String sessionId;
    private volatile AsynchronousSocketChannel worker;
    private volatile LineConsumerThread out;

    SurefireForkChannel( @Nonnull ForkNodeArguments arguments ) throws IOException
    {
        super( arguments );
        server = open( withThreadPool( THREAD_POOL ) );
        setTrueOptions( SO_REUSEADDR, TCP_NODELAY, SO_KEEPALIVE );
        InetAddress ip = InetAddress.getLoopbackAddress();
        server.bind( new InetSocketAddress( ip, 0 ), 1 );
        InetSocketAddress localAddress = (InetSocketAddress) server.getLocalAddress();
        localHost = localAddress.getHostString();
        localPort = localAddress.getPort();
        sessionId = arguments.getSessionId();
    }

    @Override
    public void connectToClient() throws IOException
    {
        if ( worker != null )
        {
            throw new IllegalStateException( "already accepted TCP client connection" );
        }

        try
        {
            worker = server.accept().get();
            verifySessionId();
        }
        catch ( InterruptedException e )
        {
            throw new IOException( e.getLocalizedMessage(), e );
        }
        catch ( ExecutionException e )
        {
            throw new IOException( e.getLocalizedMessage(), e.getCause() );
        }
    }

    private void verifySessionId() throws InterruptedException, ExecutionException, IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate( sessionId.length() );
        int read;
        do
        {
            read = worker.read( buffer ).get();
        } while ( read != -1 && buffer.hasRemaining() );
        if ( read == -1 )
        {
            throw new IOException( "Channel closed while verifying the client." );
        }
        buffer.flip();
        String clientSessionId = new String( buffer.array(), US_ASCII );
        if ( !clientSessionId.equals( sessionId ) )
        {
            throw new InvalidSessionIdException( clientSessionId, sessionId );
        }
    }

    @SafeVarargs
    private final void setTrueOptions( SocketOption<Boolean>... options )
        throws IOException
    {
        for ( SocketOption<Boolean> option : options )
        {
            if ( server.supportedOptions().contains( option ) )
            {
                server.setOption( option, true );
            }
        }
    }

    @Override
    public String getForkNodeConnectionString()
    {
        return "tcp://" + localHost + ":" + localPort + "?sessionId=" + sessionId;
    }

    @Override
    public int getCountdownCloseablePermits()
    {
        return 3;
    }

    @Override
    public CloseableDaemonThread bindCommandReader( @Nonnull CommandReader commands,
                                                    WritableByteChannel stdIn )
    {
        // dont use newBufferedChannel here - may cause the command is not sent and the JVM hangs
        // only newChannel flushes the message
        // newBufferedChannel does not flush
        WritableByteChannel channel = newChannel( newOutputStream( worker ) );
        return new StreamFeeder( "commands-fork-" + getArguments().getForkChannelId(), channel, commands,
            getArguments().getConsoleLogger() );
    }

    @Override
    public CloseableDaemonThread bindEventHandler( @Nonnull EventHandler<Event> eventHandler,
                                                   @Nonnull CountdownCloseable countdownCloseable,
                                                   ReadableByteChannel stdOut )
    {
        out = new LineConsumerThread( "fork-" + getArguments().getForkChannelId() + "-out-thread", stdOut,
            new NativeStdOutStreamConsumer( getArguments().getConsoleLogger() ), countdownCloseable );
        out.start();

        ReadableByteChannel channel = newBufferedChannel( newInputStream( worker ) );
        return new EventConsumerThread( "fork-" + getArguments().getForkChannelId() + "-event-thread", channel,
            eventHandler, countdownCloseable, getArguments() );
    }

    @Override
    public void close() throws IOException
    {
        //noinspection unused,EmptyTryBlock,EmptyTryBlock
        try ( Closeable c1 = worker; Closeable c2 = server; Closeable c3 = out )
        {
            // only close all channels
        }
    }
}
