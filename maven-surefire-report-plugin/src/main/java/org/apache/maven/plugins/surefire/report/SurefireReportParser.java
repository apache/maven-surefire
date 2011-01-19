package org.apache.maven.plugins.surefire.report;

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
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;
import org.xml.sax.SAXException;

/**
 * @version $Id$
 */
public class SurefireReportParser
{
    private NumberFormat numberFormat = NumberFormat.getInstance();

    private List reportsDirectories;

    private List testSuites = new ArrayList();

    private static final int PCENT = 100;

    public SurefireReportParser()
    {
    }

    public SurefireReportParser( List reportsDirectoriesFiles , Locale locale )
    {
        this.reportsDirectories = reportsDirectoriesFiles;

        setLocale( locale );
    }

    public List parseXMLReportFiles()
        throws MavenReportException
    {
        List xmlReportFileList = new ArrayList();
        for ( int i = 0; i < reportsDirectories.size(); i++ )
        {
            File reportsDirectory = (File) reportsDirectories.get(i);
            if ( !reportsDirectory.exists() )
            {
                continue;
            }
            String[] xmlReportFiles =
                getIncludedFiles( reportsDirectory, "*.xml",
                                  "*.txt, testng-failed.xml, testng-failures.xml, testng-results.xml" );
            for ( int j = 0; j < xmlReportFiles.length; j++ )
            {
                File xmlReport = new File( reportsDirectory, xmlReportFiles[j] );
                xmlReportFileList.add( xmlReport );
            }
        }
        TestSuiteXmlParser parser = new TestSuiteXmlParser();
        for ( int index = 0; index < xmlReportFileList.size(); index++ )
        {
            Collection suites;

            File currentReport = (File) xmlReportFileList.get( index );

            try
            {
                suites = parser.parse( currentReport.getAbsolutePath() );
            }
            catch ( ParserConfigurationException e )
            {
                throw new MavenReportException( "Error setting up parser for JUnit XML report", e );
            }
            catch ( SAXException e )
            {
                throw new MavenReportException( "Error parsing JUnit XML report " + currentReport, e );
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Error reading JUnit XML report " + currentReport, e );
            }

            testSuites.addAll( suites );
        }

        return testSuites;
    }

    protected String parseTestSuiteName( String lineString )
    {
        return lineString.substring( lineString.lastIndexOf( "." ) + 1, lineString.length() );
    }

    protected String parseTestSuitePackageName( String lineString )
    {
        return lineString.substring( lineString.indexOf( ":" ) + 2, lineString.lastIndexOf( "." ) );
    }

    protected String parseTestCaseName( String lineString )
    {
        return lineString.substring( 0, lineString.indexOf( "(" ) );
    }

    public Map getSummary( List suites )
    {
        Map totalSummary = new HashMap();

        ListIterator iter = suites.listIterator();

        int totalNumberOfTests = 0;

        int totalNumberOfErrors = 0;

        int totalNumberOfFailures = 0;

        int totalNumberOfSkipped = 0;

        float totalElapsedTime = 0.0f;

        while ( iter.hasNext() )
        {
            ReportTestSuite suite = (ReportTestSuite) iter.next();

            totalNumberOfTests += suite.getNumberOfTests();

            totalNumberOfErrors += suite.getNumberOfErrors();

            totalNumberOfFailures += suite.getNumberOfFailures();

            totalNumberOfSkipped += suite.getNumberOfSkipped();

            totalElapsedTime += suite.getTimeElapsed();
        }

        String totalPercentage =
            computePercentage( totalNumberOfTests, totalNumberOfErrors, totalNumberOfFailures, totalNumberOfSkipped );

        totalSummary.put( "totalTests", Integer.toString( totalNumberOfTests ) );

        totalSummary.put( "totalErrors", Integer.toString( totalNumberOfErrors ) );

        totalSummary.put( "totalFailures", Integer.toString( totalNumberOfFailures ) );

        totalSummary.put( "totalSkipped", Integer.toString( totalNumberOfSkipped ) );

        totalSummary.put( "totalElapsedTime", numberFormat.format( totalElapsedTime ) );

        totalSummary.put( "totalPercentage", totalPercentage );

        return totalSummary;
    }

    public void setReportsDirectory( File reportsDirectory )
    {
        this.reportsDirectories = Collections.singletonList( reportsDirectory );
    }

    public final void setLocale( Locale locale )
    {
        numberFormat = NumberFormat.getInstance( locale );
    }

    public NumberFormat getNumberFormat()
    {
        return this.numberFormat;
    }

    public Map getSuitesGroupByPackage( List testSuitesList )
    {
        ListIterator iter = testSuitesList.listIterator();

        Map suitePackage = new HashMap();

        while ( iter.hasNext() )
        {
            ReportTestSuite suite = (ReportTestSuite) iter.next();

            List suiteList = new ArrayList();

            if ( suitePackage.get( suite.getPackageName() ) != null )
            {
                suiteList = (List) suitePackage.get( suite.getPackageName() );
            }

            suiteList.add( suite );

            suitePackage.put( suite.getPackageName(), suiteList );
        }

        return suitePackage;
    }

    public String computePercentage( int tests, int errors, int failures, int skipped )
    {
        float percentage;
        if ( tests == 0 )
        {
            percentage = 0;
        }
        else
        {
            percentage = ( (float) ( tests - errors - failures - skipped ) / (float) tests ) * PCENT;
        }

        return numberFormat.format( percentage );
    }

    public List getFailureDetails( List testSuitesList )
    {
        ListIterator iter = testSuitesList.listIterator();

        List failureDetailList = new ArrayList();

        while ( iter.hasNext() )
        {
            ReportTestSuite suite = (ReportTestSuite) iter.next();

            List testCaseList = suite.getTestCases();

            if ( testCaseList != null )
            {
                ListIterator caseIter = testCaseList.listIterator();

                while ( caseIter.hasNext() )
                {
                    ReportTestCase tCase = (ReportTestCase) caseIter.next();

                    if ( tCase.getFailure() != null )
                    {
                        failureDetailList.add( tCase );
                    }
                }
            }
        }

        return failureDetailList;
    }

    private String[] getIncludedFiles( File directory, String includes, String excludes )
    {
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir( directory );

        scanner.setIncludes( StringUtils.split( includes, "," ) );

        scanner.setExcludes( StringUtils.split( excludes, "," ) );

        scanner.scan();

        return scanner.getIncludedFiles();
    }
}
