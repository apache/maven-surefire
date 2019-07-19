package org.apache.maven.surefire.util.internal;

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

import org.apache.maven.surefire.util.Randomizer;

/**
 * @author <a href="mailto:krzysztof.suszynski@wavesoftware.pl">Krzysztof Suszynski</a>
 * @since 2016-04-11
 */
public final class RandomizerSerializer
{
    private static final String SPLITTER = "\u001E\u0003";

    private RandomizerSerializer()
    {
        // nothing
    }

    /**
     * Deserialize a string into randomizer object
     * @param serialized a serialized
     * @return a randomizer object
     */
    public static Randomizer deserialize( String serialized )
    {
        String[] parts = serialized.split( SPLITTER, 2 );
        if ( parts.length == 2 )
        {
            return new Randomizer( parts[0], Long.parseLong( parts[1], 10 ) );
        }
        else
        {
            return new Randomizer( parts[0] );
        }
    }

    /**
     * Serialize the randomizer into string format
     *
     * @param randomizer a randomizer object
     * @return a stringed form
     */
    public static String serialize( Randomizer randomizer )
    {
        return String.format( "%s%s%s", randomizer.getGivenSeed(), SPLITTER, randomizer.getSeed() );
    }
}
