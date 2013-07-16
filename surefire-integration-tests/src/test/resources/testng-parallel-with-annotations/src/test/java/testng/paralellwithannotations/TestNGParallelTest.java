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

/**
 * Test that parallel tests actually run and complete within the expected time.
 */
public class TestNGParallelTest
{

    static int testCount = 0;

    static long startTime;

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
        assertTrue( testCount == 3, "Expected test to be run 3 times, but was " + testCount );
        // Note, this can be < 1000 on Windows.
        assertTrue( runtime < 1400, "Runtime was " + runtime + ". It should be a little over 1000ms" );
    }

    @Test( threadPoolSize = 2, invocationCount = 3 )
    public void incrementTestCountAndSleepForOneSecond()
        throws InterruptedException
    {
        incrementTestCount();
        Thread.sleep( 500 );
        System.out.println( "Ran test" );
    }

    private synchronized void incrementTestCount()
    {
        testCount++;
    }

}
