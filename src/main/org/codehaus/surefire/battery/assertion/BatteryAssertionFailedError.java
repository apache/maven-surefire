// Taken from JUnit.

package org.codehaus.surefire.battery.assertion;

/**
 * Thrown when an assertion failed.
 */
public class BatteryAssertionFailedError extends Error
{

    public BatteryAssertionFailedError()
    {
    }

    public BatteryAssertionFailedError( String message )
    {
        super( message );
    }
}