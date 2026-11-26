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

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 *
 */
class CountdownCloseableTest {

    @Test
    void awaitClosedShouldIgnoreSpuriousWakeup() throws Exception {
        CountdownCloseable countdownCloseable = new CountdownCloseable(mock(Closeable.class), 1);
        AtomicBoolean returnedFromAwait = new AtomicBoolean(false);

        Thread waiter = new Thread(() -> {
            try {
                countdownCloseable.awaitClosed();
                returnedFromAwait.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        waiter.start();
        awaitState(waiter, Thread.State.WAITING);

        // Simulate a spurious wakeup (JLS 17.2): notifying the monitor without the
        // countdown having reached zero must not let awaitClosed() return.
        synchronized (countdownCloseable) {
            countdownCloseable.notifyAll();
        }
        waiter.join(300L);
        assertThat(waiter.isAlive()).isTrue();
        assertThat(returnedFromAwait).isFalse();

        countdownCloseable.close();
        waiter.join(5_000L);
        assertThat(waiter.isAlive()).isFalse();
        assertThat(returnedFromAwait).isTrue();
    }

    private static void awaitState(Thread thread, Thread.State state) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000L;
        while (thread.getState() != state) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError(
                        "Thread " + thread.getName() + " never reached state " + state + ", was " + thread.getState());
            }
            Thread.sleep(10L);
        }
    }
}
