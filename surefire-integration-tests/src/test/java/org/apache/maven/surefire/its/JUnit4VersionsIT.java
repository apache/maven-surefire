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

import java.util.Arrays;
import java.util.Collection;

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import static org.junit.runners.Parameterized.*;

/**
 * Basic suite test using all known versions of JUnit 4.x
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
@RunWith( Parameterized.class )
public class JUnit4VersionsIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Parameters( name = "{index}: JUnit {0}" )
    public static Collection<Object[]> junitVersions()
    {
        return Arrays.asList( new Object[][] {
                { "4.0" },
                { "4.1" },
                { "4.2" },
                { "4.3" },
                { "4.3.1" },
                { "4.4" },
                { "4.5" },
                { "4.6" },
                { "4.7" },
                { "4.8" },
                { "4.8.1" },
                { "4.8.2" },
                { "4.9" },
                { "4.10" },
                { "4.11" },
                { "4.12" }
        } );
    }

    @Parameter
    public String version;

    private SurefireLauncher unpack()
    {
        return unpack( "/junit4", version );
    }

    @Test
    public void testJunit()
        throws Exception
    {
        runJUnitTest( version );
    }

    public void runJUnitTest( String version )
        throws Exception
    {
        unpack().setJUnitVersion( version ).executeTest().verifyErrorFree( 1 );
    }
}
