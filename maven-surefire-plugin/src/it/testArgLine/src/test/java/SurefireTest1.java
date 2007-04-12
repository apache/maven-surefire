import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SurefireTest1
    extends TestCase
{

    private boolean setUpCalled = false;

    private static boolean tearDownCalled = false;

    public SurefireTest1( String name, String extraName )
    {
        super( name );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        Test test = new SurefireTest1( "testSetUp", "dummy" );
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
