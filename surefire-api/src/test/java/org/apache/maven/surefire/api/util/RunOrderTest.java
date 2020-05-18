package org.apache.maven.surefire.api.util;

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

import junit.framework.TestCase;

/**
 *
 */
public class RunOrderTest
    extends TestCase
{
    public void testShouldReturnRunOrderForLowerCaseName()
    {
        assertEquals( RunOrder.HOURLY, RunOrder.valueOfMulti( "hourly" )[0] );
    }

    public void testMultiValue()
    {
        final RunOrder[] hourlies = RunOrder.valueOfMulti( "failedfirst,balanced" );
        assertEquals( RunOrder.FAILEDFIRST, hourlies[0] );
        assertEquals( RunOrder.BALANCED, hourlies[1] );
    }

    public void testAsString()
    {
        RunOrder[] orders = new RunOrder[]{ RunOrder.FAILEDFIRST, RunOrder.ALPHABETICAL };
        assertEquals( "failedfirst,alphabetical", RunOrder.asString( orders ) );
    }

    public void testShouldReturnRunOrderForUpperCaseName()
    {
        assertEquals( RunOrder.HOURLY, RunOrder.valueOfMulti( "HOURLY" )[0] );
    }

    public void testShouldReturnNullForNullName()
    {
        assertTrue( RunOrder.valueOfMulti( null ).length == 0 );
    }

    public void testShouldThrowExceptionForInvalidName()
    {
        try
        {
            RunOrder.valueOfMulti( "arbitraryName" );
            fail( "IllegalArgumentException not thrown." );
        }
        catch ( IllegalArgumentException expected )
        {

        }
    }
}
