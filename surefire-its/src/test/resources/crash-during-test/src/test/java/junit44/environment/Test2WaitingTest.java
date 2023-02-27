package junit44.environment;

import org.junit.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class Test2WaitingTest
{
    @Test
    public void nonCrashingTest()
            throws InterruptedException
    {
        MILLISECONDS.sleep( 1500L );
    }
}
