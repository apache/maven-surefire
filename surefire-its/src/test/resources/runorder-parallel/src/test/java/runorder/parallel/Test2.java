package runorder.parallel;

import org.junit.Test;

/**
 * @author Kristian Rosenvold
 */
public class Test2
{

    @Test
    public void testSleep1000()
    {
        System.out.println( "Test2.sleep1000 started @ " + System.currentTimeMillis() );
        Test1.sleep( 1000 );
    }

    @Test
    public void testSleep3000()
    {
        System.out.println( "Test2.sleep3000 started @ " + System.currentTimeMillis() );
        Test1.sleep( 3000 );
    }

    @Test
    public void testSleep5000()
    {
        System.out.println( "Test2.sleep5000 started @ " + System.currentTimeMillis() );
        Test1.sleep( 5000 );
    }
}
