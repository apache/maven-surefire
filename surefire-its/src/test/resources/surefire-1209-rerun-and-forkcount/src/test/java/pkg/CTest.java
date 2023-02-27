package pkg;

import org.junit.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class CTest
{
    @Test
    public void testC()
            throws InterruptedException
    {
        MILLISECONDS.sleep( 500 );
    }

}
