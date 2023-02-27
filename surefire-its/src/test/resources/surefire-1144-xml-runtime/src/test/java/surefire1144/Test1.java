package surefire1144;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Test1
{
    static void sleep( int ms )
    {
        long target = System.currentTimeMillis() + ms;
        try
        {
            do
            {
                Thread.sleep( 1L );
            }
            while ( System.currentTimeMillis() < target );
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
    }

    @Test
    public void testSleep100()
    {
        printTimeAndSleep( "Test1.sleep100", 100 );
    }

    @Test
    public void testSleep200()
    {
        printTimeAndSleep( "Test1.sleep200", 200 );
    }

    @Test
    public void testSleep300()
    {
        printTimeAndSleep( "Test1.sleep300", 300 );
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
