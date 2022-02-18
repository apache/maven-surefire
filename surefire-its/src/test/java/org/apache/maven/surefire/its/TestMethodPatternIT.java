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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.surefire.its.fixture.OutputValidator;
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
 * Test project using -Dtest=mtClass#myMethod
 *
 * @author Olivier Lamy
 */
@RunWith( Parameterized.class )
public class TestMethodPatternIT
    extends SurefireJUnit4IntegrationTestCase
{
    private static final String RUNNING_WITH_PROVIDER47 = "parallel='none', perCoreThreadCount=true, threadCount=0";

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

    private OutputValidator runMethodPattern( String projectName, Map<String, String> props, String... goals )
        throws Exception
    {
        SurefireLauncher launcher = unpack( projectName, profileId == null ? "" : "-" + profileId );

        if ( profileId != null )
        {
            launcher.activateProfile( profileId );
        }

        for ( Entry<String, String> entry : props.entrySet() )
        {
            launcher.sysProp( entry.getKey(), entry.getValue() );
        }
        for ( String goal : goals )
        {
            launcher.addGoal( goal );
        }
        String cls = profileId == null ? LEGACY_FORK_NODE : SUREFIRE_FORK_NODE;
        return launcher.showErrorStackTraces().debugLogging()
            .executeTest()
            .assertTestSuiteResults( 2, 0, 0, 0 )
            .assertThatLogLine(
                containsString( "Found implementation of fork node factory: " + cls ),
                equalTo( 1 ) );
    }

    @Test
    public void testJUnit44()
        throws Exception
    {
        runMethodPattern( "junit44-method-pattern", Collections.<String, String>emptyMap() );
    }

    @Test
    public void testJUnit48Provider4()
        throws Exception
    {
        runMethodPattern( "junit48-method-pattern", Collections.<String, String>emptyMap(), "-P surefire-junit4" );
    }

    @Test
    public void testJUnit48Provider47()
        throws Exception
    {
        runMethodPattern( "junit48-method-pattern", Collections.<String, String>emptyMap(), "-P surefire-junit47" )
            .verifyTextInLog( RUNNING_WITH_PROVIDER47 );
    }

    @Test
    public void testJUnit48WithCategoryFilter() throws Exception
    {
        String cls = profileId == null ? LEGACY_FORK_NODE : SUREFIRE_FORK_NODE;
        SurefireLauncher launcher = unpack( "junit48-method-pattern", profileId == null ? "" : "-" + profileId );

        if ( profileId != null )
        {
            launcher.activateProfile( profileId );
        }

        launcher.debugLogging()
            .addGoal( "-Dgroups=junit4.SampleCategory" )
            .executeTest()
            .assertTestSuiteResults( 1, 0, 0, 0 )
            .assertThatLogLine(
                containsString( "Found implementation of fork node factory: " + cls ),
                equalTo( 1 ) );
    }

    @Test
    public void testTestNgMethodBefore()
        throws Exception
    {
        Map<String, String> props = new HashMap<>();
        props.put( "testNgVersion", "5.7" );
        props.put( "testNgClassifier", "jdk15" );
        runMethodPattern( "testng-method-pattern-before", props );
    }

    @Test
    public void testTestNGMethodPattern()
        throws Exception
    {
        Map<String, String> props = new HashMap<>();
        props.put( "testNgVersion", "5.7" );
        props.put( "testNgClassifier", "jdk15" );
        runMethodPattern( "/testng-method-pattern", props );
    }

    @Test
    public void testMethodPatternAfter() throws Exception
    {
        String cls = profileId == null ? LEGACY_FORK_NODE : SUREFIRE_FORK_NODE;
        SurefireLauncher launcher = unpack( "testng-method-pattern-after", profileId == null ? "" : "-" + profileId );

        if ( profileId != null )
        {
            launcher.activateProfile( profileId );
        }

        launcher.debugLogging()
                .sysProp( "testNgVersion", "5.7" )
                .sysProp( "testNgClassifier", "jdk15" )
                .executeTest()
                .verifyErrorFree( 2 )
                .verifyTextInLog( "Called tearDown" )
                .assertThatLogLine(
                    containsString( "Found implementation of fork node factory: " + cls ),
                    equalTo( 1 ) );
    }

}
