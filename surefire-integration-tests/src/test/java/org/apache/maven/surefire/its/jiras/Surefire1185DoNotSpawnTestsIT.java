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

import org.apache.maven.it.VerificationException;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Surefire 2.19 spawns unnecessary tests in surefire-junit4 provider.
 * https://issues.apache.org/jira/browse/SUREFIRE-1185
 * Example, UnlistedTest is the problem here because it runs with filtered out methods:
 *
 * Running pkg.UnlistedTest
 * Tests run: 0, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 sec - in pkg.UnlistedTest
 * Running pkg.RunningTest
 * Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.001 sec - in pkg.RunningTest
 *
 * Results:
 *
 * Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
 */
public class Surefire1185DoNotSpawnTestsIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void doNotSpawnUnwantedTests()
        throws VerificationException
    {
        unpack().setTestToRun( "RunningTest#test" )
            .executeTest()
            .assertTestSuiteResults( 1 )
            .assertThatLogLine( containsString( "in pkg.RunningTest" ), is( 1 ) )
            .assertThatLogLine( containsString( "in pkg.UnlistedTest" ), is( 0 ) );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "surefire-1185" );
    }
}
