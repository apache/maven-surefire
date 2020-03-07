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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;

import org.junit.Test;

/**
 *
 */
public class Surefire809GroupExpressionsIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void categoryAB()
    {
        OutputValidator validator = unpackJUnit().groups( "junit4.CategoryA AND junit4.CategoryB" ).executeTest();
        validator.verifyErrorFreeLog();
        validator.assertTestSuiteResults( 2, 0, 0, 0 );
        validator.verifyTextInLog( "catA: 1" );
        validator.verifyTextInLog( "catB: 1" );
        validator.verifyTextInLog( "catC: 0" );
        validator.verifyTextInLog( "catNone: 0" );
        validator.verifyTextInLog( "mA: 1" );
        validator.verifyTextInLog( "mB: 1" );
        validator.verifyTextInLog( "mC: 0" );
    }

    @Test
    public void incorrectJUnitVersions()
    {
        unpackJUnit().setJUnitVersion( "4.5" ).groups(
            "junit4.CategoryA AND junit4.CategoryB" ).maven().withFailure().executeTest();
    }

    @Test
    public void testJUnitRunCategoryNotC()
    {
        OutputValidator validator = unpackJUnit().groups( "!junit4.CategoryC" ).executeTest();
        validator.verifyErrorFreeLog();
        validator.assertTestSuiteResults( 5, 0, 0, 0 );
        validator.verifyTextInLog( "catA: 2" );
        validator.verifyTextInLog( "catB: 2" );
        validator.verifyTextInLog( "catC: 0" );
        validator.verifyTextInLog( "catNone: 1" );
        validator.verifyTextInLog( "NoCategoryTest.CatNone: 1" );
    }

    @Test
    public void testExcludedGroups()
    {
        OutputValidator validator = unpackJUnit().setExcludedGroups( "junit4.CategoryC" ).executeTest();
        validator.verifyErrorFreeLog();
        validator.assertTestSuiteResults( 5, 0, 0, 0 );
        validator.verifyTextInLog( "catA: 2" );
        validator.verifyTextInLog( "catB: 2" );
        validator.verifyTextInLog( "catC: 0" );
        validator.verifyTextInLog( "catNone: 1" );
        validator.verifyTextInLog( "NoCategoryTest.CatNone: 1" );
    }

    @Test
    public void testNGRunCategoryAB()
    {
        OutputValidator validator = unpackTestNG().groups( "CategoryA AND CategoryB" ).debugLogging().executeTest();
        validator.verifyErrorFreeLog();
        validator.assertTestSuiteResults( 2, 0, 0, 0 );
        validator.verifyTextInLog( "BasicTest.testInCategoriesAB()" );
        validator.verifyTextInLog( "CategoryCTest.testInCategoriesAB()" );
    }

    @Test
    public void testNGRunCategoryNotC()
    {
        OutputValidator validator = unpackTestNG().groups( "!CategoryC" ).debugLogging().executeTest();
        validator.verifyErrorFreeLog();
        validator.assertTestSuiteResults( 8, 0, 0, 0 );
        validator.verifyTextInLog( "catA: 2" );
        validator.verifyTextInLog( "catB: 2" );
        validator.verifyTextInLog( "catC: 0" );
        validator.verifyTextInLog( "catNone: 1" );
        validator.verifyTextInLog( "mA: 2" );
        validator.verifyTextInLog( "mB: 2" );
        validator.verifyTextInLog( "mC: 0" );
        validator.verifyTextInLog( "NoCategoryTest.CatNone: 1" );
    }

    private SurefireLauncher unpackJUnit()
    {
        return unpack( "surefire-809-groupExpr-junit48" );
    }

    private SurefireLauncher unpackTestNG()
    {
        return unpack( "surefire-809-groupExpr-testng" );
    }

}
