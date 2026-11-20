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
package org.apache.maven.surefire.extensions.util;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import org.apache.maven.surefire.extensions.CloseableDaemonThread;
import org.apache.maven.surefire.extensions.EventHandler;

public final class LineConsumerThread extends CloseableDaemonThread {
    private static final int MAX_LINE_LENGTH = 1024 * 1024;

    private final Charset encoding;
    private final ReadableByteChannel channel;
    private final EventHandler<String> eventHandler;
    private final CountdownCloseable countdownCloseable;
    private volatile boolean disabled;

    public LineConsumerThread(
            @Nonnull String threadName,
            @Nonnull ReadableByteChannel channel,
            @Nonnull EventHandler<String> eventHandler,
            @Nonnull CountdownCloseable countdownCloseable) {
        this(threadName, channel, eventHandler, countdownCloseable, Charset.defaultCharset());
    }

    public LineConsumerThread(
            @Nonnull String threadName,
            @Nonnull ReadableByteChannel channel,
            @Nonnull EventHandler<String> eventHandler,
            @Nonnull CountdownCloseable countdownCloseable,
            @Nonnull Charset encoding) {
        super(threadName);
        this.channel = channel;
        this.eventHandler = eventHandler;
        this.countdownCloseable = countdownCloseable;
        this.encoding = encoding;
    }

    @Override
    public void run() {
        try (Reader reader = Channels.newReader(channel, encoding.newDecoder(), 4096);
                CountdownCloseable c = countdownCloseable) {
            StringBuilder line = new StringBuilder();
            boolean truncated = false;
            CharBuffer buffer = CharBuffer.allocate(4096);
            while (reader.read(buffer) != -1) {
                buffer.flip();
                while (buffer.hasRemaining()) {
                    char character = buffer.get();
                    if (character == '\n') {
                        dispatch(line, truncated);
                        line.setLength(0);
                        truncated = false;
                    } else if (character != '\r' && line.length() < MAX_LINE_LENGTH) {
                        line.append(character);
                    } else if (line.length() >= MAX_LINE_LENGTH) {
                        truncated = true;
                    }
                }
                buffer.clear();
            }
            if (line.length() > 0 || truncated) {
                dispatch(line, truncated);
            }
        } catch (IOException e) {
            if (e instanceof InterruptedIOException || e.getCause() instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void dispatch(StringBuilder line, boolean truncated) {
        if (!disabled) {
            eventHandler.handleEvent(truncated ? line.append(" [line truncated]").toString() : line.toString());
        }
    }

    public void disable() {
        disabled = true;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
