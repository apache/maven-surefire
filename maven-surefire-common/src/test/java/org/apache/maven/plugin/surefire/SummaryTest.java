package org.apache.maven.plugin.surefire;

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

import org.apache.maven.plugin.surefire.booterclient.ForkConfiguration;
import org.apache.maven.surefire.suite.RunResult;

import junit.framework.TestCase;

public class SummaryTest
    extends TestCase
{
    Summary summary = new Summary();

    public void testEmptySummaryShouldBeErrorFree()
    {
        assertTrue( summary.isErrorFree() );
    }

    public void testSummaryShouldBeErrorFreeAfterAddingAnException()
    {
        summary.registerException( new RuntimeException() );
        assertFalse( summary.isErrorFree() );
    }

    public void testEmptySummaryShouldHaveNoFailureOrTimeOut()
    {
        assertFalse( summary.isFailureOrTimeout() );
    }

    public void testSummaryReturnsFailureOrTimeOutStateOfLastRun()
    {
        RunResult resultWithoutFailure = new RunResult( 0, 0, 0, 0, false, false );
        RunResult resultWithFailure = new RunResult( 0, 0, 0, 0, true, true );
        summary.registerRunResult( resultWithoutFailure );
        summary.registerRunResult( resultWithFailure );
        assertTrue( summary.isFailureOrTimeout() );
    }

    public void testEmptySummaryHasNoFirstException()
    {
        assertNull( summary.getFirstException() );
    }

    public void testSummaryReturnsTheFirstOfTwoExceptions()
    {
        Exception exceptionOne = new RuntimeException();
        Exception exceptionTwo = new RuntimeException();
        summary.registerException( exceptionOne );
        summary.registerException( exceptionTwo );
        assertEquals( "Wrong exception.", exceptionOne, summary.getFirstException() );
    }

    public void testEmptySummaryHasNoResultOfLastSuccessfulRun()
    {
        assertNull( summary.getResultOfLastSuccessfulRun() );
    }

    public void testSummaryReturnsTheSecondOfTwoResult()
    {
        RunResult resultOne = new RunResult( 0, 0, 0, 0 );
        RunResult resultTwo = new RunResult( 0, 0, 0, 0 );
        summary.registerRunResult( resultOne );
        summary.registerRunResult( resultTwo );
        assertEquals( "Wrong exception.", resultTwo, summary.getResultOfLastSuccessfulRun() );
    }

    public void testEmptySummaryIsNotForking()
    {
        assertFalse( summary.isForking() );
    }

    public void testSummaryIsForkingIfTheLastConfigurationIsForking()
    {
        summary.reportForkConfiguration( createNonForkingConfiguration() );
        summary.reportForkConfiguration( createForkingConfiguration() );
        assertTrue( summary.isForking() );
    }

    public void testSummaryIsNotForkingIfTheLastConfigurationIsNotForking()
    {
        summary.reportForkConfiguration( createForkingConfiguration() );
        summary.reportForkConfiguration( createNonForkingConfiguration() );
        assertFalse( summary.isForking() );
    }

    private ForkConfiguration createForkingConfiguration()
    {
        return new ForkConfiguration( null, ForkConfiguration.FORK_ALWAYS, null );
    }

    private ForkConfiguration createNonForkingConfiguration()
    {
        return new ForkConfiguration( null, ForkConfiguration.FORK_NEVER, null );
    }
}
