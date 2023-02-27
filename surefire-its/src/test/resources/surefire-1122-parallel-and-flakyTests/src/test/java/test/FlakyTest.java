package test;

import org.junit.Test;

public class FlakyTest
{
    private static int x = 1;

    @Test
    public void failsOnFirstExecution()
    {
        if ( x++ < 2 )
        {
            org.junit.Assert.fail( "First execution always fails. Try again." );
        }
    }

    @Test
    public void alwaysPasses()
    {
    }
}
