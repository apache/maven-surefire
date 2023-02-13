package runorder.parallel;

import org.junit.Test;

/**
 * @author Kristian Rosenvold
 */
public class Test2
{

    @Test
    public void testSleep1()
    {
        Test1.sleep( Test1.ms );
    }

    @Test
    public void testSleep2()
    {
        Test1.sleep( Test1.ms );
    }

    @Test
    public void testSleep3()
    {
        Test1.sleep( Test1.ms );
    }

    @Test
    public void testSleep4()
    {
        Test1.sleep( Test1.ms );
    }

}
