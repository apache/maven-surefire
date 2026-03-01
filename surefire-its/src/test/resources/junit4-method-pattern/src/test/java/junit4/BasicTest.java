package junit4;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;


public class BasicTest
{

    private boolean setUpCalled = false;

    private static boolean tearDownCalled = false;
    
    @Before
    public void setUp()
    {
        setUpCalled = true;
        tearDownCalled = false;
        System.out.println( "Called setUp" );
    }

    @After
    public void tearDown()
    {
        setUpCalled = false;
        tearDownCalled = true;
        System.out.println( "Called tearDown" );
    }

    @Test
    public void testSetUp()
    {
        Assert.assertTrue( "setUp was not called", setUpCalled );
    }
    
    
    @Test
    public void testSuccessOne()
    {
        Assert.assertTrue( true );
    } 
    
    @Test
    @Category(SampleCategory.class)
    public void testSuccessTwo()
    {
        Assert.assertTrue( true );
    }    

    @AfterClass
    public static void oneTimeTearDown()
    {
        
    }

}
