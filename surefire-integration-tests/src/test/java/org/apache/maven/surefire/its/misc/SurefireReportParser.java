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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * @version $Id$
 */
public class SurefireReportParser
{

    private final List reportsDirectories;

    private final List testSuites = new ArrayList();

    public SurefireReportParser( List reportsDirectoriesFiles, Locale locale )
    {
        this.reportsDirectories = reportsDirectoriesFiles;

    }

    public List parseXMLReportFiles()
    {
        List xmlReportFileList = new ArrayList();
        for ( int i = 0; i < reportsDirectories.size(); i++ )
        {
            File reportsDirectory = (File) reportsDirectories.get( i );
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
                throw new RuntimeException( "Error setting up parser for JUnit XML report", e );
            }
            catch ( SAXException e )
            {
                throw new RuntimeException( "Error parsing JUnit XML report " + currentReport, e );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( "Error reading JUnit XML report " + currentReport, e );
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
