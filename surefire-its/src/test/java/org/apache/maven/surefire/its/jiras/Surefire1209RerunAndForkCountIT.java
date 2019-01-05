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
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1209">SUREFIRE-1209</a>
 * @since 2.19
 */
public class Surefire1209RerunAndForkCountIT
        extends SurefireJUnit4IntegrationTestCase
{
    private static final String SUMMARY_COUNTS = "Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Flakes: 2";

    @Test
    public void reusableForksJUnit47() throws VerificationException {
        unpack().executeTest()
                .assertTestSuiteResults( 5, 0, 0, 0, 4 )
                .assertThatLogLine( containsString( SUMMARY_COUNTS ), is( 1 ) );
    }

    @Test
    public void notReusableForksJUnit47() throws VerificationException {
        unpack().reuseForks( false )
                .executeTest()
                .assertTestSuiteResults( 5, 0, 0, 0, 4 )
                .assertThatLogLine( containsString( SUMMARY_COUNTS ), is( 1 ) );
    }

    @Test
    public void reusableForksJUnit4() throws VerificationException {
        unpack().activateProfile( "junit4" )
                .executeTest()
                .assertTestSuiteResults( 5, 0, 0, 0, 4 )
                .assertThatLogLine( containsString( SUMMARY_COUNTS ), is( 1 ) );
    }

    @Test
    public void notReusableForksJUnit4()
            throws VerificationException {
        unpack().activateProfile( "junit4" )
                .reuseForks( false )
                .executeTest()
                .assertTestSuiteResults( 5, 0, 0, 0, 4 )
                .assertThatLogLine( containsString( SUMMARY_COUNTS ), is( 1 ) );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "surefire-1209-rerun-and-forkcount" );
    }
}
