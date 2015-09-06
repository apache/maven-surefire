package pkg;

import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

public class ATest
{
    @Test
    public void someMethod()
        throws InterruptedException
    {
        TimeUnit.MILLISECONDS.sleep( 500 );
        throw new RuntimeException( "assert \"foo\" == \"bar\"\n"
                                        + "             |\n"
                                        + "             false" );
    }
}
