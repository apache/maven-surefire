package org.codehaus.surefire.battery;

import org.codehaus.surefire.report.ReportManager;

import java.util.List;

public interface Battery
{
    void execute( ReportManager reportManager )
        throws Exception;

    int getTestCount();

    String getBatteryName();

    void discoverBatteryClassNames()
        throws Exception;

    List getSubBatteryClassNames();
}
