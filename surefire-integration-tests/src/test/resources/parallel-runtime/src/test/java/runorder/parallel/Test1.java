package runorder.parallel;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Test1
{

    public static final int ms = 5000;

    static void sleep( int ms )
    {
        long target = System.currentTimeMillis() + ms;
        try
        {
            do
            {
                Thread.sleep( ms );
            } while ( System.currentTimeMillis() < target);
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Test
    public void testSleep1()
    {
        Test1.sleep( ms );
    }

    @Test
    public void testSleep2()
    {
        Test1.sleep( ms );
    }

    @Test
    public void testSleep3()
    {
        Test1.sleep( ms );
    }

    @Test
    public void testSleep4()
    {
        Test1.sleep( ms );
    }
}