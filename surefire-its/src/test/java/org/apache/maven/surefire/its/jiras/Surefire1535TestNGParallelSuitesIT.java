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

import java.nio.charset.Charset;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

public class Surefire1535TestNGParallelSuitesIT
        extends SurefireJUnit4IntegrationTestCase
{
    private static final Charset UTF8 = Charset.forName( "UTF-8" );

    @Test
    public void forks2() throws VerificationException
    {
        OutputValidator validator = unpack()
                .activateProfile( "forked-reports-directory" )
                .forkCount( 2 )
                .executeTest();

        TestFile testFile = validator.getSurefireReportsFile( "../surefire-reports-1/TEST-TestSuite.xml", UTF8 );
        testFile.assertFileExists();
        testFile.assertContainsText( "<testcase name=\"test\" classname=\"it.ParallelTest" );

        testFile = validator.getSurefireReportsFile( "../surefire-reports-2/TEST-TestSuite.xml", UTF8 );
        testFile.assertFileExists();
        testFile.assertContainsText( "<testcase name=\"test\" classname=\"it.ParallelTest" );

        validator.assertThatLogLine( containsString( "Tests run: 2, Failures: 0, Errors: 0, Skipped: 0" ), is( 1 ) )
                .assertThatLogLine( containsString( "Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, " ), is( 2 ) )
                .assertThatLogLine( containsString( "Suite1.xml" ), is( 1 ) )
                .assertThatLogLine( containsString( "Suite2.xml" ), is( 1 ) )
                .assertThatLogLine( containsString( "test 1" ), is( 1 ) )
                .assertThatLogLine( containsString( "test 2" ), is( 1 ) )
                .assertThatLogLine( containsString( "Running TestSuite" ), is( 2 ) );
    }

    @Test
    public void forks2Redirected() throws VerificationException
    {
        OutputValidator validator = unpack()
                .activateProfile( "forked-reports-directory" )
                .forkCount( 2 )
                .redirectToFile( true )
                .executeTest();

        TestFile testFile = validator.getSurefireReportsFile( "../surefire-reports-1/TEST-TestSuite.xml", UTF8 );
        testFile.assertFileExists();
        testFile.assertContainsText( "<testcase name=\"test\" classname=\"it.ParallelTest" );

        testFile = validator.getSurefireReportsFile( "../surefire-reports-2/TEST-TestSuite.xml", UTF8 );
        testFile.assertFileExists();
        testFile.assertContainsText( "<testcase name=\"test\" classname=\"it.ParallelTest" );

        validator.assertThatLogLine( containsString( "Tests run: 2, Failures: 0, Errors: 0, Skipped: 0" ), is( 1 ) )
                .assertThatLogLine( containsString( "Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, " ), is( 2 ) )
                .assertThatLogLine( containsString( "Running TestSuite" ), is( 2 ) );

        TestFile outFile = validator.getSurefireReportsFile( "../surefire-reports-1/TestSuite-output.txt" );
        outFile.assertFileExists();
        outFile.assertContainsText( anyOf( containsString( "Suite1.xml" ), containsString( "Suite2.xml" ) ) );
        outFile.assertContainsText( anyOf( containsString( "test 1" ), containsString( "test 2" ) ) );

        outFile = validator.getSurefireReportsFile( "../surefire-reports-2/TestSuite-output.txt" );
        outFile.assertFileExists();
        outFile.assertContainsText( anyOf( containsString( "Suite1.xml" ), containsString( "Suite2.xml" ) ) );
        outFile.assertContainsText( anyOf( containsString( "test 1" ), containsString( "test 2" ) ) );
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

        validator.assertThatLogLine( containsString( "Suite1.xml" ), is( 1 ) )
                .assertThatLogLine( containsString( "Suite2.xml" ), is( 1 ) )
                .assertThatLogLine( containsString( "test 1" ), is( 1 ) )
                .assertThatLogLine( containsString( "test 2" ), is( 1 ) )
                .assertThatLogLine( containsString( "Running TestSuite" ), is( 1 ) );
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

        validator.assertThatLogLine( containsString( "Running TestSuite" ), is( 1 ) );

        TestFile outFile = validator.getSurefireReportsFile( "TestSuite-output.txt" );
        outFile.assertFileExists();
        outFile.assertContainsText( "Suite1.xml" );
        outFile.assertContainsText( "Suite1.xml" );
        outFile.assertContainsText( "test 1" );
        outFile.assertContainsText( "test 2" );
    }

    private SurefireLauncher unpack()
    {
        return unpack("/surefire-1535-parallel-testng");
    }
}
