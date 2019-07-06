package consoleoutput_noisy;

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

import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class Test1
{

    public static final int thousand = Integer.parseInt( System.getProperty( "thousand", "1000" ) );

    @Test
    public void test1MillionBytes()
    {
        System.out.println( "t1 = " + System.currentTimeMillis() );
        for ( int i = 0; i < ( 10 * thousand ); i++ )
        {
            System.out.println( "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" );
        }
        System.out.println( "t2 = " + System.currentTimeMillis() );
    }

    @Test
    public void testHundredThousand()
    {
        printAlot();
    }

    private static void printAlot()
    {
        for ( int i = 0; i < thousand; i++ )
        {
            System.out.println( "AAAAAAAAAABBBBBBBBBBCCCCCCCCCCDDDDDDDDDDEEEEEEEEEEFFFFFFFFFFGGGGGGGGGGHHHHHHHHHHIIIIIIIIIIJJJJJJJJJJ" );
        }
    }

    @Test
    public void testAnotherHundredThousand()
    {
        printAlot();
    }

    @Before
    public void before()
    {
        printAlot();
    }

    @BeforeClass
    public static void beforeClass()
    {
        printAlot();
    }

    @After
    public void after()
    {
        printAlot();
    }

    @AfterClass
    public static void afterClass()
    {
        printAlot();
    }
}
