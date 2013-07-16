package testng;

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

import org.testng.annotations.*;
import org.testng.Assert;

public class BasicTest
{

    private boolean setUpCalled = false;

    private static boolean tearDownCalled = false;

    private Integer foo;
    
    @BeforeTest
    public void intialize()
    {
        setUpCalled = true;
        tearDownCalled = false;
        System.out.println( "Called setUp" );
        foo = Integer.valueOf( 1 );
    }

    @AfterTest
    public void shutdown()
    {
        setUpCalled = false;
        tearDownCalled = true;
        System.out.println( "Called tearDown" );
    }

    @Test
    public void testSetUp()
    {
        Assert.assertTrue( setUpCalled );
        Assert.assertNotNull( foo );
    }
    
    
    @Test
    public void testSuccessOne()
    {
        Assert.assertTrue( true );
        Assert.assertNotNull( foo );
    } 
    
    @Test
    public void testSuccessTwo()
    {
        Assert.assertTrue( true );
        Assert.assertNotNull( foo );
    }    

    @AfterClass
    public static void oneTimeTearDown()
    {
        
    }

}
