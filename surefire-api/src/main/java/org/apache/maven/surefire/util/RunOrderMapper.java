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


import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A mapper for a Run order object to read them from string representation and back.
 *
 * @author <a href="mailto:krzysztof.suszynski@wavesoftware.pl">Krzysztof Suszynski</a>
 */
@ParametersAreNonnullByDefault
public final class RunOrderMapper
{

    private static final String RUN_ORDERS_DELIMITER = ",";
    private static final String ARGUMENTS_DELIMITER = ":";

    /**
     * Returns the specified RunOrder from provided String representation
     *
     * @param values The runorder string value
     * @return An iterable of RunOrder objects
     */
    public RunOrders fromString( @Nullable String values )
    {
        List<RunOrderWithArguments> result = new ArrayList<RunOrderWithArguments>();
        if ( values != null )
        {
            StringTokenizer stringTokenizer = new StringTokenizer( values, RUN_ORDERS_DELIMITER );
            while ( stringTokenizer.hasMoreTokens() )
            {
                String token = stringTokenizer.nextToken();
                RunOrderWithArguments order = readOneFromString( token );
                result.add( order );
            }
        }
        return new RunOrders( result );
    }

    /**
     * Returns the string representation of provided run order(s)
     *
     * @param runOrders a provided run order(s)
     * @return a string representation of run order
     */
    public String asString( RunOrders runOrders )
    {
        StringBuilder sb = new StringBuilder();
        for ( RunOrderWithArguments order : runOrders.getIterable() )
        {
            sb.append( asString( order ) );
            sb.append( RUN_ORDERS_DELIMITER );
        }
        if ( sb.length() > 0 )
        {
            sb.delete( sb.length() - RUN_ORDERS_DELIMITER.length(), sb.length() );
        }
        return sb.toString();
    }

    Iterable<RunOrder> readWithoutArgumentsFromString( String values )
    {
        RunOrders runOrders = fromString( values );
        List<RunOrder> result = new ArrayList<RunOrder>();
        for ( RunOrderWithArguments order : runOrders.getIterable() )
        {
            result.add( order.getRunOrder() );
        }
        return result;
    }

    private String asString( RunOrderWithArguments order )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( order.getRunOrder().name() );
        for ( String arg : order.getRunOrderArguments().getPositional() )
        {
            sb.append( ARGUMENTS_DELIMITER );
            sb.append( arg );
        }
        return sb.toString();
    }

    private RunOrderWithArguments readOneFromString( String token )
    {
        String[] splited = token.split( ARGUMENTS_DELIMITER );
        String name = splited[0];
        RunOrder runOrder = RunOrder.valueOf( name );
        List<String> asList = new ArrayList<String>( Arrays.asList( splited ) );
        asList.remove( 0 );
        RunOrderArguments arguments = new RunOrderArguments( asList );
        return new RunOrderWithArguments( runOrder, arguments );
    }

}
