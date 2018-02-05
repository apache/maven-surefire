package runorder.parallel;

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

    public Test1()
    {
        System.out.println( Thread.currentThread().getName() + " Constructor" );
    }

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

    @Test
    public void testSleep2000()
    {
        System.out.println(
            Thread.currentThread().getName() + " Test1.sleep2000 started @ " + System.currentTimeMillis() );
        sleep( 2000 );
    }

    @Test
    public void testSleep4000()
    {
        System.out.println(
            Thread.currentThread().getName() + " Test1.sleep4000 started @ " + System.currentTimeMillis() );
        sleep( 4000 );
    }

    @Test
    public void testSleep6000()
    {
        System.out.println(
            Thread.currentThread().getName() + " Test1.sleep6000 started @ " + System.currentTimeMillis() );
        sleep( 6000 );
    }

    @BeforeClass
    public static void setUpBeforeClass()
        throws Exception
    {
        System.out.println( Thread.currentThread().getName() + " beforeClass sleep 175 " + System.currentTimeMillis() );
        Thread.sleep( 175 );
    }

    @AfterClass
    public static void tearDownAfterClass()
        throws Exception
    {
        System.out.println( Thread.currentThread().getName() + " afterClass sleep 175 " + System.currentTimeMillis() );
        Thread.sleep( 175 );
    }


}