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
 */
public class JUnit4RerunFailingTestsIT
    extends SurefireJUnit4IntegrationTestCase
{
    private SurefireLauncher unpack()
    {
        return unpack( "/junit4-rerun-failing-tests" );
    }

    @Test
    public void testRerunFailingErrorTestsWithOneRetry()
        throws Exception
    {
        OutputValidator outputValidator =
            unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" ).withFailure().executeTest().assertTestSuiteResults( 5, 1, 1, 0,
                                                                                                            0 );
        verifyFailuresOneRetryAllClasses( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-Dsurefire.rerunFailingTestsCount=1" ).addGoal(
            "-DforkCount=2" ).withFailure().executeTest().assertTestSuiteResults( 5, 1, 1, 0, 0 );
        verifyFailuresOneRetryAllClasses( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-Dparallel=methods" ).addGoal(
            "-DuseUnlimitedThreads=true" ).withFailure().executeTest().assertTestSuiteResults( 5, 1, 1, 0, 0 );
        verifyFailuresOneRetryAllClasses( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-Dparallel=classes" ).addGoal(
            "-DuseUnlimitedThreads=true" ).withFailure().executeTest().assertTestSuiteResults( 5, 1, 1, 0, 0 );
        verifyFailuresOneRetryAllClasses( outputValidator );
    }

    @Test
    public void testRerunFailingErrorTestsTwoRetry()
        throws Exception
    {
        // Four flakes, both tests have been re-run twice
        OutputValidator outputValidator =
            unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
                "-Dsurefire.rerunFailingTestsCount=2" ).executeTest().assertTestSuiteResults( 5, 0, 0, 0, 4 );

        verifyFailuresTwoRetryAllClasses( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-Dsurefire.rerunFailingTestsCount=2" ).addGoal( "-DforkCount=3" ).executeTest()
            .assertTestSuiteResults( 5, 0, 0, 0, 4 );

        verifyFailuresTwoRetryAllClasses( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-Dsurefire.rerunFailingTestsCount=2" ).addGoal( "-Dparallel=methods" ).addGoal(
            "-DuseUnlimitedThreads=true" ).executeTest().assertTestSuiteResults( 5, 0, 0, 0, 4 );

        verifyFailuresTwoRetryAllClasses( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-Dsurefire.rerunFailingTestsCount=2" ).addGoal( "-Dparallel=classes" ).addGoal(
            "-DuseUnlimitedThreads=true" ).executeTest().assertTestSuiteResults( 5, 0, 0, 0, 4 );

        verifyFailuresTwoRetryAllClasses( outputValidator );
    }

    @Test
    public void testRerunFailingErrorTestsFalse()
        throws Exception
    {
        OutputValidator outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion(
            "4.12" ).maven().withFailure().executeTest().assertTestSuiteResults( 5, 1, 1, 0, 0 );

        verifyFailuresNoRetryAllClasses( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-DforkCount=3" ).withFailure().executeTest().assertTestSuiteResults( 5, 1, 1, 0, 0 );

        verifyFailuresNoRetryAllClasses( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-Dparallel=methods" ).addGoal(
            "-DuseUnlimitedThreads=true" ).withFailure().executeTest().assertTestSuiteResults( 5, 1, 1, 0, 0 );

        verifyFailuresNoRetryAllClasses( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-Dparallel=classes" ).addGoal(
            "-DuseUnlimitedThreads=true" ).withFailure().executeTest().assertTestSuiteResults( 5, 1, 1, 0, 0 );

        verifyFailuresNoRetryAllClasses( outputValidator );
    }

    @Test
    public void testRerunOneTestClass()
        throws Exception
    {
        OutputValidator outputValidator =
            unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" ).addGoal(
                "-Dtest=FlakyFirstTimeTest" ).withFailure().executeTest().assertTestSuiteResults( 3, 1, 1, 0, 0 );

        verifyFailuresOneRetryOneClass( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-DforkCount=3" ).addGoal(
            "-Dtest=FlakyFirstTimeTest" ).withFailure().executeTest().assertTestSuiteResults( 3, 1, 1, 0, 0 );

        verifyFailuresOneRetryOneClass( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-Dparallel=methods" ).addGoal(
            "-DuseUnlimitedThreads=true" ).addGoal(
            "-Dtest=FlakyFirstTimeTest" ).withFailure().executeTest().assertTestSuiteResults( 3, 1, 1, 0, 0 );

        verifyFailuresOneRetryOneClass( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-Dparallel=classes" ).addGoal(
            "-DuseUnlimitedThreads=true" ).addGoal(
            "-Dtest=FlakyFirstTimeTest" ).withFailure().executeTest().assertTestSuiteResults( 3, 1, 1, 0, 0 );

        verifyFailuresOneRetryOneClass( outputValidator );
    }

    @Test
    public void testRerunOneTestMethod()
        throws Exception
    {
        OutputValidator outputValidator =
            unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
                "-Dsurefire.rerunFailingTestsCount=1" ).addGoal(
                "-Dtest=FlakyFirstTimeTest#testFailing*" ).withFailure().executeTest().assertTestSuiteResults( 1, 0, 1,
                                                                                                               0, 0 );

        verifyFailuresOneRetryOneMethod( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-DforkCount=3" ).addGoal(
            "-Dtest=FlakyFirstTimeTest#testFailing*" ).withFailure().executeTest().assertTestSuiteResults( 1, 0, 1, 0,
                                                                                                           0 );

        verifyFailuresOneRetryOneMethod( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-Dparallel=methods" ).addGoal(
            "-DuseUnlimitedThreads=true" ).addGoal(
            "-Dtest=FlakyFirstTimeTest#testFailing*" ).withFailure().executeTest().assertTestSuiteResults( 1, 0, 1, 0,
                                                                                                           0 );

        verifyFailuresOneRetryOneMethod( outputValidator );

        outputValidator = unpack().addGoal( "-Dprovider=surefire-junit4" ).setJUnitVersion( "4.12" ).maven().addGoal(
            "-Dsurefire.rerunFailingTestsCount=1" ).addGoal( "-Dparallel=classes" ).addGoal(
            "-DuseUnlimitedThreads=true" ).addGoal(
            "-Dtest=FlakyFirstTimeTest#testFailing*" ).withFailure().executeTest().assertTestSuiteResults( 1, 0, 1, 0,
                                                                                                           0 );

        verifyFailuresOneRetryOneMethod( outputValidator );
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
        outputValidator.verifyTextInLog( "junit4.FlakyFirstTimeTest.testFailingTestOne" );
        outputValidator.verifyTextInLog( "ERROR" );
        outputValidator.verifyTextInLog( "junit4.FlakyFirstTimeTest.testErrorTestOne" );

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
