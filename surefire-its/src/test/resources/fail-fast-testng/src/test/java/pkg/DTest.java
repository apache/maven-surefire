package pkg;

import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

public class DTest
{
    @Test
    public void test()
        throws InterruptedException
    {
        // checking processros # due to very slow Windows Jenkins machines
        TimeUnit.MILLISECONDS.sleep( Runtime.getRuntime().availableProcessors() == 1 ? 9000 : 3750 );
    }

}
