package junit44.environment;

import org.junit.AfterClass;
import org.junit.Test;

public class Basic06Test
{

    @Test
    public void testNothing()
    {
    }

    @AfterClass
    public static void waitSomeTimeAround()
    {
        try
        {
            Thread.sleep( Integer.getInteger( "testSleepTime", 2000 ) );
        }
        catch ( InterruptedException ignored )
        {
        }
    }

}
