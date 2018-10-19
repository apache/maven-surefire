package pkg;

import org.testng.annotations.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class CTest
{
    @Test
    public void test()
        throws InterruptedException
    {
        MILLISECONDS.sleep( 9000L );
    }

}
