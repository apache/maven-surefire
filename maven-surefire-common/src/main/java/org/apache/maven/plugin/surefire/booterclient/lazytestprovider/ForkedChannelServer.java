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

import org.apache.maven.plugin.surefire.booterclient.output.ForkClient;
import org.apache.maven.surefire.booter.Command;
import org.apache.maven.surefire.extensions.ForkedChannel;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @author <a href="mailto:jon@jonbell.net">Jonathan Bell</a>
 * @since 3.0.0-M4
 */
public abstract class ForkedChannelServer implements Closeable
{
    private final String channelConfig;
    private final ForkClient forkClient;
    protected final AbstractCommandReader commandSender;
    private final Thread commandPumperThread;
    protected final ForkedChannel encoder;


    public ForkedChannelServer( String channelConfig, ForkClient forkClient, ForkedChannel encoder,
                                AbstractCommandReader commandSender )
    {
        this.channelConfig = channelConfig;
        this.encoder = encoder;
        this.forkClient = forkClient;
        this.commandSender = commandSender;
        this.commandPumperThread = new Thread( new CommandPumper() );
        this.commandPumperThread.start();
    }

    public String getChannelConfig()
    {
        return channelConfig;
    }

    public abstract void send( Command command ) throws IOException;

    @Override
    public final void close() throws IOException
    {
        if ( commandSender != null )
        {
            commandSender.close();
        }
        closeInternal();
    }

    public abstract void closeInternal() throws IOException;

    class CommandPumper implements Runnable
    {
        @Override
        public void run()
        {
            while ( !commandSender.isClosed() )
            {
                try
                {
                    send( commandSender.readNextCommand() );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
