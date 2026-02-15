package jiras.surefire1152;

import org.junit.Test;

import static org.junit.Assert.fail;

public class FlakyTest
{
    private static int n;

    @Test
    public void testFlaky()
    {
        if ( n++ == 0 )
        {
            fail( "deliberately flaky test (should pass the next time)" );
        }
    }

}
