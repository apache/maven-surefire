package junit5;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class BasicTest
{

    private boolean setUpCalled = false;

    private static boolean tearDownCalled = false;
    
    @BeforeEach
    public void setUp()
    {
        setUpCalled = true;
        tearDownCalled = false;
        System.out.println( "Called setUp" );
    }

    @AfterEach
    public void tearDown()
    {
        setUpCalled = false;
        tearDownCalled = true;
        System.out.println( "Called tearDown" );
    }

    @Test
    public void testSetUp()
    {
        assertTrue( "setUp was not called", setUpCalled );
    }
    
    
    @Test
    public void testSuccessOne()
    {
        assertTrue( true );
    }    
    
    @Test
    public void testSuccessTwo()
    {
        assertTrue( true );
    }   
    
    @Test
    public void testFailOne()
    {
        assertFalse( false );
    } 

    @AfterClass
    public static void oneTimeTearDown()
    {
        
    }

}
