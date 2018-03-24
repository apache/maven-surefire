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
import static org.testng.Assert.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TestNGSuccessPercentFailingTest
{

    private static final AtomicInteger counter = new AtomicInteger( 0 );

    // Pass 2 of 4 tests, expect this test to fail when 60% success is required
    @Test( invocationCount = 4, successPercentage = 60 )
    public void testFailure()
    {
        int value = counter.addAndGet( 1 );
        assertTrue( isOdd( value ), "is odd: " + value );
    }

    private boolean isOdd( int number )
    {
        return number % 2 == 0;
    }

}
