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

import static org.junit.jupiter.api.Assertions.fail;


public class FlakyFirstTimeTest
{
    private static int failingCount = 0;

    private static int errorCount = 0;


    @Test
    public void testFailingTestOne()
    {
        System.out.println( "Failing test" );
        // This test will fail with only one retry, but will pass with two
        if ( failingCount < 2 )
        {
            failingCount++;
            fail( "Failing test" );
        }
    }

    @Test
    public void testErrorTestOne() throws Exception
    {
        System.out.println( "Error test" );
        // This test will error out with only one retry, but will pass with two
        if ( errorCount < 2 )
        {
            errorCount++;
            throw new IllegalArgumentException( "..." );
        }
    }

    @Test
    public void testPassingTest() throws Exception
    {
        System.out.println( "Passing test" );
    }
}
