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

/**
 * Specifies the way how the forked jvm should be terminated after
 * class AbstractCommandStream is closed and CTRL+C.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
public enum Shutdown
{
    DEFAULT( "testset" ),
    EXIT( "exit" ),
    KILL( "kill" );

    private final String param;

    Shutdown( String param )
    {
        this.param = param;
    }

    public String getParam()
    {
        return param;
    }

    public static boolean isKnown( String param )
    {
        for ( Shutdown shutdown : values() )
        {
            if ( shutdown.param.equals( param ) )
            {
                return true;
            }
        }
        return false;
    }

    public static String listParameters()
    {
        StringBuilder values = new StringBuilder();
        for ( Shutdown shutdown : values() )
        {
            if ( values.length() != 0 )
            {
                values.append( ", " );
            }
            values.append( shutdown.getParam() );
        }
        return values.toString();
    }

    public static Shutdown parameterOf( String parameter )
    {
        for ( Shutdown shutdown : values() )
        {
            if ( shutdown.param.equals( parameter ) )
            {
                return shutdown;
            }
        }
        return DEFAULT;
    }
}
