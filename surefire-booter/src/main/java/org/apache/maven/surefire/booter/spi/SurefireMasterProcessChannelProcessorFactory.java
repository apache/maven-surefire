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

import org.apache.maven.surefire.api.booter.MasterProcessChannelDecoder;
import org.apache.maven.surefire.api.booter.MasterProcessChannelEncoder;
import org.apache.maven.surefire.api.fork.ForkNodeArguments;
import org.apache.maven.surefire.api.util.internal.WritableBufferedByteChannel;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketOption;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;

import static java.net.StandardSocketOptions.SO_KEEPALIVE;
import static java.net.StandardSocketOptions.SO_REUSEADDR;
import static java.net.StandardSocketOptions.TCP_NODELAY;
import static java.nio.channels.AsynchronousChannelGroup.withFixedThreadPool;
import static java.nio.channels.AsynchronousSocketChannel.open;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.apache.maven.surefire.api.util.internal.Channels.newBufferedChannel;
import static org.apache.maven.surefire.api.util.internal.Channels.newInputStream;
import static org.apache.maven.surefire.api.util.internal.Channels.newOutputStream;
import static org.apache.maven.surefire.api.util.internal.DaemonThreadFactory.newDaemonThreadFactory;

/**
 * Producer of TCP/IP encoder and decoder.
 * <br>
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M5
 */
public class SurefireMasterProcessChannelProcessorFactory
    extends AbstractMasterProcessChannelProcessorFactory
{
    private static final int FLUSH_PERIOD_MILLIS = 100;
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
            throw new MalformedURLException( "Unknown channel string " + channelConfig );
        }

        try
        {
            URI uri = new URI( channelConfig );
            InetSocketAddress hostAddress = new InetSocketAddress( uri.getHost(), uri.getPort() );
            clientSocketChannel = open( withFixedThreadPool( 2, newDaemonThreadFactory() ) );
            setTrueOptions( SO_REUSEADDR, TCP_NODELAY, SO_KEEPALIVE );
            clientSocketChannel.connect( hostAddress ).get();
            String sessionId = extractSessionId( uri );
            if ( sessionId != null )
            {
                ByteBuffer buff = ByteBuffer.wrap( sessionId.getBytes( US_ASCII ) );
                while ( buff.hasRemaining() )
                {
                    clientSocketChannel.write( buff ).get();
                }
            }
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
    public MasterProcessChannelDecoder createDecoder( @Nonnull ForkNodeArguments forkingArguments )
    {
        ReadableByteChannel bufferedChannel = newBufferedChannel( newInputStream( clientSocketChannel ) );
        return new CommandChannelDecoder( bufferedChannel, forkingArguments );
    }

    @Override
    public MasterProcessChannelEncoder createEncoder( @Nonnull ForkNodeArguments forkingArguments )
    {
        WritableBufferedByteChannel channel = newBufferedChannel( newOutputStream( clientSocketChannel ) );
        schedulePeriodicFlusher( FLUSH_PERIOD_MILLIS, channel );
        return new EventChannelEncoder( channel );
    }

    @Override
    public void close() throws IOException
    {
        super.close();
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

    private static String extractSessionId( URI uri )
    {
        String query = uri.getQuery();
        if ( query == null )
        {
            return null;
        }
        for ( StringTokenizer tokenizer = new StringTokenizer( query, "&" ); tokenizer.hasMoreTokens(); )
        {
            String token = tokenizer.nextToken();
            int delimiter = token.indexOf( '=' );
            if ( delimiter != -1 && "sessionId".equals( token.substring( 0, delimiter ) ) )
            {
                return token.substring( delimiter + 1 );
            }
        }
        return null;
    }
}
