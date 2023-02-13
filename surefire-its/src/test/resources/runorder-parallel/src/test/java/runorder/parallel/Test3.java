package runorder.parallel;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Kristian Rosenvold
 */
public class Test3
{

    private void sleep( int ms )
    {
        try
        {
            Thread.sleep( ms );
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Test
    public void testSleep100()
    {
        System.out.println( "Test3.sleep100 started @ " + System.currentTimeMillis() );
        Test1.sleep( 100 );
    }

    @Test
    public void testSleep300()
    {
        System.out.println( "Test3.sleep300 started @ " + System.currentTimeMillis() );
        Test1.sleep( 300 );
    }

    @Test
    public void testSleep500()
    {
        System.out.println( "Test3.sleep500 started @ " + System.currentTimeMillis() );
        Test1.sleep( 500 );
    }

    @BeforeClass
    public static void setUpBeforeClass()
        throws Exception
    {
        System.out.println(
            Thread.currentThread().getName() + " Test3 beforeClass sleep 175 " + System.currentTimeMillis() );
        Thread.sleep( 175 );
    }

    @AfterClass
    public static void tearDownAfterClass()
        throws Exception
    {
        System.out.println(
            Thread.currentThread().getName() + " Test3 afterClass sleep 175 " + System.currentTimeMillis() );
        Thread.sleep( 175 );
    }

}
