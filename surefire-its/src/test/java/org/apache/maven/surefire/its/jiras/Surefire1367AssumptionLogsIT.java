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
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1367">SUREFIRE-1367</a>
 * @since 2.20.1
 */
public class Surefire1367AssumptionLogsIT extends SurefireJUnit4IntegrationTestCase
{
    private static final String NL = System.getProperty( "line.separator" );

    @Test
    public void shouldSeeLogsParallelForked()
    {
        OutputValidator outputValidator = unpack().setForkJvm().forkMode(
                "once" ).parallelClassesAndMethods().disablePerCoreThreadCount().threadCountClasses(
                2 ).threadCountMethods( 2 ).executeTest().assertTestSuiteResults( 2, 0, 0, 2 );

        verifyReportA( outputValidator );
        verifyReportB( outputValidator );
    }

    @Test
    public void shouldSeeLogsParallelInPlugin()
    {
        OutputValidator outputValidator = unpack().setForkJvm().forkMode(
                "never" ).parallelClassesAndMethods().disablePerCoreThreadCount().threadCountClasses(
                2 ).threadCountMethods( 2 ).executeTest().assertTestSuiteResults( 2, 0, 0, 2 );

        verifyReportA( outputValidator );
        verifyReportB( outputValidator );
    }

    @Test
    public void shouldSeeLogsForked()
    {
        OutputValidator outputValidator = unpack().setForkJvm().forkMode( "once" ).executeTest().assertTestSuiteResults(
                2, 0, 0, 2 );

        verifyReportA( outputValidator );
        verifyReportB( outputValidator );
    }

    @Test
    public void shouldSeeLogsInPlugin()
    {
        OutputValidator outputValidator = unpack().setForkJvm().forkMode(
                "never" ).executeTest().assertTestSuiteResults( 2, 0, 0, 2 );

        verifyReportA( outputValidator );
        verifyReportB( outputValidator );
    }


    private SurefireLauncher unpack()
    {
        return unpack( "/surefire-1367" );
    }

    private void verifyReportA( OutputValidator outputValidator )
    {
        String xmlReport = outputValidator.getSurefireReportsXmlFile( "TEST-ATest.xml" ).readFileToString();

        String outputCData = "<system-out><![CDATA[Hi" + NL + NL + "There!" + NL + "]]></system-out>\n    "
                + "<system-err><![CDATA[Hello" + NL + NL + "What's up!" + NL + "]]></system-err>";

        assertThat( xmlReport ).contains( outputCData );


        String output = outputValidator.getSurefireReportsFile( "ATest-output.txt" ).readFileToString();

        String outputExpected = "Hi" + NL + NL + "There!" + NL + "Hello" + NL + NL + "What's up!" + NL;

        assertThat( output ).isEqualTo( outputExpected );
    }

    private void verifyReportB( OutputValidator outputValidator )
    {
        String xmlReport = outputValidator.getSurefireReportsXmlFile( "TEST-BTest.xml" ).readFileToString();

        String outputCData = "<system-out><![CDATA[Hey" + NL + NL + "you!" + NL + "]]></system-out>";

        assertThat( xmlReport ).contains( outputCData );


        String output = outputValidator.getSurefireReportsFile( "BTest-output.txt" ).readFileToString();

        String outputExpected = "Hey" + NL + NL + "you!" + NL;

        assertThat( output ).isEqualTo( outputExpected );
    }
}
