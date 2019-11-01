package org.apache.maven.surefire.booter.spi;

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

import org.apache.maven.surefire.providerapi.MasterProcessChannel;
import org.apache.maven.surefire.spi.MasterProcessChannelFactory;

import java.io.IOException;
import java.net.Socket;

/**
 * @author <a href="mailto:jon@jonbell.net">Jonathan Bell</a>
 * @since 3.0.0-M4
 */
public class DefaultMasterProcessChannelFactory implements MasterProcessChannelFactory
{
    @Override
    public MasterProcessChannel createChannel( String channelConfig ) throws IOException
    {
        if ( "pipe:std:in".equals( channelConfig ) )
        {
            return new MasterProcessChannel( System.in, System.out );
        }
        else if ( channelConfig != null && channelConfig.startsWith( "tcp://" ) )
        {
            String addr = channelConfig.substring( 6 ); //"tcp://"
            String ip = addr.substring( 0, addr.indexOf( '/' ) );
            String port = addr.substring( addr.indexOf( '/' ) + 1 );
            Socket socket = new Socket( ip, Integer.parseInt( port ) );
            socket.setTcpNoDelay( true );
            return new MasterProcessChannel( socket.getInputStream(), socket.getOutputStream(), socket );
        }
        else
        {
            throw new IOException( "Unknown channel configuration string " + channelConfig );
        }
    }
}
