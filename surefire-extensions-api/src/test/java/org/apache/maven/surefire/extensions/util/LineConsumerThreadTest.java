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

import java.io.ByteArrayInputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LineConsumerThreadTest {
    @Test
    void shouldDispatchCompleteLines() throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream("first\nsecond\n".getBytes(UTF_8)));
        LineConsumerThread thread = new LineConsumerThread("line-consumer-test", channel, events::add, countdown());

        thread.start();
        thread.join(10_000);

        assertFalse(thread.isAlive());
        assertThat(events).containsExactly("first", "second");
    }

    @Test
    void shouldTruncateOverlongLine() throws Exception {
        char[] chars = new char[1_100_000];
        Arrays.fill(chars, 'A');
        List<String> events = new CopyOnWriteArrayList<>();
        ReadableByteChannel channel =
                Channels.newChannel(new ByteArrayInputStream((new String(chars) + "\n").getBytes(UTF_8)));
        LineConsumerThread thread = new LineConsumerThread("line-consumer-test", channel, events::add, countdown());

        thread.start();
        thread.join(10_000);

        assertFalse(thread.isAlive());
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).endsWith("[line truncated]");
        assertThat(events.get(0).length()).isLessThan(chars.length);
    }

    private static CountdownCloseable countdown() {
        return new CountdownCloseable(() -> {}, 1);
    }
}
