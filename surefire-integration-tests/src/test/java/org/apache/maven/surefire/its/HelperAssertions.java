package org.apache.maven.surefire.its;

import junit.framework.Assert;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.apache.maven.reporting.MavenReportException;

import java.io.File;
import java.util.List;

public class HelperAssertions
{
    /** assert that the reports in the specified testDir have the right summary statistics */
    public static void assertTestSuiteResults( int total, int errors, int failures, int skipped,
                                               File testDir ) throws MavenReportException {
        ITSuiteResults suite = parseTestResults( new File[] { testDir } );
        assertTestSuiteResults( total, errors, failures, skipped, suite );
    }
    
    protected static void assertTestSuiteResults( int total, int errors, int failures, int skipped,
                                               ITSuiteResults actualSuite )
    {
        Assert.assertEquals( "wrong number of tests", total, actualSuite.getTotal() );
        Assert.assertEquals( "wrong number of errors", errors, actualSuite.getErrors() );
        Assert.assertEquals( "wrong number of failures", failures, actualSuite.getFailures() );
        Assert.assertEquals( "wrong number of skipped", skipped, actualSuite.getSkipped() );
    }

    protected static ITSuiteResults parseTestResults( File testDir )
        throws MavenReportException
    {
        return parseTestResults( new File[] { testDir } );
    }

    protected static ITSuiteResults parseTestResults( File[] testDirs )
        throws MavenReportException
    {
        List reports = extractReports( testDirs );
        ITSuiteResults results = parseReportList( reports );
        return results;
    }

    /** Converts a list of ReportTestSuites into an ITSuiteResults object, suitable for summary assertions */
    protected static ITSuiteResults parseReportList( List reports )
    {
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

    protected static List extractReports( File testDir )
    {
        return extractReports( new File[] { testDir } );
    }
    
    /** Extracts a list of ReportTestSuites from the specified testDirs */
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
        try {
            reports = parser.parseXMLReportFiles();
        } catch (Exception e) {
            throw new RuntimeException("Couldn't parse XML reports", e);
        }
        return reports;
    }
}
