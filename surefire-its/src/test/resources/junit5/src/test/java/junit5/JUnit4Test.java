package junit5;

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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * A test using the JUnit 4 API, which should be executed by JUnit vintage enigne
 */
public class JUnit4Test
{

    private boolean setUpCalled;

    private static boolean tearDownCalled;

    @Before
    public void setUp()
    {
        setUpCalled = true;
        System.out.println( "Called setUp" );
    }

    @After
    public void tearDown()
    {
        tearDownCalled = true;
        System.out.println( "Called tearDown" );
    }

    @Test
    public void testSetUp()
    {
        Assert.assertTrue( "setUp was not called", setUpCalled );
    }

    @AfterClass
    public static void oneTimeTearDown()
    {
        Assert.assertTrue( "tearDown was not called", tearDownCalled );
    }

}
