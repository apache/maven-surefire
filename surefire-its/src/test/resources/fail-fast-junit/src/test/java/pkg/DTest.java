package pkg;

import org.junit.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class DTest
{
    @Test
    public void test()
        throws InterruptedException
    {
        MILLISECONDS.sleep( 9000L );
    }

}
