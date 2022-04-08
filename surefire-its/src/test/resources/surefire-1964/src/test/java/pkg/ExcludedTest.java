package pkg;

import org.junit.Test;

public class ExcludedTest
{
    @Test
    public void dontRun()
    {
        System.out.println( "executed dontRun" );
    }
}
