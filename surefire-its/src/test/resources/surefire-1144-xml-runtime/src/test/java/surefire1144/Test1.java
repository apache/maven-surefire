package surefire1144;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class Test1
{
    static void sleep( int ms )
    {
        try
        {
            TimeUnit.MILLISECONDS.sleep((long)ms);
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e );
        }
    }

    static void printTimeAndSleep( String msg, int ms )
    {
        System.out.println( msg + " started @ " + System.currentTimeMillis() );
        sleep( ms );
        System.out.println( msg + " finished @ " + System.currentTimeMillis() );
    }

    @Test
    public void testSleep100()
    {
        printTimeAndSleep( "Test1.sleep:100", 100 );
    }

    @Test
    public void testSleep200()
    {
        printTimeAndSleep( "Test1.sleep:200", 200 );
    }

    @Test
    public void testSleep300()
    {
        printTimeAndSleep( "Test1.sleep:300", 300 );
    }

    @BeforeClass
    public static void setUpBeforeClass()
        throws Exception
    {
        printTimeAndSleep( "beforeClass sleep 500", 500 );
    }

    @AfterClass
    public static void tearDownAfterClass()
        throws Exception
    {
        printTimeAndSleep( "afterClass sleep 500", 500 );
    }
}
