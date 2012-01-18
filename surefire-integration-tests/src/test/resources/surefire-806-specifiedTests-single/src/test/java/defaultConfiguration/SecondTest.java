package defaultConfiguration;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SecondTest
    extends TestCase
{

    public void testSetUp()
    {
        assertEquals( "second", System.getProperty( "phaseName" ) );
    }

}
