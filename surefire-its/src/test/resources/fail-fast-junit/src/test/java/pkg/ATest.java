package pkg;

import org.junit.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ATest
{
    @Test
    public void someMethod()
        throws InterruptedException
    {
        MILLISECONDS.sleep( 4_800L );
        throw new RuntimeException( "assert \"foo\" == \"bar\"\n" +
                                        "             |\n"
                                        + "             false" );
    }
}
