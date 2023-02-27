package failureresultcounting;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Kristian Rosenvold
 */
public class BeforeClassFailure
{

    @BeforeClass
    public static void failInBeforeClass()
    {
        Assert.fail( "Failing in @BeforeClass" );
    }

    @Test
    public void ok()
    {
        System.out.println( "failInBeforeClass run !!");
    }

}
