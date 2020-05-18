package org.apache.maven.surefire.api.booter;

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

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * An abstraction for physical decoder of commands. The commands are sent from master Maven process and
 * received by the child forked Surefire process. The session must be open after the MasterProcessChannelDecoderFactory
 * has created the decoder instance. The session can be closed on the decoder instance.
 */
public interface MasterProcessChannelDecoder
    extends AutoCloseable
{
    /**
     * Reads the bytes from a channel, waiting until the command is read completely or
     * the channel throws {@link java.io.EOFException}.
     * <br>
     * This method is called in a single Thread. The constructor can be called within another thread.
     *
     * @return decoded command
     * @throws IOException exception in channel
     */
    @Nonnull Command decode() throws IOException;

    @Override
    void close() throws IOException;
}
