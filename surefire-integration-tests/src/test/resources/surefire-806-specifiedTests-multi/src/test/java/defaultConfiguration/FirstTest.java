package defaultConfiguration;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FirstTest
    extends TestCase
{

    public void testSetUp()
    {
        assertEquals( "first", System.getProperty( "phaseName" ) );
    }

}
