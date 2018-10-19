package pkg;

import org.testng.annotations.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ATest
{
    @Test
    public void someMethod()
        throws InterruptedException
    {
        MILLISECONDS.sleep( 3600L );
        throw new RuntimeException( "assert \"foo\" == \"bar\"\n"
                                        + "             |\n"
                                        + "             false" );
    }
}
