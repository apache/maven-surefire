package org.codehaus.surefire.report;

public interface Report
{
    // The entire run
    void runStarting( int testCount );

    void runCompleted();

    void runStopped();

    void runAborted( ReportEntry report );

    // Battery
    void batteryStarting( ReportEntry report );

    void batteryCompleted( ReportEntry report );

    void batteryAborted( ReportEntry report );

    // Tests
    void testStarting( ReportEntry report );

    void testSucceeded( ReportEntry report );

    void testFailed( ReportEntry report );

    void dispose();
}
