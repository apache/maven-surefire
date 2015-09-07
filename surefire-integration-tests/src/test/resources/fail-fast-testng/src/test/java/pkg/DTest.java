package pkg;

import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

public class DTest
{
    @Test
    public void test()
        throws InterruptedException
    {
        TimeUnit.MILLISECONDS.sleep(500);
    }

}
