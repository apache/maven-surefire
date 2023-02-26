package junit4;

import org.junit.Assert;
import org.junit.Test;


public class PassingTest
{
    @Test
    public void testPassingTestOne()
    {
        System.out.println( "Passing test one" );
    }

    @Test
    public void testPassingTestTwo() throws Exception
    {
        System.out.println( "Passing test two" );
    }
}
