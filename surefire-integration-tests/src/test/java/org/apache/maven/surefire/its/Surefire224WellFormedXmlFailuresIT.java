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


import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;

import java.io.File;
import java.util.List;

/**
 * Test Surefire-224 (XML test reports are not well-formed when failure message contains quotes)
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class Surefire224WellFormedXmlFailuresIT
    extends AbstractSurefireIntegrationTestClass
{
    public void testWellFormedXmlFailures()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/surefire-224-wellFormedXmlFailures" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        // DGF Don't verify error free log; we expect failures
        // verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 4, 0, 4, 0, testDir );

        ReportTestSuite suite = (ReportTestSuite) HelperAssertions.extractReports( ( new File[]{ testDir } ) ).get( 0 );
        List testCases = suite.getTestCases();
        assertEquals( "Wrong number of test case objects", 4, testCases.size() );
        ReportTestCase current, testQuote = null, testLower = null, testGreater = null, testU0000 = null;
        for ( int i = 0; i < testCases.size(); i++ )
        {
            current = (ReportTestCase) testCases.get( i );
            if ( "testQuote".equals( current.getName() ) )
            {
                testQuote = current;
            }
            else if ( "testLower".equals( current.getName() ) )
            {
                testLower = current;
            }
            else if ( "testGreater".equals( current.getName() ) )
            {
                testGreater = current;
            }
            else if ( "testU0000".equals( current.getName() ) )
            {
                testU0000 = current;
            }
        }
        assertEquals( "Wrong error message", "\"", testQuote.getFailure().get( "message" ) );
        assertEquals( "Wrong error message", "<", testLower.getFailure().get( "message" ) );
        assertEquals( "Wrong error message", ">", testGreater.getFailure().get( "message" ) );
        // SUREFIRE-456 we have to doubly-escape non-visible control characters like \u0000
        assertEquals( "Wrong error message", "&#0;", testU0000.getFailure().get( "message" ) );
    }
}
