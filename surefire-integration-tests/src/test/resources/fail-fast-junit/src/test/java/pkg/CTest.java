package pkg;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class CTest
{
    @Test
    public void test()
        throws InterruptedException
    {
        // checking processros # due to very slow Windows Jenkins machines
        TimeUnit.MILLISECONDS.sleep( Runtime.getRuntime().availableProcessors() == 1 ? 3000 : 1250 );
    }

}
