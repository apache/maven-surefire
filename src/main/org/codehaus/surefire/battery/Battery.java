package org.codehaus.surefire.battery;

import org.codehaus.surefire.report.ReporterManager;

import java.util.List;

public interface Battery
{
    void execute( ReporterManager reportManager )
        throws Exception;

    int getTestCount();

    String getBatteryName();

    void discoverBatteryClassNames()
        throws Exception;

    List getSubBatteryClassNames();
}
