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

/**
 * @author <a href="mailto:tibordigana@apache.org">Tibor Digana (tibor17)</a>
 * @see <a href="https://issues.apache.org/jira/browse/SUREFIRE-1490">SUREFIRE-1490</a>
 * @since 3.0.0-M1
 */
public class Surefire1490ReportTitleDescriptionIT
        extends SurefireJUnit4IntegrationTestCase
{
    @Test
    public void shouldHaveDefaultReportTitleAndDescription()
    {
        OutputValidator validator = unpack()
                .addGoal( "verify" )
                .execute( "site" )
                .verifyErrorFreeLog();

        validator.getSiteFile( "project-reports.html" )
                .assertContainsText( "Surefire Report" )
                .assertContainsText( "Report on the test results of the project." )
                .assertContainsText( "Failsafe Report" )
                .assertContainsText( "Report on the integration test results of the project." );

        validator.getSiteFile( "failsafe-report.html" )
                .assertContainsText( "Failsafe Report" )
                .assertContainsText( "Surefire1490IT" );

        validator.getSiteFile( "surefire-report.html" )
                .assertContainsText( "Surefire Report" )
                .assertContainsText( "Surefire1490Test" );
    }

    @Test
    public void shouldHaveCustomizedReportTitleAndDescription()
    {
        OutputValidator validator = unpack()
                .sysProp( "failsafe.report.title", "failsafe title" )
                .sysProp( "failsafe.report.description", "failsafe desc" )
                .sysProp( "surefire.report.title", "surefire title" )
                .sysProp( "surefire.report.description", "surefire desc" )
                .addGoal( "verify" )
                .execute( "site" )
                .verifyErrorFreeLog();

        validator.getSiteFile( "project-reports.html" )
                .assertContainsText( "surefire title" )
                .assertContainsText( "surefire desc" )
                .assertContainsText( "failsafe title" )
                .assertContainsText( "failsafe desc" );

        validator.getSiteFile( "failsafe-report.html" )
                .assertContainsText( "failsafe title" )
                .assertContainsText( "Surefire1490IT" );

        validator.getSiteFile( "surefire-report.html" )
                .assertContainsText( "surefire title" )
                .assertContainsText( "Surefire1490Test" );
    }

    public SurefireLauncher unpack()
    {
        SurefireLauncher unpack = unpack( "surefire-1490" );
        unpack.sysProp( "user.language", "en" )
                .maven()
                .execute( "clean" );
        return unpack;
    }
}
