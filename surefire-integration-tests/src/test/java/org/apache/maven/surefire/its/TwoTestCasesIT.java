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
import org.apache.maven.plugins.surefire.report.ReportTestSuite;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Test running two test cases; confirms reporting works correctly
 *
 * @author <a href="mailto:dfabulich@apache.org">Dan Fabulich</a>
 */
public class TwoTestCasesIT
    extends AbstractSurefireIntegrationTestClass
{
    public void testTwoTestCases()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/junit-twoTestCases" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        HelperAssertions.assertTestSuiteResults( 2, 0, 0, 0, testDir );
    }

    /**
     * Runs two tests encapsulated in a suite
     */
    public void testTwoTestCaseSuite()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/junit-twoTestCaseSuite" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();
        List reports = HelperAssertions.extractReports( ( new File[]{ testDir } ) );
        Set classNames = extractClassNames( reports );
        assertContains( classNames, "junit.twoTestCaseSuite.BasicTest" );
        assertContains( classNames, "junit.twoTestCaseSuite.TestTwo" );
        assertEquals( "wrong number of classes", 2, classNames.size() );
        IntegrationTestSuiteResults results = HelperAssertions.parseReportList( reports );
        HelperAssertions.assertTestSuiteResults( 2, 0, 0, 0, results );
    }

    private void assertContains( Set set, String expected )
    {
        if ( set.contains( expected ) )
        {
            return;
        }
        fail( "Set didn't contain " + expected );
    }

    private Set extractClassNames( List reports )
    {
        ReportTestSuite suite;
        HashSet classNames = new HashSet();
        for ( int i = 0; i < reports.size(); i++ )
        {
            suite = (ReportTestSuite) reports.get( i );
            classNames.add( suite.getFullClassName() );
        }
        return classNames;
    }

    public void testJunit4Suite()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/junit4-twoTestCaseSuite" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List reports = HelperAssertions.extractReports( ( new File[]{ testDir } ) );
        Set classNames = extractClassNames( reports );
        assertContains( classNames, "twoTestCaseSuite.BasicTest" );
        assertContains( classNames, "twoTestCaseSuite.Junit4TestTwo" );
        assertEquals( "wrong number of classes", 2, classNames.size() );
        IntegrationTestSuiteResults results = HelperAssertions.parseReportList( reports );
        HelperAssertions.assertTestSuiteResults( 2, 0, 0, 0, results );
    }

    public void testTestNGSuite()
        throws Exception
    {
        File testDir = ResourceExtractor.simpleExtractResources( getClass(), "/testng-twoTestCaseSuite" );

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        this.executeGoal( verifier, "test" );
        verifier.verifyErrorFreeLog();
        verifier.resetStreams();

        List reports = HelperAssertions.extractReports( ( new File[]{ testDir } ) );
        Set classNames = extractClassNames( reports );
        assertContains( classNames, "testng.two.TestNGTestTwo" );
        assertContains( classNames, "testng.two.TestNGSuiteTest" );
        assertEquals( "wrong number of classes", 2, classNames.size() );
        IntegrationTestSuiteResults results = HelperAssertions.parseReportList( reports );
        HelperAssertions.assertTestSuiteResults( 2, 0, 0, 0, results );
    }

}
