package org.apache.maven.surefire.suite;

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

/**
 * @author Kristian Rosenvold
 */
public class RunResultTest
    extends TestCase
{

    public void testEmptySummaryShouldBeErrorFree()
    {
        RunResult summary = RunResult.noTestsRun();
        assertTrue( summary.isErrorFree() );
    }

    public void testFailuresInFirstRun()
    {
        RunResult resultOne = new RunResult( 10, 1, 3, 2 );
        RunResult resultTwo = new RunResult( 20, 0, 0, 0 );
        assertFalse( resultOne.aggregate( resultTwo ).isErrorFree() );
    }


    public void testAggregatedValues()
    {
        RunResult simple = getSimpleAggregate();
        assertEquals( 20, simple.getCompletedCount() );
        assertEquals( 3, simple.getErrors() );
        assertEquals( 7, simple.getFailures() );
        assertEquals( 4, simple.getSkipped() );

    }

    private RunResult getSimpleAggregate()
    {
        RunResult resultOne = new RunResult( 10, 1, 3, 2 );
        RunResult resultTwo = new RunResult( 10, 2, 4, 2 );
        return resultOne.aggregate( resultTwo );
    }


    private void verifySame( RunResult original, RunResult runResult )
    {
        assertEquals( original.getCompletedCount(), runResult.getCompletedCount() );
        assertEquals( original.getErrors(), runResult.getErrors() );
        assertEquals( original.getFailures(), runResult.getFailures() );
        assertEquals( original.getSkipped(), runResult.getSkipped() );
        assertEquals( original.isFailure(), runResult.isFailure() );
        assertEquals( original.isTimeout(), runResult.isTimeout() );
    }
}
