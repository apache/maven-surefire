package pkg;

import org.junit.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class BTest
{
    @Test
    public void test()
        throws InterruptedException
    {
        MILLISECONDS.sleep( 12_000L );
        throw new RuntimeException();
    }

}
