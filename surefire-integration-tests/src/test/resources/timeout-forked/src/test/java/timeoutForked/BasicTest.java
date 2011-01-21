package timeoutForked;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class BasicTest
    extends TestCase
{

    public void testSleep() throws Exception
    {
        int sleepLength = Integer.valueOf( System.getProperty( "sleepLength", "10000" ));
        Thread.sleep(sleepLength);
    }

}
