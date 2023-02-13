package failureresultcounting;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Kristian Rosenvold
 */
public class BeforeFailure
{

    @Before
    public void failInBEfore()
    {
        Assert.fail( "Failing in @before" );
    }

    @Test
    public void ok()
    {
        System.out.println( "failInBEfore run !!");
    }

}
