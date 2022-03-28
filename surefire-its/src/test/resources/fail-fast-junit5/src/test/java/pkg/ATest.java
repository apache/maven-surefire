package pkg;

import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ATest
{
    @Test
    public void someMethod()
        throws InterruptedException
    {
        MILLISECONDS.sleep( 1000L );
        throw new RuntimeException( "assert \"foo\" == \"bar\"\n" +
            "             |\n"
            + "             false" );
    }
}
