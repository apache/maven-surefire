package jiras.surefire745;


import org.testng.annotations.*;
import static org.testng.Assert.*;

public class BasicTest
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
    public void testFailure()
    {
        System.out.println( getClass() + "#testFailure" );
        fail( );
    }
}
