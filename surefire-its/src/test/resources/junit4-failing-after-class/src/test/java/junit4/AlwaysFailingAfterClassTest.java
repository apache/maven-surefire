package junit4;

import org.junit.AfterClass;
import org.junit.Test;

/**
 * Test class with @AfterClass that always fails.
 * All test methods pass, but the class-level teardown always throws.
 */
public class AlwaysFailingAfterClassTest
{
    @AfterClass
    public static void tearDown()
    {
        throw new IllegalStateException( "AfterClass always fails" );
    }

    @Test
    public void testOne()
    {
        System.out.println( "testOne passed" );
    }

    @Test
    public void testTwo()
    {
        System.out.println( "testTwo passed" );
    }
}
