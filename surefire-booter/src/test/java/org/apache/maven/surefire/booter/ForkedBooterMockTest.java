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
package org.apache.maven.surefire.booter;

import javax.annotation.Nonnull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.maven.surefire.api.booter.MasterProcessChannelDecoder;
import org.apache.maven.surefire.api.booter.MasterProcessChannelEncoder;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.api.util.internal.WritableBufferedByteChannel;
import org.apache.maven.surefire.booter.spi.AbstractMasterProcessChannelProcessorFactory;
import org.apache.maven.surefire.booter.spi.CommandChannelDecoder;
import org.apache.maven.surefire.booter.spi.EventChannelEncoder;
import org.apache.maven.surefire.booter.spi.LegacyMasterProcessChannelProcessorFactory;
import org.apache.maven.surefire.booter.spi.SurefireMasterProcessChannelProcessorFactory;
import org.apache.maven.surefire.shared.utils.cli.ShutdownHookUtils;
import org.apache.maven.surefire.spi.MasterProcessChannelProcessorFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.maven.surefire.api.util.internal.Channels.newBufferedChannel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ForkedBooter}.
 */
public class ForkedBooterMockTest {

    private static Object invokeMethod(Class<?> clazz, String methodName, Object... args) throws Exception {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                method.setAccessible(true);
                return method.invoke(null, args);
            }
        }
        throw new NoSuchMethodException(clazz.getName() + "." + methodName);
    }

    private static Object invokeMethod(Object target, String methodName, Object... args) throws Exception {
        Method[] methods = target.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                method.setAccessible(true);
                return method.invoke(target, args);
            }
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + methodName);
    }

    private static void setInternalState(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    @Test
    public void shouldCheckNewPingMechanism() throws Exception {
        boolean canUse = (boolean) invokeMethod(ForkedBooter.class, "canUseNewPingMechanism", (PpidChecker) null);
        assertThat(canUse).isFalse();

        PpidChecker pluginProcessChecker = mock(PpidChecker.class);

        org.mockito.Mockito.when(pluginProcessChecker.canUse()).thenReturn(false);
        canUse = (boolean) invokeMethod(ForkedBooter.class, "canUseNewPingMechanism", pluginProcessChecker);
        assertThat(canUse).isFalse();

        org.mockito.Mockito.when(pluginProcessChecker.canUse()).thenReturn(true);
        canUse = (boolean) invokeMethod(ForkedBooter.class, "canUseNewPingMechanism", pluginProcessChecker);
        assertThat(canUse).isTrue();
    }

    @Test
    public void shouldNotCloseChannelProcessorFactory() throws Exception {
        ForkedBooter booter = new ForkedBooter();
        MasterProcessChannelProcessorFactory channelProcessorFactory = mock(MasterProcessChannelProcessorFactory.class);
        setInternalState(booter, "channelProcessorFactory", null);

        invokeMethod(booter, "closeForkChannel");

        org.mockito.Mockito.verifyNoInteractions(channelProcessorFactory);
    }

    @Test
    public void shouldCloseChannelProcessorFactory() throws Exception {
        ForkedBooter booter = new ForkedBooter();
        MasterProcessChannelProcessorFactory channelProcessorFactory = mock(MasterProcessChannelProcessorFactory.class);
        setInternalState(booter, "channelProcessorFactory", channelProcessorFactory);

        invokeMethod(booter, "closeForkChannel");

        verify(channelProcessorFactory, times(1)).close();
        org.mockito.Mockito.verifyNoMoreInteractions(channelProcessorFactory);
    }

    @Test
    public void shouldFailOnCloseChannelProcessorFactory() throws Exception {
        ForkedBooter booter = new ForkedBooter();
        MasterProcessChannelProcessorFactory channelProcessorFactory = mock(MasterProcessChannelProcessorFactory.class);
        setInternalState(booter, "channelProcessorFactory", channelProcessorFactory);

        org.mockito.Mockito.doThrow(new IOException())
                .when(channelProcessorFactory)
                .close();

        invokeMethod(booter, "closeForkChannel");

        verify(channelProcessorFactory, times(1)).close();
        org.mockito.Mockito.verifyNoMoreInteractions(channelProcessorFactory);
    }

    @Test
    public void shouldLookupLegacyDecoderFactory() throws Exception {
        try (MasterProcessChannelProcessorFactory factory = (MasterProcessChannelProcessorFactory)
                invokeMethod(ForkedBooter.class, "lookupDecoderFactory", "pipe://3")) {
            assertThat(factory).isInstanceOf(LegacyMasterProcessChannelProcessorFactory.class);

            assertThat(factory.canUse("pipe://3")).isTrue();

            assertThat(factory.canUse("-- whatever --")).isFalse();

            assertAll(() -> assertThrows(MalformedURLException.class, () -> {
                factory.connect("tcp://localhost:123");
            }));

            factory.connect("pipe://3");

            ForkNodeArguments args = new ForkedNodeArg(1, false);
            MasterProcessChannelDecoder decoder = factory.createDecoder(args);
            assertThat(decoder).isInstanceOf(CommandChannelDecoder.class);
            MasterProcessChannelEncoder encoder = factory.createEncoder(args);
            assertThat(encoder).isInstanceOf(EventChannelEncoder.class);
        }
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void shouldScheduleFlushes() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        class Factory extends AbstractMasterProcessChannelProcessorFactory {
            @Override
            public boolean canUse(String channelConfig) {
                return false;
            }

            @Override
            public void connect(String channelConfig) {}

            @Override
            public MasterProcessChannelDecoder createDecoder(@Nonnull ForkNodeArguments args) {
                return null;
            }

            @Override
            public MasterProcessChannelEncoder createEncoder(@Nonnull ForkNodeArguments args) {
                return null;
            }

            public void runScheduler() throws InterruptedException {
                final WritableBufferedByteChannel channel = newBufferedChannel(out);
                schedulePeriodicFlusher(100, channel);
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 10; i++) {
                            try {
                                channel.write(ByteBuffer.wrap(new byte[] {1}));
                                Thread.sleep(75);
                            } catch (Exception e) {
                                //
                            }
                        }
                    }
                };
                t.setDaemon(true);
                t.start();
                t.join(5000L);
            }
        }

        Factory factory = new Factory();
        factory.runScheduler();
        factory.close();
        assertThat(out.size()).isPositive();
        assertThat(out.size()).isLessThanOrEqualTo(10);
    }

    @Test
    public void shouldLookupSurefireDecoderFactory() throws Exception {
        try (ServerSocketChannel server = ServerSocketChannel.open()) {
            server.bind(new InetSocketAddress(0));
            int serverPort = ((InetSocketAddress) server.getLocalAddress()).getPort();

            try (MasterProcessChannelProcessorFactory factory = (MasterProcessChannelProcessorFactory)
                    invokeMethod(ForkedBooter.class, "lookupDecoderFactory", "tcp://localhost:" + serverPort)) {
                assertThat(factory).isInstanceOf(SurefireMasterProcessChannelProcessorFactory.class);

                assertThat(factory.canUse("tcp://localhost:" + serverPort)).isTrue();

                assertThat(factory.canUse("-- whatever --")).isFalse();

                assertAll(
                        () -> assertThrows(MalformedURLException.class, () -> {
                            factory.connect("pipe://1");
                        }),
                        () -> assertThrows(IOException.class, () -> {
                            factory.connect("tcp://localhost:123\u0000\u0000\u0000");
                        }));

                factory.connect("tcp://localhost:" + serverPort);
                ForkNodeArguments args = new ForkedNodeArg(1, false);
                MasterProcessChannelDecoder decoder = factory.createDecoder(args);
                assertThat(decoder).isInstanceOf(CommandChannelDecoder.class);
                MasterProcessChannelEncoder encoder = factory.createEncoder(args);
                assertThat(encoder).isInstanceOf(EventChannelEncoder.class);
            }
        }
    }

    @Test
    public void shouldAuthenticate() throws Exception {
        try (ServerSocketChannel server = ServerSocketChannel.open()) {
            server.bind(new InetSocketAddress(0));
            int serverPort = ((InetSocketAddress) server.getLocalAddress()).getPort();
            final String uuid = UUID.randomUUID().toString();
            String url = "tcp://localhost:" + serverPort + "?sessionId=" + uuid;
            try (MasterProcessChannelProcessorFactory factory = (MasterProcessChannelProcessorFactory)
                    invokeMethod(ForkedBooter.class, "lookupDecoderFactory", url)) {
                assertThat(factory).isInstanceOf(SurefireMasterProcessChannelProcessorFactory.class);

                FutureTask<Boolean> task = new FutureTask<>(new Callable<Boolean>() {
                    @Override
                    public Boolean call() {
                        try {
                            SocketChannel channel = server.accept();
                            ByteBuffer bb = ByteBuffer.allocate(uuid.length());
                            int read = channel.read(bb);
                            assertThat(read).isEqualTo(uuid.length());
                            ((Buffer) bb).flip();
                            assertThat(new String(bb.array(), US_ASCII)).isEqualTo(uuid);
                            return true;
                        } catch (IOException e) {
                            return false;
                        }
                    }
                });

                Thread t = new Thread(task);
                t.setDaemon(true);
                t.start();

                factory.connect(url);

                try {
                    task.get(10, SECONDS);
                } finally {
                    factory.close();
                }
            }
        }
    }

    @Test
    public void testFlushEventChannelOnExit() throws Exception {
        try (MockedStatic<ShutdownHookUtils> mockedShutdownHookUtils = mockStatic(ShutdownHookUtils.class)) {
            final MasterProcessChannelEncoder eventChannel = mock(MasterProcessChannelEncoder.class);
            ForkedBooter booter = new ForkedBooter();
            setInternalState(booter, "eventChannel", eventChannel);

            mockedShutdownHookUtils
                    .when(() -> ShutdownHookUtils.addShutDownHook(org.mockito.ArgumentMatchers.any(Thread.class)))
                    .thenAnswer(invocation -> {
                        Thread t = invocation.getArgument(0);
                        assertThat(t.isDaemon()).isTrue();
                        t.run();
                        verify(eventChannel, times(1)).onJvmExit();
                        return null;
                    });
            invokeMethod(booter, "flushEventChannelOnExit");
        }
    }

    @Test
    public void shouldParseUUID() throws Exception {
        UUID uuid = UUID.randomUUID();
        URI uri = new URI("tcp://localhost:12345?sessionId=" + uuid);
        String parsed =
                (String) invokeMethod(SurefireMasterProcessChannelProcessorFactory.class, "extractSessionId", uri);
        assertThat(parsed).isEqualTo(uuid.toString());
    }

    @Test
    public void shouldNotParseUUID() throws Exception {
        UUID uuid = UUID.randomUUID();
        URI uri = new URI("tcp://localhost:12345?xxx=" + uuid);
        String parsed =
                (String) invokeMethod(SurefireMasterProcessChannelProcessorFactory.class, "extractSessionId", uri);
        assertThat(parsed).isNull();
    }
}
