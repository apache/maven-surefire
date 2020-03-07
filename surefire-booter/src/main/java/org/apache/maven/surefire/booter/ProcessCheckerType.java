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

import static org.apache.maven.surefire.shared.utils.StringUtils.isBlank;

/**
 *
 */
public enum ProcessCheckerType
{
    PING( "ping" ),
    NATIVE( "native" ),
    ALL( "all" );

    private final String type;

    ProcessCheckerType( String type )
    {
        this.type = type;
    }

    /**
     * Converts string (ping, native, all) to {@link ProcessCheckerType}.
     *
     * @param type ping, native, all
     * @return {@link ProcessCheckerType}
     */
    public static ProcessCheckerType toEnum( String type )
    {
        if ( isBlank( type ) )
        {
            return null;
        }

        for ( ProcessCheckerType e : values() )
        {
            if ( e.type.equals( type ) )
            {
                return e;
            }
        }

        throw new IllegalArgumentException( "unknown process checker" );
    }

    public String getType()
    {
        return type;
    }

    public static boolean isValid( String type )
    {
        try
        {
            toEnum( type );
            return true;
        }
        catch ( IllegalArgumentException e )
        {
            return false;
        }
    }
}
