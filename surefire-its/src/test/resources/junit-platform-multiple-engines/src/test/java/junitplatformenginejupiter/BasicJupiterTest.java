package junitplatformenginejupiter;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BasicJupiterTest
{

    private boolean setUpCalled;

    private static boolean tearDownCalled;

    @BeforeEach
    void setUp()
    {
        setUpCalled = true;
        tearDownCalled = false;
        System.out.println( "Called setUp" );
    }

    @AfterEach
    void tearDown()
    {
        setUpCalled = false;
        tearDownCalled = true;
        System.out.println( "Called tearDown" );
    }

    @Test
    void test(TestInfo info)
    {
        assertTrue( setUpCalled, "setUp was not called" );
        assertEquals( "test(TestInfo)", info.getDisplayName(), "display name mismatch" );
    }

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource({
                    "0,    1,   1",
                    "1,    2,   3",
                    "49,  51, 100",
                    "1,  100, 101"
    })
    void add(int first, int second, int expectedResult)
    {
        assertEquals(expectedResult, first + second, () -> first + " + " + second + " should equal " + expectedResult);
    }


    @AfterAll
    static void oneTimeTearDown()
    {
        assertTrue( tearDownCalled );
    }

}
