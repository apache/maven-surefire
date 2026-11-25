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
package org.apache.maven.plugin.surefire.extensions;

import javax.annotation.Nonnull;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

import org.apache.maven.plugin.surefire.booterclient.output.NativeStdOutStreamConsumer;
import org.apache.maven.surefire.api.event.Event;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.extensions.CloseableDaemonThread;
import org.apache.maven.surefire.extensions.CommandReader;
import org.apache.maven.surefire.extensions.EventHandler;
import org.apache.maven.surefire.extensions.ForkChannel;
import org.apache.maven.surefire.extensions.util.CountDownLauncher;
import org.apache.maven.surefire.extensions.util.CountdownCloseable;
import org.apache.maven.surefire.extensions.util.LineConsumerThread;

import static java.net.StandardSocketOptions.SO_REUSEADDR;
import static java.nio.channels.Channels.newOutputStream;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.apache.maven.surefire.api.util.internal.Channels.newBufferedChannel;
import static org.apache.maven.surefire.api.util.internal.Channels.newChannel;
import static org.apache.maven.surefire.shared.lang3.StringUtils.isBlank;
import static org.apache.maven.surefire.shared.lang3.StringUtils.isNotBlank;

/**
 * The TCP/IP server accepting only one client connection. The forked JVM connects to the server using the
 * {@link #getForkNodeConnectionString() connection string}.
 * The main purpose of this class is to accept the client connection, bind the
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
final class SurefireForkChannel extends ForkChannel {
    private static final long CONNECTION_POLL_INTERVAL_MILLIS = 100L;

    private final ServerSocketChannel server;
    private final String localHost;
    private final int localPort;
    private final String sessionId;
    private final Bindings bindings = new Bindings(2);
    private SocketChannel connection;
    private volatile LineConsumerThread out;
    private volatile CloseableDaemonThread commandReaderBindings;
    private volatile CloseableDaemonThread eventHandlerBindings;
    private volatile EventBindings eventBindings;
    private volatile CommandBindings commandBindings;

    SurefireForkChannel(@Nonnull ForkNodeArguments arguments) throws IOException {
        super(arguments);
        server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.setOption(SO_REUSEADDR, true);
        InetAddress ip = InetAddress.getLoopbackAddress();
        server.bind(new InetSocketAddress(ip, 0), 1);
        InetSocketAddress localAddress = (InetSocketAddress) server.getLocalAddress();
        localHost = localAddress.getHostString();
        localPort = localAddress.getPort();
        sessionId = arguments.getSessionId();
    }

    @Override
    public void tryConnectToClient() {
        // The listener is already bound. Accept when the fork process and the stream handlers are available.
    }

    @Override
    public String getForkNodeConnectionString() {
        try {
            URI uri = new URI(
                    "tcp",
                    null,
                    localHost,
                    localPort,
                    null,
                    isBlank(sessionId) ? null : "sessionId=" + sessionId,
                    null);
            return uri.toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int getCountdownCloseablePermits() {
        return 3;
    }

    @Override
    public void bindCommandReader(@Nonnull CommandReader commands, WritableByteChannel stdIn)
            throws IOException, InterruptedException {
        commandBindings = new CommandBindings(commands);

        bindings.countDown();
    }

    @Override
    public void bindEventHandler(
            @Nonnull EventHandler<Event> eventHandler,
            @Nonnull CountdownCloseable countdown,
            ReadableByteChannel stdOut)
            throws IOException, InterruptedException {
        bindEventHandler(eventHandler, countdown, stdOut, null);
    }

    @Override
    public void bindEventHandler(
            @Nonnull EventHandler<Event> eventHandler,
            @Nonnull CountdownCloseable countdown,
            ReadableByteChannel stdOut,
            Process process)
            throws IOException, InterruptedException {
        ForkNodeArguments args = getArguments();
        out = new LineConsumerThread(
                "fork-" + args.getForkChannelId() + "-out-thread",
                stdOut,
                new NativeStdOutStreamConsumer(args.getConsoleLock()),
                countdown);
        out.start();

        eventBindings = new EventBindings(eventHandler, countdown, process);

        bindings.countDown();
    }

    @Override
    public void disable() {
        if (eventHandlerBindings != null) {
            eventHandlerBindings.disable();
        }

        if (commandReaderBindings != null) {
            commandReaderBindings.disable();
        }
    }

    @Override
    public synchronized void close() throws IOException {
        try (Closeable stdout = out;
                Closeable socket = connection;
                Closeable listener = server) {
            // Close the listener before the accepted socket and stdout.
        }
    }

    private synchronized SocketChannel acceptConnection() throws IOException {
        connection = server.accept();
        return connection;
    }

    private void verifySessionId(SocketChannel channel) throws InterruptedException, IOException {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(sessionId.length());
            int read;
            do {
                read = channel.read(buffer);
            } while (read != -1 && buffer.hasRemaining());

            if (read == -1) {
                throw new IOException("Channel closed while verifying the client.");
            }

            ((Buffer) buffer).flip();
            String clientSessionId = new String(buffer.array(), US_ASCII);
            if (!clientSessionId.equals(sessionId)) {
                throw new InvalidSessionIdException(clientSessionId, sessionId);
            }
        } catch (ClosedByInterruptException e) {
            Thread.interrupted();
            InterruptedException interrupted = new InterruptedException();
            interrupted.initCause(e);
            throw interrupted;
        }
    }

    private static InputStream socketInputStream(SocketChannel socket) {
        // The JDK's channel stream adapters share a blocking lock. Read directly so commands can be sent concurrently.
        return new InputStream() {
            @Override
            public int read() throws IOException {
                byte[] b = new byte[1];
                return read(b, 0, b.length) == -1 ? -1 : b[0] & 0xff;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return socket.read(ByteBuffer.wrap(b, off, len));
            }

            @Override
            public void close() throws IOException {
                socket.close();
            }
        };
    }

    private class EventBindings {
        private final EventHandler<Event> eventHandler;
        private final CountdownCloseable countdown;
        private final Process process;

        private EventBindings(EventHandler<Event> eventHandler, CountdownCloseable countdown, Process process) {
            this.eventHandler = eventHandler;
            this.countdown = countdown;
            this.process = process;
        }

        void bindEventHandler(SocketChannel source) {
            ForkNodeArguments args = getArguments();
            String threadName = "fork-" + args.getForkChannelId() + "-event-thread";
            ReadableByteChannel channel = newBufferedChannel(socketInputStream(source));
            eventHandlerBindings = new EventConsumerThread(threadName, channel, eventHandler, countdown, args);
            eventHandlerBindings.start();
        }
    }

    private class CommandBindings {
        private final CommandReader commands;

        private CommandBindings(CommandReader commands) {
            this.commands = commands;
        }

        void bindCommandSender(SocketChannel source) {
            // don't use newBufferedChannel here - may cause the command is not sent and the JVM hangs
            // only newChannel flushes the message
            // newBufferedChannel does not flush
            ForkNodeArguments args = getArguments();
            WritableByteChannel channel = newChannel(newOutputStream(source));
            String threadName = "commands-fork-" + args.getForkChannelId();
            commandReaderBindings = new StreamFeeder(threadName, channel, commands, args.getConsoleLogger());
            commandReaderBindings.start();
        }
    }

    private class Bindings extends CountDownLauncher {
        private Bindings(int count) {
            super(count);
        }

        @Override
        protected void job() throws IOException, InterruptedException {
            Process process = eventBindings.process;
            SocketChannel channel = awaitConnection(process);
            if (channel == null) {
                // No event consumer will start. Release its permit so ForkStarter can collect the exit code.
                eventBindings.countdown.close();
            } else {
                if (isNotBlank(sessionId)) {
                    verifySessionId(channel);
                }
                eventBindings.bindEventHandler(channel);
                if (process == null || process.isAlive()) {
                    commandBindings.bindCommandSender(channel);
                }
            }
        }
    }

    private SocketChannel awaitConnection(Process process) throws InterruptedException, IOException {
        try (Selector selector = server.provider().openSelector()) {
            server.register(selector, OP_ACCEPT);
            while (!Thread.interrupted()) {
                // Check the listener after observing the exit so we can still drain a queued connection.
                boolean exited = process != null && !process.isAlive();
                SocketChannel socket = acceptConnection();
                if (socket != null || exited) {
                    return socket;
                }
                selector.select(CONNECTION_POLL_INTERVAL_MILLIS);
                selector.selectedKeys().clear();
            }
        }
        throw new InterruptedException();
    }
}
