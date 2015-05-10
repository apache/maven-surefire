package concurrentjunit47.src.test.java.junit47;

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

import org.junit.*;

import java.util.concurrent.TimeUnit;

public class BasicTest
{
    private boolean setUpCalled = false;

    @Before
    public void setUp()
    {
        setUpCalled = true;
        System.out.println( "Called setUp" );
    }

    @After
    public void tearDown()
    {
        setUpCalled = false;
        System.out.println( "Called tearDown" );
    }

    @Test
    public void testSetUp()
    {
        Assert.assertTrue( "setUp was not called", setUpCalled );
    }

    @Test
    public void a() throws Exception {
        TimeUnit.SECONDS.sleep( 1 );
    }

    @Test
    public void b() throws Exception {
        TimeUnit.SECONDS.sleep( 1 );
    }

    @Test
    public void c() throws Exception {
        TimeUnit.SECONDS.sleep( 1 );
    }

    @AfterClass
    public static void oneTimeTearDown()
    {

    }

}