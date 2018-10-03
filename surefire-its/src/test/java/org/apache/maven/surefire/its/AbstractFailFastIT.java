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

import org.apache.maven.surefire.its.fixture.MavenLauncher;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.HashMap;
import java.util.Map;

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
    @Parameter( 0 )
    public String description;

    @Parameter( 1 )
    public String profile;

    @Parameter( 2 )
    public Map<String, String> properties;

    @Parameter( 3 )
    public int total;

    @Parameter( 4 )
    public int failures;

    @Parameter( 5 )
    public int errors;

    @Parameter( 6 )
    public int skipped;

    protected abstract String withProvider();

    private OutputValidator prepare( String description, String profile, Map<String, String> properties )
    {
        MavenLauncher launcher = unpack( "/fail-fast-" + withProvider(), "_" + description )
            .maven();

        if ( profile != null )
        {
            launcher.addGoal( "-P " + profile );
        }

        if ( failures != 0 || errors != 0 )
        {
            launcher.withFailure();
        }

        return launcher.sysProp( properties ).executeTest();
    }

    static Map<String, String> props( int forkCount, int skipAfterFailureCount, boolean reuseForks )
    {
        Map<String, String> props = new HashMap<String, String>( 3 );
        props.put( "surefire.skipAfterFailureCount", "" + skipAfterFailureCount );
        props.put( "forkCount", "" + forkCount );
        props.put( "reuseForks", "" + reuseForks );
        return props;
    }

    @Test
    public void test()
    {
        prepare( description, profile, properties )
            .assertTestSuiteResults( total, errors, failures, skipped );
    }
}
