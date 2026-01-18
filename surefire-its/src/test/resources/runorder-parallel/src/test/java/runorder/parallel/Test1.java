package runorder.parallel;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Test1
{

    public Test1()
    {
        System.out.println( Thread.currentThread().getName() + " Constructor" );
    }

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

    @Test
    public void testSleep2000()
    {
        System.out.println(
            Thread.currentThread().getName() + " Test1.sleep2000 started @ " + System.currentTimeMillis() );
        sleep( 2000 );
    }

    @Test
    public void testSleep4000()
    {
        System.out.println(
            Thread.currentThread().getName() + " Test1.sleep4000 started @ " + System.currentTimeMillis() );
        sleep( 4000 );
    }

    @Test
    public void testSleep6000()
    {
        System.out.println(
            Thread.currentThread().getName() + " Test1.sleep6000 started @ " + System.currentTimeMillis() );
        sleep( 6000 );
    }

    @BeforeClass
    public static void setUpBeforeClass()
        throws Exception
    {
        System.out.println( Thread.currentThread().getName() + " beforeClass Test1 sleep 175 " + System.currentTimeMillis() );
        Thread.sleep( 175 );
    }

    @AfterClass
    public static void tearDownAfterClass()
        throws Exception
    {
        System.out.println( Thread.currentThread().getName() + " afterClass Test1 sleep 175 " + System.currentTimeMillis() );
        Thread.sleep( 175 );
    }


}
