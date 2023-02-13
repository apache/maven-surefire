package jiras.surefire1036;

import org.junit.Test;

import static org.junit.Assert.fail;

public class TestSomeUnit
{
    @Test
    public void thisIsJustAUnitTest()
        throws Exception
    {
        String message = "This unit test will never pass";
        System.out.println( message );
        fail();
    }
}
