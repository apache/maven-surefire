package junit44.environment;

import org.junit.AfterClass;
import org.junit.Test;


public class BasicTest
{

    
    @Test
    public void testNothing()
    {
    }

    @AfterClass
    public static void waitSomeTimeAround(){
        Thread.sleep( 60000 );
    }

}
