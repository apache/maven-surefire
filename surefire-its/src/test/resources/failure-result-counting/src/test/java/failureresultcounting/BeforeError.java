package failureresultcounting;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Kristian Rosenvold
 */
public class BeforeError
{

    @Before
    public void exceptionInBefore()
    {
        throw new RuntimeException( "Exception in @before" );
    }

    @Test
    public void ok()
    {
        System.out.println( "exceptionInBefore run!!");
    }

    /*@Test
    public void ok2()
    {
        System.out.println( "exceptionInBefore2 run!!");
    } */

}
