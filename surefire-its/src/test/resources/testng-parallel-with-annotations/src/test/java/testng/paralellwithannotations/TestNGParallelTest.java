package testng.paralellwithannotations;

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


import static org.testng.Assert.*;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test that parallel tests actually run and complete within the expected time.
 */
public class TestNGParallelTest
{
    private static final long DELAY = 3_000L;
    private static final int THREAD_POOL_SIZE = 2;
    private static final int INVOCATION_COUNT = 3;
    private static final AtomicInteger TEST_COUNT = new AtomicInteger();
    private static int testCount = 0;
    private static long startTime;

    @BeforeSuite( alwaysRun = true )
    public void startClock()
    {
        startTime = new Date().getTime();
    }

    @AfterSuite( alwaysRun = true )
    public void checkTestResults()
    {
        long runtime = new Date().getTime() - startTime;
        System.out.println( "Runtime was: " + runtime );
        long testCount = TEST_COUNT.get();
        assertTrue( testCount == INVOCATION_COUNT, "Expected test to be run 3 times, but was " + testCount );
        // Note, this can be < 6000 on Windows.
        assertTrue( runtime < INVOCATION_COUNT * DELAY - 300L,
                "Runtime was " + runtime + ". It should be a little over 3000ms but less than 6000ms." );
    }

    @Test( threadPoolSize = THREAD_POOL_SIZE, invocationCount = INVOCATION_COUNT )
    public void incrementTestCountAndSleepForOneSecond()
        throws InterruptedException
    {
        TEST_COUNT.incrementAndGet();
        Thread.sleep( DELAY );
        System.out.println( "Ran test" );
    }
}
