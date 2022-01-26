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
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Verification for <code>multipleFrameworks</code> option.
 *
 * @author Slawomir Jaranowski
 */
public class Surefire1963MultipleFrameworkIT extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void onlyJunit4DefaultValue() throws VerificationException
    {
        OutputValidator outputValidator = unpack( "/surefire-1963-multiple-framework" )
            .executeTest()
            .verifyTextInLog( "Using auto detected provider org.apache.maven.surefire.junitcore.JUnitCoreProvider" )
            .verifyErrorFree( 1 );

        assertThat( outputValidator.loadLogLines(
            containsString( "There are many providers automatically detected" ) ) ).isEmpty();
    }

    @Test
    public void onlyJunit4WithWarn() throws VerificationException
    {
        OutputValidator outputValidator = unpack( "/surefire-1963-multiple-framework" )
            .sysProp( "multipleFrameworks", "warn" )
            .executeTest()
            .verifyTextInLog( "Using auto detected provider org.apache.maven.surefire.junitcore.JUnitCoreProvider" )
            .verifyErrorFree( 1 );

        assertThat( outputValidator.loadLogLines(
            containsString( "There are many providers automatically detected" ) ) ).isEmpty();
    }

    @Test
    public void onlyJunit4WithFail() throws VerificationException
    {
        OutputValidator outputValidator = unpack( "/surefire-1963-multiple-framework" )
            .sysProp( "multipleFrameworks", "fail" )
            .executeTest()
            .verifyTextInLog( "Using auto detected provider org.apache.maven.surefire.junitcore.JUnitCoreProvider" )
            .verifyErrorFree( 1 );

        assertThat( outputValidator.loadLogLines(
            containsString( "There are many providers automatically detected" ) ) ).isEmpty();
    }

    @Test
    public void junit4AndJunit5DefaultValue() throws VerificationException
    {
        OutputValidator outputValidator = unpack( "/surefire-1963-multiple-framework" )
            .activateProfile( "junit5" )
            .executeTest()
            .verifyTextInLog(
                "Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider" )
            .verifyErrorFree( 1 );

        assertThat( outputValidator.loadLogLines(
            containsString( "There are many providers automatically detected" ) ) ).isEmpty();
    }

    @Test
    public void junit4AndJunit5Warn()
    {
        unpack( "/surefire-1963-multiple-framework" )
            .activateProfile( "junit5" )
            .sysProp( "multipleFrameworks", "warn" )
            .executeTest()
            .verifyTextInLog(
                "Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider" )
            .verifyTextInLog( "[WARNING] There are many providers automatically detected:" )
            .verifyErrorFree( 1 );
    }

    @Test
    public void junit4AndJunit5Fail()
    {
        unpack( "/surefire-1963-multiple-framework" )
            .maven()
            .withFailure()
            .activateProfile( "junit5" )
            .sysProp( "multipleFrameworks", "fail" )
            .executeTest()
            .verifyTextInLog( "[ERROR] There are many providers automatically detected:" );
    }
}
