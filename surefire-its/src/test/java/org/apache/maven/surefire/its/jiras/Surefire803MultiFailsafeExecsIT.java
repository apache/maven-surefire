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
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 *
 */
public class Surefire803MultiFailsafeExecsIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void testSecondExecutionRunsAfterFirstExecutionFails()
    {
        unpack(
            "/surefire-803-multiFailsafeExec-failureInFirst" )
                .maven()
                .withFailure()
                .executeVerify()
                .assertIntegrationTestSuiteResults( 4, 0, 2, 0 );
    }

    @Test
    public void testOneExecutionRunInTwoBuilds()
    {
        SurefireLauncher launcher = unpack( "/surefire-803-multiFailsafeExec-rebuildOverwrites" );
        launcher.sysProp( "success", "false" ).maven().withFailure().executeVerify().assertIntegrationTestSuiteResults(
            1, 0, 1, 0 );
        launcher.reset();
        launcher.sysProp( "success", "true" ).executeVerify().assertIntegrationTestSuiteResults( 1, 0, 0, 0 );
    }

}
