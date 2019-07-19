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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:krzysztof.suszynski@wavesoftware.pl">Krzysztof Suszynski</a>
 * @since 2018-05-27
 */
public class RunOrderMapperTest
{

    private final RunOrderMapper mapper = new RunOrderMapper();

    @Test
    public void testShouldReturnRunOrderForLowerCaseName()
    {
        assertEquals( RunOrder.HOURLY, mapper.fromString( "hourly" ).firstAsType() );
    }

    @Test
    public void testMultiValue()
    {
        RunOrders hourlies = mapper.fromString( "failedfirst,balanced" );
        Iterator<RunOrderWithArguments> iterator = hourlies.getIterable().iterator();
        RunOrderWithArguments first = iterator.next();
        RunOrderWithArguments second = iterator.next();
        assertFalse( iterator.hasNext() );
        assertEquals( RunOrder.FAILEDFIRST, first.getRunOrder() );
        assertEquals( RunOrder.BALANCED, second.getRunOrder() );
    }

    @Test
    public void testRandom()
    {
        RunOrders orders = mapper.fromString( "random" );
        int elems = 0;
        for ( RunOrderWithArguments ignored : orders.getIterable() )
        {
            elems++;
        }
        assertEquals( 1, elems );
        assertEquals( RunOrder.RANDOM, orders.firstAsType() );
    }

    @Test
    public void testAsString()
    {
        RunOrders orders = new RunOrders( RunOrder.FAILEDFIRST, RunOrder.ALPHABETICAL );
        assertEquals( "failedfirst,alphabetical", mapper.asString( orders ) );
    }

    @Test
    public void testShouldReturnRunOrderForUpperCaseName()
    {
        assertEquals( RunOrder.HOURLY, mapper.fromString( "HOURLY" ).firstAsType() );
    }

    @Test
    public void testShouldReturnNullForNullName()
    {
        assertFalse( mapper.fromString( null ).any() );
    }

    @Test
    public void testShouldThrowExceptionForInvalidName()
    {
        try
        {
            mapper.fromString( "arbitraryName" );
            fail( "IllegalArgumentException not thrown." );
        }
        catch ( IllegalArgumentException expected )
        {

        }
    }

    @Test
    public void testFromStringWithArguments()
    {
        // given
        String repr = "random:123322,failedfirst";

        // when
        RunOrders orders = mapper.fromString( repr );
        List<RunOrderWithArguments> asList = new ArrayList<RunOrderWithArguments>();
        for ( RunOrderWithArguments order : orders.getIterable() )
        {
            asList.add( order );
        }
        RunOrderArguments args = orders.getArguments( RunOrder.RANDOM );
        Iterator<String> positional = args.getPositional().iterator();

        // then
        assertEquals( 2, asList.size() );
        assertEquals( RunOrder.RANDOM, orders.firstAsType() );
        assertEquals( "123322", positional.next() );
        assertFalse( positional.hasNext() );
        assertEquals( RunOrder.FAILEDFIRST, asList.get( 1 ).getRunOrder() );
    }

    @Test
    public void testReadWithoutArgumentsFromString()
    {
        // given
        String repr = "random:123322,failedfirst";

        // when
        Iterable<RunOrder> orders = mapper.readWithoutArgumentsFromString( repr );
        List<RunOrder> runOrders = new ArrayList<RunOrder>();
        for ( RunOrder order : orders )
        {
            runOrders.add( order );
        }

        // then
        assertEquals( 2, runOrders.size() );
        assertEquals( RunOrder.RANDOM, runOrders.get( 0 ) );
        assertEquals( RunOrder.FAILEDFIRST, runOrders.get( 1 ) );
    }
}