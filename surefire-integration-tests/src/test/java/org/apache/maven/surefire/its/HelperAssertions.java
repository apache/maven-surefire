package org.apache.maven.surefire.its;

import junit.framework.Assert;

import org.apache.maven.plugins.surefire.report.ReportTestSuite;

public class HelperAssertions
{
    public static void assertTestSuite(int total, int errors, int failures, int skipped, ReportTestSuite actualSuite) {
        Assert.assertEquals( "wrong number of tests", total, actualSuite.getNumberOfTests() );
        Assert.assertEquals( "wrong number of errors", errors, actualSuite.getNumberOfErrors() );
        Assert.assertEquals( "wrong number of failures", failures, actualSuite.getNumberOfFailures() );
        Assert.assertEquals( "wrong number of skipped", skipped, actualSuite.getNumberOfSkipped() );
    }
}
