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

import org.apache.maven.surefire.booter.MasterProcessChannelDecoder;
import org.apache.maven.surefire.booter.MasterProcessChannelEncoder;
import org.apache.maven.surefire.spi.MasterProcessChannelProcessorFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketOption;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;

import static java.net.StandardSocketOptions.SO_KEEPALIVE;
import static java.net.StandardSocketOptions.SO_REUSEADDR;
import static java.net.StandardSocketOptions.TCP_NODELAY;
import static java.nio.channels.AsynchronousChannelGroup.withFixedThreadPool;
import static java.nio.channels.AsynchronousSocketChannel.open;
import static org.apache.maven.surefire.util.internal.Channels.newBufferedChannel;
import static org.apache.maven.surefire.util.internal.Channels.newInputStream;
import static org.apache.maven.surefire.util.internal.Channels.newOutputStream;
import static org.apache.maven.surefire.util.internal.DaemonThreadFactory.newDaemonThreadFactory;

/**
 * Producer of TCP/IP encoder and decoder.
 * <br>
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M5
 */
public class SurefireMasterProcessChannelProcessorFactory
    implements MasterProcessChannelProcessorFactory
{
    private volatile AsynchronousSocketChannel clientSocketChannel;

    @Override
    public boolean canUse( String channelConfig )
    {
        return channelConfig.startsWith( "tcp://" );
    }

    @Override
    public void connect( String channelConfig ) throws IOException
    {
        if ( !canUse( channelConfig ) )
        {
            throw new MalformedURLException( "Unknown chanel string " + channelConfig );
        }

        try
        {
            URI uri = new URI( channelConfig );
            InetSocketAddress hostAddress = new InetSocketAddress( uri.getHost(), uri.getPort() );
            clientSocketChannel = open( withFixedThreadPool( 2, newDaemonThreadFactory() ) );
            setTrueOptions( SO_REUSEADDR, TCP_NODELAY, SO_KEEPALIVE );
            clientSocketChannel.connect( hostAddress ).get();
        }
        catch ( URISyntaxException | InterruptedException e )
        {
            throw new IOException( e.getLocalizedMessage(), e );
        }
        catch ( ExecutionException e )
        {
            throw new IOException( e.getLocalizedMessage(), e.getCause() );
        }
    }

    @Override
    public MasterProcessChannelDecoder createDecoder()
    {
        return new LegacyMasterProcessChannelDecoder( newBufferedChannel( newInputStream( clientSocketChannel ) ) );
    }

    @Override
    public MasterProcessChannelEncoder createEncoder()
    {
        return new LegacyMasterProcessChannelEncoder( newBufferedChannel( newOutputStream( clientSocketChannel ) ) );
    }

    @Override
    public void close() throws IOException
    {
        if ( clientSocketChannel != null && clientSocketChannel.isOpen() )
        {
            clientSocketChannel.close();
        }
    }

    @SafeVarargs
    private final void setTrueOptions( SocketOption<Boolean>... options )
        throws IOException
    {
        for ( SocketOption<Boolean> option : options )
        {
            if ( clientSocketChannel.supportedOptions().contains( option ) )
            {
                clientSocketChannel.setOption( option, true );
            }
        }
    }
}
