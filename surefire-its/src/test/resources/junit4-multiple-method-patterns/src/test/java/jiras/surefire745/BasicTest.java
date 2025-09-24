package jiras.surefire745;


import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.junit.Assert.*;


public class BasicTest
{
    @Rule
    public final TestName testName = new TestName();

    @Test
    public void testSuccessOne()
    {
        System.out.println( getClass() + "#" + testName.getMethodName() );
    }

    @Test
    public void testSuccessTwo()
    {
        System.out.println( getClass() + "#" + testName.getMethodName() );
    }

    @Test
    public void testFailure()
    {
        System.out.println( getClass() + "#" + testName.getMethodName() );
        fail( );
    }
}
