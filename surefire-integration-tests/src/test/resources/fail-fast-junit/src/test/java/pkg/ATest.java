package pkg;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class ATest
{

    @Test
    public void someMethod()
        throws Exception
    {
        // checking processros # due to very slow Windows Jenkins machines
        TimeUnit.MILLISECONDS.sleep( Runtime.getRuntime().availableProcessors() == 1 ? 1200 : 500 );
        throw new RuntimeException( "assert \"foo\" == \"bar\"\n" +
                                        "             |\n"
                                        + "             false" );
    }
}