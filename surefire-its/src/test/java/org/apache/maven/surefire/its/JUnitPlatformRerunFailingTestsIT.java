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

import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

/**
 * JUnit4 RunListener Integration Test.
 *
 * @author <a href="mailto:qingzhouluo@google.com">Qingzhou Luo</a>
 * @author Matt Coley
 */
@SuppressWarnings( "checkstyle:magicnumber" )
public class JUnitPlatformRerunFailingTestsIT extends SurefireJUnit4IntegrationTestCase
{
    private static final String VERSION = "5.5.2";

    private SurefireLauncher unpack()
    {
        return unpack( "/junit-platform-rerun-failing-tests" );
    }

    @Test
    public void testRerunFailingErrorTestsWithOneRetry()
    {
        OutputValidator outputValidator = unpack().setJUnitVersion( VERSION ).maven().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" )
                .withFailure()
                .debugLogging()
                .executeTest()
                .assertTestSuiteResults( 5, 1, 1, 0, 0 );
        verifyFailuresOneRetryAllClasses( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().debugLogging().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" ).addGoal(
                "-DforkCount=2" ).withFailure().executeTest().assertTestSuiteResults( 5, 1, 1, 0, 0 );
        verifyFailuresOneRetryAllClasses( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().debugLogging().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-Dparallel=methods" ).addGoal(
                "-DuseUnlimitedThreads=true" ).withFailure().executeTest().assertTestSuiteResults( 5, 1, 1, 0, 0 );
        verifyFailuresOneRetryAllClasses( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().debugLogging().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-Dparallel=classes" ).addGoal(
                "-DuseUnlimitedThreads=true" ).withFailure().executeTest().assertTestSuiteResults( 5, 1, 1, 0, 0 );
        verifyFailuresOneRetryAllClasses( outputValidator );
    }

