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
 * Test
 *
 * @author Kristian Rosenvold
 */
public class Surefire705ParallelForkTimeoutIT extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testTimeoutForked()
    {
        unpack( "/fork-timeout" ).setJUnitVersion( "4.8.1" ).addGoal( "-Djunit.version=4.8.1" ).addGoal(
                "-Djunit.parallel=classes" ).addGoal(
                "-DtimeOut=1" ).maven().withFailure().executeTest().verifyTextInLog(
                "There was a timeout in the fork" );
    }
}
