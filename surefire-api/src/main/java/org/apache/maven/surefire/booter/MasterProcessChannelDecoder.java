package org.apache.maven.surefire.booter;

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

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * magic number : opcode [: opcode specific data]*
 * <br>
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 3.0.0-M4
 */
public class MasterProcessChannelDecoder
{
    public Command decode( InputStream is, ConsoleLogger logger ) throws IOException
    {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean frameStarted = false;
        boolean frameFinished = false;
        for ( int r; ( r = is.read() ) != -1 ; )
        {
            char c = (char) r;
            if ( frameFinished && c == '\n' )
            {
                continue;
            }

            if ( !frameStarted )
            {
                if ( c == ':' )
                {
                    frameStarted = true;
                    frameFinished = false;
                    token.setLength( 0 );
                    tokens.clear();
                    continue;
                }
            }
            else if ( !frameFinished )
            {
                boolean isFinishedFrame = c == ':' && isTokenComplete( tokens ) || c == '\n' || c == '\r';
                if ( isFinishedFrame || c == ':' )
                {
                    tokens.add( token.toString() );
                    token.setLength( 0 );
                    if ( isFinishedFrame )
                    {
                        frameFinished = true;
                        frameStarted = false;
                        break;
                    }
                }
                else
                {
                    token.append( c );
                }
            }

            boolean removed = removeUnsynchronizedTokens( tokens, logger );
            if ( removed && tokens.isEmpty() )
            {
                frameStarted = false;
                frameFinished = true;
            }
        }
        return new Command( MasterProcessCommand.byOpcode( tokens.get( 1 ) ) );//todo
    }

    private static boolean isTokenComplete( List<String> tokens )
    {
        if ( tokens.size() >= 2 )
        {
            MasterProcessCommand cmd = MasterProcessCommand.byOpcode( tokens.get( 1 ) );
            // todo maybe throw exc
            return cmd.hasDataType() || tokens.size() == 3;
        }
        return false;
    }

    private boolean removeUnsynchronizedTokens( Collection<String> tokens, ConsoleLogger logger )
    {
        boolean removed = false;
        for ( Iterator<String> it = tokens.iterator(); it.hasNext(); )
        {
            String token = it.next();
            if ( token.equals( "maven-surefire-std-out" ) )
            {
                break;
            }
            removed = true;
            it.remove();
            logger.error( "Forked JVM could not synchronize the '" + token + "' token with preamble sequence." );
        }
        return removed;
    }
}
