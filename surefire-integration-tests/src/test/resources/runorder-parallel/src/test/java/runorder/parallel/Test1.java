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
                Thread.sleep( ms );
            }
            while ( System.currentTimeMillis() < target );
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Test
    public void testSleep200()
    {
        System.out.println(
            Thread.currentThread().getName() + " Test1.sleep200 started @ " + System.currentTimeMillis() );
        sleep( 200 );
    }

    @Test
    public void testSleep400()
    {
        System.out.println(
            Thread.currentThread().getName() + " Test1.sleep400 started @ " + System.currentTimeMillis() );
        sleep( 400 );
    }

    @Test
    public void testSleep600()
    {
        System.out.println(
            Thread.currentThread().getName() + " Test1.sleep600 started @ " + System.currentTimeMillis() );
        sleep( 600 );
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