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

import junit.framework.TestCase;

public class Test2
    extends TestCase
{
    public void test2MillionBytes()
    {
        for ( int i = 0; i < 20 * Test1.thousand; i++ )
        {
            System.out.println(
                "0-2-3-6-8-012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" );
        }
    }

    public static void testHundredThousand()
    {
        for ( int i = 0; i < Test1.thousand; i++ )
        {
            System.out.println(
                "A-A-3-A-A-BBBBBBBBBBCCCCCCCCCCDDDDDDDDDDEEEEEEEEEEFFFFFFFFFFGGGGGGGGGGHHHHHHHHHHIIIIIIIIIIJJJJJJJJJJ" );
        }
    }

    public static void testAnotherHundredThousand()
    {
        for ( int i = 0; i < Test1.thousand; i++ )
        {
            System.out.println(
                "A-A-A-3-3-ABBBBBBBBBCCCCCCCCCCDDDDDDDDDDEEEEEEEEEEFFFFFFFFFFGGGGGGGGGGHHHHHHHHHHIIIIIIIIIIJJJJJJJJJJ" );
        }
    }
}
