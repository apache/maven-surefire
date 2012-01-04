package org.apache.maven.surefire.its.misc;
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


import org.apache.commons.io.FileUtils;
import org.apache.maven.surefire.its.fixture.SurefireVerifierTestClass;

import java.io.File;

/**
 * Test Surefire-740 Truncated comma with non us locale
 *
 * @author Kristian Rosenvold
 */
public class Surefire772SpecifiedReportsIT
    extends SurefireVerifierTestClass
{

    public Surefire772SpecifiedReportsIT()
    {
        super( "/surefire-772-specified-reports" );
    }

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        FileUtils.deleteDirectory( getTargetFile( "site" ) );
        addGoal( "-Dclean.skip=true" );
    }

    public void testReportGeneration()
        throws Exception
    {
        failNever();
        addGoal( getFailsafeReportOnlyGoal() );
        assertFalse( "Expecting not site directory", getTargetFile( "site" ).isDirectory() );
        execute( getSurefireReportOnlyGoal() );

        File siteFile = getSiteFile( "surefire-report.html" );
        System.out.println( "siteFile.getAbsolutePath() = " + siteFile.getAbsolutePath() );
        assertTrue( "Expecting surefire report file", siteFile.isFile() );

        siteFile = getSiteFile( "failsafe-report.html" );
        System.out.println( "siteFile.getAbsolutePath() = " + siteFile.getAbsolutePath() );
        assertTrue( "Expecting failsafe report file", siteFile.isFile() );
    }

    public void testSkippedFailsafeReportGeneration()
        throws Exception
    {
        failNever();
        activateProfile( "skipFailsafe" );
        addGoal( getFailsafeReportOnlyGoal() );
        assertFalse( "Expecting not site directory", getTargetFile( "site" ).isDirectory() );
        execute( getSurefireReportOnlyGoal() );

        File siteFile = getSiteFile( "surefire-report.html" );
        System.out.println( "siteFile.getAbsolutePath() = " + siteFile.getAbsolutePath() );
        assertTrue( "Expecting surefire report file", siteFile.isFile() );

        siteFile = getSiteFile( "failsafe-report.html" );
        System.out.println( "siteFile.getAbsolutePath() = " + siteFile.getAbsolutePath() );
        assertFalse( "Expecting no failsafe report file", siteFile.isFile() );
    }

    public void testSkippedSurefireReportGeneration()
        throws Exception
    {
        failNever();
        activateProfile( "skipSurefire" );
        addGoal( getFailsafeReportOnlyGoal() );
        assertFalse( "Expecting not site directory", getTargetFile( "site" ).isDirectory() );
        execute( getSurefireReportOnlyGoal() );

        File siteFile = getSiteFile( "surefire-report.html" );
        System.out.println( "siteFile.getAbsolutePath() = " + siteFile.getAbsolutePath() );
        assertFalse( "Expecting no surefire report file", siteFile.isFile() );

        siteFile = getSiteFile( "failsafe-report.html" );
        System.out.println( "siteFile.getAbsolutePath() = " + siteFile.getAbsolutePath() );
        assertTrue( "Expecting failsafe report file", siteFile.isFile() );
    }

}
