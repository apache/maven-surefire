package org.apache.maven.surefire.its;

import java.io.File;
import java.util.List;

import junit.framework.Assert;

import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.apache.maven.reporting.MavenReportException;

public class HelperAssertions
{
    public static void assertTestSuiteResults( int total, int errors, int failures, int skipped,
                                               ITSuiteResults actualSuite )
    {
        Assert.assertEquals( "wrong number of tests", total, actualSuite.getTotal() );
        Assert.assertEquals( "wrong number of errors", errors, actualSuite.getErrors() );
        Assert.assertEquals( "wrong number of failures", failures, actualSuite.getFailures() );
        Assert.assertEquals( "wrong number of skipped", skipped, actualSuite.getSkipped() );
    }
    
    public static void assertTestSuiteResults( int total, int errors, int failures, int skipped,
                                               File testDir ) throws MavenReportException {
        ITSuiteResults suite = parseTestResults( new File[] { testDir } );
        assertTestSuiteResults( total, errors, failures, skipped, suite );
    }

    public static ITSuiteResults parseTestResults( File testDir )
        throws MavenReportException
    {
        return parseTestResults( new File[] { testDir } );
    }

    public static ITSuiteResults parseTestResults( File[] testDirs )
        throws MavenReportException
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
        try {
            reports = parser.parseXMLReportFiles();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't parse XML reports", e);
        }
        Assert.assertTrue( "No reports!", reports.size() > 0 );
        int total = 0, errors = 0, failures = 0, skipped = 0;
        for (int i = 0; i < reports.size(); i++) {
            ReportTestSuite suite = (ReportTestSuite) reports.get( i );
            total += suite.getNumberOfTests();
            errors += suite.getNumberOfErrors();
            failures += suite.getNumberOfFailures();
            skipped += suite.getNumberOfSkipped();
        }
        ITSuiteResults results = new ITSuiteResults(total, errors, failures, skipped);
        return results;
    }
}
