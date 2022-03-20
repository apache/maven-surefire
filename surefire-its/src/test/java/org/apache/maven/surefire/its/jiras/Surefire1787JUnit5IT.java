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
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 *
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class Surefire1787JUnit5IT extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void bothEngines()
    {
        unpack( "junit-4-5" )
            .activateProfile( "both-engines" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyTextInLog( "Running pkg.JUnit4Test" )
            .verifyTextInLog( "Running pkg.JUnit5Test" )
            .verifyTextInLog(
                "Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider" );
    }

    @Test
    public void apiAndEngine()
    {
        unpack( "junit-4-5" )
            .activateProfile( "api-and-engines" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyTextInLog( "Running pkg.JUnit4Test" )
            .verifyTextInLog( "Running pkg.JUnit5Test" )
            .verifyTextInLog(
                "Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider" );

    }

    @Test
    public void bothApis()
    {
        unpack( "junit-4-5" )
            .activateProfile( "both-api" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyTextInLog( "Running pkg.JUnit4Test" )
            .verifyTextInLog( "Running pkg.JUnit5Test" )
            .verifyTextInLog(
                "Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider" );
    }

    @Test
    public void selectJUnit4()
    {
        unpack( "junit-4-5" )
            .activateProfile( "select-junit4" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyTextInLog( "Running pkg.JUnit4Test" )
            .verifyTextInLog(
                "Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider" );
    }

    @Test
    public void selectJUnit5()
    {
        unpack( "junit-4-5" )
            .activateProfile( "select-junit5" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyTextInLog( "Running pkg.JUnit5Test" )
            .verifyTextInLog(
                "Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider" );
    }

    @Test
    public void testNgWithJupiterApi()
    {
        unpack( "junit5-testng" )
            .activateProfile( "junit5-api" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyTextInLog( "Running pkg.JUnit5Test" )
            .verifyTextInLog( "Running pkg.TestNGTest" )
            .verifyTextInLog(
                "Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider" );

    }

    @Test
    public void testNgWithJupiterEngine()
    {
        unpack( "junit5-testng" )
            .activateProfile( "junit5-engine" )
            .executeTest()
            .verifyErrorFree( 2 )
            .verifyTextInLog( "Running pkg.JUnit5Test" )
            .verifyTextInLog( "Running pkg.TestNGTest" )
            .verifyTextInLog(
                "Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider" );
    }

    @Test
    public void junit4Runner()
    {
        unpack( "junit5-runner" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyTextInLog( "Running pkg.JUnit5Tests" )
            .verifyTextInLog( "Using auto detected provider org.apache.maven.surefire.junit4.JUnit4Provider" );
    }

    @Test
    public void junit5Suite() throws Exception
    {
        unpack( "junit5-suite" )
            .executeTest()
            .verifyErrorFree( 1 )
            .verifyTextInLog(
                "Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider" )
            .verifyTextInLog( "Running pkg.JUnit5Test" )
            .verifyTextInLog( "Running pkg.domain.AxTest" )
            .assertThatLogLine( containsString( "Running pkg.domain.BxTest" ), equalTo( 0 ) );
    }
}
