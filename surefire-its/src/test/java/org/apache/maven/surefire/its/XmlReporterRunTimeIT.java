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

import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.junit.Test;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.extractReports;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

/**
 * Test reported runtime
 *
 * @author Kristian Rosenvold
 */
@SuppressWarnings( { "checkstyle:magicnumber", "checkstyle:linelength" } )
public class XmlReporterRunTimeIT
    extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void testForkModeAlways()
        throws Exception
    {
        // just generate .surefire-<hash> in order to apply runOrder
        unpack( "/runorder-parallel" )
            .executeTest()
            .verifyErrorFree( 9 );

        // now assert test results match expected values
        OutputValidator outputValidator = unpack( "/runorder-parallel" )
            .executeTest()
            .verifyErrorFree( 9 );

        for ( ReportTestSuite report : extractReports( outputValidator.getBaseDir() ) )
        {
            if ( "runorder.parallel.Test1".equals( report.getFullClassName() ) )
            {
                // should be 6f but because of having Windows sleep discrepancy it is 5.95f
                assertThat( "runorder.parallel.Test1 report.getTimeElapsed found:" + report.getTimeElapsed(),
                            report.getTimeElapsed(), allOf( greaterThanOrEqualTo( 5.95f ), lessThan( 7f ) ) );
            }
            else if ( "runorder.parallel.Test2".equals( report.getFullClassName() ) )
            {
                // should be 5f but because of having Windows sleep discrepancy it is 4.95f
                assertThat( "runorder.parallel.Test2 report.getTimeElapsed found:" + report.getTimeElapsed(),
                            report.getTimeElapsed(), allOf( greaterThanOrEqualTo( 4.95f ), lessThan( 6f ) ) );
            }
            else
            {
                System.out.println( "report = " + report );
            }
        }
    }
}
