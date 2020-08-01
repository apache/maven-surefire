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

import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.surefire.its.fixture.HelperAssertions;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test Surefire-224 (XML test reports are not well-formed when failure message contains quotes)
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class Surefire224WellFormedXmlFailuresIT
    extends SurefireJUnit4IntegrationTestCase
{
    @SuppressWarnings( "ConstantConditions" )
    @Test
    public void testWellFormedXmlFailures()
    {
        OutputValidator outputValidator = unpack( "/surefire-224-wellFormedXmlFailures" ).executeTest();

        outputValidator.assertTestSuiteResults( 4, 0, 4, 0 );

        ReportTestSuite suite = HelperAssertions.extractReports( outputValidator.getBaseDir() ).get( 0 );
        List<org.apache.maven.plugins.surefire.report.ReportTestCase> testCases = suite.getTestCases();
        assertEquals( "Wrong number of test case objects", 4, testCases.size() );
        ReportTestCase testQuote = null, testLower = null, testGreater = null, testU0000 = null;
        for ( ReportTestCase current : testCases )
        {
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
        assertEquals( "Wrong error message", "\"", testQuote.getFailureMessage() );
        assertEquals( "Wrong error message", "<", testLower.getFailureMessage() );
        assertEquals( "Wrong error message", ">", testGreater.getFailureMessage() );
        // SUREFIRE-456 we have to doubly-escape non-visible control characters like \u0000
        assertEquals( "Wrong error message", "Hi &#0; there!", testU0000.getFailureMessage() );
    }
}
