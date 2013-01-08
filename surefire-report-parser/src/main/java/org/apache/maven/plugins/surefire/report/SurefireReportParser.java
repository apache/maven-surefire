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
import java.util.Locale;
import java.util.Map;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.maven.shared.utils.io.DirectoryScanner;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 *
 */
public class SurefireReportParser
{
    private static final String INCLUDES = "*.xml";

    private static final String EXCLUDES = "*.txt, testng-failed.xml, testng-failures.xml, testng-results.xml, failsafe-summary*.xml";

    private NumberFormat numberFormat = NumberFormat.getInstance();

    private List<File> reportsDirectories;

    private final List<ReportTestSuite> testSuites = new ArrayList<ReportTestSuite>();

    private static final int PCENT = 100;

    public SurefireReportParser()
    {
    }

    public SurefireReportParser( List<File> reportsDirectoriesFiles, Locale locale )
    {
        this.reportsDirectories = reportsDirectoriesFiles;

        setLocale( locale );
    }

    public List<ReportTestSuite> parseXMLReportFiles()
        throws MavenReportException
    {
        List<File> xmlReportFileList = new ArrayList<File>();
        for ( File reportsDirectory : reportsDirectories )
        {
            if ( !reportsDirectory.exists() )
            {
                continue;
            }
            String[] xmlReportFiles = getIncludedFiles( reportsDirectory, INCLUDES, EXCLUDES );
            for ( String xmlReportFile : xmlReportFiles )
            {
                File xmlReport = new File( reportsDirectory, xmlReportFile );
                xmlReportFileList.add( xmlReport );
            }
        }
        TestSuiteXmlParser parser = new TestSuiteXmlParser();
        for ( File aXmlReportFileList : xmlReportFileList )
        {
            Collection<ReportTestSuite> suites;

            try
            {
                suites = parser.parse( aXmlReportFileList.getAbsolutePath() );
            }
            catch ( ParserConfigurationException e )
            {
                throw new MavenReportException( "Error setting up parser for JUnit XML report", e );
            }
            catch ( SAXException e )
            {
                throw new MavenReportException( "Error parsing JUnit XML report " + aXmlReportFileList, e );
            }
            catch ( IOException e )
            {
                throw new MavenReportException( "Error reading JUnit XML report " + aXmlReportFileList, e );
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

    public Map<String, String> getSummary( List<ReportTestSuite> suites )
    {
        Map<String, String> totalSummary = new HashMap<String, String>();

        int totalNumberOfTests = 0;

        int totalNumberOfErrors = 0;

        int totalNumberOfFailures = 0;

        int totalNumberOfSkipped = 0;

        float totalElapsedTime = 0.0f;

        for ( ReportTestSuite suite : suites )
        {
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

    public Map<String, List<ReportTestSuite>> getSuitesGroupByPackage( List<ReportTestSuite> testSuitesList )
    {
        Map<String, List<ReportTestSuite>> suitePackage = new HashMap<String, List<ReportTestSuite>>();

        for ( ReportTestSuite suite : testSuitesList )
        {
            List<ReportTestSuite> suiteList = new ArrayList<ReportTestSuite>();

            if ( suitePackage.get( suite.getPackageName() ) != null )
            {
                suiteList = suitePackage.get( suite.getPackageName() );
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

    public List<ReportTestCase> getFailureDetails( List<ReportTestSuite> testSuitesList )
    {
        List<ReportTestCase> failureDetailList = new ArrayList<ReportTestCase>();

        for ( ReportTestSuite suite : testSuitesList )
        {
            List<ReportTestCase> testCaseList = suite.getTestCases();

            if ( testCaseList != null )
            {
                for ( ReportTestCase tCase : testCaseList )
                {

                    if ( tCase.getFailure() != null )
                    {
                        failureDetailList.add( tCase );
                    }
                }
            }
        }

        return failureDetailList;
    }

    /**
     * Returns {@code true} if the specified directory contains at least one report file.
     *
     * @param directory the directory
     * @return {@code true} if the specified directory contains at least one report file.
     */
    public static boolean hasReportFiles( File directory )
    {
        return directory != null && directory.isDirectory()
            && getIncludedFiles( directory, INCLUDES, EXCLUDES ).length > 0;
    }

    private static String[] getIncludedFiles( File directory, String includes, String excludes )
    {
        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir( directory );

        scanner.setIncludes( StringUtils.split( includes, "," ) );

        scanner.setExcludes( StringUtils.split( excludes, "," ) );

        scanner.scan();

        return scanner.getIncludedFiles();
    }
}
