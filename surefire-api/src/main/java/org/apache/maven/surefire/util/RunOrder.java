package org.apache.maven.surefire.util;

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
 * A RunOrder specifies the order in which the tests will be run.
 * 
 * @author Stefan Birkner
 */
public class RunOrder
{
    public static final RunOrder ALPHABETICAL = new RunOrder( "alphabetical" );

    public static final RunOrder FILESYSTEM = new RunOrder( "filesystem" );

    public static final RunOrder HOURLY = new RunOrder( "hourly" );

    public static final RunOrder RANDOM = new RunOrder( "random" );

    public static final RunOrder REVERSE_ALPHABETICAL = new RunOrder( "reversealphabetical" );

    public static RunOrder valueOf( String name )
    {
        if ( name == null )
        {
            return null;
        }
        else
        {
            RunOrder[] runOrders = values();
            for ( int i = 0; i < runOrders.length; i++ )
            {
                if ( runOrders[i].matches( name ) )
                {
                    return runOrders[i];
                }
            }

            StringBuffer errorMessage = createMessageForMissingRunOrder( name );
            throw new IllegalArgumentException( errorMessage.toString() );
        }
    }

    private static StringBuffer createMessageForMissingRunOrder( String name )
    {
        RunOrder[] runOrders = values();
        StringBuffer message = new StringBuffer();
        message.append( "There's no RunOrder with the name " );
        message.append( name );
        message.append( ". Please use one of the following RunOrders: " );
        for ( int i = 0; i < runOrders.length; i++ )
        {
            if ( i != 0 )
            {
                message.append( ", " );
            }
            message.append( runOrders[i] );
        }
        message.append( "." );
        return message;
    }

    private static RunOrder[] values()
    {
        return new RunOrder[] { ALPHABETICAL, FILESYSTEM, HOURLY, RANDOM, REVERSE_ALPHABETICAL };
    }

    private final String name;

    private RunOrder( String name )
    {
        this.name = name;
    }

    private boolean matches( String anotherName )
    {
        return name.equalsIgnoreCase( anotherName );
    }

    public String name()
    {
        return name;
    }

    public String toString()
    {
        return name;
    }
}