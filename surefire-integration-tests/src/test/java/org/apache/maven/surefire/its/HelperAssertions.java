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
                                               ReportTestSuite actualSuite )
    {
        Assert.assertEquals( "wrong number of tests", total, actualSuite.getNumberOfTests() );
        Assert.assertEquals( "wrong number of errors", errors, actualSuite.getNumberOfErrors() );
        Assert.assertEquals( "wrong number of failures", failures, actualSuite.getNumberOfFailures() );
        Assert.assertEquals( "wrong number of skipped", skipped, actualSuite.getNumberOfSkipped() );
    }
    
    public static void assertTestSuiteResults( int total, int errors, int failures, int skipped,
                                               File testDir ) throws MavenReportException {
        ReportTestSuite suite = parseTestResults( testDir );
        assertTestSuiteResults( total, errors, failures, skipped, suite );
    }

    public static ReportTestSuite parseTestResults( File testDir )
        throws MavenReportException
    {
        SurefireReportParser parser = new SurefireReportParser();
        File reportsDir = new File( testDir, "target/surefire-reports" );
        Assert.assertTrue( "Reports directory is missing: " + reportsDir.getAbsolutePath(), reportsDir.exists() );
        parser.setReportsDirectory( reportsDir );
        List reports = parser.parseXMLReportFiles();
        Assert.assertTrue( "No reports!", reports.size() > 0 );
        ReportTestSuite suite = (ReportTestSuite) reports.get( 0 );
        return suite;
    }
}
