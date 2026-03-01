package pkg;

import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class DTest
{
    @Test
    public void testD()
            throws InterruptedException
    {
        MILLISECONDS.sleep( 500 );
    }

}
