package jiras.surefire745;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class TestThree
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
    public void testFailOne()
    {
        System.out.println( getClass() + "#testFailOne" );
        fail();
    }
}
