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
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-995">SUREFIRE-995</a>
 * @since 2.18.1
 */
public class Surefire995CategoryInheritanceIT extends SurefireJUnit4IntegrationTestCase
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
        final OutputValidator outputValidator = unpack()
                .addGoal( "-Ppositive-tests" )
                .sysProp( "version.junit", "4.11" )
                .executeTest();

        outputValidator
                .verifyErrorFree( 1 )
                .verifyTextInLog( "CategorizedTest#a" );
    }

    @Test
    public void junit411ShouldExcludeExplicitCategory()
    {
        final OutputValidator outputValidator = unpack()
                .addGoal( "-Ppositive-tests-excluded-categories" )
                .sysProp( "version.junit", "4.11" )
                .executeTest();
        // SpecialCategorizedTest inherits the excluded annotation but should still run as
        // until junit 4.11, the Category annotation is not inherited
        outputValidator
                .verifyTextInLog( "SpecialCategorizedTest#b" )
                .verifyErrorFree( 16 );
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

    @Test
    public void junit411ShouldIgnoreInheritedCategories() throws VerificationException
    {
        // GIVEN a project using junit 4.11
        final OutputValidator outputValidator = unpack()
                .addGoal( "-Ppositive-tests-included-and-excluded-categories" )
                .sysProp( "version.junit", "4.11" )
                // AND the tests marked with CategoryB are marked for execution
                .setGroups( "jiras.surefire955.group.marker.CategoryB" )
                // WHEN the tests are executed
                .executeTest();

        // THEN only the tests in classes directly annotated should be executed
        outputValidator
                // Test runs when the category is present in the concrete class
                .verifyTextInLog( "Running jiras.surefire955.group.BBCTest" )
                .verifyTextInLog( "BBCTest#bbc" )
                .verifyTextInLog( "AbstractBCTest#pb" )
                .verifyTextInLog( "AbstractCTest#pc" )
                .verifyTextInLog( "Running jiras.surefire955.group.BTest" )
                .verifyTextInLog( "BTest#b" )
                // Test does not run when there is no category in the concrete class
                .assertThatLogLine( containsString( "BCTest#bc" ), is( 0 ) )
                .assertThatLogLine( containsString( "ABCTest#abc" ), is( 0 ) )
                .verifyErrorFree( 4 );
    }

    @Test
    public void junit412ShouldExecuteInheritedCategories() throws VerificationException
    {
        // GIVEN a project using junit 4.12
        final OutputValidator outputValidator = unpack()
                .addGoal( "-Ppositive-tests-included-and-excluded-categories" )
                .sysProp( "version.junit", "4.12" )
                // AND the tests marked with CategoryB are marked for execution
                .setGroups( "jiras.surefire955.group.marker.CategoryB" )
                // WHEN the tests are executed
                .executeTest();

        // THEN the tests in classes directly marked with the CategoryB
        outputValidator
                .verifyErrorFree( 10 )
                .verifyTextInLog( "Running jiras.surefire955.group.BBCTest" )
                // AND Test runs when an already existing category is added in the concrete class
                .verifyTextInLog( "BBCTest#bbc" )
                .verifyTextInLog( "AbstractBCTest#pb" )
                .verifyTextInLog( "AbstractCTest#pc" )
                .verifyTextInLog( "Running jiras.surefire955.group.BTest" )
                .verifyTextInLog( "BTest#b" )
                // AND the tests in classes inheriting the CategoryB category should be executed
                .verifyTextInLog( "Running jiras.surefire955.group.ABCTest" )
                // AND Test runs when the concrete class has an additional (not excluded) category
                .verifyTextInLog( "ABCTest#abc" )
                .verifyTextInLog( "AbstractBCTest#pb" )
                .verifyTextInLog( "AbstractCTest#pc" )
                .verifyTextInLog( "Running jiras.surefire955.group.BCTest" )
                // AND Test runs when there is no category in the concrete class
                .verifyTextInLog( "BCTest#bc" )
                .verifyTextInLog( "AbstractBCTest#pb" )
                .verifyTextInLog( "AbstractCTest#pc" );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "surefire-995-categoryInheritance" );
    }
}
