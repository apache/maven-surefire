package pkg;

import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ATest
{
    private static int count;

    @Test
    public void testA()
            throws Exception
    {
        MILLISECONDS.sleep( 500 );
        if ( count++ != 2 )
        {
            throw new RuntimeException( "assert \"foo\" == \"bar\"\n"
                                                + "             |\n"
                                                + "             false"
            );
        }
        SECONDS.sleep( 5 );
    }
}
