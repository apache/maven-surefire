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

import java.util.ArrayList;

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests using the JUnit 47 provider to rerun failing tests with the cucumber runner. The main problem that the junit4
 * provider has with the cucumber runner is that the junit Description instance created by the runner has a null test
 * class attribute. This requires that tests are rerun based on their description.
 *
 * @author mpkorstanje
 */
@RunWith( Parameterized.class )
@SuppressWarnings( "checkstyle:magicnumber" )
public class JUnit47RerunFailingTestWithCucumberIT extends SurefireJUnit4IntegrationTestCase
{
    private static final String LEGACY_FORK_NODE =
        "org.apache.maven.plugin.surefire.extensions.LegacyForkNodeFactory";

    private static final String SUREFIRE_FORK_NODE =
        "org.apache.maven.plugin.surefire.extensions.SurefireForkNodeFactory";

    @Parameters
    public static Iterable<Object[]> data()
    {
        ArrayList<Object[]> args = new ArrayList<>();
        args.add( new Object[] { "tcp" } );
        args.add( new Object[] { null } );
        return args;
    }

    @Parameter
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String profileId;

    private SurefireLauncher unpack()
    {
        SurefireLauncher launcher =
            unpack( "junit47-rerun-failing-tests-with-cucumber", profileId == null ? "" : "-" + profileId );

        if ( profileId != null )
        {
            launcher.activateProfile( profileId );
        }

        return launcher;
    }

    @Test
    public void testRerunFailingErrorTestsFalse()
        throws Exception
    {
        String cls = profileId == null ? LEGACY_FORK_NODE : SUREFIRE_FORK_NODE;
        unpack()
            .debugLogging()
            .maven()
            .sysProp( "surefire.rerunFailingTestsCount", 0 )
            .withFailure()
            .executeTest()
            .assertTestSuiteResults( 1, 0, 1, 0, 0 )
            .assertThatLogLine(
                containsString( "Found implementation of fork node factory: " + cls ),
                equalTo( 1 ) );
    }

    @Test
    public void testRerunFailingErrorTestsWithOneRetry()
        throws Exception
    {
        String cls = profileId == null ? LEGACY_FORK_NODE : SUREFIRE_FORK_NODE;
        unpack()
            .debugLogging()
            .maven()
            .sysProp( "surefire.rerunFailingTestsCount", 1 )
            .withFailure()
            .executeTest()
            .assertTestSuiteResults( 1, 0, 1, 0, 0 )
            .assertThatLogLine(
                containsString( "Found implementation of fork node factory: " + cls ),
                equalTo( 1 ) );
    }

    @Test
    public void testRerunFailingErrorTestsTwoRetry()
        throws Exception
    {
        String cls = profileId == null ? LEGACY_FORK_NODE : SUREFIRE_FORK_NODE;
        unpack()
            .maven()
            .debugLogging()
            .sysProp( "surefire.rerunFailingTestsCount", 2 )
            .executeTest()
            .assertTestSuiteResults( 1, 0, 0, 0, 2 )
            .assertThatLogLine(
                containsString( "Found implementation of fork node factory: " + cls ),
                equalTo( 1 ) );
    }
}
