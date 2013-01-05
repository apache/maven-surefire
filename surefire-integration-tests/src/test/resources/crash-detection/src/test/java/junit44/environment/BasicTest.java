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
    public static void killTheVm(){
        if ( Boolean.getBoolean( "killHard" ))
        {
            Runtime.getRuntime().halt( 0 );
        }
        else {
            System.exit( 0 );
        }
    }

}
