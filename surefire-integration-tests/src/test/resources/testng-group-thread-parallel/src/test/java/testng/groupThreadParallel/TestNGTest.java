package testng.groupThreadParallel;

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

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests grouping/threading/parallel functionality of TestNG.
 * 
 * @author jkuhnert
 */
public class TestNGTest
{

    private static volatile int m_testCount = 0;

    /**
     * Sets up testObject
     */
    @BeforeClass( groups = "functional" )
    public void configureTest()
    {
        testObject = new Object();
    }

    @AfterSuite( alwaysRun = true, groups = "functional" )
    public void check_Test_Count()
    {
        System.out.println( "check_Test_Count(): " + m_testCount );
        Assert.assertEquals( m_testCount, 3 );
    }

    Object testObject;

    @Test( groups = { "functional", "notincluded" } )
    public void test1()
        throws InterruptedException
    {
        doTest( "test1" );
    }

    private void doTest( String test )
        throws InterruptedException
    {
        incrementTestCount();
        System.out.println( "running " + test );
        Assert.assertNotNull( testObject, "testObject" );
        waitForTestCountToBeThree();
    }

    private static synchronized void incrementTestCount()
    {
        m_testCount++;
    }

    @Test( groups = { "functional", "notincluded" } )
    public void test2()
        throws InterruptedException
    {
        doTest( "test2" );
    }

    @Test( groups = { "functional", "notincluded" } )
    public void test3()
        throws InterruptedException
    {
        doTest( "test3" );
    }

    private void waitForTestCountToBeThree()
        throws InterruptedException
    {
        if ( m_testCount == 3 )
            return;
        long now = System.currentTimeMillis();
        long timeout = 5 * 1000;
        long finish = now + timeout;
        while ( m_testCount < 3 && System.currentTimeMillis() < finish )
        {
            Thread.sleep( 10 );
        }
        Assert.assertTrue( m_testCount >= 3, "Expected TestCount >= 3, but was: " + m_testCount );
    }

    /**
     * Sample method that shouldn't be run by test suite.
     */
    @Test( groups = "notincluded" )
    public void shouldNotRun()
    {
        Assert.fail( "Group specified by test shouldnt be run." );
    }
}