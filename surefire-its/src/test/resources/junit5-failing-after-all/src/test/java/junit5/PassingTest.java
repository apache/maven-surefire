package junit5;

import org.junit.jupiter.api.Test;

/**
 * A simple passing test class to verify that other tests are unaffected
 * by the failing @AfterAll in another test class.
 */
public class PassingTest
{
    @Test
    public void testPassingOne()
    {
        System.out.println( "testPassingOne passed" );
    }
}
