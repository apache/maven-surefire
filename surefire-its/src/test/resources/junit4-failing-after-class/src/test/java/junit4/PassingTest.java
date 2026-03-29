package junit4;

import org.junit.Test;

/**
 * A simple passing test class to verify that other tests are unaffected
 * by the failing @AfterClass in another test class.
 */
public class PassingTest
{
    @Test
    public void testPassingOne()
    {
        System.out.println( "testPassingOne passed" );
    }
}
