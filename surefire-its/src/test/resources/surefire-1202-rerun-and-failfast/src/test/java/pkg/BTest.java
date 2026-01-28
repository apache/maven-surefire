package pkg;

import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.SECONDS;

public class BTest
{

    private static int count;

    @Test
    public void testB()
            throws InterruptedException
    {
        SECONDS.sleep( 2 );
        if ( count++ != 2 )
        {
            throw new RuntimeException();
        }
    }

}
