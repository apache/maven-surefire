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
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-995">SUREFIRE-995</a>
 * @since 2.18.1
 */
public class Surefire995CategoryInheritanceIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void negativeTestShouldRunAllCategories()
    {
        unpack()
            .setTestToRun( "Special*Test" )
            .executeTest()
            .verifyErrorFree( 3 );
    }

    @Test
    public void junit411ShouldRunExplicitCategory()
    {
        unpack()
            .addGoal( "-Ppositive-tests" )
            .sysProp( "version.junit", "4.11" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyTextInLog( "CategorizedTest#a" );
    }

    @Test
    public void junit411ShouldExcludeExplicitCategory()
    {
        unpack()
            .addGoal( "-Ppositive-tests-excluded-categories" )
            .sysProp( "version.junit", "4.11" )
            .executeTest()
            .verifyErrorFree( 2 );
    }

    @Test
    public void junit412ShouldRunInheritedCategory()
    {
        unpack()
            .setTestToRun( "Special*Test" )
            .addGoal( "-Ppositive-tests" )
            .executeTest()
            .verifyErrorFree( 2 );
    }

    @Test
    public void junit412ShouldExcludeInheritedCategory()
    {
        unpack()
            .setTestToRun( "Special*Test" )
            .addGoal( "-Ppositive-tests-excluded-categories" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyTextInLog( "SpecialNonCategoryTest#test" );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "surefire-995-categoryInheritance" );
    }
}
