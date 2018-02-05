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

import java.io.File;

import org.apache.maven.surefire.its.fixture.IntegrationTestSuiteResults;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

import static org.apache.maven.surefire.its.fixture.HelperAssertions.parseTestResults;
import static org.apache.maven.surefire.its.fixture.HelperAssertions.assertTestSuiteResults;
import static org.junit.Assert.assertTrue;

/**
 * Test report aggregation
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 * @author <a href="mailto:krosenvold@apache.org">Kristian Rosenvold</a>
 */
public class AggregateReportIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void aggregateReport()
    {
        OutputValidator outputValidator = unpack( "/aggregate-report" ).addSurefireReportGoal().executeCurrentGoals();
        TestFile surefireReportHtml = outputValidator.getSiteFile( "surefire-report.html" );
        assertTrue( "surefire report missing: " + surefireReportHtml.getAbsolutePath(), surefireReportHtml.exists() );

        // TODO HtmlUnit tests on the surefire report

        IntegrationTestSuiteResults suite = parseTestResults( new File( outputValidator.getBaseDir(), "child1" ),
                                                              new File( outputValidator.getBaseDir(), "child2" ) );
        assertTestSuiteResults( 2, 0, 1, 0, suite );
    }
}
