package forkPerTestNoSetup;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestSurefire2
    extends TestCase
{

    private boolean setUpCalled = false;

    private static boolean tearDownCalled = false;

    public TestSurefire2( String name, String extraName )
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        Test test = new TestSurefire2( "testSetUp", "dummy" );
        suite.addTest( test );
        TestSetup setup = new TestSetup( suite )
        {

            protected void setUp()
            {
                //oneTimeSetUp();
            }

            protected void tearDown()
            {
                oneTimeTearDown();
            }

        };

        return setup;
    }

    protected void setUp()
    {
        setUpCalled = true;
        tearDownCalled = false;
        System.out.println( "Called setUp" );
    }

    protected void tearDown()
    {
        setUpCalled = false;
        tearDownCalled = true;
        System.out.println( "Called tearDown" );
    }

    public void testSetUp()
    {
        assertTrue( "setUp was not called", setUpCalled );
    }

    public static void oneTimeTearDown()
    {
        assertTrue( "tearDown was not called", tearDownCalled );
    }

}
