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
import org.junit.Before;
import org.junit.Test;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.assumeJavaVersion;

public class Surefire1585IT
        extends SurefireJUnit4IntegrationTestCase
{
    @Before
    public void setUp()
    {
        assumeJavaVersion( 1.8d );
    }

    @Test
    public void shouldRunWithJupiterApi()
    {
        unpack( "surefire-1585-jupiter-api" )
                .debugLogging()
                .executeTest()
                .verifyErrorFree( 1 );
    }

    @Test
    public void shouldRunWithVintage()
    {
        unpack( "surefire-1585-junit4-vintage" )
                .debugLogging()
                .executeTest()
                .verifyErrorFree( 1 );
    }
}
