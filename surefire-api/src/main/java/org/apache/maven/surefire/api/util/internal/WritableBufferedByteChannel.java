package org.apache.maven.surefire.api.util.internal;

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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Extends {@link WritableByteChannel} with buffered (i.e. non-flushable) write
 * operations, see {@link #writeBuffered(ByteBuffer)}. The messages are buffered
 * and the channel is flushed after the buffer has overflew.
 * <br>
 * The method {@link #write(ByteBuffer)} flushes every written message.
 * You can flush the channel by {@link #write(ByteBuffer) writing} the zero length of {@link ByteBuffer}.
 */
public interface WritableBufferedByteChannel extends WritableByteChannel
{
    void writeBuffered( ByteBuffer src ) throws IOException;
    long countBufferOverflows();
}