    @Test
    public void testRerunFailingErrorTestsTwoRetry()
    {
        // Four flakes, both tests have been re-run twice
        OutputValidator outputValidator = unpack().setJUnitVersion( VERSION ).maven().debugLogging().addGoal(
                "-Dsurefire.rerunFailingTestsCount=2" ).executeTest().assertTestSuiteResults( 5, 0, 0, 0, 4 );

        verifyFailuresTwoRetryAllClasses( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().debugLogging().addGoal(
                "-Dsurefire.rerunFailingTestsCount=2" ).addGoal( "-DforkCount=3" ).executeTest().assertTestSuiteResults(
                5, 0, 0, 0, 4 );

        verifyFailuresTwoRetryAllClasses( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().debugLogging().addGoal(
                "-Dsurefire.rerunFailingTestsCount=2" ).addGoal( "-Dparallel=methods" ).addGoal(
                "-DuseUnlimitedThreads=true" ).executeTest().assertTestSuiteResults( 5, 0, 0, 0, 4 );

        verifyFailuresTwoRetryAllClasses( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().debugLogging().addGoal(
                "-Dsurefire.rerunFailingTestsCount=2" ).addGoal( "-Dparallel=classes" ).addGoal(
                "-DuseUnlimitedThreads=true" ).executeTest().assertTestSuiteResults( 5, 0, 0, 0, 4 );

        verifyFailuresTwoRetryAllClasses( outputValidator );
    }

    @Test
    public void testRerunFailingErrorTestsFalse()
    {
        OutputValidator outputValidator = unpack().setJUnitVersion(
                VERSION ).maven().withFailure().debugLogging().executeTest().assertTestSuiteResults( 5, 1, 1, 0, 0 );

        verifyFailuresNoRetryAllClasses( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().addGoal(
                "-DforkCount=3" ).withFailure().debugLogging().executeTest().assertTestSuiteResults( 5, 1, 1, 0, 0 );

        verifyFailuresNoRetryAllClasses( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().addGoal( "-Dparallel=methods" ).addGoal(
                "-DuseUnlimitedThreads=true" ).withFailure().debugLogging().executeTest().assertTestSuiteResults( 5, 1,
                1, 0, 0 );

        verifyFailuresNoRetryAllClasses( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().addGoal( "-Dparallel=classes" ).addGoal(
                "-DuseUnlimitedThreads=true" ).withFailure().debugLogging().executeTest().assertTestSuiteResults( 5, 1,
                1, 0, 0 );

        verifyFailuresNoRetryAllClasses( outputValidator );
    }

    @Test
    public void testRerunOneTestClass()
    {
        OutputValidator outputValidator = unpack().setJUnitVersion( VERSION ).maven().debugLogging().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" ).addGoal(
                "-Dtest=FlakyFirstTimeTest" ).withFailure().executeTest().assertTestSuiteResults( 3, 1, 1, 0, 0 );

        verifyFailuresOneRetryOneClass( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-DforkCount=3" ).addGoal(
                "-Dtest=FlakyFirstTimeTest" ).withFailure().debugLogging().executeTest().assertTestSuiteResults( 3, 1,
                1, 0, 0 );

        verifyFailuresOneRetryOneClass( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-Dparallel=methods" ).addGoal(
                "-DuseUnlimitedThreads=true" ).addGoal(
                "-Dtest=FlakyFirstTimeTest" ).withFailure().debugLogging().executeTest().assertTestSuiteResults( 3, 1,
                1, 0, 0 );

        verifyFailuresOneRetryOneClass( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-Dparallel=classes" ).addGoal(
                "-DuseUnlimitedThreads=true" ).addGoal(
                "-Dtest=FlakyFirstTimeTest" ).withFailure().debugLogging().executeTest().assertTestSuiteResults( 3, 1,
                1, 0, 0 );

        verifyFailuresOneRetryOneClass( outputValidator );
    }

    @Test
    public void testRerunOneTestMethod()
    {
        OutputValidator outputValidator = unpack().setJUnitVersion( VERSION ).maven().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" ).addGoal(
                "-Dtest=FlakyFirstTimeTest#testFailing*" ).withFailure().debugLogging().executeTest()
                .assertTestSuiteResults( 1, 0, 1, 0, 0 );

        verifyFailuresOneRetryOneMethod( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-DforkCount=3" ).addGoal(
                "-Dtest=FlakyFirstTimeTest#testFailing*" ).withFailure().debugLogging().executeTest()
                .assertTestSuiteResults( 1, 0, 1, 0, 0 );

        verifyFailuresOneRetryOneMethod( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-Dparallel=methods" ).addGoal(
                "-DuseUnlimitedThreads=true" ).addGoal(
                "-Dtest=FlakyFirstTimeTest#testFailing*" )
                .withFailure()
                .debugLogging()
                .executeTest()
                .assertTestSuiteResults( 1, 0, 1, 0, 0 );

        verifyFailuresOneRetryOneMethod( outputValidator );

        outputValidator = unpack().setJUnitVersion( VERSION ).maven().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-Dparallel=classes" ).addGoal(
                "-DuseUnlimitedThreads=true" ).addGoal(
                "-Dtest=FlakyFirstTimeTest#testFailing*" )
                .withFailure()
                .debugLogging()
                .executeTest()
                .assertTestSuiteResults( 1, 0, 1, 0, 0 );

        verifyFailuresOneRetryOneMethod( outputValidator );
    }

    @Test
    public void testFailOnTooManyFlakes()
    {
        OutputValidator outputValidator = unpack().setJUnitVersion( VERSION ).maven().debugLogging()
            .addGoal( "-Dsurefire.rerunFailingTestsCount=2" )
            .addGoal( "-Dsurefire.failOnFlakeCount=1" )
            .withFailure()
            .executeTest()
            .assertTestSuiteResults( 5, 0, 0, 0, 4 );

        outputValidator.verifyTextInLog( "There are 2 flakes and failOnFlakeCount is set to 1" );
    }

    @Test
    public void testParameterizedTest()
    {
        unpack()
            .setJUnitVersion( VERSION )
            .maven()
            .activateProfile( "parameters" )
            .withFailure()
            .debugLogging()
            .executeTest()
            .assertTestSuiteResults( 6, 0, 1, 1, 0 )
            .getSurefireReportsXmlFile( "TEST-junitplatform.ParametersTest.xml" )
            .assertContainsText( "testOneFailingPassingTest(ConnectionPoolFactory)[1]" )
            .assertContainsText( "testOneFailingPassingTest(ConnectionPoolFactory)[2]" )
            .assertContainsText( "testOneFailingPassingTest(ConnectionPoolFactory)[3]" )
            .assertContainsText( "testAllPassingTest(ConnectionPoolFactory)[1]" )
            .assertContainsText( "testAllPassingTest(ConnectionPoolFactory)[2]" )
            .assertContainsText( "testAllPassingTest(ConnectionPoolFactory)[3]" );
    }

    private void verifyFailuresOneRetryAllClasses( OutputValidator outputValidator )
    {
        verifyFailuresOneRetry( outputValidator, 5, 1, 1, 0 );
    }

    private void verifyFailuresTwoRetryAllClasses( OutputValidator outputValidator )
    {
        verifyFailuresTwoRetry( outputValidator, 5, 0, 0, 2 );
    }

    private void verifyFailuresNoRetryAllClasses( OutputValidator outputValidator )
    {
        verifyFailuresNoRetry( outputValidator, 5, 1, 1, 0 );
    }

    private void verifyFailuresOneRetryOneClass( OutputValidator outputValidator )
    {
        verifyFailuresOneRetry( outputValidator, 3, 1, 1, 0 );
    }

    private void verifyFailuresOneRetryOneMethod( OutputValidator outputValidator )
    {
        verifyOnlyFailuresOneRetry( outputValidator, 1, 1, 0, 0 );
    }

    private void verifyFailuresOneRetry( OutputValidator outputValidator, int run, int failures, int errors,
                                         int flakes )
    {
        outputValidator.verifyTextInLog( "Failures:" );
        outputValidator.verifyTextInLog( "Run 1: FlakyFirstTimeTest.testFailingTestOne" );
        outputValidator.verifyTextInLog( "Run 2: FlakyFirstTimeTest.testFailingTestOne" );

        outputValidator.verifyTextInLog( "Errors:" );
        outputValidator.verifyTextInLog( "Run 1: FlakyFirstTimeTest.testErrorTestOne" );
        outputValidator.verifyTextInLog( "Run 2: FlakyFirstTimeTest.testErrorTestOne" );

        verifyStatistics( outputValidator, run, failures, errors, flakes );
    }

    private void verifyOnlyFailuresOneRetry( OutputValidator outputValidator, int run, int failures, int errors,
                                             int flakes )
    {
        outputValidator.verifyTextInLog( "Failures:" );
        outputValidator.verifyTextInLog( "Run 1: FlakyFirstTimeTest.testFailingTestOne" );
        outputValidator.verifyTextInLog( "Run 2: FlakyFirstTimeTest.testFailingTestOne" );

        verifyStatistics( outputValidator, run, failures, errors, flakes );
    }

    private void verifyFailuresTwoRetry( OutputValidator outputValidator, int run, int failures, int errors,
                                         int flakes )
    {
        outputValidator.verifyTextInLog( "Flakes:" );
        outputValidator.verifyTextInLog( "Run 1: FlakyFirstTimeTest.testFailingTestOne" );
        outputValidator.verifyTextInLog( "Run 2: FlakyFirstTimeTest.testFailingTestOne" );
        outputValidator.verifyTextInLog( "Run 3: PASS" );

        outputValidator.verifyTextInLog( "Run 1: FlakyFirstTimeTest.testErrorTestOne" );
        outputValidator.verifyTextInLog( "Run 2: FlakyFirstTimeTest.testErrorTestOne" );

        verifyStatistics( outputValidator, run, failures, errors, flakes );
    }

    private void verifyFailuresNoRetry( OutputValidator outputValidator, int run, int failures, int errors, int flakes )
    {
        outputValidator.verifyTextInLog( "Failures:" );
        outputValidator.verifyTextInLog( "junitplatform.FlakyFirstTimeTest.testFailingTestOne" );
        outputValidator.verifyTextInLog( "ERROR" );
        outputValidator.verifyTextInLog( "junitplatform.FlakyFirstTimeTest.testErrorTestOne" );

        verifyStatistics( outputValidator, run, failures, errors, flakes );
    }

    private void verifyStatistics( OutputValidator outputValidator, int run, int failures, int errors, int flakes )
    {
        if ( flakes > 0 )
        {
            outputValidator.verifyTextInLog(
                    "Tests run: " + run + ", Failures: " + failures + ", Errors: " + errors + ", Skipped: 0, Flakes: "
                            + flakes );
        }
        else
        {
            outputValidator.verifyTextInLog(
                    "Tests run: " + run + ", Failures: " + failures + ", Errors: " + errors + ", Skipped: 0" );
        }
    }
}
