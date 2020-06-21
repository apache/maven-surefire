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
import org.junit.Test;

/**
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1146">SUREFIRE-1146</a>
 */
public class Surefire1146RerunFailedAndParameterizedIT
    extends SurefireJUnit4IntegrationTestCase
{
    
    @Test
    public void testsAreRerun()
    {
        OutputValidator outputValidator = unpack( "surefire-1146-rerunFailingTests-with-Parameterized" ).executeTest();
        verify( outputValidator, 8, 0, 0, 0, 5 );
    }

    @SuppressWarnings( "checkstyle:linelength" )
    private void verify( OutputValidator outputValidator, int run, int failures, int errors, int skipped, int flakes )
    {
        outputValidator.verifyTextInLog( "Flakes:" );
        outputValidator.verifyTextInLog( "jiras.surefire1146.CustomDescriptionParameterizedTest.flakyTest[0: (Test11); Test12; Test13;]" );
        outputValidator.verifyTextInLog( "Run 1: CustomDescriptionParameterizedTest.flakyTest:" );
        outputValidator.verifyTextInLog( "Run 2: CustomDescriptionParameterizedTest.flakyTest:" );
        outputValidator.verifyTextInLog( "Run 3: PASS" );

        outputValidator.getSurefireReportsXmlFile( "TEST-jiras.surefire1146.CustomDescriptionParameterizedTest.xml" )
            .assertContainsText( "<testcase name=\"flakyTest[0: (Test11); Test12; Test13;]\" classname=\"jiras.surefire1146.CustomDescriptionParameterizedTest\"" )
            .assertContainsText( "<testcase name=\"flakyTest[1: (Test21); Test22; Test23;]\" classname=\"jiras.surefire1146.CustomDescriptionParameterizedTest\"" );

        outputValidator.verifyTextInLog( "jiras.surefire1146.CustomDescriptionWithCommaParameterizedTest.flakyTest[0: (Test11), Test12, Test13;]" );
        outputValidator.verifyTextInLog( "Run 1: CustomDescriptionWithCommaParameterizedTest.flakyTest:" );
        outputValidator.verifyTextInLog( "Run 2: CustomDescriptionWithCommaParameterizedTest.flakyTest:" );
        outputValidator.verifyTextInLog( "Run 3: PASS" );

        outputValidator.getSurefireReportsXmlFile( "TEST-jiras.surefire1146.CustomDescriptionWithCommaParameterizedTest.xml" )
            .assertContainsText( "<testcase name=\"flakyTest[0: (Test11), Test12, Test13;]\" classname=\"jiras.surefire1146.CustomDescriptionWithCommaParameterizedTest\"" )
            .assertContainsText( "<testcase name=\"flakyTest[1: (Test21), Test22, Test23;]\" classname=\"jiras.surefire1146.CustomDescriptionWithCommaParameterizedTest\"" )
            .assertContainsText( "<testcase name=\"flakyTest[2: (Test31), Test32, Test33;]\" classname=\"jiras.surefire1146.CustomDescriptionWithCommaParameterizedTest\"" );
        
        outputValidator.verifyTextInLog( "jiras.surefire1146.CustomDescriptionWithCommaParameterizedTest.flakyTest[2: (Test31), Test32, Test33;]" );
        outputValidator.verifyTextInLog( "Run 1: CustomDescriptionWithCommaParameterizedTest.flakyTest:" );
        outputValidator.verifyTextInLog( "Run 2: PASS" );
        
        outputValidator.verifyTextInLog( "jiras.surefire1146.SimpleParameterizedTest.flakyTest[0]" );
        outputValidator.verifyTextInLog( "Run 1: SimpleParameterizedTest.flakyTest:" );
        outputValidator.verifyTextInLog( "Run 2: SimpleParameterizedTest.flakyTest:" );
        outputValidator.verifyTextInLog( "Run 3: PASS" );
        outputValidator.getSurefireReportsXmlFile( "TEST-jiras.surefire1146.SimpleParameterizedTest.xml" )
            .assertContainsText( "<testcase name=\"flakyTest[0]\" classname=\"jiras.surefire1146.SimpleParameterizedTest\"" );
        
        outputValidator.verifyTextInLog( "jiras.surefire1146.StandardTest.flakyTest" );
        outputValidator.verifyTextInLog( "Run 1: StandardTest.flakyTest:" );
        outputValidator.verifyTextInLog( "Run 2: PASS" );
        outputValidator.getSurefireReportsXmlFile( "TEST-jiras.surefire1146.StandardTest.xml" )
            .assertContainsText( "<testcase name=\"flakyTest\" classname=\"jiras.surefire1146.StandardTest\"" );

        verifyStatistics( outputValidator, run, failures, errors, skipped, flakes );
    }
    
    private void verifyStatistics( OutputValidator outputValidator, int run, int failures, int errors, int skipped,
                                   int flakes )
    {
        outputValidator.verifyTextInLog( "Tests run: " + run + ", Failures: " + failures + ", Errors: " + errors
                                             + ", Skipped: " + skipped + ", Flakes: " + flakes );
    }
}
