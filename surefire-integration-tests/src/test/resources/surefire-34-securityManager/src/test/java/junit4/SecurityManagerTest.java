package junit4;

import java.io.File;

import junit.framework.TestCase;


public class SecurityManagerTest
    extends TestCase
{

    private boolean setUpCalled = false;

    private static boolean tearDownCalled = false;
    
    public void setUp()
    {
        setUpCalled = true;
        tearDownCalled = false;
        System.out.println( "Called setUp" );
    }

    public void tearDown()
    {
        setUpCalled = false;
        tearDownCalled = true;
        System.out.println( "Called tearDown" );
    }

    public void testSetUp()
    {
        assertTrue( "setUp was not called", setUpCalled );
    }
  
    public void testWillFailWhenAccessingCurrentDirectory()
    {
        File file = new File( "." );
        file.isDirectory();
    }

}
