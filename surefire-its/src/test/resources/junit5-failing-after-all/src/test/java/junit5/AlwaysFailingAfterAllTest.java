package junit5;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/**
 * Test class with @AfterAll that always fails.
 * All test methods pass, but the class-level teardown always throws.
 */
public class AlwaysFailingAfterAllTest
{
    @AfterAll
    static void tearDown()
    {
        throw new IllegalStateException( "AfterAll always fails" );
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
