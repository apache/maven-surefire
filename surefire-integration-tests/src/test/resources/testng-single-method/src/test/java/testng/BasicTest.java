package testng;
import org.testng.annotations.*;
import org.testng.Assert;

public class BasicTest
{

    private boolean setUpCalled = false;

    private static boolean tearDownCalled = false;
    
    @BeforeTest
    public void setUp()
    {
        setUpCalled = true;
        tearDownCalled = false;
        System.out.println( "Called setUp" );
    }

    @AfterTest
    public void tearDown()
    {
        setUpCalled = false;
        tearDownCalled = true;
        System.out.println( "Called tearDown" );
    }

    @Test
    public void testSetUp()
    {
        Assert.assertTrue( setUpCalled );
    }
    
    
    @Test
    public void testSuccessOne()
    {
        Assert.assertTrue( true );
    } 
    
    @Test
    public void testSuccessTwo()
    {
        Assert.assertTrue( true );
    }    

    @AfterClass
    public static void oneTimeTearDown()
    {
        
    }

}
