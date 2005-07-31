package org.codehaus.surefire.battery.assertion;

public class BatteryAssertionFailedError
    extends Error
{

    public BatteryAssertionFailedError()
    {
    }

    public BatteryAssertionFailedError( String message )
    {
        super( message );
    }
}