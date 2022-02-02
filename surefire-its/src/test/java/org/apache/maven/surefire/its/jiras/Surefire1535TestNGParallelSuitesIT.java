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
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class Surefire1535TestNGParallelSuitesIT
        extends SurefireJUnit4IntegrationTestCase
{
    private static final String TEST_RESULT_1 =
            platformEncoding( "Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, " );
    private static final String TEST_RESULT_2 =
            platformEncoding( "Tests run: 2, Failures: 0, Errors: 0, Skipped: 0" );
    private static final String SUITE1 = platformEncoding( "Suite1" );
    private static final String SUITE2 = platformEncoding( "Suite2" );
    private static final String TEST1 = platformEncoding( "test 1" );
    private static final String TEST2 = platformEncoding( "test 2" );
    private static final String TEST_SUITE = platformEncoding( "Running TestSuite" );

    @Test
    public void forks2() throws VerificationException
    {
        OutputValidator validator = unpack()
                .activateProfile( "forked-reports-directory" )
                .forkCount( 2 )
                .executeTest();

        TestFile testFile = validator.getSurefireReportsFile( "../surefire-reports-1/TEST-TestSuite.xml", UTF_8 );
        testFile.assertFileExists();
        testFile.assertContainsText( "<testcase name=\"test\" classname=\"it.ParallelTest" );
        String xml = testFile.readFileToString();
        boolean parallelTest11 = xml.contains( "<testcase name=\"test\" classname=\"it.ParallelTest1\"" );
        boolean parallelTest12 = xml.contains( "<testcase name=\"test\" classname=\"it.ParallelTest2\"" );
        assertThat( parallelTest11 ^ parallelTest12 )
                .isTrue();

        testFile = validator.getSurefireReportsFile( "../surefire-reports-2/TEST-TestSuite.xml", UTF_8 );
        testFile.assertFileExists();
        testFile.assertContainsText( "<testcase name=\"test\" classname=\"it.ParallelTest" );
        xml = testFile.readFileToString();
        boolean parallelTest21 = xml.contains( "<testcase name=\"test\" classname=\"it.ParallelTest1\"" );
        boolean parallelTest22 = xml.contains( "<testcase name=\"test\" classname=\"it.ParallelTest2\"" );
        assertThat( parallelTest21 ^ parallelTest22 )
                .isTrue();

        assertThat( parallelTest11 && parallelTest22 || parallelTest12 && parallelTest21 )
                .isTrue();

        validator.assertThatLogLine( containsString( TEST_RESULT_2 ), is( 1 ) )
                .assertThatLogLine( containsString( TEST_RESULT_1 ), is( 2 ) )
                .assertThatLogLine( containsString( SUITE1 ), is( 1 ) )
                .assertThatLogLine( containsString( SUITE2 ), is( 1 ) )
                .assertThatLogLine( containsString( TEST1 ), is( 1 ) )
                .assertThatLogLine( containsString( TEST2 ), is( 1 ) )
                .assertThatLogLine( containsString( TEST_SUITE ), is( 2 ) );
    }

    @Test
    public void forks2Redirected() throws VerificationException
    {
        OutputValidator validator = unpack()
                .activateProfile( "forked-reports-directory" )
                .forkCount( 2 )
                .redirectToFile( true )
                .executeTest();

        TestFile testFile = validator.getSurefireReportsFile( "../surefire-reports-1/TEST-TestSuite.xml", UTF_8 );
        testFile.assertFileExists();
        testFile.assertContainsText( "<testcase name=\"test\" classname=\"it.ParallelTest" );
        String xml = testFile.readFileToString();
        boolean parallelTest11 = xml.contains( "<testcase name=\"test\" classname=\"it.ParallelTest1\"" );
        boolean parallelTest12 = xml.contains( "<testcase name=\"test\" classname=\"it.ParallelTest2\"" );
        assertThat( parallelTest11 ^ parallelTest12 )
                .isTrue();
        String log = validator.getSurefireReportsFile( "../surefire-reports-1/TestSuite-output.txt", UTF_8 )
                .readFileToString();
        assertThat( log.contains( TEST1 ) )
                .isEqualTo( parallelTest11 );
        assertThat( log.contains( SUITE1 ) )
                .isEqualTo( parallelTest11 );
        assertThat( log.contains( TEST2 ) )
                .isEqualTo( parallelTest12 );
        assertThat( log.contains( SUITE2 ) )
                .isEqualTo( parallelTest12 );

        testFile = validator.getSurefireReportsFile( "../surefire-reports-2/TEST-TestSuite.xml", UTF_8 );
        testFile.assertFileExists();
        testFile.assertContainsText( "<testcase name=\"test\" classname=\"it.ParallelTest" );
        xml = testFile.readFileToString();
        boolean parallelTest21 = xml.contains( "<testcase name=\"test\" classname=\"it.ParallelTest1\"" );
        boolean parallelTest22 = xml.contains( "<testcase name=\"test\" classname=\"it.ParallelTest2\"" );
        assertThat( parallelTest21 ^ parallelTest22 )
                .isTrue();
        log = validator.getSurefireReportsFile( "../surefire-reports-2/TestSuite-output.txt", UTF_8 )
                .readFileToString();
        assertThat( log.contains( TEST1 ) )
                .isEqualTo( parallelTest21 );
        assertThat( log.contains( SUITE1 ) )
                .isEqualTo( parallelTest21 );
        assertThat( log.contains( TEST2 ) )
                .isEqualTo( parallelTest22 );
        assertThat( log.contains( SUITE2 ) )
                .isEqualTo( parallelTest22 );

        assertThat( parallelTest11 && parallelTest22 || parallelTest12 && parallelTest21 )
                .isTrue();

        validator.assertThatLogLine( containsString( TEST_RESULT_2 ), is( 1 ) )
                .assertThatLogLine( containsString( TEST_RESULT_1 ), is( 2 ) )
                .assertThatLogLine( containsString( TEST_SUITE ), is( 2 ) );

        TestFile outFile = validator.getSurefireReportsFile( "../surefire-reports-1/TestSuite-output.txt" );
        outFile.assertFileExists();
        outFile.assertContainsText( anyOf( containsString( SUITE1 ), containsString( SUITE2 ) ) );
        outFile.assertContainsText( anyOf( containsString( TEST1 ), containsString( TEST2 ) ) );

        outFile = validator.getSurefireReportsFile( "../surefire-reports-2/TestSuite-output.txt" );
        outFile.assertFileExists();
        outFile.assertContainsText( anyOf( containsString( SUITE1 ), containsString( SUITE2 ) ) );
        outFile.assertContainsText( anyOf( containsString( TEST1 ), containsString( TEST2 ) ) );
    }

    @Test
    public void forks0() throws VerificationException
    {
        OutputValidator validator = unpack()
                .forkCount( 0 )
                .executeTest();

        TestFile testFile = validator.getSurefireReportsFile( "TEST-TestSuite.xml" );
        testFile.assertFileExists();
        testFile.assertContainsText( "<testcase name=\"test\" classname=\"it.ParallelTest1\"" );
        testFile.assertContainsText( "<testcase name=\"test\" classname=\"it.ParallelTest2\"" );

        validator.assertThatLogLine( containsString( SUITE1 ), is( 1 ) )
                .assertThatLogLine( containsString( SUITE2 ), is( 1 ) )
                .assertThatLogLine( containsString( TEST1 ), is( 1 ) )
                .assertThatLogLine( containsString( TEST2 ), is( 1 ) )
                .assertThatLogLine( containsString( TEST_SUITE ), is( 1 ) );
    }

    @Test
    public void forks0Redirected() throws VerificationException
    {
        OutputValidator validator = unpack()
                .forkCount( 0 )
                .redirectToFile( true )
                .executeTest();

        TestFile testFile = validator.getSurefireReportsXmlFile( "TEST-TestSuite.xml" );
        testFile.assertFileExists();
        testFile.assertContainsText( "<testcase name=\"test\" classname=\"it.ParallelTest1\"" );
        testFile.assertContainsText( "<testcase name=\"test\" classname=\"it.ParallelTest2\"" );

        validator.assertThatLogLine( containsString( TEST_SUITE ), is( 1 ) );

        TestFile outFile = validator.getSurefireReportsFile( "TestSuite-output.txt" );
        outFile.assertFileExists();
        outFile.assertContainsText( SUITE1 );
        outFile.assertContainsText( SUITE2 );
        outFile.assertContainsText( TEST1 );
        outFile.assertContainsText( TEST2 );
    }

    private SurefireLauncher unpack()
    {
        return unpack( "/surefire-1535-parallel-testng" );
    }

    private static String platformEncoding( String text )
    {
        return new String( text.getBytes( UTF_8 ) );
    }
}
