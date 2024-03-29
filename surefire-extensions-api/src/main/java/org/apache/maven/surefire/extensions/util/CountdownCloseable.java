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

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import java.io.Closeable;
import java.io.IOException;

/**
 * Decrements {@code countdown} and calls {@code closeable} if reached zero.
 */
public final class CountdownCloseable implements Closeable {
    private final Closeable closeable;
    private volatile int countdown;

    public CountdownCloseable(@Nonnull Closeable closeable, @Nonnegative int countdown) {
        this.closeable = closeable;
        this.countdown = countdown;
    }

    @Override
    public synchronized void close() throws IOException {
        if (--countdown == 0) {
            try {
                closeable.close();
            } finally {
                notifyAll();
            }
        }
    }

    /**
     * Waiting for one Thread in {@link CommandlineExecutor#awaitExit()}.
     *
     * @throws InterruptedException see {@link Object#wait()}
     */
    public synchronized void awaitClosed() throws InterruptedException {
        if (countdown > 0) {
            wait();
        }
    }
}
