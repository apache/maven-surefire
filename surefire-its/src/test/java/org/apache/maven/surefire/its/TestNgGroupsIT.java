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

import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;

import org.junit.Test;

/**
 * Test the group filter for TestNG
 *
 */
public class TestNgGroupsIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testExclusion()
    {
        unpack().setExcludedGroups( "notincluded" ).executeTest().verifyErrorFree( 5 );
    }

    @Test
    public void testOnlyGroups()
    {
        unpack().setGroups( "functional" ).executeTest().verifyErrorFree( 2 );
    }

    @Test
    public void testGroupsAndExclusion()
    {
        unpack().setGroups( "functional" ).setExcludedGroups( "notincluded" ).executeTest().verifyErrorFree( 1 );
    }

    @Test
    public void groupsWithDash()
    {
        unpack().setGroups( "abc-def" ).executeTest().verifyErrorFree( 1 );
    }

    @Test
    public void groupsBySimpleRegex()
    {
        unpack().setGroups( "foo\\..*" ).executeTest().verifyErrorFree( 2 );
    }

    public SurefireLauncher unpack()
    {
        return unpack( "/testng-groups" );
    }

}
