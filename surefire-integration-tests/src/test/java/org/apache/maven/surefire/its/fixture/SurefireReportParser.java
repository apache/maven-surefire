package org.apache.maven.surefire.its.fixture;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * @version $Id$
 */
public class SurefireReportParser
{

    private final List<File> reportsDirectories;

    private final List<ReportTestSuite> testSuites = new ArrayList<ReportTestSuite>();

    public SurefireReportParser( List<File> reportsDirectoriesFiles )
    {
        this.reportsDirectories = reportsDirectoriesFiles;

    }

    public List<ReportTestSuite> parseXMLReportFiles()
    {
        List<File> xmlReportFileList = new ArrayList<File>();
        for ( File reportsDirectory : reportsDirectories )
        {
            if ( !reportsDirectory.exists() )
            {
                continue;
            }
            String[] xmlReportFiles = getIncludedFiles( reportsDirectory, "*.xml",
                                                        "*.txt, testng-failed.xml, testng-failures.xml, testng-results.xml" );
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
                throw new RuntimeException( "Error setting up parser for JUnit XML report", e );
            }
            catch ( SAXException e )
            {
                throw new RuntimeException( "Error parsing JUnit XML report " + aXmlReportFileList, e );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Error reading JUnit XML report " + aXmlReportFileList, e );
            }

            testSuites.addAll( suites );
        }

        return testSuites;
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
