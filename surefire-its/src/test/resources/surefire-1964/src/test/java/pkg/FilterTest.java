package pkg;

import org.junit.Test;

public class FilterTest
{
    @Test
    public void testABC()
    {
        System.out.println( "executed testABC" );
    }

    @Test
    public void dontRun()
    {
        System.out.println( "executed dontRun" );
    }

    @Test
    public void testXYZ()
    {
        System.out.println( "executed testXYZ" );
    }
}
