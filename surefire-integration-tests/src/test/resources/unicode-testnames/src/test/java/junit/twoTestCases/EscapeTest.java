package junit.twoTestCases;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class EscapeTest
    extends TestCase
{

    private boolean setUpCalled = false;

    private static boolean tearDownCalled = false;

    public EscapeTest( String name )
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        Test test = new EscapeTest( "testSetUp" );
        suite.addTest( test );

        return new TestSetup( suite )
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
