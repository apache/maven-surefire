package defaultConfiguration;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FourthTest
    extends TestCase
{

    public void testSetUp()
    {
        assertEquals( "second", System.getProperty( "phaseName" ) );
    }

}
