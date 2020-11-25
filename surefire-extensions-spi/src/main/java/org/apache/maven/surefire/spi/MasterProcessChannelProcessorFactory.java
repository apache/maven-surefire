package org.apache.maven.surefire.spi;

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

import org.apache.maven.surefire.api.booter.MasterProcessChannelDecoder;
import org.apache.maven.surefire.api.booter.MasterProcessChannelEncoder;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;

/**
 * The SPI interface, a factory of an encoder and a decoder.
 */
public interface MasterProcessChannelProcessorFactory extends Closeable
{
    /**
     * Evaluates the {@code channelConfig}.
     *
     * @param channelConfig a connection string used by the fork JVM
     * @return {@code true} if {@code channelConfig} is applicable and thus this SPI is eligible in the fork
     */
    boolean canUse( String channelConfig );

    /**
     * Open a new connection.
     *
     * @param channelConfig e.g. "pipe://3" or "tcp://localhost:65035"
     * @throws IOException if cannot connect
     */
    void connect( String channelConfig ) throws IOException;

    /**
     * Decoder factory method.
     * @param forkingArguments forking arguments
     * @return a new instance of decoder
     */
    MasterProcessChannelDecoder createDecoder( @Nonnull ForkNodeArguments forkingArguments ) throws IOException;

    /**
     * Encoder factory method.
     * @param forkingArguments forking arguments
     * @return a new instance of encoder
     */
    MasterProcessChannelEncoder createEncoder( @Nonnull ForkNodeArguments forkingArguments ) throws IOException;
}
