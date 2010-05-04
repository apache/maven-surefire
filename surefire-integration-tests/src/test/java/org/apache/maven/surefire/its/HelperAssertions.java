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

import junit.framework.Assert;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.apache.maven.reporting.MavenReportException;

import java.io.File;
import java.util.List;

public class HelperAssertions
{
    /**
     * assert that the reports in the specified testDir have the right summary statistics
     */
    public static void assertTestSuiteResults( int total, int errors, int failures, int skipped, File testDir )
        throws MavenReportException
    {
        IntegrationTestSuiteResults suite = parseTestResults( new File[]{ testDir } );
        assertTestSuiteResults( total, errors, failures, skipped, suite );
    }

    protected static void assertTestSuiteResults( int total, int errors, int failures, int skipped,
                                                  IntegrationTestSuiteResults actualSuite )
    {
        Assert.assertEquals( "wrong number of tests", total, actualSuite.getTotal() );
        Assert.assertEquals( "wrong number of errors", errors, actualSuite.getErrors() );
        Assert.assertEquals( "wrong number of failures", failures, actualSuite.getFailures() );
        Assert.assertEquals( "wrong number of skipped", skipped, actualSuite.getSkipped() );
    }

    protected static IntegrationTestSuiteResults parseTestResults( File testDir )
        throws MavenReportException
    {
        return parseTestResults( new File[]{ testDir } );
    }

    protected static IntegrationTestSuiteResults parseTestResults( File[] testDirs )
        throws MavenReportException
    {
        List reports = extractReports( testDirs );
        IntegrationTestSuiteResults results = parseReportList( reports );
        return results;
    }

    /**
     * Converts a list of ReportTestSuites into an IntegrationTestSuiteResults object, suitable for summary assertions
     */
    protected static IntegrationTestSuiteResults parseReportList( List reports )
    {
        Assert.assertTrue( "No reports!", reports.size() > 0 );
        int total = 0, errors = 0, failures = 0, skipped = 0;
        for ( int i = 0; i < reports.size(); i++ )
        {
            ReportTestSuite suite = (ReportTestSuite) reports.get( i );
            total += suite.getNumberOfTests();
            errors += suite.getNumberOfErrors();
            failures += suite.getNumberOfFailures();
            skipped += suite.getNumberOfSkipped();
        }
        IntegrationTestSuiteResults results = new IntegrationTestSuiteResults( total, errors, failures, skipped );
        return results;
    }

    protected static List extractReports( File testDir )
    {
        return extractReports( new File[]{ testDir } );
    }

    /**
     * Extracts a list of ReportTestSuites from the specified testDirs
     */
    protected static List extractReports( File[] testDirs )
    {
        SurefireReportParser parser = new SurefireReportParser();
        File[] reportsDirs = new File[testDirs.length];
        for ( int i = 0; i < testDirs.length; i++ )
        {
            File testDir = testDirs[i];
            File reportsDir = new File( testDir, "target/surefire-reports" );
            Assert.assertTrue( "Reports directory is missing: " + reportsDir.getAbsolutePath(), reportsDir.exists() );
            reportsDirs[i] = reportsDir;
        }
        parser.setReportsDirectories( reportsDirs );
        List reports;
        try
        {
            reports = parser.parseXMLReportFiles();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( "Couldn't parse XML reports", e );
        }
        return reports;
    }
}
