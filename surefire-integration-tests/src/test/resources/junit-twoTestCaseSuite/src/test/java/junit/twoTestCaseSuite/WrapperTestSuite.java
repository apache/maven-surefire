package junit.twoTestCaseSuite;

import junit.framework.Test;
import junit.framework.TestSuite;

public class WrapperTestSuite
    extends TestSuite
{
    public WrapperTestSuite( String name )
    {
        super( name );
    }

    public static Test suite() {
        WrapperTestSuite suite = new WrapperTestSuite( "My Acceptance Test Suite" );
        suite.addTestSuite( TestTwo.class );
        suite.addTest( BasicTest.suite() );
        return suite;
    }
}
