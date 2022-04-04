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

import java.io.IOException;
import org.apache.maven.it.VerificationException;
import org.apache.maven.surefire.its.fixture.OutputValidator;
import org.apache.maven.surefire.its.fixture.SurefireJUnit4IntegrationTestCase;
import org.apache.maven.surefire.its.fixture.SurefireLauncher;
import org.apache.maven.surefire.its.fixture.TestFile;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test Surefire-740 Truncated comma with non us locale
 *
 * @author Kristian Rosenvold
 */
public class Surefire772NoFailsafeReportsIT
    extends SurefireJUnit4IntegrationTestCase
{

    @Test
    public void testReportGeneration()
        throws Exception
    {
        final OutputValidator site =
            unpack().addFailsafeReportOnlyGoal().addSurefireReportOnlyGoal().executeCurrentGoals();

        assertSurefireReportPresent( site );
        assertNoFailsefeReport( site );
    }

    @Test
    public void testSkippedFailsafeReportGeneration()
        throws Exception
    {
        final OutputValidator validator = unpack().activateProfile(
            "skipFailsafe" ).addFailsafeReportOnlyGoal().addSurefireReportOnlyGoal().executeCurrentGoals();
        assertSurefireReportPresent( validator );
        assertNoFailsefeReport( validator );

    }

    @Test
    public void testForcedFailsafeReportGeneration()
        throws Exception
    {
        final OutputValidator validator = unpack().activateProfile(
            "forceFailsafe" ).addFailsafeReportOnlyGoal().addSurefireReportOnlyGoal().executeCurrentGoals();
        assertSurefireReportPresent( validator );
        assertFailsafeReport( validator );
    }

    @Test
    public void testSkipForcedFailsafeReportGeneration()
        throws Exception
    {
        final OutputValidator validator = unpack().activateProfile( "forceFailsafe" ).activateProfile(
            "skipFailsafe" ).addFailsafeReportOnlyGoal().addSurefireReportOnlyGoal().executeCurrentGoals();

        assertSurefireReportPresent( validator );
        assertNoFailsefeReport( validator );
    }

    private void assertNoFailsefeReport( OutputValidator site )
    {
        TestFile siteFile = site.getSiteFile( "failsafe-report.html" );
        assertFalse( "Expecting no failsafe report file", siteFile.isFile() );
    }

    private void assertFailsafeReport( OutputValidator site )
    {
        TestFile siteFile = site.getSiteFile( "failsafe-report.html" );
        assertTrue( "Expecting no failsafe report file", siteFile.isFile() );
    }

    private void assertSurefireReportPresent( OutputValidator site )
    {
        TestFile siteFile = site.getSiteFile( "surefire-report.html" );
        assertTrue( "Expecting surefire report file", siteFile.isFile() );
    }

    private SurefireLauncher unpack()
        throws VerificationException, IOException
    {
        final SurefireLauncher unpack = unpack( "surefire-772-no-failsafe-reports" );
        unpack.maven().deleteSiteDir().skipClean().failNever().verifyFileNotPresent( "site" );
        return unpack;
    }

}
