package junit44.environment;

import dummy.DummyClass;
import org.junit.AfterClass;
import org.junit.Test;

public class Basic02Test
{

    @Test
    public void testNothing()
    {
        System.out.println( new DummyClass().toString() );
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
