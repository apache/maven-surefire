package failureresultcounting;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Kristian Rosenvold
 */
public class OrdinaryError
{

    @Test
    public void ordinaryEror()
    {
        throw new RuntimeException( "Exception in @before" );
    }

    @Test
    public void ordinaryFailure()
    {
        Assert.fail();
    }

}
