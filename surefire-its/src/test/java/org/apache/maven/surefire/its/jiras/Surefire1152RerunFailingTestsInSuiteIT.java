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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * SUREFIRE-1152 Assert rerunFailingTestsCount works with test suites
 *
 * @author Sean Flanigan
 */
public class Surefire1152RerunFailingTestsInSuiteIT
    extends SurefireJUnit4IntegrationTestCase
{
    private static final String RUNNING_WITH_PROVIDER47 =
        "Using configured provider org.apache.maven.surefire.junitcore.JUnitCoreProvider";

    private OutputValidator runMethodPattern( String... goals )
    {
        SurefireLauncher launcher = unpack("surefire-1152-rerunFailingTestsCount-suite" );
        for ( String goal : goals )
        {
            launcher.addGoal( goal );
        }
        OutputValidator outputValidator = launcher.showErrorStackTraces().debugLogging().executeVerify();
        outputValidator.assertTestSuiteResults( 3, 0, 0, 0, 3 );
        outputValidator.assertIntegrationTestSuiteResults( 1, 0, 0, 0 );
        return outputValidator;
    }

    @Test
    public void testJUnit48Provider4()
    {
        runMethodPattern("-P surefire-junit4" );
    }

    @Test
    public void testJUnit48Provider47()
    {
        runMethodPattern("-P surefire-junit47" )
            .verifyTextInLog( RUNNING_WITH_PROVIDER47 );
    }

}
