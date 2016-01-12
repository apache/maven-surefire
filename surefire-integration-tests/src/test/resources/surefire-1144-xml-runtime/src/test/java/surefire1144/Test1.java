package surefire1144;

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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Test1
{
    static void sleep( int ms )
    {
        long target = System.currentTimeMillis() + ms;
        try
        {
            do
            {
                Thread.sleep( 1L );
            }
            while ( System.currentTimeMillis() < target );
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e );
        }
    }

    static void printTimeAndSleep( String msg, int ms )
    {
        System.out.println( msg + " started @ " + System.currentTimeMillis() );
        sleep( ms );
    }

    @Test
    public void testSleep100()
    {
        printTimeAndSleep( "Test1.sleep100", 100 );
    }

    @Test
    public void testSleep200()
    {
        printTimeAndSleep( "Test1.sleep200", 200 );
    }

    @Test
    public void testSleep300()
    {
        printTimeAndSleep( "Test1.sleep300", 300 );
    }

    @BeforeClass
    public static void setUpBeforeClass()
        throws Exception
    {
        printTimeAndSleep( "beforeClass sleep 500", 500 );
    }

    @AfterClass
    public static void tearDownAfterClass()
        throws Exception
    {
        printTimeAndSleep( "afterClass sleep 500", 500 );
    }
}
