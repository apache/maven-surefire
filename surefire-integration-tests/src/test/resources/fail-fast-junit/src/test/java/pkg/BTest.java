package pkg;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class BTest
{
    @Test
    public void test()
        throws InterruptedException
    {
        // checking processros # due to very slow Windows Jenkins machines
        TimeUnit.MILLISECONDS.sleep( Runtime.getRuntime().availableProcessors() == 1 ? 9000 : 3750 );
        throw new RuntimeException();
    }

}
