package org.apache.maven.plugin.surefire.runorder.impl;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.maven.plugin.surefire.runorder.StringUtil;
import org.apache.maven.plugin.surefire.runorder.api.RunOrder;
import org.apache.maven.plugin.surefire.runorder.spi.RunOrderProvider;

import static java.lang.Thread.currentThread;
import static org.apache.maven.plugin.surefire.runorder.impl.PriorityComparator.byPriority;

/**
 * RunOrderLoader gives you instances of RunOrder provided by implementation of
 * RunOrderProvider found on classpath which is used to order test classes.
 *
 * @author Dipak Pawar
 */
public class RunOrderLoader
{

    private static RunOrderProvider runOrderProvider;

    /**
     * Returns the specified RunOrders
     *
     * @param values the runorder names in comma separated format
     * @return the instances of RunOrder implementation, never null
     */
    public static RunOrder[] runOrdersOf( String values )
    {
        List<RunOrder> result = new ArrayList<RunOrder>();
        if ( values != null )
        {
            final RunOrder[] runOrders = values();
            final String[] runOrderValues = values.split( "\\s*,\\s*" );
            for ( String runOrder : runOrderValues )
            {
                result.add( valueOf( runOrder,  runOrders ) );
            }
        }
        return result.toArray( new RunOrder[result.size()] );
    }

    /**
     * Scans classpath for implementation of RunOrderProvider with higher priority
     * If not found, default RunOrderProvider {@link DefaultRunOrderProvider} is used.
     *
     * @return An implementation of RunOrderProvider
     */
    public static RunOrderProvider getRunOrderProvider()
    {
        if ( runOrderProvider == null )
        {
            final ServiceLoader<RunOrderProvider> runOrderProviderLoader =
                    ServiceLoader.load( RunOrderProvider.class, currentThread().getContextClassLoader() );

            final Set<RunOrderProvider> implementations = new HashSet<RunOrderProvider>();

            for ( RunOrderProvider providerLoader : runOrderProviderLoader )
            {
                implementations.add( providerLoader );
            }

            if ( implementations.isEmpty() )
            {
                RunOrderLoader.runOrderProvider = new DefaultRunOrderProvider();
            }
            else
            {
                RunOrderLoader.runOrderProvider = Collections.max( implementations, byPriority() );
            }
        }

        return runOrderProvider;
    }

    public static RunOrder[] asArray( Collection<RunOrder> runOrders )
    {
        return runOrders.toArray( new RunOrder[runOrders.size()] );
    }

    public static RunOrder[] defaultRunOrder()
    {
        return asArray( getRunOrderProvider().defaultRunOrder() );
    }

    public static String asString( RunOrder[] runOrders )
    {
        return StringUtil.joins( runOrders );
    }

    public static String asString( Collection<RunOrder> runOrders )
    {
        return asString( runOrders.toArray( new RunOrder[runOrders.size()] ) );
    }

    private static RunOrder valueOf( String name, RunOrder[] runOrders )
    {
        if ( name == null )
        {
            return null;
        }

        for ( RunOrder runOrder : runOrders )
        {
            if ( name.equalsIgnoreCase( runOrder.getName() ) )
            {
                return runOrder;
            }
        }

        String errorMessage = createMessageForMissingRunOrder( name );
        throw new IllegalArgumentException( errorMessage );
    }

    private static String createMessageForMissingRunOrder( String name )
    {
        return "There's no RunOrder with the name "
                + name
                + ". Please use one of the following RunOrders: "
                + StringUtil.joins( values() ) + '.';
    }

    private static RunOrder[] values()
    {
        final Collection<RunOrder> runOrders = getRunOrderProvider().getRunOrders();

        return runOrders.toArray( new RunOrder[runOrders.size()] );
    }
}
