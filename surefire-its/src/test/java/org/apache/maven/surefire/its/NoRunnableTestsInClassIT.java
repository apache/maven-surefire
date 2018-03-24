package org.apache.maven.surefire.its;

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
 * SUREFIRE-621 Asserts proper test counts when running junit 3 tests in parallel
 *
 * @author Kristian Rosenvold
 */
public class NoRunnableTestsInClassIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testJunit3ParallelBuildResultCount()
    {
        unpack( "norunnableTests" ).failNever().executeTest().verifyTextInLog(
            "No tests found in junit.norunnabletests.BasicTest" );
    }
}