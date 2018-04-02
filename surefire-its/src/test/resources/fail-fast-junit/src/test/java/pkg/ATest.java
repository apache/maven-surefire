package pkg;

import org.junit.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ATest
{
    static final int DELAY_MULTIPLIER = 3;

    @Test
    public void someMethod()
        throws Exception
    {
        // checking processros # due to very slow Windows Jenkins machines
        MILLISECONDS.sleep( DELAY_MULTIPLIER * ( Runtime.getRuntime().availableProcessors() == 1 ? 3600L : 1500L ) );
        throw new RuntimeException( "assert \"foo\" == \"bar\"\n" +
                                        "             |\n"
                                        + "             false" );
    }
}