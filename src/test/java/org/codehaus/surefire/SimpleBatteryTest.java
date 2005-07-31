package org.codehaus.surefire;

import org.codehaus.surefire.battery.AbstractBattery;

/**
 *
 * 
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 *
 * @version $Id$
 */
public class SimpleBatteryTest
    extends AbstractBattery
{
    public void testFoo()
    {
        assertEquals( 1, 1 );
    }

    public void testBar()
    {
        assertEquals( 1, 1 );
    }
}
