package org.apache.maven.plugin.surefire.runorder.api;

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
import org.apache.maven.plugin.surefire.runorder.impl.RunOrderLoader;

import static org.apache.maven.plugin.surefire.runorder.model.RunOrderFactory.ALPHABETICAL;
import static org.apache.maven.plugin.surefire.runorder.model.RunOrderFactory.BALANCED;
import static org.apache.maven.plugin.surefire.runorder.model.RunOrderFactory.FAILEDFIRST;
import static org.apache.maven.plugin.surefire.runorder.model.RunOrderFactory.HOURLY;

public class RunOrderTest
    extends TestCase
{
    public void testShouldReturnRunOrderForLowerCaseName()
    {
        assertEquals( HOURLY, RunOrderLoader.runOrdersOf( "hourly" )[0] );
    }

    public void testMultiValue()
    {
        final RunOrder[] hourlies = RunOrderLoader.runOrdersOf( "failedfirst,balanced" );
        assertEquals( FAILEDFIRST, hourlies[0] );
        assertEquals( BALANCED, hourlies[1] );
    }

    public void testAsString()
    {
        RunOrder[] orders = new RunOrder[]{ FAILEDFIRST, ALPHABETICAL };
        assertEquals( "failedfirst,alphabetical", RunOrderLoader.asString( orders ) );
    }

    public void testShouldReturnRunOrderForUpperCaseName()
    {
        assertEquals( HOURLY, RunOrderLoader.runOrdersOf( "HOURLY" )[0] );
    }

    public void testShouldReturnNullForNullName()
    {
        assertTrue( RunOrderLoader.runOrdersOf( null ).length == 0 );
    }

    public void testShouldThrowExceptionForInvalidName()
    {
        try
        {
            RunOrderLoader.runOrdersOf( "arbitraryName" );
            fail( "IllegalArgumentException not thrown." );
        }
        catch ( IllegalArgumentException expected )
        {

        }
    }
}