package org.apache.maven.surefire.report;

import junit.framework.TestCase;

/**
 * @author Kristian Rosenvold
 */
public class DescriptionDecoderTest
    extends TestCase
{
    public void testGetReportNameWithParams()
        throws Exception
    {
        String category = "[0] 1\u002C 2\u002C 3 (testSum)(surefire.testcase.JunitParamsTest)";
        ReportEntry reportEntry = new SimpleReportEntry( "fud", category );
        final String reportName = new DescriptionDecoder().getReportName( reportEntry );
        assertEquals( "[0] 1, 2, 3 (testSum)", reportName );
    }

    public void testClassNameOnly()
        throws Exception
    {
        String category = "surefire.testcase.JunitParamsTest";
        ReportEntry reportEntry = new SimpleReportEntry( "fud", category );
        final String reportName = new DescriptionDecoder().getReportName( reportEntry );
        assertEquals( "surefire.testcase.JunitParamsTest", reportName );
    }

    public void testRegular()
    {
        ReportEntry reportEntry = new SimpleReportEntry( "fud", "testSum(surefire.testcase.NonJunitParamsTest)" );
        final String reportName = new DescriptionDecoder().getReportName( reportEntry );
        assertEquals( "testSum", reportName );
    }
}
