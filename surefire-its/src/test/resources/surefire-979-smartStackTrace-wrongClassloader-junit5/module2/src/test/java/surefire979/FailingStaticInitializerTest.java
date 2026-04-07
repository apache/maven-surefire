package surefire979;

import org.junit.jupiter.api.Test;


public class FailingStaticInitializerTest extends TestBase
{

    @Test
    public void test()
    {
        throw new IllegalStateException("This test will never run");

    }

}
