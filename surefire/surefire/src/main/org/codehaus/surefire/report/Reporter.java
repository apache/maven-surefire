package org.codehaus.surefire.report;

public interface Reporter
{
    void writeMessage( String message );

    // The entire run
    void runStarting( int testCount );

    void runCompleted();

    void runStopped();

    void runAborted( ReportEntry report );

    // Battery
    void batteryStarting( ReportEntry report )
        throws Exception;

    void batteryCompleted( ReportEntry report );

    void batteryAborted( ReportEntry report );

    // Tests
    void testStarting( ReportEntry report );

    void testSucceeded( ReportEntry report );

    void testError( ReportEntry report, String stdOut, String stdErr );

    void testFailed( ReportEntry report, String stdOut, String stdErr );

    void dispose();

    // Counters
    int getNbErrors();

    int getNbFailures();

    int getNbTests();

    void setReportsDirectory( String reportsDirectory );
}
