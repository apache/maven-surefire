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
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Test https://issues.apache.org/jira/browse/SUREFIRE-1426
 *
 */
public class Surefire1426JvmCrashShouldNotBeIgnoredIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void mavenShouldFail() throws VerificationException
    {
        unpack( "surefire-1426-ignore-fail-jvm-crash" )
            .maven()
            .withFailure()
            .debugLogging()
            .executeTest()
            .assertThatLogLine( containsString( "BUILD SUCCESS" ), is( 0 ) )
            .verifyTextInLog( "BUILD FAILURE" );
    }
}
