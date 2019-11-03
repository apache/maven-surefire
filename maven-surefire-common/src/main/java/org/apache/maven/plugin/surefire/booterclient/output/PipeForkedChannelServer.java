package org.apache.maven.plugin.surefire.booterclient.output;

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

import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.AbstractCommandReader;
import org.apache.maven.plugin.surefire.booterclient.lazytestprovider.ForkedChannelServer;
import org.apache.maven.shared.utils.cli.StreamConsumer;
import org.apache.maven.surefire.booter.Command;
import org.apache.maven.surefire.extensions.ForkedChannel;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author <a href="mailto:jon@jonbell.net">Jonathan Bell</a>
 * @since 3.0.0-M4
 */
public class PipeForkedChannelServer extends ForkedChannelServer
{
    private OutputStream outputStream;
    private final ThreadedStreamConsumer threadedStreamConsumer;

    public PipeForkedChannelServer( String channelConfig, ForkClient forkClient, ForkedChannel encoder,
                                    AbstractCommandReader commandSender )
    {
        super( channelConfig, forkClient, encoder, commandSender );
        this.threadedStreamConsumer = new ThreadedStreamConsumer( forkClient );
    }

    public void initialize( Process p )
    {
        this.outputStream = p.getOutputStream();
    }

    public ThreadedStreamConsumer getThreadedStreamConsumer()
    {
        return threadedStreamConsumer;
    }

    @Override
    public void send( Command command ) throws IOException
    {
        byte[] ret = encoder.encode( command );
        if ( ret == null )
        {
            //EOF
            return;
        }
        else
        {
            this.outputStream.write( ret );
        }
        this.outputStream.flush();
    }

    @Override
    public void closeInternal() throws IOException
    {
        this.threadedStreamConsumer.close();

//        if ( this.inputStream != null )
//        {
//            this.inputStream.close();
//        }
//        if ( this.outputStream != null )
//        {
//            this.outputStream.close();
//        }
    }

    private static class EventHandlerAdapter implements StreamConsumer
    {
        private final EventHandler<String> events;

        private EventHandlerAdapter( EventHandler<String> events )
        {
            this.events = events;
        }

        @Override
        public void consumeLine( String line )
        {
            events.handleEvent( line );
        }
    }
}
