package org.codehaus.surefire;

import org.codehaus.surefire.report.Report;
import org.codehaus.surefire.report.ReportEntry;

/**
 *
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class TestReport
    implements Report
{
    public void runStarting( int testCount )
    {
        System.out.println( "TestReport::runStarting -> " + testCount );
    }

    public void testStarting( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::testStarting -> " + reportEntry.getMessage() );
    }

    public void testSucceeded( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::testSucceeded -> " + reportEntry.getMessage() );
    }

    public void testFailed( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::testFailed -> " + reportEntry.getMessage() );
    }

    public void batteryStarting( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::suiteStarting -> " + reportEntry.getMessage() );
    }

    public void batteryCompleted( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::suiteCompleted -> " + reportEntry.getMessage() );
    }

    public void batteryAborted( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::suiteAborted -> " + reportEntry.getMessage() );
    }

    public void infoProvided( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::infoProvided -> " + reportEntry.getMessage() );
    }

    public void runStopped()
    {
        System.out.println( "TestReport::runStopped" );
    }

    public void runAborted( ReportEntry reportEntry )
    {
        System.out.println( "TestReport::runAborted -> " + reportEntry.getMessage() );
    }

    public void runCompleted()
    {
        System.out.println( "TestReport::runCompleted" );
    }

    public void dispose()
    {
        System.out.println( "TestReport::dispose" );
    }
}
