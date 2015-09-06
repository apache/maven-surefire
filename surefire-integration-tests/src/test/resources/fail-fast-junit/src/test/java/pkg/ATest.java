package pkg;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class ATest
{

    @Test
    public void someMethod()
        throws Exception
    {
        TimeUnit.MILLISECONDS.sleep( 500 );
        throw new RuntimeException( "assert \"foo\" == \"bar\"\n" +
                                        "             |\n"
                                        + "             false" );
    }
}