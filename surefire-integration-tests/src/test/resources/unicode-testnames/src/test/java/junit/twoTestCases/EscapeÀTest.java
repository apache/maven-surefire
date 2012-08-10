package junit.twoTestCases;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class EscapeÀTest
    extends TestCase
{

    private boolean setUpCalled = false;

    private static boolean tearDownCalled = false;

    public EscapeÀTest( String name )
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        Test test = new EscapeÀTest( "testSetUp" );
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
