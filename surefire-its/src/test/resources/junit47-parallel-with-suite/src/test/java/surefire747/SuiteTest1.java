package surefire747;

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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Kristian Rosenvold
 */
public class SuiteTest1
{
    private static final int PERFORMANCE_TEST_MULTIPLICATION_FACTOR = 4;

    private static long startedAt;

    public SuiteTest1()
    {
        System.out.println( "SuiteTest1.constructor" );
    }

    @BeforeClass
    public static void beforeClass()
    {
        startedAt = System.currentTimeMillis();
    }

    @AfterClass
    public static void afterClass()
    {
        System.out.println( String.format( "%s test finished after duration=%d", SuiteTest1.class.getSimpleName(),
                                           System.currentTimeMillis() - startedAt ) );
    }

    @Before
    public void setUp()
    {
        System.out.println( "SuiteTest1.setUp" );
    }

    @After
    public void tearDown()
    {
        System.out.println( "SuiteTest1.tearDown" );
    }

    @Test
    public void first()
        throws InterruptedException
    {
        System.out.println( "begin SuiteTest1.first" );
        Thread.sleep( 500 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR );
        System.out.println( "end SuiteTest1.first" );
    }

    @Test
    public void second()
        throws InterruptedException
    {
        System.out.println( "begin SuiteTest1.second" );
        Thread.sleep( 500 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR );
        System.out.println( "end SuiteTest1.second" );
    }

    @Test
    public void third()
        throws InterruptedException
    {
        System.out.println( "begin SuiteTest1.third" );
        Thread.sleep( 500 * PERFORMANCE_TEST_MULTIPLICATION_FACTOR );
        System.out.println( "end SuiteTest1.third" );
    }
}
