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

import org.apache.maven.surefire.util.TestsToRun;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Produces external TestToRun providers based on URL passed in
 *
 * @author Marek Piechut
 */
public class ExternalTestToRunFactory
{

    public static final String SOCKET = "socket";

    static TestsToRun createTestToRun ( String sourceUrl )
                    throws MalformedURLException
    {
        URI url = null;
        try
        {
            url = new URI( sourceUrl );

            if ( SOCKET.equals( url.getScheme() ) )
            {
                return new LazySocketTestToRun( url, 3 );
            }
            else
            {
                throw new IllegalArgumentException( "No support for external test provider with URL: " + url );
            }
        }
        catch ( URISyntaxException e )
        {

            throw new IllegalArgumentException( "No support for external test provider with URL: " + url );
        }
    }

}
