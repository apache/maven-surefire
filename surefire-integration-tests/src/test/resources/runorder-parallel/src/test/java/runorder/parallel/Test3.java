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

/**
 * @author Kristian Rosenvold
 */
public class Test3
{

    private void sleep( int ms )
    {
        try
        {
            Thread.sleep( ms );
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Test
    public void testSleep100()
    {
        System.out.println( "Test3.sleep100 started @ " + System.currentTimeMillis() );
        Test1.sleep( 100 );
    }

    @Test
    public void testSleep300()
    {
        System.out.println( "Test3.sleep300 started @ " + System.currentTimeMillis() );
        Test1.sleep( 300 );
    }

    @Test
    public void testSleep500()
    {
        System.out.println( "Test3.sleep500 started @ " + System.currentTimeMillis() );
        Test1.sleep( 500 );
    }

    @BeforeClass
    public static void setUpBeforeClass()
        throws Exception
    {
        System.out.println(
            Thread.currentThread().getName() + " Test3 beforeClass sleep 175 " + System.currentTimeMillis() );
        Thread.sleep( 175 );
    }

    @AfterClass
    public static void tearDownAfterClass()
        throws Exception
    {
        System.out.println(
            Thread.currentThread().getName() + " Test3 afterClass sleep 175 " + System.currentTimeMillis() );
        Thread.sleep( 175 );
    }

}