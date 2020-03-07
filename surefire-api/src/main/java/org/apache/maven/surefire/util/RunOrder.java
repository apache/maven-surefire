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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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

    public static final RunOrder BALANCED = new RunOrder( "balanced" );

    public static final RunOrder FAILEDFIRST = new RunOrder( "failedfirst" );

    public static final RunOrder[] DEFAULT = new RunOrder[]{ FILESYSTEM };

    /**
     * Returns the specified RunOrder
     *
     * @param values The runorder string value
     * @return An array of RunOrder objects, never null
     */
    public static RunOrder[] valueOfMulti( String values )
    {
        List<RunOrder> result = new ArrayList<>();
        if ( values != null )
        {
            StringTokenizer stringTokenizer = new StringTokenizer( values, "," );
            while ( stringTokenizer.hasMoreTokens() )
            {
                result.add( valueOf( stringTokenizer.nextToken() ) );
            }
        }
        return result.toArray( new RunOrder[result.size()] );
    }

    public static RunOrder valueOf( String name )
    {
        if ( name == null )
        {
            return null;
        }
        else
        {
            RunOrder[] runOrders = values();
            for ( RunOrder runOrder : runOrders )
            {
                if ( runOrder.matches( name ) )
                {
                    return runOrder;
                }
            }

            String errorMessage = createMessageForMissingRunOrder( name );
            throw new IllegalArgumentException( errorMessage );
        }
    }

    private static String createMessageForMissingRunOrder( String name )
    {
        RunOrder[] runOrders = values();
        StringBuilder message = new StringBuilder( "There's no RunOrder with the name " );
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
        message.append( '.' );
        return message.toString();
    }

    private static RunOrder[] values()
    {
        return new RunOrder[]{ ALPHABETICAL, FILESYSTEM, HOURLY, RANDOM, REVERSE_ALPHABETICAL, BALANCED, FAILEDFIRST };
    }

    public static String asString( RunOrder[] runOrder )
    {
        StringBuilder stringBuffer = new StringBuilder();
        for ( int i = 0; i < runOrder.length; i++ )
        {
            stringBuffer.append( runOrder[i].name );
            if ( i < ( runOrder.length - 1 ) )
            {
                stringBuffer.append( "," );
            }
        }
        return stringBuffer.toString();

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

    @Override
    public String toString()
    {
        return name;
    }
}