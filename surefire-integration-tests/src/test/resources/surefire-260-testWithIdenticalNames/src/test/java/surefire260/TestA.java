package surefire260;

import junit.framework.TestCase;

public class TestA
    extends TestCase
{
    public void testOne()
    {
    }

    public void testDup()
    {
        fail( "This is what we want" );
    }
}
