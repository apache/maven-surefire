package org.apache.maven.plugin.surefire.booterclient.lazytestprovider;

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

import org.apache.maven.surefire.extensions.CommandReader;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * Stream reader returns bytes which ar finally sent to the forked jvm std-input-stream.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public abstract class AbstractCommandReader
        implements CommandReader, DefferedChannelCommandSender
{
    private volatile FlushReceiverProvider flushReceiverProvider;

    /**
     * @param flushReceiverProvider the provider for a flush receiver.
     */
    @Override
    public void setFlushReceiverProvider( FlushReceiverProvider flushReceiverProvider )
    {
        this.flushReceiverProvider = requireNonNull( flushReceiverProvider );
    }

    @Override
    public void tryFlush()
        throws IOException
    {
        if ( flushReceiverProvider != null )
        {
            FlushReceiver flushReceiver = flushReceiverProvider.getFlushReceiver();
            if ( flushReceiver != null )
            {
                flushReceiver.flush();
            }
        }
    }
}
