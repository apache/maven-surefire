package org.apache.maven.surefire.extensions;

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

import org.apache.maven.surefire.booter.MasterProcessCommand;

/**
 * Commands which are sent from plugin to the forked jvm.
 * <br>
 * <br>
 * magic number : opcode [: opcode specific data]*
 * <br>
 * or data encoded with Base64
 * <br>
 * magic number : opcode [: Base64(opcode specific data)]*
 *
 * The command must be finished by New Line or the character ':'.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public abstract class ForkedChannel
{
    private volatile String channelConfig;

    public String getChannelConfig()
    {
        return channelConfig;
    }

    public void setChannelConfig( String channelConfig )
    {
        this.channelConfig = channelConfig;
    }

    public abstract byte[] encode( MasterProcessCommand command );
    public abstract byte[] encode( MasterProcessCommand command, String data );
}
