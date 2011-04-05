package testng;
import org.testng.annotations.*;
import org.testng.Assert;

public class BasicTest
{

    private boolean setUpCalled = false;

    private static boolean tearDownCalled = false;

    private Integer foo;
    
    @BeforeTest
    public void intialize()
    {
        setUpCalled = true;
        tearDownCalled = false;
        System.out.println( "Called setUp" );
        foo = Integer.valueOf( 1 );
    }

    @AfterTest
    public void shutdown()
    {
        setUpCalled = false;
        tearDownCalled = true;
        System.out.println( "Called tearDown" );
    }

    @Test
    public void testSetUp()
    {
        Assert.assertTrue( setUpCalled );
        Assert.assertNotNull( foo );
    }
    
    
    @Test
    public void testSuccessOne()
    {
        Assert.assertTrue( true );
        Assert.assertNotNull( foo );
    } 
    
    @Test
    public void testSuccessTwo()
    {
        Assert.assertTrue( true );
        Assert.assertNotNull( foo );
    }    

    @AfterClass
    public static void oneTimeTearDown()
    {
        
    }

}
