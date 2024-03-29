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
package org.apache.maven.plugin.surefire.booterclient.lazytestprovider;

import org.apache.maven.surefire.api.booter.Shutdown;

/**
 * Remote interface of forked JVM with command methods.
 * <br>
 * Implemented by {@link TestProvidingInputStream} and {@link TestLessInputStream} where the method
 * {@link TestLessInputStream#provideNewTest()} purposefully does nothing. Some methods in
 * {@link org.apache.maven.plugin.surefire.booterclient.lazytestprovider.TestLessInputStream.TestLessInputStreamBuilder}
 * throw {@link UnsupportedOperationException}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 * @see TestProvidingInputStream
 * @see TestLessInputStream
 */
public interface NotifiableTestStream {
    /**
     * Forked jvm notifies master process to provide a new test.
     * <br>
     * Notifies {@link TestProvidingInputStream} in order to dispatch a new test back to the forked
     * jvm (particular fork which hits this call); or do nothing in {@link TestLessInputStream}.
     */
    void provideNewTest();

    /**
     * Sends an event to a fork jvm in order to skip tests.
     * Returns immediately without blocking.
     */
    void skipSinceNextTest();

    void shutdown(Shutdown shutdownType);

    void noop();

    void acknowledgeByeEventReceived();
}
