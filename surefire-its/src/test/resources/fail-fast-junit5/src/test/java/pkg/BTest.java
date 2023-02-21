package pkg;

import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class BTest
{
    @Test
    public void test()
        throws InterruptedException
    {
        MILLISECONDS.sleep( 1000L );
        throw new RuntimeException();
    }

}
