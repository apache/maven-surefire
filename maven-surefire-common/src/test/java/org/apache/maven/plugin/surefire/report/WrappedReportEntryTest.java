package org.apache.maven.plugin.surefire.report;

import junit.framework.TestCase;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.SimpleReportEntry;

/**
 * @author Kristian Rosenvold
 */
public class WrappedReportEntryTest
    extends TestCase
{

    public void testClassNameOnly()
        throws Exception
    {
        String category = "surefire.testcase.JunitParamsTest";
        WrappedReportEntry wr =
            new WrappedReportEntry( new SimpleReportEntry( "fud", category ), null, 12, null, null );
        final String reportName = wr.getReportName();
        assertEquals( "surefire.testcase.JunitParamsTest", reportName );
    }

    public void testRegular()
    {
        ReportEntry reportEntry = new SimpleReportEntry( "fud", "testSum(surefire.testcase.NonJunitParamsTest)" );
        WrappedReportEntry wr = new WrappedReportEntry( reportEntry, null, 12, null, null );
        final String reportName = wr.getReportName();
        assertEquals( "testSum", reportName );
    }

    public void testGetReportNameWithParams()
        throws Exception
    {
        String category = "[0] 1\u002C 2\u002C 3 (testSum)(surefire.testcase.JunitParamsTest)";
        ReportEntry reportEntry = new SimpleReportEntry( "fud", category );
        WrappedReportEntry wr = new WrappedReportEntry( reportEntry, null, 12, null, null );
        final String reportName = wr.getReportName();
        assertEquals( "[0] 1, 2, 3 (testSum)", reportName );
    }


}
