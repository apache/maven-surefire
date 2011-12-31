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
import org.junit.Before;
import org.junit.Test;

/**
 * @author Kristian Rosenvold
 */
public class SuiteTest2
{
    public SuiteTest2()
    {
        System.out.println( "SuiteTest2.constructor" );
    }

    @Before
    public void setUp()
    {
        System.out.println( "SuiteTest2.setUp" );
    }

    @After
    public void tearDown()
    {
        System.out.println( "SuiteTest2.tearDown" );
    }

    @Test
    public void first()
        throws InterruptedException
    {
        System.out.println( "begin SuiteTest2.first" );
        Thread.sleep( 300 );
        System.out.println( "end SuiteTest2.first" );
    }

    @Test
    public void second()
        throws InterruptedException
    {
        System.out.println( "begin SuiteTest2.second" );
        Thread.sleep( 300 );
        System.out.println( "end SuiteTest2.second" );
    }

    @Test
    public void third()
        throws InterruptedException
    {
        System.out.println( "begin SuiteTest2.third" );
        Thread.sleep( 300 );
        System.out.println( "end SuiteTest2.third" );
    }
}
