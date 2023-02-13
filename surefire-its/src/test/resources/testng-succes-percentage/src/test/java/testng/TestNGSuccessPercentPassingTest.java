package testng;

import org.testng.annotations.*;
import static org.testng.Assert.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TestNGSuccessPercentPassingTest
{

    private static final AtomicInteger counter = new AtomicInteger( 0 );

    // Pass 2 of 4 tests, expect this test to pass when 50% success is required
    @Test( invocationCount = 4, successPercentage = 50 )
    public void testSuccess()
    {
        int value = counter.addAndGet( 1 );
        assertTrue( isOdd( value ), "is odd: " + value );
    }

    private boolean isOdd( int number )
    {
        return number % 2 == 0;
    }
}
