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
import java.net.MalformedURLException;

import static org.apache.maven.surefire.util.internal.Channels.newBufferedChannel;

/**
 * Producer of encoder and decoder for process pipes.
 * <br>
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M5
 */
public class LegacyMasterProcessChannelProcessorFactory
    implements MasterProcessChannelProcessorFactory
{
    @Override
    public boolean canUse( String channelConfig )
    {
        return channelConfig.startsWith( "pipe://" );
    }

    @Override
    public void connect( String channelConfig ) throws IOException
    {
        if ( !canUse( channelConfig ) )
        {
            throw new MalformedURLException( "Unknown chanel string " + channelConfig );
        }
    }

    @Override
    public MasterProcessChannelDecoder createDecoder()
    {
        return new LegacyMasterProcessChannelDecoder( newBufferedChannel( System.in ) );
    }

    @Override
    public MasterProcessChannelEncoder createEncoder()
    {
        return new LegacyMasterProcessChannelEncoder( newBufferedChannel( System.out ) );
    }

    @Override
    public void close()
    {
    }
}
