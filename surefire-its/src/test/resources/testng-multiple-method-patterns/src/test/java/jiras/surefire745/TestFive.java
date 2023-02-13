package jiras.surefire745;

import org.testng.annotations.*;

public class TestFive
{

    @Test
    public void testSuccessOne()
    {
        System.out.println( getClass() + "#testSuccessOne" );
    }

    @Test
    public void testSuccessTwo()
    {
        System.out.println( getClass() + "#testSuccessTwo" );
    }

    @Test
    public void testSuccessThree()
    {
        System.out.println( getClass() + "#testSuccessThree" );
    }
}
