package surefire979;


import org.junit.Test;


public class FailingStaticInitializerTest extends TestBase
{

    @Test
    public void test()
    {
        throw new IllegalStateException("This test will never run");

    }

}
