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
 * @author agudian
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1122">SUREFIRE-1122</a>
 */
public class Surefire1122ParallelAndFlakyTestsIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void nonParallelCreatesCorrectReport()
    {
        unpack( "surefire-1122-parallel-and-flakyTests" )
            .executeTest()
            .assertTestSuiteResults( 2, 0, 0, 0, 1 );
    }

    @Test
    public void parallelCreatesCorrectReport()
    {
        unpack( "surefire-1122-parallel-and-flakyTests" )
            .activateProfile( "parallel" )
            .executeTest()
            .assertTestSuiteResults( 2, 0, 0, 0, 1 );
    }

    @Test
    public void parallelCreatesCorrectReportFork()
    {
        unpack( "surefire-1122-parallel-and-flakyTests" )
                .activateProfile( "parallel-one-fork" )
                .executeTest()
                .assertTestSuiteResults( 2, 0, 0, 0, 1 );
    }
}
