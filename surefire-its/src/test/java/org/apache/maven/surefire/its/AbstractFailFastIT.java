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

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.surefire.its.fixture.MavenLauncher;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.runners.Parameterized.Parameter;

/**
 * Base test class for SUREFIRE-580, configuration parameter {@code skipAfterFailureCount}.
 *
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @since 2.19
 */
@RunWith( Parameterized.class )
public abstract class AbstractFailFastIT
    extends SurefireJUnit4IntegrationTestCase
{
    private static final String LEGACY_FORK_NODE =
        "org.apache.maven.plugin.surefire.extensions.LegacyForkNodeFactory";

    private static final String SUREFIRE_FORK_NODE =
        "org.apache.maven.plugin.surefire.extensions.SurefireForkNodeFactory";

    @Parameter( 0 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String description;

    @Parameter( 1 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public String profile;

    @Parameter( 2 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public Map<String, String> properties;

    @Parameter( 3 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public int total;

    @Parameter( 4 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public int failures;

    @Parameter( 5 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public int errors;

    @Parameter( 6 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public int skipped;

    @Parameter( 7 )
    @SuppressWarnings( "checkstyle:visibilitymodifier" )
    public boolean useProcessPipes;

    protected abstract String withProvider();

    private OutputValidator prepare( String description, String profile, Map<String, String> properties )
    {
        MavenLauncher launcher = unpack( "/fail-fast-" + withProvider(), "_" + description )
            .maven()
            .debugLogging();

        if ( profile != null )
        {
            launcher.activateProfile( profile );
        }

        if ( !useProcessPipes )
        {
            launcher.activateProfile( "tcp" );
        }

        if ( failures != 0 || errors != 0 )
        {
            launcher.withFailure();
        }

        return launcher.sysProp( properties ).executeTest();
    }

    static Map<String, String> props( int forkCount, int skipAfterFailureCount, boolean reuseForks )
    {
        Map<String, String> props = new HashMap<>( 3 );
        props.put( "surefire.skipAfterFailureCount", "" + skipAfterFailureCount );
        props.put( "forkCount", "" + forkCount );
        props.put( "reuseForks", "" + reuseForks );
        return props;
    }

    @Test
    public void test() throws Exception
    {
        String cls = useProcessPipes ? LEGACY_FORK_NODE : SUREFIRE_FORK_NODE;
        prepare( description, profile, properties )
            .assertTestSuiteResults( total, errors, failures, skipped )
            .assertThatLogLine( containsString( "Found implementation of fork node factory: " + cls ), equalTo( 1 ) );
    }
}
