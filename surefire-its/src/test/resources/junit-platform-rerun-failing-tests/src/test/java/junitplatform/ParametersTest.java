package junitplatform;

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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;


public class ParametersTest
{
    public static Stream<ConnectionPoolFactory> pools()
    {
        return Stream.of( new ConnectionPoolFactory( "duplex" ),
            new ConnectionPoolFactory( "multiplex" ),
            new ConnectionPoolFactory( "round-robin" ) );
    }

    @ParameterizedTest
    @MethodSource( "pools" )
    public void testAllPassingTest( ConnectionPoolFactory factory )
    {
        System.out.println( "testAllPassingTest factory " + factory );
    }

    @ParameterizedTest
    @MethodSource( "pools" )
    public void testOneFailingPassingTest( ConnectionPoolFactory factory ) throws Exception
    {
        Assumptions.assumeFalse( factory.name.equals( "round-robin" ) );
        System.out.println( "Passing test factory " + factory );
        if ( factory.name.equals( "multiplex" ) )
        {
            assertEquals( 1, 2 );
        }
    }

    private static class ConnectionPoolFactory
    {
        private final String name;

        private ConnectionPoolFactory( String name )
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }
}
