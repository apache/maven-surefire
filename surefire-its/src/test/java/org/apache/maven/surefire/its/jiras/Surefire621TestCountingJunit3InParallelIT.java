package org.apache.maven.surefire.its.jiras;

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

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

/**
 * SUREFIRE-621 Asserts proper test counts when running junit 3 tests in parallel<br>
 * SUREFIRE-1264 Some tests can be lost when running in parallel with parameterized tests<br>
 * <br>
 * Removed decision making with JUnit3 in {@code TestSet} class during Jira activity of <code>SUREFIRE-1264</code>
 * which results in one hot spot where the test class is determined (see JUnitCoreRunListener#fillTestCountMap()).
 *
 * @author Kristian Rosenvold
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 */
public class Surefire621TestCountingJunit3InParallelIT
    extends SurefireJUnit4IntegrationTestCase
{
    /**
     * SUREFIRE-1264
     */
    @Test
    public void testJunit3AllParallelBuildResultCount()
    {
        unpack( "surefire-621-testCounting-junit3-in-parallel" )
                .activateProfile( "all-parallel-junit3-testcases" )
                .execute( "integration-test" )
                .assertTestSuiteResults( 6, 0, 0, 0 );
    }

    /**
     * SUREFIRE-621
     */
    @Test
    public void testJunit3ParallelBuildResultCount()
    {
        unpack( "surefire-621-testCounting-junit3-in-parallel" )
                .failNever()
                .activateProfile( "parallel-junit3-testcases" )
                .execute( "install" )
                .assertTestSuiteResults( 6, 0, 0, 0 );
    }

    /**
     * SUREFIRE-1264
     */
    @Test
    public void testJunit3BuildResultCount()
    {
        unpack( "surefire-621-testCounting-junit3-in-parallel" )
                .activateProfile( "junit3-testcases" )
                .execute( "integration-test" )
                .assertTestSuiteResults( 6, 0, 0, 0 );
    }

    /**
     * SUREFIRE-1264
     */
    @Test
    public void testJunit3ParallelSuiteBuildResultCount()
    {
        unpack( "surefire-621-testCounting-junit3-in-parallel" )
                .activateProfile( "parallel-junit3-testsuite" )
                .execute( "integration-test" )
                .assertTestSuiteResults( 6, 0, 0, 0 );
    }

    /**
     * SUREFIRE-1264
     */
    @Test
    public void testJunit3SuiteBuildResultCount()
    {
        unpack( "surefire-621-testCounting-junit3-in-parallel" )
                .activateProfile( "junit3-testsuite" )
                .execute( "integration-test" )
                .assertTestSuiteResults( 6, 0, 0, 0 );
    }
}
