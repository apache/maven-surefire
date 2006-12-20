package org.apache.maven.plugins.surefire.report;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;
import org.apache.maven.reporting.MavenReportException;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SurefireReportParserTest
    extends TestCase
{
    private SurefireReportParser report;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        report = new SurefireReportParser();

        report.setLocale( Locale.ENGLISH );
    }

    public void testParseXMLReportFiles()
        throws MavenReportException
    {
        report.setReportsDirectory( new File( System.getProperty( "basedir" ), "target/test-classes/test-reports" ) );

        List suites = report.parseXMLReportFiles();

        assertEquals( 4, suites.size() );

        Iterator it = suites.iterator();
        while ( it.hasNext() )
        {
            ReportTestSuite suite = (ReportTestSuite) it.next();
            assertNotNull( suite.getName() + " was not correctly parsed", suite.getTestCases() );
            assertNotNull( suite.getName() );
            assertNotNull( suite.getPackageName() );
        }
    }

    public void testParseTestSuiteName()
    {
        assertEquals( "CircleTest", report.parseTestSuiteName( "Battery: com.shape.CircleTest" ) );
    }

    public void testParseTestSuitePackageName()
    {
        assertEquals( "com.shape", report.parseTestSuitePackageName( "Battery: com.shape.CircleTest" ) );
    }

    public void testParseTestCaseName()
    {
        assertEquals( "testCase", report.parseTestCaseName( "testCase(com.shape.CircleTest)" ) );
    }

    public void testGetSummary()
        throws Exception
    {
        ReportTestSuite tSuite1 = new ReportTestSuite();

        ReportTestSuite tSuite2 = new ReportTestSuite();

        tSuite1.setNumberOfErrors( 10 );

        tSuite1.setNumberOfFailures( 20 );

        tSuite1.setNumberOfSkipped( 2 );

        tSuite1.setTimeElapsed( 1.0f );

        tSuite1.setNumberOfTests( 100 );

        tSuite2.setNumberOfErrors( 10 );

        tSuite2.setNumberOfFailures( 20 );
        
        tSuite2.setNumberOfSkipped( 2 );

        tSuite2.setTimeElapsed( 1.0f );

        tSuite2.setNumberOfTests( 100 );

        List suiteList = new ArrayList();

        suiteList.add( tSuite1 );

        suiteList.add( tSuite2 );

        Map testMap = report.getSummary( suiteList );

        assertEquals( 20, Integer.parseInt( testMap.get( "totalErrors" ).toString() ) );

        assertEquals( 40, Integer.parseInt( testMap.get( "totalFailures" ).toString() ) );

        assertEquals( 200, Integer.parseInt( testMap.get( "totalTests" ).toString() ) );

        assertEquals( 4, Integer.parseInt( testMap.get( "totalSkipped" ).toString() ) );

        NumberFormat numberFormat = report.getNumberFormat();

        assertEquals( 2.0f, numberFormat.parse( testMap.get( "totalElapsedTime" ).toString() )
            .floatValue(), 0.0f );

        assertEquals( 68.00f, numberFormat.parse( (String) testMap.get( "totalPercentage" ) )
            .floatValue(), 0 );
    }

    public void testSetReportsDirectory()
    {
        report.setReportsDirectory( new File( "Reports_Directory" ) );

        assertEquals( new File( "Reports_Directory" ), report.getReportsDirectory() );
    }

    public void testGetSuitesGroupByPackage()
    {
        ReportTestSuite tSuite1 = new ReportTestSuite();

        ReportTestSuite tSuite2 = new ReportTestSuite();

        ReportTestSuite tSuite3 = new ReportTestSuite();

        tSuite1.setPackageName( "Package1" );

        tSuite2.setPackageName( "Package1" );

        tSuite3.setPackageName( "Package2" );

        List suiteList = new ArrayList();

        suiteList.add( tSuite1 );

        suiteList.add( tSuite2 );

        suiteList.add( tSuite3 );

        Map groupMap = report.getSuitesGroupByPackage( suiteList );

        assertEquals( 2, groupMap.size() );

        assertEquals( tSuite1, ( (List) groupMap.get( "Package1" ) ).get( 0 ) );

        assertEquals( tSuite2, ( (List) groupMap.get( "Package1" ) ).get( 1 ) );

        assertEquals( tSuite3, ( (List) groupMap.get( "Package2" ) ).get( 0 ) );
    }

    public void testComputePercentage()
        throws Exception
    {
        NumberFormat numberFormat = report.getNumberFormat();

        assertEquals( 70.00f, numberFormat.parse( report.computePercentage( 100, 20, 10, 0 ) )
            .floatValue(), 0 );
    }

    public void testGetFailureDetails()
    {
        ReportTestSuite tSuite1 = new ReportTestSuite();

        ReportTestSuite tSuite2 = new ReportTestSuite();

        ReportTestCase tCase1 = new ReportTestCase();

        ReportTestCase tCase2 = new ReportTestCase();

        ReportTestCase tCase3 = new ReportTestCase();

        tCase1.addFailure( null, null );

        tCase3.addFailure( null, null );

        List tCaseList = new ArrayList();

        List tCaseList2 = new ArrayList();

        tCaseList.add( tCase1 );

        tCaseList.add( tCase2 );

        tCaseList2.add( tCase3 );

        tSuite1.setTestCases( tCaseList );

        tSuite2.setTestCases( tCaseList2 );

        List suiteList = new ArrayList();

        suiteList.add( tSuite1 );

        suiteList.add( tSuite2 );

        List failList = report.getFailureDetails( suiteList );

        assertEquals( 2, failList.size() );

        assertEquals( tCase1, failList.get( 0 ) );

        assertEquals( tCase3, failList.get( 1 ) );
    }
}
